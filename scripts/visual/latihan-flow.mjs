import {connect,wait} from './latihan-cdp.mjs';
const c=await connect();
const wheel=async dy=>{await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:700,y:650,deltaX:0,deltaY:dy});await wait(400);};
async function click(v){for(let i=0;i<18;i++){let r=await c.evaluate(`(()=>{let es=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent===${JSON.stringify(v)}).map(e=>e.getBoundingClientRect());let r=es.find(r=>r.width&&r.y>50&&r.bottom<900);return r?{x:r.x+r.width/2,y:r.y+r.height/2}:null})()`);if(r){await c.click(r.x,r.y);await wait(350);return;}await wheel(420);}throw Error(v);}
try{
 await wheel(550);await c.shot('catalog-images-1440');await click('Lihat detail');await wheel(500);await c.shot('detail-1440');await click('Tambahkan ke urutan sesi');await wheel(-10000);await click('Tinjau urutan');await wheel(-10000);await c.shot('review-1440');await click('Mulai persiapan');await wheel(-10000);await c.shot('preparation-1440');await wait(6500);console.log(await c.evaluate('document.body.innerText'));await click('Konfirmasi selesai / berikutnya');await wait(2500);await c.shot('active-1440');await click('Jeda');await c.shot('paused-1440');await click('Lanjutkan');await wait(1000);await click('Keluar dari sesi');console.log(await c.evaluate('document.body.innerText'));
}finally{c.ws.close();}
