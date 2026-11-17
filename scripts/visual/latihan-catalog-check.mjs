import {connect,wait} from './latihan-cdp.mjs';
const c=await connect();let width=1440;
const wheel=async dy=>{await c.send('Input.dispatchMouseEvent',{type:'mouseWheel',x:width/2,y:650,deltaX:0,deltaY:dy});await wait(300);};
async function click(v){for(let i=0;i<20;i++){let r=await c.evaluate(`(()=>{let es=[...document.querySelectorAll('*')].filter(e=>e.childElementCount===0&&e.textContent.split(String.fromCharCode(10)).join(' ').replaceAll('  ',' ').trim()===${JSON.stringify(v)}).map(e=>e.getBoundingClientRect());let r=es.find(r=>r.width&&r.y>40&&r.bottom<innerHeight-60);return r?{x:r.x+r.width/2,y:r.y+r.height/2}:null})()`);if(r){await c.click(r.x,r.y);await wait(400);return;}await wheel(350);}throw Error(v);}
try{
 await c.resize(width);await c.key('Escape','Escape',27);await wheel(-10000);if(!(await c.evaluate('document.body.innerText')).includes('CATATAN HARIAN'))await click('Catatan');await c.click(285,204);await wait(600);await click('Jelajahi katalog latihan RepDB');await wait(800);await c.shot('activity-catalog-1440');await click('Cari nama latihan (bahasa Inggris)');await c.send('Input.insertText',{text:'Clean'});await wait(500);await wheel(200);await c.shot('search-clean');let text=await c.evaluate('document.body.innerText');if(!text.includes('3 latihan tersedia'))throw Error('Search failed');
 // First filter (difficulty), choose first nonempty value and verify intersection.
 await wheel(-10000);await click('Semua');await c.key('ArrowDown','ArrowDown',40);await c.key('Enter','Enter',13);await wait(400);await c.shot('filter-clean');console.log(await c.evaluate('document.body.innerText'));
 await click('Atur ulang filter');width=390;await c.resize(width);await wheel(-10000);await c.shot('catalog-390');await wheel(500);await c.shot('catalog-images-390');
}finally{c.ws.close();}
