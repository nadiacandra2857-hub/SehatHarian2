import fs from 'node:fs';
import {execFileSync} from 'node:child_process';
import {connect,wait} from './latihan-cdp.mjs';
const c=await connect();const out='docs/verification/rombak',shots='docs/screenshots/rombak';fs.mkdirSync(shots,{recursive:true});
const text=()=>c.evaluate('document.body.innerText');
async function wheel(delta){await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:280,y:470,deltaX:0,deltaY:delta});await wait(500);}
async function click(value,index=0){for(let i=0;i<16;i++){let r=await c.evaluate(`(()=>{let a=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent.trim()===${JSON.stringify(value)}).map(e=>e.getBoundingClientRect()).filter(r=>r.width&&r.y>=0&&r.bottom<innerHeight);let r=a[${index}];return r?{x:r.x+r.width/2,y:r.y+r.height/2}:null})()`);if(r){await c.click(r.x,r.y);await wait(700);return;}await wheel(400);}throw Error('Tidak terjangkau '+value+'\n'+await text());}
async function top(){await wheel(-25000);}
async function until(value,seconds=20){for(let i=0;i<seconds*2;i++){if((await text()).includes(value))return;await wait(500);}throw Error('Tidak muncul '+value+'\n'+await text());}
async function shot(name){await wait(500);const r=await c.send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(`${shots}/${name}.png`,Buffer.from(r.data,'base64'));}
async function matrix(name){const sizes=[];for(let w of [360,390,768,1440]){await c.resize(w);await top();await shot(`${name}-${w}`);sizes.push(await c.evaluate(`({width:innerWidth,scrollWidth:document.documentElement.scrollWidth,height:innerHeight,text:document.body.innerText})`));}fs.writeFileSync(`${out}/browser-${name}.json`,JSON.stringify(sizes,null,2));}
try{
const mode=process.argv[2]||'inspect';
if(mode==='release-smoke'){
 await c.send('Page.reload');await c.resize(1440);await until('Masukkan kata sandi Anda',40);await click('misal: nadia@sehatharian.id');await c.send('Input.insertText',{text:'nadia@sehatharian.local'});await click('Masukkan kata sandi Anda');await c.send('Input.insertText',{text:'password123'});await c.key('Enter','Enter',13);await until('Halo, Nadia',30);await shot('release-home-light-1440');await click('Tema terang / gelap');await shot('release-home-dark-1440');await click('Tema terang / gelap');await c.resize(390);await shot('release-home-light-390');
 fs.writeFileSync(`${out}/extraction-browser.json`,JSON.stringify({login:true,themes:true,url:'http://localhost:8088',text:await text()},null,2));
}
if(mode==='reload'){await c.send('Page.reload');await wait(4000);}
if(mode==='inspect')console.log(await text());
if(mode==='login'){await c.resize(1440);await until('Masukkan kata sandi Anda');await matrix('login');await click('misal: nadia@sehatharian.id');await c.send('Input.insertText',{text:'alpha@example.test'});await click('Masukkan kata sandi Anda');await c.send('Input.insertText',{text:'fixture123'});await c.key('Enter','Enter',13);await until('Halo, Fixture',30);await matrix('beranda');}
if(mode==='pages'){
 for(let [label,name] of [['Nutrisi','nutrisi'],['Hidrasi','hidrasi'],['Jadwal & Alarm Tidur','tidur'],['Statistik Mingguan','progres'],['Tantangan Harian','tantangan'],['Latihan','latihan']]){
  await c.resize(1440);await top();await click(label);await wait(1500);await matrix(name);
 }
}
if(mode==='video'){
 await c.resize(390);await top();await click('Lihat gerakan · Jalan Kaki');await until('Target detik / repetisi');await shot('video-detail-390');await click('Tambahkan ke sesi');await top();await click('Tinjau sesi');await shot('tinjauan-390');await click('Mulai persiapan');await wait(5500);await click('Konfirmasi selesai / berikutnya');await wait(1800);await shot('video-timer-390');
 const media=await c.evaluate(`Array.from(document.querySelectorAll('video')).map(v=>({src:v.currentSrc,paused:v.paused,loop:v.loop,readyState:v.readyState,currentTime:v.currentTime,videoWidth:v.videoWidth,videoHeight:v.videoHeight,rect:(()=>{let r=v.getBoundingClientRect();return {x:r.x,y:r.y,w:r.width,h:r.height}})()}))`);fs.writeFileSync(`${out}/browser-media.json`,JSON.stringify(media,null,2));console.log(media);await click('Jeda');await shot('video-jeda-390');await matrix('video-timer-paused');
}
if(mode==='published-session'){
 await c.resize(1440);await top();await click('Program Uji · lihat program');await click('Gunakan program & tinjau');await shot('program-review-final-1440');
 const db=process.env.ROMBAK_DB||'sehatharian_test_rombak_20261110b';if(!db.startsWith('sehatharian_test_rombak_'))throw Error('test DB required');
 const sql=q=>execFileSync('mariadb',['--no-defaults','-h127.0.0.1','-P13317','-uroot','--batch','--skip-column-names',db,'-e',q],{encoding:'utf8'}).trim();
 const count=()=>Number(sql("SELECT COUNT(*) FROM aktivitas WHERE pengguna_id=(SELECT id FROM pengguna WHERE email='alpha@example.test')"));
 const evidence={db,before:count()};await click('Mulai persiapan');await wait(5500);await click('Konfirmasi selesai / berikutnya');await wait(2000);await shot('program-active-final-1440');
 evidence.media=await c.evaluate("Array.from(document.querySelectorAll('video')).map(v=>({paused:v.paused,loop:v.loop,time:v.currentTime,ready:v.readyState}))");
 await c.resize(390);await shot('program-active-final-390');for(let i=0;i<6;i++){await wait(10000);console.log('timer wait '+(i+1));}
 await click('Konfirmasi selesai / berikutnya');await until('Durasi aktif:');await top();await shot('program-summary-final-390');evidence.summary=await text();evidence.beforeSave=count();if(evidence.beforeSave!==evidence.before)throw Error('implicit write');await click('Simpan aktivitas');await wait(1000);evidence.afterSave=count();evidence.saved=await text();if(evidence.afterSave!==evidence.before+1)throw Error('save count mismatch');await shot('program-saved-final-390');fs.writeFileSync(`${out}/browser-session-final.json`,JSON.stringify(evidence,null,2));
}
if(mode==='admin'){
 // Separate tab/instance, preserving the existing user's session.
 await c.send('Page.navigate',{url:'http://localhost:8088'});await wait(4000);await c.resize(1440);await until('Masukkan kata sandi Anda');await click('misal: nadia@sehatharian.id');await c.send('Input.insertText',{text:'admin@example.test'});await click('Masukkan kata sandi Anda');await c.send('Input.insertText',{text:'fixture123'});await c.key('Enter','Enter',13);await until('Halo, Fixture');await click('Admin konten');await until('Kelola konten');await matrix('admin');
}
if(mode==='editor'){
 await c.resize(390);await top();await click('Buat konten baru');await until('Konten baru');await shot('admin-editor-390');
}
if(mode==='auth-onboarding'){
 await c.resize(390);await click('Lainnya');await click('Keluar');await until('Masuk');await click('Masuk');await until('Masukkan kata sandi Anda');await click('Daftar');await matrix('registrasi');
 // Account Beta was registered in the isolated fixture and intentionally has no onboarding.
 await click('Masuk');await until('Masukkan kata sandi Anda');await c.resize(1440);await click('misal: nadia@sehatharian.id');await c.send('Input.insertText',{text:'beta@example.test'});await click('Masukkan kata sandi Anda');await c.send('Input.insertText',{text:'fixture123'});await c.key('Enter','Enter',13);await until('Berapa lama waktu luang');await matrix('onboarding');
}
if(mode==='shot')await shot(process.argv[3]||'manual');
}finally{c.ws.close();}
