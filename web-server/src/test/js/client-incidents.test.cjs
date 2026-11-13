const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/js/core/client-incidents.js'), 'utf8');

function browser({transport, iframe = false} = {}) {
    const listeners = {}, documentListeners = {}, requests = [];
    const location = new URL('https://eis.test/web/view?page=systemsbreakdown-edit&modal=1&dialogId=edit-7&token=secret');
    const window = {addEventListener: (name, callback) => { listeners[name] = callback; }};
    window.parent = iframe ? {location: {href: 'https://eis.test/web/view?page=systemsbreakdown-main&secret=hidden'}} : window;
    const context = vm.createContext({window, location, URL, URLSearchParams,
        document: {currentScript: {src: 'https://eis.test/js/core/client-incidents.js?v=build-123'},
            addEventListener: (name, callback) => { documentListeners[name] = callback; }},
        navigator: {userAgent: 'Test browser'},
        fetch: (url, options) => {
            requests.push({url, options, payload: JSON.parse(options.body)});
            return transport ? transport() : Promise.resolve({status: 204});
        }});
    const install = () => vm.runInContext(source, context);
    install();
    return {window, listeners, documentListeners, requests, install};
}

test('captures an uncaught error with original stack and iframe context', () => {
    const b = browser({iframe: true});
    const error = new Error('Pointer capture failed');
    error.stack = 'NotFoundError: Pointer capture failed\n    at start (https://eis.test/js/components/dialog-geometry.js:69:24)';
    b.listeners.error({error, message: error.message, filename: 'https://eis.test/js/components/dialog-geometry.js?v=123', lineno: 69, colno: 24});
    const {url, options, payload} = b.requests[0];
    assert.equal(url, '/api/incidents/client');
    assert.equal(options.keepalive, true);
    assert.equal(options.credentials, 'same-origin');
    assert.equal(payload.stack, error.stack);
    assert.equal(payload.build, 'build-123');
    assert.equal(payload.dialogId, 'edit-7');
    assert.equal(payload.line, '69');
    assert.equal(payload.column, '24');
    assert.equal(payload.parentUrl, 'https://eis.test/web/view?page=systemsbreakdown-main');
    assert(!payload.url.includes('secret'));
    assert.equal(payload.file, 'https://eis.test/js/components/dialog-geometry.js');
});

test('captures rejected promises including string reasons without suppressing the browser event', () => {
    const b = browser();
    const reason = new Error('Async failure');
    b.listeners.unhandledrejection({reason, preventDefault() { throw new Error('Must not suppress'); }});
    b.listeners.unhandledrejection({reason: 'Rejected string'});
    assert.equal(b.requests[0].payload.stack, reason.stack);
    assert.equal(b.requests[0].payload.kind, 'unhandledrejection');
    assert.equal(b.requests[1].payload.message, 'Rejected string');
});

test('deduplicates errors and caps unique reports per document', () => {
    const b = browser();
    for (let i = 0; i < 10; i++) b.listeners.error({message: 'Same failure'});
    assert.equal(b.requests.length, 1);
    for (let i = 0; i < 30; i++) b.listeners.error({message: `Failure ${i}`});
    assert.equal(b.requests.length, 20);
});

test('transport failures never escape or create rejected logging promises', async () => {
    const rejected = browser({transport: () => Promise.reject(new Error('Offline'))});
    const thrown = browser({transport: () => { throw new Error('Fetch disabled'); }});
    assert.doesNotThrow(() => rejected.listeners.error({message: 'Original error'}));
    assert.doesNotThrow(() => thrown.listeners.error({message: 'Original error'}));
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(rejected.requests.length, 1);
    assert.equal(thrown.requests.length, 1);
});

test('captures active resize and drag context', () => {
    const b = browser();
    const target = resize => ({nodeType: 1, classList: {contains: () => resize}, style: {cursor: resize ? '' : 'move'}});
    b.documentListeners.pointerdown({target: target(true)});
    b.listeners.error({message: 'Resize failure'});
    assert.equal(b.requests.at(-1).payload.gesture, 'resize');
    b.documentListeners.pointerup();
    b.listeners.error({message: 'After resize'});
    assert.equal(b.requests.at(-1).payload.gesture, '');
    b.documentListeners.pointerdown({target: target(false)});
    b.listeners.error({message: 'Drag failure'});
    assert.equal(b.requests.at(-1).payload.gesture, 'drag');
});

test('ignores resource errors, handles hostile error objects and installs only once', () => {
    const b = browser();
    const handler = b.listeners.error;
    b.install();
    assert.equal(b.listeners.error, handler);
    b.listeners.error({target: {tagName: 'IMG'}});
    assert.equal(b.requests.length, 0);
    const reason = {get message() { throw new Error('Getter failed'); }};
    assert.doesNotThrow(() => b.listeners.unhandledrejection({reason}));
    b.listeners.error({message: 'Still working'});
    assert.equal(b.requests.length, 1);
});

test('allows explicit reporting of caught errors and truncates oversized stacks', () => {
    const b = browser();
    b.window.eisClientIncidents.report({message: 'Caught error', stack: 'x'.repeat(30000)});
    assert.equal(b.requests[0].payload.kind, 'reported');
    assert.equal(b.requests[0].payload.stack.length, 16000);
});
