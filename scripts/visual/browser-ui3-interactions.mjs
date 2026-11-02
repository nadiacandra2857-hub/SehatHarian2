import fs from 'node:fs';
import {connect,wait} from './ui3-cdp.mjs';
const c=await connect();const navigationOnly=process.argv.includes('--navigation-only');const proof=navigationOnly?JSON.parse(fs.readFileSync('/tmp/ui3-interactions-progress.json','utf8')):[];let control,other,browserContextId;
const missingAsset=".runtime/repdb/images/flat/clean-start.png", missingBackup=missingAsset+".fixture-backup";
const text=()=>c.evaluate('document.body.innerText');
async function until(check,description){for(let i=0;i<80;i++){if(await check())return;await wait(120);}throw Error(description);}
async function rectText(value,last=false){return c.evaluate(`(()=>{let a=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent===${JSON.stringify(value)}).map(e=>e.getBoundingClientRect()).filter(r=>r.width>0&&r.x>=0&&r.right<=innerWidth+1&&r.y>=0&&r.bottom<=innerHeight+1);let r=a[${last?'a.length-1':'0'}];return r?{x:r.x+r.width/2,y:r.y+r.height/2}:null;})()`);}
async function clickText(value,last=false){let r=await rectText(value,last);if(!r)throw Error('Visible control missing: '+value);await c.click(r.x,r.y);await wait(250);}
async function prompt(value,input){let r=await rectText(value,true);if(!r)throw Error('Input prompt missing '+value);await c.click(r.x,r.y);await c.key('a','KeyA',65,2);await c.send('Input.insertText',{text:input});await wait(250);}
const values=()=>c.evaluate(`[...document.querySelectorAll('input')].filter(e=>e.getBoundingClientRect().width>0).map(e=>e.value)`);
async function key(key,code,n){await c.key(key,code,n);await wait(250);}
async function sql(){await key('F9','F9',120);let t=await text();let m=t.match(/SQL fixture=(\d+)/);if(!m)throw Error('Missing SQL audit');return Number(m[1]);}
async function scroll(delta,width=1440){await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:Math.floor(width*.6),y:width>=768?700:650,deltaY:delta,deltaX:0});await wait(450);}
try {
 if(!navigationOnly){
 await c.send('Page.reload',{});await wait(5500);await c.resize(1440);await clickText('Rute berikut');await until(async()=> (await text()).includes('RUTE: CatatanKebiasaan | filled'),'Catatan route');
 await clickText('Aktivitas');await wait(300);await prompt('Durasi (menit)','17');await key('Tab','Tab',9);proof.push({test:'keyboardTab',activeTag:await c.evaluate('document.activeElement.tagName')});await c.shot('activity-keyboard-focus-1440');let before=await sql();
 await clickText('Jelajahi katalog latihan RepDB');await until(async()=> (await text()).includes('24 latihan tersedia'),'Catalog ready');await c.shot('activity-catalog-dialog-1440');
 await prompt('Cari nama latihan (bahasa Inggris)','Clean');await until(async()=> (await text()).includes('3 latihan tersedia'),'Search filter');proof.push({test:'search',query:'Clean',resultCount:3});
 await clickText('Lihat detail');await until(async()=> (await text()).includes('ID RepDB: clean'),'Detail clean');
 if(!(await text()).includes('Posisi awal')||!(await text()).includes('Posisi akhir/puncak'))throw Error('Missing pose captions');await c.shot('activity-detail-dialog-1440');
 fs.renameSync(missingAsset,missingBackup);
 try {await clickText('Kembali ke katalog');await clickText('Lihat detail');await until(async()=> (await text()).includes('Gambar tidak tersedia'),'Missing image placeholder');await c.shot('detail-missing-image-1440');proof.push({test:'missingImagePlaceholder',passed:true});}
 finally {fs.renameSync(missingBackup,missingAsset);}
 await scroll(10000);await c.shot('activity-detail-dialog-bottom-1440');await clickText('Gunakan nama untuk catatan');await wait(400);
 let form=await values();if(!(await text()).includes('\nClean\n')||!form.includes('17')||await sql()!==before)throw Error('Selection mutated duration or wrote SQL');proof.push({test:'selection',name:'Clean',duration:'17',additionalWrites:0});
 await key('F6','F6',117);await clickText('Simpan Aktivitas');await until(async()=> (await text()).includes('Data belum tersimpan atau tidak berubah'),'Failed write feedback');await c.shot('write-failed-1440');await clickText('OK');
 if(!(await values()).includes('17'))throw Error('Failure cleared form');proof.push({test:'failedWriteRetainsForm',passed:true});
 await key('F7','F7',118);await clickText('Simpan Aktivitas');await until(async()=> (await text()).includes('Data mungkin telah tersimpan'),'Uncertain feedback');await c.shot('write-uncertain-1440');await clickText('OK');
 const uncertainCount=await sql();await clickText('Simpan Aktivitas');if(await sql()!==uncertainCount || !(await values()).includes('17'))throw Error('Uncertain write retried or cleared form');await c.shot('write-uncertain-disabled-1440');proof.push({test:'uncertainBlocksRepeat',passed:true,sqlExecutions:uncertainCount});
 await c.resize(360);
 for(let i=0;i<8 && !(await rectText('Jelajahi katalog latihan RepDB'));i++)await scroll(450,360);
 await clickText('Jelajahi katalog latihan RepDB');await until(async()=> (await text()).includes('24 latihan tersedia'),'Mobile catalog ready');await c.shot('activity-catalog-dialog-360');
 await prompt('Cari nama latihan (bahasa Inggris)','Archer Pull Ups');await until(async()=> (await text()).includes('1 latihan tersedia'),'Mobile catalog search');await scroll(10000,360);await clickText('Lihat detail');await scroll(-10000,360);await c.shot('activity-detail-dialog-360');await scroll(500,360);await c.shot('activity-detail-poses-360');await scroll(10000,360);await c.shot('activity-detail-dialog-bottom-360');await clickText('Tutup katalog');proof.push({test:'mobileCatalogDetailScroll',passed:true,width:360});
 await c.resize(1440);
 await key('F10','F10',121);await until(async()=> (await text()).includes('LULUS: view A ditolak setelah login B'),'Stale A rejection');await c.shot('stale-view-A-after-B');proof.push({test:'staleViewAfterReplacement',passed:true,owner:2});
 fs.writeFileSync('docs/screenshots/ui-3/browser-interactions-core.json',JSON.stringify({generatedAt:new Date().toISOString(),proof},null,2));
 }
 // Fresh independent browser context: real mobile menu routes and logout, while the first retains B.
 control=await connect('browser');
 browserContextId=(await control.send('Target.createBrowserContext')).browserContextId;
 const targetId=(await control.send('Target.createTarget',{url:'http://127.0.0.1:8087/',browserContextId})).targetId;await wait(5500);
 other=await connect(targetId);
 const clickOther=async value=>{const r=await other.evaluate(`(()=>{const a=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent===${JSON.stringify(value)}).map(e=>e.getBoundingClientRect()).filter(r=>r.width>0&&r.y>0&&r.bottom<innerHeight-(${JSON.stringify(value)}==='Hapus'?64:0)+1);let r=a[a.length-1];return r?{x:r.x+r.width/2,y:r.y+r.height/2}:null;})()`);if(!r)throw Error('Mobile control missing '+value);await other.click(r.x,r.y);await wait(450);};
 await other.resize(360);await clickOther('Catatan');await other.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:250,y:680,deltaY:10000,deltaX:0});await wait(400);await clickOther('Hapus');await wait(400);await other.shot('delete-cancel-360');await clickOther('Batal');await other.key('F9','F9',120);await wait(250);if(!(await other.evaluate('document.body.innerText')).includes('SQL fixture=0'))throw Error('Cancel deletion wrote data');proof.push({test:'deleteCancel',passed:true,additionalWrites:0});
 await clickOther('Progres');await wait(400);if(!(await other.evaluate('document.body.innerText')).includes('Progres mingguan'))throw Error('Progress navigation failed');await other.shot('mobile-navigation-Progres');await clickOther('Beranda');await wait(400);if(!(await other.evaluate('document.body.innerText')).includes('Rutinitas Sehat Hari Ini'))throw Error('Home navigation failed');await clickOther('Catatan');await wait(400);proof.push({test:'mobilePrimaryNavigation',routes:['Progres','Beranda','Catatan'],passed:true});
 for(const route of ['Tantangan','Rekomendasi','Tidur','Evaluasi','Profil']){await clickOther('Lainnya');await wait(250);await clickOther(route);await wait(500);await other.shot('mobile-navigation-'+route);proof.push({test:'mobileNavigation',route,passed:true});}
 await clickOther('Lainnya');await wait(250);await clickOther('Keluar');await until(async()=> !(await other.evaluate('document.body.innerText')).includes('RUTE:'),'Logout removes private fixture shell');await other.shot('logout-360');
 await c.key('F9','F9',120);await wait(250);
 if(!/PRATINJAU SINTETIS · SQL fixture=\d+ · owner=2/.test(await text()))throw Error('Other browser logout changed first session');proof.push({test:'independentBrowserLogout',passed:true});
 other.ws.close();await control.send('Target.disposeBrowserContext',{browserContextId});
 fs.writeFileSync('docs/screenshots/ui-3/browser-interactions.json',JSON.stringify({generatedAt:new Date().toISOString(),proof},null,2));console.log(JSON.stringify(proof,null,2));
} finally {if(fs.existsSync(missingBackup))fs.renameSync(missingBackup,missingAsset);fs.writeFileSync('/tmp/ui3-interactions-progress.json',JSON.stringify(proof,null,2));if(other)other.ws.close();if(control&&browserContextId)await control.send('Target.disposeBrowserContext',{browserContextId}).catch(()=>{});if(control)control.ws.close();c.ws.close();}
