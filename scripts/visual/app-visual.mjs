import fs from 'node:fs';
import {execFileSync} from 'node:child_process';
import {connect,wait} from './latihan-cdp.mjs';
const phase=process.env.APP_PHASE||'after',mode=process.argv[2]||'matrix',out='docs/verification/visual-lanjutan',shots=`docs/screenshots/visual-lanjutan/${phase}`;
fs.mkdirSync(shots,{recursive:true});const c=await connect(),e={phase,mode,screens:[]};
const body=()=>c.evaluate('document.body.innerText');
const reset=()=>c.evaluate("window.scrollTo(0,0);document.querySelectorAll('.jpro-application').forEach(e=>e.scrollTop=0)");
async function wheel(d){await reset();await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:280,y:420,deltaX:0,deltaY:d});await wait(250);}
async function top(){await wheel(-25000);}
async function until(s){for(let i=0;i<180;i++){if((await body()).includes(s))return;await wait(500);}throw Error('Missing '+s+'\n'+await body());}
async function click(s,index=0){await reset();for(let i=0;i<30;i++){const p=await c.evaluate(`(()=>{let a=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent.trim()===${JSON.stringify(s)}).map(e=>e.getBoundingClientRect()).filter(r=>r.width&&r.y>=0&&r.bottom<innerHeight-62);let r=a[${index}];return r?{x:r.x+r.width/2,y:r.y+r.height/2}:null})()`);if(p){await c.click(p.x,p.y);await wait(900);return;}await wheel(300);}throw Error('Cannot click '+s+'\n'+await body());}
async function resize(w){await c.send('Emulation.setDeviceMetricsOverride',{width:w,height:w===1440?1000:844,deviceScaleFactor:1,mobile:false});await wait(850);await reset();}
async function shot(name){await reset();await wait(350);if((await body()).includes('Reconnecting'))throw Error('JPro reconnect overlay: '+name);const r=await c.send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(`${shots}/${name}.png`,Buffer.from(r.data,'base64'));e.screens.push({name,...await c.evaluate('({width:innerWidth,height:innerHeight,scrollWidth:document.documentElement.scrollWidth,text:document.body.innerText})')});}
async function login(email='alpha@example.test'){await c.send('Page.reload');await resize(1440);await until('Masukkan kata sandi Anda');await click('misal: nadia@sehatharian.id');await c.send('Input.insertText',{text:email});await click('Masukkan kata sandi Anda');await c.send('Input.insertText',{text:'fixture123'});await c.key('Enter','Enter',13);await until(email==='onboard-visual@example.test'?'Waktu Luang':'Keluar ke Beranda');}
const allRoutes=[['Beranda','beranda'],['Hidrasi','hidrasi'],['Jadwal & Alarm Tidur','tidur'],['Nutrisi','nutrisi'],['Latihan','latihan'],['Tantangan Harian','tantangan'],['Rekomendasi Aktivitas','rekomendasi'],['Statistik Mingguan','progres'],['Catatan Kebiasaan','catatan'],['Riwayat tantangan lama','tantangan-lama'],['Statistik lama','statistik-lama'],['Evaluasi Mingguan','evaluasi'],['Profil','profil']];
const routes=process.env.APP_ROUTES?allRoutes.filter(x=>process.env.APP_ROUTES.split(',').includes(x[1])):allRoutes;
try{
if(mode==='matrix'||mode==='admin'){
 await login(mode==='admin'?'admin@example.test':'alpha@example.test');
 for(const [nav,name] of (mode==='admin'?[['Admin konten','admin']]:routes)){
  await resize(1440);await top();await click(nav);await until(({beranda:'Lihat rutinitas',hidrasi:'Kapasitas gelas',tidur:'Catat tidur',nutrisi:'Catat makanan',latihan:'Program terbit',tantangan:'Tantangan & bacaan',rekomendasi:'Filter katalog',progres:'Periode (hari)',catatan:'Hari ini','tantangan-lama':'Tantangan','statistik-lama':'Statistik',evaluasi:'Evaluasi',profil:'Nama Lengkap:',admin:'Jenis konten'})[name]);await wait(1500);for(const w of (phase==='before'?[390]:[390,360,1440])){await resize(w);await top();await shot(`${name}-${w}`);}
 }
}
if(mode==='dark'){await login();await click('Tema terang / gelap');for(const [nav,name] of routes.filter(x=>['hidrasi','tidur','nutrisi','latihan','tantangan','progres','profil'].includes(x[1]))){await resize(1440);await top();await click(nav);await wait(1300);await resize(390);await top();await shot(`${name}-gelap-390`);}}
if(mode==='public'){
 await c.send('Page.reload');await resize(390);await until('Masukkan kata sandi Anda');for(const w of (phase==='before'?[390]:[390,360,1440])){await resize(w);await top();await shot('login-'+w);}await click('Daftar');await until('Nama Lengkap');for(const w of (phase==='before'?[390]:[390,360,1440])){await resize(w);await top();await shot('registrasi-'+w);}await click('← Kembali ke Beranda');await until('SehatHarian');for(const w of (phase==='before'?[390]:[390,360,1440])){await resize(w);await top();await shot('landing-'+w);}
}
if(mode==='onboarding'){await login('onboard-visual@example.test');for(const w of (phase==='before'?[390]:[390,360,1440])){await resize(w);await top();await shot('onboarding-'+w);}}

if(mode==='flow'){
 const db=process.env.APP_DB;if(!db?.startsWith('sehatharian_test_'))throw Error('isolated schema required');
 const sql=q=>execFileSync('mariadb',['--no-defaults','-h127.0.0.1','-P13317','-uroot','--batch','--skip-column-names',db,'-e',q],{encoding:'utf8'}).trim();
 const count=()=>Number(sql("SELECT COUNT(*) FROM aktivitas WHERE pengguna_id=(SELECT id FROM pengguna WHERE email='alpha@example.test')"));
 const water=()=>Number(sql("SELECT COALESCE(SUM(jumlah_ml),0) FROM hidrasi WHERE pengguna_id=(SELECT id FROM pengguna WHERE email='alpha@example.test') AND tanggal=CURDATE()"));
 await login();await top();await click('Hidrasi');await resize(390);await top();e.waterBefore=water();await click('+ 250 ml');await until('Air berhasil dicatat.');e.waterAfter=water();if(e.waterAfter!==e.waterBefore+250)throw Error('water mismatch');await top();await shot('hidrasi-tersimpan-390');
 await resize(1440);await top();await click('Latihan');await until('Program Uji · lihat program');e.activityBefore=count();await resize(390);await click('Program Uji · lihat program');await until('Gunakan program & tinjau');await top();await shot('program-detail-390');await click('Gunakan program & tinjau');await until('Mulai persiapan');await top();await shot('latihan-review-390');await click('Mulai persiapan');await wait(5600);await click('Konfirmasi selesai / berikutnya');await top();await wait(1400);await shot('latihan-aktif-390');
 e.video=await c.evaluate("[...document.querySelectorAll('video')].map(v=>({paused:v.paused,ready:v.readyState,loop:v.loop,w:v.getBoundingClientRect().width}))");await click('Jeda');await top();await shot('latihan-jeda-390');await wait(1000);await click('Lanjutkan');
 await until('Konfirmasi selesai / berikutnya');await click('Konfirmasi selesai / berikutnya');await until('Durasi aktif:');await top();await shot('latihan-ringkasan-390');e.activityBeforeSave=count();if(e.activityBeforeSave!==e.activityBefore)throw Error('implicit activity write');await click('Simpan aktivitas');await until('Data berhasil disimpan.');e.activityAfter=count();if(e.activityAfter!==e.activityBefore+1)throw Error('activity save mismatch');await top();await shot('latihan-tersimpan-390');
 e.row=sql("SELECT pengguna_id,durasi_menit,keterangan FROM aktivitas WHERE pengguna_id=(SELECT id FROM pengguna WHERE email='alpha@example.test') ORDER BY id DESC LIMIT 1");
}


if(mode==='support'){
 await login();await click('Jadwal & Alarm Tidur');await until('Catat tidur');await resize(390);await top();await click('Catat tidur');await wheel(280);await shot('tidur-form-390');await top();await click('Jadwal tidur & pengingat (rencana)');await wheel(520);await shot('jadwal-tidur-390');
 await resize(1440);await top();await click('Nutrisi');await until('Catat makanan');await resize(390);await top();await click('+ Catat makanan');await until('Cari nama makanan dalam katalog');await top();await shot('nutrisi-form-390');
}
if(mode==='onboarding-steps'){
 await login('onboard-visual@example.test');await resize(390);await top();await click('Kurang dari 1 jam');await click('Berikutnya →');await until('Di mana Anda lebih sering');await top();await shot('onboarding-2-390');await click('Indoor');await click('Berikutnya →');await until('Selesai & Buka Dashboard');await top();await shot('onboarding-3-390');
}

if(mode==='inspect')console.log(await body());
}catch(error){e.error=String(error);throw error;}finally{fs.writeFileSync(`${out}/${phase}-${mode}${process.env.APP_BATCH?'-'+process.env.APP_BATCH:''}.json`,JSON.stringify(e,null,2));c.ws.close();}
