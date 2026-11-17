import fs from 'node:fs';
import {execFileSync} from 'node:child_process';
import {connect,wait} from './latihan-cdp.mjs';
process.env.CDP_PORT??='9236';process.env.APP_PORT??='8096';
const mode=process.argv[2]||'inspect';
const db=process.env.DB_NAME||'sehatharian_test_sleep_browser2_20261116';
if(!/^sehatharian_test_sleep_[a-z0-9_]+$/.test(db))throw Error('Dedicated sleep schema required');
const out=process.env.SLEEP_EVIDENCE||'docs/verification/tidur-notifikasi';
const shots=process.env.SLEEP_SCREENSHOTS||'docs/screenshots/tidur-notifikasi';
fs.mkdirSync(out,{recursive:true});fs.mkdirSync(shots,{recursive:true});
const c=await connect(),e={mode,db,appPort:process.env.APP_PORT,started:new Date().toISOString(),screens:[]};
const body=()=>c.evaluate("document.body?.innerText||''");
const sql=q=>execFileSync('mariadb',['--no-defaults','-h127.0.0.1','-P'+(process.env.DB_PORT||'13317'),'-uroot','--batch','--skip-column-names',db,'-e',q],{encoding:'utf8'}).trim();
const owner="(SELECT id FROM pengguna WHERE email='sleep@example.test')";
async function until(s,ms=45000){for(let i=0;i<ms/300;i++){if((await body()).includes(s))return;await wait(300);}throw Error('Missing '+s+'\n'+await body());}
async function top(){await c.evaluate("window.scrollTo(0,0);document.querySelectorAll('.jpro-application').forEach(e=>e.scrollTop=0)");const x=await c.evaluate('innerWidth>768?innerWidth*.6:innerWidth/2');await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x,y:350,deltaX:0,deltaY:-40000});await wait(200);}
async function click(text,index=0){await top();for(let i=0;i<40;i++){let p=await c.evaluate(`(()=>{let a=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent.trim()===${JSON.stringify(text)}&&e.getBoundingClientRect().width>0);let r=a.at(${index})?.getBoundingClientRect();return r&&r.width&&r.y>=0&&r.bottom<innerHeight-(innerWidth<768?62:5)?{x:r.x+r.width/2,y:r.y+r.height/2}:null})()`);if(p){await c.click(p.x,p.y);await wait(450);return;}const x=text==='Keluar ke Beranda'?150:await c.evaluate('innerWidth>768?innerWidth*.6:innerWidth/2');await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x,y:450,deltaX:0,deltaY:240});await wait(130);}throw Error('Cannot click '+text+'\n'+await body());}
async function shot(name){await wait(400);const r=await c.send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(`${shots}/${name}.png`,Buffer.from(r.data,'base64'));e.screens.push({name,...await c.evaluate('({width:innerWidth,height:innerHeight,scrollWidth:document.documentElement.scrollWidth,text:document.body.innerText})')});}
async function login(email='sleep@example.test'){
 await c.send('Page.navigate',{url:`http://127.0.0.1:${process.env.APP_PORT}`});await c.resize(1440);await until('Masukkan kata sandi Anda');
 await click('misal: nadia@sehatharian.id');await c.send('Input.insertText',{text:email});
 await click('Masukkan kata sandi Anda');await c.send('Input.insertText',{text:'fixture123'});await c.key('Enter','Enter',13);
 await until('Keluar ke Beranda');await click((await body()).includes('Jadwal & Alarm Tidur')?'Jadwal & Alarm Tidur':'Tidur');await until('Aktual · bangun hari ini');
}
const clock=value=>{if(process.env.SLEEP_PACKAGE)throw Error('Production package clock cannot be overridden');fs.writeFileSync(process.env.SLEEP_TEST_CLOCK||'/tmp/sehatharian-sleep-clock',value);};
async function input(index,text){const p=await c.evaluate(`(()=>{const e=[...document.querySelectorAll('input')][${index}];if(!e)return null;const r=e.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2};})()`);if(!p)throw Error('Input absent '+index);await c.click(p.x,p.y);await c.key('a','KeyA',65,2);await c.send('Input.insertText',{text});await c.key('Tab','Tab',9);}
try{
 if(mode==='target'){e.target=await c.send('Target.createTarget',{url:`http://127.0.0.1:${process.env.SLEEP_OPEN_PORT||'8098'}`});console.log(JSON.stringify(e));}
 if(mode==='inspect'){e.body=await body();e.inputs=await c.evaluate("[...document.querySelectorAll('input')].map((x,i)=>({i,value:x.value,placeholder:x.placeholder,type:x.type,rect:x.getBoundingClientRect().toJSON()}))");console.log(JSON.stringify(e,null,2));}
 if(mode==='login'){await login();e.body=await body();console.log(e.body);}
 if(mode==='page'){
  for(const width of [390,360,1440]){await c.resize(width);await top();await shot('tidur-'+width);}
  await click('Suara pengingat');await shot('suara-390');
 }
 if(mode==='flow'){
  clock('2026-11-16T14:49:00Z');await login();await c.resize(1440);await top();
  await click('Ubah jadwal');await click('Simpan jadwal');await until('Jadwal tersimpan.');
  e.schedule=sql(`SELECT j.tanggal,j.jam_tidur,j.jam_bangun,j.pengingat_aktif FROM sleep_active_plan a JOIN jadwal_tidur j ON j.id=a.schedule_id WHERE a.pengguna_id=${owner}`);
  await c.evaluate(`(()=>{window.__sleepEvidence={starts:[],stops:[]};const start=AudioBufferSourceNode.prototype.start,stop=AudioBufferSourceNode.prototype.stop;AudioBufferSourceNode.prototype.start=function(...a){window.__sleepEvidence.starts.push({at:Date.now(),duration:this.buffer?.duration});return start.apply(this,a)};AudioBufferSourceNode.prototype.stop=function(...a){window.__sleepEvidence.stops.push(Date.now());return stop.apply(this,a)}})()`);
  await click('Aktifkan suara');await until('Audio siap pada instance ini.');
  await click('Coba suara');await until('Pemutar memulai suara;');await shot('coba-suara-1440');
  await click('Hentikan alarm');await until('Suara dihentikan.');
  if(!sql(`SELECT enabled FROM sleep_sound_preference WHERE pengguna_id=${owner}`).startsWith('1'))await click('Suara pengingat',-1);await click('Simpan pilihan suara');await until('Pilihan suara tersimpan.');
  e.sound=sql(`SELECT enabled,volume FROM sleep_sound_preference WHERE pengguna_id=${owner}`);if(!e.sound.startsWith('1'))throw Error('sound preference not enabled');
  await wait(5500);clock('2026-11-16T14:50:00Z');await until('Pengingat: sekitar 10 menit lagi');await c.resize(390);await top();await shot('persiapan-390');
  for(let minute=51;minute<=59;minute++){clock(`2026-11-16T14:${minute}:00Z`);await wait(550);}
  clock('2026-11-16T14:59:55Z');await until('Waktu tidur tiba (WIB).');await until('Pemutar memulai suara;');
  e.claim=sql(`SELECT occurrence,kind FROM sleep_alarm_claim WHERE pengguna_id=${owner}`);await c.resize(390);await top();await shot('alarm-390');
  await wait(6700);e.audio=await c.evaluate('window.__sleepEvidence');if(e.audio.starts.length!==2)throw Error('preview and one automatic playback expected: '+JSON.stringify(e.audio));
  await click('Hentikan alarm');
 }
 if(mode==='flow'||mode==='finish'){
  await c.resize(1440);
  // Same occurrence after navigation cannot play again.
  await click('Beranda');await click((await body()).includes('Jadwal & Alarm Tidur')?'Jadwal & Alarm Tidur':'Tidur');await until('Aktual · bangun hari ini');await wait(5500);
  e.audio=await c.evaluate('window.__sleepEvidence');
  if((await c.evaluate('window.__sleepEvidence.starts.length'))!==2)throw Error('navigation replayed alarm');
  await c.resize(1440);await click('Catat tidur');await click('22:00',-1);await c.key('a','KeyA',65,2);await c.send('Input.insertText',{text:'23:00'});await c.key('Tab','Tab',9);
  await click('06:00',-1);await c.key('a','KeyA',65,2);await c.send('Input.insertText',{text:'03:00'});await c.key('Tab','Tab',9);
  await click('Simpan tidur aktual');await until('Tidur aktual tersimpan.');await until('Saran tersedia');
  e.actual=sql(`SELECT started_at,ended_at,trigger_name FROM sleep_actual WHERE pengguna_id=${owner} ORDER BY id DESC LIMIT 1`);
  e.results=sql(`SELECT wake_day,revision,state,summary,details,stale FROM sleep_evaluation_result WHERE pengguna_id=${owner} ORDER BY id DESC LIMIT 3`);
  await click('Hasil terdahulu');await c.resize(390);await shot('evaluasi-390');
  for(const width of [390,360,1440]){await c.resize(width);await top();await shot('tidur-'+width);}
 }
 if(mode==='save-failure'){
  await c.resize(1440);
  if(!(await body()).includes('Revisi'))await click('Riwayat tidur aktual');await click('Revisi');
  if(!(await body()).includes('Simpan tidur aktual'))await click('Catat atau revisi tidur');
  await click('03:00');await c.key('a','KeyA',65,2);await c.send('Input.insertText',{text:'02:59'});await c.key('Tab','Tab',9);
  e.before=sql(`SELECT COUNT(*) FROM sleep_actual WHERE pengguna_id=${owner}`);
  sql("CREATE TRIGGER sleep_test_evaluation_fail BEFORE INSERT ON sleep_evaluation_result FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='controlled sleep evaluation failure'");
  try{
   await click('Simpan tidur aktual');await until('Tidur aktual tersimpan.');await until('Evaluasi belum berhasil.');
   e.persisted=sql(`SELECT ended_at FROM sleep_actual WHERE pengguna_id=${owner} ORDER BY ended_at DESC LIMIT 1`);
   e.pending=sql(`SELECT state FROM sleep_evaluation_request WHERE pengguna_id=${owner} AND wake_day='2026-11-16'`);
   if(e.pending!=='PENDING'||e.before!==sql(`SELECT COUNT(*) FROM sleep_actual WHERE pengguna_id=${owner}`))throw Error('save/evaluation failure boundary incorrect');
   await c.resize(390);await shot('catatan-tersimpan-evaluasi-gagal-390');
  }finally{sql('DROP TRIGGER IF EXISTS sleep_test_evaluation_fail');}
  await c.resize(1440);await click('Periksa / coba lagi evaluasi');await until('Saran tersedia');
  e.after=sql(`SELECT state,revision FROM sleep_evaluation_request WHERE pengguna_id=${owner} AND wake_day='2026-11-16'`);
  await c.resize(390);await shot('evaluasi-pulih-390');
 }
 if(mode==='tabs'){
  e.firstKeys=await c.evaluate("Object.keys(window).filter(k=>k.startsWith('sleepAudio'))");
  const target=await c.send('Target.createTarget',{url:`http://127.0.0.1:${process.env.APP_PORT}`});e.secondTarget=target.targetId;
  execFileSync(process.execPath,[process.argv[1],'tab-same'],{env:{...process.env,UI3_TARGET_ID:target.targetId},stdio:['ignore','pipe','pipe']});
  await c.send('Page.bringToFront');e.firstAfterOtherTab=await c.evaluate("Object.keys(window).filter(k=>k.startsWith('sleepAudio')).map(k=>({key:k,ready:window[k].ready}))");
  if(e.firstAfterOtherTab.some(x=>x.ready))throw Error('returning hidden tab kept automatic readiness');
  await c.resize(390);await top();await shot('aktivasi-ulang-setelah-tab-390');
 }
 if(mode==='tab-same'){
  await login();await c.resize(1440);
  await c.evaluate(`(()=>{window.__secondStarts=0;const orig=AudioBufferSourceNode.prototype.start;AudioBufferSourceNode.prototype.start=function(...a){window.__secondStarts++;return orig.apply(this,a)}})()`);
  await click('Aktifkan suara');await until('Audio siap pada instance ini.');await wait(5500);
  e.starts=await c.evaluate('window.__secondStarts');e.claims=sql(`SELECT COUNT(*) FROM sleep_alarm_claim WHERE pengguna_id=${owner}`);
  if(e.starts!==0||e.claims!=='1')throw Error('same account tab replayed automatic audio');
  await c.resize(390);await top();await shot('tab-kedua-tanpa-alarm-ulang-390');
  await c.resize(1440);await click('Keluar ke Beranda');await until('Masuk');
  e.keysAfterLogout=await c.evaluate("Object.keys(window).filter(k=>k.startsWith('sleepAudio'))");if(e.keysAfterLogout.length)throw Error('old audio state after logout');
  sql("UPDATE pengguna SET onboarding_selesai=TRUE,golongan_usia='dewasa',golongan_kesibukan='dalam_rumah',waktu_luang_menit=60 WHERE email='other@example.test'");
  await login('other@example.test');await until('Belum ada jadwal tersimpan.');
  e.other=await body();if(e.other.includes('Saran tersedia')||e.other.includes('Tidur aktual tersimpan.'))throw Error('other identity received private result');
  await c.resize(390);await top();await shot('akun-kedua-kosong-390');
 }
 if(mode==='package'){
  await login();e.body=await body();e.migrations=sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('sleep_active_plan','sleep_sound_preference','sleep_alarm_claim','sleep_record_receipt','sleep_evaluation_request','sleep_evaluation_result')");
  if(e.migrations!=='6')throw Error('missing package sleep migrations');
  await c.resize(1440);await click('Aktifkan suara');await until('Audio siap pada instance ini.');await click('Coba suara');await until('Pemutar memulai suara;');await click('Hentikan alarm');await until('Suara dihentikan.');
  for(const width of [390,360,1440]){await c.resize(width);await top();await shot('paket-tidur-'+width);}
  e.latestMenu=fs.existsSync(process.env.SLEEP_PACKAGE+'/content/menus/lanjutan-2026.json');if(!e.latestMenu)throw Error('missing latest menu');
  e.payload=JSON.parse(fs.readFileSync(process.env.SLEEP_PACKAGE+'/RELEASE-INFO.json'));e.realClock=true;e.syntheticAlarmClockUsed=false;
 }
 if(mode==='audio-fault'){
  await login();await c.resize(1440);
  await c.evaluate(`(()=>{window.__denySleepAudio=true;const original=AudioContext.prototype.resume;AudioContext.prototype.resume=function(){return window.__denySleepAudio?Promise.reject(new DOMException('controlled autoplay rejection','NotAllowedError')):original.call(this)}})()`);
  await click('Aktifkan suara');await until('Audio belum siap atau dibatasi.');e.denied=await body();
  await c.resize(390);await shot('audio-ditolak-390');await c.resize(1440);
  await c.evaluate('window.__denySleepAudio=false');
  c.ws.addEventListener('message',event=>{const m=JSON.parse(event.data);if(m.method==='Fetch.requestPaused')c.send('Fetch.failRequest',{requestId:m.params.requestId,errorReason:'Failed'}).catch(()=>{});});
  await c.send('Fetch.enable',{patterns:[{urlPattern:'*',resourceType:'Fetch',requestStage:'Request'}]});
  try{await click('Aktifkan suara');await until('Berkas suara gagal dimuat.');e.mediaFailed=await body();await c.resize(390);await shot('media-gagal-390');}
  finally{await c.send('Fetch.disable');}
  await c.resize(1440);await click('Aktifkan suara');await until('Audio siap pada instance ini.');await click('Coba suara');await until('Pemutar memulai suara;');await click('Hentikan alarm');await until('Suara dihentikan.');e.recovered=true;
 }
 if(mode==='wire'){
 await c.resize(1440);const frames=[];await c.send('Network.enable');c.ws.addEventListener('message',event=>{const m=JSON.parse(event.data);if(m.method==='Network.webSocketFrameSent'&&/READY|STOPPED/.test(m.params.response.payloadData))frames.push(m.params.response.payloadData)});await click('Hentikan alarm');await click('Aktifkan suara');await wait(1200);e.frames=frames;console.log(JSON.stringify(e,null,2));
 }
 if(mode==='activation'){
 await c.resize(1440);
 const failures=[],logs=[];await c.send('Log.enable');c.ws.addEventListener('message',async event=>{const m=JSON.parse(event.data);if(m.method==='Log.entryAdded')logs.push(m.params.entry);if(m.method==='Debugger.paused'){failures.push(m.params.data);await c.send('Debugger.resume');}});
 await c.evaluate(`(()=>{window.__clicks=[];document.addEventListener('click',e=>window.__clicks.push({target:e.target.outerHTML,trust:e.isTrusted}),true)})()`);
 await c.send('Debugger.enable');await c.send('Debugger.setPauseOnExceptions',{state:'all'});
 await c.evaluate(`(()=>{window.__activation=[];const A=window.AudioContext;window.AudioContext=new Proxy(A,{construct(T,args){try{const ctx=new T(...args);window.__activation.push({made:ctx.state});const r=ctx.resume.bind(ctx);ctx.resume=async()=>{try{await r();window.__activation.push({resumed:ctx.state})}catch(e){window.__activation.push({error:e.name,message:e.message});throw e}};return ctx;}catch(e){window.__activation.push({constructError:e.name,message:e.message});throw e}}})})()`);
 await click('Aktifkan suara');await wait(2000);e.activation=await c.evaluate('window.__activation');e.failures=failures;e.logs=logs;e.clicks=await c.evaluate('window.__clicks');await c.send('Debugger.setPauseOnExceptions',{state:'none'});await c.send('Debugger.disable');console.log(JSON.stringify(e,null,2));
 }
 if(mode==='diagnose'){
 e.environment=await c.evaluate(`({buttons:[...document.querySelectorAll('button')].map(x=>x.outerHTML),width:innerWidth,hidden:document.hidden,activation:navigator.userActivation?.hasBeenActive,resources:performance.getEntriesByType('resource').filter(x=>x.name.includes('wav')).map(x=>({name:x.name,status:x.responseStatus})),audio:window.__sleepEvidence})`);
 console.log(JSON.stringify(e,null,2));
 }
 if(mode==='controls'){e.body=await body();e.inputs=await c.evaluate("[...document.querySelectorAll('input')].map((x,i)=>({i,value:x.value,placeholder:x.placeholder,type:x.type,rect:x.getBoundingClientRect().toJSON()}))");e.buttons=await c.evaluate("[...document.querySelectorAll('button')].map(x=>({text:x.innerText,rect:x.getBoundingClientRect().toJSON()}))");e.audio=await c.evaluate("Object.keys(window).filter(k=>k.startsWith('sleepAudio')).map(k=>({key:k,ready:window[k].ready}))");console.log(JSON.stringify(e,null,2));}
}catch(error){e.error=String(error);try{e.body=await body();await shot('failure-'+mode);}catch{}throw error;}
finally{e.finished=new Date().toISOString();fs.writeFileSync(`${out}/browser-${mode}.json`,JSON.stringify(e,null,2));c.ws.close();}
