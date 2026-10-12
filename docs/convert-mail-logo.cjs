// Convert the existing SVG with the browser renderer, preserving fonts and transparency.
const fs=require('node:fs'), path=require('node:path'), {spawn}=require('node:child_process');
const target=path.resolve('common/target'); fs.mkdirSync(target,{recursive:true});
const profile=fs.mkdtempSync(path.join(target,'logo-browser-'));
const edge=spawn(process.env.EIS_TEST_BROWSER || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    ['--headless=new','--disable-gpu','--no-first-run','--remote-debugging-port=0',`--user-data-dir=${profile}`,'about:blank'],{windowsHide:true,stdio:'ignore'});
let socket; const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms));
async function main(){
    const active=path.join(profile,'DevToolsActivePort');
    for(let i=0;i<200&&!fs.existsSync(active);i++)await sleep(50);
    const port=fs.readFileSync(active,'utf8').split('\n')[0];
    const pages=await(await fetch(`http://127.0.0.1:${port}/json/list`)).json();
    socket=new WebSocket(pages.find(page=>page.type==='page').webSocketDebuggerUrl);
    await new Promise((resolve,reject)=>{socket.onopen=resolve;socket.onerror=reject;});
    let id=0;const pending=new Map();
    socket.onmessage=event=>{const result=JSON.parse(event.data);if(result.id){const handlers=pending.get(result.id);pending.delete(result.id);result.error?handlers.reject(result.error):handlers.resolve(result.result);}};
    const send=(method,params={})=>new Promise((resolve,reject)=>{const requestId=++id;pending.set(requestId,{resolve,reject});socket.send(JSON.stringify({id:requestId,method,params}));});
    await send('Page.enable');
    await send('Emulation.setDeviceMetricsOverride',{width:280,height:64,deviceScaleFactor:4,mobile:false});
    await send('Emulation.setDefaultBackgroundColorOverride',{color:{r:0,g:0,b:0,a:0}});
    const svg=fs.readFileSync('web-server/src/main/webapp/svg/eis-logo-light.svg','utf8');
    await send('Page.navigate',{url:'data:image/svg+xml;base64,'+Buffer.from(svg).toString('base64')});
    for(let i=0;i<100;i++){const result=await send('Runtime.evaluate',{expression:"document.readyState==='complete' && !!document.querySelector('svg')",returnByValue:true});if(result.result.value)break;await sleep(50);}
    const result=await send('Page.captureScreenshot',{format:'png',clip:{x:0,y:0,width:280,height:64,scale:1}});
    const output='common/src/main/resources/mail-assets/eis-logo-light.png';fs.mkdirSync(path.dirname(output),{recursive:true});
    fs.writeFileSync(output,Buffer.from(result.data,'base64'));console.log('Created '+output);
}
main().catch(error=>{console.error(error);process.exitCode=1;}).finally(()=>{socket?.close();edge.kill();});
