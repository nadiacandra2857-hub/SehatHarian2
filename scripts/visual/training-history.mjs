import fs from 'node:fs';
import {execFileSync} from 'node:child_process';
import {connect,wait} from './latihan-cdp.mjs';
const out='docs/verification/riwayat-latihan',shots='docs/screenshots/riwayat-latihan';
const mode=process.argv[2]||'flow',db=process.env.HISTORY_DB;
if(!db?.startsWith('sehatharian_test_history_'))throw Error('Disposable history schema required');
const c=await connect(),e={mode,db,screens:[]};
const sql=q=>execFileSync('mariadb',['--no-defaults','-h127.0.0.1','-P13317','-uroot','--batch','--skip-column-names',db,'-e',q],{encoding:'utf8'}).trim();
const owner="(SELECT id FROM pengguna WHERE email='alpha@example.test')";
const count=()=>Number(sql('SELECT COUNT(*) FROM aktivitas WHERE pengguna_id='+owner));
const body=()=>c.evaluate('document.body.innerText');
const reset=()=>c.evaluate("window.scrollTo(0,0);document.querySelectorAll('.jpro-application').forEach(e=>e.scrollTop=0)");
async function wheel(deltaY){await reset();await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:280,y:420,deltaX:0,deltaY});await wait(250);}
async function top(){await wheel(-25000);}
async function until(s){for(let i=0;i<170;i++){if((await body()).includes(s))return;await wait(500);}throw Error('Missing '+s+'\n'+await body());}
async function click(s,index=0){await reset();for(let i=0;i<35;i++){const p=await c.evaluate(`(()=>{let a=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent.trim()===${JSON.stringify(s)}).map(e=>e.getBoundingClientRect()).filter(r=>r.width&&r.y>=0&&r.bottom<innerHeight-(innerWidth<768?62:5));let r=a[${index}];return r?{x:r.x+r.width/2,y:r.y+r.height/2}:null})()`);if(p){await c.click(p.x,p.y);await wait(700);return;}await wheel(300);}throw Error('Cannot click '+s+'\n'+await body());}
async function resize(w){await c.resize(w);await top();}
async function shot(name){await reset();await wait(500);if((await body()).includes('Reconnecting'))throw Error('Reconnect overlay');const r=await c.send('Page.captureScreenshot',{format:'png'});fs.mkdirSync(shots,{recursive:true});fs.writeFileSync(`${shots}/${name}.png`,Buffer.from(r.data,'base64'));e.screens.push({name,...await c.evaluate('({width:innerWidth,height:innerHeight,scrollWidth:document.documentElement.scrollWidth,text:document.body.innerText})')});}
async function modalFits(width){
 const close=await c.evaluate("(()=>{const e=[...document.querySelectorAll('*')].find(e=>e.childElementCount===0&&e.textContent==='Tutup');return e?.getBoundingClientRect().toJSON()})()");
 e.modalSizing??=[];e.modalSizing.push({width,close});
 if(!close||close.x<0||close.right>width||close.y<0||close.bottom>(width>=768?1000:844))throw Error('Dialog close control outside viewport');
}
async function login(email='alpha@example.test'){await c.send('Page.reload');await resize(1440);await until('Masukkan kata sandi Anda');await click('misal: nadia@sehatharian.id');await c.send('Input.insertText',{text:email});await click('Masukkan kata sandi Anda');await c.send('Input.insertText',{text:'fixture123'});await c.key('Enter','Enter',13);await until('Keluar ke Beranda');await click('Latihan');await until('Riwayat latihan');}
try{
if(mode==='inspect'){console.log(await body());console.log(await c.evaluate("[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent.includes('Legacy')).map(e=>({text:e.textContent,rect:e.getBoundingClientRect().toJSON()}))"));}
if(mode==='flow'){
 await login();e.activityBefore=count();await click('Program Riwayat · lihat program');await click('Gunakan program & tinjau');await resize(390);await click('Mulai persiapan');await wait(5800);await click('Konfirmasi selesai / berikutnya');await until('Waktu berakhir · konfirmasi untuk lanjut');await click('Konfirmasi selesai / berikutnya');await until('Durasi aktif:');await top();await shot('ringkasan-390');e.activityBeforeSave=count();if(e.activityBeforeSave!==e.activityBefore)throw Error('implicit activity insert');await click('Simpan aktivitas');await until('Data berhasil disimpan.');e.activityAfter=count();if(e.activityAfter!==e.activityBefore+1)throw Error('activity count');
 e.saved=sql('SELECT t.id,t.operation_id,t.activity_id,t.active_nanos,(SELECT SUM(s.active_nanos) FROM training_step s WHERE s.session_id=t.id),t.program_name FROM training_session t WHERE t.pengguna_id='+owner+' ORDER BY t.id DESC LIMIT 1');
 await click('Riwayat latihan');await until('Lihat rincian sesi');await top();await shot('daftar-390');await click('Lihat rincian sesi');await until('Rincian saat latihan dilakukan');await top();await shot('detail-390');await wheel(400);await shot('langkah-390');
 for(const width of [360,1440]){await resize(width);await top();await shot('detail-'+width);await click('Kembali ke riwayat');await until('Lihat rincian sesi');await top();await shot('daftar-'+width);await click('Lihat rincian sesi');await until('Rincian saat latihan dilakukan');}
 await resize(1440);await top();await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:130,y:700,deltaX:0,deltaY:1500});await wait(500);await click('Tema terang / gelap');await resize(390);await top();await shot('detail-gelap-390');await click('Kembali ke riwayat');await until('Lihat rincian sesi');await top();await shot('daftar-gelap-390');
}
if(mode==='states'){
 await login('beta@example.test');await click('Riwayat latihan');await until('Belum ada riwayat latihan pada periode ini.');await resize(390);await shot('kosong-390');
 await login();
 try{
  sql('RENAME TABLE training_session TO training_session_unavailable');
  await click('Riwayat latihan');await until('Riwayat latihan gagal dimuat.');await resize(390);await shot('gagal-390');
 }finally{sql('RENAME TABLE training_session_unavailable TO training_session');}
 await click('Coba muat kembali');await until('Lihat rincian sesi');await top();await shot('pulih-390');

}
if(mode==='legacy'){
 await login();await resize(1440);await click('Catatan Kebiasaan');await click('Aktivitas');await top();
 await c.click(700,760);await c.key('Home','Home',36);
 for(let index=0;index<30;index++){
  const rect=await c.evaluate("(()=>{const e=[...document.querySelectorAll('*')].find(e=>e.childElementCount===0&&e.textContent==='Legacy training · rincian');return e?.getBoundingClientRect().toJSON()})()");
  if(rect&&rect.y>735&&rect.bottom<878)break;
  await c.key('ArrowDown','ArrowDown',40);await wait(250);
 }
 await wait(500);await shot('catatan-aktivitas-1440');
 await click('Legacy training · rincian');await until('Rincian latihan belum tersedia');
 for(const width of [390,360,1440]){await resize(width);await modalFits(width);await shot('legacy-'+width);}
 await click('Tutup');await c.click(700,760);await c.key('Home','Home',36);await wait(700);await click('Latihan SehatHarian · rincian');await until('Program Riwayat');
 for(const width of [390,360,1440]){await resize(width);await modalFits(width);await shot('tautan-detail-'+width);}
}
if(mode==='dialog-dark'){
 await click('Tutup');await resize(1440);await click('Tema terang / gelap');
 await c.click(700,760);await c.key('Home','Home',36);await wait(600);await click('Latihan SehatHarian · rincian');await until('Program Riwayat');await resize(390);await modalFits(390);await shot('tautan-detail-gelap-390');
}
if(mode==='relogin'){
 await login();await click('Riwayat latihan');await until('Lihat rincian sesi');await click('Lihat rincian sesi');await until('Rincian saat latihan dilakukan');await resize(390);await top();await shot('detail-apres-relogin-390');e.text=await body();if(!e.text.includes('Program Riwayat'))throw Error('persisted browser session missing');
}
}catch(err){e.error=String(err);e.body=await body();throw err;}finally{fs.writeFileSync(`${out}/browser-${mode}.json`,JSON.stringify(e,null,2));c.ws.close();}
