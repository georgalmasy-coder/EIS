const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const {spawn} = require('node:child_process');

const root = path.resolve(__dirname, '../../main');
const output = path.resolve(__dirname, '../../../target/interfaces-viewport-test');
fs.mkdirSync(output, {recursive: true});
const html = fs.readFileSync(path.join(root, 'resources/html-pages/psys-interfaces.html'), 'utf8')
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
    await send('Emulation.setDeviceMetricsOverride', {width: 1600, height: 900, deviceScaleFactor: 1, mobile: false});
    await send('Page.navigate', {url: `http://127.0.0.1:${server.address().port}/`});
    await pause(500);
    const source = fs.readFileSync(path.join(root, 'webapp/js/pages/interfaces-page.js'), 'utf8')
        .replace(/^import .*;\r?\n/gm, '')
    const xmlHelpers = fs.readFileSync(path.join(root, 'webapp/js/core/xml.js'), 'utf8').replace(/^export /gm, '');
    const read = file => fs.readFileSync(path.join(root,'webapp/js',file),'utf8').replace(/^import .*;\r?\n/gm,'').replace(/^export /gm,'');
    const result = await run(xmlHelpers + '\n' + read('components/interface-block-loader.js') + '\n' + read('components/interface-matrix-viewport.js') + '\n' + read('components/interface-xlsx-export.js') + '\n' + read('components/interface-flow-arrows.js') + '\n' + source + `
        function setText(id,text) { const n=document.getElementById(id);if(n)n.textContent=text; }
        function formatNumber(v) { return String(v); }
        function initInterfaceEditDialog() {}
        function setInputValue() {}
        function showDialog(id) { document.getElementById(id).showModal(); }
        function closeDialogElement(id) { (typeof id==='string'?document.getElementById(id):id)?.close(); }
        function applyPageHeaderFromDocument() {}
        let edited=null;
        function openInterfaceEditDialog(options) { edited=options; }
        let entityEditOptions;
        function openEditDialog(options) { entityEditOptions=options; }
        const check=(ok,message)=>{if(!ok)throw new Error(message);};
        const flush=()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));
        (async()=>{
            const errors=[];window.addEventListener('error',e=>errors.push(e.message));window.addEventListener('unhandledrejection',e=>errors.push(String(e.reason)));
            initializeEvents();
            const structures=Array.from({length:1900},(_,i)=>({entityId:String(i+1),id:'P-'+(i+1),name:'Name '+i,label:'P-'+(i+1)}));
            const cell={fromEntityId:'1',toEntityId:'2',irlId:'4',irlCode:'IRL 4',irlColor:'#9dcca8',classificationCodes:['Power'],classificationIds:[],nextIrlMeeting:'2020-01-02'};
            const matrix={structures,cellsByKey:new Map([['1|2',cell]]),lookup:{irlById:new Map([['4',{id:'4',code:'IRL 4'}]]),classificationById:new Map()},rowGroupLabel:'Physical',columnGroupLabel:'Physical'};
            // Use the real key format.
            matrix.cellsByKey=new Map([[buildCellKey('1','2'),cell]]);state.matrix=matrix;
            for(let i=2;i<100002;i++) { const from=String(Math.floor(i/1900)+1),to=String(i%1900+1); matrix.cellsByKey.set(buildCellKey(from,to),{...cell,fromEntityId:from,toEntityId:to,irlId:'5',nextIrlMeeting:'2099-01-02'}); }
            let start=performance.now();renderInterfaceMatrix(matrix);await flush();const renderMs=performance.now()-start;const initialCells=document.querySelectorAll("td.interfaces-cell").length;
            const scroller=document.getElementById('interfacesTableScroll'),table=document.querySelector('.interfaces-table');
            const cellCount=()=>table.querySelectorAll('td.interfaces-cell').length;
            check(cellCount()<500,'matrix created too many cells');
            check(table.scrollWidth>200000 && table.scrollHeight>200000,'full scroll extent missing');
            const first=table.querySelector('td[data-from-entity-id="1"][data-to-entity-id="2"]');check(first,'first window missing');
            first.dispatchEvent(new MouseEvent('dblclick',{bubbles:true}));check(edited?.cell?.irlId==='4','edit cell data lost');
            first.dispatchEvent(new MouseEvent('contextmenu',{bubbles:true,clientX:500,clientY:300}));check(state.contextTarget?.cell===cell,'context target incorrect');
            first.dispatchEvent(new MouseEvent('mouseover',{bubbles:true}));check(table.querySelector('.interfaces-flow-arrow'),'flow arrows missing');
            scroller.scrollTop=120000;scroller.scrollLeft=126000;await flush();await flush();
            check(cellCount()<500,'scroll grew cell count');
            const far=table.querySelector('td.interfaces-cell');check(Number(far.dataset.fromEntityId)>990 && Number(far.dataset.toEntityId)>990,'far window missing');
            const position=far.getBoundingClientRect(),bounds=scroller.getBoundingClientRect();
            const expectedTop=bounds.top+126+(Number(far.dataset.fromEntityId)-1)*120-scroller.scrollTop;
            check(Math.abs(position.top-expectedTop)<2,'virtual row geometry drift: '+position.top+' expected '+expectedTop);
            scroller.scrollTop=scroller.scrollHeight;scroller.scrollLeft=scroller.scrollWidth;await flush();await flush();
            check(table.querySelector('td[data-from-entity-id="1900"][data-to-entity-id="1900"]'),'last diagonal unreachable');
            check(scroller.scrollLeft===scroller.scrollWidth-scroller.clientWidth,'horizontal scrolling unstable');
            // Off-screen reciprocal endpoints must still allow arrows through visible cells.
            scroller.scrollTop=120000;scroller.scrollLeft=0;await flush();await flush();
            const offDiagonal=table.querySelector('td.interfaces-cell:not(.interfaces-cell-self)');offDiagonal.dispatchEvent(new MouseEvent('mouseover',{bubbles:true}));
            check(table.querySelector('.interfaces-flow-arrow'),'offscreen diagonal flow arrows missing');
            state.showOverdueOnly=true;state.selectedIrlIds=['4'];renderInterfaceMatrix(matrix);await flush();
            check(table.querySelectorAll('td.interfaces-cell').length===4,'global filters lost offscreen matching endpoint');
            check(!interfaceViewport,'small filtered matrix should use regular table');
            // Full export must contain offscreen rows and columns, including a cell never rendered.
            const small={...matrix,structures:structures.slice(0,55),cellsByKey:new Map([[buildCellKey('55','54'),{...cell,fromEntityId:'55',toEntityId:'54'}]])};
            state.showOverdueOnly=false;state.selectedIrlIds=[];state.matrix=small;renderInterfaceMatrix(small);await flush();
            const before=cellCount(),snapshot=await buildFullInterfaceExport(small,'N2','Project');
            check(snapshot.widths.length===57 && snapshot.heights.length===56,'export dimensions are viewport limited');
            check(snapshot.cells.some(c=>c.row===55 && c.column===55 && c.text.includes('IRL 4')),'offscreen export cell missing');
            check(snapshot.cells.find(c=>c.row===1 && c.column===0).rowSpan===55,'export axis merge missing');
            check(cellCount()===before,'export mutated visible viewport');
            const button=document.getElementById('interfacesExcelButton');let alerts=0;window.alert=()=>alerts++;
            state.matrix=matrix;button.click();await flush();check(alerts===1 && !exportRunning,'oversized export not rejected');
            check(!document.querySelector('.interfaces-export-surface'),'export resources leaked');
            check(errors.length===0,'browser errors '+errors.join(';'));
            return {potentialCells:1900*1900,initialCells,interfaces:matrix.cellsByKey.size,renderMs,scrollAndFilters:'passed',fullExportCells:snapshot.cells.length};
        })()
    `);
    console.log('Interface virtual matrix:',JSON.stringify(result));
    const connected=await run(`(()=>{
        const check=(v,m)=>{if(!v)throw new Error(m);};
        const structures=['1','2','3'].map(entityId=>({entityId,id:'S-'+entityId,name:'System '+entityId}));
        const cell={fromEntityId:'1',toEntityId:'2',irlId:'4',irlCode:'IRL 4',classificationCodes:[],classificationIds:[],nextIrlMeeting:'2020-01-02'};
        const matrix={...state.matrix,lazy:false,structures,cellsByKey:new Map([[buildCellKey('1','2'),cell],[buildCellKey('3','3'),{...cell,fromEntityId:'3',toEntityId:'3'}]])};
        state.matrix=matrix;state.showOverdueOnly=false;state.selectedIrlIds=[];state.showConnectedOnly=false;
        renderInterfaceMatrix(matrix);
        const toggle=document.getElementById('interfacesConnectedOnlyToggle');toggle.checked=true;toggle.dispatchEvent(new Event('change'));
        check(document.querySelectorAll('td.interfaces-cell').length===4,'connected-only must exclude isolated/self-only items');
        check(buildVisibleMatrix(matrix,false,[]).rowStructures.map(s=>s.entityId).join(',')==='1,2','both incoming/outgoing endpoints retained');
        state.selectedIrlIds=['9'];renderInterfaceMatrix(matrix);
        check(document.getElementById('interfacesEmptyState').textContent.includes('No interfaces match'),'empty combined filter message');
        state.showOverdueOnly=true;
        document.getElementById('interfacesClearFiltersButton').click();
        check(!state.showConnectedOnly&&!state.showOverdueOnly&&state.selectedIrlIds.length===0,'clear filters did not reset state');
        check(!toggle.checked&&!document.getElementById('interfacesOverdueOnlyToggle').checked,'checkboxes not reset');
        check(document.querySelectorAll('td.interfaces-cell').length===9,'clear filters did not restore full matrix');
        return 'passed';
    })()`);
    console.log('Connected-only and clear filters:',connected);
    const lazy=await run(`(async()=>{
        const check=(v,m)=>{if(!v)throw new Error(m);};
        const flush=()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));
        const pause=ms=>new Promise(resolve=>setTimeout(resolve,ms));
        const ids=Array.from({length:130},(_,i)=>String(i+1));
        const xml='<PsysInterfaceMatrixDocument><interfaceMatrix snapshotId="blocks"><meta/><irlMeta><irl irlId="4" code="IRL 4" color="#9dcca8"/></irlMeta><entities>'+ids.map(id=>'<entity><entityId>'+id+'</entityId><id>P-'+id+'</id><name>Test</name></entity>').join('')+'</entities></interfaceMatrix></PsysInterfaceMatrixDocument>';
        const cells=[{fromEntityId:'1',toEntityId:'2',irlId:'4',classificationIds:'',nextIrlMeeting:'2020-01-02'},
            {fromEntityId:'65',toEntityId:'64',irlId:'4',classificationIds:'',nextIrlMeeting:'2099-01-02'},
            {fromEntityId:'120',toEntityId:'125',irlId:'4',classificationIds:'',nextIrlMeeting:'2020-01-02'}];
        const calls=[];let outstanding=0,maxActive=0,serverSnapshot="blocks",removePair=false;
        window.fetch=async(url,options)=>{
            const command=new URL(url,location.href).searchParams.get('cmd');calls.push({command,body:options.body});
            check(options.method==='POST','lazy endpoint must use POST');
            if(command==='snapshot')return new Response(xml);
            check(options.body.get('snapshotId')===serverSnapshot,'missing/current snapshot identity');
            if(command==='refresh') {
                const from=options.body.get('fromEntityId'),to=options.body.get('toEntityId');
                for(let i=cells.length-1;i>=0;i--)if((cells[i].fromEntityId===from && cells[i].toEntityId===to)||(cells[i].fromEntityId===to && cells[i].toEntityId===from))cells.splice(i,1);
                const cell=removePair?null:{fromEntityId:from,toEntityId:to,irlId:'9',classificationIds:'',nextIrlMeeting:'2099-01-02'};
                if(cell)cells.push(cell);serverSnapshot='rotated-'+calls.length;
                return new Response(JSON.stringify({snapshotId:serverSnapshot,data:{patches:[{fromEntityId:from,toEntityId:to,cell},{fromEntityId:to,toEntityId:from,cell:null}]}}));
            }
            if(command==='view')return new Response(JSON.stringify({snapshotId:serverSnapshot,data:cells.some(c=>c.fromEntityId==='120'&&c.nextIrlMeeting<'2026-01-01')?['120','125']:[]}));
            const row=Number(options.body.get('row')),column=Number(options.body.get('column'));
            outstanding++;maxActive=Math.max(maxActive,outstanding);await pause(70);outstanding--;
            const data={snapshotId:serverSnapshot,row,column,cells:cells.filter(c=>Number(c.fromEntityId)-1>=row&&Number(c.fromEntityId)-1<row+32&&Number(c.toEntityId)-1>=column&&Number(c.toEntityId)-1<column+32)};
            return new Response(JSON.stringify({snapshotId:serverSnapshot,data}));
        };
        state.showOverdueOnly=false;state.selectedIrlIds=[];
        await loadInterfaceMatrix();await flush();
        const table=document.querySelector('.interfaces-table'),scroll=document.getElementById('interfacesTableScroll');
        check(calls[0].command==='snapshot' && state.matrix.lazy,'page did not enter snapshot mode');
        check(table.querySelector('.interfaces-cell-loading'),'unloaded cells must show an hourglass');
        check(table.querySelector('.interfaces-cell-loading').getAttribute('aria-busy')==='true','missing loading state');
        await pause(120);await flush();await flush();
        check(!table.querySelector('.interfaces-cell-loading'),'loaded blank cells retained hourglasses');
        const first=table.querySelector('td[data-from-entity-id="1"][data-to-entity-id="2"]');
        check(first.textContent.includes('IRL 4'),'block cells not painted');
        first.dispatchEvent(new MouseEvent('dblclick',{bubbles:true}));check(edited?.cell?.irlId==='4','lazy edit uses stale data');
        const oldCalls=calls.length;scroll.scrollTop=4800;scroll.scrollLeft=5040;await flush();await pause(120);await flush();
        check(calls.length>oldCalls && calls.some(c=>c.command==='block'&&c.body.get('row')==='32'&&c.body.get('column')==='32'),'scroll did not fetch next block');
        state.showOverdueOnly=true;renderInterfaceMatrix(state.matrix);await pause(160);await flush();
        check(calls.some(c=>c.command==='view'),'filter did not query entire server snapshot');
        check(virtualView.rowStructures.map(s=>s.entityId).join()==='120,125','server filter axes were ignored');
        await pause(100);await flush();
        check(table.querySelector('td[data-from-entity-id="120"][data-to-entity-id="125"]').textContent.includes('IRL 4'),'filtered offscreen interface missing');
        state.showOverdueOnly=false;renderInterfaceMatrix(state.matrix);await flush();
        const exported=await buildFullInterfaceExport(state.matrix,'N2','Project');
        check(exported.cells.some(c=>c.row===65&&c.column===65&&c.text.includes('IRL 4')),'export omitted unloaded block');
        check(calls.some(c=>c.command==='block'&&c.body.get('row')==='64'),'export did not fetch missing blocks');
        check(maxActive<=2,'too many parallel block requests');
        scroll.scrollTop=4800;scroll.scrollLeft=5040;await flush();await flush();
        const savedTop=scroll.scrollTop,savedLeft=scroll.scrollLeft,count=calls.length;
        openStructureEditPage({entityId:'41'});
        await entityEditOptions.onRequestRefresh({kind:'interface',basePath:INTERFACE_BASE_PATH,fromEntityId:'41',toEntityId:'42'});await flush();await flush();
        check(calls.length===count+1 && calls.at(-1).command==='refresh','save reloaded snapshot or unchanged blocks');
        check(scroll.scrollTop===savedTop && scroll.scrollLeft===savedLeft,'save lost scroll position');
        const updated=table.querySelector('td[data-from-entity-id="41"][data-to-entity-id="42"]');
        check(updated?.textContent.includes('9'),'new interface not patched in visible cached block');
        removePair=true;await refreshInterfacePair({fromEntityId:'41',toEntityId:'42'});await flush();await flush();
        const deleted=table.querySelector('td[data-from-entity-id="41"][data-to-entity-id="42"]');
        check(deleted && deleted.textContent==='' && !deleted.classList.contains('interfaces-cell-loading'),'delete left stale content');
        removePair=false;state.showOverdueOnly=true;renderInterfaceMatrix(state.matrix);await pause(100);await flush();
        check(virtualView.rowStructures.length===2,'overdue filter setup failed');
        await refreshInterfacePair({fromEntityId:'120',toEntityId:'125'});await pause(100);await flush();
        check(!table.querySelector('td[data-from-entity-id="120"]'),'save did not update global overdue filter');
        check(calls.filter(c=>c.command==='snapshot').length===1,'targeted update created a new full snapshot');
        state.matrix.blockLoader.dispose();interfaceViewport?.dispose();
        return {snapshotRequests:calls.filter(c=>c.command==='snapshot').length,blockRequests:calls.filter(c=>c.command==='block').length,maxActive,export:'passed',targetedRefresh:'passed'};
    })()`);
    console.log('Interface POST snapshot/block/view integration:',JSON.stringify(lazy));

}
main().catch(error=>{console.error(error);process.exitCode=1;}).finally(()=>{socket?.close();browser?.kill();server.close();});
