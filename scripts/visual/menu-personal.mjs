import fs from 'node:fs';
import {execFileSync} from 'node:child_process';
import {connect,wait} from './latihan-cdp.mjs';
const out='docs/verification/menu-personal',shots='docs/screenshots/menu-personal';
const mode=process.argv[2]||'inspect',db=process.env.MENU_DB;
if(!db?.startsWith('sehatharian_test_menu_'))throw Error('Disposable menu schema required');
const c=await connect(),e={mode,db,screens:[]};
const sql=q=>execFileSync('mariadb',['--no-defaults','-h127.0.0.1','-P13317','-uroot','--batch','--skip-column-names',db,'-e',q],{encoding:'utf8'}).trim();
const owner="(SELECT id FROM pengguna WHERE email='ui@example.test')";
const body=()=>c.evaluate('document.body.innerText');
const reset=()=>c.evaluate("window.scrollTo(0,0);document.querySelectorAll('.jpro-application').forEach(e=>e.scrollTop=0)");
async function wheel(deltaY){await reset();await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:await c.evaluate('innerWidth<768?280:900'),y:420,deltaX:0,deltaY});await wait(250);}
async function top(){await wheel(-25000);}
async function until(s){for(let i=0;i<100;i++){if((await body()).includes(s))return;await wait(400);}throw Error('Missing '+s+'\n'+await body());}
async function click(s,index=0){await reset();for(let i=0;i<35;i++){const p=await c.evaluate(`(()=>{let a=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent.trim()===${JSON.stringify(s)}).map(e=>e.getBoundingClientRect()).filter(r=>r.width&&r.y>=0&&r.bottom<innerHeight-(innerWidth<768?62:5));let r=a.at(${index});return r?{x:r.x+r.width/2,y:r.y+r.height/2}:null})()`);if(p){if(p.y>650&&s.startsWith("Pilih")){await wheel(350);continue;}await c.click(p.x,p.y);await wait(500);await reset();return;}await wheel(300);}throw Error('Cannot click '+s+'\n'+await body());}
async function fill(prompt,value){await click(prompt);await c.key('a','KeyA',65,2);await c.send('Input.insertText',{text:value});await wait(300);}
async function resize(w){await c.resize(w);await top();}
async function shot(name){await reset();await wait(450);const r=await c.send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(`${shots}/${name}.png`,Buffer.from(r.data,'base64'));e.screens.push({name,...await c.evaluate('({width:innerWidth,height:innerHeight,scrollWidth:document.documentElement.scrollWidth,text:document.body.innerText})')});}
async function login(email){await c.send('Page.reload');await resize(1440);await until('Masukkan kata sandi Anda');await fill('misal: nadia@sehatharian.id',email);await fill('Masukkan kata sandi Anda','fixture123');await c.key('Enter','Enter',13);await until('Keluar ke Beranda');}
try{
 await top();
 if(mode==='inspect'){console.log(await body());console.log(await c.evaluate("[...document.querySelectorAll('input,textarea')].map(e=>({tag:e.tagName,placeholder:e.placeholder,rect:e.getBoundingClientRect().toJSON()}))"));}
 if(mode==='admin-open'){await login('admin@example.test');await click('Admin konten');await click('Menu resep');await click('Cari konten');console.log(await body());}
 if(mode==='admin-edit'||mode==='admin-lines'){
 if(mode==='admin-edit'){
  await click('[UJI] Menu JPro · DRAFT · v1');await click('Bahan belum ditinjau');await click('Daftar bahan lengkap — untuk ditinjau',-1);
  await fill('Misal 2 porsi hasil resep','1');await fill('Sumber bahan dan porsi resep','SINTETIS UJI JPro — satu bahan beras 100 g, hasil 1 porsi; bukan resep/nilai nyata.');
  }
  await fill(mode==='admin-lines'?'rice':'Cari nama atau alias bahan resep','rice');await click('Cari bahan resep');await until('Pilih bahan dari hasil pencarian.');await click('Pilih bahan hasil pencarian');await click('Beras',-1);await fill('Jumlah bahan, kosong bila belum diketahui','100');
  await click('Konversi dan pemetaan gizi (opsional)');await fill('Cari item katalog gizi untuk bahan ini','Beras sintetis');await click('Cari item gizi bahan');await until('Pilih item yang benar');await click('Pilih item gizi hasil pencarian');await c.key('ArrowDown','ArrowDown',40);await c.key('Enter','Enter',13);await click('Identitas item dan basis per 100 g sudah diperiksa');await fill('Sumber pemeriksaan identitas dan basis per 100 g','SINTETIS UJI JPro: uji-food-rice fixture-v1, 100 kkal per100g, identitas dipilih eksplisit.');
  await click('Tambahkan bahan resep');await until('Bahan ditambahkan');await click('Simpan sebagai draft revisi');console.log(await body());
 }
 if(mode==='admin-finish'||mode==='admin-save'){if(mode==='admin-finish'){await click('Pilih bahan hasil pencarian');await click('Beras',-1);}await click('Tambahkan bahan resep');await until('Bahan ditambahkan');await click('Simpan sebagai draft revisi');await until('Perubahan konten tersimpan.');e.drafts=sql("SELECT id,title,state,revision FROM content_version WHERE title='[UJI] Menu JPro'");}
 if(mode==='admin-publish'){
  await top();await click('Cari konten');await click('[UJI] Menu JPro · DRAFT · v2');await click('Tandai ditinjau');await top();await click('Cari konten');await click('[UJI] Menu JPro · REVIEWED · v2');await click('Terbitkan');await until('Perubahan konten tersimpan.');e.content=sql("SELECT id,title,state,revision FROM content_version WHERE title='[UJI] Menu JPro'");await top();await shot('admin-terbit-1440');
 }
 if(mode==='user-open'){await login('ui@example.test');await click('Nutrisi');await click('Buka menu personal');await until('Preferensi menu & pantangan');await click('Preferensi menu & pantangan');console.log(await body());}
 if(mode==='reset-preferences'){await click('Ada pilihan yang dilaporkan');await click('Tidak ada — saya menyatakan eksplisit',-1);await click('Simpan preferensi menu');await until('Sesuai pilihan vegetarian');await resize(390);await top();await shot('hasil-awal-390');e.beforeRestriction=await body();}
 if(mode==='preferences'){
  await click('Belum diisi',0);await click('Vegetarian (telur/susu diperbolehkan)',-1);
  await click('Belum diisi',0);await click('Tidak ada — saya menyatakan eksplisit',-1);
  await click('Belum diisi',0);await click('Tidak ada — saya menyatakan eksplisit',-1);
  await click('Simpan preferensi menu');await until('Sesuai pilihan vegetarian');await resize(390);await top();await shot('hasil-awal-390');e.beforeRestriction=await body();
 }
 if(mode==='restriction'){
  await top();await click('Preferensi menu & pantangan');await click('Kategori pantangan yang dilaporkan');await click('Susu');await resize(390);await top();await wheel(550);await shot('preferensi-pantangan-390');await click('Simpan preferensi menu');await until('Sesuai pilihan vegetarian');e.afterRestriction=await body();if(e.afterRestriction.includes('Tinjau porsi · [UJI] Susu beras'))throw Error('milk not excluded');await top();await shot('hasil-disaring-390');e.preferences=sql('SELECT diet,avoid_status,restriction_status,allergens,revision FROM menu_preferences WHERE pengguna_id='+owner);
 }
 if(mode==='review-390'){await resize(390);await click('Buka menu personal');await until('Tinjau porsi · [UJI] Menu JPro');await click('Tinjau porsi · [UJI] Menu JPro');await click('Tinjau porsi menu');await until('Simpan porsi menu');await wheel(450);await shot('tinjauan-porsi-390');e.form=await body();}
 if(mode==='review-final'){await resize(390);await click('Preferensi menu & pantangan');await click('Tinjau porsi · [UJI] Menu JPro');await click('Tinjau porsi menu');await until('Simpan porsi menu');await wheel(450);await shot('tinjauan-porsi-390');e.form=await body();}
 if(mode==='portion'){
  await top();await click('Bahan & sumber · [UJI] Menu JPro');await shot('bahan-390');await click('Tinjau porsi · [UJI] Menu JPro');await click('Tinjau porsi menu');await until('Simpan porsi menu');await wheel(450);await shot('tinjauan-porsi-390');e.before=sql('SELECT COUNT(*) FROM kebiasaan_makan WHERE pengguna_id='+owner);await click('Simpan porsi menu');await until('Porsi menu berhasil disimpan.');e.after=sql('SELECT COUNT(*) FROM kebiasaan_makan WHERE pengguna_id='+owner);e.snapshot=sql('SELECT s.meal_id,s.pengguna_id,s.menu_id,s.content_revision,s.preference_revision,s.portions,s.recipe_servings,s.nutrients,s.source FROM menu_meal_snapshot s WHERE s.pengguna_id='+owner);if(Number(e.after)!==Number(e.before)+1)throw Error('meal count');await top();await shot('porsi-tersimpan-390');
 }
 if(mode==='states'){
  await resize(390);await top();
  try{sql('RENAME TABLE menu_composition TO menu_composition_unavailable');await until('Menu gagal dimuat.');await shot('gagal-muat-390');}finally{sql('RENAME TABLE menu_composition_unavailable TO menu_composition');}
  await click('Periksa menu personal');await until('Sesuai pilihan vegetarian');await shot('pulih-muat-390');
  await click('Preferensi menu & pantangan');await fill('Cari bahan atau alias','rice');await click('Cari bahan preferensi');await until('Hindari · Beras');await click('Hindari · Beras');await click('Simpan preferensi menu');await until('Belum ada menu yang dapat');await top();await shot('hasil-kosong-390');e.empty=await body();
  await click('Preferensi menu & pantangan');await click('Ada pilihan yang dilaporkan',0);await click('Tidak ada — saya menyatakan eksplisit',-1);await click('Simpan preferensi menu');await until('Sesuai pilihan vegetarian');await top();
 }
 if(mode==='history'){
  await click('Kembali ke Nutrisi');await until('Makan siang · 1 catatan');await click('Makan siang · 1 catatan');await resize(390);await wheel(600);await shot('riwayat-porsi-390');e.history=await body();if(!e.history.includes('100.00 kkal')||!e.history.includes('1.0 porsi resep'))throw Error('snapshot missing in nutrition history');
 }
 if(mode==='sizes'){
  for(const width of [360,1440]){await resize(width);await shot('hasil-'+width);await click('Bahan & sumber · [UJI] Menu JPro');await shot('bahan-'+width);await click('Bahan & sumber · [UJI] Menu JPro');}
  await resize(1440);await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:130,y:700,deltaX:0,deltaY:1700});await wait(500);await click('Tema terang / gelap');await resize(390);await shot('hasil-gelap-390');
 }
}catch(err){e.error=String(err);e.body=await body();throw err;}finally{fs.writeFileSync(`${out}/browser-${mode}.json`,JSON.stringify(e,null,2));c.ws.close();}
