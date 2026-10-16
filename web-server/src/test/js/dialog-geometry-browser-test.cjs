// Browser interaction tests using the real editor shell, dialog markup and styles.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const {spawn} = require('node:child_process');
const webapp = path.resolve(__dirname, '../../main/webapp');
const output = path.resolve(__dirname, '../../../target/dialog-geometry-test');
fs.mkdirSync(output, {recursive: true});
const server = http.createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost');
    if (url.pathname === '/version-fixture') {
        res.setHeader('Content-Type', 'application/xml');
        return res.end(`<Entity><Name header="Name" control="text">Version ${url.searchParams.get('version')}</Name></Entity>`);
    }
    if (url.pathname === '/') {
        res.setHeader('Content-Type', 'text/html');
        return res.end(`<link rel="stylesheet" href="/css/common.css"><script type="module">
            import {openEditDialog} from '/js/components/edit-dialog.js';
            window.openEditor = page => window.editor = openEditDialog({page});
            openEditor('systemsbreakdown-edit');
        </script>`);
    }
    if (url.pathname === '/fixture') {
        const file = path.resolve(__dirname, '../../main/resources/html-pages', url.searchParams.get('template'));
        const html = fs.readFileSync(file, 'utf8');
        const styles = [...html.matchAll(/<link[^>]+href="[^"]*\/css\/([^"?]+)[^"]*"[^>]*>/g)]
            .map(match => `<link rel="stylesheet" href="/css/${match[1]}">`).join('');
        const dialogs = [...html.matchAll(/<dialog\b[\s\S]*?<\/dialog>/g)].map(match => match[0]).join('');
        res.setHeader('Content-Type', 'text/html');
        return res.end(`${styles}${dialogs}<script type="module">
            import {installDialogGeometry} from '/js/components/dialog-geometry.js';
            window.install = installDialogGeometry;
        </script>`);
    }
    if (url.pathname === '/dashboard-fixture') {
        const html = fs.readFileSync(path.resolve(__dirname, '../../main/resources/html-pages/admin/views/dashboard-mail-status-view.html'), 'utf8');
        res.setHeader('Content-Type', 'text/html');
        return res.end(`<link rel="stylesheet" href="/css/common.css"><link rel="stylesheet" href="/css/admin-dashboard.css">${html}
            <script type="module">import {installDialogGeometry} from '/js/components/dialog-geometry.js';window.install=installDialogGeometry;</script>`);
    }
    if (url.pathname === '/dynamic-fixture') {
        const extract = (file, expression) => fs.readFileSync(path.join(webapp, 'js/components', file), 'utf8').match(expression)[1];
        const markup = {
            preview: `<dialog class="import-preview-dialog">${extract('import-dialog.js', /previewDialog\.innerHTML = `([\s\S]*?)`;/)}</dialog>`,
            progress: `<dialog class="import-progress-dialog">${extract('import-dialog.js', /importProgressDialog\.innerHTML = `([\s\S]*?)`;/)}</dialog>`,
            link: `<dialog id="linkDialog">${extract('links-table.js', /dialog\.innerHTML = `([\s\S]*?)`;/)}</dialog>`,
            move: extract('entity-move-selection.js', /document\.body\.insertAdjacentHTML\("beforeend", `([\s\S]*?)`\);/)
        };
        res.setHeader('Content-Type', 'text/html');
        return res.end(`<link rel="stylesheet" href="/css/common.css"><link rel="stylesheet" href="/css/import-dialog.css"><link rel="stylesheet" href="/css/links-table.css"><link rel="stylesheet" href="/css/history-table.css">
            <script type="module">
                import {installDialogGeometry} from '/js/components/dialog-geometry.js';
                import {openHistoryDiff} from '/js/components/history-diff-dialog.js';
                installDialogGeometry();
                window.openDynamic = async name => {
                    if(name==='diff') return openHistoryDiff({versions:[], historyRows:[]});
                    const t=document.createElement('template');t.innerHTML=${JSON.stringify(markup)}[name];
                    const d=t.content.querySelector('dialog');document.body.appendChild(d);d.showModal();return d;
                };
            </script>`);
    }
    if (url.pathname === '/web/view') {
        res.setHeader('Content-Type', 'text/html');
        if (url.searchParams.get('mode') === 'fixture-real') {
            const html = fs.readFileSync(path.resolve(__dirname, '../../main/resources/html-pages', url.searchParams.get('page') + '.html'), 'utf8');
            return res.end(html.replace(/<script\b[\s\S]*?<\/script>/g, '')
                .replace(/href="\.\.\/css\//g, 'href="/css/')
                .replace(/<body([^>]*?)class="([^"]*)"/, '<body$1class="$2 eis-edit-dialog-mode"'));
        }
        return res.end(`<link rel="stylesheet" href="/css/common.css">
            <style>body{margin:0;height:100vh;display:flex;flex-direction:column}.systemsbreakdown-edit-section-header{padding:20px}main{flex:1;overflow:auto}footer{padding:20px}</style>
            <header class="section-header systemsbreakdown-edit-section-header">Physical Architecture <button>Help</button></header>
            <main><input value="Diesel Generator System"></main><footer><button>Save</button><button>Cancel</button></footer>`);
    }
    const file = path.resolve(webapp, '.' + url.pathname);
    if (!file.startsWith(webapp + path.sep) || !fs.existsSync(file)) { res.writeHead(404); return res.end(); }
    res.setHeader('Content-Type', file.endsWith('.js') ? 'text/javascript' : 'text/css');
    res.end(fs.readFileSync(file));
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
    const wait = async expression => {
        for (let i = 0; i < 100; i++) { if (await run(expression)) return; await pause(50); }
        throw new Error(`Timed out: ${expression}`);
    };
    const rect = () => run('editor.dialog.getBoundingClientRect().toJSON()');
    const drag = async (x, y, dx, dy) => {
        await send('Input.dispatchMouseEvent', {type: 'mouseMoved', x, y});
        await send('Input.dispatchMouseEvent', {type: 'mousePressed', x, y, button: 'left', clickCount: 1});
        await send('Input.dispatchMouseEvent', {type: 'mouseMoved', x: x + dx, y: y + dy, button: 'left', buttons: 1});
        await send('Input.dispatchMouseEvent', {type: 'mouseReleased', x: x + dx, y: y + dy, button: 'left', clickCount: 1});
    };
    await send('Emulation.setDeviceMetricsOverride', {width: 1600, height: 1200, deviceScaleFactor: 1, mobile: false});

    await send('Page.navigate', {url: `http://127.0.0.1:${server.address().port}/`});
    await wait("!!document.querySelector('.eis-dialog-resize')");
    const initial = await rect();
    assert.equal(await run("editor.dialog.getAttribute('style')"), null, 'initial geometry uses existing CSS');
    await drag(initial.left + 60, initial.top + 25, 60, 40);
    const moved = await rect();
    assert.equal(moved.left, initial.left + 60);
    assert.equal(moved.top, initial.top + 40);
    assert.equal(moved.width, initial.width);
    assert.equal(moved.height, initial.height);
    await drag(moved.right - 7, moved.bottom - 7, 100, 80);
    const enlarged = await rect();
    assert.equal(enlarged.width, initial.width + 100);
    assert.equal(enlarged.height, initial.height + 80);
    await drag(enlarged.right - 7, enlarged.bottom - 7, -250, -200);
    const restored = await rect();
    assert.equal(restored.width, initial.width, 'cannot shrink below opening width');
    assert.equal(restored.height, initial.height, 'cannot shrink below opening height');
    await drag(restored.left + 60, restored.top + 25, -2000, -2000);
    assert.equal((await rect()).left, 0);
    assert.equal((await rect()).top, 0);
    await run("editor.close(); openEditor('systemsbreakdown-edit')");
    await wait("!!document.querySelector('.eis-dialog-resize')");
    assert.deepEqual(await rect(), initial, 'reopening resets geometry');
    await run("editor.close(); openEditor('project-edit')");
    await wait("!!document.querySelector('.eis-dialog-resize')");
    const project = await rect();
    await drag(project.left + 60, project.top + 25, 30, 20);
    assert.equal((await rect()).left, project.left + 30, 'other iframe editors also move');
    await run("editor.close(); openEditor('forgot-password')");
    await wait("editor.iframe.contentDocument?.querySelector('footer')");
    assert.equal(await run("!!document.querySelector('.eis-dialog-resize')"), false, 'excluded iframe pages have no handle');

    const fixtures = [
        ['systemsbreakdown-edit.html', 'noteDialog'],
        ['systemsbreakdown-edit.html', 'attachmentDialog'],
        ['systemsbreakdown-edit.html', 'relationDialog'],
        ['systemsbreakdown-edit.html', 'helpDialog'],
        ['systemsbreakdown-main.html', 'exportDialog'],
        ['systemsbreakdown-main.html', 'importDialog'],
        ['baseline-main.html', 'baselineDialog'],
        ['department-main.html', 'departmentDialog'],
        ['lookup-maintenance.html', 'lookupDialog'],
        ['psys-interfaces.html', 'interfaceDialog'],
        ['psys-interfaces.html', 'interfaceRemoveDialog'],
        ['basistraceabilitymatrix.html', 'traceabilityRequirementDialog'],
        ['basisrelationdiagram.html', 'requirementDialog'],
        ['basisrelationdiagram.html', 'relationCreateDialog'],
        ['admin/user-administration.html', 'userEditDialog'],
        ['admin/customer-administration.html', 'customerEditDialog'],
        ['admin/subscription-editor.html', 'subscriptionEditorDialog'],
        ['admin/menu-editor.html', 'menuDialog'],
        ['admin/lookup-main.html', 'lookupDialog'],
        ['admin/lookup-main.html', 'lookupExportDialog'],
        ['admin/lookup-main.html', 'lookupImportDialog'],
        ['requirements.html', 'stakeholderExportDialog'],
        ['requirements.html', 'stakeholderImportDialog'],
        ['requirements.html', 'systemExportDialog'],
        ['requirements.html', 'systemImportDialog'],
        ['admin/incidents-main.html', 'incidentTraceDialog'],
        ['admin/mail-templates.html', 'templateDialog'],
        ['admin/mail-templates.html', 'linkDialog'],
        ['admin/mail-templates.html', 'previewDialog'],
        ['admin/mail-templates.html', 'testDialog'],
        ['admin/mail-templates.html', 'importDialog']
    ];
    const origin = `http://127.0.0.1:${server.address().port}`;
    for (const [template, id] of fixtures) {
        await send('Page.navigate', {url: `${origin}/fixture?template=${encodeURIComponent(template)}`});
        await wait('!!window.install');
        const before = await run(`window.testDialog=document.getElementById(${JSON.stringify(id)});testDialog.showModal();testDialog.getBoundingClientRect().toJSON()`);
        await run('install();install()');
        await wait("!!testDialog.querySelector('.eis-dialog-resize')");
        assert.deepEqual(await run('testDialog.getBoundingClientRect().toJSON()'), before, `${id}: unchanged opening geometry`);
        assert.equal(await run("testDialog.querySelectorAll('.eis-dialog-resize').length"), 1, 'installation is idempotent');
        const point = await run("(() => {const h=testDialog.querySelector('.eis-dialog-resize');const r=h.getBoundingClientRect();return {x:r.right-4,y:r.bottom-4};})()");
        assert(point.x <= before.right && point.x >= before.left, `${id}: handle belongs to dialog`);
        // Move to the top left first, so even large dialogs have room to grow.
        const headerPoint = await run("(() => {const headers=[...testDialog.querySelectorAll('*')].filter(e=>e.style.cursor==='move');const r=headers[0].getBoundingClientRect();return {x:r.left+5,y:r.top+5};})()");
        await drag(headerPoint.x, headerPoint.y, -before.left, -before.top);
        let current = await run('testDialog.getBoundingClientRect().toJSON()');
        assert(Math.abs(current.left) < 1 && Math.abs(current.top) < 1, `${template}/${id}: header moves dialog`);
        const baselineLayout = () => run("({text:document.getElementById('fieldDescription').getBoundingClientRect().toJSON(),save:document.getElementById('btnSaveBaseline').getBoundingClientRect().toJSON(),cancel:document.getElementById('btnCancelBaseline').getBoundingClientRect().toJSON()})");
        const baselineBefore = id === 'baselineDialog' ? await baselineLayout() : null;
        const growGrip = await run("testDialog.querySelector('.eis-dialog-resize').getBoundingClientRect().toJSON()");
        await drag(growGrip.right - 4, growGrip.bottom - 4, 40, 40);
        current = await run('testDialog.getBoundingClientRect().toJSON()');
        assert(current.width >= before.width && current.height >= before.height, `${id}: grows`);
        assert(current.width > before.width || current.height > before.height, `${id}: resize changes dimensions`);
        if (baselineBefore) {
            const after = await baselineLayout();
            const near = (actual, expected) => assert(Math.abs(actual - expected) < 1, `baseline layout: ${actual} should match ${expected}`);
            assert(after.text.height >= baselineBefore.text.height + 39, 'baseline description fills the added height');
            near(after.text.width, baselineBefore.text.width + 40);
            for (const key of ['save', 'cancel']) {
                near(after[key].bottom, current.bottom - 9);
                near(after[key].right, baselineBefore[key].right + 40);
            }
        }
        const grip = await run("testDialog.querySelector('.eis-dialog-resize').getBoundingClientRect().toJSON()");
        await drag(grip.right - 4, grip.bottom - 4, -200, -200);
        current = await run('testDialog.getBoundingClientRect().toJSON()');
        assert.equal(current.width, before.width, `${id}: minimum width`);
        assert.equal(current.height, before.height, `${id}: minimum height`);
        await run('testDialog.close()');
        await wait("!testDialog.querySelector('.eis-dialog-resize')");
        await run('testDialog.showModal()');
        await wait("!!testDialog.querySelector('.eis-dialog-resize')");
        assert.deepEqual(await run('testDialog.getBoundingClientRect().toJSON()'), before, `${id}: reopen resets`);
    }
    // Shared relation dialogs must use matching foreground/background colours.
    for (const template of ['basisrelationdiagram.html', 'rflprelationdiagram.html']) {
        await send('Page.navigate', {url: `${origin}/fixture?template=${template}`});
        await wait('!!window.install');
        for (const theme of ['theme-light', 'theme-blue', 'theme-black']) {
            const contrast = await run(`(() => {
                document.body.className=${JSON.stringify(theme)};
                const d=document.getElementById('relationCreateDialog');
                if(!d.open)d.showModal();
                const prompt=d.querySelector('.relationdiagram-dialog-description');
                prompt.innerHTML='<div class="relationdiagram-dialog-line-intro">Please confirm</div><div><span class="relationdiagram-dialog-line-label">From:</span> <span class="relationdiagram-dialog-line-value">Physical Architecture</span></div>';
                const luminance=color=>{const c=color.match(/[\\d.]+/g).slice(0,3).map(n=>{const v=Number(n)/255;return v<=.04045?v/12.92:((v+.055)/1.055)**2.4;});return c[0]*.2126+c[1]*.7152+c[2]*.0722;};
                return [['.relationdiagram-dialog-head h3','.relationdiagram-dialog-head'],['.relationdiagram-dialog-line-intro','.relationdiagram-dialog-description'],['.relationdiagram-dialog-line-label','.relationdiagram-dialog-description'],['.relationdiagram-dialog-line-value','.relationdiagram-dialog-description']].map(([text,bg])=>{
                    const a=luminance(getComputedStyle(d.querySelector(text)).color),b=luminance(getComputedStyle(d.querySelector(bg)).backgroundColor);
                    return (Math.max(a,b)+.05)/(Math.min(a,b)+.05);
                });
            })()`);
            assert(contrast.every(value => value >= 4.5), `${template}/${theme}: relation text has readable contrast (${contrast})`);
        }
    }
    await send('Page.navigate', {url: origin});
    await wait("!!document.querySelector('.eis-dialog-resize')");
    const editors = ['project-edit', 'stakeholder-edit', 'stakeholderrequirement-edit', 'systemrequirement-edit', 'functionalstructure-edit', 'logicalstructure-edit', 'systemsbreakdown-edit', 'user-edit'];
    for (const page of editors) {
        await run(`editor.close();window.editor = null`);
        // Open with the real HTML template, without starting API-dependent page code.
        await run(`import('/js/components/edit-dialog.js').then(({openEditDialog})=>window.editor=openEditDialog({page:${JSON.stringify(page)},mode:'fixture-real'}))`);
        await wait("!!document.querySelector('.eis-dialog-resize')");
        const before = await rect();
        const point = await run("(() => {const d=editor.iframe.contentDocument;const h=[...d.querySelectorAll('*')].find(e=>e.style.cursor==='move');const r=h.getBoundingClientRect(),f=editor.iframe.getBoundingClientRect();return {x:f.left+r.left+5,y:f.top+r.top+5};})()");
        await drag(point.x, point.y, 20, 10);
        assert.equal((await rect()).left, before.left + 20, `${page}: actual editor title moves outer dialog`);
    }
    await send('Emulation.setDeviceMetricsOverride', {width: 900, height: 600, deviceScaleFactor: 1, mobile: false});
    await pause(100);
    const fitted = await rect();
    assert(fitted.left >= 0 && fitted.top >= 0 && fitted.right <= 900 && fitted.bottom <= 600, 'smaller browser keeps dialog accessible');
    await send('Emulation.setDeviceMetricsOverride', {width: 1600, height: 1200, deviceScaleFactor: 1, mobile: false});

    await run("editor.close();import('/js/components/edit-dialog.js').then(({openEditDialog})=>window.editor=openEditDialog({page:'systemsbreakdown-edit',mode:'fixture-real'}))");
    await wait("!!document.querySelector('.eis-edit-dialog .eis-dialog-resize')");
    const editorBeforeDiff = await rect();
    const openNestedDiff = () => run(`(async () => {window.diff = await editor.iframe.contentWindow.eval(
        "import('/js/components/history-diff-dialog.js').then(({openHistoryDiff}) => openHistoryDiff({versions:['1','2'], historyRows:[], id:'test', buildVersionUrl:({version})=>'/version-fixture?version='+version, findDetailNode:doc=>doc.documentElement}))"
    )})()`);
    await openNestedDiff();
    await wait("diff.querySelector('.history-diff-content').getAttribute('aria-busy')==='false'");
    assert.equal(await run('diff.ownerDocument === document'), true, 'nested comparison belongs to main window');
    assert.equal(await run("!!editor.iframe.contentDocument.querySelector('.history-diff-dialog')"), false, 'comparison escapes iframe clipping');
    assert.equal(await run("diff.querySelectorAll('.eis-dialog-resize').length"), 1, 'comparison has one geometry controller');
    assert.equal(await run("diff.querySelectorAll('.history-diff-row.is-changed').length"), 1, 'real version data still loads and compares');
    const originalDiff = await run('diff.getBoundingClientRect().toJSON()');
    const diffTitle = await run("diff.querySelector('.history-diff-toolbar').getBoundingClientRect().toJSON()");
    await drag(diffTitle.left + 5, diffTitle.top + 5, -originalDiff.left, -originalDiff.top);
    let diffRect = await run('diff.getBoundingClientRect().toJSON()');
    assert.equal(diffRect.left, 0, 'comparison reaches screen left');
    assert.equal(diffRect.top, 0, 'comparison reaches screen top');
    let diffGrip = await run("diff.querySelector('.eis-dialog-resize').getBoundingClientRect().toJSON()");
    await drag(diffGrip.right - 4, diffGrip.bottom - 4, 2000, 2000);
    diffRect = await run('diff.getBoundingClientRect().toJSON()');
    assert.equal(diffRect.right, 1600, 'comparison reaches screen right');
    assert.equal(diffRect.bottom, 1200, 'comparison reaches screen bottom');
    assert(diffRect.right > editorBeforeDiff.right && diffRect.bottom > editorBeforeDiff.bottom, 'comparison grows beyond underlying editor');
    diffGrip = await run("diff.querySelector('.eis-dialog-resize').getBoundingClientRect().toJSON()");
    await drag(diffGrip.right - 4, diffGrip.bottom - 4, -2000, -2000);
    diffRect = await run('diff.getBoundingClientRect().toJSON()');
    assert.equal(diffRect.width, originalDiff.width, 'comparison retains minimum width');
    assert.equal(diffRect.height, originalDiff.height, 'comparison retains minimum height');
    await send('Input.dispatchKeyEvent', {type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27});
    await send('Input.dispatchKeyEvent', {type: 'keyUp', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27});
    await wait('!diff.isConnected');
    assert.equal(await run('editor.dialog.open'), true, 'Escape closes comparison only');
    assert.deepEqual(await rect(), editorBeforeDiff, 'underlying editor geometry is unchanged');
    await openNestedDiff();
    await run("diff.querySelector('button').click()");
    await wait('!diff.isConnected');
    await openNestedDiff();
    await run('editor.iframe.src=editor.iframe.src');
    await wait('!diff.isConnected');
    await wait("!!document.querySelector('.eis-edit-dialog .eis-dialog-resize')");
    await openNestedDiff();
    await run('editor.close()');
    await wait('!diff.isConnected');

    for (const page of ['login', 'forgot-password', 'enter-new-password', 'createcustomer', 'customer-registration', 'dashboard-customer-creation-view']) {
        await send('Page.navigate', {url: `${origin}/fixture?template=systemsbreakdown-edit.html&page=${page}`});
        await wait('!!window.install');
        await run('document.getElementById("noteDialog").showModal();install()');
        assert.equal(await run("!!document.querySelector('.eis-dialog-resize')"), false, `${page}: excluded`);
    }

    await send('Page.navigate', {url: `${origin}/dynamic-fixture`});
    await wait('!!window.openDynamic');
    for (const name of ['preview', 'progress', 'link', 'move', 'diff']) {
        await run(`(async () => {window.dynamic=await openDynamic(${JSON.stringify(name)})})()`);
        await wait("!!dynamic.querySelector('.eis-dialog-resize')");
        const before = await run('dynamic.getBoundingClientRect().toJSON()');
        const point = await run("(() => {const h=[...dynamic.querySelectorAll('*')].find(e=>e.style.cursor==='move');const r=h.getBoundingClientRect();return {x:r.left+5,y:r.top+5};})()");
        await drag(point.x, point.y, -20, -10);
        assert.equal((await run('dynamic.getBoundingClientRect().toJSON()')).left, before.left - 20, `${name}: dynamic dialog moves`);
        const grip = await run("dynamic.querySelector('.eis-dialog-resize').getBoundingClientRect().toJSON()");
        await drag(grip.right - 4, grip.bottom - 4, 30, 30);
        assert((await run('dynamic.getBoundingClientRect().toJSON()')).width > before.width, `${name}: dynamic dialog resizes`);
        await run('dynamic.close();dynamic.remove()');
    }
    await send('Page.navigate', {url: `${origin}/dashboard-fixture`});
    await wait('!!window.install');
    for (const name of ['mailDetails', 'mailResend']) {
        await run(`window.backdrop=document.querySelector('[data-modal="${name}"]');backdrop.hidden=false;window.modal=backdrop.querySelector('[role=dialog]');install()`);
        await wait("!!modal.querySelector('.eis-dialog-resize')");
        const current = await run('modal.getBoundingClientRect().toJSON()');
        const handle = await run("modal.querySelector('.eis-dialog-resize').getBoundingClientRect().toJSON()");
        assert(handle.right <= current.right && handle.bottom <= current.bottom, 'dashboard handle stays in modal');
        await drag(current.left + 30, current.top + 25, -30, -20);
        assert.equal((await run('modal.getBoundingClientRect().toJSON()')).left, current.left - 30);
        await run('backdrop.hidden=true');
        await wait("!modal.querySelector('.eis-dialog-resize')");
    }
    console.log(`Passed: nested diff reaches all screen edges and cleans up on close/navigation; ${editors.length} real iframe editors, ${fixtures.length} native dialog variants, 5 dynamic dialogs, 2 dashboard modals, exclusions, minimum sizes and reopening.`);
}
main().catch(error => { console.error(error); process.exitCode = 1; }).finally(() => { socket?.close(); browser?.kill(); server.close(); });
