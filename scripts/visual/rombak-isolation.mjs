import fs from 'node:fs';
import {execFileSync} from 'node:child_process';
import {connect,wait} from './latihan-cdp.mjs';
const schema=process.env.ROMBAK_DB;if(!schema?.startsWith('sehatharian_test_rombak_release_'))throw Error('disposable release schema required');
const sql=q=>execFileSync('mariadb',['--no-defaults','-h127.0.0.1','-P13317','-uroot','--batch','--skip-column-names',schema,'-e',q],{encoding:'utf8'}).trim();
sql("INSERT INTO pengguna(nama,email,password_hash,onboarding_selesai) SELECT 'Sesi Beta','beta@fixture.local',password_hash,TRUE FROM pengguna WHERE email='nadia@sehatharian.local' AND NOT EXISTS(SELECT 1 FROM pengguna WHERE email='beta@fixture.local')");
const browser=await connect('browser'),contexts=[],clients=[];
async function click(c,text){for(let i=0;i<20;i++){let r=await c.evaluate(`(()=>{let e=[...document.querySelectorAll('*')].find(e=>e.childElementCount===0&&e.textContent.trim()===${JSON.stringify(text)}&&e.getBoundingClientRect().width);if(!e)return null;let r=e.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2}})()`);if(r){await c.click(r.x,r.y);await wait(800);return;}await wait(300);}throw Error('click '+text);}
async function until(c,value){for(let i=0;i<80;i++){if((await c.evaluate('document.body.innerText')).includes(value))return;await wait(500);}throw Error('not visible '+value);}
async function open(email,greeting){const {browserContextId}=await browser.send('Target.createBrowserContext');contexts.push(browserContextId);const {targetId}=await browser.send('Target.createTarget',{url:'http://localhost:8088',browserContextId});await wait(2500);let c=await connect(targetId);clients.push(c);await c.resize(1440);await until(c,'Masukkan kata sandi Anda');await click(c,'misal: nadia@sehatharian.id');await c.send('Input.insertText',{text:email});await click(c,'Masukkan kata sandi Anda');await c.send('Input.insertText',{text:'password123'});await c.key('Enter','Enter',13);await until(c,greeting);return c;}
try{
 const a=await open('nadia@sehatharian.local','Halo, Nadia'),b=await open('beta@fixture.local','Halo, Sesi');
 const count=email=>Number(sql(`SELECT COUNT(*) FROM hidrasi WHERE pengguna_id=(SELECT id FROM pengguna WHERE email='${email}')`));
 const evidence={schema,beforeA:count('nadia@sehatharian.local'),beforeB:count('beta@fixture.local')};
 await click(a,'Hidrasi');await until(a,'Target pilihan Anda');await click(a,'+ 250 ml');await wait(1200);
 await click(b,'Keluar ke Beranda');await wait(1200);await click(a,'Hidrasi');await until(a,'Target pilihan Anda');
 evidence.afterA=count('nadia@sehatharian.local');evidence.afterB=count('beta@fixture.local');evidence.aStillActive=(await a.evaluate('document.body.innerText')).includes('Target pilihan Anda');evidence.bLoggedOut=(await b.evaluate('document.body.innerText')).includes('Masuk');
 if(evidence.afterA!==evidence.beforeA+1||evidence.afterB!==evidence.beforeB||!evidence.aStillActive||!evidence.bLoggedOut)throw Error('isolation failed '+JSON.stringify(evidence));
 fs.writeFileSync('docs/verification/rombak/browser-isolation-release.json',JSON.stringify(evidence,null,2));console.log(evidence);
}finally{for(let c of clients)c.ws.close();for(let browserContextId of contexts)await browser.send('Target.disposeBrowserContext',{browserContextId});browser.ws.close();}
