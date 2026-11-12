const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const {spawn} = require('node:child_process');

const root = path.resolve(__dirname, '../../main');
const output = path.resolve(__dirname, '../../../target/dashboard-hierarchy-test');
fs.mkdirSync(output, {recursive: true});
let html = fs.readFileSync(path.join(root, 'resources/html-pages/dashboard-systems-teamwork.html'), 'utf8')
    .replace(/<script\b[^>]*>[\s\S]*?<\/script>/g, '');
const server = http.createServer((req, res) => {
    if (req.url.startsWith('/css/') || req.url.startsWith('/fonts/')) {
        res.setHeader('Content-Type', req.url.startsWith('/fonts/') ? 'font/woff2' : 'text/css');
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
    const shared = read('components/hierarchy-controls.js') + '\n' + read('components/dashboard-hierarchy.js');
    const fixtures = {
        'dashboard-systems-teamwork': `
            const lookup={trlById:new Map(),irlById:new Map(),classificationById:new Map(),userById:new Map(),departmentById:new Map()};
            const interfaces=['S-1','S-1.1','S-1.1.1','S-2'].map((code,i)=>({fromEntityId:String(i+1),fromSbsCode:code,fromSystemName:'Source '+code,toSbsCode:'T-1.1',toEntityId:'9'}));
            state.dashboard={interfaces,lookup}; initializePageShell(); renderDashboard(state.dashboard);
        `,
        'dashboard-irls': `
            const structures=['S-1','S-1.1','S-1.1.1','S-2'].map((id,i)=>({id,entityId:String(i+1),name:'System '+id,description:'',trlId:'',trlCode:'',daysNextTrl:'',irlCounts:new Map()}));
            const dashboard={structures,lookup:{trlById:new Map(),irlById:new Map()},irlTotalsById:new Map()};
            initializeEvents(); renderDashboard(dashboard);
        `,
        'dashboard-srls': `
            const rows=['S-1','S-1.1','S-1.1.1','S-2'].map((code,i)=>({code,entityId:String(i+1),name:'System '+code,level:3,groupAverageLevel:3}));
            const dashboard={rows,groups:[{system:rows[0],children:rows.slice(1,3)},{system:rows[3],children:[]}],summary:null,levels:[1,2,3],levelCodes:new Map(),measuredCount:4};
            loadedDashboard=dashboard;document.getElementById('srlWorkspace').hidden=false;
            mountHierarchyControls('dashboard-srlsHierarchyToolbar',expanded=>{hierarchy.setAll(rows.map(r=>r.code),expanded);renderDashboard(dashboard);},{container:true});
            renderDashboard(dashboard);
        `
    };
    for (const page of Object.keys(fixtures)) {
        html = fs.readFileSync(path.join(root, 'resources/html-pages', page+'.html'),'utf8').replace(/<script\b[^>]*>[\s\S]*?<\/script>/g,'');
        await send('Page.navigate',{url:'http://127.0.0.1:'+server.address().port+'/'+page});
        await pause(350);
        await run("document.body.classList.add('menu-is-open');document.querySelector('.app').style.setProperty('transition','none','important')");
        let prefix = shared;
        if(page==='dashboard-systems-teamwork') prefix += '\n'+read('components/teamwork-viewport.js');
        if(page==='dashboard-srls') prefix += '\n'+read('pages/dashboard-srls-model.js');
        await run(prefix+'\n'+read('pages/'+page+'-page.js')+`
            function setText(id,text){const n=typeof id==='string'?document.getElementById(id):id;if(n)n.textContent=text;}
            const userPreferences={getItem:()=>null,setItem:()=>{}};
            function mountTopbar(){} function initMenu(){return Promise.resolve();} function initHelpDialog(){} function menuHasRoute(){return true;}
            function openEditDialog(){}
        `+fixtures[page]);
        await pause(150);
        if (page === 'dashboard-systems-teamwork') {
            assert.equal(await run("getComputedStyle(document.querySelector('.dashboard-systems-teamwork-header-cell--fromSbsCode')).paddingLeft"), '12px', 'SBS header has the same left padding as other tables');
            assert.deepEqual(await run(`['fromSbsCode','toSbsCode'].map(key => {
                const cell = document.querySelector('.dashboard-systems-teamwork-row .dashboard-systems-teamwork-cell--'+key+' .hierarchy-code-cell');
                return getComputedStyle(cell).paddingLeft;
            })`), ['18px','18px'], 'Both SBS columns use shared hierarchy indentation');
            assert.equal(await run(`(() => {
                const incoming = {fromEntityId:'2',fromSbsCode:'=P-ARC-1.1',fromSystemName:'Child',
                    toEntityId:'1',toSbsCode:'=P-ARC-1',toSystemName:'WTG System',toTrlId:'8',
                    fromIrlId:'7',toIrlId:'',fromClassificationIds:'AA,AB',toClassificationIds:''};
                const entries = groupTeamworkRecords([incoming], createDashboardHierarchy());
                const parent = entries.find(entry => entry.group).record;
                return parent.fromEntityId === '1' && parent.toEntityId === '2'
                    && parent.fromTrlId === '8' && parent.toIrlId === '7'
                    && parent.fromIrlId === '' && parent.toClassificationIds === 'AA,AB'
                    && entries.some(entry => entry.record === incoming);
            })()`), true, 'Incoming parent interface appears with directional IRL and classification preserved');
            assert.equal(await run(`(() => {
                const child = {fromSbsCode:'P-1.1',fromSystemName:'Child'};
                const parent = {fromSbsCode:'P-1',fromSystemName:'Parent',fromTrlId:'4',fromSystemOwnerId:'7'};
                const entries = groupTeamworkRecords([child], createDashboardHierarchy(), [parent]);
                return entries[0].record === parent && entries[0].record.fromTrlId === '4' && entries[1].record === child;
            })()`), true, 'Parent without outgoing interfaces retains its system metadata');
            assert.equal(await run("document.querySelector('.dashboard-teamwork-group .dashboard-systems-teamwork-cell--toSbsCode').textContent.trim()"), 'T-1.1', 'Level 1 retains target system data');
            assert.equal(await run("document.querySelectorAll('.dashboard-teamwork-group .dashboard-systems-teamwork-cell--fromSbsCode.dashboard-systems-teamwork-cell--interactive').length"), 2, 'Level 1 retains edit controls');
            assert.equal(await run("document.querySelectorAll('tbody tr:not(.teamwork-viewport-spacer)').length"), 4, 'Level 1 records render once');
        }
        assert.equal(await run("document.querySelectorAll('.hierarchy-bulk-controls button').length"),2,page+' bulk controls');
        assert.equal(await run("document.querySelector('.hierarchy-toggle').getAttribute('aria-expanded')"),'true',page+' initially expanded');
        await run("document.querySelector('.hierarchy-toggle').click()");await pause(100);
        assert.equal(await run("document.querySelector('.hierarchy-toggle').getAttribute('aria-expanded')"),'false',page+' root closes');
        await run("document.querySelector('[aria-label=\"Expand all\"]').click()");await pause(100);
        assert.equal(await run("[...document.querySelectorAll('.hierarchy-toggle')].every(b=>b.getAttribute('aria-expanded')==='true')"),true,page+' expand all');
        await run("document.querySelector('[aria-label=\"Collapse all\"]').click()");await pause(100);
        assert.equal(await run("[...document.querySelectorAll('.hierarchy-toggle')].every(b=>b.getAttribute('aria-expanded')==='false')"),true,page+' collapse all');
        assert.equal(await run("getComputedStyle(document.querySelector('table.hierarchy-table')).fontSize"),'13px',page+' table font');
        assert.equal(await run("Math.round(document.querySelector('.hierarchy-bulk-controls button').getBoundingClientRect().height)<=32"),true,page+' compact buttons');
        const count = await run("[...document.querySelectorAll('table.hierarchy-table tbody tr:not(.teamwork-viewport-spacer)')].filter(row=>!row.hidden).length");
        assert.equal(count,2,page+' collapse hides all descendants');
        await run(`document.querySelector('[aria-label="Expand all"]').click()`);await pause(100);
        assert.equal(await run("[...document.querySelectorAll('[data-hierarchy-code]')].every(b=>!b.dataset.hierarchyCode.includes('.'))"),true,page+' level 1 only');
        const rect=await run("(()=>{const r=document.querySelector('.hierarchy-root-row').getBoundingClientRect();return {x:r.left+80,y:r.top+10};})()");
        await send('Input.dispatchMouseEvent',{type:'mouseMoved',x:rect.x,y:rect.y});
        await pause(50);
        assert.equal(await run("document.querySelector('.hierarchy-root-row').matches(':hover')"),true,page+' hover target');
        if (page === 'dashboard-srls') {
            const codeFits = await run(`(() => {
                const table=document.querySelector('.dashboard-srls-table');
                table.style.setProperty('--srl-code-width','136px');
                const cell=table.querySelectorAll('.dashboard-srls-code')[2];
                const label=cell.querySelector('.dashboard-srls-code-text');
                label.textContent='=P-ARC-1.1.1.123456789';
                cell.querySelector('.hierarchy-code-cell').style.setProperty('--hierarchy-depth','3');
                const bounds=cell.getBoundingClientRect(), text=label.getBoundingClientRect();
                return text.right <= bounds.right && label.scrollWidth > label.clientWidth
                    && getComputedStyle(label).textOverflow==='ellipsis' && Boolean(cell.title);
            })()`);
            assert.equal(codeFits,true,'SRL codes must truncate inside narrow columns at deep levels');
        }
        const shot=await send('Page.captureScreenshot',{format:'png'});
        fs.writeFileSync(path.join(output,page+'.png'),Buffer.from(shot.data,'base64'));
        console.log('PASS '+page+': browser rendering, root toggle, bulk controls and typography');
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;}).finally(()=>{socket?.close();browser?.kill();server.close();});
