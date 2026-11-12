const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/js/pages/allprojects-page.js'), 'utf8')
    .replace(/^import .*;\r?\n/gm, '');
async function test() {
    const buttons = [], requests = [], alerts = [];
    let complete;
    const list = {innerHTML: '', appendChild() {}};
    const context = vm.createContext({URLSearchParams,
        console: {error() {}},
        window: {location: {href: ''}, alert: value => alerts.push(value)},
        document: {
            addEventListener() {},
            getElementById: () => list,
            createElement() {
                const button = {disabled: false, addEventListener(event, handler) {this.click = handler;}};
                buttons.push(button);
                return {innerHTML: '', querySelector: () => button};
            }
        },
        fetch: (url, options) => {
            requests.push({url, options});
            return new Promise(resolve => {complete = resolve;});
        }
    });
    vm.runInContext(source + '\nallProjects = [{id:"6",pk:"812",name:"Project six",owner:"Owner",statusCode:"ACTIVE"}];renderProjects();', context);
    const pending = buttons[0].click();
    assert.equal(requests[0].url, '/Menu');
    assert.equal(requests[0].options.method, 'POST');
    assert.equal(requests[0].options.body.get('cmd'), 'selectproject');
    assert.equal(requests[0].options.body.get('projectId'), '6');
    assert.equal(requests[0].options.credentials, 'same-origin');
    assert.equal(context.window.location.href, '', 'must await the saved selection');
    await buttons[0].click();
    assert.equal(requests.length, 1, 'repeated clicks must not race');
    complete({ok: true}); await pending;
    assert.equal(context.window.location.href, '/web/view?page=projectoverview');

    context.window.location.href = '';
    vm.runInContext('renderProjects()', context);
    const failed = buttons[1].click();
    complete({ok: false, status: 500}); await failed;
    assert.equal(context.window.location.href, '', 'failed selection must not navigate');
    assert.equal(buttons[1].disabled, false, 'failed selection must allow retry');
    assert.equal(alerts.length, 1);
    console.log('PASS project Open: selected project ID, shared menu endpoint, save before navigation, repeated clicks and failed selection');
}
test().catch(error => {console.error(error); process.exitCode = 1;});
