import fs from 'node:fs';
import {connect,wait} from './latihan-cdp.mjs';
const c=await connect();
const wheel=async dy=>{await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:700,y:600,deltaX:0,deltaY:dy});await wait(300);};
async function click(text){for(let i=0;i<15;i++){const r=await c.evaluate(`(()=>{const r=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent.trim()===${JSON.stringify(text)}).map(e=>e.getBoundingClientRect()).filter(r=>r.width&&r.y>0&&r.bottom<innerHeight).at(-1);return r?{x:r.x+r.width/2,y:r.y+r.height/2}:null})()`);if(r){await c.click(r.x,r.y);await wait(500);return;}await wheel(400);}throw Error('Tidak terlihat: '+text);}
async function shot(name){const dir=process.env.UX_SHOTS||'docs/screenshots/ux-latihan-20261106';fs.mkdirSync(dir,{recursive:true});let r=await c.send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(dir+'/'+name+'.png',Buffer.from(r.data,'base64'));}
try{
const mode=process.argv[2];
if(mode==='enter'){await c.key('Enter','Enter',13);await wait(2000);}
else if(mode==='login'){
 await c.resize(1440);await click('misal: nadia@sehatharian.id');await c.send('Input.insertText',{text:'nadia@sehatharian.local'});await click('Masukkan kata sandi Anda');await c.send('Input.insertText',{text:'password123'});await c.key('Enter','Enter',13);await wait(2000);await shot('home-1440');
}else if(mode==='catalog'){await click('Latihan · katalog RepDB');await wait(1200);await shot('catalog-1440');}
else if(mode==='second'){await wheel(500);await click('Lihat detail');await wheel(-10000);await click('Tambahkan ke sesi');}
else if(mode==='choose'){await click('Lihat detail');await wheel(-10000);await click('Tambahkan ke sesi');await shot('target');}
else if(mode==='timed'){await click('30');for(let i=0;i<30;i++)await c.key('ArrowUp','ArrowUp',38);await click('Tambahkan ke sesi');await wheel(-10000);await click('Kembali ke katalog');await shot('first-selected');}
else if(mode==='rep'){await click('Mode repetisi (selesai dikonfirmasi manual)');await click('Tambahkan ke sesi');await wheel(-10000);await click('Tinjau sesi');await shot('review-1440');}
else if(mode==='add'){await click('Tambahkan ke sesi');await wheel(-10000);await shot('selected');}
else if(mode==='review'){await click('Tinjau sesi');await wheel(-10000);await shot('review-1440');}
else if(mode==='run1'){await click('Mulai persiapan');await wait(6000);await click('Konfirmasi selesai / berikutnya');await wait(3000);await click('Jeda');await shot('paused-1440');await wait(2000);await click('Lanjutkan');await shot('active-1440');await wait(35000);}
else if(mode==='run2'){await wait(30000);await click('Konfirmasi selesai / berikutnya');await wheel(-10000);await shot('rest-1440');await wait(16000);await click('Konfirmasi selesai / berikutnya');await wait(3000);await click('Konfirmasi selesai / berikutnya');await wheel(-10000);await shot('summary-1440');}
else if(mode==='start'){await click('Mulai persiapan');await wait(6000);await shot('active-1440');}
else if(mode==='click'){await click(process.argv[3]);await shot('state');}
else if(mode==='matrix'){for(const w of [1440,768,390,360]){await c.resize(w);await shot('catalog-'+w);}}
else if(mode==='shot'){await shot(process.argv[3]);}
else if(mode==='dom'){console.log(await c.evaluate(`Array.from(document.querySelectorAll('input')).map(e=>({html:e.outerHTML,rect:(()=>{let r=e.getBoundingClientRect();return {x:r.x,y:r.y,w:r.width,h:r.height}})()}))`));console.log(await c.evaluate(`(()=>{let e=[...document.querySelectorAll('*')].find(e=>e.childElementCount===0&&e.textContent.trim()==='Pilih atau tulis nama aktivitas');return e?.parentElement?.parentElement?.parentElement?.outerHTML.slice(0,8000)})()`));}
else if(mode==='text'){console.log(await c.evaluate('document.body.innerText'));}
else if(mode==='resize'){await c.resize(Number(process.argv[3]));await shot(process.argv[4]||('viewport-'+process.argv[3]));}
else if(mode==='up'){await wheel(-10000);}
else if(mode==='down'){await wheel(Number(process.argv[3]||600));}
else if(mode==='tab'){await c.key('Tab','Tab',9);console.log(await c.evaluate('({tag:document.activeElement.tagName,html:document.activeElement.outerHTML})'));await shot('focus-'+process.argv[3]);}
else if(mode==='point'){await c.click(Number(process.argv[3]),Number(process.argv[4]));}
else if(mode==='reload'){await c.send('Page.reload');await wait(3000);}
else if(mode==='styles'){const rows=await c.evaluate(`Array.from(document.querySelectorAll('*')).filter(e=>e.childElementCount===0&&e.textContent.trim()).map(e=>{let s=getComputedStyle(e),r=e.getBoundingClientRect();let chain=[];for(let n=e;n;n=n.parentElement){let c=getComputedStyle(n);chain.push({tag:n.tagName,opacity:c.opacity,fill:c.fill,color:c.color,background:c.backgroundColor});}return {text:e.textContent.trim(),rect:{x:r.x,y:r.y,w:r.width,h:r.height},style:{color:s.color,fill:s.fill,opacity:s.opacity},chain};}).filter(e=>e.rect.w&&e.rect.y>=0&&e.rect.y<innerHeight)`);fs.writeFileSync('docs/verification/ux-styles-'+process.argv[3]+'.json',JSON.stringify(rows,null,2));}

console.log(await c.evaluate('document.body.innerText'));
}finally{c.ws.close();}
