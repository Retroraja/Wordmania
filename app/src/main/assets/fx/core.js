/* Wordnoid 3D effects: shared pieces, copied from the approved preview (design/animations/approved-3d-preview.html). */
'use strict';
let W=1,H=1,T=0,tweens=[],parts=[],shake=0;
const rnd=a=>(Math.random()-.5)*a, clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
const E={lin:k=>k,outCubic:k=>1-Math.pow(1-k,3),inCubic:k=>k*k*k,inQuad:k=>k*k,inOut:k=>k<.5?4*k*k*k:1-Math.pow(-2*k+2,3)/2,
  outBack:k=>{const c1=1.70158,c3=c1+1;return 1+c3*Math.pow(k-1,3)+c1*Math.pow(k-1,2)},
  outBounce:k=>{const n=7.5625,d=2.75;if(k<1/d)return n*k*k;if(k<2/d)return n*(k-=1.5/d)*k+.75;if(k<2.5/d)return n*(k-=2.25/d)*k+.9375;return n*(k-=2.625/d)*k+.984375}};
function tween(dur,fn,o={}){const tw={start:T+(o.delay||0),dur:Math.max(dur,1e-4),fn,ease:o.ease||E.outCubic,done:o.done};tweens.push(tw);return tw}
function wait(s,f){return tween(1e-4,()=>{},{delay:s,done:f})}
function runTweens(){const list=tweens.slice();for(const tw of list){if(T<tw.start)continue;const k=Math.min(1,(T-tw.start)/tw.dur);tw.fn(tw.ease(k),k);if(k>=1){const i=tweens.indexOf(tw);if(i>=0)tweens.splice(i,1);tw.done&&tw.done()}}}
function addPart(obj,parent,o){parent.add(obj);const p=Object.assign({v:new THREE.Vector3(),av:new THREE.Vector3(),g:0,life:1,drag:0,s0:obj.scale.x},o);p.obj=obj;p.parent=parent;p.max=p.life;parts.push(p)}
function updParts(dt){for(let i=parts.length-1;i>=0;i--){const p=parts[i];p.life-=dt;if(p.life<=0){p.parent.remove(p.obj);parts.splice(i,1);continue}
  p.v.y-=p.g*dt;if(p.drag)p.v.multiplyScalar(Math.max(0,1-p.drag*dt));p.obj.position.addScaledVector(p.v,dt);
  p.obj.rotation.x+=p.av.x*dt;p.obj.rotation.y+=p.av.y*dt;p.obj.rotation.z+=p.av.z*dt;const k=p.life/p.max;
  if(p.fade&&p.obj.material)p.obj.material.opacity=Math.min(1,k*1.6)*(p.op||1);
  if(p.shrink)p.obj.scale.setScalar(p.s0*Math.min(1,k/.35));
  if(p.grow)p.obj.scale.setScalar(p.s0+(1-k)*p.grow);
  if(p.floor!==undefined&&p.obj.position.y<p.floor){p.obj.position.y=p.floor;p.v.y*=-.35;p.v.x*=.7;p.v.z*=.7}}}
/* ---------- shared 3D pieces ---------- */
function ctex(w,h,draw){const c=document.createElement('canvas');c.width=w;c.height=h;draw(c.getContext('2d'),w,h);const t=new THREE.CanvasTexture(c);t.anisotropy=4;return t}
function rr(x,a,b,w,h,r){x.beginPath();x.moveTo(a+r,b);x.arcTo(a+w,b,a+w,b+h,r);x.arcTo(a+w,b+h,a,b+h,r);x.arcTo(a,b+h,a,b,r);x.arcTo(a,b,a+w,b,r);x.closePath()}
function rrShape(w,h,r){const s=new THREE.Shape(),x=-w/2,y=-h/2;s.moveTo(x+r,y);s.lineTo(x+w-r,y);s.quadraticCurveTo(x+w,y,x+w,y+r);s.lineTo(x+w,y+h-r);
  s.quadraticCurveTo(x+w,y+h,x+w-r,y+h);s.lineTo(x+r,y+h);s.quadraticCurveTo(x,y+h,x,y+h-r);s.lineTo(x,y+r);s.quadraticCurveTo(x,y,x+r,y);return s}
function tileGeo(w,h,d,r,b){const g=new THREE.ExtrudeGeometry(rrShape(w-2*b,h-2*b,Math.max(.01,r-b)),{depth:d,bevelEnabled:true,bevelThickness:b,bevelSize:b,bevelSegments:4,curveSegments:8});g.translate(0,0,-d/2);return g}
const geo={};
const TILE_COLORS=['#c3d63a','#e8457a','#22aeb0','#8e3bc4','#f2861e','#2a8fe0','#e2492f','#4757c9','#ff6fa8','#39b86a'];
const letterTex={};
function getLetterTex(ch){return letterTex[ch]||(letterTex[ch]=ctex(128,128,(x,w,h)=>{
  const g=x.createLinearGradient(0,6,0,70);g.addColorStop(0,'rgba(255,255,255,.42)');g.addColorStop(1,'rgba(255,255,255,0)');x.fillStyle=g;rr(x,10,6,108,60,26);x.fill();
  x.font='84px "Lilita One","Arial Black",sans-serif';x.textAlign='center';x.textBaseline='middle';x.fillStyle='rgba(0,0,0,.3)';x.fillText(ch,64,73);x.fillStyle='#fff';x.fillText(ch,64,67)}))}
function makeTile(ch,color){const g=new THREE.Group();const mat=new THREE.MeshStandardMaterial({color,roughness:.3,metalness:.05});
  const m=new THREE.Mesh(geo.tile||(geo.tile=tileGeo(1,1,.34,.17,.06)),mat);m.castShadow=true;g.add(m);
  const p=new THREE.Mesh(geo.lp||(geo.lp=new THREE.PlaneGeometry(.92,.92)),new THREE.MeshBasicMaterial({map:getLetterTex(ch),transparent:true}));p.position.z=.234;g.add(p);
  g.userData.mat=mat;g.userData.face=p;return g}
function addLights(scene,o={}){scene.add(new THREE.HemisphereLight(0xffffff,o.ground||0x8a7fd0,o.hemi==null?.85:o.hemi));
  const d=new THREE.DirectionalLight(0xffffff,o.sun==null?.8:o.sun);d.position.set(...(o.pos||[4,9,7]));
  if(o.shadow){d.castShadow=true;d.shadow.mapSize.set(1024,1024);const s=o.shadow;Object.assign(d.shadow.camera,{left:-s,right:s,top:s,bottom:-s,near:.5,far:80});d.shadow.bias=-.0006;if(o.target)d.target.position.set(...o.target);scene.add(d.target)}
  scene.add(d);return d}
const glowTex=ctex(64,64,x=>{const g=x.createRadialGradient(32,32,0,32,32,32);g.addColorStop(0,'rgba(255,255,255,1)');g.addColorStop(.35,'rgba(255,240,180,.8)');g.addColorStop(1,'rgba(255,200,80,0)');x.fillStyle=g;x.fillRect(0,0,64,64)});
const starTex=ctex(64,64,x=>{x.fillStyle='#fff';x.translate(32,32);x.beginPath();for(let i=0;i<10;i++){const r=i%2?11:30,a=i*Math.PI/5-Math.PI/2;x.lineTo(Math.cos(a)*r,Math.sin(a)*r)}x.closePath();x.fill()});
function sprite(tex,color,size,add=true){const s=new THREE.Sprite(new THREE.SpriteMaterial({map:tex,color,transparent:true,depthWrite:false,blending:add?THREE.AdditiveBlending:THREE.NormalBlending}));s.scale.setScalar(size);return s}
let coinMats=null;
function coinMesh(){if(!coinMats){const face=ctex(128,128,x=>{const g=x.createRadialGradient(48,42,6,64,64,64);g.addColorStop(0,'#fff6b0');g.addColorStop(.55,'#ffc93c');g.addColorStop(1,'#d18a00');
    x.fillStyle=g;x.beginPath();x.arc(64,64,64,0,7);x.fill();x.strokeStyle='#b8730a';x.lineWidth=7;x.beginPath();x.arc(64,64,49,0,7);x.stroke();
    x.font='62px "Lilita One",sans-serif';x.textAlign='center';x.textBaseline='middle';x.fillStyle='#b8730a';x.fillText('W',64,71);x.fillStyle='#fff3a6';x.fillText('W',64,66)});
  const edge=new THREE.MeshStandardMaterial({color:'#e8a817',metalness:.45,roughness:.3,emissive:'#4a3000'});const f=new THREE.MeshStandardMaterial({map:face,metalness:.25,roughness:.35,emissive:'#2a1a00'});
  coinMats=[edge,f,f];geo.coin=new THREE.CylinderGeometry(.3,.3,.07,32);geo.coin.rotateX(Math.PI/2)}
  const g=new THREE.Group();const m=new THREE.Mesh(geo.coin,coinMats);g.add(m);g.userData.spin=m;return g}
function fitDist(cam,wU,hU,wPx,hPx){const vF=2*Math.tan(cam.fov*Math.PI/360);return Math.max(hU*H/(Math.max(40,hPx)*vF),wU*W/(Math.max(40,wPx)*vF*cam.aspect))}
function screenToWorld(cam,px,py,z){const v=new THREE.Vector3(px/W*2-1,-(py/H)*2+1,.5).unproject(cam);const dir=v.sub(cam.position).normalize();const d=(z-cam.position.z)/dir.z;return cam.position.clone().add(dir.multiplyScalar(d))}
function std(color,rough=.5,metal=0){return new THREE.MeshStandardMaterial({color,roughness:rough,metalness:metal})}
function signTex(text,d,broken){return ctex(512,112,(x,w,h)=>{x.fillStyle=broken?'#d9dbe0':'#fff8ec';rr(x,6,6,w-12,h-12,24);x.fill();x.lineWidth=8;x.strokeStyle=broken?'#8a8f9c':d.roof;x.stroke();
  x.font=`${text.length>9?50:64}px "Lilita One",sans-serif`;x.textAlign='center';x.textBaseline='middle';x.fillStyle=broken?'#6b6f7a':d.roof;
  if(!broken){x.fillText(text,w/2,h/2+4);return}
  const cw=x.measureText(text).width/text.length;const sx=w/2-cw*text.length/2+cw/2;[...text].forEach((ch,i)=>{x.save();x.translate(sx+i*cw,h/2+4+(i%2?7:-6));x.rotate((i%3-1)*.24);x.fillText(ch,0,0);x.restore()})})}
function swirlTex(c1){return ctex(128,128,x=>{x.fillStyle='#fff';x.beginPath();x.arc(64,64,64,0,7);x.fill();x.strokeStyle=c1;x.lineWidth=13;x.lineCap='round';x.beginPath();
  for(let a=0;a<6*Math.PI;a+=.1){const r=a*3.1;x.lineTo(64+Math.cos(a)*r,64+Math.sin(a)*r)}x.stroke()})}
function makeTiko(){const g=new THREE.Group(),blue=std('#3fa9ff',.32),white=std('#ffffff',.2),black=std('#111111',.2),pink=std('#ff8fb8',.5),dark=std('#1f6fd0',.4);
  const body=new THREE.Mesh(geo.tikoB||(geo.tikoB=tileGeo(1,1,.46,.24,.09)),blue);body.position.y=.7;g.add(body);
  const shine=new THREE.Mesh(geo.tikoS||(geo.tikoS=new THREE.PlaneGeometry(.78,.2)),new THREE.MeshBasicMaterial({color:'#ffffff',transparent:true,opacity:.22}));shine.position.set(0,1.06,.33);g.add(shine);
  for(const s of[-1,1]){const e=new THREE.Mesh(new THREE.SphereGeometry(.15,20,16),white);e.position.set(s*.2,.82,.31);e.scale.set(1,1.15,.5);g.add(e);
    const p=new THREE.Mesh(new THREE.SphereGeometry(.075,16,12),black);p.position.set(s*.19,.8,.38);g.add(p);
    const hl=new THREE.Mesh(new THREE.SphereGeometry(.025,8,6),white);hl.position.set(s*.19+.025,.83,.45);g.add(hl);
    const ch=new THREE.Mesh(new THREE.SphereGeometry(.07,12,10),pink);ch.position.set(s*.34,.6,.32);ch.scale.z=.3;g.add(ch);
    const arm=new THREE.Group();arm.position.set(s*.55,.78,0);arm.rotation.z=s*.5;g.add(arm);
    const up=new THREE.Mesh(new THREE.CylinderGeometry(.065,.065,.32,10),blue);up.position.y=-.16;arm.add(up);
    const hand=new THREE.Mesh(new THREE.SphereGeometry(.11,14,10),white);hand.position.y=-.34;arm.add(hand);
    const leg=new THREE.Mesh(new THREE.CylinderGeometry(.08,.08,.22,10),dark);leg.position.set(s*.22,.15,0);g.add(leg);
    const f=new THREE.Mesh(new THREE.SphereGeometry(.15,16,12),dark);f.position.set(s*.22,.05,.06);f.scale.set(1,.55,1.35);g.add(f);
    g.userData[s<0?'armL':'armR']=arm}
  const smile=new THREE.Mesh(new THREE.TorusGeometry(.11,.025,8,20,Math.PI),black);smile.rotation.z=Math.PI;smile.position.set(0,.66,.33);g.add(smile);
  const belt=new THREE.Mesh(new THREE.BoxGeometry(1.04,.09,.54),std('#7a4a2a',.6));belt.position.y=.34;g.add(belt);
  const buckle=new THREE.Mesh(new THREE.BoxGeometry(.14,.12,.04),std('#ffc93c',.3,.5));buckle.position.set(0,.34,.28);g.add(buckle);
  const hm=std('#ffb020',.35);const hat=new THREE.Mesh(new THREE.SphereGeometry(.4,24,12,0,Math.PI*2,0,Math.PI/2),hm);hat.position.y=1.16;g.add(hat);
  const brim=new THREE.Mesh(new THREE.CylinderGeometry(.48,.48,.06,24),hm);brim.position.set(0,1.17,.08);g.add(brim);
  const ridge=new THREE.Mesh(new THREE.BoxGeometry(.08,.08,.8),hm);ridge.position.y=1.5;ridge.scale.y=.6;g.add(ridge);
  g.traverse(o=>{if(o.isMesh&&o!==shine)o.castShadow=true});return g}
let cs=null,cc=null;
const v3=(x=0,y=0,z=0)=>new THREE.Vector3(x,y,z);
function textMesh(text,o={}){const fs=96,c=document.createElement('canvas'),x=c.getContext('2d');const FONT=`${fs}px "Lilita One","Arial Black",sans-serif`;x.font=FONT;
  const tw=Math.ceil(x.measureText(text).width)+60;c.width=tw;c.height=fs*1.5;const y=c.getContext('2d');y.font=FONT;y.textAlign='center';y.textBaseline='middle';y.lineJoin='round';
  y.lineWidth=fs*.17;y.strokeStyle=o.stroke||'#5a2400';y.strokeText(text,tw/2,c.height/2+5);const g=y.createLinearGradient(0,c.height*.2,0,c.height*.85);
  const cols=o.colors||['#fffbd0','#ffd84a','#f39200'];cols.forEach((cc2,i)=>g.addColorStop(i/(cols.length-1),cc2));y.fillStyle=g;y.fillText(text,tw/2,c.height/2+5);
  const h=o.h||.8;const m=new THREE.Mesh(new THREE.PlaneGeometry(h*tw/c.height,h),new THREE.MeshBasicMaterial({map:new THREE.CanvasTexture(c),transparent:true,depthWrite:false}));m.renderOrder=5;return m}
const GREEN_TXT={colors:['#e6ffe9','#5cf09a','#169c4c'],stroke:'#0b4a22'};
function chunks(wp,color,n=10,pow=1,o={}){const m=new THREE.MeshStandardMaterial({color,roughness:o.rough==null?.3:o.rough,metalness:o.metal||0,emissive:o.emissive||'#000000'});
  for(let k=0;k<n;k++){const c=new THREE.Mesh(geo.chunk||(geo.chunk=new THREE.BoxGeometry(1,1,1)),m);c.scale.setScalar((.14+Math.random()*.2)*(o.size||1));c.position.copy(wp).add(v3(rnd(.5),rnd(.5),.25));
    const d=v3(rnd(1),rnd(1)+.15,Math.random()*.9+.25).normalize();addPart(c,cs,{v:d.multiplyScalar((4+Math.random()*5)*pow),av:v3(rnd(14),rnd(14),rnd(14)),g:13,life:1+Math.random()*.4,shrink:true})}}
function flash(wp,s=3,col=0xffffff){const f=sprite(glowTex,col,.2);f.position.copy(wp);f.position.z+=.5;cs.add(f);tween(.32,e=>{f.scale.setScalar(.2+e*s);f.material.opacity=1-e},{done:()=>cs.remove(f)})}
function starsAt(wp,n=8,col=0xffe27a,pow=1){for(let i=0;i<n;i++){const s=sprite(starTex,col,.35);s.position.copy(wp);s.position.z+=.5;addPart(s,cs,{v:v3(rnd(7)*pow,(2+Math.random()*5)*pow,rnd(2)+1),g:7,life:1,fade:true})}}
function confettiAt(p,n,spread=2){const cols=['#ff6fa8','#ffd166','#7fe3ff','#b8f27a','#c3a6ff','#ff9a3c','#ffffff'];for(let i=0;i<n;i++){
  const m=new THREE.Mesh(geo.conf||(geo.conf=new THREE.PlaneGeometry(.16,.26)),new THREE.MeshBasicMaterial({color:cols[i%cols.length],side:THREE.DoubleSide,transparent:true}));
  m.position.copy(p).add(v3(rnd(spread),rnd(.5),rnd(1)));addPart(m,cs,{v:v3(rnd(9),4+Math.random()*6,rnd(3)+2),av:v3(rnd(14),rnd(14),rnd(14)),g:7,drag:.7,life:2.4+Math.random(),fade:true})}}
function squash(t){tween(.3,(e,k)=>{const s=Math.sin(k*Math.PI)*(1-k*.4);t.scale.set(1+.14*s,1-.2*s,1)},{ease:E.lin,done:()=>t.scale.set(1,1,1)})}
function wpos(o){o.updateMatrixWorld(true);return o.getWorldPosition(v3())}
function burstTile(B,c,r,o={}){const t=B.at(c,r);if(!t)return null;const wp=wpos(t);t.parent.remove(t);B.set(c,r,null);chunks(wp,t.userData.mat.color,o.n||10,o.pow||1);flash(wp,2.4);
  if(!o.quiet)sfx.blast();shake=Math.max(shake,o.shake||.06);return wp}
function refill(B,delay=0,o={}){for(let c=0;c<B.cols;c++){const col=[];for(let r=B.rows-1;r>=0;r--){const t=B.at(c,r);if(t)col.push(t)}const miss=B.rows-col.length;if(!miss)continue;
  for(let i=0;i<miss;i++){const t=makeTile(rLetter(),rColor());t.position.copy(B.pos(c,0));t.position.y+=B.sp*(i+1)+.4;B.g.add(t);col.push(t);if(o.pop){const r=miss-1-i;t.position.y=B.pos(c,r).y;t.scale.setScalar(.01);tween(.4,e=>t.scale.setScalar(Math.max(.01,e)),{delay:delay+.25+i*.06,ease:E.outBack,done:()=>sfx.clack()})}}
  col.forEach((t,i)=>{const r=B.rows-1-i;B.set(c,r,t);const to=B.pos(c,r).y,from=t.position.y;if(Math.abs(from-to)<.01)return;
    tween(.55,e=>t.position.y=from+(to-from)*e,{delay:delay+(B.rows-1-r)*.03+c*.02,ease:E.outBounce,done:()=>{squash(t);if(Math.random()<.4)sfx.clack()}})})}}
function flipLetter(t,ch,color,delay=0){let sw=false;tween(.55,(e,k)=>{t.rotation.x=e*Math.PI*2;t.position.z=Math.sin(k*Math.PI)*.6;
  if(!sw&&k>.5){sw=true;const f=t.userData.face.material;f.map=getLetterTex(ch);f.needsUpdate=true;if(color)t.userData.mat.color.set(color)}},{delay,ease:E.inOut,done:()=>{t.rotation.x=0;squash(t)}})}
function glowPulse(t,col=[.5,.45,.1],dur=.8,times=2){const m=t.userData.mat;tween(dur,(e,k)=>{const s=Math.abs(Math.sin(k*Math.PI*times));m.emissive.setRGB(col[0]*s,col[1]*s,col[2]*s)},{ease:E.lin,done:()=>m.emissive.setRGB(0,0,0)})}
let coins=120,moves=12;
function starShape(r1,r2,n=5){const s=new THREE.Shape();for(let i=0;i<n*2;i++){const r=i%2?r2:r1,a=i*Math.PI/n+Math.PI/2;const px=Math.cos(a)*r,py=Math.sin(a)*r;i?s.lineTo(px,py):s.moveTo(px,py)}s.closePath();return s}
function starMesh(r1,r2,col='#ffd84a'){const g=new THREE.ExtrudeGeometry(starShape(r1,r2),{depth:.18,bevelEnabled:true,bevelThickness:.06,bevelSize:.05,bevelSegments:3});g.center();
  const m=new THREE.Mesh(g,new THREE.MeshStandardMaterial({color:col,metalness:.4,roughness:.25,emissive:'#6a4a00'}));m.castShadow=true;return m}
function rich(m){m.traverse(o=>{if(o.isMesh){o.castShadow=true}});return m}

/* ---- shared props ---- */
let woodT=null;const wood=()=>woodT||(woodT=ctex(128,128,(x,w,h)=>{x.fillStyle='#c98b4a';x.fillRect(0,0,w,h);for(let i=0;i<4;i++){x.fillStyle=i%2?'#b87a3c':'#d39a5a';x.fillRect(8,8+i*28,w-16,26)}
  x.strokeStyle='#7a4a1e';x.lineWidth=10;x.strokeRect(5,5,w-10,h-10);x.beginPath();x.moveTo(12,12);x.lineTo(w-12,h-12);x.stroke()}));
function crate(){const m=new THREE.Mesh(geo.crate||(geo.crate=new THREE.BoxGeometry(.98,.98,.7)),new THREE.MeshStandardMaterial({map:wood(),roughness:.7}));m.castShadow=true;return m}
function chest(){const g=new THREE.Group(),wm=new THREE.MeshStandardMaterial({map:wood(),roughness:.6}),gm=std('#ffc93c',.25,.5);
  const b=new THREE.Mesh(new THREE.BoxGeometry(.9,.55,.6),wm);g.add(b);const band=new THREE.Mesh(new THREE.BoxGeometry(.94,.1,.64),gm);band.position.y=.1;g.add(band);
  const piv=new THREE.Group();piv.position.set(0,.275,-.3);g.add(piv);const lid=new THREE.Mesh(new THREE.BoxGeometry(.94,.22,.62),wm);lid.position.set(0,.11,.31);piv.add(lid);
  const lb=new THREE.Mesh(new THREE.BoxGeometry(.98,.08,.66),gm);lb.position.set(0,.2,.31);piv.add(lb);const lk=new THREE.Mesh(new THREE.BoxGeometry(.16,.2,.06),gm);lk.position.set(0,.02,.62);piv.add(lk);
  g.userData.lid=piv;return rich(g)}
function openChest(wp,prizeCoins=3){const ch=chest();ch.position.copy(wp).setZ(.4);ch.scale.setScalar(.01);cs.add(ch);sfx.whoosh();
  tween(.5,e=>{ch.scale.setScalar(Math.max(.01,e*1.3));ch.position.z=.4+e*1.2;ch.rotation.x=.35*e},{ease:E.outBack});
  wait(.65,()=>{sfx.shine();tween(.4,e=>ch.userData.lid.rotation.x=-1.9*e,{ease:E.outBack});
    const beam=new THREE.Mesh(new THREE.CylinderGeometry(.15,.75,2.6,24,1,true),new THREE.MeshBasicMaterial({color:'#fff2a0',transparent:true,opacity:0,blending:THREE.AdditiveBlending,side:THREE.DoubleSide,depthWrite:false}));
    beam.position.copy(ch.position).add(v3(0,1.5,0));cs.add(beam);tween(.4,e=>beam.material.opacity=.55*e);wait(1.4,()=>tween(.4,e=>beam.material.opacity=.55*(1-e),{done:()=>cs.remove(beam)}));
    const gl=sprite(glowTex,0xffe080,2.5);gl.position.copy(ch.position).add(v3(0,.5,.3));cs.add(gl);wait(1.6,()=>cs.remove(gl));
    for(let i=0;i<3;i++){const gem=new THREE.Mesh(new THREE.OctahedronGeometry(.2),new THREE.MeshStandardMaterial({color:['#3fd8ff','#ff6fa8','#7ed957'][i],metalness:.3,roughness:.1,emissive:'#222'}));
      gem.position.copy(ch.position).add(v3(0,.3,0));cs.add(gem);addPart(gem,cs,{v:v3((i-1)*1.5,4,1),av:v3(0,6,0),g:5,life:1.6})}
    coinBurstTo(ch.position.clone().add(v3(0,.5,0)),prizeCoins,()=>setCoins(coins+10));starsAt(ch.position,10)});
  wait(2.4,()=>tween(.4,e=>ch.scale.setScalar(Math.max(.01,1.3*(1-e))),{done:()=>cs.remove(ch)}))}
function mkHammer(){const g=new THREE.Group();const h=new THREE.Mesh(new THREE.CylinderGeometry(.07,.08,1.5,12),std('#a0622d',.6));h.position.y=.75;g.add(h);
  const hd=new THREE.Mesh(new THREE.BoxGeometry(.78,.36,.36),std('#8d98ad',.3,.5));hd.position.y=1.5;g.add(hd);const bd=new THREE.Mesh(new THREE.BoxGeometry(.2,.4,.4),std('#ffc93c',.3,.4));bd.position.y=1.5;g.add(bd);return rich(g)}
function mkAxe(){const g=new THREE.Group();const h=new THREE.Mesh(new THREE.CylinderGeometry(.06,.07,1.4,12),std('#a0622d',.6));g.add(h);
  const s=new THREE.Shape();s.moveTo(0,-.25);s.quadraticCurveTo(.35,-.55,.62,-.5);s.quadraticCurveTo(.5,0,.62,.5);s.quadraticCurveTo(.35,.55,0,.25);s.closePath();
  const bg=new THREE.ExtrudeGeometry(s,{depth:.06,bevelEnabled:true,bevelThickness:.02,bevelSize:.02,bevelSegments:2});bg.translate(0,0,-.03);
  const b=new THREE.Mesh(bg,std('#c9d2e0',.25,.6));b.position.set(.04,.45,0);g.add(b);return rich(g)}
function mkMagnet(){const g=new THREE.Group();const u=new THREE.Mesh(new THREE.TorusGeometry(.5,.17,16,32,Math.PI),std('#e5333d',.35));g.add(u);
  for(const s of[-1,1]){const tip=new THREE.Mesh(new THREE.BoxGeometry(.36,.32,.36),std('#d9e0ea',.25,.6));tip.position.set(s*.5,-.14,0);g.add(tip)}return rich(g)}
function diceTex(n){return ctex(128,128,(x,w,h)=>{x.fillStyle='#fff';rr(x,4,4,120,120,22);x.fill();x.fillStyle='#e5333d';const P={1:[[64,64]],2:[[36,36],[92,92]],3:[[32,32],[64,64],[96,96]],4:[[36,36],[92,36],[36,92],[92,92]],5:[[34,34],[94,34],[64,64],[34,94],[94,94]],6:[[36,30],[92,30],[36,64],[92,64],[36,98],[92,98]]}[n];P.forEach(([a,b])=>{x.beginPath();x.arc(a,b,12,0,7);x.fill()})})}
let diceMats=null;function mkDice(){diceMats=diceMats||[1,6,2,5,3,4].map(n=>new THREE.MeshStandardMaterial({map:diceTex(n),roughness:.3}));const m=new THREE.Mesh(new THREE.BoxGeometry(.7,.7,.7),diceMats);m.castShadow=true;return m}
function cardTex(emoji,name,rare){return ctex(256,360,(x,w,h)=>{const g=x.createLinearGradient(0,0,0,h);g.addColorStop(0,rare?'#ffe680':'#bfe3ff');g.addColorStop(1,rare?'#ff9a3c':'#5a8cff');x.fillStyle=g;rr(x,0,0,w,h,26);x.fill();
  x.lineWidth=12;x.strokeStyle=rare?'#fff3b0':'#ffffff';rr(x,8,8,w-16,h-16,20);x.stroke();x.font='130px sans-serif';x.textAlign='center';x.textBaseline='middle';x.fillText(emoji,w/2,h*.42);
  x.font='40px "Lilita One",sans-serif';x.fillStyle='#1b2140';x.fillText(name,w/2,h*.78);if(rare){x.font='26px "Lilita One",sans-serif';x.fillStyle='#8a3b00';x.fillText('★ RARE ★',w/2,h*.9)}})}
let backT=null;const cardBack=()=>backT||(backT=ctex(256,360,(x,w,h)=>{const g=x.createLinearGradient(0,0,w,h);g.addColorStop(0,'#9b74ff');g.addColorStop(1,'#4b25c4');x.fillStyle=g;rr(x,0,0,w,h,26);x.fill();
  x.lineWidth=12;x.strokeStyle='#ffc93c';rr(x,8,8,w-16,h-16,20);x.stroke();x.font='180px "Lilita One",sans-serif';x.textAlign='center';x.textBaseline='middle';x.fillStyle='#ffd84a';x.fillText('?',w/2,h/2+10)}));
function mkCard(front){const edge=std('#ffffff',.4);const m=new THREE.Mesh(new THREE.BoxGeometry(1.1,1.55,.05),[edge,edge,edge,edge,new THREE.MeshStandardMaterial({map:front,roughness:.35}),new THREE.MeshStandardMaterial({map:cardBack(),roughness:.35})]);m.castShadow=true;return m}
const raysTex=ctex(256,256,x=>{x.translate(128,128);for(let i=0;i<16;i++){x.rotate(Math.PI/8);x.fillStyle=i%2?'rgba(255,230,120,.55)':'rgba(255,255,255,.25)';x.beginPath();x.moveTo(0,0);x.lineTo(-18,-128);x.lineTo(18,-128);x.fill()}
  x.globalCompositeOperation='destination-in';const g=x.createRadialGradient(0,0,10,0,0,128);g.addColorStop(0,'#fff');g.addColorStop(1,'rgba(255,255,255,0)');x.fillStyle=g;x.fillRect(-128,-128,256,256)});
function rays(size,col=0xffffff){const m=new THREE.Mesh(new THREE.PlaneGeometry(size,size),new THREE.MeshBasicMaterial({map:raysTex,color:col,transparent:true,depthWrite:false,blending:THREE.AdditiveBlending}));return m}
function recolor(g,fromHex,to){const m=std(to,.32);g.traverse(o=>{if(o.isMesh&&o.material.color&&o.material.color.getHex()===fromHex)o.material=m})}
function makeBoss(){const g=new THREE.Group(),bod=new THREE.Group();g.add(bod);
  const skin=new THREE.MeshStandardMaterial({color:'#8a4dff',roughness:.45}),belly=std('#c9adff',.6),white=std('#ffffff',.25),black=std('#15102a',.3),horn=std('#ffb020',.35,.1),dark=std('#3a1478',.5);
  const body=new THREE.Mesh(new THREE.SphereGeometry(1.7,40,30),skin);body.scale.set(1.15,1.05,.8);body.position.y=1.9;bod.add(body);
  const bl=new THREE.Mesh(new THREE.SphereGeometry(1.1,30,20),belly);bl.scale.set(1,.8,.45);bl.position.set(0,1.0,.95);bod.add(bl);
  const eyes=[],pupils=[],xs=[];
  for(const s of[-1,1]){const e=new THREE.Mesh(new THREE.SphereGeometry(.5,24,18),white);e.position.set(s*.62,2.55,1.0);e.scale.z=.7;bod.add(e);eyes.push(e);
    const p=new THREE.Mesh(new THREE.SphereGeometry(.22,16,12),black);p.position.set(s*.62,2.5,1.34);bod.add(p);pupils.push(p);p.userData.bx=s*.62;
    const hl=new THREE.Mesh(new THREE.SphereGeometry(.07,8,6),white);hl.position.set(.08,.1,.18);p.add(hl);
    const xg=new THREE.Group();for(const r of[1,-1]){const b=new THREE.Mesh(new THREE.BoxGeometry(.5,.1,.06),black);b.rotation.z=r*Math.PI/4;xg.add(b)}xg.position.set(s*.62,2.55,1.38);xg.visible=false;bod.add(xg);xs.push(xg);
    const br=new THREE.Mesh(new THREE.BoxGeometry(.62,.16,.16),dark);br.position.set(s*.6,3.12,1.0);br.rotation.z=s*.4;bod.add(br);
    const hn=new THREE.Mesh(new THREE.ConeGeometry(.26,.8,16),horn);hn.position.set(s*1.05,3.55,0);hn.rotation.z=-s*.45;bod.add(hn)}
  const mouth=new THREE.Group();mouth.position.set(0,1.72,1.3);mouth.rotation.x=-.25;bod.add(mouth);
  const mo=new THREE.Mesh(new THREE.CircleGeometry(.62,32,Math.PI,Math.PI),std('#3a0a2a',.8));mo.scale.y=.75;mouth.add(mo);
  const tongue=new THREE.Mesh(new THREE.CircleGeometry(.3,20,Math.PI,Math.PI),std('#ff6f8f',.6));tongue.position.set(.1,-.3,.01);tongue.scale.y=.6;mouth.add(tongue);
  for(const s of[-1,1]){const t=new THREE.Mesh(new THREE.ConeGeometry(.12,.26,4),white);t.rotation.z=Math.PI;t.position.set(s*.28,-.12,.02);mouth.add(t)}
  for(let i=0;i<5;i++){const sp=new THREE.Mesh(new THREE.ConeGeometry(.16,.5,10),skin);sp.position.set((i-2)*.32,3.75-Math.abs(i-2)*.12,-.2);sp.rotation.z=-(i-2)*.25;bod.add(sp)}
  const sh=[-1,1].map(s=>v3(s*1.8,1.55,.1));
  const segGeo=new THREE.CylinderGeometry(.27,.27,1,16);const joint=new THREE.SphereGeometry(.3,16,12);
  const arms=sh.map(()=>{const up=new THREE.Mesh(segGeo,skin),lo=new THREE.Mesh(segGeo,skin),j1=new THREE.Mesh(joint,skin),j2=new THREE.Mesh(joint,skin);return{up,lo,j1,j2}});
  const hands=[-1,1].map(s=>{const h=new THREE.Group();const palm=new THREE.Mesh(new THREE.SphereGeometry(.45,20,16),skin);palm.scale.set(1.1,.8,.9);h.add(palm);
    for(let i=0;i<3;i++){const f=new THREE.Mesh(new THREE.CylinderGeometry(.11,.11,.4,10),skin);f.position.set((i-1)*.26,-.28,.32);f.rotation.x=.5;h.add(f);
      const nail=new THREE.Mesh(new THREE.ConeGeometry(.07,.14,8),white);nail.position.set((i-1)*.26,-.52,.42);nail.rotation.x=Math.PI;h.add(nail)}return h});
  rich(g);hands.forEach(rich);arms.forEach(a=>Object.values(a).forEach(m=>m.castShadow=true));
  return{g,bod,eyes,pupils,xs,mouth,hands,arms,sh,skin}}
function placeSeg(m,a,b){const d=b.clone().sub(a),L=d.length();m.position.copy(a).add(b).multiplyScalar(.5);m.scale.set(1,L,1);m.quaternion.setFromUnitVectors(v3(0,1,0),d.normalize())}
