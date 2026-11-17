import {connect,wait} from './latihan-cdp.mjs';
const c=await connect();
async function clickText(v){let r=await c.evaluate(`(()=>{const e=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent===${JSON.stringify(v)}).map(e=>e.getBoundingClientRect()).find(r=>r.width&&r.y>=0&&r.bottom<innerHeight);return e?{x:e.x+e.width/2,y:e.y+e.height/2}:null;})()`);if(!r)throw Error('Missing: '+v);await c.click(r.x,r.y);await wait(600);}
try {
 await c.send('Page.reload');await wait(2500);await c.resize(1440);let text=await c.evaluate('document.body.innerText');
 if(!text.includes('Demo Presentasi')){
 await clickText('Login');await wait(1000);
 await clickText('misal: nadia@sehatharian.id');await c.send('Input.insertText',{text:'demo90@sehatharian.local'});
 await clickText('Masukkan kata sandi Anda');await c.send('Input.insertText',{text:'Presentasi90!'});await c.key('Enter','Enter',13);await wait(2000);
 }
 for(let i=0;i<40;i++){if((await c.evaluate('document.body.innerText')).includes('Halo, Demo'))break;await wait(500);}
 if(!(await c.evaluate('document.body.innerText')).includes('Halo, Demo'))throw Error('Login not completed');
 console.log(await c.evaluate('document.body.innerText'));
 await c.shot('home-1440');
 await clickText('Latihan · katalog RepDB');await wait(1800);console.log(await c.evaluate('document.body.innerText'));await c.shot('catalog-1440');
} finally {c.ws.close();}
