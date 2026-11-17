import fs from 'node:fs';
import {connect,wait} from './latihan-cdp.mjs';
const phase=process.env.HOME_PHASE||'after', mode=process.argv[2]||'capture';
const out='docs/verification/beranda-visual',shots=`docs/screenshots/beranda-visual/${phase}`;
fs.mkdirSync(shots,{recursive:true});
const c=await connect(),evidence={phase,started:new Date().toISOString(),screens:[],routes:[]};
const body=()=>c.evaluate('document.body.innerText');
const reset=()=>c.evaluate("window.scrollTo(0,0);document.querySelectorAll('.jpro-application').forEach(e=>e.scrollTop=0)");
async function wheel(y){await reset();await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:280,y:420,deltaX:0,deltaY:y});await wait(350);}
async function top(){await wheel(-25000);}
async function until(text){for(let i=0;i<80;i++){if((await body()).includes(text))return;await wait(500);}throw Error('Missing '+text+'\n'+await body());}
async function click(text){await reset();for(let i=0;i<18;i++){let p=await c.evaluate(`(()=>{let a=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent.trim()===${JSON.stringify(text)});let r=a.map(e=>e.getBoundingClientRect()).find(r=>r.width&&r.y>=0&&r.bottom<innerHeight-64);return r?{x:r.x+r.width/2,y:r.y+r.height/2}:null})()`);if(p){await c.click(p.x,p.y);await wait(700);return;}await wheel(300);}throw Error('Cannot click '+text);}
async function resize(w){await c.send('Emulation.setDeviceMetricsOverride',{width:w,height:w===1440?1000:844,deviceScaleFactor:1,mobile:false});await wait(1000);await reset();}
async function shot(name){await reset();await wait(400);const r=await c.send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(`${shots}/${name}.png`,Buffer.from(r.data,'base64'));evidence.screens.push({name,...await c.evaluate(`({width:innerWidth,height:innerHeight,scrollWidth:document.documentElement.scrollWidth,text:document.body.innerText,images:[...document.querySelectorAll('img')].map(e=>({src:e.getAttribute('src'),complete:e.complete,w:e.naturalWidth,rect:e.getBoundingClientRect().toJSON()}))})`)});}
async function home(){await resize(1440);await click('Beranda');await until('Lihat rutinitas');await top();await resize(390);}
try{
if(mode!=='failed'){await c.send('Page.reload');await resize(1440);await until('Masukkan kata sandi Anda');await click('misal: nadia@sehatharian.id');await c.send('Input.insertText',{text:process.env.HOME_EMAIL||'alpha@example.test'});await click('Masukkan kata sandi Anda');await c.send('Input.insertText',{text:'fixture123'});await c.key('Enter','Enter',13);await until('Profil');await top();}
if(mode==='failed'){await home();await until('Progres gagal dimuat.');await top();await shot('beranda-gagal-390');if(!(await body()).includes('Gagal dimuat'))throw Error('Failure state failed');}
if(mode==='capture'){
 for(const w of [390,360,1440]){await resize(w);await top();await shot(`beranda-${w}`);}
 await resize(390);await wheel(480);await shot('beranda-scroll-390');await resize(1440);await top();await click('Tema terang / gelap');for(const w of [390,360,1440]){await resize(w);await top();await shot(`beranda-gelap-${w}`);}
}
if(mode==='recovered'){await resize(390);await top();await until('4/4 tercatat');await shot('beranda-pulih-390');}
if(mode==='empty'){await resize(390);await top();await shot('beranda-kosong-390');if(!(await body()).includes('0/4 tercatat')||!(await body()).includes('Belum tercatat'))throw Error('Empty state failed');}
if(mode==='carousel'){
 await resize(390);await top();await wheel(260);await shot('tantangan-sebelum-geser-390');
 await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:220,y:430,deltaX:220,deltaY:0});await wait(700);await shot('tantangan-sesudah-geser-390');
 evidence.horizontalOverflow=await c.evaluate('document.documentElement.scrollWidth>innerWidth');
}
if(mode==='routes'){
 for(const [label,expected] of [['Lihat rutinitas','Kebiasaan harian'],['Tidur','Catatan tidur aktual'],['Hidrasi','Kapasitas gelas'],['Nutrisi','Catat makanan'],['Aktivitas','Program'],['Lihat semua','Tantangan & bacaan'],['Buka tantangan','Tantangan & bacaan'],['Lihat progres','Progres kebiasaan']]){
  await home();await click(label);await until(expected);evidence.routes.push({label,expected,passed:true,text:await body()});
 }
}
}catch(e){evidence.error=String(e);throw e;}finally{fs.writeFileSync(`${out}/${phase}-${mode}.json`,JSON.stringify(evidence,null,2));c.ws.close();}
