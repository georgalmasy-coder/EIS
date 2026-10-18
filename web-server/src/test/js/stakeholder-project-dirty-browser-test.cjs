const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{spawn}=require('node:child_process');
const assert=require('node:assert/strict');
const base=path.resolve(__dirname,'../../main');
const out=path.resolve(__dirname,'../../../target/dirty-browser');fs.mkdirSync(out,{recursive:true});
const server=http.createServer((req,res)=>{const u=new URL(req.url,'http://localhost');
 if(u.pathname==='/fixture'){res.setHeader('Content-Type','text/html');return res.end(fs.readFileSync(path.join(base,'resources/html-pages',u.searchParams.get('page')+'-edit.html'),'utf8').replace(/<script src="https:[^>]*><\/script>/g,'').replaceAll('../js/','/js/').replaceAll('../css/','/css/'));}
 if(u.pathname==='/project'||u.pathname==='/basis/stakeholder'){const type=u.pathname.includes('stakeholder')?'stakeholder':'project';res.setHeader('Content-Type','application/xml');return res.end(`<Root><${type}Document><${type}><EntityId control="hidden">1</EntityId><Name control="text" display="true" editable="true">Original</Name></${type}></${type}Document><EntityNotes/><EntityAttachments/></Root>`);}
 if(u.pathname.startsWith('/js/')||u.pathname.startsWith('/css/')){const f=path.join(base,'webapp',u.pathname);if(fs.existsSync(f)){res.setHeader('Content-Type',f.endsWith('.js')?'text/javascript':'text/css');return res.end(fs.readFileSync(f));}}
 res.setHeader('Content-Type','application/json');res.end('{}');});
let browser,socket;const pause=ms=>new Promise(r=>setTimeout(r,ms));
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));const profile=fs.mkdtempSync(path.join(out,'profile-'));
 browser=spawn('C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',['--headless=new','--disable-gpu','--no-first-run','--remote-debugging-port=0',`--user-data-dir=${profile}`,'about:blank'],{windowsHide:true,stdio:'ignore'});
 const active=path.join(profile,'DevToolsActivePort');for(let i=0;i<200&&!fs.existsSync(active);i++)await pause(50);
 const port=fs.readFileSync(active,'utf8').split('\n')[0];const pages=await(await fetch(`http://127.0.0.1:${port}/json/list`)).json();
 socket=new WebSocket(pages.find(p=>p.type==='page').webSocketDebuggerUrl);await new Promise((r,j)=>{socket.onopen=r;socket.onerror=j;});
 let seq=0;const pending=new Map();socket.onmessage=e=>{const v=JSON.parse(e.data);if(v.id){const p=pending.get(v.id);pending.delete(v.id);v.error?p.j(v.error):p.r(v.result);}};
 const send=(method,params={})=>new Promise((r,j)=>{const id=++seq;pending.set(id,{r,j});socket.send(JSON.stringify({id,method,params}));});
 const run=async expression=>{const v=await send('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(v.exceptionDetails)throw Error(JSON.stringify(v.exceptionDetails));return v.result.value;};
 await send('Runtime.enable');
 for(const page of ['project','stakeholder']){await send('Page.navigate',{url:`http://127.0.0.1:${server.address().port}/fixture?page=${page}&id=1`});for(let i=0;i<100;i++){if(await run("['Loaded','Error'].includes(document.getElementById('loadStatus')?.textContent)"))break;await pause(50);}
 console.log(page,await run("({status:document.getElementById('loadStatus').textContent,detail:document.getElementById('dlgStatus')?.textContent,disabled:document.getElementById('btnSave').disabled,fields:document.getElementById('basisInfoFields').innerHTML})"));
 assert.equal(await run("document.getElementById('btnSave').disabled"),true);
 assert.equal(await run("getComputedStyle(document.getElementById('btnSave')).opacity"),'0.55',page + ': disabled Save must look disabled');
 assert.equal(await run("getComputedStyle(document.getElementById('btnSave')).cursor"),'not-allowed');
 console.log('after change',await run("(()=>{const f=document.querySelector('#basisInfoFields [data-field=Name]');f.value='Changed';f.dispatchEvent(new Event('input',{bubbles:true}));return document.getElementById('btnSave').disabled})()"));
 assert.equal(await run("document.getElementById('btnSave').disabled"),false);
 assert.equal(await run("getComputedStyle(document.getElementById('btnSave')).opacity"),'1',page + ': dirty Save must look enabled');
 assert.equal(await run("(()=>{const f=document.querySelector('#basisInfoFields [data-field=Name]');f.value='Original';f.dispatchEvent(new Event('input',{bubbles:true}));return document.getElementById('btnSave').disabled})()"),true);
 console.log(page + ': real page load, edit and revert passed');
 }
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(()=>{socket?.close();browser?.kill();server.close();});
