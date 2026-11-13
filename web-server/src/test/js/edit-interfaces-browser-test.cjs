const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const {spawn} = require('node:child_process');

const root = path.resolve(__dirname, '../../main');
const output = path.resolve(__dirname, '../../../target/edit-interfaces-test');
fs.mkdirSync(output, {recursive: true});
const html = fs.readFileSync(path.join(root, 'resources/html-pages/systemsbreakdown-edit.html'), 'utf8')
    .replace(/<script\b[^>]*>[\s\S]*?<\/script>/g, '');
const server = http.createServer((req, res) => {
    if (req.url.startsWith('/css/') || req.url.startsWith('/js/')) {
        res.setHeader('Content-Type', req.url.startsWith('/js/') ? 'text/javascript' : 'text/css');
        res.end(fs.readFileSync(path.join(root, 'webapp', req.url.split('?')[0])));
    } else {
        res.setHeader('Content-Type', 'text/html');
        res.end(html);
    }
});
let browser, socket;
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));

async function main() {
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
    const profile = fs.mkdtempSync(path.join(output, 'profile-'));
    browser = spawn(process.env.EIS_TEST_BROWSER || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
        ['--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check', '--remote-debugging-port=0', `--user-data-dir=${profile}`, 'about:blank'],
        {windowsHide: true, stdio: 'ignore'});
    const active = path.join(profile, 'DevToolsActivePort');
    for (let i = 0; i < 200 && !fs.existsSync(active); i++) await pause(50);
    const port = fs.readFileSync(active, 'utf8').split('\n')[0];
    const pages = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
    socket = new WebSocket(pages.find(page => page.type === 'page').webSocketDebuggerUrl);
    await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
    let sequence = 0;
    const pending = new Map();
    socket.onmessage = event => {
        const reply = JSON.parse(event.data);
        if (!reply.id) return;
        const {resolve, reject} = pending.get(reply.id);
        pending.delete(reply.id);
        reply.error ? reject(new Error(reply.error.message)) : resolve(reply.result);
    };
    const send = (method, params = {}) => new Promise((resolve, reject) => {
        const id = ++sequence;
        pending.set(id, {resolve, reject});
        socket.send(JSON.stringify({id, method, params}));
    });
    const run = async expression => {
        const result = await send('Runtime.evaluate', {expression, returnByValue: true, awaitPromise: true});
        if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails));
        return result.result.value;
    };
    await send('Emulation.setDeviceMetricsOverride', {width: 1600, height: 900, deviceScaleFactor: 1, mobile: false});
    await send('Page.navigate', {url: `http://127.0.0.1:${server.address().port}/`});
    await pause(500);
    const result = await run(`(async () => {
        const {createEditInterfacesTable} = await import('/js/components/edit-interfaces-table.js');
        const check = (ok, message) => {if (!ok) throw new Error(message);};
        document.querySelectorAll('.tab-panel').forEach(panel => panel.style.display = 'none');
        document.getElementById('tabPanel7').style.display = 'flex';
        const xml = new DOMParser().parseFromString('<SystemBreakdownInfo><systembreakdowns><systembreakdown><EntityId>10</EntityId><SBSCode>A</SBSCode><SystemName>Current</SystemName></systembreakdown></systembreakdowns><editInterfaces entityId="10" entityType="2"><entities><entity><entityId>10</entityId><id>A</id><name>Current</name></entity><entity><entityId>20</entityId><id>B</id><name>Other</name></entity><entity><entityId>30</entityId><id>C</id><name>Third</name></entity></entities><leftInterfaces><interface><fromEntityId>10</fromEntityId><toEntityId>20</toEntityId><toEntityCode>B</toEntityCode><toEntityName>Other</toEntityName><fromIrlId>7</fromIrlId></interface></leftInterfaces><rightInterfaces><interface><fromEntityId>30</fromEntityId><toEntityId>10</toEntityId><fromEntityCode>C</fromEntityCode><fromEntityName>Third</fromEntityName><fromIrlId>7</fromIrlId></interface></rightInterfaces><irlMeta><irl irlId="7" code="IRL 7"/></irlMeta><classificationMeta/></editInterfaces></SystemBreakdownInfo>', 'application/xml');
        let saved;
        const component = createEditInterfacesTable({onSaved: draft => {saved = draft;}});
        component.loadFromDocument(xml);
        const addTo = document.getElementById('addInterfaceToButton');
        const addFrom = document.getElementById('addInterfaceFromButton');
        check(!addTo.hidden && !addFrom.hidden, 'buttons hidden');
        for (const button of [addTo, addFrom]) {
            const footer = button.parentElement.getBoundingClientRect();
            const rect = button.getBoundingClientRect();
            check(rect.width > 0 && Math.abs(footer.right - rect.right - 10) < 2, 'button must be right aligned');
        }
        const requests = [];
        window.fetch = async (url, options) => {requests.push({url, body: options.body});return {ok:true};};
        addTo.click();
        check(document.getElementById('interfaceDialog').open, 'create dialog not opened');
        check(document.getElementById('interfaceDialogTitle').textContent === 'Create Interface', 'create title');
        check(document.getElementById('interfaceFromFields').textContent.includes('Current'), 'fixed source missing');
        let select = document.getElementById('interfaceDialogEntitySelect');
        check(Array.from(select.options).map(option => option.value).join(',') === ',30', 'outgoing choices include self or existing target');
        check(document.getElementById('interfaceDialogSaveButton').disabled, 'save enabled without target');
        select.value = '30';select.dispatchEvent(new Event('change'));
        document.getElementById('interfaceDialogIrlSelect').value = '7';
        document.getElementById('interfaceDialogSaveButton').click();
        await new Promise(resolve => setTimeout(resolve, 20));
        check(saved.fromEntityId === '10' && saved.toEntityId === '30', 'outgoing saved direction');
        check(requests[0].url.includes('createOnly=true'), 'create must not update existing interface');
        addFrom.click();
        check(document.getElementById('interfaceToFields').textContent.includes('Current'), 'fixed target missing');
        select = document.getElementById('interfaceDialogEntitySelect');
        check(Array.from(select.options).map(option => option.value).join(',') === ',20', 'incoming choices include self or existing source');
        select.value = '20';select.dispatchEvent(new Event('change'));
        document.getElementById('interfaceDialogIrlSelect').value = '7';
        document.getElementById('interfaceDialogSaveButton').click();
        await new Promise(resolve => setTimeout(resolve, 20));
        check(saved.fromEntityId === '20' && saved.toEntityId === '10', 'incoming saved direction');
        document.querySelector('#rightInterfacesBody tr').dispatchEvent(new MouseEvent('dblclick', {bubbles:true}));
        check(document.getElementById('interfaceDialogTitle').textContent === 'Edit Interface', 'edit title');
        check(!document.getElementById('interfaceDialogEntitySelect'), 'selector leaked into editing');
        check(document.getElementById('interfaceFromFields').textContent.includes('Third'), 'incoming edit source');
        check(document.getElementById('interfaceToFields').textContent.includes('Current'), 'incoming edit target');
        document.getElementById('interfaceDialogSaveButton').click();
        await new Promise(resolve => setTimeout(resolve, 20));
        check(saved.fromEntityId === '30' && saved.toEntityId === '10', 'incoming edit saved direction');
        check(!requests[2].url.includes('createOnly'), 'edit must allow updating');
        const {openEditDialog} = await import('/js/components/edit-dialog.js?rev=interface-refresh-2026-11-13');
        let refresh;
        const entityDialog = openEditDialog({page:'systemsbreakdown-edit', mode:'edit', id:'10',
            onRequestRefresh: payload => {refresh = payload;}});
        await new Promise(resolve => entityDialog.iframe.addEventListener('load', resolve, {once:true}));
        const bridge = await entityDialog.iframe.contentWindow.eval("import('/js/components/edit-dialog-bridge.js?rev=interface-refresh-2026-11-13')");
        check(bridge.requestEditDialogRefresh({kind:'interface', fromEntityId:'10', toEntityId:'20'}), 'iframe did not send refresh');
        await new Promise(resolve => setTimeout(resolve, 30));
        check(refresh?.fromEntityId === '10' && refresh?.toEntityId === '20', 'parent did not receive interface pair');
        check(entityDialog.dialog.open, 'interface refresh closed entity dialog');
        entityDialog.close();
        return {creation:'both directions passed', editing:'passed', buttonAlignment:'passed', parentRefresh:'passed'};
    })()`);
    console.log('Edit interfaces browser test:', JSON.stringify(result));
}
main().catch(error=>{console.error(error);process.exitCode=1;}).finally(()=>{socket?.close();browser?.kill();server.close();});
