const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const {spawn} = require('node:child_process');
const webapp = path.resolve(__dirname, '../../main/webapp');
const output = path.resolve(__dirname, '../../../target/dashboard-srls-test');
fs.mkdirSync(output, {recursive: true});
let mode = 'normal';
let includeN2Route = true;
const preferences = {};
const server = http.createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost');
    if (url.pathname === '/api/user-preferences') {
        res.setHeader('Content-Type', 'application/json');
        if (req.method === 'POST') {
            let body = '';
            req.on('data', chunk => body += chunk);
            req.on('end', () => {
                const params = new URLSearchParams(body);
                preferences[params.get('key')] = params.get('value');
                res.end('{}');
            });
            return;
        }
        return res.end(JSON.stringify({settings: preferences}));
    }
    if (url.pathname === '/web/view') {
        res.setHeader('Content-Type', 'text/html');
        if (url.searchParams.get('page') === 'systemsbreakdown-edit') return res.end('<html><body>Edit fixture</body></html>');
        if (url.searchParams.get('page') === 'dashboard-irls') return res.end(fs.readFileSync(path.resolve(__dirname, '../../main/resources/html-pages/dashboard-irls.html')));
        return res.end(fs.readFileSync(path.resolve(__dirname, '../../main/resources/html-pages/dashboard-srls.html')));
    }
    if (url.pathname === '/master/srlcalculation') {
        res.setHeader('Content-Type', 'application/xml');
        if (mode === 'error') { res.writeHead(500); return res.end(); }
        if (mode === 'diagnostics' || mode === 'markers' || mode === 'prl-pair') return res.end(`<SrlCalculationDocument><srlCalculation><meta>
            <lookupMaxima maxTrlId="9" maxIrlId="9"/>
            <systemsMeta><system entityId="1" code="S-1" codeLevel="1" name="Root" trlId="4" normalizedTrl="0.444"/>
            <system entityId="2" code="S-1.2" codeLevel="2" name="Child" trlId="4" normalizedTrl="0.444"/>
            <system entityId="3" code="S-1.2.3" codeLevel="3" name="Grandchild" trlId="4" normalizedTrl="0.444"/></systemsMeta>
            <srlMeta><srl srlId="1"/><srl srlId="2"/><srl srlId="3"/><srl srlId="6" code="Readiness 6"/><srl srlId="7"/><srl srlId="9"/></srlMeta></meta>
            <calculation><SystemsInterfaces>${[2, 3].map(id => `<systemInterface fromEntityId="${id}" toEntityId="${id}" irlValue="9"/>`).join('')}</SystemsInterfaces>
            <componentSrl>${mode === 'diagnostics' ? '<component entityId="1" measuredSrlId="6" weightedTrlSum="0.444" interfaceCount="1" srl="0.444" measuredSrlRange="0.377 - 0.524"/>' : mode === 'markers' ? '<component entityId="1" measuredSrlId="2"/>' : ''}${[2, 3].map(id => `<component entityId="${id}" measuredSrlId="${mode === 'prl-pair' && id === 3 ? 7 : mode === 'markers' && id === 3 ? 3 : 6}" weightedTrlSum="0.444" interfaceCount="1" srl="0.444" measuredSrlRange="0.377 - 0.524"/>`).join('')}</componentSrl>
            <srlTransitionModel><level srlId="6" referenceValue="0.444" lowerBound="0.377" upperBound="0.525" upperBoundInclusive="false"/></srlTransitionModel>
            </calculation></srlCalculation></SrlCalculationDocument>`);
        const systems = mode === 'empty' ? '' : mode === 'complete'
            ? '<system entityId="10" code="S-1" codeLevel="1" name="One"/><system entityId="2" code="S-2" codeLevel="1" name="Two"/>'
            : '<system entityId="1" code="S-1" codeLevel="1" name="Group One"/><system entityId="10" code="S-1.10" codeLevel="2" name="Ten &amp; more"/><system entityId="2" code="S-1.2" codeLevel="2" name="Two"/><system entityId="3" code="S-1.2.1" codeLevel="3" name="Missing"/><system entityId="4" code="S-2" codeLevel="1" name="Group Two"/>';
        return res.end(`<SrlCalculationDocument><TopPanel><WorkspaceHeading>SRL fixture</WorkspaceHeading><HelpFileName>dashboard-srls</HelpFileName></TopPanel><srlCalculation><meta><systemsMeta>${systems}</systemsMeta><srlMeta>${[1, 3, 4, 5, 7, 9].map(level => `<srl srlId="${level}" code="Readiness ${level}"/>`).join('')}</srlMeta></meta><calculation><componentSrl><component entityId="10" measuredSrlId="5" srl="0.300"/><component entityId="2" measuredSrlId="3" srl="0.100"/></componentSrl></calculation></srlCalculation></SrlCalculationDocument>`);
    }
    if (url.pathname === '/Menu') {
        res.setHeader('Content-Type', 'application/xml');
        return res.end(includeN2Route ? '<Menu><main-menu-item><display>Interfaces</display><url>/web/view?page=psys-interfaces</url></main-menu-item></Menu>' : '<Menu/>');
    }
    if (url.pathname === '/pro/dashboardirl') {
        res.setHeader('Content-Type', 'application/xml');
        return res.end(`<DashboardIrlDocument><dashboardIrl><trlMeta><trl trlId="1" code="1"/></trlMeta><irlMeta/><physicalStructures>${['=P-ARC-1', '=P-ARC-1.1', '=P-ARC-1.1.1', '=P-ARC-2'].map((code, i) => `<physicalStructure><entityId>${i + 1}</entityId><id>${code}</id><name>System ${i + 1}</name><trlId>1</trlId><irlCounts/></physicalStructure>`).join('')}</physicalStructures></dashboardIrl></DashboardIrlDocument>`);
    }
    const file = path.resolve(webapp, '.' + url.pathname);
    if (!file.startsWith(webapp + path.sep) || !fs.existsSync(file)) { res.writeHead(404); return res.end(); }
    res.setHeader('Content-Type', file.endsWith('.js') ? 'text/javascript' : 'text/css');
    res.setHeader('Cache-Control', 'no-store');
    if (mode === 'legacy' && file.endsWith('dashboard-srls-model.js')) {
        return res.end(fs.readFileSync(file, 'utf8').replace('levelCodes: new Map', 'legacyLevelCodes: new Map'));
    }
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
    await send('Emulation.setDeviceMetricsOverride', {width: 1500, height: 950, deviceScaleFactor: 1, mobile: false});
    await send('Page.navigate', {url: `http://127.0.0.1:${server.address().port}/web/view?page=dashboard-srls`});
    await wait("document.querySelectorAll('#srlSystems tr').length === 5");
    assert.equal(await run("document.querySelector('#srlReload')"), null);
    assert.equal(await run("document.querySelector('thead th').textContent"), 'SBS Code');
    assert.deepEqual(await run("[...document.querySelectorAll('#srlSystems tr:not([hidden]) .dashboard-srls-name')].map(n => n.textContent)"), ['Group One', 'Group Two']);
    assert.equal(await run("document.querySelector('.dashboard-srls-group-toggle').getAttribute('aria-expanded')"), 'false');
    assert.equal(await run("document.querySelector('#btnDownloadPdf').disabled"), false);
    const checkN2Button = async () => {
        await wait("document.querySelector('#btnInterfaceManagement').hidden === false");
        assert.equal(await run("document.querySelector('#btnInterfaceManagement').getAttribute('href')"), '/web/view?page=psys-interfaces');
        assert(await run(`(() => {
            const n2 = document.querySelector('#btnInterfaceManagement').getBoundingClientRect();
            const pdf = document.querySelector('#btnDownloadPdf').getBoundingClientRect();
            return n2.width === pdf.width && n2.height === pdf.height && Math.abs(n2.top - pdf.top) < 1 && n2.right < pdf.left;
        })()`), 'N2 button is placed beside PDF in the header');
    };
    await checkN2Button();
    const srlPdfButtonSize = await run("(() => {const r=document.querySelector('#btnDownloadPdf').getBoundingClientRect();return [r.width,r.height]})()");
    assert(await run("(() => {const button=document.querySelector('#btnDownloadPdf').getBoundingClientRect();const header=document.querySelector('.dashboard-srls-header').getBoundingClientRect();return Math.abs(button.right-header.right)<1})()"));
    const downloadedPdf = await run(`(async () => {
        const createUrl = URL.createObjectURL;
        let pdfBlob;
        URL.createObjectURL = blob => {pdfBlob = blob; return createUrl(blob);};
        const preventDownload = event => {if (event.target.closest('a[download]')) event.preventDefault();};
        document.addEventListener('click', preventDownload, true);
        try {
            document.querySelector('#btnDownloadPdf').click();
            return pdfBlob ? await pdfBlob.text() : null;
        } finally {
            URL.createObjectURL = createUrl;
            document.removeEventListener('click', preventDownload, true);
        }
    })()`);
    assert(downloadedPdf?.startsWith('%PDF-1.4'));
    assert(downloadedPdf.includes('(S-1.2.1) Tj'), 'PDF includes collapsed nested systems');
    assert(downloadedPdf.includes('(Group One) Tj'));
    assert.deepEqual(await run("[...document.querySelectorAll('.dashboard-srls-group-title, .dashboard-srls-group-name > .dashboard-srls-name')].map(n => getComputedStyle(n).fontWeight)"), ['800', '800', '800', '800']);
    assert.deepEqual(await run("[...document.querySelectorAll('.dashboard-srls-group-count')].map(n => n.textContent)"), ['3', '0']);
    assert.equal(await run("document.querySelector('.dashboard-srls-count-heading').textContent"), '# Subsystems');
    assert(await run(`(() => {
        const center = node => {const r = node.getBoundingClientRect(); return r.left + r.width / 2;};
        return Math.abs(center(document.querySelector('.dashboard-srls-count-heading')) - center(document.querySelector('.dashboard-srls-group-count'))) < 1;
    })()`), 'subsystem header aligns with counts');
    assert.equal(await run("getComputedStyle(document.querySelector('.dashboard-srls-summary-item--average .dashboard-srls-project-marker')).backgroundColor"), 'rgb(0, 138, 101)');
    assert.equal(await run("document.querySelectorAll('#srlSystems .dashboard-srls-summary-item').length"), 0);
    assert.match(await run("document.querySelector('.dashboard-srls-group .dashboard-srls-system-track').getAttribute('aria-label')"), /Descendant average SRL: 4/);
    assert.equal(await run("document.querySelector('.dashboard-srls-group .dashboard-srls-system-marker').style.left"), '37.5%');
    assert.match(await run("document.querySelector('.dashboard-srls-group .dashboard-srls-system-track').title"), /Descendant average SRL: 4/);
    assert.match(await run("document.querySelector('#srlSystems tr:not(.dashboard-srls-group) .dashboard-srls-system-track').title"), /Own SRL: 3.*Readiness 3\nDescendant average SRL: 4/);
    assert.equal(await run("getComputedStyle(document.querySelector('.dashboard-srls-system-marker--average')).backgroundColor"), await run("getComputedStyle(document.querySelector('.dashboard-srls-summary-item--average .dashboard-srls-project-marker')).backgroundColor"));
    assert.equal(await run("document.querySelector('.dashboard-srls-summary-item--average').title"), 'Readiness 4');
    await run("document.querySelector('.dashboard-srls-group-toggle').click()");
    assert.equal(await run("document.querySelectorAll('#srlSystems tr:not([hidden])').length"), 5);
    assert.equal(await run("document.querySelector('.dashboard-srls-group-summary')"), null);
    assert.deepEqual(await run("[...document.querySelectorAll('#srlSystems tr:not(.dashboard-srls-group-summary) th')].map(n => n.textContent)"), ['S-1.2', 'S-1.2.1', 'S-1.10']);
    assert.equal(await run("parseFloat(document.querySelector('#srlSystems th').style.paddingLeft)"), 47);
    assert.deepEqual(await run("[...document.querySelectorAll('#srlSystems th')].map(n => parseFloat(n.style.paddingLeft))"), [47, 62, 47]);
    assert(await run(`(() => {
        const originalClass = document.body.className;
        try {
            return ['', 'theme-light', 'theme-blue', 'theme-black'].every(theme => {
                document.body.className = originalClass.replace(/theme-(light|blue|black)/g, '') + ' ' + theme;
                const cells = [...document.querySelectorAll('#srlSystems tr:not(.dashboard-srls-group) th')];
                const colors = cells.map(cell => getComputedStyle(cell).backgroundColor);
                return colors[0] !== colors[1] && colors[0] === colors[2] && colors.every(color => color !== 'rgba(0, 0, 0, 0)');
            });
        } finally { document.body.className = originalClass; }
    })()`), 'expanded rows alternate background colors in all themes');
    assert(await run(`(() => {
        const headerName = document.querySelector('.dashboard-srls-group-name .dashboard-srls-name');
        const childName = document.querySelector('#srlSystems tr:not(.dashboard-srls-group) td.dashboard-srls-name');
        const range = document.createRange(); range.selectNodeContents(childName);
        return Math.abs(headerName.getBoundingClientRect().left - range.getBoundingClientRect().left) < 1;
    })()`), 'group and child system names align');
    assert.equal(await run("document.querySelector('.dashboard-srls-group-title').textContent"), 'S-1');
    await run("document.querySelector('#srlSystems tr:not(.dashboard-srls-group)').dispatchEvent(new MouseEvent('dblclick', {bubbles: true}))");
    await wait("!!document.querySelector('dialog[open] iframe')");
    const editUrl = new URL(await run("document.querySelector('dialog[open] iframe').src"));
    assert.equal(editUrl.searchParams.get('page'), 'systemsbreakdown-edit');
    assert.equal(editUrl.searchParams.get('id'), '2');
    await run("document.querySelector('dialog[open]').close()");
    const expandedScreenshot = await send('Page.captureScreenshot', {format: 'png'});
    fs.writeFileSync(path.join(output, 'dashboard-expanded.png'), Buffer.from(expandedScreenshot.data, 'base64'));
    await run("document.querySelector('.dashboard-srls-group-toggle').click()");
    assert.equal(await run("document.querySelectorAll('#srlSystems tr:not([hidden])').length"), 2);
    assert.deepEqual(await run("[...document.querySelectorAll('#srlProjectSummary .dashboard-srls-summary-item')].map(n => n.textContent)"), ['Low', 'Current Level', 'High']);
    assert.equal(await run("document.querySelectorAll('#srlProjectSummary .dashboard-srls-summary-line').length"), 1);
    assert.deepEqual(await run("[...document.querySelectorAll('#srlScale span')].map(n => n.textContent)"), ['SRL 1', 'SRL 3', 'SRL 4', 'SRL 5', 'SRL 7', 'SRL 9']);
    const checkAlignment = async () => {
        assert(await run(`(() => {
            const center = node => {const rect = node.getBoundingClientRect(); return rect.left + rect.width / 2;};
            const labels = [...document.querySelectorAll('#srlScale span')];
            return [3, 4, 5].every((level, i) => Math.abs(center(document.querySelectorAll('#srlProjectSummary .dashboard-srls-project-marker')[i]) - center(labels.find(n => n.textContent === 'SRL ' + level))) < 1)
                && Math.abs(center(document.querySelector('.dashboard-srls-group .dashboard-srls-system-marker')) - center(labels.find(n => n.textContent === 'SRL 4'))) < 1;
        })()`), 'PRL markers align with the active SRL scale');
        assert(await run(`(() => {
            const item = document.querySelector('.dashboard-srls-summary-item--average');
            const previous = item.style.left;
            item.style.left = '100%';
            const rect = item.getBoundingClientRect();
            const inside = rect.right <= item.closest('td').getBoundingClientRect().right
                && getComputedStyle(item).whiteSpace === 'nowrap';
            item.style.left = previous;
            return inside;
        })()`), 'summary labels fit on a single line at the right endpoint');
    };
    await checkAlignment();
    for (const [key, delta] of [['code', 20], ['name', 90], ['count', 15]]) {
        const position = await run(`(() => {
            const r = document.querySelector('th[data-resize-column="${key}"] .dashboard-srls-column-resizer').getBoundingClientRect();
            return {x: r.left + r.width / 2, y: r.top + r.height / 2};
        })()`);
        await send('Input.dispatchMouseEvent', {type: 'mousePressed', ...position, button: 'left', clickCount: 1});
        await send('Input.dispatchMouseEvent', {type: 'mouseMoved', x: position.x + delta, y: position.y, buttons: 1});
        await send('Input.dispatchMouseEvent', {type: 'mouseReleased', x: position.x + delta, y: position.y, button: 'left', clickCount: 1});
        await checkAlignment();
    }
    for (let i = 0; i < 100 && JSON.parse(preferences['dashboard.srls.columnWidths'] || '{}').count !== 155; i++) await pause(20);
    assert.deepEqual(JSON.parse(preferences['dashboard.srls.columnWidths']), {code: 180, name: 370, count: 155});
    await send('Page.reload');
    await wait("document.querySelectorAll('#srlSystems tr').length === 5");
    assert.deepEqual(await run("['code','name','count'].map(key => parseFloat(getComputedStyle(document.querySelector('th[data-resize-column=\"' + key + '\"]')).width))"), [180, 370, 155]);
    await checkAlignment();
    await run("document.querySelector('.dashboard-srls-group-toggle').click()");
    assert(await run(`(() => {
        const name = document.querySelector('#srlSystems tr:not(.dashboard-srls-group) .dashboard-srls-name');
        name.textContent = 'A very long system name that must remain on a single line even in a narrow column';
        const style = getComputedStyle(name);
        return style.whiteSpace === 'nowrap' && style.textOverflow === 'ellipsis' && style.overflow === 'hidden';
    })()`), 'System Name remains on one line');
    assert.equal(await run("document.querySelector('#pageHeading').textContent"), 'SRL fixture');
    assert.equal(await run("document.querySelectorAll('.dashboard-srls-system-marker').length"), 6);
    assert.equal(await run("document.querySelector('#srlWorkspace').hidden"), false);
    assert(await run("document.querySelector('.dashboard-srls-frame').getBoundingClientRect().bottom <= innerHeight"));
    const screenshot = await send('Page.captureScreenshot', {format: 'png'});
    fs.writeFileSync(path.join(output, 'dashboard.png'), Buffer.from(screenshot.data, 'base64'));
    await send('Emulation.setDeviceMetricsOverride', {width: 390, height: 844, deviceScaleFactor: 1, mobile: false});
    await checkAlignment();
    await run("document.querySelector('.dashboard-srls-scroll').scrollLeft = 300");
    await checkAlignment();
    assert(await run("document.querySelector('.dashboard-srls-scroll').scrollWidth > document.querySelector('.dashboard-srls-scroll').clientWidth"));
    mode = 'error';
    await send('Page.reload');
    await wait("document.querySelector('#srlStatus').textContent.includes('HTTP 500')");
    mode = 'empty';
    await send('Page.reload');
    await wait("document.querySelector('#srlStatus').textContent.includes('No active systems')");
    assert.equal(await run("document.querySelector('#srlWorkspace').hidden"), true);
    mode = 'normal';
    await send('Page.reload');
    await wait("!document.querySelector('#srlWorkspace').hidden");
    assert.equal(await run("document.querySelector('.dashboard-srls-group-toggle').getAttribute('aria-expanded')"), 'false');
    mode = 'complete';
    await send('Page.reload');
    await wait("!document.querySelector('#srlWorkspace').hidden && document.querySelectorAll('#srlSystems tr').length === 2");
    assert.equal(await run("document.querySelector('#srlStatus').textContent"), '');
    assert.equal(await run("getComputedStyle(document.querySelector('#srlStatus')).display"), 'none');
    mode = 'legacy';
    await send('Page.reload');
    await wait("!document.querySelector('#srlWorkspace').hidden && document.querySelectorAll('#srlSystems tr').length === 5");
    assert.match(await run("document.querySelector('.dashboard-srls-group .dashboard-srls-system-track').title"), /Descendant average SRL: 4/);
    const srlHeaderTypography = await run(`(() => {
        const typography = node => { const s = getComputedStyle(node); return [s.fontFamily, s.fontSize, s.fontWeight, s.lineHeight, s.color]; };
        return [...document.querySelectorAll('.dashboard-srls-table thead th[data-resize-column], #srlScale span')].map(typography);
    })()`);
    mode = 'diagnostics';
    await send('Page.reload');
    await wait("document.querySelectorAll('#srlSystems tr').length === 3");
    for (const selector of ['.dashboard-srls-group', '.dashboard-srls-group-name', '.dashboard-srls-group-name .dashboard-srls-name', '.dashboard-srls-group .dashboard-srls-system-track', '.dashboard-srls-group .dashboard-srls-system-marker']) {
        assert.match(await run(`document.querySelector('${selector}').title`), /SRL calculation \(temporary test details\): S-1\n/,
            'Level 1 calculation details are available on the collapsed group row, name and marker');
    }
    await run("document.querySelector('.dashboard-srls-group-toggle').click()");
    for (const selector of ['#srlSystems tr:not(.dashboard-srls-group)', '#srlSystems tr:last-child']) {
        const tooltip = await run(`document.querySelector('${selector} .dashboard-srls-system-track').title`);
        assert.match(tooltip, /Readiness 6/);
        assert.match(tooltip, /9\/9 × 4\/9/);
        assert.match(tooltip, /unrounded AA \/ 1 ≈ 0\.444/);
        assert.equal(await run(`document.querySelector('${selector}').title`), tooltip);
        assert.match(await run(`document.querySelector('${selector} .dashboard-srls-name').title`), /SRL calculation/);
    }
    assert.match(await run("document.querySelector('.dashboard-srls-group .dashboard-srls-system-track').title"), /Descendant average SRL: 6/);
    assert.deepEqual(await run(`([...document.querySelectorAll('#srlSystems tr:not(.dashboard-srls-group) .dashboard-srls-system-track')].map(track => {
        const marker = track.querySelector('.dashboard-srls-system-marker--combined');
        return [track.querySelectorAll('.dashboard-srls-system-marker').length, marker?.style.left,
            marker ? getComputedStyle(marker).backgroundImage : null];
    }))`), [[1, '62.5%', 'linear-gradient(to right, rgb(0, 138, 101) 50%, rgb(220, 38, 38) 50%)'],
        [1, '62.5%', 'linear-gradient(to right, rgb(0, 138, 101) 50%, rgb(220, 38, 38) 50%)']],
    'Matching SRLs render one dot with green on the left and red on the right');
    assert.deepEqual(await run("[...document.querySelectorAll('#srlProjectSummary .dashboard-srls-summary-item')].map(n => n.textContent)"), ['Low / Current Level / High']);
    assert(await run(`(() => {
        const style = getComputedStyle(document.querySelector('.dashboard-srls-summary-item--combined .dashboard-srls-project-marker'));
        return style.backgroundImage === 'linear-gradient(to right, rgb(0, 138, 101) 50%, ' + style.borderTopColor + ' 50%)';
    })()`));
    mode = 'prl-pair';
    await send('Page.reload');
    await wait("document.querySelector('#srlProjectSummary')?.textContent.includes('Current Level / High') && document.querySelectorAll('#srlProjectSummary .dashboard-srls-summary-item').length === 2");
    assert.deepEqual(await run("[...document.querySelectorAll('#srlProjectSummary .dashboard-srls-summary-item')].map(n => n.textContent)"), ['Low', 'Current Level / High']);
    mode = 'markers';
    await send('Page.reload');
    await wait("document.querySelectorAll('#srlSystems tr').length === 3");
    await run("document.querySelector('.dashboard-srls-group-toggle').click()");
    assert.deepEqual(await run(`(() => {
        const tracks = [...document.querySelectorAll('#srlSystems .dashboard-srls-system-track')];
        return tracks.map(track => [
            track.querySelector('.dashboard-srls-system-marker:not(.dashboard-srls-system-marker--average)')?.style.left,
            track.querySelector('.dashboard-srls-system-marker--average')?.style.left || null,
            track.querySelector('.dashboard-srls-progress').style.width
        ]);
    })()`), [['12.5%', '50%', '50%'], ['62.5%', '50%', '62.5%'], ['25%', '50%', '50%']],
    'Own XML level stays red, every descendant inherits the level 1 green mean, and each line reaches the rightmost marker');
    mode = 'normal';
    await send('Page.navigate', {url: `http://127.0.0.1:${server.address().port}/web/view?page=dashboard-irls`});
    await wait("document.querySelectorAll('.dashboard-irls-row').length === 4");
    await checkN2Button();
    assert.deepEqual(await run("(() => {const r=document.querySelector('#btnDownloadPdf').getBoundingClientRect();return [r.width,r.height]})()"), srlPdfButtonSize, 'PDF button size matches IRL');
    assert.deepEqual(await run("[...document.querySelectorAll('.dashboard-irls-row')].map(row => [...row.querySelectorAll('.dashboard-irls-cell--id, .dashboard-irls-cell--name')].map(cell => getComputedStyle(cell).fontWeight))"), [['800', '800'], ['400', '400'], ['400', '400'], ['800', '800']]);
    const irlHeaderTypography = await run(`(() => {
        const s = getComputedStyle(document.querySelector('.dashboard-irls-header-label'));
        return [s.fontFamily, s.fontSize, s.fontWeight, s.lineHeight, s.color];
    })()`);
    for (const typography of srlHeaderTypography) assert.deepEqual(typography, irlHeaderTypography, 'SRL header typography matches IRL');
    includeN2Route = false;
    for (const page of ['dashboard-srls', 'dashboard-irls']) {
        await send('Page.navigate', {url: `http://127.0.0.1:${server.address().port}/web/view?page=${page}`});
        await wait("document.querySelector('#menuStatus')?.textContent.includes('Ingen menupunkter')");
        assert.equal(await run("getComputedStyle(document.querySelector('#btnInterfaceManagement')).display"), 'none', 'N2 hidden when route is not available');
    }
    console.log('Passed: collapsed groups, descendant summaries, SRL alignment, responsive scrolling, API error/empty states, reload recovery and no placeholder dash after successful calculations.');
}
main().catch(error => { console.error(error); process.exitCode = 1; }).finally(() => { socket?.close(); browser?.kill(); server.close(); });
