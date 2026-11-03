/* Wordnoid 3D effects engine. Runs inside a WebView that never takes touches.
 * Kotlin calls window.FX.* with screen positions in dp (the WebView's CSS px), and the page tells Kotlin
 * about sounds and finished animations through window.WN (a JavascriptInterface).
 * Two layers: an optional full-screen backdrop scene (town, intro, wheel...) and the overlay scene for
 * effects over the game (1 world unit = 1 board cell, set by FX.unit). */
'use strict';
const $=s=>document.querySelector(s);
const canvas=$('#gl');
let renderer=null;
try{renderer=new THREE.WebGLRenderer({canvas,antialias:true,alpha:true,preserveDrawingBuffer:false})}catch(e){renderer=null}
if(renderer){renderer.setPixelRatio(Math.min(window.devicePixelRatio||1,2));renderer.shadowMap.enabled=true;renderer.shadowMap.type=THREE.PCFSoftShadowMap;
  renderer.localClippingEnabled=true;renderer.autoClear=false;renderer.setClearColor(0x000000,0)}

/* ---------- sounds and events go to Kotlin ---------- */
let gameQuiet=true;
function snd(n){try{if(window.WN)WN.sfx(n)}catch(e){}}
const sfx={coin(){snd('coin')},blast(){snd('burst')},thud(){snd('bang')},pop(){snd('pop')},whoosh(){snd('whoosh')},shine(){snd('sparkle')},
  ding(i){snd('note'+Math.min(9,3+(i||0)*2))},fanfare(){snd('fanfare')},clack(){snd('clack')},cash(){snd('cash')},crack(){snd('crack')},shatter(){snd('shatter')},snap(){snd('snap')}};
function tone(){}function noise(){}
function emit(name,data){try{if(window.WN)WN.event(name,JSON.stringify(data||{}))}catch(e){}}
function bump(){}

/* ---------- worlds ---------- */
function mkWorld(fov){const scene=new THREE.Scene(),cam=new THREE.PerspectiveCamera(fov||40,1,.1,300);return{scene,cam,tweens:[],parts:[]}}
let curW=null;
function use(w){curW=w;tweens=w.tweens;parts=w.parts;cs=w.scene;cc=w.cam}
const ov=mkWorld(40);addLights(ov.scene,{hemi:.9,sun:.8,pos:[3,6,9]});const OV_KEEP=ov.scene.children.length;
let bd=null;            // backdrop: {world, name, api}
let U=56;               // dp per world unit in the overlay
const A={coins:[60,24],moves:[60,60],goal:[60,100],bag:[0,0]}; // anchors (dp)
function fitOv(){const c=ov.cam;c.aspect=W/H;c.fov=40;c.position.set(0,0,(H/U/2)/Math.tan(20*Math.PI/180));c.lookAt(0,0,0);c.updateProjectionMatrix();c.updateMatrixWorld(true)}
/** Screen point (dp) to the overlay world. With z, the point at that depth that shows at the same spot. */
function P(x,y,z){if(!z)return v3((x-W/2)/U,(H/2-y)/U,0);ov.cam.updateMatrixWorld(true);return screenToWorld(ov.cam,x,y,z)}
function anchor(a,z){const p=Array.isArray(a)?a:(A[a]||A.coins);return P(p[0],p[1],z||2.4)}

function resize(){W=Math.max(1,window.innerWidth);H=Math.max(1,window.innerHeight);if(renderer)renderer.setSize(W,H,false);fitOv();
  if(bd&&bd.api.onResize){use(bd.world);bd.world.cam.aspect=W/H;bd.world.cam.updateProjectionMatrix();bd.api.onResize()}}
window.addEventListener('resize',resize);

/* ---------- main loop (stops when nothing is showing) ---------- */
let running=false,last=0;
function kick(){if(!running&&renderer){running=true;last=performance.now();requestAnimationFrame(loop)}}
function ovBusy(){return ov.tweens.length||ov.parts.length||ov.scene.children.length>OV_KEEP||boss}
function loop(now){const dt=Math.min(.05,Math.max(0,(now-last)/1000));last=now;T+=dt;
  try{
    renderer.clear();
    if(bd){use(bd.world);runTweens();updParts(dt);const c=bd.world.cam;bd.api.update&&bd.api.update(dt,T);
      if(shake>0){c.position.x+=rnd(shake);c.position.y+=rnd(shake);shake*=Math.pow(.02,dt);if(shake<.005)shake=0}
      renderer.render(bd.world.scene,c);renderer.clearDepth()}
    use(ov);runTweens();updParts(dt);ovUpdate(dt,T);renderer.render(ov.scene,ov.cam);
  }catch(e){console.log('fx loop',e&&e.stack)}
  if(bd||ovBusy())requestAnimationFrame(loop);else{renderer.clear();running=false}}

/* ---------- helpers for the overlay ---------- */
function add(o){ov.scene.add(o);return o}
function rm(o){if(o&&o.parent)o.parent.remove(o)}
function flyTo(obj,to,dur,o={}){let st=null;return tween(dur,(e,k)=>{if(!st)st=obj.position.clone();const t=anchor(to,o.z||2.4);const c=st.clone().lerp(t,.45).add(v3(0,o.up==null?1.4:o.up,1));
  const a=st.clone().lerp(c,e),b=c.clone().lerp(t,e);obj.position.copy(a.lerp(b,e));if(o.shrink!==false)obj.scale.setScalar((o.s0||1)*(1-.6*e));o.each&&o.each(e,k)},
  {delay:o.delay||0,ease:o.ease||E.inCubic,done:()=>{rm(obj);o.done&&o.done()}})}
function coinBurstTo(wp,n,each,to){for(let i=0;i<n;i++){const c=coinMesh();c.position.copy(wp).add(v3(rnd(.6),rnd(.6),.8));c.scale.setScalar(.01);cs.add(c);const sp=c.userData.spin;
  tween(.25,e=>{c.scale.setScalar(Math.max(.01,e));sp.rotation.y+=.3},{delay:i*.07,ease:E.outBack});
  flyTo(c,to||'coins',.6,{delay:.3+i*.07,each:()=>sp.rotation.y+=.4,done:()=>{sfx.coin();each&&each()}})}}
function setCoins(){}
function popupTo(text,from,to,cb,o={}){const m=textMesh(text,o);m.position.copy(from);m.scale.setScalar(.01);cs.add(m);
  tween(.35,e=>m.scale.setScalar(Math.max(.01,e*(o.s||1.2))),{ease:E.outBack});if(!to){wait(o.hold||1.1,()=>tween(.3,e=>m.material.opacity=1-e,{done:()=>rm(m)}));return m}
  wait(o.hold||.7,()=>flyTo(m,to,.55,{s0:o.s||1.2,done:()=>{cb&&cb()}}));return m}
function bigMsg(text,hold=1.1){const m=$('#xMsg');m.textContent=text;m.className='big-msg';void m.offsetWidth;m.classList.add('show');wait(hold,()=>m.classList.add('out'))}
const BRICK_TXT={colors:['#fff1e0','#ff9a6a','#c2401c'],stroke:'#4a1200'};
function hexOf(c){return typeof c==='string'?c:'#'+('000000'+(c>>>0).toString(16)).slice(-6)}
/** A copy of a board tile at a screen spot. */
function tileAt(q,s){const t=makeTile(q.ch||' ',q.c||'#2a8fe0');t.position.copy(P(q.x,q.y));t.scale.setScalar(s||.92);return add(t)}

/* ---------- words ---------- */
function word(o){const n=o.cells.length,s=.92;const tiles=o.cells.map(q=>tileAt(q,s));const c=P(o.cx,o.cy);
  tiles.forEach((t,i)=>{const m=t.userData.mat;tween(.24,e=>{t.position.z=e*.8;t.scale.setScalar(s*(1+.2*e));m.emissive.setRGB(.3*e,.28*e,.12*e)},{delay:i*.05,ease:E.outBack});
    tween(.42,e=>t.rotation.y=e*Math.PI*2,{delay:.14+i*.05,ease:E.inOut})});
  const gap=Math.min(1.45,(W/U*.62)/Math.max(3,n));
  wait(.5,()=>{tiles.forEach((t,i)=>{const p0=t.position.clone(),tg=v3(c.x+(i-(n-1)/2)*gap,c.y+.35,3);tween(.32,e=>{t.position.lerpVectors(p0,tg,e);t.scale.setScalar(s*(1.2+.25*e)*Math.min(1,gap/1.2))},{ease:E.outBack})});
    const tx=textMesh(o.text||'SWEET!',{h:.95,...(o.green?GREEN_TXT:{})});tx.position.set(c.x,c.y+1.75,3.1);tx.scale.setScalar(.01);add(tx);tween(.35,e=>tx.scale.setScalar(Math.max(.01,e)),{delay:.12,ease:E.outBack});
    wait(.85,()=>{rm(tx);tiles.forEach((t,i)=>wait(i*.05,()=>{const wp=wpos(t);rm(t);chunks(wp,t.userData.mat.color,10,1.05);flash(wp,2.4);starsAt(wp,4)}))})})}
function popup(o){popupTo(o.text,P(o.x,o.y,2.5),o.to||null,()=>emit('popupDone',{to:o.to}),o.green?{...GREEN_TXT,h:o.h||.8}:o.brick?{...BRICK_TXT,h:o.h||.8}:{h:o.h||.8})}
function coinsFx(o){coinBurstTo(P(o.x,o.y,.8),o.n||3,null,o.to)}
function burst(o){const wp=P(o.x,o.y,.2);chunks(wp,new THREE.Color(o.c||'#2a8fe0'),o.n||10,o.pow||1);flash(wp,2.4);if(o.stars)starsAt(wp,o.stars)}
/** Goal reached: leftover tiles burst one by one, each turning into a coin that flies to the coin box. */
function goalBlast(o){const list=o.cells.slice().sort((a,b)=>Math.hypot(a.x-o.cx,a.y-o.cy)-Math.hypot(b.x-o.cx,b.y-o.cy));const step=Math.min(.085,1.6/Math.max(1,list.length));
  for(let i=0;i<60;i++){const s=sprite(starTex,[0xffe27a,0xff9ec7,0x7fe3ff][i%3],.4);s.position.copy(P(o.cx,o.cy,1)).add(v3(rnd(8),rnd(5),0));addPart(s,ov.scene,{v:v3(rnd(8),Math.random()*7,rnd(3)),g:6,life:1.6,fade:true})}
  list.forEach((q,i)=>wait(.2+i*step,()=>{const t=tileAt(q);const m=t.userData.mat;
    tween(.14,(e,k)=>{t.position.z=e*.6;t.scale.setScalar(.92*(1+e*.2));m.emissive.setRGB(e*.55,e*.55,e*.5)},{done:()=>{const wp=t.position.clone();rm(t);
      chunks(wp,m.color,8,1);flash(wp,2.2);if(i%2===0)sfx.blast();
      const c=coinMesh();c.position.copy(wp).setZ(.7);c.scale.setScalar(.01);add(c);const sp=c.userData.spin;
      tween(.22,e=>{c.scale.setScalar(Math.max(.01,e*1.1));sp.rotation.y+=.35},{ease:E.outBack});
      flyTo(c,'coins',.6,{delay:.25,each:()=>sp.rotation.y+=.45,done:()=>{sfx.coin();emit('coin')}})}})}));
  wait(.4+list.length*step+.9,()=>emit('goalBlastDone'))}

/* ---------- blockers ---------- */
const crackTex=ctex(128,128,x=>{x.strokeStyle='#ffffff';x.lineWidth=4;x.lineCap='round';for(let i=0;i<7;i++){x.beginPath();x.moveTo(64,64);let px=64,py=64;const a=i*.9+.3;for(let k=0;k<4;k++){px+=Math.cos(a+rnd(.8))*16;py+=Math.sin(a+rnd(.8))*16;x.lineTo(px,py)}x.stroke()}});
const heartTex=ctex(64,64,x=>{x.fillStyle='#ff5c8a';x.beginPath();x.moveTo(32,56);x.bezierCurveTo(4,36,6,10,22,10);x.bezierCurveTo(30,10,32,18,32,20);x.bezierCurveTo(32,18,34,10,42,10);x.bezierCurveTo(58,10,60,36,32,56);x.fill()});
function crateBreak(o){const cr=crate();const wp=P(o.x,o.y,.1);cr.position.copy(wp);cr.scale.setScalar(.92);add(cr);
  tween(.3,(e,k)=>{cr.rotation.z=Math.sin(k*40)*.09;cr.scale.setScalar(.92+k*.08)},{ease:E.lin,done:()=>{rm(cr);
    for(let i=0;i<9;i++){const p=new THREE.Mesh(geo.plank||(geo.plank=new THREE.BoxGeometry(.9,.16,.08)),cr.material);p.position.copy(wp).add(v3(rnd(.6),rnd(.6),.3));p.rotation.z=Math.random()*3;
      addPart(p,ov.scene,{v:v3(rnd(8),2+Math.random()*5,2+Math.random()*3),av:v3(rnd(10),rnd(10),rnd(10)),g:12,life:1.3,shrink:true})}
    flash(wp,3.2);if(o.prize===false)return;const c=coinMesh();c.position.copy(wp).setZ(.8);c.scale.setScalar(.01);add(c);const gl=sprite(glowTex,0xffe080,2.4);gl.position.copy(wp).setZ(.6);add(gl);
    tween(.45,e=>{c.scale.setScalar(Math.max(.01,e*1.6));c.position.z=.8+e*1.2;c.userData.spin.rotation.y+=.3;gl.material.opacity=e},{ease:E.outBack});
    if(o.text)popupTo(o.text,wp.clone().add(v3(0,1.1,1.5)),null,null,{h:.7,hold:.8});
    wait(.75,()=>{rm(gl);flyTo(c,o.to||'coins',.6,{s0:1.6,each:()=>c.userData.spin.rotation.y+=.4,done:()=>sfx.coin()})})}})}
function iceCrack(o){const cr=new THREE.Mesh(new THREE.PlaneGeometry(1,1),new THREE.MeshBasicMaterial({map:crackTex,transparent:true,opacity:0,depthWrite:false}));cr.position.copy(P(o.x,o.y)).setZ(.42);add(cr);
  tween(.15,e=>cr.material.opacity=e);starsAt(cr.position,5,0xffffff,.6);wait(.9,()=>tween(.3,e=>cr.material.opacity=1-e,{done:()=>rm(cr)}))}
function iceBreak(o){const wp=P(o.x,o.y,.1);const mat=new THREE.MeshStandardMaterial({color:'#bdf0ff',transparent:true,opacity:.6,roughness:.05,metalness:.2,emissive:'#4aa8d0',emissiveIntensity:.5,depthWrite:false});
  for(let i=0;i<18;i++){const s=new THREE.Mesh(geo.shard||(geo.shard=new THREE.TetrahedronGeometry(.16)),mat.clone());s.position.copy(wp).add(v3(rnd(.9),rnd(.9),.3));
    addPart(s,ov.scene,{v:v3(rnd(9),rnd(8)+2,1.5+Math.random()*3),av:v3(rnd(12),rnd(12),rnd(12)),g:11,life:1.1,fade:true,op:.6})}
  starsAt(wp,8,0xffffff);flash(wp,3,0xbff4ff)}
function chainBreak(o){const wp=P(o.x,o.y,.33),g=new THREE.Group(),m=new THREE.MeshStandardMaterial({color:'#b8c0d0',metalness:.6,roughness:.35,emissive:'#222222'});
  for(const d of[1,-1])for(let i=-3;i<=3;i++){const l=new THREE.Mesh(geo.link||(geo.link=new THREE.TorusGeometry(.085,.03,8,16)),m);l.position.set(i*.13,d*i*.13,0);l.rotation.z=d*Math.PI/4;if(i%2)l.rotation.x=Math.PI/2;g.add(l)}
  g.position.copy(wp);add(g);tween(.3,(e,k)=>{m.emissive.setRGB(.9*e,.45*e,.1*e);g.rotation.z=Math.sin(k*45)*.05},{ease:E.lin,done:()=>{
    g.children.slice().forEach(l=>{const p=wpos(l);const q=l.getWorldQuaternion(new THREE.Quaternion());g.remove(l);l.position.copy(p);l.quaternion.copy(q);
      addPart(l,ov.scene,{v:v3(rnd(8),rnd(6)+2,2+Math.random()*2),av:v3(rnd(16),rnd(16),rnd(16)),g:11,life:1.2,shrink:true})});rm(g);flash(wp,2.5,0xffd8a0)}})}
function goldBurst(o){const wp=P(o.x,o.y,.3);const sh=new THREE.Mesh(new THREE.PlaneGeometry(.25,1.4),new THREE.MeshBasicMaterial({color:'#ffffff',transparent:true,opacity:.8,blending:THREE.AdditiveBlending,depthWrite:false}));
  sh.rotation.z=.6;sh.position.copy(wp);add(sh);tween(.35,e=>sh.position.x=wp.x-.6+1.2*e,{ease:E.inOut,done:()=>rm(sh)});
  wait(.4,()=>{chunks(wp,'#ffcf3a',16,1.2,{metal:.6,rough:.2,emissive:'#5a3d00'});starsAt(wp,12,0xffd84a,1.2);flash(wp,3.5,0xffe080);coinBurstTo(wp,o.n||2)})}
function balloonPop(o){const wp=P(o.x,o.y-(o.cell||U)*.5,.75),g=new THREE.Group();const bm=new THREE.Mesh(new THREE.SphereGeometry(.42,24,18),new THREE.MeshStandardMaterial({color:'#ff4d6d',roughness:.18,metalness:.05}));bm.scale.y=1.18;g.add(bm);
  g.position.copy(wp);add(g);tween(.2,(e,k)=>{g.scale.setScalar(1+.35*e);g.rotation.z=Math.sin(k*40)*.08},{done:()=>{rm(g);
    for(let i=0;i<8;i++){const p=new THREE.Mesh(new THREE.PlaneGeometry(.22,.1),new THREE.MeshBasicMaterial({color:'#ff4d6d',side:THREE.DoubleSide,transparent:true}));p.position.copy(wp);
      addPart(p,ov.scene,{v:v3(rnd(10),rnd(10),rnd(4)+2),av:v3(rnd(20),rnd(20),rnd(20)),g:8,life:1,fade:true})}
    confettiAt(wp,40,1);const tx=textMesh('POP!',{h:.9,colors:['#ffe0ea','#ff6fa8','#c2185b'],stroke:'#5a0a2a'});tx.position.copy(wp).setZ(1.6);add(tx);
    tween(.6,e=>{tx.scale.setScalar(.4+e*1.1);tx.material.opacity=1-e*e},{done:()=>rm(tx)})}})}
function chestOpen(o){openChest(P(o.x,o.y,.4),o.n||3);popupTo('TREASURE!',P(o.x,o.y,1.6).add(v3(0,1.6,0)),null,null,{h:.8})}
function friendHop(o){const f=makeTiko();recolor(f,0x3fa9ff,o.c||'#ff7eb6');f.scale.setScalar(.48);f.position.copy(P(o.x,o.y,.25)).add(v3(0,-.48,0));add(f);
  const p0=f.position.clone();tween(.25,(e,k)=>f.rotation.z=Math.sin(k*Math.PI*4)*.3,{ease:E.lin});
  wait(.3,()=>{tween(.9,(e,k)=>{f.position.set(p0.x+e*1.5,p0.y+Math.sin(k*Math.PI)*1.6-e*2.5,p0.z+e*3);f.rotation.y=k*Math.PI*2},{ease:E.lin,done:()=>rm(f)});
    for(let i=0;i<10;i++){const h=sprite(heartTex,0xffffff,.4,false);h.position.copy(p0);addPart(h,ov.scene,{v:v3(rnd(4),2+Math.random()*3,1.5),life:1.4,fade:true})}
    popupTo('FRIEND SAVED!',p0.clone().add(v3(0,1.4,2)),null,null,{...GREEN_TXT,h:.7})})}

/* ---------- 3D tools (boosters), also drawn as icons for the booster buttons ---------- */
function mkWand(){const w=new THREE.Group();const st=new THREE.Mesh(new THREE.CylinderGeometry(.04,.05,1.3,10),std('#222233',.4));w.add(st);const tip=new THREE.Mesh(new THREE.CylinderGeometry(.045,.045,.18,10),std('#ffffff'));tip.position.y=.6;w.add(tip);
  const s=starMesh(.32,.14);s.position.y=.85;w.add(s);w.userData.star=s;return rich(w)}
function mkBulb(){const g=new THREE.Group();const bm=new THREE.MeshStandardMaterial({color:'#fff6c0',emissive:'#ffd84a',emissiveIntensity:.25,roughness:.1,transparent:true,opacity:.95});
  const bulb=new THREE.Mesh(new THREE.SphereGeometry(.45,24,18),bm);g.add(bulb);const bs=new THREE.Mesh(new THREE.CylinderGeometry(.22,.24,.4,16),std('#9aa3b5',.3,.5));bs.position.y=-.5;g.add(bs);g.userData.bm=bm;return rich(g)}
function mkUndo(){const g=new THREE.Group();const arc=new THREE.Mesh(new THREE.TorusGeometry(.5,.13,12,32,Math.PI*1.5),std('#ffb020',.3));g.add(arc);const cone=new THREE.Mesh(new THREE.ConeGeometry(.26,.42,16),arc.material);cone.position.set(0,-.5,0);cone.rotation.z=Math.PI/2;g.add(cone);return rich(g)}
function mkShuffle(){const g=new THREE.Group();for(const s of[0,1]){const a=new THREE.Mesh(new THREE.TorusGeometry(.42,.1,10,28,Math.PI*.8),std(s?'#7fe3ff':'#ff6fa8',.3));a.rotation.z=s*Math.PI;g.add(a);
  const c=new THREE.Mesh(new THREE.ConeGeometry(.2,.34,14),a.material);const ang=s*Math.PI+Math.PI*.8;c.position.set(Math.cos(ang)*.42,Math.sin(ang)*.42,0);c.rotation.z=ang;g.add(c)}return rich(g)}
function mkPlus3(){return textMesh('+3',{h:1,...GREEN_TXT})}
function mkBrush(){const g=new THREE.Group();const h=new THREE.Mesh(new THREE.CylinderGeometry(.06,.07,1.1,12),std('#a0622d',.6));h.position.y=.75;g.add(h);const fe=new THREE.Mesh(new THREE.CylinderGeometry(.11,.11,.22,12),std('#c9d2e0',.25,.6));fe.position.y=.15;g.add(fe);
  const br=new THREE.Mesh(new THREE.ConeGeometry(.16,.42,12),std('#ff6fa8',.6));br.rotation.x=Math.PI;br.position.y=-.15;g.add(br);g.userData.tip=br;return rich(g)}
function mkHourglass(){const g=new THREE.Group(),glass=new THREE.MeshStandardMaterial({color:'#cfefff',transparent:true,opacity:.5,roughness:.05});
  for(const s of[1,-1]){const c=new THREE.Mesh(new THREE.ConeGeometry(.4,.6,24,1,true),glass);c.position.y=s*.3;if(s>0)c.rotation.x=Math.PI;g.add(c);const cap=new THREE.Mesh(new THREE.CylinderGeometry(.5,.5,.1,24),std('#a0622d',.6));cap.position.y=s*.65;g.add(cap)}
  const sand=new THREE.Mesh(new THREE.ConeGeometry(.3,.3,20),std('#ffd84a',.6));sand.position.y=-.45;g.add(sand);return rich(g)}
function mkArrows(vert){const g=new THREE.Group();const m=std('#ff9a3c',.4);const bar=new THREE.Mesh(new THREE.CylinderGeometry(.09,.09,1.1,12),m);bar.rotation.z=Math.PI/2;g.add(bar);
  for(const s of[-1,1]){const a=new THREE.Mesh(new THREE.ConeGeometry(.25,.4,16),m);a.rotation.z=-s*Math.PI/2;a.position.x=s*.7;g.add(a)}if(vert)g.rotation.z=Math.PI/2;return rich(g)}
function mkSpinRing(){const g=new THREE.Group();const r=new THREE.Mesh(new THREE.TorusGeometry(.5,.1,12,36,Math.PI*1.6),std('#9be7ff',.3));g.add(r);const c=new THREE.Mesh(new THREE.ConeGeometry(.22,.36,14),r.material);c.position.set(.5,0,0);g.add(c);return rich(g)}
let mapT=null;function mapTex(){return mapT||(mapT=ctex(256,170,(x,w,h)=>{x.fillStyle='#f3dfb0';x.fillRect(0,0,w,h);x.strokeStyle='#b88a4a';x.lineWidth=8;x.strokeRect(4,4,w-8,h-8);x.setLineDash([10,9]);x.strokeStyle='#7a4a1e';x.lineWidth=5;
  x.beginPath();x.moveTo(30,140);x.bezierCurveTo(80,40,140,150,200,60);x.stroke();x.setLineDash([]);x.strokeStyle='#e5333d';x.lineWidth=10;x.beginPath();x.moveTo(186,46);x.lineTo(214,74);x.moveTo(214,46);x.lineTo(186,74);x.stroke()}))}
function mkMap(){const m=new THREE.Mesh(new THREE.BoxGeometry(1.5,1,.04),[std('#f3dfb0'),std('#f3dfb0'),std('#f3dfb0'),std('#f3dfb0'),new THREE.MeshStandardMaterial({map:mapTex(),roughness:.6}),std('#d8c08a')]);m.rotation.x=-.25;return m}
function mkVowel(){const t=makeTile('A','#e8457a');return t}
function mkScanner(){const g=new THREE.Group();const ring=new THREE.Mesh(new THREE.TorusGeometry(.42,.08,12,32),std('#ffc93c',.25,.5));g.add(ring);const gl=new THREE.Mesh(new THREE.CircleGeometry(.4,32),new THREE.MeshStandardMaterial({color:'#cfefff',transparent:true,opacity:.45,roughness:.05}));g.add(gl);
  const hd=new THREE.Mesh(new THREE.CylinderGeometry(.08,.09,.7,12),std('#7a4a2a',.5));hd.position.set(.5,-.5,0);hd.rotation.z=Math.PI/4;g.add(hd);return rich(g)}
function mkLock(){const g=new THREE.Group();const body=new THREE.Mesh(tileGeo(.62,.5,.2,.1,.04),std('#ffc93c',.25,.5));g.add(body);const sh=new THREE.Mesh(new THREE.TorusGeometry(.2,.06,10,20,Math.PI),std('#d9e0ea',.25,.6));sh.position.y=.25;g.add(sh);
  const kh=new THREE.Mesh(new THREE.CylinderGeometry(.06,.06,.05,12),std('#3a2a00'));kh.rotation.x=Math.PI/2;kh.position.z=.13;g.add(kh);g.userData.shackle=sh;return rich(g)}
function mkDouble(){const g=new THREE.Group();const s=starMesh(.6,.27);g.add(s);const t=textMesh('x2',{h:.5});t.position.z=.2;g.add(t);return g}
function mkCloud(){const cl=new THREE.Group();for(let i=0;i<5;i++){const m=new THREE.Mesh(new THREE.SphereGeometry(.32+((i*37)%10)/40,16,12),std(i%2?'#ffffff':'#e8eefc',.9));m.position.set(i*.32-.64,((i*53)%7)/40,0);cl.add(m)}
  for(let i=0;i<3;i++){const d=new THREE.Mesh(new THREE.SphereGeometry(.07,8,8),std('#3fa9ff',.3));d.position.set(i*.35-.35,-.45-(i%2)*.15,0);d.scale.y=1.5;cl.add(d)}return cl}
function mkJoker(){return mkCard(cardTex('🃏','JOKER',true))}
function mkTreasure(){const c=chest();return c}
function mkTiko(pink){const t=makeTiko();if(pink)recolor(t,0x3fa9ff,'#ff7eb6');return t}
const TOOLS={HAMMER:mkHammer,AXE:mkAxe,MAGNET:mkMagnet,DICE:mkDice,JOKER:mkJoker,MAGIC_SWAP:mkWand,HINT:mkBulb,UNDO:mkUndo,SHUFFLE:mkShuffle,PLUS_MOVES:mkPlus3,PAINT:mkBrush,
  FREE_MOVE:mkHourglass,ROW_SLIDE:()=>mkArrows(false),COLUMN_SLIDE:()=>mkArrows(true),SPIN:mkSpinRing,TREASURE_FINDER:mkMap,VOWEL_FLIP:mkVowel,SCANNER:mkScanner,LOCK:mkLock,DOUBLE:mkDouble,
  LETTER_RAIN:mkCloud,TIKO:()=>mkTiko(false),FRIEND:()=>mkTiko(true),CHEST:mkTreasure,COIN:()=>coinMesh(),BRICK:()=>{const m=new THREE.Mesh(tileGeo(1.2,.6,.45,.1,.05),std('#e0603c',.5));return m}};
/** How each tool sits in its icon. */
const ICON_POSE={HAMMER:{rz:-.6,s:.62,y:-.45},AXE:{rz:-.5,s:.75},MAGNET:{s:.95},DICE:{rx:.5,ry:.7,s:1.05},JOKER:{ry:-.3,s:.62},MAGIC_SWAP:{rz:-.5,s:.75,y:-.15},HINT:{s:.95,y:.1},UNDO:{s:1.0},
  SHUFFLE:{s:1.0},PLUS_MOVES:{s:.9},PAINT:{rz:-.6,s:.7,y:-.2},FREE_MOVE:{rz:.3,s:.9},ROW_SLIDE:{s:.85},COLUMN_SLIDE:{s:.85},SPIN:{s:1.0},TREASURE_FINDER:{s:.9},VOWEL_FLIP:{s:.8,ry:-.3},
  SCANNER:{s:.95},LOCK:{s:1.3},DOUBLE:{s:1.0},LETTER_RAIN:{s:.9},TIKO:{s:.6,y:-.55,ry:-.3},FRIEND:{s:.6,y:-.55,ry:-.3},CHEST:{s:1.1,rx:.3,ry:-.4},COIN:{s:1.4,rx:.2,ry:-.4},BRICK:{s:.8,rx:.3,ry:-.4}};
function toolMesh(k){const f=TOOLS[k]||mkHammer;return f()}

/** Renders every tool to a small picture and sends it to Kotlin (the booster buttons use them). */
function icons(o){if(!renderer)return;const keys=(o&&o.keys)||Object.keys(TOOLS);const size=(o&&o.size)||128;
  const rt=new THREE.WebGLRenderTarget(size,size,{format:THREE.RGBAFormat});const sc=new THREE.Scene();addLights(sc,{hemi:1,sun:.9,pos:[3,5,8]});
  const cam=new THREE.PerspectiveCamera(30,1,.1,50);cam.position.set(0,0,4.6);cam.lookAt(0,0,0);const buf=new Uint8Array(size*size*4);
  const c2=document.createElement('canvas');c2.width=size;c2.height=size;const x2=c2.getContext('2d');const img=x2.createImageData(size,size);
  for(const k of keys){const m=toolMesh(k);const p=ICON_POSE[k]||{};m.scale.multiplyScalar(p.s||1);m.rotation.set(p.rx||0,p.ry||0,p.rz||0);m.position.y=p.y||0;
    const box=new THREE.Box3().setFromObject(m);const cen=box.getCenter(v3());m.position.sub(v3(cen.x,cen.y-(p.y||0)*0,0)).add(v3(0,0,0));
    const piv=new THREE.Group();piv.add(m);sc.add(piv);
    renderer.setRenderTarget(rt);renderer.setClearColor(0x000000,0);renderer.clear();renderer.render(sc,cam);renderer.readRenderTargetPixels(rt,0,0,size,size,buf);renderer.setRenderTarget(null);
    for(let y=0;y<size;y++){const src=(size-1-y)*size*4;img.data.set(buf.subarray(src,src+size*4),y*size*4)}
    x2.putImageData(img,0,0);sc.remove(piv);try{WN.icon(k,c2.toDataURL('image/png'))}catch(e){}}
  rt.dispose();renderer.setClearColor(0x000000,0)}

function popIn(o,p,s=1.5){o.position.copy(p).add(v3(1.5,1.5,1.5));o.scale.setScalar(.01);add(o);return tween(.4,e=>{o.position.lerpVectors(p.clone().add(v3(1.5,1.5,1.5)),p,e);o.scale.setScalar(Math.max(.01,e*s))},{ease:E.outBack})}
function popOut(o,d=0){wait(d,()=>tween(.3,e=>{o.scale.multiplyScalar(.85);o.position.y+=.08},{done:()=>rm(o)}))}
/** Booster animations. The board itself (Compose) changes the tiles; this draws the tool and the sparkle. */
function booster(o){const k=o.k,tp=o.x!=null?P(o.x,o.y,.6):P(o.bx+o.bw/2,o.by+o.bh/2,.6);const top=P(o.bx+o.bw/2,o.by,1.2).add(v3(0,.3,0));const mid=P(o.bx+o.bw/2,o.by+o.bh/2,2.2);
  const cellsW=(o.cells||[]).map(q=>P(q.x,q.y,.5));
  switch(k){
  case'HAMMER':{const h=mkHammer();const piv=tp.clone().add(v3(1.3,-.2,0));popIn(h,piv,1.1);h.rotation.z=-.5;wait(.32,()=>tween(.22,e=>h.rotation.z=-.5+(Math.PI/2+.5)*e,{ease:E.inCubic,done:()=>{starsAt(tp,8);flash(tp,2.6);
    tween(.2,e=>h.rotation.z=Math.PI/2-.4*e,{ease:E.outBack});popOut(h,.35)}}));break}
  case'AXE':{const a=mkAxe();a.position.copy(tp).add(v3(-1.6,.6,0));a.scale.setScalar(1.3);add(a);tween(.38,(e,k2)=>{a.position.lerpVectors(tp.clone().add(v3(-1.6,.6,0)),tp.clone().add(v3(.2,0,0)),e);a.rotation.z=-k2*Math.PI*3},{ease:E.inCubic,done:()=>{flash(tp,3);starsAt(tp,8);popOut(a,.2)}});break}
  case'MAGNET':{const m=mkMagnet();const p=tp.clone().add(v3(0,1.8,.8));popIn(m,p,1.3);const l=new THREE.Line(new THREE.BufferGeometry(),new THREE.LineBasicMaterial({color:'#7fe3ff'}));add(l);
    tween(.5,(e,k2)=>{m.rotation.z=Math.sin(k2*60)*.05;const a=m.position.clone().add(v3(0,-.4,0));const pts=[];for(let s=0;s<=8;s++){const q=a.clone().lerp(tp,s/8);if(s&&s<8)q.add(v3(rnd(.35),rnd(.35),rnd(.2)));pts.push(q)}l.geometry.setFromPoints(pts)},{delay:.3,ease:E.lin,done:()=>{rm(l);coinBurstTo(tp,3);starsAt(tp,10,0xffd84a);popOut(m,.4)}});break}
  case'DICE':{const d=mkDice();const s=tp.clone().add(v3(3,-2.5,2));d.position.copy(s);d.scale.setScalar(1.1);add(d);tween(.75,(e,k2)=>{d.position.lerpVectors(s,tp.clone().setZ(1),e);d.position.z+=Math.abs(Math.sin(k2*Math.PI*3))*(1-k2)*1.5;d.rotation.set(k2*12,k2*9,k2*4)},{ease:E.outCubic,done:()=>{d.rotation.set(0,0,0);starsAt(tp,6);tween(.3,e=>d.scale.setScalar(Math.max(.01,1.1*(1-e))),{delay:.35,done:()=>rm(d)})}});break}
  case'JOKER':{const c=mkJoker();const st=mid.clone().setZ(4);c.position.copy(st);add(c);tween(.6,(e,k2)=>{c.position.lerpVectors(st,tp.clone().setZ(.8),e);c.rotation.y=(1-e)*Math.PI*4;c.scale.setScalar(1.2-.5*e)},{ease:E.outCubic,done:()=>{flash(tp,3);
    tween(.25,e=>c.scale.setScalar(Math.max(.01,.7*(1-e))),{delay:.15,done:()=>rm(c)});starsAt(tp,14,0xff9ec7);starsAt(tp,8,0x7fe3ff)}});break}
  case'MAGIC_SWAP':{const w=mkWand();popIn(w,top.clone().add(v3(0,-.5,.4)),1.2);tween(.6,(e,k2)=>{w.rotation.z=Math.sin(k2*Math.PI*3)*.5;const sp=sprite(starTex,0x9be7ff,.25);sp.position.copy(wpos(w.userData.star));addPart(sp,ov.scene,{v:v3(rnd(1),-1,0),life:.6,fade:true})},{delay:.3,ease:E.lin});
    wait(.9,()=>{cellsW.forEach(p=>{starsAt(p,8,0x9be7ff);flash(p,2.2,0x9be7ff)});popOut(w,.2)});break}
  case'HINT':{const g=mkBulb();popIn(g,top.clone().add(v3(0,-.4,.4)),1.1);const gl=sprite(glowTex,0xffe27a,.01);g.add(gl);
    wait(.4,()=>{tween(.6,(e,k2)=>{const f=k2<.6?(Math.sin(k2*60)>0?1:.2):1;g.userData.bm.emissiveIntensity=1.3*e*f;gl.scale.setScalar(3*e*f)},{ease:E.lin});wait(.7,()=>{cellsW.forEach(p=>flash(p,2.2,0xffe27a));popOut(g,.9)})});break}
  case'UNDO':{const g=mkUndo();popIn(g,mid,1.7);tween(.8,e=>g.rotation.z=e*Math.PI*2,{delay:.25,ease:E.inOut,done:()=>popOut(g,.1)});break}
  case'SHUFFLE':{const r=new THREE.Mesh(new THREE.TorusGeometry(Math.min(o.bw,o.bh)/U*.42,.08,10,64),new THREE.MeshBasicMaterial({color:'#9be7ff',transparent:true,depthWrite:false}));r.position.copy(P(o.bx+o.bw/2,o.by+o.bh/2,.6));add(r);
    const s=mkShuffle();popIn(s,mid,1.5);tween(1.1,(e,k2)=>{r.rotation.z=-k2*8;r.material.opacity=Math.sin(k2*Math.PI);s.rotation.z=-k2*10;r.scale.setScalar(.6+.4*Math.sin(k2*Math.PI))},{ease:E.inOut,done:()=>{rm(r);popOut(s)}});break}
  case'PLUS_MOVES':{const m=mkPlus3();m.position.copy(mid);m.scale.setScalar(.01);add(m);tween(.5,(e,k2)=>{m.scale.setScalar(Math.max(.01,e*1.6));m.rotation.y=(1-k2)*Math.PI*2},{ease:E.outBack});confettiAt(mid,30,2);wait(.8,()=>flyTo(m,'moves',.55,{s0:1.6}));break}
  case'PAINT':{const g=mkBrush();popIn(g,tp.clone().add(v3(-.5,.3,.5)),1.1);g.rotation.z=-.5;wait(.35,()=>tween(.7,(e,k2)=>{g.position.x=tp.x+Math.sin(k2*Math.PI*3)*.5;if(Math.random()<.4){const sp=new THREE.Mesh(new THREE.CircleGeometry(.07,10),new THREE.MeshBasicMaterial({color:'#ff6fa8'}));sp.position.copy(wpos(g.userData.tip));addPart(sp,ov.scene,{v:v3(rnd(4),rnd(3),2),g:6,life:.7})}},
    {ease:E.lin,done:()=>{starsAt(tp,8,0xff9ec7);popOut(g)}}));break}
  case'FREE_MOVE':{const g=mkHourglass();popIn(g,mid,1.6);wait(.45,()=>tween(.6,e=>g.rotation.z=e*Math.PI,{ease:E.outBack,done:()=>{popupTo('FREE MOVE!',mid.clone().add(v3(0,-1.2,.2)),'moves',null,GREEN_TXT);popOut(g,.5)}}));break}
  case'ROW_SLIDE':case'COLUMN_SLIDE':{const v=k==='COLUMN_SLIDE';const g=mkArrows(v);popIn(g,tp.clone().setZ(1.2),1.3);tween(.8,(e,k2)=>{const s=1+Math.sin(k2*Math.PI*4)*.2;g.scale.setScalar(1.3*s)},{delay:.3,ease:E.lin,done:()=>popOut(g)});
    cellsW.forEach((p,i)=>wait(.3+i*.05,()=>starsAt(p,3,0xffb060,.6)));break}
  case'SPIN':{const c=cellsW.length?cellsW.reduce((a,b)=>a.add(b),v3()).multiplyScalar(1/cellsW.length):tp;const ring=new THREE.Mesh(new THREE.TorusGeometry(1.15,.06,10,48),new THREE.MeshBasicMaterial({color:'#9be7ff',transparent:true}));ring.position.copy(c).setZ(.7);add(ring);
    tween(.8,(e,k2)=>{ring.rotation.z=-k2*4;ring.material.opacity=Math.sin(k2*Math.PI)},{ease:E.inOut,done:()=>rm(ring)});break}
  case'TREASURE_FINDER':{const m=new THREE.Mesh(new THREE.PlaneGeometry(2.6,1.7),new THREE.MeshBasicMaterial({map:mapTex(),side:THREE.DoubleSide,transparent:true}));m.position.copy(mid);m.scale.set(.02,1,1);add(m);
    tween(.45,e=>m.scale.x=Math.max(.02,e),{ease:E.outBack});wait(1.0,()=>tween(.3,e=>{m.scale.setScalar(Math.max(.01,1-e));m.position.lerp(tp,e)},{done:()=>rm(m)}));break}
  case'VOWEL_FLIP':{cellsW.concat(cellsW.length?[]:[tp]).forEach(p=>{starsAt(p,10,0xffd84a);flash(p,2.4)});break}
  case'SCANNER':{const g=mkScanner();const a=P(o.bx,o.by+o.bh*.3,1),b=P(o.bx+o.bw,o.by+o.bh*.7,1);popIn(g,a,1.2);tween(1.4,(e,k2)=>{g.position.lerpVectors(a,b,e);g.position.y+=Math.sin(k2*Math.PI*2)*.8},{delay:.3,ease:E.inOut,done:()=>popOut(g)});break}
  case'LOCK':{const g=mkLock();g.position.copy(tp).add(v3(0,3,.2));add(g);tween(.55,e=>g.position.y=tp.y+3*(1-e),{ease:E.outBounce,done:()=>{flash(tp,2.2,0xffe080);starsAt(tp,8,0xffd84a);
    const ring=new THREE.Mesh(new THREE.TorusGeometry(.6,.05,8,40),new THREE.MeshBasicMaterial({color:'#ffd84a',transparent:true}));ring.position.copy(tp);add(ring);tween(.5,e=>{ring.scale.setScalar(1+e*.6);ring.material.opacity=1-e},{done:()=>rm(ring)});
    tween(.35,e=>{g.scale.setScalar(1-.6*e);g.position.set(tp.x+.38*e,tp.y+.38*e,tp.z)},{delay:.25,done:()=>rm(g)})}});break}
  case'UNLOCK':{const g=mkLock();g.position.copy(tp).add(v3(.38,.38,.2));g.scale.setScalar(.5);add(g);const sh=g.userData.shackle;tween(.3,e=>{g.scale.setScalar(.5+.6*e);g.position.copy(tp).add(v3(.38*(1-e),.38*(1-e)+e*.6,.4))},{ease:E.outBack,done:()=>{
    tween(.25,e=>{sh.position.y=.25+.18*e;sh.rotation.y=e*1.2},{done:()=>{starsAt(tp,10,0xffd84a);flash(tp,2.4,0xffe080);tween(.4,e=>{g.position.y+=.05;g.scale.setScalar(1.1*(1-e)+.01)},{delay:.2,done:()=>rm(g)})}})}});break}
  case'DOUBLE':{const s=mkDouble();s.position.copy(mid);s.scale.setScalar(.01);add(s);tween(.55,(e,k2)=>{s.scale.setScalar(Math.max(.01,e*1.6));s.rotation.y=(1-k2)*Math.PI*2},{ease:E.outBack});wait(1.1,()=>popOut(s));break}
  case'LETTER_RAIN':{const cl=mkCloud();cl.scale.setScalar(1.6);const y=top.y+.3;cl.position.set(top.x-o.bw/U,y,1);add(cl);tween(.5,e=>cl.position.x=top.x-o.bw/U*(1-e),{ease:E.outCubic});
    cellsW.forEach((p,i)=>wait(.5+i*.15,()=>{const d=new THREE.Mesh(new THREE.SphereGeometry(.1,10,8),std('#3fa9ff',.3));d.scale.y=1.6;d.position.set(p.x,y,1);add(d);tween(.35,e=>d.position.y=y+(p.y-y)*e,{ease:E.inQuad,done:()=>{rm(d);flash(p,2,0x9be7ff);starsAt(p,5,0x9be7ff)}})}));
    wait(1.6,()=>tween(.45,e=>cl.position.x=top.x+o.bw/U*1.2*e,{ease:E.inCubic,done:()=>rm(cl)}));break}
  }}

/* ---------- win, lose, confetti ---------- */
function brickMesh(ch,lit){const g=new THREE.Group();const m=new THREE.Mesh(geo.brick||(geo.brick=tileGeo(1.36,.66,.5,.12,.06)),std(lit?'#e0603c':'#8a8f9c',.5));g.add(m);
  const tex=ctex(256,128,(x,w,h)=>{for(let i=0;i<70;i++){x.fillStyle=Math.random()<.5?'rgba(255,255,255,.13)':'rgba(90,20,0,.15)';x.fillRect(Math.random()*w,Math.random()*h,4,4)}
    x.font='100px "Lilita One",sans-serif';x.textAlign='center';x.textBaseline='middle';x.lineJoin='round';x.lineWidth=12;x.strokeStyle=lit?'#7a2410':'#4a4f5c';x.strokeText(ch,w/2,h/2+7);
    const gr=x.createLinearGradient(0,22,0,108);gr.addColorStop(0,lit?'#fff6b0':'#e6e8ee');gr.addColorStop(1,lit?'#ffb300':'#a3a7b3');x.fillStyle=gr;x.fillText(ch,w/2,h/2+7)});
  const p=new THREE.Mesh(new THREE.PlaneGeometry(1.3,.64),new THREE.MeshBasicMaterial({map:tex,transparent:true}));p.position.z=.314;g.add(p);return g}
let winObjs=[];
/** Big W-I-N bricks drop into a box in the given rect. [n] bricks are earned (the others stay grey). */
function win(o){winClear();const c=P(o.x+o.w/2,o.y+o.h/2);const unitsW=o.w/U,sc=Math.min(unitsW/6.2,(o.h/U)/2.4);const g=new THREE.Group();g.position.copy(c);g.scale.setScalar(sc);g.rotation.x=.38;add(g);winObjs.push(g);
  const addm=(m,x,y,z)=>{m.position.set(x,y,z);g.add(m);return m};
  addm(new THREE.Mesh(new THREE.BoxGeometry(5.9,.14,2.3),std('#ffc93c',.3,.35)),0,-.62,0);addm(new THREE.Mesh(new THREE.BoxGeometry(5.6,.55,2.0),std('#2453d6',.5)),0,-.275,0);addm(new THREE.Mesh(new THREE.BoxGeometry(5.8,.14,2.2),std('#ffc93c',.3,.35)),0,.07,0);
  const TOP=.14,SX=[-1.8,0,1.8];const slots=SX.map(x=>addm(new THREE.Mesh(new THREE.BoxGeometry(1.5,.05,.85),new THREE.MeshStandardMaterial({color:'#14308f',emissive:'#ffc93c',emissiveIntensity:0,roughness:.4})),x,TOP+.02,0));
  [...'WIN'].forEach((ch,i)=>{const lit=i<(o.n==null?3:o.n);const b=brickMesh(ch,lit);b.visible=false;g.add(b);
    wait(.35+i*.5,()=>{const x=SX[i],s=v3(x+rnd(2),7.5,2.5),e=v3(x,TOP+.33,0),r0=[rnd(6),rnd(6),rnd(3)];b.visible=true;snd('whoosh');
      tween(.5,(k,lin)=>{b.position.lerpVectors(s,e,k);const r=1-E.outCubic(lin);b.rotation.set(r0[0]*r,r0[1]*r,r0[2]*r)},{ease:E.inQuad,done:()=>{snd(lit?'star':'clack');emit('brick',{i,lit});
        tween(.4,(q,k)=>{const sq=1-Math.sin(k*Math.PI*2)*(1-k)*.28;b.scale.set(1/Math.sqrt(sq),sq,1/Math.sqrt(sq));b.position.y=TOP+.33*sq},{ease:E.lin});
        if(lit){tween(.5,(q,k)=>slots[i].material.emissiveIntensity=k<.3?k/.3:Math.max(.35,1-(k-.3)),{ease:E.lin});
          for(let n=0;n<10;n++){const st=sprite(starTex,0xffd84a,.45);st.position.copy(wpos(b)).add(v3(0,.5,.5));addPart(st,ov.scene,{v:v3(rnd(6),3+Math.random()*4,rnd(2)+1),g:7,life:1.1,fade:true})}}}})})});
  if(o.voice)wait(.35+2*.5+.5,()=>emit('winLanded'))}
function winClear(){winObjs.forEach(rm);winObjs=[]}
function confetti(o){confettiAt(P(o.x,o.y,1),o.n||80,o.spread||4)}
function fireworks(o){const n=o.n||4;for(let i=0;i<n;i++)wait(i*.35,()=>{const p=P(W*(.2+.6*Math.random()),H*(.2+.3*Math.random()),1.5);snd('bang');flash(p,5,[0xffe27a,0xff9ec7,0x7fe3ff,0xb8f27a][i%4]);
  for(let k=0;k<26;k++){const s=sprite(starTex,[0xffe27a,0xff9ec7,0x7fe3ff,0xb8f27a][(i+k)%4],.35);s.position.copy(p);const a=k/26*Math.PI*2;addPart(s,ov.scene,{v:v3(Math.cos(a)*6,Math.sin(a)*6,rnd(2)),g:3,drag:1.2,life:1.3,fade:true})}})}
/** Out of moves: the tiles turn grey and fall. */
function lose(o){const grey=new THREE.Color('#7d8291');const tiles=o.cells.map(q=>tileAt(q));
  tiles.forEach((t,i)=>{const q=o.cells[i];const a=t.userData.mat.color.clone();tween(.5,e=>t.userData.mat.color.copy(a).lerp(grey,e),{delay:((q.x-o.cells[0].x)+(q.y-o.cells[0].y))/U*.04})});
  wait(.8,()=>tiles.map((t,i)=>[t,o.cells[i]]).sort((a,b)=>a[1].y-b[1].y||a[1].x-b[1].x).forEach(([t],i)=>wait(i*.025,()=>{addPart(t,ov.scene,{v:v3(rnd(1.5),Math.random()*1.5,1+Math.random()),av:v3(rnd(3)-2,rnd(2),rnd(3)),g:9,life:2.2});if(i%6===0)sfx.clack()})));
  wait(2.2,()=>emit('loseDone'))}

/* ---------- mystery cards (Compose handles the taps on the same rects) ---------- */
let cardSet=null;
function cards(o){cardsClear();const from=P(o.fx,o.fy,0);const list=o.items.map((it,i)=>{const r=o.rects[i];const c=mkCard(cardTex(it.emoji,it.name,it.rare));c.userData.it=it;c.rotation.y=Math.PI;c.position.copy(from);c.scale.setScalar(.2);add(c);
    const home=P(r.x+r.w/2,r.y+r.h/2);const s=(r.h/U)/1.55;c.userData.home=home;c.userData.s=s;
    tween(.6,e=>{c.position.lerpVectors(from,home,e);c.scale.setScalar(.2+(s-.2)*e);c.rotation.z=(1-e)*3},{delay:i*.15,ease:E.outBack,done:()=>sfx.clack()});return c});
  cardSet={list,picked:false,rays:null};snd('sparkle')}
function cardPick(o){const S=cardSet;if(!S||S.picked)return;const i=o.i,c=S.list[i];if(!c)return;S.picked=true;const it=c.userData.it;
  const r=rays(it.rare?7:5,it.rare?0xffd84a:0xffffff);r.position.copy(c.userData.home).setZ(1.2);r.scale.setScalar(.01);add(r);S.rays=r;const to=c.userData.home.clone().setZ(1.6);to.y+=.3;
  tween(.55,(e,k)=>{c.position.lerpVectors(c.userData.home,to,e);c.rotation.y=Math.PI+e*Math.PI;c.scale.setScalar(c.userData.s*(1+.25*e));r.scale.setScalar(Math.max(.01,e*c.userData.s))},{ease:E.outBack,done:()=>{
    snd('fanfare');starsAt(c.position,it.rare?20:10,it.rare?0xffd84a:0xffffff);if(it.rare)confettiAt(c.position,60,2);
    S.list.forEach((x,j)=>{if(j!==i)tween(.5,e=>{x.rotation.y=Math.PI+e*Math.PI;x.material[4].color.setScalar(1-.45*e)},{delay:.3+j*.1,ease:E.inOut})});
    wait(1.8,()=>{rm(r);flyTo(c,'bag',.6,{s0:c.userData.s*1.25,done:()=>emit('cardDone',{i})});S.list.forEach((x,j)=>{if(j!==i)tween(.3,e=>x.scale.setScalar(Math.max(.01,x.userData.s*(1-e))),{done:()=>rm(x)})})})}})}
function cardsClear(){if(cardSet){cardSet.list.forEach(rm);rm(cardSet.rays)}cardSet=null}

/* ---------- boss: Scramble sits behind the board's top edge ---------- */
let boss=null;
function bossShow(o){bossHide();const BS=makeBoss();const sc=(o.w/U)/8.2;const topY=P(0,o.y).y,cx=P(o.x+o.w/2,0).x;
  // Clip the body at the board's top edge as seen from the camera, so he is always behind the board.
  const cp=ov.cam.position;const n=v3(0,cp.z,topY-cp.y).normalize();const plane=new THREE.Plane(n,-n.y*topY);
  BS.g.traverse(m=>{if(m.isMesh){m.material=m.material.clone();m.material.clippingPlanes=[plane]}});BS.skin=null;BS.g.traverse(m=>{if(m.isMesh&&m.material.color&&m.material.color.getHexString()==='8a4dff'&&!BS.skin)BS.skin=m.material});
  const root=v3(cx,topY-1.15*sc,-2.7*sc);BS.g.scale.setScalar(sc);BS.g.position.copy(root);add(BS.g);
  const skin=new THREE.MeshStandardMaterial({color:'#8a4dff',roughness:.45});BS.arms.forEach(a=>Object.values(a).forEach(m=>{m.material=skin;add(m)}));
  BS.hands.forEach((h,i)=>{const s=i?1:-1;h.scale.setScalar(sc);h.userData.grip=v3(cx+s*(o.w/U)*.36,topY+.18*sc,-.05);h.position.copy(h.userData.grip);h.rotation.set(0,0,-s*.15);add(h)});
  const bar=new THREE.Group();const bg=new THREE.Mesh(new THREE.PlaneGeometry(2.6,.34),new THREE.MeshBasicMaterial({color:'#1b1440'}));bar.add(bg);
  const fill=new THREE.Mesh(new THREE.PlaneGeometry(2.46,.22),new THREE.MeshBasicMaterial({color:'#ff4d6d'}));fill.position.z=.01;bar.add(fill);
  const nm=textMesh(o.name||'SCRAMBLE',{h:.48,colors:['#ffe0f0','#ff8fd0','#c2187a'],stroke:'#3a0030'});nm.position.set(0,.45,.02);bar.add(nm);
  bar.position.set(cx+(o.w/U)*.5+1.4*sc*1.2,topY+2.2*sc,-.5);bar.scale.setScalar(Math.max(.6,sc*1.3));add(bar);
  boss={BS,sc,topY,cx,root,bar,fill,hp:o.hp==null?1:o.hp,state:'idle',stT:T,armT:null,rel:0,grin:0,dizzy:[]};setHp(boss.hp,true)}
function setHp(v,now){const b=boss;if(!b)return;const a=b.hp;b.hp=v;const f=b.fill;if(now){f.scale.x=Math.max(.001,v);f.position.x=-(1-v)*1.23;return}
  tween(.5,e=>{const x=a+(v-a)*e;f.scale.x=Math.max(.001,x);f.position.x=-(1-x)*1.23},{ease:E.outCubic})}
function eyesX(on){boss.BS.xs.forEach(x=>x.visible=on);boss.BS.pupils.forEach(p=>p.visible=!on)}
function headW(){return boss.BS.bod.localToWorld(v3(0,2.4,1.3))}
/** A word hits the boss: a star flies from the word to his head. */
function bossHit(o){const b=boss;if(!b)return;const from=P(o.x,o.y,1);const pr=starMesh(.35,.15);pr.position.copy(from);add(pr);
  tween(.45,(e,k)=>{const t=headW();pr.position.lerpVectors(from,t,e);pr.position.y+=Math.sin(k*Math.PI)*1.2;pr.rotation.z=k*10},{ease:E.inCubic,done:()=>{rm(pr);const t=headW();flash(t,3.5,0xffe080);starsAt(t,12);
    if(o.hp!=null&&o.hp<=0){bossDefeat();return}
    b.state='hit';b.stT=T;eyesX(true);setHp(o.hp==null?Math.max(0,b.hp-.25):o.hp);for(let i=0;i<3;i++){const s=sprite(starTex,0xffe27a,.5*b.sc*2);add(s);b.dizzy.push(s)}
    wait(1.6,()=>{if(boss!==b)return;b.dizzy.forEach(rm);b.dizzy=[];eyesX(false);b.state='idle'})}})}
/** The boss leans in and throws a box (or freezes a tile) at a cell. */
function bossAttack(o){const b=boss;if(!b)return;b.state='attack';b.stT=T;b.grin=1;const h=b.BS.hands[1],gp=h.userData.grip.clone(),up=gp.clone().add(v3(-.8*b.sc,2.8*b.sc,-1.2*b.sc));
  b.armT=gp.clone();tween(.4,(e,k)=>{b.armT.copy(gp).lerp(up,e);b.armT.y+=Math.sin(k*Math.PI)*.4*b.sc},{ease:E.outBack});
  const obj=o.freeze?new THREE.Mesh(geo.ice||(geo.ice=tileGeo(1.1,1.1,.5,.2,.06)),new THREE.MeshStandardMaterial({color:'#bdf0ff',transparent:true,opacity:.6,roughness:.05,emissive:'#4aa8d0',emissiveIntensity:.5})):crate();obj.scale.setScalar(.01);add(obj);
  tween(.6,e=>{obj.scale.setScalar(Math.max(.01,e*.9));obj.position.copy(h.position).add(v3(0,.75*b.sc,.1))},{delay:.2});
  wait(.8,()=>{const s=h.position.clone().add(v3(0,.75*b.sc,.1)),tp=P(o.x,o.y,.1);const a0=b.armT.clone();tween(.2,e=>b.armT.copy(a0).lerp(gp.clone().add(v3(-.6*b.sc,1.4*b.sc,.4)),e));
    tween(.55,(e,k)=>{obj.position.lerpVectors(s,tp,e);obj.position.y+=Math.sin(k*Math.PI)*2.0;obj.position.z+=Math.sin(k*Math.PI)*1.6;obj.rotation.set(k*6,k*4,0)},{ease:E.lin,done:()=>{
      rm(obj);for(let i=0;i<12;i++){const d=new THREE.Mesh(geo.puff||(geo.puff=new THREE.SphereGeometry(.14,10,8)),new THREE.MeshBasicMaterial({color:o.freeze?'#d8f6ff':'#e9e2ff',transparent:true}));d.position.copy(tp);addPart(d,ov.scene,{v:v3(rnd(4),rnd(4),1),drag:2,life:.7,fade:true,grow:1.4})}
      emit('bossAttackLanded');const a1=b.armT.clone();tween(.45,e=>b.armT.copy(a1).lerp(gp,e),{done:()=>{b.armT=null;b.state='idle';b.grin=0}})}})})}
/** The final hit: he lets go and falls backwards behind the board with smoke. */
function bossDefeat(){const b=boss;if(!b)return;setHp(0);b.state='dead';b.stT=T;eyesX(true);b.grin=-1;tween(.35,e=>b.rel=e,{ease:E.outCubic});
  const g=b.BS.g,p0=g.position.clone();
  tween(1.4,(e,k)=>{g.rotation.x=-1.35*e;g.position.set(p0.x,p0.y+Math.sin(k*Math.PI)*.9*b.sc-e*3.2*b.sc,p0.z-e*3.5*b.sc);g.rotation.z=Math.sin(k*18)*.12*(1-k)},{delay:.4,ease:E.inCubic,done:()=>{
    snd('bang');const pp=v3(b.cx,b.topY+.4*b.sc,-1.6*b.sc);for(let i=0;i<24;i++){const d=new THREE.Mesh(geo.puff||(geo.puff=new THREE.SphereGeometry(.14,10,8)),new THREE.MeshBasicMaterial({color:'#e2d6ff',transparent:true}));d.position.copy(pp).add(v3(rnd(6*b.sc),rnd(.4),rnd(1)));d.scale.setScalar(b.sc*1.5);addPart(d,ov.scene,{v:v3(rnd(2),1.5+Math.random()*2.5,.6),drag:1.2,life:1.4,fade:true,grow:2.4*b.sc})}
    starsAt(v3(b.cx,b.topY+b.sc,-.5),18,0xffe27a,1.3);rm(b.bar);b.BS.hands.forEach(h=>h.visible=false);b.BS.arms.forEach(a=>Object.values(a).forEach(m=>m.visible=false));
    wait(.4,()=>{confettiAt(v3(b.cx,b.topY+1,2),100,6);emit('bossGone')});wait(1.6,()=>{if(boss===b)bossHide()})}})}
function bossHide(){if(!boss)return;const b=boss;rm(b.BS.g);rm(b.bar);b.BS.hands.forEach(rm);b.BS.arms.forEach(a=>Object.values(a).forEach(rm));b.dizzy.forEach(rm);boss=null}
function bossArm(i,t){const b=boss,s=i?1:-1,h=b.BS.hands[i],a=b.BS.arms[i];const S=b.BS.g.localToWorld(b.BS.sh[i].clone());
  if(b.state==='dead'){const fl=S.clone().add(v3(s*1.3*b.sc,(1.4+Math.sin(T*16+i*2)*.35)*b.sc,.5*b.sc));h.position.copy(h.userData.grip).lerp(fl,b.rel);h.rotation.z=-s*(.15+b.rel*.8)+Math.sin(T*16+i)*.3*b.rel}
  else if(i===1&&b.armT)h.position.copy(b.armT);
  else{h.position.copy(h.userData.grip);h.position.y+=Math.abs(Math.sin(t*2.6+i*1.7))*.08*b.sc}
  const M=S.clone().add(h.position).multiplyScalar(.5).add(v3(s*.55*b.sc,.55*b.sc,-.15*b.sc));
  placeSeg(a.up,S,M);placeSeg(a.lo,M,h.position);a.up.scale.x=a.up.scale.z=a.lo.scale.x=a.lo.scale.z=b.sc;a.j1.position.copy(S);a.j2.position.copy(M);a.j1.scale.setScalar(b.sc);a.j2.scale.setScalar(b.sc)}
let bossHeld=false;
function bossUpdate(dt,t){const b=boss;if(!b)return;const g=b.BS.g,bod=b.BS.bod;const vis=!bossHeld;g.visible=vis;b.bar.visible=vis;
  if(b.state!=='dead'){b.BS.hands.forEach(h=>h.visible=vis);b.BS.arms.forEach(a=>Object.values(a).forEach(m=>m.visible=vis))}
  if(b.state!=='dead'){const k=T-b.stT;const lean=b.state==='attack'?Math.min(1,k*3)*(k<1.4?1:Math.max(0,1-(k-1.4)*3)):0;const fl=b.state==='hit'?Math.max(0,1-k*.8):0;
    g.position.copy(b.root);g.position.y+=(Math.sin(t*1.8)*.08+lean*.3)*b.sc;g.rotation.set(lean*.28-fl*.3+Math.sin(T*22)*.03*fl,Math.sin(t*.7)*.1,Math.sin(t*.9)*.04);
    bod.scale.set(1+Math.sin(t*2.2)*.015,1-Math.sin(t*2.2)*.015,1);
    const blink=(t%3.2)<.12?.15:1;b.BS.eyes.forEach(e=>e.scale.y=blink);b.BS.pupils.forEach(p=>{p.position.x=p.userData.bx+Math.sin(t*.8)*.12;p.scale.y=blink})}
  const ms=b.state==='hit'||b.state==='dead'?1.7:b.grin>0?1.3:1;b.BS.mouth.scale.set(b.grin>0?1.25:1,b.BS.mouth.scale.y+(ms-b.BS.mouth.scale.y)*Math.min(1,dt*10),1);
  bossArm(0,t);bossArm(1,t);
  b.dizzy.forEach((d,i)=>{const a=T*5+i*2.09;const hp2=b.BS.bod.localToWorld(v3(0,3.6,0));d.position.set(hp2.x+Math.cos(a)*1.4*b.sc,hp2.y+.2*b.sc,hp2.z+Math.sin(a)*1.4*b.sc)})}

/* ---------- shop: coins fly to the item, the item flies into the bag ---------- */
let shopBag=null;
function shopShow(o){shopHide();const bag=new THREE.Group();const pts=[[0,0],[.8,.05],[1.05,.55],[1,1.15],[.55,1.55],[.45,1.7],[.65,1.95]].map(([a,b])=>new THREE.Vector2(a,b));
  bag.add(new THREE.Mesh(new THREE.LatheGeometry(pts,32),std('#b07a45',.8)));const rope=new THREE.Mesh(new THREE.TorusGeometry(.48,.07,8,24),std('#ffd84a',.5));rope.rotation.x=Math.PI/2;rope.position.y=1.62;bag.add(rope);
  const s=(o.s||90)/U/2.2;bag.scale.setScalar(s);bag.position.copy(P(o.x,o.y+(o.s||90)/2));add(bag);bag.userData.s=s;shopBag=bag;A.bag=[o.x,o.y]}
function shopHide(){rm(shopBag);shopBag=null}
function shopBuy(o){const from=anchor('coins',2),to=P(o.x,o.y,1);for(let i=0;i<10;i++){const c=coinMesh();c.position.copy(from);add(c);const st=from.clone();
    tween(.5,(e)=>{const cp=st.clone().lerp(to,.5).add(v3(0,1.5,1));const a=st.clone().lerp(cp,e),b=cp.clone().lerp(to,e);c.position.copy(a.lerp(b,e));c.userData.spin.rotation.y+=.4},{delay:i*.05,ease:E.inOut,done:()=>{rm(c);if(i%3===0)sfx.coin()}})}
  wait(.9,()=>{const t=toolMesh(o.k);const p=ICON_POSE[o.k]||{};t.rotation.set(p.rx||0,p.ry||0,p.rz||0);t.position.copy(to);t.scale.setScalar(.01);add(t);snd('whoosh');
    tween(.3,e=>{t.position.z=1+e*1.2;t.scale.setScalar(Math.max(.01,(p.s||1)*1.2*e));t.rotation.y+=.2},{ease:E.outBack});
    const bp=shopBag?shopBag.position.clone().add(v3(0,1.9*shopBag.userData.s,.3)):anchor('bag',1);
    tween(.65,(e,k)=>{t.position.lerpVectors(to.clone().setZ(2.2),bp,e);t.position.y+=Math.sin(k*Math.PI)*2.2;t.scale.setScalar((p.s||1)*1.2*(1-.75*e));t.rotation.z=k*6},{delay:.35,ease:E.inOut,done:()=>{rm(t);snd('bang');
      if(shopBag){const b=shopBag,s=b.userData.s;tween(.4,(q,k2)=>{const z=1+Math.sin(k2*Math.PI*2)*(1-k2)*.25;b.scale.set(s/z,s*z,s/z)},{ease:E.lin})}
      starsAt(bp,8);popupTo('+1 '+(o.name||'').toUpperCase(),bp.clone().add(v3(-.5,1,.5)),null,null,{...GREEN_TXT,h:.6})}})})}

/* ---------- overlay per-frame ---------- */
function ovUpdate(dt,t){bossUpdate(dt,t);if(cardSet&&!cardSet.picked)cardSet.list.forEach((c,i)=>{c.position.y=c.userData.home.y+Math.sin(t*2+i)*.06});if(cardSet&&cardSet.rays)cardSet.rays.rotation.z=t*.8;
  winObjs.forEach(g=>{g.rotation.y=Math.sin(t*.6)*.08})}

/* ====================================================================== backdrops (full-screen scenes) */
const SCENES={};
function setBackdrop(name,data){document.body.style.background="";if(bd&&bd.api.dispose){use(bd.world);try{bd.api.dispose()}catch(e){}}$('#introName').classList.remove('show');$('#introSub').classList.remove('show');document.body.className='';
  bd=null;shake=0;if(!name||!SCENES[name]){use(ov);return}const world=mkWorld(40);world.cam.aspect=W/H;world.cam.updateProjectionMatrix();use(world);
  const api=SCENES[name](world,data||{});bd={world,name,api};api.start&&api.start();use(ov)}
function act(name,data){if(!bd||!bd.api.act)return;use(bd.world);bd.api.act(name,data||{});use(ov)}

/* ---------- studio intro ---------- */
SCENES.intro=function(w){const scene=w.scene,cam=w.cam;cam.fov=38;cam.far=300;cam.updateProjectionMatrix();document.body.className='bg-intro';
  addLights(scene,{hemi:.95,sun:.7,pos:[3,6,10],ground:0x1a2a7a});
  const sweep=new THREE.PointLight(0xffffff,0,26);sweep.position.set(-14,1,4);scene.add(sweep);
  const N=600,arr=new Float32Array(N*3);for(let i=0;i<N;i++){arr[i*3]=rnd(140);arr[i*3+1]=rnd(80);arr[i*3+2]=-15-Math.random()*70}
  const sg=new THREE.BufferGeometry();sg.setAttribute('position',new THREE.BufferAttribute(arr,3));
  const stars=new THREE.Points(sg,new THREE.PointsMaterial({color:0xcfe0ff,size:.35,transparent:true,opacity:.85}));scene.add(stars);
  const logo=new THREE.Group();scene.add(logo);const S=.22,vox=[];
  const c=document.createElement('canvas');c.width=48;c.height=20;const x=c.getContext('2d');x.fillStyle='#fff';x.font='8px "Press Start 2P",monospace';x.textBaseline='top';
  x.fillText('RETRO',2,1);x.fillText('RAJA',6,11);const data=x.getImageData(0,0,48,20).data;
  const isL=(i,j)=>i>=0&&j>=0&&i<48&&j<20&&data[(j*48+i)*4+3]>110;const bx0=-1,bx1=44,by0=-2,by1=20;
  const tail=(i,j)=>(j===by1+1&&i>=4&&i<=9)||(j===by1+2&&i>=4&&i<=7)||(j===by1+3&&i>=4&&i<=5);
  const inB=(i,j)=>{if(i<bx0||i>bx1||j<by0||j>by1)return tail(i,j);const cx=Math.min(i-bx0,bx1-i),cy=Math.min(j-by0,by1-j);return !(cx<3&&cy<3&&cx+cy<3)};
  for(let j=by0;j<=by1+3;j++)for(let i=bx0;i<=bx1;i++){if(!inB(i,j))continue;const edge=!inB(i-1,j)||!inB(i+1,j)||!inB(i,j-1)||!inB(i,j+1);
    const X=(i-21.5)*S,Y=-(j-9)*S+1.7;vox.push({X,Y,z:0,col:edge?'#9a1c1c':'#e33b3b'});vox.push({X,Y,z:-1,col:'#9a1c1c'});if(isL(i,j))vox.push({X,Y,z:1,col:'#ffffff'})}
  const ctrl=["...BBBBBBBBBBBBBBBB...",".BBBBBBBBBBBBBBBBBBBB.","BBBBDBBBBBBBBBBBBYBBBB","BBBDDDBBBBLLBBBBRBYBBB","BBBBDBBBBBBBBBBBBRBBBB","BBBBBBBBBBBBBBBBBBBBBB","BBBBBBB........BBBBBBB",".BBBBB..........BBBBB."];
  const CC={B:'#2f6fe0',D:'#16295e',Y:'#ffc93c',R:'#e5533d',L:'#9cc8ff'};
  ctrl.forEach((row,j)=>[...row].forEach((ch,i)=>{if(ch==='.')return;const X=(i-10.5)*S+1.4,Y=-2.15-j*S;vox.push({X,Y,z:0,col:CC[ch]});vox.push({X,Y,z:-1,col:ch==='B'?'#1d4fb0':CC[ch]});if('DYRL'.includes(ch))vox.push({X,Y,z:1,col:CC[ch]})}));
  const mat=new THREE.MeshStandardMaterial({roughness:.35,metalness:.08,transparent:true,opacity:1});
  const im=new THREE.InstancedMesh(new THREE.BoxGeometry(S*.93,S*.93,S*.93),mat,vox.length);const col=new THREE.Color();
  vox.forEach((v,k)=>{col.set(v.col);im.setColorAt(k,col);v.t=new THREE.Vector3(v.X,v.Y,v.z*S);v.s=new THREE.Vector3(rnd(1),rnd(1),Math.random()*.8-.2).normalize().multiplyScalar(14+Math.random()*16);
    v.r=[Math.random()*7,Math.random()*7,Math.random()*7];v.d=.15+((v.X+5.5)/11)*.9+Math.random()*.25});
  im.instanceColor.needsUpdate=true;logo.add(im);const dummy=new THREE.Object3D();let settled=false,t0=T;
  function place(t){if(settled)return;let all=true;vox.forEach((v,k)=>{const k1=clamp((t-v.d)/1.05,0,1);if(k1<1)all=false;const e=E.outCubic(k1);
    dummy.position.lerpVectors(v.s,v.t,e);const r=1-e;dummy.rotation.set(v.r[0]*r,v.r[1]*r,v.r[2]*r);dummy.scale.setScalar(.25+.75*e);dummy.updateMatrix();im.setMatrixAt(k,dummy.matrix)});
    im.instanceMatrix.needsUpdate=true;if(all)settled=true}
  const sparkMat=new THREE.MeshStandardMaterial({color:'#ffd84a',emissive:'#8a6200',roughness:.3,transparent:true});
  const sparks=[[-6.2,3.4],[6.0,4.3],[6.6,-1.6],[-5.6,-2.4],[-2.0,5.0]].map(([sx,sy],i)=>{const g=new THREE.Group();
    [[0,0],[1,0],[-1,0],[0,1],[0,-1],[2,0],[-2,0],[0,2],[0,-2]].forEach(([a,b],n)=>{if(n>4&&i%2)return;const m=new THREE.Mesh(new THREE.BoxGeometry(S*.93,S*.93,S*.93),sparkMat);m.position.set(a*S,b*S,0);g.add(m)});
    g.position.set(sx,sy,.5);g.scale.setScalar(.001);logo.add(g);return g});
  const word=new THREE.Group();scene.add(word);const wt=[];
  [...'WORDNOID'].forEach((ch,i)=>{const t=makeTile(ch,TILE_COLORS[[1,4,0,2,5,3,8,6][i]]);t.scale.setScalar(1.15);t.position.set((i-3.5)*1.3,30,0);word.add(t);wt.push(t)});
  const camP={x:5,y:-2.5,z:9};let fitLogo=18,fitWord=12,phase=0;
  function fit(){fitLogo=fitDist(cam,14,9.5,W*.94,H*.74);fitWord=fitDist(cam,11.2,2.2,W*.9,H*.5)}
  return{onResize:fit,
    start(){fit();t0=T;camP.x=6;camP.y=-3;camP.z=fitLogo*.5;sfx.whoosh();
      tween(3.0,e=>{camP.x=6*(1-e);camP.y=-3*(1-e)+.2*e;camP.z=fitLogo*(.5+.5*e)},{ease:E.inOut});
      wait(2.2,()=>{sfx.shine();tween(1.1,(e,k)=>{sweep.position.x=-14+28*e;sweep.intensity=Math.sin(k*Math.PI)*3.2},{ease:E.lin})});
      sparks.forEach((g,i)=>wait(2.35+i*.13,()=>{sfx.pop();tween(.45,e=>g.scale.setScalar(Math.max(.001,e)),{ease:E.outBack})}));
      wait(2.6,()=>$('#introName').classList.add('show'));
      wait(4.4,()=>{$('#introName').classList.remove('show');sfx.whoosh();phase=1;
        tween(.75,e=>{logo.position.z=e*12;logo.rotation.y=e*1.1;mat.opacity=1-e;sparkMat.opacity=1-e},{ease:E.inCubic,done:()=>logo.visible=false});
        tween(1.0,e=>{camP.z=fitLogo+(fitWord-fitLogo)*e;camP.y=.2+.4*e},{ease:E.inOut})});
      wt.forEach((t,i)=>{const fr=(i%2?-1:1)*.07,r0=rnd(2);wait(4.75+i*.1,()=>{t.position.y=9;tween(.85,(e,k)=>{t.position.y=9*(1-e);t.rotation.z=r0*(1-k)+fr*k},{ease:E.outBounce,done:()=>sfx.thud()})})});
      wait(6.05,()=>{$('#introSub').classList.add('show');sfx.fanfare();emit('introName');sweep.position.set(-12,1.5,4);
        tween(1.0,(e,k)=>{sweep.position.x=-12+24*e;sweep.intensity=Math.sin(k*Math.PI)*3},{ease:E.lin});
        for(let i=0;i<40;i++){const s=sprite(starTex,0xffe27a,.35+Math.random()*.3);s.position.set(rnd(11),rnd(2),.8);
          addPart(s,scene,{v:new THREE.Vector3(rnd(6),3+Math.random()*5,rnd(3)),g:6,life:1.4+Math.random()*.6,fade:true})}});
      wait(6.9,()=>{phase=2;emit('introDone')})},
    update(dt,t){place(t-t0);stars.rotation.z+=dt*.01;
      cam.position.set(camP.x+Math.sin(t*.5)*.25,camP.y+Math.cos(t*.4)*.15,camP.z);cam.lookAt(0,phase?0:.4,0);
      if(settled&&phase===0){logo.rotation.y=Math.sin(t*1.1)*.12;logo.position.y=Math.sin(t*1.6)*.08}
      sparks.forEach((g,i)=>g.rotation.z=t*(i%2?1.5:-1.5));
      if(phase===2)wt.forEach((w2,i)=>{w2.position.y=Math.sin(t*2.6-i*.55)*.16;w2.rotation.y=Math.sin(t*1.3-i*.4)*.12})}}};

/* ---------- title: the Wordnoid tiles bobbing (after the intro, behind the welcome screen) ---------- */
SCENES.title=function(w){const scene=w.scene,cam=w.cam;document.body.className='bg-intro';addLights(scene,{hemi:.95,sun:.7,pos:[3,6,10],ground:0x1a2a7a});
  const wt=[...'WORDNOID'].map((ch,i)=>{const t=makeTile(ch,TILE_COLORS[[1,4,0,2,5,3,8,6][i]]);t.scale.setScalar(1.15);t.position.set((i-3.5)*1.3,0,0);scene.add(t);return t});
  let fd=12;const fit=()=>{fd=fitDist(cam,11.2,2.2,W*.8,H*.3)};
  return{onResize:fit,start(){fit()},update(dt,t){cam.position.set(Math.sin(t*.5)*.3,-1.2*(H/W>.6?1:1),fd);cam.lookAt(0,-1.3,0);wt.forEach((w2,i)=>{w2.position.y=Math.sin(t*2.6-i*.55)*.16;w2.rotation.y=Math.sin(t*1.3-i*.4)*.12})}}};

/* ---------- level start: letter tiles float behind the goal card ---------- */
SCENES.start=function(w,o){const scene=w.scene,cam=w.cam;cam.fov=42;cam.updateProjectionMatrix();addLights(scene,{hemi:.95,sun:.7,pos:[2,5,8],ground:0x2f7a4a});
  const L=(o.letters||'WORDNOIDLETTERSFUN').toUpperCase(),tiles=[];
  for(let i=0;i<30;i++){const t=makeTile(L[i%L.length],TILE_COLORS[(i*3)%TILE_COLORS.length]);t.userData.f={x:rnd(26),y:rnd(14),z:-5-Math.random()*10,rx:Math.random()*6,ry:Math.random()*6,sp:.25+Math.random()*.4,ph:Math.random()*6};scene.add(t);tiles.push(t)}
  return{update(dt,t){cam.position.set(Math.sin(t*.3)*.4,0,14);cam.lookAt(0,0,0);
    tiles.forEach(tl=>{const f=tl.userData.f;f.y+=dt*f.sp;if(f.y>8)f.y=-8;tl.position.set(f.x+Math.sin(t*.5+f.ph)*.4,f.y,f.z);tl.rotation.x=f.rx+t*.35;tl.rotation.y=f.ry+t*.25})}}};

/* ---------- Tiko's Town: one street, built from Kotlin's data ---------- */
SCENES.town=function(w,o){const scene=w.scene,cam=w.cam;cam.fov=42;cam.far=200;cam.updateProjectionMatrix();const sky=o.sky||'#ffd6ea';
  scene.fog=new THREE.Fog(new THREE.Color(sky),40,90);document.body.style.background='linear-gradient('+(o.skyTop||'#8fd3ff')+','+sky+')';
  addLights(scene,{hemi:.72,sun:.9,pos:[-10,16,12],shadow:22,ground:0xc08ad8,target:[0,0,-3]});
  const ground=new THREE.Mesh(new THREE.PlaneGeometry(160,90),std(o.grass||'#9be27a',.9));ground.rotation.x=-Math.PI/2;ground.receiveShadow=true;scene.add(ground);
  const box=(w2,h,d,c,x,y,z,r=.8)=>{const m=new THREE.Mesh(new THREE.BoxGeometry(w2,h,d),typeof c==='string'?std(c,r):c);m.position.set(x,y,z);m.receiveShadow=true;scene.add(m);return m};
  const walk=o.walk||'#ffd0e4';box(90,.2,3.2,walk,0,.1,-2.9);box(90,.06,5,'#5b6178',0,.03,1.3);box(90,.2,2.2,walk,0,.1,4.9);for(let x=-44;x<=44;x+=3)box(1.4,.07,.18,'#ffffff',x,.07,1.3);
  // 4 shops and the big building in the middle. o.shops: [{name,bad,fixed,col,roof}], o.big: {name,fixed,locked,label}
  const xs=[-11.8,-6.2,6.2,11.8],ws=[3.6,3.8,3.6,3.6],hs=[4.0,3.5,4.7,3.7];
  const defs=(o.shops||[]).map((s,i)=>({name:s.name,bad:s.bad,x:xs[i],w:ws[i],h:hs[i],col:s.col,roof:s.roof,fixed:s.fixed}));
  if(o.big)defs.splice(2,0,{name:o.big.name,x:0,w:5.2,h:6.2,col:o.big.col||'#c3a6ff',roof:o.big.roof||'#7b4dff',fixed:o.big.fixed,label:o.big.label,big:1});
  const GREY=new THREE.Color('#a3a7b3'),GREY2=new THREE.Color('#7d8291'),D=3.2,BZ=-6.4;
  const bld=defs.map((d,idx)=>{const g=new THREE.Group();g.position.set(d.x,0,BZ);scene.add(g);const inner=new THREE.Group();g.add(inner);
    const cB=new THREE.Color(d.col),cR=new THREE.Color(d.roof);const bodyMat=std('#a3a7b3',.75),roofMat=std('#7d8291',.6);
    const addm=(m,x,y,z)=>{m.position.set(x,y,z);m.castShadow=true;m.receiveShadow=true;inner.add(m);return m};
    addm(new THREE.Mesh(new THREE.BoxGeometry(d.w,d.h,D),bodyMat),0,d.h/2,0);
    const tri=new THREE.Shape();tri.moveTo(-d.w/2-.3,0);tri.lineTo(d.w/2+.3,0);tri.lineTo(0,1.5);tri.closePath();
    const rg=new THREE.ExtrudeGeometry(tri,{depth:D+.5,bevelEnabled:false});rg.translate(0,0,-(D+.5)/2);addm(new THREE.Mesh(rg,roofMat),0,d.h,0);
    const chim=addm(new THREE.Mesh(new THREE.BoxGeometry(.45,1.2,.45),roofMat),d.w*.28,d.h+.95,-.5);
    addm(new THREE.Mesh(new THREE.BoxGeometry(d.w+.2,.35,D+.2),std('#fff4fa',.6)),0,.17,0);
    addm(new THREE.Mesh(new THREE.BoxGeometry(.9,1.4,.12),std('#7a4a2a',.6)),0,.85,D/2+.04);
    const awnMat=new THREE.MeshStandardMaterial({map:ctex(128,32,x=>{for(let i=0;i<8;i++){x.fillStyle=i%2?'#ffffff':d.roof;x.fillRect(i*16,0,16,32)}}),roughness:.6,color:'#999999'});
    const awn=addm(new THREE.Mesh(new THREE.BoxGeometry(d.w*.86,.1,.85),awnMat),0,1.85,D/2+.42);awn.rotation.x=.28;
    const glass=[];const fm=std('#ffffff',.5);
    for(let y=2.75;y<d.h-.35;y+=1.15)for(const xx of[-d.w*.26,d.w*.26]){addm(new THREE.Mesh(new THREE.BoxGeometry(.8,.8,.08),fm),xx,y,D/2+.04);
      const gm=new THREE.MeshStandardMaterial({color:'#2f3b5c',emissive:'#ffc55a',emissiveIntensity:0,roughness:.2});addm(new THREE.Mesh(new THREE.BoxGeometry(.62,.62,.1),gm),xx,y,D/2+.07);glass.push(gm)}
    const sw=Math.min(d.w*.72,3.6);const signText=d.label||(d.fixed?d.name:(d.bad||d.name));
    const signMat=new THREE.MeshBasicMaterial({map:signTex(signText,d,!d.fixed),transparent:true});
    const sign=new THREE.Mesh(new THREE.PlaneGeometry(sw,sw*.22),signMat);sign.position.set(0,d.h+.45,D/2+.27);inner.add(sign);if(!d.fixed&&!d.label)sign.rotation.z=-.12;
    // Broken shops have boards nailed across the door.
    const planks=[];if(!d.fixed){for(const r of[-.5,.5]){const p=new THREE.Mesh(new THREE.BoxGeometry(1.2,.16,.06),std('#c98b4a',.7));p.position.set(0,.9,D/2+.14);p.rotation.z=r;inner.add(p);planks.push(p)}}
    const api={d,g,inner,chim,sign,signMat,planks,set(k){bodyMat.color.copy(GREY).lerp(cB,k);roofMat.color.copy(GREY2).lerp(cR,k);awnMat.color.set('#999999').lerp(new THREE.Color('#ffffff'),k);
      glass.forEach((m,i)=>m.emissiveIntensity=k>=(i+1)/(glass.length+1)?.9:0)}};api.set(d.fixed?1:0);return api});
  const white=std('#ffffff',.4),sway=[];const deco=o.deco||{};
  function lolli(x,z,c,s,on){const g=new THREE.Group();const st=new THREE.Mesh(new THREE.CylinderGeometry(.06,.06,2.2,8),white);st.position.y=1.1;st.castShadow=true;g.add(st);
    const cc2=on?c:'#a3a7b3';const t=swirlTex(cc2);const disc=new THREE.Mesh(new THREE.CylinderGeometry(.75,.75,.22,32),[std(cc2,.3),new THREE.MeshStandardMaterial({map:t,roughness:.3}),new THREE.MeshStandardMaterial({map:t,roughness:.3})]);
    disc.rotation.x=Math.PI/2;disc.position.y=2.6;disc.castShadow=true;g.add(disc);g.position.set(x,0,z);g.scale.setScalar(s);g.userData.ph=Math.random()*6;scene.add(g);sway.push(g)}
  [[-14.6,-4.6,'#e8457a',1],[-9.0,-4.4,'#7b4dff',.8],[-3.5,-4.4,'#f2861e',.85],[3.4,-4.4,'#2a8fe0',.85],[9.0,-4.4,'#e8457a',.8],[14.6,-4.6,'#39b86a',1],
   [-13,5.3,'#2a8fe0',1.15],[-2.5,5.4,'#e8457a',1.2],[8.5,5.3,'#f2861e',1.15],[17,5.3,'#7b4dff',1.2]].forEach(a=>lolli(...a,!!deco.trees));
  [[-8.6,-4.3,'#ff6fa8'],[-3.9,-4.3,'#ffd166'],[3.9,-4.3,'#7fe3ff'],[8.6,-4.3,'#b8f27a'],[-16,-4.2,'#c3a6ff'],[16,-4.2,'#ff9ec7']].forEach(([x,z,c])=>{
    const m=new THREE.Mesh(new THREE.SphereGeometry(.45,20,14),std(deco.fountain?c:'#a3a7b3',.25));m.scale.y=.75;m.position.set(x,.33,z);m.castShadow=true;scene.add(m)});
  [-9,3,13].forEach(x=>{box(.12,2.6,.12,'#3d3a5c',x,1.3,4.3);const l=new THREE.Mesh(new THREE.SphereGeometry(.25,16,12),new THREE.MeshStandardMaterial({color:deco.lamps?'#fff3c4':'#8a8f9c',emissive:'#ffd36b',emissiveIntensity:deco.lamps?.8:0}));l.position.set(x,2.7,4.3);scene.add(l)});
  const clouds=[];for(let i=0;i<6;i++){const g=new THREE.Group();for(let k=0;k<5;k++){const m=new THREE.Mesh(new THREE.SphereGeometry(.9+Math.random()*.6,16,12),std('#ffffff',.9));m.position.set(k*1.1-2.2,Math.random()*.5,rnd(.8));g.add(m)}
    g.position.set(-30+i*11,10+Math.random()*3,-22-Math.random()*8);g.userData.sp=.3+Math.random()*.4;scene.add(g);clouds.push(g)}
  const car=new THREE.Group();const cb=new THREE.Mesh(new THREE.BoxGeometry(1.7,.5,.9),std('#ff6fa8',.35));cb.position.y=.45;cb.castShadow=true;car.add(cb);
  const cab=new THREE.Mesh(new THREE.BoxGeometry(.95,.45,.8),std('#ffffff',.3));cab.position.set(-.1,.9,0);car.add(cab);const wheels=[];
  for(const[wx,wz]of[[-.55,.45],[.55,.45],[-.55,-.45],[.55,-.45]]){const wh=new THREE.Mesh(new THREE.CylinderGeometry(.22,.22,.15,16),std('#2b2b3a',.6));wh.rotation.x=Math.PI/2;wh.position.set(wx,.22,wz);car.add(wh);wheels.push(wh)}
  car.position.set(-30,0,2.4);car.visible=!!deco.extra;scene.add(car);
  const tiko=makeTiko();tiko.scale.setScalar(1.25);tiko.position.set(-3.6,.2,-1.9);tiko.rotation.y=-.35;scene.add(tiko);let tikoHop=false;
  const smokes=bld.filter(b=>b.d.fixed);let smokeT=0;
  const camP={tx:0,ty:2.6,tz:-3.5,yaw:0,pitch:.3,R:20};let userYaw=0,wide={},busy=false;
  function fit(){wide={tx:0,ty:2.6,tz:-3.5,yaw:0,pitch:.3,R:clamp(fitDist(cam,31,12,W,H*.9),15,70)};if(!busy)Object.assign(camP,wide)}
  function camTo(p,dur){const a={...camP};tween(dur,e=>{for(const k in p)camP[k]=a[k]+(p[k]-a[k])*e},{ease:E.inOut})}
  function puff(p){for(let i=0;i<7;i++){const m=new THREE.Mesh(geo.puff||(geo.puff=new THREE.SphereGeometry(.14,10,8)),new THREE.MeshBasicMaterial({color:'#fff7fb',transparent:true,opacity:.9}));
    m.position.copy(p);addPart(m,scene,{v:new THREE.Vector3(rnd(2.5),Math.random()*1.5,Math.random()*1.5),drag:2,life:.6,fade:true,grow:1.6})}}
  function confetti(p,n){const cols=['#ff6fa8','#ffd166','#7fe3ff','#b8f27a','#c3a6ff','#ff9a3c','#ffffff'];for(let i=0;i<n;i++){
    const m=new THREE.Mesh(geo.conf||(geo.conf=new THREE.PlaneGeometry(.16,.26)),new THREE.MeshBasicMaterial({color:cols[i%cols.length],side:THREE.DoubleSide,transparent:true}));
    m.position.copy(p).add(new THREE.Vector3(rnd(2),rnd(.5),rnd(1)));addPart(m,scene,{v:new THREE.Vector3(rnd(9),5+Math.random()*6,rnd(5)+1.5),av:new THREE.Vector3(rnd(14),rnd(14),rnd(14)),g:7,drag:.7,life:2.6+Math.random(),fade:true})}}
  /** Rebuild a shop (or the big building) in 3D: Tiko hops over, scaffolding, bricks fly in, the colours come back, the sign flips. */
  function fixIt(spot,o2){const b=bld.find(x=>x.d.big?spot==='big':defs.indexOf(x.d)===(spot>=2?spot+1:spot)&&!x.d.big);if(!b||busy){emit('townFixDone');return}busy=true;sfx.whoosh();
    const bx=b.d.x,front=BZ+D/2+.15;
    camTo({tx:bx+.9,ty:2.2,tz:-5.2,yaw:.1,pitch:.26,R:clamp(fitDist(cam,12,8.5,W*.9,H*.85),12,45)},1.1);
    const t0={x:tiko.position.x};tikoHop=true;tween(.9,(e,k)=>{tiko.position.x=t0.x+(bx+2.4-t0.x)*e;tiko.position.z=-1.9-.6*e;tiko.position.y=.2+Math.abs(Math.sin(k*Math.PI*3))*.55;tiko.rotation.y=-.35-.4*e},{ease:E.lin,done:()=>{tikoHop=false;tiko.position.y=.2}});
    const sc=new THREE.Group();const wd=std('#c98b4a',.7);const hw=b.d.w/2+.25,hh=b.d.h+.5;
    for(const sx of[-hw,0,hw]){const p=new THREE.Mesh(new THREE.BoxGeometry(.1,hh,.1),wd);p.position.set(sx,hh/2,0);sc.add(p)}
    for(const py of[1.3,2.5,hh-.2]){const p=new THREE.Mesh(new THREE.BoxGeometry(hw*2+.3,.09,.45),wd);p.position.set(0,py,0);sc.add(p)}
    sc.position.set(bx,0,front+.25);sc.scale.y=.001;scene.add(sc);
    wait(1.0,()=>{sfx.clack();tween(.5,e=>sc.scale.y=Math.max(.001,e),{ease:E.outBack})});
    const NB=14;let got=0;b.planks.forEach(p=>wait(1.3,()=>{addPart(p,scene,{v:v3(rnd(3),3,2),av:v3(rnd(6),rnd(6),rnd(6)),g:9,life:1.2,shrink:true})}));
    for(let i=0;i<NB;i++)wait(1.45+i*.12,()=>{const br=new THREE.Mesh(geo.brick2||(geo.brick2=new THREE.BoxGeometry(.44,.22,.22)),std('#d0563b',.6));br.castShadow=true;
      const s=tiko.position.clone().add(new THREE.Vector3(0,1.4,0)),en=new THREE.Vector3(bx+rnd(b.d.w*.8),.6+Math.random()*(b.d.h-.6),front);scene.add(br);const sp=rnd(14);
      tween(.5,(e,k)=>{br.position.lerpVectors(s,en,k);br.position.y+=Math.sin(k*Math.PI)*2.6;br.rotation.set(sp*k,sp*.6*k,0)},{ease:E.lin,done:()=>{scene.remove(br);puff(en);if(i%2===0)sfx.clack();got++;b.set(got/NB)}})});
    wait(3.4,()=>{sfx.shine();let sw=false;tween(.8,(e,k)=>{b.sign.rotation.y=k*Math.PI*2;b.sign.rotation.z=-.12*(1-k);if(!sw&&k>.5){sw=true;b.signMat.map=signTex(b.d.name,b.d,false);b.signMat.needsUpdate=true}},{ease:E.inOut})});
    wait(4.0,()=>{tween(.45,(e)=>{sc.position.y=-e*1;sc.scale.y=Math.max(.001,1-e)},{ease:E.inCubic,done:()=>scene.remove(sc)})});
    wait(4.3,()=>{sfx.fanfare();shake=.12;tween(.8,(e,k)=>{const s=1+Math.sin(k*Math.PI*3)*(1-k)*.14;b.inner.scale.set(1/Math.sqrt(s),s,1/Math.sqrt(s))},{ease:E.lin});
      confetti(new THREE.Vector3(bx,b.d.h+1.2,BZ+1.5),160);for(let i=0;i<14;i++){const s=sprite(starTex,0xffe27a,.5);s.position.set(bx+rnd(3),b.d.h*.6+rnd(2),front+.6);
        addPart(s,scene,{v:new THREE.Vector3(rnd(6),2+Math.random()*4,2),g:4,life:1.3,fade:true})}
      tiko.userData.cheer=T;b.d.fixed=1;smokes.push(b);emit('townFixed')});
    wait(6.4,()=>{camTo(wide,1.4)});wait(7.9,()=>{busy=false;emit('townFixDone')})}
  /** Brighten decorations: fountain, trees, lamps, extra. */
  return{onResize:fit,start(){fit()},
    act(name,d){if(name==='fix')fixIt(d.spot,d);if(name==='drag')userYaw=clamp(userYaw-(d.dx||0)*.006,-.75,.75);if(name==='cheer')tiko.userData.cheer=T},
    update(dt,t){const yaw=camP.yaw+userYaw+Math.sin(t*.18)*.1,R=camP.R,p=camP.pitch;
      cam.position.set(camP.tx+Math.sin(yaw)*Math.cos(p)*R,camP.ty+Math.sin(p)*R,camP.tz+Math.cos(yaw)*Math.cos(p)*R);cam.lookAt(camP.tx,camP.ty,camP.tz);
      sway.forEach(g=>g.rotation.z=Math.sin(t*1.3+g.userData.ph)*.05);
      clouds.forEach(c=>{c.position.x+=dt*c.userData.sp;if(c.position.x>40)c.position.x=-40});
      if(car.visible){car.position.x+=dt*4.5;if(car.position.x>40)car.position.x=-40;wheels.forEach(wh=>wh.rotation.y-=dt*12)}
      if(!tikoHop){const ch=tiko.userData.cheer&&T-tiko.userData.cheer<2.2;tiko.position.y=.2+(ch?Math.abs(Math.sin(t*9))*.45:Math.abs(Math.sin(t*2.2))*.06);tiko.scale.y=1.25*(1+Math.sin(t*4.4)*.03)}
      if(smokes.length&&(smokeT-=dt)<0){smokeT=.5;const b=smokes[(Math.random()*smokes.length)|0];const wp=new THREE.Vector3();b.chim.getWorldPosition(wp);wp.y+=.75;
        const m=new THREE.Mesh(geo.puff2||(geo.puff2=new THREE.SphereGeometry(.22,12,10)),new THREE.MeshBasicMaterial({color:'#ffffff',transparent:true,opacity:.7}));m.position.copy(wp);
        addPart(m,scene,{v:new THREE.Vector3(.35+rnd(.2),.9,rnd(.2)),life:2.6,fade:true,op:.7,grow:2.2})}}}};

/* ---------- spin wheel ---------- */
SCENES.spin=function(w,o){const scene=w.scene,cam=w.cam;cam.fov=38;cam.updateProjectionMatrix();addLights(scene,{hemi:.9,sun:.8,pos:[2,5,9]});
  const SL=o.slices||['🪙25','🔨','🪙50','🎲','🪙100','🧱 1','🪙300','✨'],COL=['#ff7eb6','#6fc3ff','#ffd84a','#7ed957'];const n=SL.length,seg=Math.PI*2/n;
  const face=ctex(1024,1024,(x)=>{x.translate(512,512);for(let i=0;i<n;i++){x.beginPath();x.moveTo(0,0);x.arc(0,0,510,-Math.PI/2+i*seg-seg/2,-Math.PI/2+i*seg+seg/2);x.closePath();x.fillStyle=COL[i%4];x.fill();x.strokeStyle='#ffffff';x.lineWidth=8;x.stroke()}
    for(let i=0;i<n;i++){x.save();x.rotate(i*seg);x.font='84px "Lilita One",sans-serif';x.textAlign='center';x.textBaseline='middle';x.fillStyle='#1b2140';x.fillText(SL[i],0,-330);x.restore()}});
  const wheel=new THREE.Group();scene.add(wheel);const disc=new THREE.Mesh(new THREE.CylinderGeometry(2.45,2.45,.3,64),std('#ffffff',.4));disc.rotation.x=Math.PI/2;wheel.add(disc);
  const fm=new THREE.Mesh(new THREE.CircleGeometry(2.4,64),new THREE.MeshStandardMaterial({map:face,roughness:.4}));fm.position.z=.16;wheel.add(fm);
  const hub=new THREE.Mesh(new THREE.CylinderGeometry(.42,.42,.3,32),std('#ffc93c',.25,.5));hub.rotation.x=Math.PI/2;hub.position.z=.25;wheel.add(hub);const hs=starMesh(.3,.13,'#ff6fa8');hs.position.z=.45;wheel.add(hs);
  const rim=new THREE.Mesh(new THREE.TorusGeometry(2.55,.16,14,80),std('#ffc93c',.25,.5));rim.position.z=.1;scene.add(rim);const bulbs=[];
  for(let i=0;i<20;i++){const a=i/20*Math.PI*2;const b=new THREE.Mesh(new THREE.SphereGeometry(.1,10,8),new THREE.MeshStandardMaterial({color:'#fff8d0',emissive:'#ffd84a',emissiveIntensity:1}));b.position.set(Math.cos(a)*2.55,Math.sin(a)*2.55,.24);scene.add(b);bulbs.push(b)}
  const stand=new THREE.Mesh(new THREE.BoxGeometry(1,2.4,.5),std('#2453d6',.5));stand.position.set(0,-2.9,-.4);scene.add(stand);const base2=new THREE.Mesh(new THREE.BoxGeometry(3.2,.4,1.2),std('#ffc93c',.3,.4));base2.position.set(0,-4.1,-.3);scene.add(base2);
  const ptr=new THREE.Group();const pc=new THREE.Mesh(new THREE.ConeGeometry(.3,.7,20),std('#e5333d',.35));pc.rotation.z=Math.PI;pc.position.y=-.3;ptr.add(pc);ptr.position.set(0,2.95,.45);scene.add(rich(ptr));
  const tiko=makeTiko();tiko.scale.setScalar(1.3);tiko.position.set(-4.3,-3.6,.5);tiko.rotation.y=.4;scene.add(tiko);
  let ang=0,kick2=0,lastIdx=0,spinning=false,cheer=0,shown=[];const ox=o.offsetX||0;
  function spin(i){if(spinning)return;spinning=true;shown.forEach(x=>scene.remove(x));shown=[];const a0=ang;const target=a0-(a0%(Math.PI*2))+Math.PI*2*5+i*seg;sfx.whoosh();
    tween(4.2,e=>{ang=a0+(target-a0)*e},{ease:E.outCubic,done:()=>{spinning=false;won(i)}})}
  function won(i){sfx.fanfare();cheer=T;const t=textMesh('YOU WON '+SL[i]+'!',{h:.9});t.position.set(0,0,2.2);t.scale.setScalar(.01);scene.add(t);tween(.45,e=>t.scale.setScalar(Math.max(.01,e)),{ease:E.outBack});
    const r=rays(6,0xffe27a);r.position.set(0,2.2,.6);scene.add(r);shown=[t,r];confettiAt(v3(0,2,1.5),80,4);starsAt(v3(0,2.2,.5),16);emit('spinDone',{i});wait(2.6,()=>{scene.remove(t);scene.remove(r)})}
  return{act(name,d){if(name==='spin')spin(d.i|0)},
    update(dt,t){wheel.rotation.z=ang;const idx=Math.floor((ang+seg/2)/seg);if(idx!==lastIdx){lastIdx=idx;kick2=.55;if(spinning)snd('tick')}kick2*=Math.pow(.002,dt);ptr.rotation.z=-kick2;
      bulbs.forEach((b,i)=>b.material.emissiveIntensity=((i+Math.floor(t*(spinning?14:4)))%2)?1.2:.15);
      const fd=fitDist(cam,9.4,8.2,W*.55,Math.max(120,H-90));cam.position.set(-ox,-.2,fd);cam.lookAt(-ox,-.6,0);
      tiko.position.y=-3.6+(cheer&&T-cheer<2?Math.abs(Math.sin(T*9))*.5:Math.abs(Math.sin(t*2.2))*.06)}}};

/* ---------- daily gift ---------- */
SCENES.gift=function(w,o){const scene=w.scene,cam=w.cam;cam.fov=38;cam.updateProjectionMatrix();addLights(scene,{hemi:.9,sun:.8,pos:[-3,8,7],shadow:6});
  const fl=new THREE.Mesh(new THREE.CircleGeometry(5,48),std('#5a3fd0',.8));fl.rotation.x=-Math.PI/2;fl.receiveShadow=true;scene.add(fl);
  let box,lid,tiko,opened=o.opened||false,cheer=0,beam=null,extras=[];const ox=o.offsetX||0;
  box=new THREE.Group();const pink=std('#ff6fa8',.4),gold=std('#ffd84a',.3,.3);
  const b=new THREE.Mesh(new THREE.BoxGeometry(1.8,1.4,1.8),pink);b.position.y=.7;box.add(b);for(const r of[0,1]){const s=new THREE.Mesh(new THREE.BoxGeometry(r?.3:1.84,1.42,r?1.84:.3),gold);s.position.y=.7;box.add(s)}scene.add(rich(box));
  lid=new THREE.Group();const l=new THREE.Mesh(new THREE.BoxGeometry(1.98,.4,1.98),pink);lid.add(l);for(const r of[0,1]){const s=new THREE.Mesh(new THREE.BoxGeometry(r?.32:2.0,.42,r?2.0:.32),gold);lid.add(s)}
  for(const s of[-1,1]){const bw=new THREE.Mesh(new THREE.TorusGeometry(.3,.1,10,20),gold);bw.position.set(s*.3,.45,0);bw.rotation.y=Math.PI/2;bw.rotation.x=s*.5;lid.add(bw)}const k=new THREE.Mesh(new THREE.SphereGeometry(.14,12,10),gold);k.position.y=.35;lid.add(k);
  lid.position.y=1.6;scene.add(rich(lid));if(opened)lid.visible=false;tiko=makeTiko();tiko.scale.setScalar(1.2);tiko.position.set(2.4,0,.6);tiko.rotation.y=-.5;scene.add(tiko);
  function open(d){if(opened)return;opened=true;const Bx=box,L=lid;[0,1,2].forEach(i=>wait(i*.35,()=>{snd('clack');tween(.3,(e,k2)=>{const a=Math.sin(k2*Math.PI*6)*(.05+i*.04);Bx.rotation.z=a;L.rotation.z=a;const s=1+Math.sin(k2*Math.PI)*.06*(i+1);Bx.scale.set(s,1/s,s)},{ease:E.lin})}));
    wait(1.1,()=>{sfx.blast();sfx.fanfare();shake=.12;addPart(lid,scene,{v:v3(1.5,7,1.5),av:v3(3,5,6),g:9,life:2.5});lid=null;
      beam=new THREE.Mesh(new THREE.CylinderGeometry(1.2,.75,5,32,1,true),new THREE.MeshBasicMaterial({color:'#fff2a0',transparent:true,opacity:0,blending:THREE.AdditiveBlending,side:THREE.DoubleSide,depthWrite:false}));beam.position.y=3.9;scene.add(beam);
      tween(.4,e=>beam.material.opacity=.5*e);const gl=sprite(glowTex,0xffe080,4);gl.position.y=1.8;scene.add(gl);extras.push(gl);confettiAt(v3(0,1.6,.5),120,1.5);cheer=T;
      const stack=new THREE.Group();for(let i=0;i<5;i++){const c=coinMesh();c.children[0].rotation.x=Math.PI/2;c.position.y=i*.08;stack.add(c)}stack.position.set(-.45,1.3,0);scene.add(stack);extras.push(stack);
      const card=mkCard(cardTex(d.emoji||'🎁',d.name||'Gift',false));card.position.set(.5,1.3,0);card.scale.setScalar(.6);scene.add(card);extras.push(card);
      tween(1.0,(e,k2)=>{stack.position.y=1.3+e*1.6;card.position.y=1.3+e*2.0;card.rotation.y=k2*Math.PI*2;stack.rotation.y=k2*4},{ease:E.outBack});
      wait(1.0,()=>{if(d.text){const tx=textMesh(d.text,{h:.6});tx.position.set(0,4.1,.8);tx.scale.setScalar(.01);scene.add(tx);extras.push(tx);tween(.4,e=>tx.scale.setScalar(Math.max(.01,e)),{ease:E.outBack})}emit('giftOpened')})})}
  return{act(name,d){if(name==='open')open(d)},
    update(dt,t){const fd=fitDist(cam,5.6,5.2,W*.6,Math.max(120,H-100));cam.position.set(Math.sin(t*.3)*1.2-ox,3.2,fd);cam.lookAt(-ox,1.9,0);if(beam)beam.rotation.y=t;
      tiko.position.y=cheer&&T-cheer<3?Math.abs(Math.sin(T*9))*.5:Math.abs(Math.sin(t*2.2))*.06;if(!opened){box.rotation.y=Math.sin(t*.8)*.15;const hop=Math.max(0,Math.sin(t*4))*.12*((t%2.4)<1.2?1:0);box.position.y=hop;if(lid)lid.position.y=1.6+hop}}}};

/* ---------- quiz: floating question marks and Tiko ---------- */
SCENES.quiz=function(w){const scene=w.scene,cam=w.cam;addLights(scene,{hemi:.9,sun:.8,pos:[3,6,9]});const tiko=makeTiko();tiko.scale.setScalar(1.5);tiko.rotation.y=.45;scene.add(tiko);const qs=[];let mood=null,moodT=0;
  for(let i=0;i<10;i++){const q=textMesh('?',{h:.9+Math.random()*.8,colors:['#ffffff','#d9c8ff','#9b74ff'],stroke:'#2a1470'});q.position.set(rnd(14),rnd(8),-3-Math.random()*4);q.userData.p=Math.random()*6;scene.add(q);qs.push(q)}
  return{act(name,d){mood=name==='right'?'yes':'no';moodT=T;if(name==='right'){const wp=screenToWorld(cam,d.x,d.y,1);starsAt(wp,18,0xffd84a,1.3);confettiAt(wp,60,1);flash(wp,4,0xfff0a0);popupTo('+2 🧱',wp.clone().add(v3(0,1.2,.5)),null,null,{...BRICK_TXT,h:1})}},
    update(dt,t){cam.position.set(Math.sin(t*.3)*.4,0,10);cam.lookAt(0,0,0);const xL=-(Math.tan(20*Math.PI/180)*10*W/H)+1.6;tiko.position.x=xL;
      qs.forEach(q=>{q.position.y+=dt*.3;if(q.position.y>5)q.position.y=-5;q.rotation.z=Math.sin(t+q.userData.p)*.2});
      const k=T-moodT;tiko.position.y=-2.6+(mood==='yes'&&k<2?Math.abs(Math.sin(T*9))*.6:Math.abs(Math.sin(t*2.2))*.06);tiko.rotation.y=.45+(mood==='no'&&k<1.2?Math.sin(T*25)*.35:0)}}};

/* ---------- map: floating world islands, Tiko on the current one ---------- */
SCENES.map=function(w,o){const scene=w.scene,cam=w.cam;cam.fov=42;cam.updateProjectionMatrix();addLights(scene,{hemi:.9,sun:.8,pos:[-6,12,10],shadow:30,target:[10,0,0]});
  const names=o.names||['1 Candy Land','2 Under the Sea','3 Fruit Jungle','4 Cloud Kingdom','5 Sunny Galaxy'];const tops=['#ffb3d9','#7fe3ff','#9be27a','#ffffff','#c9a8ff'];const isl=[];const open=o.open==null?1:o.open;
  function deco(i,g){if(i===0){for(const[x,z,c]of[[-1.3,-.9,'#e8457a'],[1.4,-.6,'#2a8fe0']]){const st=new THREE.Mesh(new THREE.CylinderGeometry(.05,.05,1.4,6),std('#fff'));st.position.set(x,.95,z);g.add(st);const d=new THREE.Mesh(new THREE.CylinderGeometry(.45,.45,.14,24),std(c,.3));d.rotation.x=Math.PI/2;d.position.set(x,1.7,z);g.add(d)}}
    if(i===1){const wv=new THREE.Mesh(new THREE.TorusGeometry(2.7,.25,10,48),std('#3fa9ff',.2));wv.rotation.x=Math.PI/2;wv.position.y=.1;g.add(wv);for(const[x,z]of[[-1.2,-1],[1,-1.2]]){const c=new THREE.Mesh(new THREE.ConeGeometry(.2,1,8),std('#ff7e8a',.5));c.position.set(x,.75,z);g.add(c)}}
    if(i===2){const tr=new THREE.Group();for(let k=0;k<5;k++){const s=new THREE.Mesh(new THREE.CylinderGeometry(.11,.13,.42,8),std('#9a6a3a',.7));s.position.set(k*.05,.45+k*.4,0);s.rotation.z=-.08;tr.add(s)}
      for(let k=0;k<6;k++){const l=new THREE.Mesh(new THREE.SphereGeometry(.6,12,8),std('#2fa84f',.5));l.scale.set(1.3,.12,.45);l.position.set(Math.cos(k)*.6+.2,2.4,Math.sin(k)*.6);l.rotation.y=-k;l.rotation.z=-.3;tr.add(l)}tr.position.set(-1,0,-.8);g.add(tr)}
    if(i===3){for(const[x,z]of[[-1,-.8],[1.1,-.5]]){const c=new THREE.Group();for(let k=0;k<4;k++){const m=new THREE.Mesh(new THREE.SphereGeometry(.45,12,10),std('#ffffff',.9));m.position.set(k*.45-.7,.6+Math.random()*.2,0);c.add(m)}c.position.set(x,0,z);g.add(c)}}
    if(i===4){const pl=new THREE.Mesh(new THREE.SphereGeometry(.6,24,18),std('#ffb020',.4));pl.position.set(-.8,2,-.8);g.add(pl);const rg=new THREE.Mesh(new THREE.TorusGeometry(.95,.06,8,40),std('#ff6fa8',.4));rg.position.copy(pl.position);rg.rotation.x=1.2;g.add(rg)}}
  for(let i=0;i<5;i++){const g=new THREE.Group();g.position.set(i*7.5,i%2?.6:0,0);const lockd=i>=open;const t=new THREE.Mesh(new THREE.CylinderGeometry(2.8,2.6,.5,40),std(lockd?'#b8bcc8':tops[i],.7));t.receiveShadow=true;t.castShadow=true;g.add(t);
    const rk=new THREE.Mesh(new THREE.ConeGeometry(2.6,2.8,40),std('#8a6a4a',.9));rk.rotation.x=Math.PI;rk.position.y=-1.65;g.add(rk);deco(i,g);const nm=textMesh((lockd?'🔒 ':'')+names[i],{h:.6});nm.position.set(0,3.2,0);g.add(nm);scene.add(rich(g));g.userData.ph=i;isl.push(g)}
  const stones=[];for(let k=0;k<10;k++){const s=new THREE.Mesh(new THREE.CylinderGeometry(.28,.3,.14,20),std('#9aa3b5',.6));s.castShadow=true;s.position.set(-2+k*.44,.32,.9+Math.sin(k*.9)*.9);stones.push(s)}
  const cw=clamp(o.world||0,0,4),cl=clamp(o.level==null?0:o.level,0,9);stones.forEach((s,i)=>{s.material=std(i<cl?'#ffc93c':i===cl?'#36d878':'#9aa3b5',.5,i<cl?.3:0);isl[cw].add(s)});
  const tiko=makeTiko();tiko.scale.setScalar(.55);scene.add(tiko);let camX=cw*7.5,hop=0;
  return{act(name,d){if(name==='hop')hop=T},
    update(dt,t){isl.forEach(g=>{g.position.y=(g.userData.ph%2?.6:0)+Math.sin(t*.8+g.userData.ph)*.12});const p=wpos(stones[cl]);tiko.position.copy(p).add(v3(0,.05+Math.abs(Math.sin(t*2.4))*.15,0));
      camX+=(cw*7.5-camX)*Math.min(1,dt*2);cam.position.set(camX+.5,5.2,clamp(fitDist(cam,13,7,W,H*.8),9,20));cam.lookAt(camX+.5,.2,0);tiko.rotation.y=Math.sin(t*.8)*.3}}};

/* ====================================================================== API for Kotlin */
let ready=false;
function wrap(f){return function(o){try{use(ov);const r=f(typeof o==='string'?JSON.parse(o):o);kick();return r}catch(e){console.log('FX error',e&&e.stack)}}}
window.FX={
  unit:wrap(o=>{U=Math.max(8,o.u||56);fitOv()}),
  anchors:wrap(o=>{Object.assign(A,o)}),
  backdrop:wrap(o=>setBackdrop(o.name,o.data)),
  act:wrap(o=>act(o.name,o.data)),
  clear:wrap(()=>{ov.tweens.length=0;ov.parts.forEach(p=>rm(p.obj));ov.parts.length=0;bossHide();cardsClear();winClear();shopHide();ov.scene.children.slice(OV_KEEP).forEach(rm)}),
  word:wrap(word),popup:wrap(popup),coins:wrap(coinsFx),burst:wrap(burst),goalBlast:wrap(goalBlast),
  crate:wrap(crateBreak),iceCrack:wrap(iceCrack),ice:wrap(iceBreak),chain:wrap(chainBreak),gold:wrap(goldBurst),balloon:wrap(balloonPop),chest:wrap(chestOpen),friend:wrap(friendHop),
  booster:wrap(booster),win:wrap(win),winClear:wrap(winClear),lose:wrap(lose),confetti:wrap(confetti),fireworks:wrap(fireworks),
  cards:wrap(cards),cardPick:wrap(cardPick),cardsClear:wrap(cardsClear),
  bossShow:wrap(bossShow),bossHold:wrap(o=>{bossHeld=!!o.on}),bossHit:wrap(bossHit),bossAttack:wrap(bossAttack),bossDefeat:wrap(bossDefeat),bossHide:wrap(bossHide),
  shopShow:wrap(shopShow),shopHide:wrap(shopHide),shopBuy:wrap(shopBuy),
  icons:wrap(icons),
};
resize();
Promise.all([document.fonts.load('96px "Lilita One"'),document.fonts.load('8px "Press Start 2P"')]).catch(()=>{}).then(()=>{ready=true;try{if(window.WN)WN.ready(!!renderer)}catch(e){}});
