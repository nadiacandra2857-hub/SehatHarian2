import {connect} from './latihan-cdp.mjs';
const c=await connect('browser');
try{const {targetInfos}=await c.send('Target.getTargets');for(const t of targetInfos)if(t.type==='page'&&t.url.includes('8088'))await c.send('Target.closeTarget',{targetId:t.targetId});await c.send('Target.createTarget',{url:'http://localhost:8088'});}finally{c.ws.close();}
