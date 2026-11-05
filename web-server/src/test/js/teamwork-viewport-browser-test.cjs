const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const {spawn} = require('node:child_process');

const root = path.resolve(__dirname, '../../main');
const output = path.resolve(__dirname, '../../../target/teamwork-viewport-test');
fs.mkdirSync(output, {recursive: true});
const html = fs.readFileSync(path.join(root, 'resources/html-pages/dashboard-systems-teamwork.html'), 'utf8')
    .replace(/<script\b[^>]*>[\s\S]*?<\/script>/g, '');
const server = http.createServer((req, res) => {
    if (req.url.startsWith('/css/')) {
        res.setHeader('Content-Type', 'text/css');
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
    await send('Emulation.setDeviceMetricsOverride', {width: 1100, height: 900, deviceScaleFactor: 1, mobile: false});
    await send('Page.navigate', {url: `http://127.0.0.1:${server.address().port}/`});
    await pause(500);

    const read = file => fs.readFileSync(path.join(root,'webapp/js',file),'utf8').replace(/^import .*;\r?\n/gm,'').replace(/^export /gm,'');
    const result = await run(read('components/teamwork-viewport.js') + '\n' + read('pages/dashboard-systems-teamwork-page.js') + `
        function setText(id,text) { const n=typeof id==='string'?document.getElementById(id):id;if(n)n.textContent=text; }
        let edited=null, pdf=null;
        function openEditDialog(options) { edited=options; }
        function downloadDashboardSystemsTeamworkPdf(options) { pdf=options; }
        const check=(ok,message)=>{if(!ok)throw new Error(message);};
        const flush=()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));
        (async()=>{
            const errors=[];window.addEventListener('error',e=>errors.push(e.message));
            const lookup={trlById:new Map([['1',{label:'One'}],['2',{label:'Two'}]]),irlById:new Map(),
                classificationById:new Map(),userById:new Map(),departmentById:new Map()};
            const interfaces=Array.from({length:20000},(_,i)=>({fromEntityId:String(i+1),toEntityId:String(i+2),
                fromSbsCode:'S-'+(i+1),toSbsCode:'S-'+(i+2),fromSystemName:'System '+i,toSystemName:'Target '+i,
                fromTrlId:String(i%2+1),toTrlId:'',fromClassificationIds:i%3===0?'1,2,3,4,5,6,7,8,9':'1',
                toClassificationIds:'2',fromIrlId:'4',toIrlId:'5'}));
            for(let i=1;i<=9;i++)lookup.classificationById.set(String(i),{label:'Class-'+i,code:'Class-'+i,title:'Class '+i});
            state.dashboard={interfaces,lookup};
            const started=performance.now();renderDashboard(state.dashboard);await flush();await flush();
            const renderMs=performance.now()-started;
            const scroller=document.getElementById('dashboardSystemsTeamworkTableScroll');
            const rows=()=>[...document.querySelectorAll('.dashboard-systems-teamwork-row')];
            const count=rows().length;check(count<80,'too many rows initially: '+count);
            check(document.getElementById('dashboardSystemsTeamworkTableCount').textContent==='20000 of 20000','count lost full data');
            rows()[0].querySelector('td').dispatchEvent(new MouseEvent('dblclick',{bubbles:true}));
            check(edited?.id==='1','editing first row failed');
            for(const fraction of [.4,.8,1]){
                scroller.scrollTop=(scroller.scrollHeight-scroller.clientHeight)*fraction;
                await flush();await flush();await flush();
                check(rows().length<80,'scroll retained old rows');
                const bounds=scroller.getBoundingClientRect();
                check(rows().some(r=>{const b=r.getBoundingClientRect();return b.bottom>bounds.top+90&&b.top<bounds.bottom;}),'blank viewport after scroll');
            }
            check(rows().at(-1).textContent.includes('System 19999'),'last row unreachable');
            updateColumnWidth(5,70);await flush();await flush();
            check(rows().length<80,'resizing grew rendered rows');
            scroller.scrollLeft=500;await flush();check(scroller.scrollLeft>0,'horizontal scroll lost');
            state.filters.trl.set('2',{id:'2',label:'Two'});renderDashboard(state.dashboard);await flush();
            check(document.getElementById('dashboardSystemsTeamworkTableCount').textContent==='10000 of 20000','filter did not use all data');
            state.sort={key:'fromSbsCode',direction:'desc'};renderDashboard(state.dashboard);await flush();
            check(rows()[0].textContent.includes('S-20000'),'sorting did not use all filtered data');
            handleDownloadPdf();check(pdf.records.length===10000&&pdf.records[0].fromEntityId==='20000','PDF used virtual slice');
            check(errors.length===0,'browser errors '+errors.join(','));
            teamworkViewport.dispose();
            return {totalRows:20000,initialRenderedRows:count,renderMs,scrollResizeFilterSortEditPdf:'passed'};
        })()`);
    console.log('Teamwork virtual table:',JSON.stringify(result));
}
main().catch(error=>{console.error(error);process.exitCode=1;}).finally(()=>{socket?.close();browser?.kill();server.close();});
