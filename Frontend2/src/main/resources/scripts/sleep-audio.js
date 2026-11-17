/* Original browser audio controller; installed per authenticated JPro instance.
 * Activation and preview are native clicks. No device or permission is assumed. */
(()=>{
 const k='__KEY__';
 if(window[k])return;
 const send=value=>__CALLBACK__(value);
 let ctx,buffer,source,timer,activationTimer,activation=0,disposed=false,enabled=__ENABLED__,volume=__VOLUME__;
 const stop=()=>{
  clearTimeout(timer);
  if(source){source.onended=null;try{source.stop()}catch(_){}source.disconnect();source=null;}
 };
 const blocked=()=>{state.ready=false;stop();send('BLOCKED');};
 const visibility=()=>{
  if(document.hidden){stop();state.ready=false;send('HIDDEN');}
  // Returning to the tab always requires a new explicit activation.
 };
 const state={
  ready:false,
  configure:(on,vol)=>{enabled=on;volume=vol;if(!on)stop();},
  stop:()=>{stop();send('STOPPED');},
  close:()=>{disposed=true;activation++;clearTimeout(activationTimer);state.ready=false;stop();document.removeEventListener('visibilitychange',visibility);document.removeEventListener('click',click,true);if(ctx){ctx.onstatechange=null;ctx.close().catch(()=>{});}delete window[k];},
  activate:async()=>{
   const attempt=++activation;let phase='resume';
   try{
    if(!ctx){ctx=new(window.AudioContext||window.webkitAudioContext)();ctx.onstatechange=()=>{if(!disposed&&ctx.state!=='running')blocked();};}
    await Promise.race([ctx.resume(),new Promise((resolve,reject)=>{activationTimer=setTimeout(()=>reject(Error('activation timeout')),5000);})]);
    clearTimeout(activationTimer);
    if(disposed||attempt!==activation)return;
    phase='media';
    if(!buffer){const response=await fetch('__SOURCE__');if(!response.ok)throw Error('media unavailable');buffer=await ctx.decodeAudioData(await response.arrayBuffer());}
    if(disposed||attempt!==activation)return;
    state.ready=ctx.state==='running'&&!document.hidden;
    send(state.ready?'READY':'BLOCKED');
   }catch(_){clearTimeout(activationTimer);if(!disposed&&attempt===activation){state.ready=false;stop();send(phase==='media'?'MEDIA_FAILED':'BLOCKED');}}
  },
  play:(deadline,preview=false)=>{
   if(disposed)return;
   if(!preview&&!enabled){send('MUTED');return;}
   if(!state.ready||!buffer||!ctx||ctx.state!=='running'||document.hidden){blocked();return;}
   if(deadline&&Date.now()>deadline){send('EXPIRED');return;}
   stop();
   try{
    source=ctx.createBufferSource();source.buffer=buffer;
    const gain=ctx.createGain();gain.gain.value=Math.min(.65,Math.max(0,volume));
    source.connect(gain);gain.connect(ctx.destination);
    source.onended=()=>{stop();send('ENDED');};
    source.start();timer=setTimeout(()=>{stop();send('ENDED');},6000);
    send('PLAYING');
   }catch(_){blocked();}
  }
 };
 const click=event=>{
  const button=event.target.closest?.('button[data-sleep-audio]');
  if(!event.isTrusted||!button||button.dataset.sleepAudio!==k)return;
  switch(button.dataset.action){case 'activate':state.activate();break;case 'preview':state.play(0,true);break;case 'stop':state.stop();}
 };
 document.addEventListener('click',click,true);
 document.addEventListener('visibilitychange',visibility);window[k]=state;
})();
