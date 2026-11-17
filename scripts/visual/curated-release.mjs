import fs from 'node:fs';
import {execFileSync} from 'node:child_process';
import {connect,wait} from './latihan-cdp.mjs';
const out='docs/verification/kurasi-rilis',shots='docs/screenshots/kurasi-rilis';
const mode=process.argv[2]||'candidate',db=process.env.CURATED_DB;
if(!db?.startsWith('sehatharian_test_curated_'))throw Error('Disposable menu schema required');
const c=await connect(),e={mode,db,screens:[]};
const sql=q=>execFileSync('mariadb',['--no-defaults','-h127.0.0.1','-P13317','-uroot','--batch','--skip-column-names',db,'-e',q],{encoding:'utf8'}).trim();
const owner="(SELECT id FROM pengguna WHERE email='release@example.test')";
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
 await login('release@example.test');await click('Nutrisi');await click('Buka menu personal');await until('3 menu cocok');await resize(390);await shot('paket-'+mode+'-menu-390');e.menus=await body();
 await click('Tinjau porsi · Pisang beku lembut');await click('Tinjau porsi menu');await until('Simpan porsi menu');await wheel(450);await shot('paket-'+mode+'-porsi-390');e.review=await body();if(!e.review.includes('89.00 kkal'))throw Error('real portion estimate absent');
 await resize(1440);await click('Latihan');await until('Lanjutkan sesi tersimpan');await resize(390);await shot('paket-'+mode+'-pemulihan-390');e.checkpoint=await body();
 await click('Riwayat latihan');await until('Lihat rincian sesi');await click('Lihat rincian sesi');await until('Rincian saat latihan dilakukan');await top();await shot('paket-'+mode+'-riwayat-390');e.history=await body();if(!e.history.includes('[UJI paket] Hasil latihan'))throw Error('training history missing');
 e.sql=sql('SELECT (SELECT COUNT(*) FROM food_catalog),(SELECT COUNT(*) FROM curated_menu_import),(SELECT COUNT(*) FROM content_version WHERE kind=\'MENU\' AND state=\'PUBLISHED\'),(SELECT COUNT(*) FROM aktivitas WHERE pengguna_id='+owner+'),(SELECT COUNT(*) FROM training_session WHERE pengguna_id='+owner+'),(SELECT COUNT(*) FROM training_step WHERE session_id IN (SELECT id FROM training_session WHERE pengguna_id='+owner+')),(SELECT COUNT(*) FROM training_checkpoint WHERE pengguna_id='+owner+' AND status=\'ACTIVE\')');
 if(e.sql!=='7795\t5\t5\t1\t1\t1\t1')throw Error('release SQL mismatch '+e.sql);
 e.duration=sql('SELECT t.active_nanos,(SELECT SUM(s.active_nanos) FROM training_step s WHERE s.session_id=t.id),a.durasi_menit FROM training_session t JOIN aktivitas a ON a.id=t.activity_id WHERE t.pengguna_id='+owner);if(e.duration!=='60000000000\t60000000000\t1')throw Error('training duration mismatch');
}catch(err){e.error=String(err);e.body=await body();throw err;}finally{fs.writeFileSync(`${out}/package-browser-${mode}.json`,JSON.stringify(e,null,2));c.ws.close();}
