"""Fault/callback contract for the real browser controller; no physical-audio claim."""
from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[2]

class SleepAudioTest(unittest.TestCase):
    def test_controller_faults_and_identity_cleanup(self):
        script = r'''
const fs=require('fs'),vm=require('vm'),assert=require('assert');
const code=fs.readFileSync('Frontend2/src/main/resources/scripts/sleep-audio.js','utf8')
 .replaceAll('__KEY__','testAudio').replaceAll('__CALLBACK__','callback')
 .replaceAll('__SOURCE__','/local.wav').replaceAll('__ENABLED__','false').replaceAll('__VOLUME__','.35');
(async()=>{
 let events=[],started=0,stopped=0,listeners=new Map(),rejectResume=false,mediaFails=false;
 class AudioContext{
  state='suspended';destination={};
  async resume(){if(rejectResume)throw Error();this.state='running';}
  async decodeAudioData(){return {};}
  createGain(){return {gain:{},connect(){}};}
  createBufferSource(){return {connect(){},disconnect(){},start(){started++;},stop(){stopped++;}};}
  async close(){this.state='closed';}
 }
 const doc={hidden:false,addEventListener:(k,f)=>listeners.set(k,f),removeEventListener:k=>listeners.delete(k)};
 const sandbox={window:{AudioContext},document:doc,callback:x=>events.push(x),fetch:async()=>({ok:!mediaFails,arrayBuffer:async()=>new ArrayBuffer(4)}),setTimeout:()=>1,clearTimeout(){}};
 vm.runInNewContext(code,sandbox);const s=sandbox.window.testAudio;
 rejectResume=true;await s.activate();assert.equal(s.ready,false);assert.equal(events.at(-1),'BLOCKED');
 rejectResume=false;mediaFails=true;await s.activate();assert.equal(s.ready,false);
 mediaFails=false;await s.activate();assert.equal(s.ready,true);
 s.play(0);assert.equal(started,0);assert.equal(events.at(-1),'MUTED');
 s.play(0,true);assert.equal(started,1);assert.equal(events.at(-1),'PLAYING');
 s.stop();assert.equal(stopped,1);assert.equal(events.at(-1),'STOPPED');
 s.configure(true,.35);s.play(Date.now()-1);assert.equal(started,1);
 s.play(Date.now()+5000);assert.equal(started,2);
 doc.hidden=true;listeners.get('visibilitychange')();assert.equal(s.ready,false);assert.equal(stopped,2);
 doc.hidden=false;listeners.get('visibilitychange')();assert.equal(s.ready,false);
 await s.activate();assert.equal(s.ready,true);s.close();assert.equal(listeners.size,0);assert.equal(sandbox.window.testAudio,undefined);
 // A pending activate must never revive output after logout.
 vm.runInNewContext(code,sandbox);const t=sandbox.window.testAudio;const pending=t.activate();t.close();await pending;assert.equal(t.ready,false);
 console.log('browser audio faults/preview/expiry/visibility/cleanup passed');
})().catch(e=>{console.error(e);process.exit(1)});
'''
        result = subprocess.run(['node', '-e', script], cwd=ROOT, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

if __name__ == '__main__':
    unittest.main()
