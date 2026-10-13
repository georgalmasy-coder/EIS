// Standalone browser test with a mock API; no server jobs or emails are started.
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const {spawn} = require('node:child_process');
const assert = require('node:assert/strict');
const root = process.cwd();
const output = path.join(root, 'web-server/target/mail-template-browser-test');
fs.mkdirSync(output, {recursive:true});

function fixtureRows() {
    const logo = 'data:image/png;base64,' + fs.readFileSync('common/src/main/resources/mail-assets/eis-logo-light.png').toString('base64');
    const definitions = [
        ['CUSTOMER_CONFIRMATION', 'Customer Confirmation'],
        ['CUSTOMER_REACTIVATED', 'Customer Reactivated'],
        ['CUSTOMER_SUSPENDED', 'Customer Suspended'],
        ['PASSWORD_RESET', 'Password Reset'],
        ['PAYMENT_FAILED', 'Payment Failed'],
        ['PAYMENT_OVERDUE', 'Payment Overdue'],
        ['PAYMENT_REQUESTED', 'Payment Requested'],
        ['SUBSCRIPTION_CONTINUATION_CONFIRMATION', 'Subscription Continuation Confirmation'],
        ['SUBSCRIPTION_EXPIRING', 'Subscription Expiring'],
        ['SUBSCRIPTION_RENEWAL_CONFIRMATION', 'Subscription Renewal Confirmation'],
        ['SYSTEM_NOTIFICATION', 'System Notification'],
        ['TRIAL_EXPIRING', 'Trial Expiring'],
        ['USER_CREATED', 'User Created']
    ];
    return definitions.map(([key, name]) => ({
        key, name, subject: key === 'SYSTEM_NOTIFICATION' ? '' : name,
        bodyHtml: `<html><body><table data-eis-banner="true" style="width:100%;background:#073f34;"><tr><td style="width:120px;padding:18px 12px;"><img src="${logo}" width="110" height="26"></td><td style="padding:18px 8px;text-align:center;color:#ffffff;font-size:18px;font-weight:bold;white-space:nowrap;">${name}</td><td style="width:120px;padding:18px 12px;"></td></tr></table><p>Hello {{customerName}}, this is an example template for ${name}.</p><a href="{{resetLink}}">Continue</a></body></html>`,
        contentType: 'text/html; charset=UTF-8',
        sourceFileName: key.toLowerCase().replaceAll('_', '-') + '.mail',
        placeholdersJson: '["customerName","resetLink"]', revision: 1, updatedAt: '2026-10-12T09:00:00Z'
    }));
}
const rows=fixtureRows(); let saved=null, conflict=false, testMail=null, importedCount=0;
const bundle = () => ({format:'eis-mail-templates',version:1,templates:rows.map(({key,name,subject,bodyHtml,contentType,placeholdersJson})=>({key,name,subject,bodyHtml,contentType,placeholders:JSON.parse(placeholdersJson)}))});
function example(text) { return text.replace(/\{\{(\w+)\}\}/g, (_, token) => token.endsWith('Link') ? 'https://example.invalid/demo' : token==='customerName' ? 'Example Ltd' : `[Example: ${token}]`); }
const server=http.createServer(async(req,res)=>{
    const url=new URL(req.url,'http://localhost');
    if(url.pathname==='/api/admin/mail-templates') {
        res.setHeader('Content-Type','application/json');
        if(req.method==='GET' && url.searchParams.get('action')==='export') return res.end(JSON.stringify(bundle()));
        if(req.method==='POST') {
            let body=''; for await(const chunk of req) body+=chunk;
            const edit=JSON.parse(body), row=rows.find(row=>row.key===edit.key);
            assert.equal(req.headers['x-csrf-token'],'test-token');
            if(url.searchParams.get('action')==='import') { assert.equal(edit.format,'eis-mail-templates');assert.equal(edit.templates.length,13);importedCount++;return res.end(JSON.stringify({message:'13 templates imported.',imported:13})); }
            if(edit.action==='preview') return res.end(JSON.stringify({subject:example(edit.subject),bodyHtml:example(edit.bodyHtml)}));
            if(edit.action==='test') { testMail=edit; return res.end(JSON.stringify({message:`Test email queued for ${edit.recipientEmail}.`,mailId:101})); }
            if(conflict) { res.statusCode=409; return res.end(JSON.stringify({message:'Someone else has changed this template.'})); }
            saved=edit; Object.assign(row,edit,{revision:row.revision+1}); return res.end(JSON.stringify(row));
        }
        const key=url.searchParams.get('key');
        return res.end(JSON.stringify(key?rows.find(row=>row.key===key):{templates:rows.map(row=>({...row,bodyHtml:null})),csrfToken:'test-token'}));
    }
    if(url.pathname==='/Menu') { res.setHeader('Content-Type','application/xml'); return res.end('<menu><items/></menu>'); }
    let file = url.pathname==='/web/view' ? path.join(root,'web-server/src/main/resources/html-pages/admin/mail-templates.html') : path.join(root,'web-server/src/main/webapp',url.pathname);
    if(!file.startsWith(root) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {res.statusCode=404;return res.end();}
    res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.svg')?'image/svg+xml':'text/html');
    res.end(fs.readFileSync(file));
});
let edge, socket;
const pause=ms=>new Promise(resolve=>setTimeout(resolve,ms));
async function main(){
    await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
    const port=server.address().port;
    const executable=process.env.EIS_TEST_BROWSER || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
    const profile=fs.mkdtempSync(path.join(output,'profile-'));
    edge=spawn(executable,['--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port=0',`--user-data-dir=${profile}`,'about:blank'],{windowsHide:true,stdio:'ignore'});
    const active=path.join(profile,'DevToolsActivePort');
    for(let i=0;i<200&&!fs.existsSync(active);i++) await pause(50);
    const debugPort=fs.readFileSync(active,'utf8').split('\n')[0];
    let pages;
    for(let i=0;i<100;i++) { try{pages=await(await fetch(`http://127.0.0.1:${debugPort}/json/list`)).json();if(pages.some(page=>page.type==='page'))break;}catch{}await pause(50); }
    socket=new WebSocket(pages.find(page=>page.type==='page').webSocketDebuggerUrl);
    await new Promise((resolve,reject)=>{socket.onopen=resolve;socket.onerror=reject;});
    let id=0;const pending=new Map();
    socket.onmessage=event=>{const result=JSON.parse(event.data);if(result.id){const handlers=pending.get(result.id);pending.delete(result.id);result.error?handlers.reject(new Error(result.error.message)):handlers.resolve(result.result);}};
    const send=(method,params={})=>new Promise((resolve,reject)=>{const requestId=++id;pending.set(requestId,{resolve,reject});socket.send(JSON.stringify({id:requestId,method,params}));});
    const run=async expression=>{const result=await send('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(result.exceptionDetails)throw new Error(JSON.stringify(result.exceptionDetails));return result.result.value;};
    const wait=async expression=>{for(let i=0;i<160;i++){if(await run(expression))return;await pause(50);}throw new Error(`Timed out: ${expression}`);};
    await send('Page.enable');
    await send('Emulation.setDeviceMetricsOverride',{width:1440,height:1080,deviceScaleFactor:1,mobile:false});
    await send('Page.navigate',{url:`http://127.0.0.1:${port}/web/view?page=mail-templates`});
    await wait("document.querySelectorAll('#templateRows tr').length===13");
    await run("window.exportedBlobUrl=null;const originalClick=HTMLAnchorElement.prototype.click;HTMLAnchorElement.prototype.click=function(){if(this.download==='eis-mail-templates.json'){window.exportedBlobUrl=this.href;return;}return originalClick.call(this);};document.getElementById('exportTemplates').click();");
    await wait("!!window.exportedBlobUrl");
    const exported=await run("(async()=>await(await fetch(window.exportedBlobUrl)).json())()");
    assert.equal(exported.format,'eis-mail-templates');assert.equal(exported.templates.length,13);assert(exported.templates[0].bodyHtml.includes('data:image/png;base64,'));
    const exchangeFile=path.join(output,'exchange.json');fs.writeFileSync(exchangeFile,JSON.stringify(exported));
    const dom=await send('DOM.getDocument');const input=await send('DOM.querySelector',{nodeId:dom.root.nodeId,selector:'#templateImportFile'});
    await send('DOM.setFileInputFiles',{nodeId:input.nodeId,files:[exchangeFile]});
    await wait("document.getElementById('importDialog').open");
    assert.equal(importedCount,0);await run("document.getElementById('cancelImport').click();document.getElementById('templateImportFile').value='';");
    await send('DOM.setFileInputFiles',{nodeId:input.nodeId,files:[exchangeFile]});
    await wait("document.getElementById('importDialog').open");
    await run("document.getElementById('applyImport').click();");
    await wait("document.getElementById('listMessage').textContent==='13 templates imported.'");assert.equal(importedCount,1);
    for (const row of rows.filter(row => row.subject)) {
        await run(`document.getElementById('templateSearch').value=${JSON.stringify(row.name)};document.getElementById('templateSearch').dispatchEvent(new Event('input'));document.querySelector('#templateRows button').click();`);
        await wait("document.getElementById('templateDialog').open && !document.getElementById('saveTemplate').disabled");
        await run("document.getElementById('showTemplate').click();");
        await wait("document.getElementById('previewDialog').open && !document.getElementById('templatePreview').hidden && document.getElementById('templatePreview').contentDocument?.body?.dataset.previewReady==='true'");
        assert(await run("document.getElementById('templatePreview').contentDocument.body.innerText.trim().length > 20"), `Preview actually renders ${row.key}`);
        if(row.key==='PAYMENT_REQUESTED') { const shot=await send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(path.join(output,'payment-preview.png'),Buffer.from(shot.data,'base64')); }
        await run("document.getElementById('backToTemplate').click();document.getElementById('cancelTemplate').click();");
    }
    await run("document.getElementById('templateSearch').value='Subscription Continuation';document.getElementById('templateSearch').dispatchEvent(new Event('input'));document.querySelector('#templateRows tr').dispatchEvent(new MouseEvent('dblclick',{bubbles:true}));");
    await wait("document.getElementById('templateDialog').open && !document.getElementById('saveTemplate').disabled");
    const headingLayout = await run("(() => {const doc=document.getElementById('templateEditor').contentDocument;const heading=doc.querySelector('[data-eis-banner] td:nth-child(2)');const range=doc.createRange();range.selectNodeContents(heading);return {nowrap:doc.defaultView.getComputedStyle(heading).whiteSpace,height:range.getBoundingClientRect().height,width:doc.documentElement.scrollWidth,viewport:doc.documentElement.clientWidth,oldBlue:doc.body.innerHTML.includes('#102a43')};})()");
    assert.equal(headingLayout.nowrap,'nowrap'); assert(headingLayout.height < 25); assert(!headingLayout.oldBlue); assert(headingLayout.width <= headingLayout.viewport);
    await run("document.getElementById('showTemplate').click();");
    await wait("document.getElementById('previewDialog').open && !document.getElementById('templatePreview').hidden");
    const longHeadingScreenshot=await send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(path.join(output,'continuation-preview.png'),Buffer.from(longHeadingScreenshot.data,'base64'));
    await run("document.getElementById('backToTemplate').click();document.getElementById('cancelTemplate').click();");
    await run("document.getElementById('templateSearch').value='User Created';document.getElementById('templateSearch').dispatchEvent(new Event('input'));document.querySelector('#templateRows tr').dispatchEvent(new MouseEvent('dblclick',{bubbles:true}));");
    await wait("document.getElementById('templateDialog').open && !document.getElementById('saveTemplate').disabled");
    assert.equal(await run("document.getElementById('templateEditor').contentDocument.querySelectorAll('svg').length"),0);
    await wait("document.getElementById('templateEditor').contentDocument.querySelector('[data-eis-banner] img')?.naturalWidth > 0");
    await run("const doc=document.getElementById('templateEditor').contentDocument;doc.querySelector('[data-eis-banner] td:nth-child(2)').textContent='Welcome to edited EIS';doc.body.dispatchEvent(new Event('input',{bubbles:true}));document.getElementById('templateSubject').value='Edited subject';document.getElementById('templateSubject').dispatchEvent(new Event('input'));document.getElementById('showTemplate').click();");
    await wait("document.getElementById('previewDialog').open && !document.getElementById('templatePreview').hidden");
    const preview=await run("document.getElementById('templatePreview').srcdoc");
    assert(preview.includes('Welcome to edited EIS'));assert(!preview.includes('{{resetLink}}'));assert(!/<a[^>]+href=/i.test(preview));
    await run("document.getElementById('testPreview').click();");
    assert.equal(await run("document.getElementById('testRecipient').value"),'');
    await run("document.getElementById('testRecipient').value='review@example.test';document.getElementById('sendTest').click();");
    await wait("document.getElementById('testMessage').textContent.includes('Test email queued for review@example.test')");
    assert.equal(testMail.subject,'Edited subject'); assert(testMail.bodyHtml.includes('Welcome to edited EIS'));assert.equal(testMail.recipientEmail,'review@example.test'); assert.equal(saved,null);
    await run("document.getElementById('cancelTest').click();");
    const screenshot=await send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(path.join(output,'preview.png'),Buffer.from(screenshot.data,'base64'));
    await run("document.getElementById('backToTemplate').click();document.getElementById('saveTemplate').click();");
    await wait("document.getElementById('editMessage').textContent==='Template saved.'");
    assert.equal(saved.subject,'Edited subject');assert(saved.bodyHtml.includes('Welcome to edited EIS'));assert(saved.bodyHtml.includes('{{resetLink}}'));assert(!saved.bodyHtml.includes('contenteditable='));
    conflict=true;
    await run("document.getElementById('templateSubject').value='Conflict';document.getElementById('templateSubject').dispatchEvent(new Event('input'));document.getElementById('saveTemplate').click();");
    await wait("document.getElementById('editMessage').textContent.includes('Someone else')");
    assert.equal(saved.subject,'Edited subject');
    console.log('Browser tests passed: all 12 populated previews rendered, repeated opening, unsaved test email with manually entered recipient, save, conflict.');
}
main().catch(error=>{console.error(error);process.exitCode=1;}).finally(()=>{socket?.close();edge?.kill();server.close();});
