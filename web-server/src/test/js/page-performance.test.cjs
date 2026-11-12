const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const vm = require('node:vm');
const {test} = require('node:test');
const source = readFileSync(new URL('../../main/webapp/js/core/page-performance.js', `file://${__filename.replaceAll('\\', '/')}`), 'utf8');

function browser(page = 'systemrequirement-main', click = null, type = 'navigate') {
    const listeners = {}, frames = [], requests = [];
    const status = {textContent: 'Loading'};
    const url = `https://eis.test/web/view?page=${page}`;
    let cellsLoading = false, observe, disconnected = false;
    const storage = new Map(click ? [['eis.pagePerformance.navigation', JSON.stringify({url, time: click})]] : []);
    const document = {
        documentElement: {},
        getElementById: () => status,
        querySelector: () => cellsLoading ? {} : null,
        addEventListener: (name, callback) => { listeners[name] = callback; }
    };
    vm.runInNewContext(source, {
        document, location: new URL(url), URL, URLSearchParams,
        performance: {timeOrigin: 10000, now: () => 450, getEntriesByType: () => [{type}]},
        sessionStorage: {getItem: key => storage.get(key), removeItem: key => storage.delete(key), setItem: (key, value) => storage.set(key, value)},
        MutationObserver: class { constructor(callback) { observe = callback; } observe() {} disconnect() { disconnected = true; } },
        requestAnimationFrame: callback => frames.push(callback),
        fetch: (url, options) => { requests.push({url, options}); return Promise.resolve(); }
    });
    return {
        requests, status, listeners,
        change() { observe?.(); },
        frame() { frames.splice(0).forEach(callback => callback()); },
        cells(value) { cellsLoading = value; },
        get disconnected() { return disconnected; }
    };
}

test('waits for loaded data and two frames, then reports only once', () => {
    const b = browser();
    b.change(); b.frame(); b.frame();
    assert.equal(b.requests.length, 0);
    b.status.textContent = 'Loaded'; b.change(); b.frame();
    assert.equal(b.requests.length, 0);
    b.frame();
    assert.equal(b.requests.length, 1);
    assert.equal(b.requests[0].options.body.get('durationMs'), '450');
    assert.equal(b.requests[0].options.body.get('page'), 'systemrequirement-main');
    assert.equal(b.requests[0].url, '/api/performance/frontend');
    assert.equal(b.disconnected, true);
    b.change(); b.frame(); b.frame();
    assert.equal(b.requests.length, 1);
});

test('waits for initial matrix cells, including cells rendered during a frame', () => {
    const b = browser();
    b.status.textContent = 'Loaded'; b.change(); b.frame();
    b.cells(true); b.frame();
    assert.equal(b.requests.length, 0);
    b.cells(false); b.change(); b.frame(); b.frame();
    assert.equal(b.requests.length, 1);
});

test('does not measure the admin dashboard', () => {
    const b = browser('admindashboard');
    b.status.textContent = 'Loaded'; b.change(); b.frame(); b.frame();
    assert.equal(b.requests.length, 0);
});

test('includes time from the originating link click', () => {
    const b = browser('project-main', 9900);
    b.status.textContent = 'Loaded'; b.change(); b.frame(); b.frame();
    assert.equal(b.requests[0].options.body.get('durationMs'), '550');
});

test('reloads ignore a stored click and use navigation start', () => {
    const b = browser('project-main', 9900, 'reload');
    b.status.textContent = 'Idle'; b.change(); b.frame(); b.frame();
    assert.equal(b.requests[0].options.body.get('durationMs'), '450');
});

test('explicit readiness supports settings pages; errors are not successes', () => {
    const b = browser('projectsettings');
    b.status.textContent = 'Error'; b.change(); b.frame(); b.frame();
    assert.equal(b.requests.length, 0);
    b.listeners['eis:initial-view-ready'](); b.frame(); b.frame();
    assert.equal(b.requests.length, 1);
});
