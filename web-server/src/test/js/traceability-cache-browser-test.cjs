const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const {spawn} = require('node:child_process');
const root = path.resolve(__dirname, '../../main');
const output = path.resolve(__dirname, '../../../target/traceability-cache-test');
fs.mkdirSync(output, {recursive:true});
const read = file => fs.readFileSync(path.join(root,file),'utf8');
// Reproduce the previous renderer's contract: sparse missing cells are empty,
// and the renderer has no viewport callback to trigger block requests.
const currentView = read('webapp/js/components/traceability-matrix-view.js');
const oldView = currentView.replace(/    if \(matrix.snapshotId[\s\S]*?\n    \}\r?\n/, '')
    .replace('        onRangeChange?.({rowStart, rowEnd, colStart, colEnd});','');
const stubs = {
    '/js/components/menu.js':'export function initMenu(){}',
    '/js/core/user-preferences.js':"export const userPreferences={getItem:()=> 'colors',setItem(){}};",
    '/js/components/help-dialog.js':'export function initHelpDialog(){}',
    '/js/components/topbar.js':'export function mountTopbar(){}',
    '/js/components/edit-dialog.js':'export function openEditDialog(){}',
    '/js/components/traceability-word-highlights.js':'export function highlightMatchingWords(){}',
    '/js/core/page-header.js':'export function applyTopPanelFromDocument(){}'
};
const header = '<TraceabilityMatrixDocument><traceabilityMatrix snapshotId="s1" blockSize="32" rowCount="1" columnCount="1" defaultCellStyle="normal" defaultCellValue=""><styles><style id="green" backgroundColor="#00FF00"/></styles><rows><row index="0" id="1"/></rows><columns><column index="0" id="2"/></columns><cells/></traceabilityMatrix></TraceabilityMatrixDocument>';
let blocks=0, release, legacyViewRequests=0, versionedViewRequests=0;
const server = http.createServer((req,res)=>{
    const url=new URL(req.url,'http://localhost');
    if(url.pathname==='/basis/basistraceability') {
        res.setHeader('Cache-Control','no-store');
        if(url.searchParams.get('cmd')==='snapshot') {res.setHeader('Content-Type','application/xml');res.end(header);return;}
        if(url.searchParams.get('cmd')==='block') {
            blocks++;release=()=>{res.setHeader('Content-Type','application/json');res.end(JSON.stringify({snapshotId:'s1',rowStart:0,columnStart:0,rowEnd:1,columnEnd:1,cells:[{rowIndex:0,columnIndex:0,value:'X',style:'green'}]}));};return;
        }
    }
    if(url.pathname==='/web/view') {
        let html=read('resources/html-pages/basistraceabilitymatrix.html');
        if(url.searchParams.get('legacy')) html=html.replaceAll('?rev=traceability-blocks-4','?rev=legacy-test');
        res.setHeader('Content-Type','text/html');res.end(html);return;
    }
    if(url.pathname==='/js/components/traceability-matrix-view.js') {
        res.setHeader('Content-Type','text/javascript');res.setHeader('Cache-Control','public,max-age=3600');
        if(!url.search) {legacyViewRequests++;res.end(oldView);} else {versionedViewRequests++;res.end(currentView);} return;
    }
    if(url.pathname==='/js/pages/basistraceability-page.js') {
        let source=read('webapp'+url.pathname);
        if(url.searchParams.get('rev')==='legacy-test') source=source.replaceAll('?rev=traceability-blocks-4','');
        res.setHeader('Content-Type','text/javascript');res.end(source);return;
    }
    if(stubs[url.pathname]) {res.setHeader('Content-Type','text/javascript');res.end(stubs[url.pathname]);return;}
    const file=path.join(root,'webapp',url.pathname);
    if(fs.existsSync(file)&&fs.statSync(file).isFile()) {
        res.setHeader('Content-Type',url.pathname.endsWith('.css')?'text/css':'text/javascript');res.end(fs.readFileSync(file));return;
    }
    res.statusCode=404;res.end();
});
let browser,socket;
const pause=ms=>new Promise(resolve=>setTimeout(resolve,ms));
async function main(){
    await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
    const profile=fs.mkdtempSync(path.join(output,'profile-'));
    browser=spawn(process.env.EIS_TEST_BROWSER||'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
        ['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port=0',`--user-data-dir=${profile}`,'about:blank'],{windowsHide:true,stdio:'ignore'});
    const active=path.join(profile,'DevToolsActivePort');
    for(let i=0;i<200&&!fs.existsSync(active);i++)await pause(50);
    const port=fs.readFileSync(active,'utf8').split('\n')[0];
    const pages=await(await fetch(`http://127.0.0.1:${port}/json/list`)).json();
    socket=new WebSocket(pages.find(page=>page.type==='page').webSocketDebuggerUrl);
    await new Promise((resolve,reject)=>{socket.onopen=resolve;socket.onerror=reject;});
    let sequence=0;const pending=new Map();
    socket.onmessage=event=>{const reply=JSON.parse(event.data);if(!reply.id)return;const job=pending.get(reply.id);pending.delete(reply.id);reply.error?job.reject(reply.error):job.resolve(reply.result);};
    const send=(method,params={})=>new Promise((resolve,reject)=>{const id=++sequence;pending.set(id,{resolve,reject});socket.send(JSON.stringify({id,method,params}));});
    const run=async expression=>{const result=await send('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(result.exceptionDetails)throw Error(JSON.stringify(result.exceptionDetails));return result.result.value;};
    await send('Emulation.setDeviceMetricsOverride',{width:1400,height:800,deviceScaleFactor:1,mobile:false});
    const base=`http://127.0.0.1:${server.address().port}`;
    async function waitFor(expression){for(let i=0;i<100;i++){if(await run(expression))return;await pause(50);}throw Error('Timed out: '+expression);}
    // Load the new page with an old, cacheable component: exact reported symptom.
    await send('Page.navigate',{url:base+'/web/view?legacy=1'});
    await waitFor("!!document.querySelector('.traceability-cell')");
    assert.equal(await run("getComputedStyle(document.querySelector('.traceability-cell')).backgroundColor"),'rgb(255, 255, 255)');
    await pause(100);assert.equal(blocks,0);
    assert.equal(legacyViewRequests,1);
    // Same browser/cache; the revised entry point must request the revised component.
    await send('Page.navigate',{url:base+'/web/view?page=basistraceabilitymatrix'});
    await waitFor("!!document.querySelector('.traceability-cell.is-loading')");
    assert.equal(await run("getComputedStyle(document.querySelector('.traceability-cell')).backgroundColor"),'rgb(255, 255, 255)');
    assert.equal(await run("!!document.querySelector('.traceability-cell .traceability-loading-icon[aria-label=Loading]')"),true);
    assert.equal(blocks,1);assert.equal(versionedViewRequests,1);
    release();
    await waitFor("!!document.querySelector('.traceability-cell:not(.is-loading)')");
    assert.equal(await run("getComputedStyle(document.querySelector('.traceability-cell')).backgroundColor"),'rgb(0, 255, 0)');
    assert.equal(await run("document.querySelectorAll('.traceability-loading-icon').length"),0);
    assert.equal(legacyViewRequests,1);
    console.log('Passed: reproduced white cells/no block requests with stale renderer; versioned actual ES modules display hourglasses, request block and render green using the same browser cache.');
}
main().catch(error=>{console.error(error);process.exitCode=1;}).finally(()=>{socket?.close();browser?.kill();server.close();});
