const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const {spawn} = require('node:child_process');

const root = path.resolve(__dirname, '../../main');
const output = path.resolve(__dirname, '../../../target/relation-diagram-test');
fs.mkdirSync(output, {recursive: true});
const xmlPath = process.argv[2];
const html = fs.readFileSync(path.join(root, 'resources/html-pages/rflprelationdiagram.html'), 'utf8')
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
    const source = fs.readFileSync(path.join(root, 'webapp/js/pages/nflprelationdiagram-page.js'), 'utf8')
        .replace(/^import .*;\r?\n/gm, '')
        .replace(/if \(document.readyState === "loading"\) \{\s*document.addEventListener\("DOMContentLoaded", startNflpRelationDiagramPage\);\s*\} else \{\s*startNflpRelationDiagramPage\(\);\s*\}/, '');
    const hierarchySource = fs.readFileSync(path.join(root, 'webapp/js/components/relation-diagram-hierarchy.js'), 'utf8').replace(/^export /gm, '');
    const loaderSource = fs.readFileSync(path.join(root, 'webapp/js/components/relation-diagram-loader.js'), 'utf8').replace(/^export /gm, '');
    const xmlHelpers = fs.readFileSync(path.join(root, 'webapp/js/core/xml.js'), 'utf8').replace(/^export /gm, '');
    const cssHelpers = fs.readFileSync(path.join(root, 'webapp/js/core/css.js'), 'utf8').replace(/^export /gm, '');
    const result = await run(hierarchySource + '\n' + loaderSource + '\n' + xmlHelpers + '\n' + cssHelpers + '\n' + source + `
        function setText(id,text) { const node=document.getElementById(id);if(node)node.textContent=text; }
        function applyPageHeaderFromDocument() {}
        function naturalCompare(a,b) { return a.localeCompare(b); }
        window.check=(ok,message)=>{if(!ok)throw new Error(message);};
        window.flush=()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));
        window.wait=ms=>new Promise(resolve=>setTimeout(resolve,ms));
        window.paths=()=>[...document.querySelectorAll('#relationsSvgGlobal path')].map(p=>p.dataset.relationId).sort();
        window.errors=[];window.addEventListener('unhandledrejection',e=>errors.push(String(e.reason)));
        window.addEventListener('error',e=>errors.push(e.message));
        (async()=>{
            const nodes=[['STK-1',5,'stakeholderRequirements','requirement'],['SYS-REQ-2',6,'systemRequirements','requirement'],['F-ELM-3',9,'functionalStructures','functional'],['F-ELM-4',9,'functionalStructures','functional'],['L-ELM-5',8,'logicalStructures','logical'],['P-ARC-6',2,'systemsBreakdowns','systemsBreakdown'],['P-ARC-7',2,'systemsBreakdowns','systemsBreakdown']];
            const ids=nodes.map(n=>n[0]);
            const edges=[[0,1],[1,2],[2,3],[2,4],[4,5]].map(([from,to],i)=>({id:'r'+i,from:ids[from],to:ids[to],type:'confirmed'}));
            const groups=[...new Set(nodes.map(n=>n[2]))];
            const xml='<RflpRelationDiagramDocument><NflpRelationDiagram snapshotId="rflp">'+groups.map(group=>'<'+group+'>'+nodes.filter(n=>n[2]===group).map(([id,type,g,tag],i)=>'<'+tag+' id="'+id+'" entityType="'+type+'" entityId="'+(i+1)+'" hasRelations="'+(id!==ids[6])+'" relationCount="'+(id===ids[6]?0:5)+'"><code>'+id+'</code><name>Item</name></'+tag+'>').join('')+'</'+group+'>').join('')+'</NflpRelationDiagram></RflpRelationDiagramDocument>';
            const calls=[];
            window.fetch=async(url,options)=>{
                check(options.method==='POST','API must use POST');
                const cmd=new URL(url,location.href).searchParams.get('cmd');calls.push(cmd);
                if(cmd==='snapshot')return new Response(xml);
                check(options.body.get('snapshotId')==='rflp','snapshot id missing');
                const id=options.body.get('internalId');await wait(id===ids[0]?65:10);
                const data=cmd==='search'?[ids[6]]:cmd==='details'?{description:'description only '+id,relatedIds:[ids[2]]}:{visibleIds:id===ids[6]?[id]:ids.slice(0,6),relations:id===ids[6]?[]:edges};
                return new Response(JSON.stringify({snapshotId:'rflp',data}));
            };
            initializeEvents();await loadRelationDiagram();await flush();
            check(calls.join()==='snapshot','initial load fetched graph');
        const toggle = document.getElementById('viewStakeholder');
        check(toggle.tagName==='BUTTON','column toggle must be a button');
        toggle.click(); await new Promise(resolve=>setTimeout(resolve,180)); await flush();
        check(toggle.getAttribute('aria-pressed')==='false','hidden toggle state missing');
        check(document.querySelector('[data-entity-type="stakeholder"]').classList.contains('is-hidden-by-view'),'column did not hide');
        check(getComputedStyle(toggle.querySelector('.relationdiagram-column-icon')).opacity==='0.3','hidden icon must be dimmed');
        toggle.click(); await flush();
        check(toggle.getAttribute('aria-pressed')==='true','restored toggle state missing');
        check(!document.querySelector('[data-entity-type="stakeholder"]').classList.contains('is-hidden-by-view'),'column did not return');
            check(state.requirementsByInternalId.size===7 && state.relations.length===0,'compact model failed');
            check(paths().length===0,'initial SVG must be empty');
            const card=id=>document.querySelector('[data-internal-id="'+id+'"].relationdiagram-requirement-card');
            check(card(ids[0]).querySelector('.relationdiagram-requirement-count').textContent==='5','relation count missing');
            check(card(ids[6]).querySelector('.relationdiagram-requirement-count').textContent==='0','zero count missing');
            card(ids[0]).dispatchEvent(new MouseEvent('mouseenter'));await wait(140);
            card(ids[6]).dispatchEvent(new MouseEvent('mouseenter'));await wait(240);await flush();
            check(paths().length===0,'late trace overrode newer hover');
            card(ids[2]).dispatchEvent(new MouseEvent('mouseenter'));await wait(200);await flush();
            check(paths().length===5,'hover across five types or same-type trace failed');
            setFocusedRequirement(ids[4]);await wait(90);await flush();
            check(paths().length===5 && card(ids[6]).classList.contains('is-hidden-by-filter'),'focus filtering failed');
            clearFocusedRequirement();state.hoverInternalId=null;
            document.getElementById('filterOnlyWithoutRelations').checked=true;applyFiltersAndRedraw();await flush();
            check(!card(ids[6]).classList.contains('is-hidden-by-filter') && card(ids[0]).classList.contains('is-hidden-by-filter'),'hasRelations filter failed');
            document.getElementById('filterOnlyWithoutRelations').checked=false;
            document.getElementById('filterRequirementText').value='description';applyFiltersAndRedraw();await wait(70);await flush();
            check(!card(ids[6]).classList.contains('is-hidden-by-filter') && card(ids[0]).classList.contains('is-hidden-by-filter'),'server description search failed');
            openRequirementDialog(state.requirementsByInternalId.get(ids[0]));
            openRequirementDialog(state.requirementsByInternalId.get(ids[4]));
            check(state.dialogInternalId===ids[4],'loading dialog actions refer to previous entity');
            await wait(120);await flush();
            check(document.getElementById('dialogRequirementDescription').textContent==='description only '+ids[4],'late details replaced current entity');
            check(document.getElementById('dialogRelatedGroups').textContent.includes(ids[2]),'direct neighbors missing');
            document.getElementById('requirementDialog').close();
            check(errors.length===0,'browser errors: '+errors.join(';'));
            return {entities:state.requirementsByInternalId.size,requests:calls.length,interactions:'passed'};
        })()
    `);
    console.log('RFLP lazy loading:', JSON.stringify(result));
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
