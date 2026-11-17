import fs from 'node:fs';
import {execFileSync} from 'node:child_process';
import {connect,wait} from './latihan-cdp.mjs';
const out='docs/verification/kurasi-rilis',shots='docs/screenshots/kurasi-rilis';
const mode=process.argv[2]||'inspect',db=process.env.CURATED_DB;
if(!db?.startsWith('sehatharian_test_curated_'))throw Error('Disposable menu schema required');
const c=await connect(),e={mode,db,screens:[]};
const sql=q=>execFileSync('mariadb',['--no-defaults','-h127.0.0.1','-P13317','-uroot','--batch','--skip-column-names',db,'-e',q],{encoding:'utf8'}).trim();
const owner="(SELECT id FROM pengguna WHERE email='alpha@example.test')";
const body=()=>c.evaluate('document.body?.innerText || \"\"');
const reset=()=>c.evaluate("window.scrollTo(0,0);document.querySelectorAll('.jpro-application').forEach(e=>e.scrollTop=0)");
async function wheel(deltaY){await reset();await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:await c.evaluate('innerWidth<768?280:900'),y:420,deltaX:0,deltaY});await wait(250);}
async function top(){await wheel(-25000);}
async function until(s){for(let i=0;i<100;i++){if((await body()).includes(s))return;await wait(400);}throw Error('Missing '+s+'\n'+await body());}
async function click(s,index=0){
 for(let i=0;i<60;i++){
  await reset();await wait(150);const a=await c.evaluate(`(()=>{let a=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent.trim()===${JSON.stringify(s)}).map(e=>e.getBoundingClientRect()).filter(r=>r.width);let r=a.at(${index});return r?{x:r.x+r.width/2,y:r.y+r.height/2,bottom:r.bottom,h:innerHeight,visible:r.y>=40&&r.bottom<innerHeight-(innerWidth<768?62:5)}:null})()`);
  if(a?.visible){await c.click(a.x,a.y);await wait(650);await reset();return;}
  await wheel(a&&a.y<40?-240:240);
 }
 throw Error('Cannot click '+s+'\n'+await body());
}
async function fill(prompt,value){await click(prompt);await c.key('a','KeyA',65,2);await c.send('Input.insertText',{text:value});await wait(300);}
async function resize(w){await c.resize(w);await wait(1800);await top();}
async function shot(name){await reset();await wait(1200);for(let i=0;i<60&&(await body()).includes('Reconnecting');i++)await wait(500);if((await body()).includes('Reconnecting'))throw Error('JPro disconnected; screenshot not accepted');const r=await c.send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(`${shots}/${name}.png`,Buffer.from(r.data,'base64'));e.screens.push({name,...await c.evaluate('({width:innerWidth,height:innerHeight,scrollWidth:document.documentElement.scrollWidth,text:document.body.innerText})')});}
async function login(email){await c.send('Page.reload');await resize(1440);await until('Masukkan kata sandi Anda');await fill('misal: nadia@sehatharian.id',email);await fill('Masukkan kata sandi Anda','fixture123');await c.key('Enter','Enter',13);await until('Keluar ke Beranda');}
try{
 await top();
 if(mode==='open-current'){await resize(1440);if((await body()).includes('Buka menu personal'))await click('Buka menu personal');await until('menu cocok');await resize(390);await shot('hasil-terkini-390');}
 if(mode==='unrestrict'){await resize(1440);await click('Preferensi menu & pantangan');await click('Ada pilihan yang dilaporkan');await click('Tidak ada — saya menyatakan eksplisit',-1);await click('Simpan preferensi menu');await until('3 menu cocok');await resize(390);await shot('rekomendasi-390');}
 if(mode==='resume'){await login('alpha@example.test');await click('Nutrisi');await click('Buka menu personal');await until('2 menu cocok');await resize(390);await shot('pantangan-susu-390');}
 if(mode==='inspect')console.log(await body());
 if(mode==='admin'){
  await login('admin@example.test');await click('Admin konten');await click('Menu resep');
  for(const title of ['Pisang beku lembut','Omelet spinach','Salad tomat mentimun','Oat semalam buah','Nasi goreng sayur']){
   await top();await click(title+' · DRAFT · v1');await click('Tandai ditinjau');await until('Perubahan konten tersimpan.');await top();await click(title+' · REVIEWED · v1');await click('Terbitkan');await until('Perubahan konten tersimpan.');
  }
  e.published=sql("SELECT id,title,state FROM content_version WHERE kind='MENU'");await top();await resize(390);await shot('admin-publikasi-390');
 }
 if(mode==='preferences'){
  await login('alpha@example.test');await click('Nutrisi');await click('Buka menu personal');await click('Preferensi menu & pantangan');
  await click('Belum diisi',0);await click('Vegetarian (telur/susu diperbolehkan)',-1);await click('Belum diisi',0);await click('Tidak ada — saya menyatakan eksplisit',-1);await click('Belum diisi',0);await click('Tidak ada — saya menyatakan eksplisit',-1);await click('Simpan preferensi menu');await until('3 menu cocok');await resize(390);await shot('rekomendasi-390');e.before=await body();
 }
 if(mode==='restriction'){
  await resize(1440);await click('Bahan & sumber · Omelet spinach');await resize(390);await shot('bahan-omelet-390');await resize(1440);await click('Bahan & sumber · Omelet spinach');
  await top();await click('Preferensi menu & pantangan');await click('Kategori pantangan yang dilaporkan');
  await click('Vegetarian (telur/susu diperbolehkan)');await c.key('Escape','Escape',27);
  for(let i=0;i<4;i++){await c.key('Tab','Tab',9);await wait(200);}await c.key(' ','Space',32);await wait(300);
  await click('Simpan preferensi menu');await until('2 menu cocok');await resize(390);await top();await shot('pantangan-susu-390');e.after=await body();if(e.after.includes('Tinjau porsi · Omelet spinach'))throw Error('milk conflict shown');
 }
 if(mode==='debug-preference'){await resize(1440);await click('Preferensi menu & pantangan');await click('Kategori pantangan yang dilaporkan');await shot('debug-preference-before-1440');console.log(await body());}
 if(mode==='keyboard-milk'){await resize(1440);await click('Vegetarian (telur/susu diperbolehkan)');await c.key('Escape','Escape',27);for(let i=0;i<4;i++){await c.key('Tab','Tab',9);await wait(200);}await c.key(' ','Space',32);await wait(300);for(let i=0;i<2;i++){await c.key('Tab','Tab',9);await wait(200);}await c.key(' ','Space',32);await shot('keyboard-milk-1440');console.log(await body());}
 if(mode==='debug-milk'){await resize(1440);await click('Susu');await shot('debug-milk-1440');console.log(await body());}
 if(mode==='save-preference'){await click('Simpan preferensi menu');await until('2 menu cocok');await resize(390);await shot('pantangan-susu-390');e.after=await body();}
 if(mode==='portion'){
  await resize(390);await click('Bahan & sumber · Pisang beku lembut');await shot('bahan-pisang-390');await click('Tinjau porsi · Pisang beku lembut');await click('Makan siang');await click('Camilan',-1);await click('Tinjau porsi menu');await until('Simpan porsi menu');await wheel(430);await shot('tinjauan-porsi-390');e.before=sql('SELECT COUNT(*) FROM kebiasaan_makan WHERE pengguna_id='+owner);await click('Simpan porsi menu');await until('Porsi menu berhasil disimpan.');e.after=sql('SELECT COUNT(*) FROM kebiasaan_makan WHERE pengguna_id='+owner);e.receipt=sql('SELECT meal_id,menu_id,content_revision,preference_revision,portions,recipe_servings,nutrients FROM menu_meal_snapshot WHERE pengguna_id='+owner);if(Number(e.after)!==Number(e.before)+1)throw Error('meal count');await top();await shot('porsi-disimpan-390');
 }
 if(mode==='sizes'){
  for(const width of [360,1440]){await resize(width);await shot('rekomendasi-'+width);await click('Bahan & sumber · Pisang beku lembut');await shot('bahan-pisang-'+width);await click('Bahan & sumber · Pisang beku lembut');}
  await resize(1440);await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:130,y:700,deltaX:0,deltaY:1700});await wait(500);await click('Tema terang / gelap');await resize(390);await shot('rekomendasi-gelap-390');
 }
 if(mode==='catalog'){
  await resize(390);await click('Jelajahi menu umum');await until('Katalog umum');await click('Bahan & sumber · Oat semalam buah');await shot('katalog-belum-lengkap-390');e.general=await body();
 }
 if(mode==='history'){
  await resize(390);await top();await click('Kembali ke Nutrisi');await until('Camilan · 1 catatan');await click('Camilan · 1 catatan');await wheel(550);await shot('riwayat-porsi-390');e.history=await body();if(!e.history.includes('89.00 kkal'))throw Error('stored nutrition missing');
 }
}catch(err){e.error=String(err);e.body=await body();throw err;}finally{fs.writeFileSync(`${out}/browser-${mode}.json`,JSON.stringify(e,null,2));c.ws.close();}
