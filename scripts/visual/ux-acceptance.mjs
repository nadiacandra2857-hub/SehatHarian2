import fs from 'node:fs';
import {execFileSync} from 'node:child_process';
import {connect,wait} from './latihan-cdp.mjs';
const c=await connect();const dir=process.env.UX_SHOTS||'docs/screenshots/ux-latihan-20261106';fs.mkdirSync(dir,{recursive:true});
const out=process.env.UX_EVIDENCE||'docs/verification/ux-latihan-20261106';fs.mkdirSync(out,{recursive:true});
const evidence={database:'sehatharian_test_ux_20261104',owner:1,startedAt:new Date().toISOString(),checks:[],screenshots:[]};
function sql(query){return execFileSync('mariadb',['--no-defaults','-h127.0.0.1','-P13306','-uroot','--batch','--skip-column-names',evidence.database,'-e',query],{encoding:'utf8'}).trim();}
function count(){return Number(sql('SELECT COUNT(*) FROM aktivitas WHERE pengguna_id=1'));}
const text=()=>c.evaluate('document.body.innerText');
async function wheel(delta){const p=await c.evaluate('({x:innerWidth*.7,y:innerHeight*.65})');await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',...p,deltaX:0,deltaY:delta});await wait(350);}
async function click(value,index=0,prefix=false){for(let i=0;i<18;i++){const r=await c.evaluate(`(()=>{let a=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&(${prefix}?e.textContent.trim().startsWith(${JSON.stringify(value)}):e.textContent.trim()===${JSON.stringify(value)})).map(e=>e.getBoundingClientRect()).filter(r=>r.width&&r.y>=0&&r.bottom<innerHeight);let r=a[${index}];return r?{x:r.x+r.width/2,y:r.y+r.height/2}:null})()`);if(r){await c.click(r.x,r.y);await wait(1100);return;}await wheel(420);}throw Error('Tidak terjangkau: '+value);}
async function more(){
 for(let attempt=0;attempt<3;attempt++){
  if((await text()).includes('\nLatihan\nTantangan'))return;
  await click('Lainnya');
  for(let i=0;i<6;i++){await wait(500);if((await text()).includes('\nLatihan\nTantangan'))return;}
 }
 throw Error('Menu Lainnya belum terbuka');
}
async function until(value,seconds=15){for(let i=0;i<seconds*2;i++){if((await text()).includes(value))return;await wait(500);}throw Error('Tidak muncul: '+value);}
async function shot(name){await wait(400);let r=await c.send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(dir+'/'+name+'.png',Buffer.from(r.data,'base64'));evidence.screenshots.push(name);}
async function styles(name){const rows=await c.evaluate(`Array.from(document.querySelectorAll('*')).filter(e=>e.childElementCount===0&&e.textContent.trim()).map(e=>{let s=getComputedStyle(e),r=e.getBoundingClientRect();let chain=[];for(let n=e;n;n=n.parentElement){let c=getComputedStyle(n);chain.push({tag:n.tagName,opacity:c.opacity,color:c.color,background:c.backgroundColor});}return {text:e.textContent.trim(),rect:{x:r.x,y:r.y,w:r.width,h:r.height},style:{color:s.color,opacity:s.opacity},chain};}).filter(e=>e.rect.w&&e.rect.y>=0&&e.rect.y<innerHeight)`);fs.writeFileSync(out+'/styles-'+name+'.json',JSON.stringify(rows,null,2));await shot('audit-'+name);}
async function fill(prompt,value){const r=await c.evaluate(`(()=>{let candidates=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent.trim()===${JSON.stringify(prompt)}).reverse();for(let e of candidates){for(let n=e,k=0;n&&k<8;n=n.parentElement,k++){let a=[...n.querySelectorAll('input')].map(e=>e.getBoundingClientRect()).filter(r=>r.width&&r.y>=0&&r.bottom<innerHeight);if(a.length===1){let r=a[0];return {x:r.x+r.width/2,y:r.y+r.height/2};}if(a.length>1)break;}}return null;})()`);if(!r)throw Error('Input tidak ditemukan: '+prompt);await c.click(r.x,r.y);await c.send('Input.insertText',{text:value});await wait(500);}
async function matrix(name){for(const w of [1440,768,390,360]){await c.resize(w);await wheel(-10000);await shot(name+'-'+w);await wheel(10000);await shot(name+'-controls-'+w);await c.key('Tab','Tab',9);await wait(1200);await shot(name+'-focus-'+w);evidence.checks.push({state:name,width:w,focus:await c.evaluate('({tag:document.activeElement.tagName,role:document.activeElement.getAttribute("role"),rect:(()=>{let r=document.activeElement.getBoundingClientRect();return {x:r.x,y:r.y,w:r.width,h:r.height}})()})')});}await c.resize(1440);await wheel(-10000);}
try{
const mode=process.argv[2]||'session';
if(mode==='login'){
 await c.resize(1440);await until('Masukkan kata sandi Anda');await shot('after-login-1440');
 await click('misal: nadia@sehatharian.id');await c.send('Input.insertText',{text:'nadia@sehatharian.local'});await click('Masukkan kata sandi Anda');await c.send('Input.insertText',{text:'password123'});await c.key('Enter','Enter',13);await until('Halo, Nadia',30);await shot('after-home-1440');
}else if(mode==='session'){
 if(sql('SELECT email FROM pengguna WHERE id=1')!=='nadia@sehatharian.local')throw Error('Owner mismatch');
 evidence.allOwnersBefore=sql('SELECT pengguna_id,COUNT(*) FROM aktivitas GROUP BY pengguna_id ORDER BY pengguna_id');evidence.before=count();await click('Latihan · katalog RepDB');await until('24 latihan tersedia');await matrix('after-catalog');
 await click('Lihat detail',0);await wheel(-10000);await click('Tambahkan ke sesi');await click('30');for(let i=0;i<30;i++)await c.key('ArrowUp','ArrowUp',38);await click('Tambahkan ke sesi');await wheel(-10000);await click('Kembali ke katalog');
 await click('Lihat detail',1);await wheel(-10000);await click('Tambahkan ke sesi');await click('Mode repetisi (selesai dikonfirmasi manual)');await click('Tambahkan ke sesi');await wheel(-10000);await click('Tinjau sesi');await until('Mulai persiapan');await matrix('after-review');
 evidence.afterSelection=count();if(evidence.afterSelection!==evidence.before)throw Error('Selection wrote activity');
 await click('Mulai persiapan');await shot('after-preparation-1440');await wait(5500);await click('Konfirmasi selesai / berikutnya');await wait(2500);await click('Jeda');await shot('after-paused-1440');await wait(2500);await click('Lanjutkan');await matrix('after-active');
 await wait(55000);await click('Konfirmasi selesai / berikutnya');await wheel(-10000);await shot('after-rest-1440');await wait(16000);await click('Konfirmasi selesai / berikutnya');await wait(5500);await click('Konfirmasi selesai / berikutnya');await wheel(-10000);await until('Durasi aktif:');await matrix('after-summary');
 evidence.summary=await text();evidence.beforeSave=count();if(evidence.beforeSave!==evidence.before)throw Error('Session wrote before save');
 await click('Simpan aktivitas');await wait(1500);evidence.saved=await text();evidence.afterSave=count();if(evidence.afterSave!==evidence.before+1)throw Error('Expected exactly one write');
 evidence.row=sql('SELECT id,pengguna_id,durasi_menit,keterangan FROM aktivitas WHERE pengguna_id=1 ORDER BY id DESC LIMIT 1');const match=evidence.summary.match(/Durasi aktif: (\d+) menit (\d+) detik/);if(!match)throw Error('Summary duration missing');const parts=evidence.row.split('\t');if(Number(parts[1])!==evidence.owner||Number(parts[2])!==Number(match[1])||!parts[3].includes('Aktif '+(Number(match[1])*60+Number(match[2]))+' detik'))throw Error('Stored duration/owner differs from summary');evidence.allOwnersAfter=sql('SELECT pengguna_id,COUNT(*) FROM aktivitas GROUP BY pengguna_id ORDER BY pengguna_id');await shot('after-saved-1440');await styles('saved-disabled');
 fs.writeFileSync(out+'/browser-session.json',JSON.stringify(evidence,null,2));
}else if(mode==='retention'){
 evidence.before=count();await c.resize(390);await click('Catatan');await more();await click('Latihan');await until('24 latihan tersedia');
 await click('Cari nama latihan (bahasa Inggris)');await c.send('Input.insertText',{text:'clean'});await until('3 latihan tersedia');
 await wheel(430);await click('Lihat detail');await wheel(-10000);await click('Kembali ke katalog');await until('3 latihan tersedia');
 evidence.catalog=await text();evidence.after=count();if(evidence.before!==evidence.after)throw Error('Catalog wrote activity');await shot('after-search-return-390');fs.writeFileSync(out+'/browser-retention.json',JSON.stringify(evidence,null,2));
}else if(mode==='history'){
 // Actual compact sidebar Catatan icon, same page controller as mobile Catatan.
 await c.resize(390);await click('Catatan');await until('CATATAN HARIAN');await click('Aktivitas');await matrix('after-history');await wheel(800);await shot('after-history-rows-1440');evidence.history=await text();evidence.countAfterReopen=count();fs.writeFileSync(out+'/browser-history.json',JSON.stringify(evidence,null,2));
 }else if(mode==='manual'){
 evidence.before=count();await wheel(-10000);await fill('Durasi (menit)','7');await fill('Keterangan / Lokasi','UX isian tetap');
 await click('Jelajahi katalog latihan RepDB');await until('24 latihan tersedia');await click('Lihat detail');await wheel(-10000);await click('Gunakan untuk catatan');
 evidence.picker=await text();if(!evidence.picker.includes('UX isian tetap')||!evidence.picker.includes('7'))throw Error('Manual picker cleared fields');
 await click('Latihan berpemandu');await until('24 latihan tersedia');await click('Lihat detail',1);await wheel(-10000);await click('Gunakan untuk catatan');await until('CATATAN HARIAN');
 evidence.page=await text();if(!evidence.page.includes('Air Bike')||!evidence.page.includes('UX isian tetap')||!evidence.page.includes('7'))throw Error('Training page cleared fields');
 evidence.after=count();if(evidence.before!==evidence.after)throw Error('Manual selection wrote activity');await shot('after-manual-preserved-1440');fs.writeFileSync(out+'/browser-manual.json',JSON.stringify(evidence,null,2));
 }else if(mode==='audit'){
 await wheel(-10000);await click((await text()).includes('CATATAN HARIAN')?'Latihan berpemandu':'Latihan · katalog RepDB');await until('24 latihan tersedia');for(const w of [1440,768,390,360]){await c.resize(w);await wheel(-10000);await click('Cari nama latihan (bahasa Inggris)');await wheel(10000);await c.key('Tab','Tab',9);await wait(1200);await shot('after-keyboard-scroll-'+w);}await c.resize(1440);await wheel(-10000);await click('Filter tambahan');await styles('catalog-secondary');await click('Semua');await styles('dropdown');await click('Pemula');await styles('combo-selected');await click('Pemula');await styles('dropdown-selected');await click('Pemula');
 await click('Lihat detail');await wheel(-10000);await click('Tambahkan ke sesi');await click('Mode repetisi (selesai dikonfirmasi manual)');await click('Tambahkan ke sesi');await wheel(-10000);await click('Tinjau sesi');await styles('spinner');
 }else if(mode==='lifecycle-setup'){
 await c.resize(390);await click('Catatan');await more();await click('Latihan');await until('24 latihan tersedia');await click('Lihat detail');await wheel(-10000);await click('Tambahkan ke sesi');await click('Mode repetisi (selesai dikonfirmasi manual)');await click('Tambahkan ke sesi');await wheel(-10000);await click('Tinjau sesi');
 }else if(mode==='lifecycle'){
 evidence.before=count();await click('Mulai persiapan');await wait(6000);await click('Konfirmasi selesai / berikutnya');await c.resize(390);await click('Catatan');await until('Buang sesi dan keluar');await shot('after-leave-choice-390');await click('Tetap di latihan');await until('Dijeda');await shot('after-leave-cancel-390');await click('Lanjutkan');
 await more();await click('Latihan');await until('Gerakan 1 / 1');evidence.reentered=await text();
 await click('Catatan');await click('Buang sesi dan keluar');await until('CATATAN HARIAN');await more();await click('Latihan');await until('Urutan: 0 / 20');await shot('after-discard-return-390');
 await more();await click('Keluar');await until('Proyek akademik SehatHarian',30);evidence.logout=await text();if(evidence.logout.includes('24 latihan tersedia'))throw Error('Private page remains after logout');evidence.after=count();if(evidence.before!==evidence.after)throw Error('Discard/logout wrote activity');await shot('after-logout-390');fs.writeFileSync(out+'/browser-lifecycle.json',JSON.stringify(evidence,null,2));
 }else if(mode==='landing'){
 await c.resize(1440);if((await text()).includes('← Kembali ke Beranda'))await click('← Kembali ke Beranda');await until('PROYEK AKADEMIK');
 for(const w of [1440,768,390,360]){await c.resize(w);await wheel(-10000);await shot('after-landing-'+w);await click('Ilustrasi kampus generik',0,true);await shot('after-campus-'+w);await wheel(-10000);}
 await c.resize(1440);await wheel(-10000);await click('Masuk');await until('Masukkan kata sandi Anda');await matrix('after-login');fs.writeFileSync(out+'/browser-landing.json',JSON.stringify(evidence,null,2));
}else if(mode==='recommendations'){
 evidence.before=count();await c.resize(390);await more();await click('Rekomendasi');await until('Rekomendasi');await matrix('after-recommendations');evidence.page=await text();evidence.after=count();if(evidence.before!==evidence.after)throw Error('Recommendations wrote activity');fs.writeFileSync(out+'/browser-recommendations.json',JSON.stringify(evidence,null,2));
}else if(mode==='sidebar'){
 await c.resize(390);await click('Beranda');await c.resize(1440);await wheel(-10000);await shot('after-home-1440');await c.click(66,277);await until('24 latihan tersedia');await shot('after-sidebar-latihan-1440');
 evidence.page=await text();fs.writeFileSync(out+'/browser-sidebar.json',JSON.stringify(evidence,null,2));
}else if(mode==='matrix'){await matrix(process.argv[3]||'state');}
console.log(JSON.stringify(evidence,null,2));
}catch(e){await shot('ERROR-'+Date.now());console.error(await text());throw e;}finally{c.ws.close();}
