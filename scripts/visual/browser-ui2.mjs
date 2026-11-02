import fs from 'node:fs';
const targets=await (await fetch('http://127.0.0.1:9228/json')).json();
const ws=new WebSocket(targets.find(t=>t.type==='page').webSocketDebuggerUrl);
await new Promise(r=>ws.addEventListener('open',r,{once:true}));
let id=0;const pending=new Map();
ws.addEventListener('message',e=>{let m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(m.error):p.resolve(m.result);}});
const send=(method,params={})=>new Promise((resolve,reject)=>{const n=++id;pending.set(n,{resolve,reject});ws.send(JSON.stringify({id:n,method,params}));});
await send('Page.enable');await send('Runtime.enable');
await send('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false});

const wait=ms=>new Promise(r=>setTimeout(r,ms));
const click=async(x,y)=>{await send('Input.dispatchMouseEvent',{type:'mousePressed',x,y,button:'left',clickCount:1});await send('Input.dispatchMouseEvent',{type:'mouseReleased',x,y,button:'left',clickCount:1});};
const key=async(key,code,n)=>{await send('Input.dispatchKeyEvent',{type:'keyDown',key,code,windowsVirtualKeyCode:n});await send('Input.dispatchKeyEvent',{type:'keyUp',key,code,windowsVirtualKeyCode:n});};
const shot=async(name)=>{const im=await send('Page.captureScreenshot',{format:'png'});fs.writeFileSync('docs/screenshots/ui-2/browser-'+name+'.png',Buffer.from(im.data,'base64'));};
// Continue the existing fixture instance, preserving browser interaction state.
const routes=['Dashboard','CatatanKebiasaan','JadwalTidur','Tantangan','Rekomendasi','StatistikMingguan','EvaluasiMingguan','ProfilPengaturan','Login','CreateAccount','Onboarding','LandingPage'];
for(let i=0;i<routes.length;i++) {
 await send('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false});await wait(600);
 await click(405,20);await key('Home','Home',36);
 for(let j=0;j<i;j++)await key('ArrowDown','ArrowDown',40);
 await key('Enter','Enter',13);await wait(1300);
 for(const width of [1440,768,390,360]) {
  await send('Emulation.setDeviceMetricsOverride',{width,height:width>=768?1000:844,deviceScaleFactor:1,mobile:false});await wait(700);
  await shot(routes[i]+'-'+width);
 }
 console.log('BROWSER '+routes[i]);
}
ws.close();
