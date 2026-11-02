import fs from 'node:fs';
import {connect,wait} from './ui3-cdp.mjs';
const c=await connect();
const routes=['Dashboard','CatatanKebiasaan','JadwalTidur','Tantangan','Rekomendasi','StatistikMingguan','EvaluasiMingguan','ProfilPengaturan','Login','CreateAccount','Onboarding','LandingPage','KatalogLatihan','DetailLatihan'];
const titles=['Rutinitas Sehat Hari Ini','Kebiasaan harian','Jadwal tidur','Tantangan','Rekomendasi','Statistik','Evaluasi','Profil','Masuk','Buat','Waktu Luang','Sehat','Katalog latihan','ID RepDB:'];
const records=[];const start=Number(process.argv[2]||0),end=Number(process.argv[3]||routes.length),selected=process.argv[4]?new Set(process.argv[4].split(",").map(Number)):null;
async function clickText(text) {
 const rect=await c.evaluate(`(()=>{const e=[...document.querySelectorAll('*')].find(e=>e.childElementCount===0&&e.textContent===${JSON.stringify(text)}&&e.getBoundingClientRect().width>0);if(!e)return null;let r=e.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2};})()`);
 if(!rect)throw Error('Control missing '+text);await c.click(rect.x,rect.y);await wait(200);
}
async function awaitHeader(route,state){for(let j=0;j<70;j++){const text=await c.evaluate('document.body.innerText');if(text.includes('RUTE: '+route+' | '+state))return;await wait(150);}throw Error('Route did not settle '+route+' '+state);}
async function settled(){for(let i=0;i<35;i++){let text=await c.evaluate('document.body.innerText');if(!text.includes('Memuat katalog lokal…'))return text;await wait(150);}throw Error('Catalog still loading');}
try {
 await c.send('Page.reload',{});await wait(6000);await c.resize(1440);
 for(let skip=0;skip<start;skip++){await clickText('Rute berikut');await awaitHeader(routes[skip+1],'filled');}
 for(let i=start;i<end;i++) {
  if(i>start){await c.resize(1440);await clickText('Rute berikut');await wait(500);}
  if(selected && !selected.has(i))continue;
  const states=i>=8 && i<=11?['filled']:['filled','empty','failed'];
  for(const state of states) {
   await c.resize(1440);await clickText({filled:'Isi',empty:'Kosong',failed:'Gagal'}[state]);await awaitHeader(routes[i],state);await wait(450);let text=await settled();
   if(!text.includes('RUTE: '+routes[i]+' | '+state))throw Error('Wrong route/state: '+text.slice(0,120));
   for(const width of [1440,768,390,360]) {
    await c.resize(width);await wait(300);text=await settled();
    const name=routes[i]+'-'+state+'-'+width;
    await c.shot(name);
    const metrics=await c.evaluate(`(()=>({width:innerWidth,scrollWidth:document.documentElement.scrollWidth,inputs:[...document.querySelectorAll('input')].map(e=>({label:e.getAttribute('aria-label'),type:e.type})),renderPending:document.querySelector('jpro-app')?.getAttribute('data-render-pending')}))()`);
    if(metrics.scrollWidth>width+1)throw Error('Document horizontal overflow '+name);
    await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:Math.floor(width*.7),y:width>=768?820:700,deltaY:10000,deltaX:0});await wait(300);
    if(width<768)await c.shot(name+'-bottom');
    records.push({route:routes[i],state,width,screenshot:'browser-'+name+'.png',metrics,textIncludesExpected:state==='filled'?text.toLowerCase().includes(titles[i].toLowerCase()):null,textLength:text.length});
    await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:Math.floor(width*.7),y:width>=768?820:700,deltaY:-10000,deltaX:0});await wait(200);
   }
   console.log('BROWSER_MATRIX '+routes[i]+' '+state);
  }
 }
 fs.writeFileSync(`docs/screenshots/ui-3/browser-matrix-${selected?"selected":start+"-"+end}.json`,JSON.stringify({generatedAt:new Date().toISOString(),browser:await c.send('Browser.getVersion'),records},null,2));
} finally {fs.writeFileSync('/tmp/ui3-matrix-progress.json',JSON.stringify(records,null,2));c.ws.close();}
