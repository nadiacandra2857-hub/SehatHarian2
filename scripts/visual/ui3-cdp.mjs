import fs from 'node:fs';
export const wait=ms=>new Promise(r=>setTimeout(r,ms));
export async function connect(targetId = process.env.UI3_TARGET_ID) {
 const browserMode=targetId==='browser';
 const targets=browserMode?[]:await(await fetch('http://127.0.0.1:9228/json')).json();
 const target=browserMode?await(await fetch('http://127.0.0.1:9228/json/version')).json():targets.find(t=>t.type==='page' && t.url.includes('8087') && (!targetId || t.id===targetId));
 if(!target)throw Error('No UI3 browser target');
 const ws=new WebSocket(target.webSocketDebuggerUrl);await new Promise(r=>ws.addEventListener('open',r,{once:true}));
 let id=0;const pending=new Map();
 ws.addEventListener('message',e=>{const m=JSON.parse(e.data);if(m.id){let p=pending.get(m.id);pending.delete(m.id);if(p)m.error?p.reject(m.error):p.resolve(m.result);}});
 const send=(method,params={})=>new Promise((resolve,reject)=>{let n=++id;pending.set(n,{resolve,reject});ws.send(JSON.stringify({id:n,method,params}));});
 const evaluate=async expression=>{let r=await send('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(r.exceptionDetails)throw Error(JSON.stringify(r.exceptionDetails));return r.result.value;};
 const click=async(x,y)=>{await send('Input.dispatchMouseEvent',{type:'mousePressed',x,y,button:'left',clickCount:1});await wait(60);await send('Input.dispatchMouseEvent',{type:'mouseReleased',x,y,button:'left',clickCount:1});};
 const key=async(key,code,n,modifiers=0)=>{await send('Input.dispatchKeyEvent',{type:'keyDown',key,code,windowsVirtualKeyCode:n,modifiers});await send('Input.dispatchKeyEvent',{type:'keyUp',key,code,windowsVirtualKeyCode:n,modifiers});};
 const resize=async width=>{await send('Emulation.setDeviceMetricsOverride',{width,height:width>=768?1000:844,deviceScaleFactor:1,mobile:false});await wait(600);};
 const shot=async name=>{fs.mkdirSync('docs/screenshots/ui-3',{recursive:true});const r=await send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(`docs/screenshots/ui-3/browser-${name}.png`,Buffer.from(r.data,'base64'));};
 if(!browserMode){await send('Page.enable');await send('Runtime.enable');}
 return {ws,send,evaluate,click,key,resize,shot};
}
