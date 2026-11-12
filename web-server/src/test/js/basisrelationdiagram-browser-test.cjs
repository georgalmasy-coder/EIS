const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const {spawn} = require('node:child_process');

const root = path.resolve(__dirname, '../../main');
const output = path.resolve(__dirname, '../../../target/relation-diagram-test');
fs.mkdirSync(output, {recursive: true});
const xmlPath = process.argv[2];
const html = fs.readFileSync(path.join(root, 'resources/html-pages/basisrelationdiagram.html'), 'utf8')
    .replace(/<script\b[^>]*>[\s\S]*?<\/script>/g, '');
const server = http.createServer((req, res) => {
    if (req.url === '/data.xml' && xmlPath) {
        res.setHeader('Content-Type', 'application/xml');
        fs.createReadStream(xmlPath).pipe(res);
    } else if (req.url.startsWith('/css/')) {
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
    const source = fs.readFileSync(path.join(root, 'webapp/js/pages/basisrelationdiagram-page.js'), 'utf8')
        .replace(/^import .*;\r?\n/gm, '')
        .replace(/if \(document.readyState === "loading"\) \{\s*document.addEventListener\("DOMContentLoaded", startBasisRelationDiagramPage\);\s*\} else \{\s*startBasisRelationDiagramPage\(\);\s*\}/, '');
    const hierarchySource = fs.readFileSync(path.join(root, 'webapp/js/components/relation-diagram-hierarchy.js'), 'utf8').replace(/^export /gm, '');
    const loaderSource = fs.readFileSync(path.join(root, 'webapp/js/components/relation-diagram-loader.js'), 'utf8').replace(/^export /gm, '');
    const xmlHelpers = fs.readFileSync(path.join(root, 'webapp/js/core/xml.js'), 'utf8').replace(/^export /gm, '');
    await run(hierarchySource + '\n' + loaderSource + '\n' + xmlHelpers + '\n' + source + `
        function applyPageHeaderFromDocument() {}
        function setText(id, text) { const node = document.getElementById(id); if (node) node.textContent = text; }
        function naturalCompare(a, b) { return a.localeCompare(b, 'da', {numeric:true}); }
        window.testErrors=[];
        window.addEventListener('error',event=>testErrors.push(event.message));
        window.addEventListener('unhandledrejection',event=>testErrors.push(String(event.reason)));
        window.check = (ok, message) => { if (!ok) throw new Error(message); };
        window.flush = () => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
        window.pathIds = () => Array.from(document.querySelectorAll('#relationsSvgGlobal path'), p => p.dataset.relationId).sort();
        window.makeDiagram = () => {
            const make = (id, type) => ({internalId:id, type, visibleId:id, name:id, description:'', entityId:1});
            const edges = [['a','x'],['b','x'],['x','p'],['x','y'],['y','x'],['y','q'],['a','b'],['p','p'],['x','missing'],['a','x']];
            return {stakeholderRequirements:[make('a','stakeholder'),make('b','stakeholder'),make('c','stakeholder')],
                systemRequirements:[make('x','system'),make('y','system')], systemsBreakdowns:[make('p','systemsBreakdown'),make('q','systemsBreakdown')],
                relations:edges.map(([from,to],i)=>({id:'r'+i,from,to}))};
        };
        // Reference the former directional traversal, including its edge selection.
        window.referenceTrace = id => {
            const visibleIds = new Set([id]), keys = new Set();
            for (const direction of [-1,1]) {
                const visited = new Set([id]), pending = [id];
                for(let i=0;i<pending.length;i++) {
                    const current = pending[i], order = TYPE_ORDER[state.requirementsByInternalId.get(current)?.type];
                    for(const next of state.relationNeighbors.get(current)||[]) {
                        const nextOrder = TYPE_ORDER[state.requirementsByInternalId.get(next)?.type];
                        if(order == null || nextOrder == null || (nextOrder-order)*direction<0) continue;
                        keys.add(JSON.stringify([current,next])); keys.add(JSON.stringify([next,current]));
                        if(!visited.has(next)) { visited.add(next); visibleIds.add(next); pending.push(next); }
                    }
                }
            }
            return {nodes:[...visibleIds].sort(), edges:state.relations.filter(r=>keys.has(JSON.stringify([r.from,r.to]))).map(r=>r.id).sort()};
        };
        initializeEvents();
    `);
    const regression = await run(`(async () => {
        let comparisons = 0, seed = 42;
        const random = () => ((seed = (Math.imul(seed,1664525)+1013904223)>>>0)/4294967296);
        for(let iteration=0;iteration<40;iteration++) {
            const d=makeDiagram(), ids=['a','b','c','x','y','p','q','missing'];
            if(iteration) d.relations=Array.from({length:40},(_,i)=>({id:'r'+i,from:ids[Math.floor(random()*ids.length)],to:ids[Math.floor(random()*ids.length)]}));
            setRelationDiagramState(d);
            for(const id of ids) {
                const before=referenceTrace(id), after=getFocusedTrace(id);
                check(JSON.stringify(before.nodes)===JSON.stringify([...after.visibleIds].sort()),'trace nodes differ');
                check(JSON.stringify(before.edges)===JSON.stringify([...after.relations].map(r=>r.id).sort()),'trace edges differ');
                check(getFocusedTrace(id)===after,'trace cache not reused'); comparisons++;
            }
        }
        const d=makeDiagram(); setRelationDiagramState(d); renderRelationDiagram(); await flush();
        check(pathIds().length===0,'initial diagram must not create hidden lines');
        const a=state.cardsByInternalId.get('a');
        a.dispatchEvent(new MouseEvent('mouseenter')); await flush();
        const expected=referenceTrace('a').edges;
        check(JSON.stringify(pathIds())===JSON.stringify(expected),'hover lines differ');
        check(a.classList.contains('is-selected'),'hover highlight missing');
        a.dispatchEvent(new MouseEvent('mouseleave')); await flush();
        check(pathIds().length===0,'mouseleave left stale lines');
        a.click(); await flush();
        check(document.getElementById('requirementDialog').open,'click dialog missing');
        check(pathIds().length===expected.length,'persistent selection missing');
        document.getElementById('requirementDialog').close();
        state.hoverInternalId='c'; scheduleDiagramUpdate(); await flush();
        check(pathIds().length===0,'isolated hover must hide selected trace');
        state.hoverInternalId=null; scheduleDiagramUpdate(); await flush();
        check(pathIds().length===expected.length,'mouseleave must restore selected trace');
        const oldDraw=drawRelations; let draws=0;
        drawRelations=(...args)=>{draws++;return oldDraw(...args);};
        // Allow the initial ResizeObserver callback to settle before counting.
        await flush(); draws=0;
        const measurements=new Map();
        for(const card of state.cardsByInternalId.values()) {
            const read=card.getBoundingClientRect.bind(card);
            card.getBoundingClientRect=()=>{measurements.set(card,(measurements.get(card)||0)+1);return read();};
        }
        handleDiagramScroll();handleDiagramScroll();scheduleDiagramUpdate({geometry:true});await flush();
        check(draws===1,'scroll burst should cause one draw');
        check([...measurements.values()].every(n=>n===1),'card measured more than once per draw');
        const savedRelations=state.relations;
        state.relations=new Proxy(savedRelations,{get(target,key){if(key===Symbol.iterator||key==='filter')throw new Error('interaction scanned all relations');return Reflect.get(target,key);}});
        state.traceCache=null; state.hoverInternalId='y';scheduleDiagramUpdate();await flush();
        handleDiagramScroll();await flush();
        state.relations=savedRelations;state.hoverInternalId=null;scheduleDiagramUpdate();await flush();
        window.dispatchEvent(new Event('resize'));await flush();
        const path=document.querySelector('#relationsSvgGlobal path[data-relation-id="r0"]');
        const start=path.getAttribute('d').match(/^M ([^ ]+) ([^ ]+)/);
        const frame=document.getElementById('relationdiagramFrame').getBoundingClientRect(),rect=a.getBoundingClientRect();
        check(Math.abs(Number(start[1])-(rect.right-frame.left))<0.01,'line x coordinate changed');
        check(Math.abs(Number(start[2])-(rect.top+rect.height/2-frame.top))<0.01,'line y coordinate changed');
        state.columnVisibility.system=false; applyViewMode(); await flush();
        check([...document.querySelectorAll('#relationsSvgGlobal path')].every(p=>!['x','y'].includes(p.dataset.from)&&!['x','y'].includes(p.dataset.to)),'hidden column lines remain');
        state.columnVisibility.system=true; applyViewMode(); await flush();
        document.getElementById('filterRequirementText').value='c';applyFiltersAndRedraw();await flush();
        check(pathIds().length===0,'text search left filtered lines');
        check(!state.cardsByInternalId.get('c').classList.contains('is-hidden-by-filter'),'text search hid matching card');
        document.getElementById('filterRequirementText').value='';
        document.getElementById('filterOnlyWithoutRelations').checked=true; applyFiltersAndRedraw(); await flush();
        check(pathIds().length===0,'relation filter did not remove lines');
        check(!state.cardsByInternalId.get('c').classList.contains('is-hidden-by-filter'),'isolated requirement filtered out');
        setFocusedRequirement('x'); await flush();
        check(!document.getElementById('filterOnlyWithoutRelations').checked,'focus must clear filter');
        check(JSON.stringify(pathIds())===JSON.stringify(referenceTrace('x').edges),'focus trace differs');
        state.hoverInternalId='c'; scheduleDiagramUpdate(); await flush();
        check(JSON.stringify(pathIds())===JSON.stringify(referenceTrace('x').edges),'focus must override hover');
        clearFocusedRequirement(); await flush(); check(pathIds().length===0,'clear focus left lines');
        setRelationDiagramState({stakeholderRequirements:[],systemRequirements:[],systemsBreakdowns:[],relations:[]});renderRelationDiagram();await flush();
        check(pathIds().length===0&&state.cardsByInternalId.size===0,'reload left stale cards or lines');
        check(testErrors.length===0,'asynchronous browser error: '+testErrors.join('; '));
        return {traceComparisons:comparisons, interactions:'passed'};
    })()`);
    console.log('Regression:', JSON.stringify(regression));
    assert.equal(regression.traceComparisons, 320);
    const lazy = await run(`(async () => {
        const d=makeDiagram(); setRelationDiagramState(d);
        const traces=new Map([...state.requirementsByInternalId.keys()].map(id=>[id,getFocusedTrace(id)]));
        for(const requirement of state.requirementsByInternalId.values()) requirement.hasRelations=requirement.internalId!=='c';
        const calls=[];
        diagramLoader=createRelationDiagramLoader({snapshotId:'s', onExpired:()=>{}, request:async(kind,key)=>{
            calls.push([kind,key]); await new Promise(resolve=>setTimeout(resolve,key==='a'?60:10));
            return {snapshotId:'s',data:kind==='trace'?{visibleIds:[...traces.get(key).visibleIds],relations:[...traces.get(key).relations]}:kind==='search'?['c']:{description:'description only',relatedIds:['x']}};
        }});
        state.relations=[]; renderRelationDiagram(); await flush();
        check(calls.length===0,'initial rendering fetched relation traces');
        document.getElementById('filterOnlyWithoutRelations').checked=true;applyFiltersAndRedraw();await flush();
        check(!state.cardsByInternalId.get('c').classList.contains('is-hidden-by-filter'),'compact hasRelations filter failed');
        document.getElementById('filterOnlyWithoutRelations').checked=false;applyFiltersAndRedraw();await flush();
        state.hoverInternalId='a';scheduleDiagramUpdate();await flush();
        await new Promise(resolve=>setTimeout(resolve,135));
        state.hoverInternalId='x';scheduleDiagramUpdate();await flush();
        await new Promise(resolve=>setTimeout(resolve,220));await flush();
        check(JSON.stringify(pathIds())===JSON.stringify([...traces.get('x').relations].map(r=>r.id).sort()),'late hover trace replaced current trace');
        setFocusedRequirement('b');await new Promise(resolve=>setTimeout(resolve,90));await flush();
        check(JSON.stringify(pathIds())===JSON.stringify([...traces.get('b').relations].map(r=>r.id).sort()),'lazy focus trace failed');
        clearFocusedRequirement();state.hoverInternalId=null;
        document.getElementById('filterRequirementText').value='description';applyFiltersAndRedraw();
        await new Promise(resolve=>setTimeout(resolve,70));await flush();
        check(!state.cardsByInternalId.get('c').classList.contains('is-hidden-by-filter'),'server description search failed');
        check(state.cardsByInternalId.get('a').classList.contains('is-hidden-by-filter'),'search did not hide nonmatch');
        openRequirementDialog(state.requirementsByInternalId.get('a'));
        check(document.getElementById('dialogFocusButton').dataset.internalId==='a','loading dialog retains stale focus button');
        openRequirementDialog(state.requirementsByInternalId.get('b'));
        await new Promise(resolve=>setTimeout(resolve,120));await flush();
        check(document.getElementById('dialogRequirementId').textContent==='b','stale details replaced current entity');
        check(document.getElementById('dialogRequirementDescription').textContent==='description only','lazy details missing');
        check(document.getElementById('dialogRelatedGroups').textContent.includes('x'),'lazy direct neighbors missing');
        document.getElementById('requirementDialog').close();
        diagramLoader.dispose();diagramLoader=null;clearTimeout(traceTimer);traceRequestId=null;
        document.getElementById('filterRequirementText').value='';searchRequest=searchResults=null;
        check(testErrors.length===0,'lazy browser errors: '+testErrors.join('; '));
        return {requests:calls.length,interactions:'passed'};
    })()`);
    console.log('Lazy loading:', JSON.stringify(lazy));
    await run(`(async () => {
        const originalFetch=window.fetch, calls=[];
        window.fetch=async(url,options)=>{
            check(options.method==='POST','diagram API must use POST');
            calls.push(url);
            if(url.endsWith('cmd=snapshot'))return new Response('<RelationDiagramDocument><relationDiagram snapshotId="api"><stakeholderRequirements><requirement id="STK-1" entityType="5" entityId="1" hasRelations="true" relationCount="7"><code>S1</code><name>One</name></requirement></stakeholderRequirements><systemRequirements><requirement id="SYS-REQ-2" entityType="6" entityId="2" hasRelations="true" relationCount="7"><code>R2</code><name>Two</name></requirement></systemRequirements><systemsBreakdowns/></relationDiagram></RelationDiagramDocument>');
            check(options.body.get('snapshotId')==='api','missing snapshot id in POST');
            return new Response(JSON.stringify({snapshotId:'api',data:{visibleIds:['STK-1','SYS-REQ-2'],relations:[{id:'r',from:'STK-1',to:'SYS-REQ-2',type:'confirmed'}]}}));
        };
        await loadRelationDiagram();await flush();
        check(calls.length===1 && state.relations.length===0,'initial API request must be compact');
        const toggle = document.getElementById('viewStakeholder');
        check(toggle.tagName==='BUTTON','column toggle must be a button');
        toggle.click(); await new Promise(resolve=>setTimeout(resolve,180)); await flush();
        check(toggle.getAttribute('aria-pressed')==='false','hidden toggle state missing');
        check(document.querySelector('.relationdiagram-column-left').classList.contains('is-hidden-by-view'),'column did not hide');
        check(getComputedStyle(toggle.querySelector('.relationdiagram-column-icon')).opacity==='0.3','hidden icon must be dimmed');
        toggle.click(); await flush();
        check(toggle.getAttribute('aria-pressed')==='true','restored toggle state missing');
        check(!document.querySelector('.relationdiagram-column-left').classList.contains('is-hidden-by-view'),'column did not return');
        check(state.cardsByInternalId.get('STK-1').querySelector('.relationdiagram-requirement-count').textContent==='7','snapshot count must render before traces load');
        state.hoverInternalId='STK-1';scheduleDiagramUpdate();await flush();
        await new Promise(resolve=>setTimeout(resolve,200));await flush();
        check(pathIds().join()==='r','API trace response did not render relation');
        check(calls.length===2,'unexpected background requests');
        diagramLoader.dispose();diagramLoader=null;clearTimeout(traceTimer);traceRequestId=null;
        window.fetch=originalFetch;
    })()`);
    console.log('Snapshot/trace POST integration: passed');

    if (xmlPath) {
        const measured = await run(`(async () => {
            let t=performance.now();const text=await(await fetch('/data.xml')).text();const readMs=performance.now()-t;
            t=performance.now();const doc=new DOMParser().parseFromString(text,'application/xml');const xmlParseMs=performance.now()-t;
            check(!hasXmlParseError(doc),'invalid XML');
            t=performance.now();const d=parseRelationDiagramDocument(doc);const modelMs=performance.now()-t;
            t=performance.now();setRelationDiagramState(d);const indexMs=performance.now()-t;
            t=performance.now();renderRelationDiagram();await flush();const initialRenderMs=performance.now()-t;
            check(pathIds().length===0,'large initial diagram created hidden lines');
            const id=d.stakeholderRequirements[0].internalId;
            t=performance.now();state.cardsByInternalId.get(id).dispatchEvent(new MouseEvent('mouseenter'));await flush();const hoverMs=performance.now()-t;
            const expected=referenceTrace(id).edges;
            check(JSON.stringify(pathIds())===JSON.stringify(expected),'large hover lines differ');
            t=performance.now();drawRelations();const redrawMs=performance.now()-t;
            t=performance.now();handleDiagramScroll();await flush();const scrollMs=performance.now()-t;
            return {readMs,xmlParseMs,modelMs,indexMs,initialRenderMs,hoverMs,redrawMs,scrollMs,cards:state.cardsByInternalId.size,relations:d.relations.length,activePaths:pathIds().length};
        })()`);
        console.log('Dataset:', JSON.stringify(measured));
        fs.writeFileSync(path.join(output,'measurements.json'),JSON.stringify(measured,null,2));
    }
    await run(`(() => {
        const container = document.createElement('div');
        document.body.append(container);
        const requirements = ['1','1.1','1.1.1','1.2','2'].map(code => ({internalId:'tree-'+code, visibleId:code}));
        for (const requirement of requirements) {
            const card = document.createElement('button');
            card.className = 'relationdiagram-requirement-card';
            card.dataset.internalId = requirement.internalId;
            container.append(card);
        }
        let changes = 0;
        const update = () => {
            changes++;
            container.querySelectorAll('.relationdiagram-requirement-card').forEach(card =>
                card.classList.toggle('is-hidden-by-filter', card.dataset.hierarchyHidden==='true'));
        };
        decorateRelationDiagramHierarchy(container, requirements, update);
        const rows = [...container.children];
        check(rows[0].querySelector('.is-level-one'), 'level one background class missing');
        check(rows[1].style.getPropertyValue('--hierarchy-depth')==='1' && rows[2].style.getPropertyValue('--hierarchy-depth')==='1', 'all descendants must have identical indentation');
        const rootToggle = rows[0].querySelector('.relationdiagram-hierarchy-toggle');
        check(rows.slice(1,4).every(row => !row.querySelector('.relationdiagram-hierarchy-toggle')), 'only level one may have toggles');
        rootToggle.click();
        check(rows.slice(1,4).every(row => getComputedStyle(row).display==='none'), 'entire branch did not collapse');
        check(getComputedStyle(rows[4]).display!=='none', 'unrelated root collapsed');
        rootToggle.click();
        check(rows.slice(1,4).every(row => getComputedStyle(row).display!=='none'), 'all descendants must expand together');
        check(rootToggle.getAttribute('aria-expanded')==='true' && changes===2, 'toggle accessibility or callback failed');
        container.remove();
    })()`);
    console.log('Hierarchy collapse/expand: passed');

}
main().catch(error => { console.error(error); process.exitCode = 1; }).finally(() => {
    socket?.close(); browser?.kill(); server.close();
});
