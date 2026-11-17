import fs from 'node:fs';
import crypto from 'node:crypto';
import {execFileSync} from 'node:child_process';
import {connect,wait} from './latihan-cdp.mjs';
const mode=process.argv[2]||'inspect',db=process.env.RECOVERY_DB;
if(!db?.startsWith('sehatharian_test_recovery_'))throw Error('Dedicated recovery schema required');
const out='docs/verification/pemulihan-latihan',shots='docs/screenshots/pemulihan-latihan';fs.mkdirSync(shots,{recursive:true});
const c=await connect(),e={mode,db,started:new Date().toISOString(),screens:[]};
const sql=q=>execFileSync('mariadb',['--no-defaults','-h127.0.0.1','-P13317','-uroot','--batch','--skip-column-names',db,'-e',q],{encoding:'utf8',stdio:['ignore','pipe','pipe']}).trim();
const owner="(SELECT id FROM pengguna WHERE email='recovery@example.test')";
const count=table=>Number(sql(`SELECT COUNT(*) FROM ${table} WHERE pengguna_id=${owner}`));
function prefix(hex){
 const b=Buffer.from(hex,'hex');let offset=0;const i=()=>{const v=b.readInt32BE(offset);offset+=4;return v;};const u=()=>{const n=b.readUInt16BE(offset);offset+=2;const v=b.subarray(offset,offset+n).toString('utf8');offset+=n;return v;};const n=()=>{const v=b.readBigInt64BE(offset).toString();offset+=8;return v;};
 const magic=i(),version=i(),operation=u(),program=i(),programVersion=i(),name=u(),preparation=i(),rest=i(),index=i(),phase=u(),confirmedFrom=u(),elapsedNanos=n(),activeNanos=n();const paused=!!b.readUInt8(offset++),stopped=!!b.readUInt8(offset++);if(b.readUInt8(offset++))n();if(b.readUInt8(offset++))n();const phaseRevision=n(),stepCount=i();return {magic,version,operation,program,programVersion,name,preparation,rest,index,phase,confirmedFrom,elapsedNanos,activeNanos,paused,stopped,phaseRevision,stepCount};
}
function checkpoint(email='recovery@example.test'){
 const selectedOwner="(SELECT id FROM pengguna WHERE email='"+email+"')";
 const raw=sql(`START TRANSACTION; SELECT operation_id,holder_token,revision,checkpoint_at,HEX(payload),payload_hash,status FROM training_checkpoint WHERE pengguna_id=${selectedOwner} ORDER BY checkpoint_at DESC LIMIT 1 FOR UPDATE; COMMIT;`);
 if(!raw)return null;const [operation,holder,revision,at,hex,hash,status]=raw.split('\t');
 if(status!=='ACTIVE')return {operation,holder,revision:Number(revision),at,status};
 if(crypto.createHash('sha256').update(Buffer.from(hex,'hex')).digest('hex')!==hash)throw Error('checkpoint SQL payload hash mismatch');
 return {operation,holder,revision:Number(revision),at,status,hash,...prefix(hex)};
}
const body=()=>c.evaluate('document.body.innerText');
const reset=()=>c.evaluate("window.scrollTo(0,0);document.querySelectorAll('.jpro-application').forEach(e=>e.scrollTop=0)");
async function wheel(deltaY){await reset();await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:280,y:420,deltaX:0,deltaY});await wait(250);}
async function top(){await wheel(-25000);}
async function until(s,ms=90000){for(let i=0;i<ms/400;i++){if((await body()).includes(s))return;await wait(400);}throw Error('Missing '+s+'\n'+await body());}
async function click(s,index=0){await reset();for(let i=0;i<30;i++){const p=await c.evaluate(`(()=>{let a=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent.trim()===${JSON.stringify(s)}).map(e=>e.getBoundingClientRect()).filter(r=>r.width&&r.y>=0&&r.bottom<innerHeight-(innerWidth<768?62:5));let r=a.at(${index});return r?{x:r.x+r.width/2,y:r.y+r.height/2}:null})()`);if(p){await c.click(p.x,p.y);await wait(650);return;}await wheel(300);}throw Error('Cannot click '+s+'\n'+await body());}
async function resize(w){await c.resize(w);await top();}
async function shot(name){await reset();await wait(350);if((await body()).includes('Reconnecting'))throw Error('Reconnect overlay in '+name);const r=await c.send('Page.captureScreenshot',{format:'png'});fs.writeFileSync(`${shots}/${name}.png`,Buffer.from(r.data,'base64'));e.screens.push({name,...await c.evaluate('({width:innerWidth,height:innerHeight,scrollWidth:document.documentElement.scrollWidth,text:document.body.innerText})')});}
async function login(email='recovery@example.test'){
 await c.send('Page.reload');await resize(1440);await until('Masukkan kata sandi Anda');await click('misal: nadia@sehatharian.id');await c.send('Input.insertText',{text:email});await click('Masukkan kata sandi Anda');await c.send('Input.insertText',{text:'fixture123'});await c.key('Enter','Enter',13);await until('Keluar ke Beranda');await click('Latihan');
}
try{
 if(mode==='inspect'){console.log(await body());}
 if(mode==='start'){
  await login();await until('Program Pemulihan · lihat program');if(checkpoint()?.status==='ACTIVE')throw Error('unexpected old browser checkpoint');
  e.activitiesBefore=count('aktivitas');e.historiesBefore=count('training_session');await click('Program Pemulihan · lihat program');await click('Gunakan program & tinjau');await until('Mulai persiapan');await resize(390);await click('Mulai persiapan');await until('Konfirmasi selesai / berikutnya');await click('Konfirmasi selesai / berikutnya');await until('Latihan aktif');
  for(let i=0;i<120;i++){const value=checkpoint();if(value?.phase==='EXERCISE'&&Number(value.activeNanos)>=10e9){e.beforeKill=value;break;}await wait(400);}
  if(!e.beforeKill)throw Error('no confirmed active checkpoint');await until('Checkpoint terkonfirmasi');await top();await shot('checkpoint-sebelum-stop-390');e.memoryBeforeKill=await body();
  const pid=Number(process.env.RECOVERY_JPRO_PID);if(!Number.isInteger(pid)||pid<=1)throw Error('explicit test JPro pid required');
  const env=fs.readFileSync(`/proc/${pid}/environ`,'utf8').split('\0');if(!env.includes(`DB_NAME=${db}`))throw Error('process is not using dedicated fixture database');
  e.killedPid=pid;e.killedAt=new Date().toISOString();process.kill(pid,'SIGKILL');await wait(500);e.persistedAfterKill=checkpoint();
  if(count('aktivitas')!==e.activitiesBefore||count('training_session')!==e.historiesBefore)throw Error('checkpoint created result before Save');
 }
 if(mode==='recover'){
  const start=JSON.parse(fs.readFileSync(`${out}/browser-start.json`));e.previousProcessPid=start.killedPid;e.durable=start.persistedAfterKill;
  await login();await until('Lanjutkan sesi tersimpan');
  for(const width of [390,360,1440]){await resize(width);await top();await shot('kartu-pemulihan-'+width);}
  e.beforeClaim=checkpoint();if(e.beforeClaim.operation!==e.durable.operation||e.beforeClaim.activeNanos!==e.durable.activeNanos)throw Error('offline time modified checkpoint');
  await resize(390);await click('Lanjutkan sesi tersimpan');await until('Pulihkan & ambil alih');await click('Pulihkan & ambil alih');await until('Dijeda · latihan');
  for(const width of [390,360,1440]){await resize(width);await top();await shot('sesi-pulih-jeda-'+width);}
  e.pausedVideo=await c.evaluate("[...document.querySelectorAll('video')].map(v=>({paused:v.paused,autoplay:v.autoplay,ready:v.readyState}))");
  if(e.pausedVideo.some(v=>!v.paused||v.autoplay))throw Error('restored video autoplay');e.afterClaim=checkpoint();await wait(2200);e.afterWaiting=checkpoint();
  if(e.afterWaiting.operation!==e.durable.operation||e.afterWaiting.activeNanos!==e.durable.activeNanos)throw Error('paused restored time received activity credit');
  if(e.afterClaim.holder===e.durable.holder)throw Error('takeover did not rotate token');
  await resize(390);await top();await click('Lanjutkan');await until('Latihan aktif');await until('Waktu berakhir · konfirmasi untuk lanjut',75000);await click('Konfirmasi selesai / berikutnya');await until('Durasi aktif:');await top();await shot('ringkasan-pulih-390');
  e.activitiesBeforeSave=count('aktivitas');await click('Simpan aktivitas');await until('Data berhasil disimpan.');e.activitiesAfter=count('aktivitas');e.historiesAfter=count('training_session');
  if(e.activitiesAfter!==start.activitiesBefore+1||e.historiesAfter!==start.historiesBefore+1||e.activitiesBeforeSave!==start.activitiesBefore)throw Error('result row counts incorrect');
  e.result=sql(`SELECT t.operation_id,t.active_nanos,SUM(s.active_nanos),t.activity_id,a.durasi_menit,t.id FROM training_session t JOIN training_step s ON s.session_id=t.id JOIN aktivitas a ON a.id=t.activity_id WHERE t.pengguna_id=${owner} GROUP BY t.id ORDER BY t.id DESC LIMIT 1`).split('\t');
  if(e.result[0]!==e.durable.operation||e.result[1]!==e.result[2]||e.result[1]!=='60000000000')throw Error('recovered result identity/time mismatch');e.terminal=checkpoint();if(e.terminal.status!=='COMPLETED')throw Error('checkpoint not atomically completed');
  await click('Riwayat latihan');await until('Lihat rincian sesi');await top();await shot('riwayat-hasil-pulih-390');await click('Lihat rincian sesi');await until('Rincian saat latihan dilakukan');await top();await shot('detail-hasil-pulih-390');
 }
 if(mode==='states'){
  const draftOwner="(SELECT id FROM pengguna WHERE email='ready@example.test')";
  await login('ready@example.test');await until('Lanjutkan sesi tersimpan');e.original=checkpoint('ready@example.test');
  try{sql(`UPDATE training_checkpoint SET format_version=99 WHERE pengguna_id=${draftOwner} AND status='ACTIVE'`);await click('Periksa status sesi');await until('Checkpoint belum dapat dipulihkan');await resize(390);await top();await shot('checkpoint-tidak-kompatibel-390');}
  finally{sql(`UPDATE training_checkpoint SET format_version=1 WHERE pengguna_id=${draftOwner} AND status='ACTIVE'`);}
  try{sql('RENAME TABLE training_checkpoint_slot TO training_checkpoint_slot_unavailable');await click('Periksa status sesi');await until('Status checkpoint dan hasil belum dapat dipastikan.');await top();await shot('checkpoint-gagal-dimuat-390');}
  finally{sql('RENAME TABLE training_checkpoint_slot_unavailable TO training_checkpoint_slot');}
  await click('Periksa status sesi');await until('Lanjutkan sesi tersimpan');await click('Lanjutkan sesi tersimpan');await click('Pulihkan & ambil alih');await until('Urutan: '+e.original.stepCount+' / 20 gerakan');
  for(let i=0;i<60&&!checkpoint('ready@example.test').paused;i++)await wait(200);
  e.confirmedBeforeFailure=checkpoint('ready@example.test');
  try{
   sql("CREATE TRIGGER recovery_checkpoint_fail BEFORE UPDATE ON training_checkpoint FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture checkpoint write failure'");
   await click('Hapus');await until('Checkpoint belum terkonfirmasi.');await top();await shot('checkpoint-gagal-tulis-390');e.localAfterFailure=await body();e.confirmedAfterFailure=checkpoint('ready@example.test');
   if(e.confirmedAfterFailure.stepCount!==e.original.stepCount||!e.localAfterFailure.includes('Urutan: '+(e.original.stepCount-1)+' / 20 gerakan'))throw Error('failed checkpoint did not preserve separate local and confirmed progress');
  }finally{sql('DROP TRIGGER IF EXISTS recovery_checkpoint_fail');}
  await click('Simpan progres sekarang');for(let i=0;i<60&&checkpoint('ready@example.test').stepCount!==e.original.stepCount-1;i++)await wait(200);
  e.afterRetry=checkpoint('ready@example.test');if(e.afterRetry.stepCount!==e.original.stepCount-1)throw Error('explicit checkpoint retry failed');
  await click('Buang sesi');await until('Buang progres sesi');await click('Batal');if(checkpoint('ready@example.test').status!=='ACTIVE')throw Error('cancel discarded checkpoint');
  await click('Buang sesi');await until('Buang progres sesi');await click('Buang sesi',-1);await until('Progres sesi sudah dibuang.');e.discarded=checkpoint('ready@example.test');if(e.discarded.status!=='DISCARDED')throw Error('discard not terminal');await top();await shot('checkpoint-dibuang-390');
 }
 if(mode==='instance-a'){
  await login('exercise@example.test');await until('Lanjutkan sesi tersimpan');await resize(390);await click('Lanjutkan sesi tersimpan');await click('Pulihkan & ambil alih');await until('Dijeda · latihan');await click('Lanjutkan');await until('Latihan aktif');e.samples=[];
  for(let i=0;i<40;i++){const sample=checkpoint('exercise@example.test');if(!e.samples.length||e.samples.at(-1).revision!==sample.revision)e.samples.push(sample);await wait(400);}
  e.intervalMilliseconds=e.samples.slice(1).map((sample,index)=>Date.parse(sample.at.replace(' ','T')+'Z')-Date.parse(e.samples[index].at.replace(' ','T')+'Z'));
  e.owned=checkpoint('exercise@example.test');await top();await shot('instance-a-sebelum-ambil-alih-390');
 }
 if(mode==='instance-b'){
  await login('exercise@example.test');await until('Lanjutkan sesi tersimpan');await click('Tema terang / gelap');await resize(390);await top();await shot('kartu-pemulihan-gelap-390');await click('Lanjutkan sesi tersimpan');await click('Pulihkan & ambil alih');await until('Dijeda ·');e.owned=checkpoint('exercise@example.test');await top();await shot('instance-b-pulih-jeda-390');
  const a=JSON.parse(fs.readFileSync(`${out}/browser-instance-a.json`));if(e.owned.holder===a.owned.holder)throw Error('two instances kept same holder');
 }
 if(mode==='instance-a-check'){
  await c.send('Page.bringToFront');await until('Instance ini tidak dapat mengubah checkpoint',15000);e.text=await body();e.owner=checkpoint('exercise@example.test');await resize(390);await top();await shot('instance-a-kehilangan-hak-390');
  if(e.text.includes('Simpan aktivitas')||e.text.includes('Konfirmasi selesai / berikutnya'))throw Error('stale instance kept mutation controls');
 }
 if(mode==='instance-b-discard'){
  await c.send('Page.bringToFront');await click('Buang sesi');await until('Buang progres sesi');await click('Buang sesi',-1);await until('Progres sesi sudah dibuang.');e.terminal=checkpoint('exercise@example.test');
 }
 if(mode==='terminal'){
  const previous=checkpoint();await login();await until('Rincian saat latihan dilakukan');e.terminal=checkpoint();if(e.terminal.status!=='COMPLETED'||e.terminal.operation!==previous.operation)throw Error('committed checkpoint offered again');await resize(390);await shot('receipt-setelah-login-390');
 }
}catch(error){e.error=String(error);try{e.body=await body();}catch{}throw error;}
finally{e.finished=new Date().toISOString();fs.writeFileSync(`${out}/browser-${mode}.json`,JSON.stringify(e,null,2));c.ws.close();}
