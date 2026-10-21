const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {spawn} = require('node:child_process');
const output = path.resolve(__dirname, '../../../target/interface-xlsx-test');
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

    for (const name of ['psys', 'lsys', 'fsys', 'stk', 'sys']) {
        const html = fs.readFileSync(path.resolve(__dirname, '../../main/resources/html-pages', name + '-interfaces.html'), 'utf8');
        assert.match(html, /interfaces-back-button[\s\S]*?<\/a>\s*<button id="interfacesExcelButton"/);
    }
    const read = file => fs.readFileSync(path.resolve(__dirname, '../../main/webapp/js', file), 'utf8').replace(/^import .*;\r?\n/gm, '').replace(/^export /gm, '');
    const css = ['common', 'app', 'interfaces'].map(name => fs.readFileSync(path.resolve(__dirname, '../../main/webapp/css', name + '.css'), 'utf8')).join('\n');
    await run(`const style = document.createElement('style'); style.textContent = ${JSON.stringify(css)}; document.head.appendChild(style);`);
    const result = await run(read('core/xml.js') + '\n' + read('pages/interfaces-page.js') + '\n' + read('components/interface-xlsx-export.js') + '\n' + read('components/interface-flow-arrows.js') + '\n' + `(() => {
        document.body.innerHTML = '<table class="interfaces-table"><thead id="interfacesTableHead"></thead><tbody id="interfacesTableBody"></tbody></table>';
        const table = document.querySelector('table');
        const matrix = {rowGroupLabel:'Physical Architecture', structures:[
            {entityId:'1',id:'P-1',name:'Køling & strøm'}, {entityId:'2',id:'P-2',name:'=Safe string'}, {entityId:'3',id:'P-3',name:'Filtered out'}
        ], lookup:{irlById:new Map(), classificationById:new Map()}, cellsByKey:new Map()};
        const cell = {fromEntityId:'1',toEntityId:'2',irlId:'4',irlCode:'IRL 4',irlColor:'#9dcca8',classificationIds:[],classificationCodes:['Power'],nextIrlMeeting:'2020-01-02'};
        matrix.cellsByKey.set(buildCellKey('1','2'),cell);
        matrix.cellsByKey.set(buildCellKey('2','3'),{...cell,fromEntityId:'2',toEntityId:'3',irlId:'5'});
        const view = buildVisibleMatrix(matrix, true, ['4']);
        const groups = buildHeaderGroups(view.columnStructures);
        renderHeader(table.tHead,view,groups);
        renderBody(table.tBodies[0],view,groups);
        const beforeHover = snapshotInterfaceMatrix(table,'N² - Physical Architecture','Projekt æøå');
        installInterfaceFlowArrows(table.tBodies[0]);
        const active = table.querySelector('td[data-from-entity-id="1"][data-to-entity-id="2"]');
        active.dispatchEvent(new MouseEvent('mouseover', {bubbles:true}));
        if (!table.querySelector('.interfaces-flow-arrow')) throw new Error('Hover arrow was not drawn');
        const afterHover = snapshotInterfaceMatrix(table,'N² - Physical Architecture','Projekt æøå');
        if (JSON.stringify(beforeHover) !== JSON.stringify(afterHover)) throw new Error('Hover arrow changed export');
        return snapshotInterfaceMatrix(table,'N² - Physical Architecture','Projekt æøå');
    })()`);
    assert.equal(result.widths.length,4);
    assert.equal(result.heights.length,3);
    assert.equal(result.cells.find(cell => cell.row === 1 && cell.column === 0).rowSpan,2);
    assert.ok(result.cells.some(cell => cell.text === 'P-1\nKøling & strøm'));
    assert.ok(result.cells.some(cell => cell.text.includes('IRL: IRL 4') && cell.background === '9DCCA8' && cell.text.includes('Overdue')));
    assert.ok(result.cells.some(cell => cell.text === 'P-2\n=Safe string' && cell.background === 'E5E7EB'));
    assert.ok(result.cells.every(cell => !cell.text.includes('Filtered out') && !cell.text.includes('<svg')));
    fs.writeFileSync(path.join(output,'snapshot.json'),JSON.stringify(result));
    console.log('Passed: all five buttons follow Back; filtered N2 snapshot preserves text, colors and merged axis without hover arrows.');
}
main().catch(error => { console.error(error); process.exitCode = 1; }).finally(() => {socket?.close(); browser?.kill();});
