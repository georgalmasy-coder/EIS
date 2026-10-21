const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {spawn} = require('node:child_process');
const output = path.resolve(__dirname, '../../../target/traceability-matrix-test');
fs.mkdirSync(output, {recursive: true});
let browser, socket;
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
async function main() {
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

    const pageSource = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/js/pages/basistraceability-page.js'), 'utf8').replace(/^import .*;\r?\n/gm, '');
    const exportSource = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/js/components/interface-xlsx-export.js'), 'utf8').replace(/^export /gm, '');
    const html = fs.readFileSync(path.resolve(__dirname, '../../main/resources/html-pages/basistraceabilitymatrix.html'), 'utf8');
    assert.match(html, /traceability-back-button[\s\S]*?<\/a>\s*<button id="traceabilityExcelButton"/);
    const helperSource = ['xml', 'css'].map(name => fs.readFileSync(path.resolve(__dirname, '../../main/webapp/js/core', name + '.js'), 'utf8').replace(/^export /gm, '')).join('\n');
    const preferencesSource = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/js/core/user-preferences.js'), 'utf8').replace(/^import .*;\r?\n/gm, '').replace(/^export /gm, '');
    const preferenceApi = `
        const preferenceSettings = {'traceability.matrix.viewMode':'colors'};
        const preferenceRequests = [];
        class XMLHttpRequest {
            open() {} setRequestHeader() {}
            send() { this.status = 200; this.responseText = JSON.stringify({settings:preferenceSettings}); }
        }
        async function postForm(url, body) {
            preferenceRequests.push({url,key:body.get('key'),value:body.get('value')});
            preferenceSettings[body.get('key')] = body.get('value');
        }
        function setText(id, value) { const node = document.getElementById(id); if (node) node.textContent = value; }
    `;
    const css = ['common', 'app', 'basistraceability'].map(name => fs.readFileSync(path.resolve(__dirname, '../../main/webapp/css', name + '.css'), 'utf8')).join('\n');
    await run(`const style = document.createElement('style'); style.textContent = ${JSON.stringify(css)}; document.head.appendChild(style);`);
    const xml = '<TraceabilityMatrixDocument><traceabilityMatrix rowCount="1" columnCount="3"><styles><style id="green" backgroundColor="#00FF00"/><style id="grayItalic" backgroundColor="#CCCCCC"/><style id="yellow" backgroundColor="#FFFF00"/></styles><columns><column index="0" id="2"/><column index="1" id="3"/><column index="2" id="4"/></columns><rows><row index="0" id="1"/></rows><cells><cell row="0" col="0" value="X" style="green"/><cell row="0" col="1" value="NR" style="grayItalic"/><cell row="0" col="2" value="" style="yellow"/></cells></traceabilityMatrix></TraceabilityMatrixDocument>';
    const result = await run(preferenceApi + '\n' + preferencesSource + '\n' + helperSource + '\n' + pageSource + '\n' + exportSource + '\n' + `(() => {
        const matrix = parseTraceabilityMatrixDocument(new DOMParser().parseFromString(${JSON.stringify(xml)}, 'application/xml'));
        injectTraceabilityStyles(matrix.styles);
        const table = document.createElement('table');
        const head = document.createElement('thead');
        const body = document.createElement('tbody');
        table.className = 'traceability-table';
        head.id = 'traceabilityTableHead';
        body.id = 'traceabilityTableBody';
        table.append(head, body);
        document.body.appendChild(table);
        document.body.insertAdjacentHTML('beforeend', ${JSON.stringify(html.match(/<button id="traceabilityViewToggle"[\s\S]*?<\/button>/)[0])} + '<div id="traceabilityLegend"></div>');
        initializeEvents();
        state.matrix = matrix;
        renderHeader(head, matrix);
        renderBody(body, matrix);
        const result = {rows:matrix.rows.length, columns:matrix.columns.length, cells:Array.from(body.querySelectorAll('td'), cell => ({text:cell.textContent, color:getComputedStyle(cell).backgroundColor})), snapshot:snapshotInterfaceMatrix(table,'Traceability Matrix','Test project')};
        const toggle = document.getElementById('traceabilityViewToggle');
        toggle.click();
        result.iconCells = Array.from(body.querySelectorAll('td'), cell => ({
            background:getComputedStyle(cell).backgroundColor,
            color:getComputedStyle(cell.querySelector('svg')).color,
            label:cell.querySelector('svg').getAttribute('aria-label')
        }));
        result.iconSnapshot = snapshotInterfaceMatrix(table,'Traceability Matrix','Test project');
        result.iconLegendCount = document.querySelectorAll('#traceabilityLegend .traceability-status-icon').length;
        result.pressed = toggle.getAttribute('aria-pressed');
        result.contextActions = getTraceabilityCellActions({style:'green'}).map(action => action.action);
        // Reload preferences from the mocked API to verify the persisted selection.
        values.clear(); loaded = false; loadSync();
        result.restored = userPreferences.getItem(VIEW_MODE_KEY);
        toggle.click();
        result.legacyIcons = body.querySelectorAll('.traceability-status-icon').length;
        result.legacyColors = Array.from(body.querySelectorAll('td'), cell => getComputedStyle(cell).backgroundColor);
        result.preferenceRequests = preferenceRequests;
        return result;
    })()`);
    assert.equal(result.rows, 1);
    assert.equal(result.columns, 3);
    assert.deepEqual(result.cells, [
        {text:'', color:'rgb(0, 255, 0)'},
        {text:'', color:'rgb(204, 204, 204)'},
        {text:'', color:'rgb(255, 255, 0)'}
    ]);
    assert.equal(result.snapshot.widths.length, 5);
    assert.equal(result.snapshot.cells.find(cell => cell.row === 1 && cell.column === 0).rotation, 90);
    assert.deepEqual(result.snapshot.cells.filter(cell => cell.row === 1 && cell.column >= 2).map(cell => [cell.text, cell.background]), [['', '00FF00'], ['', 'CCCCCC'], ['', 'FFFF00']]);
    fs.writeFileSync(path.join(output, 'snapshot.json'), JSON.stringify(result.snapshot));
    assert.deepEqual(result.iconCells, [
        {background:'rgb(255, 255, 255)',color:'rgb(40, 122, 69)',label:'Confirmed relations'},
        {background:'rgb(255, 255, 255)',color:'rgb(98, 102, 107)',label:'Not relevant'},
        {background:'rgb(255, 255, 255)',color:'rgb(153, 102, 0)',label:'Possible relations'}
    ]);
    assert.equal(result.iconLegendCount, 3);
    assert.equal(result.pressed, 'true');
    assert.equal(result.restored, 'icons');
    assert.equal(result.legacyIcons, 0);
    assert.deepEqual(result.legacyColors,result.cells.map(cell => cell.color));
    assert.equal(result.preferenceRequests[0].url, '/api/user-preferences');
    assert.deepEqual(result.preferenceRequests.map(request => request.value), ['icons','colors']);
    assert.ok(result.contextActions.includes('removeConfirmedRelation'));
    assert.deepEqual(result.iconSnapshot.cells.filter(cell => cell.row === 1 && cell.column >= 2).map(cell => [cell.text, cell.background, cell.foreground]), [['✓','FFFFFF','287A45'],['⊘','FFFFFF','62666B'],['?','FFFFFF','996600']]);
    fs.writeFileSync(path.join(output, 'icon-snapshot.json'), JSON.stringify(result.iconSnapshot));
    console.log('Passed: icon/color toggle, exact colors, white backgrounds, legend, persisted user preference, context actions and both XLSX snapshots.');
}
main().catch(error => { console.error(error); process.exitCode = 1; }).finally(() => {socket?.close(); browser?.kill();});
