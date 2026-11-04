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
    const viewSource = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/js/components/traceability-matrix-view.js'), 'utf8').replace(/^import .*;\r?\n/gm, '').replace(/^export /gm, '');
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
    const result = await run(preferenceApi + '\n' + preferencesSource + '\n' + helperSource + '\n' + viewSource + '\n' + pageSource + '\n' + exportSource + '\n' + `(async () => {
        const matrix = parseTraceabilityMatrixDocument(new DOMParser().parseFromString(${JSON.stringify(xml)}, 'application/xml'));
        injectTraceabilityStyles(matrix.styles);
        const scroll = document.createElement('div');
        scroll.id = 'traceabilityTableScroll';
        scroll.className = 'traceability-table-scroll';
        scroll.style.cssText = 'width:1000px;height:500px;flex:none';
        const body = document.createElement('div');
        body.id = 'traceabilityGrid';
        scroll.appendChild(body);
        document.body.appendChild(scroll);
        document.body.insertAdjacentHTML('beforeend', ${JSON.stringify(html.match(/<button id="traceabilityViewToggle"[\s\S]*?<\/button>/)[0])} + '<div id="traceabilityLegend"></div><button id="traceabilityExcelButton"></button>');
        initializeEvents();
        state.matrix = matrix;
        renderTraceabilityMatrix(matrix);
        const result = {rows:matrix.rows.length, columns:matrix.columns.length, cells:Array.from(body.querySelectorAll('.traceability-cell'), cell => ({text:cell.textContent, color:getComputedStyle(cell).backgroundColor})), snapshot:await snapshotTraceabilityMatrix(matrix)};
        const toggle = document.getElementById('traceabilityViewToggle');
        toggle.click();
        result.iconCells = Array.from(body.querySelectorAll('.traceability-cell'), cell => ({
            background:getComputedStyle(cell).backgroundColor,
            color:getComputedStyle(cell.querySelector('svg')).color,
            label:cell.querySelector('svg').getAttribute('aria-label')
        }));
        result.iconSnapshot = await snapshotTraceabilityMatrix(matrix);
        result.iconLegendCount = document.querySelectorAll('#traceabilityLegend .traceability-status-icon').length;
        result.pressed = toggle.getAttribute('aria-pressed');
        result.contextActions = getTraceabilityCellActions({style:'green'}).map(action => action.action);
        // Reload preferences from the mocked API to verify the persisted selection.
        values.clear(); loaded = false; loadSync();
        result.restored = userPreferences.getItem(VIEW_MODE_KEY);
        toggle.click();
        result.legacyIcons = body.querySelectorAll('.traceability-status-icon').length;
        result.legacyColors = Array.from(body.querySelectorAll('.traceability-cell'), cell => getComputedStyle(cell).backgroundColor);
        result.preferenceRequests = preferenceRequests;
        let exportWork = 0;
        window.confirm = () => false;
        window.alert = () => {};
        snapshotTraceabilityMatrix = async () => {exportWork++; throw Error('Unexpected export work');};
        const oversized = {...matrix, rows:Array(151).fill(matrix.rows[0]), columns:Array(150).fill(matrix.columns[0])};
        state.matrix = oversized;
        updateTraceabilityExportButton(oversized);
        const exportButton = document.getElementById('traceabilityExcelButton');
        result.exportDisabled = exportButton.disabled;
        exportButton.dispatchEvent(new MouseEvent('click', {bubbles:true}));
        result.cancelledExportWork = exportWork;
        window.confirm = () => true;
        exportButton.dispatchEvent(new MouseEvent('click', {bubbles:true}));
        await new Promise(resolve=>setTimeout(resolve,0));
        result.acceptedExportWork = exportWork;
        exportWork = 0;
        state.matrix = {...oversized, rows:Array(1000).fill(matrix.rows[0]), columns:Array(1000).fill(matrix.columns[0])};
        // Even a synthetic click on an enabled button must respect the data limit.
        exportButton.disabled = false;
        exportButton.dispatchEvent(new MouseEvent('click', {bubbles:true}));
        result.oversizedExportWork = exportWork;
        return result;
    })()`);
    assert.equal(result.rows, 1);
    assert.equal(result.columns, 3);
    assert.equal(result.exportDisabled, false);
    assert.equal(result.cancelledExportWork, 0);
    assert.equal(result.acceptedExportWork, 1);
    assert.equal(result.oversizedExportWork, 0);
    const blockSource = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/js/components/traceability-block-loader.js'), 'utf8').replace(/^export /gm, '');
    const headerXml = xml.replace('<traceabilityMatrix ', '<traceabilityMatrix snapshotId="test-snapshot" blockSize="2" ')
        .replace(/<cells>[\s\S]*?<\/cells>/, '<cells/>');
    const lazy = await run(blockSource + '\n' + `(async () => {
        window.applyPageHeaderFromDocument = () => {};
        const requests = [], pending = [];
        window.fetch = async (url, options) => {
            requests.push({url,method:options.method});
            if(url.includes('cmd=snapshot')) return {ok:true,text:async()=>${JSON.stringify(headerXml)}};
            if(url.includes('cmd=block')) return await new Promise(resolve=>pending.push({resolve,r:Number(options.body.get('rowStart')),c:Number(options.body.get('columnStart'))}));
            throw Error('unexpected URL '+url);
        };
        await loadTraceabilityMatrix();
        clearContextState();
        const unloaded = document.querySelector('.traceability-cell');
        const background = getComputedStyle(unloaded).backgroundColor;
        const colorHourglass = !!unloaded.querySelector('svg.traceability-loading-icon[aria-label="Loading"]');
        const busy = unloaded.getAttribute('aria-busy');
        document.getElementById('traceabilityViewToggle').click();
        const iconHourglass = !!document.querySelector('.traceability-cell svg.traceability-loading-icon');
        document.getElementById('traceabilityViewToggle').click();
        unloaded.dispatchEvent(new MouseEvent('contextmenu',{bubbles:true,cancelable:true}));
        const blocked = state.contextCell === null;
        const before = state.matrix.loadedBlocks.size;
        for(const item of pending) item.resolve({ok:true,json:async()=>({snapshotId:'test-snapshot',rowStart:item.r,columnStart:item.c,
            rowEnd:1,columnEnd:Math.min(item.c+2,3),cells:item.c===0?[{rowIndex:0,columnIndex:0,value:'X',style:'green'}]:[]})});
        await new Promise(resolve=>setTimeout(resolve,60));
        const after = state.matrix.loadedBlocks.size;
        const first = getTraceabilityCell(state.matrix,state.matrix.rows[0],state.matrix.columns[0]);
        const empty = getTraceabilityCell(state.matrix,state.matrix.rows[0],state.matrix.columns[2]);
        const color = getComputedStyle(document.querySelector('.traceability-cell')).backgroundColor;
        const sameMatrix=state.matrix, scroll=document.getElementById('traceabilityTableScroll');
        const scrollBefore=[scroll.scrollTop,scroll.scrollLeft];
        let actionSnapshot;
        window.fetch=async (url,options)=>{
            requests.push({url,method:options.method});
            if(!url.includes('relationnotrelevant'))throw Error('relation update unexpectedly reloaded the matrix');
            actionSnapshot=options.body.get('snapshotId');
            return {ok:true,json:async()=>({snapshotId:'updated-snapshot',cell:{rowIndex:0,columnIndex:0,value:'NR',style:'grayItalic',rowStyle:'normal',columnStyle:'normal'}})};
        };
        const r=sameMatrix.rows[0],c=sameMatrix.columns[0];
        await executeTraceabilityRelationAction('/basis/basistraceability/relationnotrelevant',r,c,first,document.querySelector('.traceability-cell'));
        const action={sameMatrix:state.matrix===sameMatrix,snapshot:state.matrix.snapshotId,cell:getTraceabilityCell(state.matrix,r,c).style,
            rowStyle:r.style,columnStyle:c.style,loadedBlocks:state.matrix.loadedBlocks.size,actionSnapshot,
            scrollUnchanged:scrollBefore[0]===scroll.scrollTop&&scrollBefore[1]===scroll.scrollLeft};
        blockLoader.dispose();
        return {action,background,colorHourglass,iconHourglass,busy,remainingHourglasses:document.querySelectorAll('.traceability-loading-icon').length,blocked,before,after,first:first.style,empty:empty.style,emptyLoaded:!empty.loading,color,requests};
    })()`);
    assert.equal(lazy.background, 'rgb(255, 255, 255)');
    assert.equal(lazy.colorHourglass, true);
    assert.equal(lazy.iconHourglass, true);
    assert.equal(lazy.busy, 'true');
    assert.equal(lazy.remainingHourglasses, 0);
    assert.equal(lazy.blocked, true);
    assert.equal(lazy.before, 0);
    assert.equal(lazy.after, 2);
    assert.equal(lazy.first, 'green');
    assert.equal(lazy.empty, 'normal');
    assert.equal(lazy.emptyLoaded, true);
    assert.equal(lazy.color, 'rgb(0, 255, 0)');
    assert.deepEqual(lazy.action,{sameMatrix:true,snapshot:'updated-snapshot',cell:'grayItalic',rowStyle:'normal',columnStyle:'normal',loadedBlocks:2,actionSnapshot:'test-snapshot',scrollUnchanged:true});
    assert.ok(lazy.requests.every(request => request.method === 'POST'));
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
    console.log('Passed: icon/color toggle, POST snapshot/block loading, hourglass loading cells, sparse empty blocks, blocked unloaded actions, export confirmation/cancellation/limit, preferences and both XLSX snapshots.');
}
main().catch(error => { console.error(error); process.exitCode = 1; }).finally(() => {socket?.close(); browser?.kill();});
