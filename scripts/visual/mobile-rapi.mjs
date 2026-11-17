import fs from 'node:fs';
import {execFileSync} from 'node:child_process';
import {connect,wait} from './latihan-cdp.mjs';
export const phase=process.env.MOBILE_PHASE||'after';
const out='docs/verification/mobile-rapi',shots=`docs/screenshots/mobile-rapi/${phase}`;
fs.mkdirSync(shots,{recursive:true});
const c=await connect(),evidence={phase,started:new Date().toISOString(),screens:[]};
const body=()=>c.evaluate('document.body.innerText');
async function wheel(delta){await c.evaluate("window.scrollTo(0,0);document.querySelectorAll('.jpro-application').forEach(e=>e.scrollTop=0)");await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:280,y:460,deltaX:0,deltaY:delta});await wait(250);}
async function top(){await wheel(-25000);}
async function previousDay(){const before=(await body()).split('\n').slice(0,4).join('\n');await click('‹');if((await body()).split('\n').slice(0,4).join('\n')===before){await c.key(' ','Space',32);await wait(700);evidence.dateKeyboardFallback=true;}}
async function until(value,seconds=40){for(let i=0;i<seconds*2;i++){if((await body()).includes(value))return;await wait(500);}throw Error('Tidak muncul: '+value+'\n'+await body());}
async function click(value,index=0){await c.evaluate("window.scrollTo(0,0);document.querySelectorAll('.jpro-application').forEach(e=>e.scrollTop=0)");for(let i=0;i<20;i++){let p=await c.evaluate(`(()=>{let a=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent.trim()===${JSON.stringify(value)}).map(e=>e.getBoundingClientRect()).filter(r=>r.width&&r.y>=0&&r.bottom<innerHeight-72);let r=a[${index}];return r?{x:r.x+r.width/2,y:r.y+r.height/2}:null})()`);if(p){await c.click(p.x,p.y);await wait(500);return;}await wheel(350);}throw Error('Tidak terjangkau: '+value+'\n'+await body());}
async function resize(w,h=w===1440?1000:844){
 const previous=await c.evaluate('({w:innerWidth,h:innerHeight})');
 // JPro's fullscreen host in this headless environment reacts to width resize; height-only emulation leaves the old scene size.
 if(previous.w===w&&previous.h!==h){await c.send('Emulation.setDeviceMetricsOverride',{width:w+1,height:h,deviceScaleFactor:1,mobile:false});await wait(250);evidence.heightResizeWorkaround=true;}
 await c.send('Emulation.setDeviceMetricsOverride',{width:w,height:h,deviceScaleFactor:1,mobile:false});await wait(700);
}
async function shot(name){await c.evaluate("window.scrollTo(0,0);document.querySelectorAll('.jpro-application').forEach(e=>e.scrollTop=0)");await wait(350);let r=await c.send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(`${shots}/${name}.png`,Buffer.from(r.data,'base64'));evidence.screens.push({name,...await c.evaluate(`({width:innerWidth,height:innerHeight,scrollWidth:document.documentElement.scrollWidth,text:document.body.innerText,video:[...document.querySelectorAll('video')].map(v=>({paused:v.paused,time:v.currentTime,ready:v.readyState,loop:v.loop,rect:(()=>{let r=v.getBoundingClientRect();return {x:r.x,y:r.y,w:r.width,h:r.height}})()}))})`)});}
async function matrix(name){for(let w of [390,360,1440]){await resize(w);await top();await shot(name+'-'+w);}}
async function go(name){await resize(1440);await top();await click(name);await top();}
try{
const mode=process.argv[2]||'capture';
if(mode==='inspect')console.log(await body());
if(['login','capture','nutrition-flow','hydration-flow','dark-check'].includes(mode)){
 await c.send('Page.reload');await resize(1440);await until('Masukkan kata sandi Anda');await click('misal: nadia@sehatharian.id');await c.send('Input.insertText',{text:'alpha@example.test'});await click('Masukkan kata sandi Anda');await c.send('Input.insertText',{text:'fixture123'});await c.key('Enter','Enter',13);await until('Halo, Fixture');
}
if(mode==='capture'){
 for(let [nav,name] of [['Beranda','beranda'],['Nutrisi','nutrisi'],['Hidrasi','hidrasi']]){await go(nav);await matrix(name);}
 await go('Latihan');await click('Program Uji · lihat program');await click('Gunakan program & tinjau');await click('Mulai persiapan');await wait(5500);await click('Konfirmasi selesai / berikutnya');await wait(600);await matrix('latihan-aktif');await click('Jeda');await matrix('latihan-jeda');
 const first=await body();await wait(1800);evidence.pausedStable=first===await body();
}
if(mode==='nutrition-flow'){
 const db=process.env.MOBILE_DB;if(!db?.startsWith('sehatharian_test_rombak_mobile_'))throw Error('isolated DB required');
 const sql=q=>execFileSync('mariadb',['--no-defaults','-h127.0.0.1','-P13317','-uroot','--batch','--skip-column-names',db,'-e',q],{encoding:'utf8'}).trim();
 const count=()=>Number(sql("SELECT COUNT(*) FROM kebiasaan_makan WHERE pengguna_id=(SELECT id FROM pengguna WHERE email='alpha@example.test')"));
 const fill=async(old,value)=>{await click(old);await c.key('a','KeyA',65,2);await c.send('Input.insertText',{text:value});await wait(400);};
 evidence.before=count();await go('Nutrisi');await resize(390);await top();await click('+ Catat makanan');await click('Cari nama makanan dalam katalog');await c.send('Input.insertText',{text:'Baked potato'});await click('Cari katalog');await click('Baked potato',1);await fill('100','150');await resize(390,480);await shot('nutrisi-porsi-viewport-390x480');await click('Tinjau porsi');await until('Periksa estimasi');await shot('nutrisi-review-viewport-390x480');
 evidence.estimate=(await body()).includes('129.00 kkal');if(!evidence.estimate)throw Error('expected 129 kcal preview');await click('Simpan porsi ini');await until('Porsi berhasil disimpan.');evidence.after=count();if(evidence.after!==evidence.before+1)throw Error('one meal write expected');
 evidence.snapshot=sql("SELECT s.grams,s.nutrients,s.version FROM food_snapshot s JOIN kebiasaan_makan m ON m.id=s.meal_id WHERE m.pengguna_id=(SELECT id FROM pengguna WHERE email='alpha@example.test') ORDER BY m.id DESC LIMIT 1");await resize(390);await top();await shot('nutrisi-setelah-simpan-390');
 await click('+ Catat makanan');await click('Cari katalog');await click('Baked potato',1);await fill('100','0');await click('Tinjau porsi');await until('Periksa porsi:');evidence.afterInvalid=count();if(evidence.afterInvalid!==evidence.after)throw Error('invalid input wrote');await shot('nutrisi-porsi-invalid-390');
}
if(mode==='hydration-flow'){
 const db=process.env.MOBILE_DB;if(!db?.startsWith('sehatharian_test_rombak_mobile_'))throw Error('isolated DB required');
 const sql=q=>execFileSync('mariadb',['--no-defaults','-h127.0.0.1','-P13317','-uroot','--batch','--skip-column-names',db,'-e',q],{encoding:'utf8'}).trim();
 const amount=()=>Number(sql("SELECT COALESCE(SUM(jumlah_ml),0) FROM hidrasi WHERE pengguna_id=(SELECT id FROM pengguna WHERE email='alpha@example.test') AND tanggal=CURDATE()"));
 const fill=async(old,value)=>{await click(old);await c.key('a','KeyA',65,2);await c.send('Input.insertText',{text:value});await wait(300);};
 evidence.before=amount();await go('Hidrasi');await resize(390);await click('500 ml');await click('+ 500 ml');await until('Air berhasil dicatat.');evidence.afterQuick=amount();if(evidence.afterQuick!==evidence.before+500)throw Error('500 ml write mismatch');await top();await shot('hidrasi-setelah-volume-390');
 await click('Jumlah khusus');await fill('250','125');await click('Catat jumlah khusus');await until('Air berhasil dicatat.');evidence.afterCustom=amount();if(evidence.afterCustom!==evidence.afterQuick+125)throw Error('125 ml write mismatch');
 const initialTarget=sql("SELECT hydration_ml FROM habit_preferences WHERE pengguna_id=(SELECT id FROM pengguna WHERE email='alpha@example.test')");await click('Target harian · '+initialTarget+' ml');await fill(initialTarget,'1500');await click('Simpan target');await until('Target pilihan disimpan.');evidence.target=Number(sql("SELECT hydration_ml FROM habit_preferences WHERE pengguna_id=(SELECT id FROM pengguna WHERE email='alpha@example.test')"));if(evidence.target!==1500)throw Error('target save mismatch');await top();await shot('hidrasi-target-1500-390');
 sql("INSERT INTO hidrasi(pengguna_id,tanggal,jumlah_ml,catatan) SELECT id,DATE_SUB(CURDATE(),INTERVAL 1 DAY),0,'Fixture legacy nol' FROM pengguna WHERE email='alpha@example.test'");
 await previousDay();await top();await shot('hidrasi-nol-valid-390');evidence.zeroState=(await body()).includes('ml tercatat');await previousDay();await top();await shot('hidrasi-kosong-390');evidence.emptyState=(await body()).includes('belum tercatat');if(!evidence.zeroState||!evidence.emptyState)throw Error('empty and zero state check failed');
}
if(mode==='session-save'){
 const db=process.env.MOBILE_DB;if(!db?.startsWith('sehatharian_test_rombak_mobile_'))throw Error('isolated DB required');
 const sql=q=>execFileSync('mariadb',['--no-defaults','-h127.0.0.1','-P13317','-uroot','--batch','--skip-column-names',db,'-e',q],{encoding:'utf8'}).trim();
 const count=()=>Number(sql("SELECT COUNT(*) FROM aktivitas WHERE pengguna_id=(SELECT id FROM pengguna WHERE email='alpha@example.test')"));
 evidence.before=count();await resize(390);await top();await click('Lanjutkan');await wait(1500);evidence.resumedMedia=await c.evaluate("[...document.querySelectorAll('video')].map(v=>({paused:v.paused,ready:v.readyState,loop:v.loop,time:v.currentTime}))");
 await until('Konfirmasi selesai / berikutnya',85);await click('Konfirmasi selesai / berikutnya');await until('Durasi aktif:');await top();await shot('latihan-ringkasan-390');evidence.summary=await body();evidence.beforeSave=count();if(evidence.beforeSave!==evidence.before)throw Error('implicit write');await click('Simpan aktivitas');await until('Data berhasil disimpan.');evidence.after=count();if(evidence.after!==evidence.before+1)throw Error('activity write mismatch');await shot('latihan-tersimpan-390');
 evidence.row=sql("SELECT pengguna_id,durasi_menit,keterangan FROM aktivitas WHERE pengguna_id=(SELECT id FROM pengguna WHERE email='alpha@example.test') ORDER BY id DESC LIMIT 1");
}
if(mode==='dark-check'){
 await resize(1440);await click('Tema terang / gelap');for(let [nav,name] of [['Beranda','beranda'],['Nutrisi','nutrisi'],['Hidrasi','hidrasi']]){await go(nav);await resize(390);await top();await shot(name+'-gelap-390');}
 await go('Latihan');await click('Program Uji · lihat program');await click('Gunakan program & tinjau');await click('Mulai persiapan');await wait(5500);await click('Konfirmasi selesai / berikutnya');await click('Jeda');await resize(390);await top();await shot('latihan-gelap-jeda-390');
}
if(mode==='shot'){await resize(Number(process.argv[3]||390));await top();await shot(process.argv[4]||'manual');}
fs.writeFileSync(`${out}/${phase}-${mode}.json`,JSON.stringify(evidence,null,2));
}catch(error){evidence.error=String(error);fs.writeFileSync(`${out}/${phase}-${process.argv[2]||'capture'}-failed.json`,JSON.stringify(evidence,null,2));throw error;}finally{c.ws.close();}
