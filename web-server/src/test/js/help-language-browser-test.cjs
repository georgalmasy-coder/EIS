// Run from the repository root. Real browser/modules with an in-memory preference API.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const {spawn} = require('node:child_process');
const webapp = path.resolve('web-server/src/main/webapp');
const output = path.resolve('web-server/target/help-language-test');
fs.mkdirSync(output, {recursive: true});
let developmentMode = true, failSave = false;
const settings = {};
const writes = [];
const server = http.createServer(async (req, res) => {
    const url = new URL(req.url, 'http://localhost');
    if (url.pathname === '/api/user-preferences') {
        res.setHeader('Content-Type', 'application/json');
        if (req.method === 'POST') {
            let text = '';
            for await (const chunk of req) text += chunk;
            const form = new URLSearchParams(text);
            writes.push([form.get('key'), form.get('value')]);
            if (failSave) { res.statusCode = 500; return res.end('{"error":"Test save failed"}'); }
            settings[form.get('key')] = form.get('value');
            return res.end('{"ok":true}');
        }
        return res.end(JSON.stringify({settings}));
    }
    if (url.pathname === '/Menu') {
        res.setHeader('Content-Type', 'application/xml');
        return res.end(`<menu development-mode="${developmentMode}"><main-menu-item><display>[USER-MENU]</display>
            <submain-menu-item><display>My profile</display><url>/profile</url></submain-menu-item>
            <submain-menu-item><display>Logout</display><url>/logout</url></submain-menu-item>
            </main-menu-item></menu>`);
    }
    if (url.pathname === '/api/help') {
        const language = developmentMode && settings['eis.language'] === 'dk' ? 'dk' : 'en';
        res.setHeader('Content-Type', 'text/markdown; charset=UTF-8');
        return res.end(fs.readFileSync(path.join(webapp, 'WEB-INF/help', language, 'myprofile-edit.md')));
    }
    if (url.pathname === '/') {
        res.setHeader('Content-Type', 'text/html; charset=UTF-8');
        return res.end(`<!doctype html><meta charset="utf-8"><link rel="stylesheet" href="/css/menu.css">
            <aside id="sideMenu" class="side-menu"><h2>Menu</h2><div id="menuStatus"></div><ul id="menuRoot"></ul></aside>
            <main class="app"><input id="unsaved" value="Keep my changes"><button data-help-page="myprofile-edit">Help</button></main>
            <dialog id="helpDialog"><h2 id="helpDialogTitle"></h2><div id="helpDialogContent"></div><button id="helpDialogOkButton">OK</button></dialog>
            <script type="module">import {initMenu} from '/js/components/menu.js';
            import {initHelpDialog} from '/js/components/help-dialog.js';
            window.alertMessages=[];window.alert=message=>alertMessages.push(message);
            initHelpDialog();await initMenu();window.ready=true;</script>`);
    }
    const file = path.resolve(webapp, '.' + url.pathname);
    if (!file.startsWith(webapp + path.sep) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
        res.statusCode = 404; return res.end();
    }
    res.setHeader('Content-Type', file.endsWith('.js') ? 'text/javascript' : file.endsWith('.svg') ? 'image/svg+xml' : 'text/css');
    res.end(fs.readFileSync(file));
});
let browser, socket;
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
async function main() {
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
    const origin = `http://127.0.0.1:${server.address().port}`;
    const profile = fs.mkdtempSync(path.join(output, 'profile-'));
    browser = spawn(process.env.EIS_TEST_BROWSER || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
        ['--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check', '--remote-debugging-port=0', `--user-data-dir=${profile}`, 'about:blank'],
        {windowsHide: true, stdio: 'ignore'});
    const activePort = path.join(profile, 'DevToolsActivePort');
    for (let i = 0; i < 200 && !fs.existsSync(activePort); i++) await pause(50);
    const port = fs.readFileSync(activePort, 'utf8').split('\n')[0];
    let page;
    for (let i = 0; i < 100 && !page; i++) {
        const pages = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
        page = pages.find(p => p.type === 'page');
        if (!page) await pause(50);
    }
    socket = new WebSocket(page.webSocketDebuggerUrl);
    await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
    let id = 0;
    const pending = new Map();
    socket.onmessage = event => {
        const result = JSON.parse(event.data);
        if (!result.id) return;
        const handler = pending.get(result.id);
        pending.delete(result.id);
        result.error ? handler.reject(new Error(result.error.message)) : handler.resolve(result.result);
    };
    const send = (method, params = {}) => new Promise((resolve, reject) => {
        const requestId = ++id;
        pending.set(requestId, {resolve, reject});
        socket.send(JSON.stringify({id: requestId, method, params}));
    });
    const run = async expression => {
        const result = await send('Runtime.evaluate', {expression, returnByValue: true, awaitPromise: true});
        if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails));
        return result.result.value;
    };
    const wait = async expression => {
        for (let i = 0; i < 160; i++) { if (await run(expression)) return; await pause(50); }
        throw new Error('Timed out: ' + expression);
    };
    const navigate = async () => {
        await send('Page.navigate', {url: origin + '/'});
        await wait('window.ready === true');
    };
    const help = async expected => {
        await run("document.querySelector('[data-help-page]').click()");
        await wait(`document.querySelector('#helpDialogContent h1')?.textContent === ${JSON.stringify(expected)}`);
        await run("document.getElementById('helpDialogOkButton').click()");
    };
    await send('Page.enable');
    await send('Emulation.setDeviceMetricsOverride', {width: 1280, height: 900, deviceScaleFactor: 1, mobile: false});
    await navigate();
    assert.equal(await run("document.querySelectorAll('.menu-language-choice').length"), 2);
    assert.deepEqual(await run("[...document.querySelectorAll('.menu-footer-sub-menu-list li')].map(li=>li.textContent.trim())"), ['My profile', 'Logout', 'Dansk', 'Engelsk✓']);
    assert.equal(await run("document.querySelector('[data-language=en]').getAttribute('aria-pressed')"), 'true');
    await run("document.querySelector('.menu-footer-more').click()");
    await wait("[...document.querySelectorAll('.menu-language-choice img')].every(img=>img.complete && img.naturalWidth>0)");
    const screenshot = await send('Page.captureScreenshot', {format: 'png'});
    fs.writeFileSync(path.join(output, 'language-menu.png'), Buffer.from(screenshot.data, 'base64'));
    await help('My Profile');
    await run("document.querySelector('[data-language=dk]').click()");
    await wait("document.querySelector('[data-language=dk]').getAttribute('aria-pressed') === 'true'");
    assert.equal(settings['eis.language'], 'dk');
    assert.deepEqual(writes.at(-1), ['eis.language', 'dk']);
    assert.equal(await run("document.getElementById('unsaved').value"), 'Keep my changes');
    await help('Min profil');
    await navigate();
    assert.equal(await run("document.querySelector('[data-language=dk]').getAttribute('aria-pressed')"), 'true');
    await help('Min profil');
    await run("document.querySelector('[data-language=en]').click()");
    await wait("document.querySelector('[data-language=en]').getAttribute('aria-pressed') === 'true'");
    await help('My Profile');
    failSave = true;
    await run("document.querySelector('[data-language=dk]').click()");
    await wait('window.alertMessages.length === 1');
    assert.equal(await run("document.querySelector('[data-language=en]').getAttribute('aria-pressed')"), 'true');
    assert.equal(settings['eis.language'], 'en');
    await help('My Profile');
    developmentMode = false;
    settings['eis.language'] = 'dk';
    await navigate();
    assert.equal(await run("document.querySelectorAll('.menu-language-choice').length"), 0);
    assert.equal(await run("document.querySelectorAll('.menu-footer-sub-menu-list a').length"), 2);
    await help('My Profile');
    console.log('Passed: development-only menu, flags, English default, saved Danish/English selection, reload persistence, help language, failed save, production English.');
}
main().catch(error => { console.error(error); process.exitCode = 1; })
    .finally(() => { socket?.close(); browser?.kill(); server.close(); });
