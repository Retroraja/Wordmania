'use strict';
/* Wordnoid 3D towns (Update 1.2): five themed streets made from Kenney CC0 models (Starter Kit City Builder) and code-made props.
 * Used by the town screen (SCENES.town), the story intro / boss scenes and the browser preview.
 * Needs three.js r128, GLTFLoader, core.js (helpers + Tiko) and models.js (window.GLB, window.GLB_TEX).
 * Spots: 0-3 shops, 4-7 decorations (per area), 8 the big building. A spot is grey and cut low until it is built. */
const T3=(()=>{
const TS=3,H0=.18;
const MODEL_NAMES=['building-small-a','building-small-b','building-small-c','building-small-d','building-garage','grass-trees','grass-trees-tall','pavement-fountain'];
const SHOP_X=[-12,-6,6,12],SHOP_Z=-3,BIG_Z=-6.9,EXTRA_X=17.6;
let MODELS=null,BASE=null,loading=null;

/* ---------------------------------------------------------------- loading */
function b64buf(b){const s=atob(b),u=new Uint8Array(s.length);for(let i=0;i<s.length;i++)u[i]=s.charCodeAt(i);return u.buffer}
function load(){if(loading)return loading;const man=new THREE.LoadingManager();man.setURLModifier(u=>/colormap\.png$/.test(u)?window.GLB_TEX:u);
  const L=new THREE.GLTFLoader(man);MODELS={};
  loading=Promise.all(MODEL_NAMES.map(n=>new Promise((res,rej)=>L.parse(b64buf(window.GLB[n]),'',g=>{MODELS[n]=g.scene;res()},rej)))).then(()=>{
    MODELS['building-small-a'].traverse(o=>{if(o.isMesh&&o.material.map&&!BASE)BASE=o.material.map.image});
    return document.fonts&&document.fonts.load?document.fonts.load('72px "Lilita One"').catch(()=>{}):null});
  return loading}

/* ---------------------------------------------------------------- colours */
function hsl(r,g,b){r/=255;g/=255;b/=255;const mx=Math.max(r,g,b),mn=Math.min(r,g,b),l=(mx+mn)/2;if(mx===mn)return[0,0,l];const d=mx-mn,s=l>.5?d/(2-mx-mn):d/(mx+mn);
  const h=mx===r?(g-b)/d+(g<b?6:0):mx===g?(b-r)/d+2:(r-g)/d+4;return[h/6,s,l]}
function rgb(h,s,l){if(!s)return[l*255,l*255,l*255];const q=l<.5?l*(1+s):l+s-l*s,p=2*l-q;const f=t=>{t=(t%1+1)%1;return(t<1/6?p+(q-p)*6*t:t<.5?q:t<2/3?p+(q-p)*(2/3-t)*6:p)*255};return[f(h+1/3),f(h),f(h-1/3)]}
const TEXC={};
/** Kenney's colour atlas, re-coloured for one building: hues pulled toward spec.h (degrees), glass and wood kept. */
function paletteTex(spec,linear){const key=(spec?[spec.h,spec.s,spec.l,spec.k].join():'0')+(linear?'L':'S');if(TEXC[key])return TEXC[key];
  const c=document.createElement('canvas');c.width=c.height=128;const x=c.getContext('2d');x.drawImage(BASE,0,0,128,128);
  if(spec){const d=x.getImageData(0,0,128,128),a=d.data;for(let i=0;i<a.length;i+=4){const px=(i>>2)&127,py=(i>>2)>>7;
      if(py<32)continue;if(px>=96&&px<112&&py>=64&&py<96)continue;if(px>=80&&py>=96)continue;
      const[h,s,l]=hsl(a[i],a[i+1],a[i+2]);if(s<.16)continue;const nh=spec.h/360+((h+.5)%1-.5)*(spec.k==null?.22:spec.k);
      const o=rgb(nh,Math.min(1,s*(spec.s||1)),Math.max(0,Math.min(1,l+(spec.l||0))));a[i]=o[0];a[i+1]=o[1];a[i+2]=o[2]}x.putImageData(d,0,0)}
  const t=new THREE.CanvasTexture(c);t.flipY=false;t.generateMipmaps=false;t.minFilter=THREE.LinearFilter;t.encoding=linear?THREE.LinearEncoding:THREE.sRGBEncoding;TEXC[key]=t;return t}
/** Adds a "grey" switch to a material: uGrey 1 = stone grey, 0 = full colour. */
const GREY_SRC=sh=>{sh.fragmentShader='uniform float uGrey;\n'+sh.fragmentShader.replace('#include <map_fragment>',
  '#include <map_fragment>\n{float gl=dot(diffuseColor.rgb,vec3(.3,.59,.11));diffuseColor.rgb=mix(diffuseColor.rgb,vec3(gl*.62+.1,gl*.62+.1,gl*.62+.125),uGrey);}')};
function greyable(m,u){m.onBeforeCompile=sh=>{sh.uniforms.uGrey=u;GREY_SRC(sh)};m.customProgramCacheKey=()=>'t3grey';m.needsUpdate=true}
function tex(lin,w,h,draw){const t=ctex(w,h,draw);t.encoding=lin?THREE.LinearEncoding:THREE.sRGBEncoding;return t}
function stripes(lin,cols,n=8,vertical=false){return tex(lin,vertical?32:256,vertical?256:32,(x,w,h)=>{for(let i=0;i<n;i++){x.fillStyle=cols[i%cols.length];if(vertical)x.fillRect(0,i*h/n,w,h/n);else x.fillRect(i*w/n,0,w/n,h)}})}

/* ---------------------------------------------------------------- small builders */
const M=(c,r=.6,m=0)=>std(c,r,m);
function mesh(g,mat,x=0,y=0,z=0,parent){const o=new THREE.Mesh(g,mat);o.position.set(x,y,z);o.castShadow=true;o.receiveShadow=true;if(parent)parent.add(o);return o}
function grp(x=0,y=0,z=0,parent){const g=new THREE.Group();g.position.set(x,y,z);if(parent)parent.add(g);return g}
function glowMat(c,lit=0){return new THREE.MeshStandardMaterial({color:c,emissive:c,emissiveIntensity:lit,roughness:.35})}
function kenney(name,lin){const m=MODELS[name].clone(true);m.scale.setScalar(TS);m.traverse(o=>{if(o.isMesh){o.castShadow=true;o.receiveShadow=true;o.material=o.material.clone();if(lin&&o.material.map)o.material.map=paletteTex(null,true)}});return m}
function bbox(o){o.updateMatrixWorld(true);return new THREE.Box3().setFromObject(o)}
function palm(x,z,s=1,parent,bananas){const g=grp(x,0,z,parent);const tm=M('#9a6a3a',.85);let y=0,ox=0;
  for(let i=0;i<7;i++){const c=mesh(new THREE.CylinderGeometry(.16-i*.012,.2-i*.012,.72,10),tm,ox,y+.36,0,g);c.rotation.z=-.05*i;ox+=.05*i;y+=.68}
  const lm=M('#2e9e48',.6),lm2=M('#3fbf5a',.6);for(let i=0;i<8;i++){const l=mesh(new THREE.SphereGeometry(1,12,8),i%2?lm:lm2,0,0,0,g);l.scale.set(1.7,.09,.42);const a=i/8*Math.PI*2;
    l.position.set(ox+Math.cos(a)*1.35,y-.3,Math.sin(a)*1.35);l.rotation.set(0,-a,-.42)}
  if(bananas)for(let i=0;i<3;i++){const b=grp(ox+Math.cos(i*2.1)*.32,y-.6,Math.sin(i*2.1)*.32,g);for(let k=0;k<5;k++){const ba=mesh(new THREE.CylinderGeometry(.05,.07,.42,8),M('#ffd23c',.4),Math.cos(k*1.25)*.12,-k*.04,Math.sin(k*1.25)*.12,b);ba.rotation.z=.5;ba.rotation.y=k*1.25}}
  else for(let i=0;i<3;i++)mesh(new THREE.SphereGeometry(.16,10,8),M('#6b3f1f',.6),ox+Math.cos(i*2)*.24,y-.36,Math.sin(i*2)*.24,g);
  g.scale.setScalar(s);g.userData.sway=Math.random()*6;return g}
function puffs(parent,x,y,z,n,s=1,col='#ffffff'){const g=grp(x,y,z,parent);const m=M(col,.95);for(let i=0;i<n;i++){const p=mesh(new THREE.SphereGeometry((.55+Math.random()*.45)*s,16,12),m,(i-(n-1)/2)*.75*s,Math.random()*.25*s,rnd(.35*s),g);p.scale.y=.75}return g}
function lampPost(x,z,face,o){const g=grp(x,H0,z);const pole=M(o.pole,.45,o.metal||0);
  if(o.bamboo){for(let i=0;i<4;i++){mesh(new THREE.CylinderGeometry(.09,.1,.8,10),M('#b9a35a',.6),0,.4+i*.8,0,g);mesh(new THREE.TorusGeometry(.1,.025,6,12),M('#7f6b2a',.6),0,.8+i*.8,0,g).rotation.x=Math.PI/2}}
  else{mesh(new THREE.CylinderGeometry(.08,.12,3.2,12),pole,0,1.6,0,g);mesh(new THREE.CylinderGeometry(.22,.26,.25,12),pole,0,.12,0,g)}
  mesh(new THREE.BoxGeometry(.07,.07,.7),pole,0,3.12,.3*face,g);const hz=.62*face;let head;const gm=glowMat(o.glow);
  if(o.head==='shell'){head=mesh(new THREE.SphereGeometry(.34,18,10,0,Math.PI*2,0,Math.PI/2),gm,0,2.82,hz,g);head.scale.set(1,.55,1);head.rotation.x=Math.PI;
    for(let i=0;i<7;i++){const r=mesh(new THREE.BoxGeometry(.03,.03,.36),M(o.trim||'#ffffff',.5),Math.sin(i*.45-1.35)*.2,2.86,hz+Math.cos(i*.45-1.35)*.05,g);r.rotation.y=i*.45-1.35}}
  else if(o.head==='lantern'){mesh(new THREE.CylinderGeometry(.02,.02,.25,6),M('#3a2a1a',.6),0,3,hz,g);head=mesh(new THREE.CylinderGeometry(.24,.24,.46,14),gm,0,2.68,hz,g);
    mesh(new THREE.CylinderGeometry(.27,.27,.07,14),M('#3a2a1a',.6),0,2.93,hz,g);mesh(new THREE.CylinderGeometry(.27,.27,.07,14),M('#3a2a1a',.6),0,2.43,hz,g)}
  else if(o.head==='orb'){head=mesh(new THREE.SphereGeometry(.3,20,14),gm,0,2.86,hz,g);const ring=mesh(new THREE.TorusGeometry(.38,.03,8,28),M('#ffc93c',.3,.6),0,2.86,hz,g);ring.rotation.x=1.2}
  else if(o.head==='planet'){head=mesh(new THREE.SphereGeometry(.3,20,14),gm,0,2.86,hz,g);const ring=mesh(new THREE.TorusGeometry(.48,.05,8,32),glowMat(o.ring||'#ffe27a'),0,2.86,hz,g);ring.rotation.set(1.3,0,.3);g.userData.ring=ring.material}
  else{head=mesh(new THREE.SphereGeometry(.26,16,12),gm,0,2.95,hz,g);mesh(new THREE.ConeGeometry(.34,.25,16),pole,0,3.2,hz,g)}
  g.userData.head=head;return g}
function sandTex(lin,base,dots,n=600){return tex(lin,128,128,(x,w,h)=>{x.fillStyle=base;x.fillRect(0,0,w,h);for(let i=0;i<n;i++){x.fillStyle=dots[i%dots.length];x.fillRect(Math.random()*w,Math.random()*h,2,2)}})}

/* ---------------------------------------------------------------- the five areas */
/* Each area: sky, light, street look, how the 4 shops are coloured and dressed, the 4 decorations (spots 4-7) and the big building. */
const THEMES=[
 {key:'candy',sky:['#ff8cc6','#ffc4e2','#ffe8d6'],fog:'#ffe8d6',ground:'#86dc93',hills:['#79d388','#86dc93','#71cc80'],light:{sun:[0xfff0dc,1],hemi:[0xfff4fb,0x9f86d8,.62],rim:[0xb7a6ff,.45]},
  pave:['#c9c3e3','#b4add2'],curb:['#ff6fae','#ffffff'],road:['#4a4e6a','#ffffff','#f5f0ff'],ghost:'#62d8ff',
  shops:['building-small-a','building-small-b','building-small-c','building-small-d'],tint:[null,null,null,null],extraBuilt:true,
  lamp:{pole:'#5b4f86',glow:'#fff3a0'},clouds:'#ffffff'},
 {key:'sea',sky:['#3fb4ff','#9fdcff','#e9f8ff'],fog:'#d8f1ff',ground:'#f1d9a2',hills:['#e9cf92','#f1d9a2','#e2c283'],light:{sun:[0xfff6e6,1.05],hemi:[0xeaf8ff,0x6aa7c9,.6],rim:[0x9fe3ff,.45]},
  pave:'planks',curb:['#2a8fe0','#ffffff'],road:['#c9a874','#ffffff','#f3e6c8'],ghost:'#7fe8ff',
  shops:['building-small-c','building-small-a','building-small-d','building-small-b'],tint:[{h:205,s:1.1,l:0},{h:175,s:1,l:.02},{h:12,s:1.05,l:.02},{h:48,s:1,l:.04}],
  lamp:{pole:'#22406a',glow:'#ffd6e8',head:'shell',trim:'#ffffff'},clouds:'#ffffff'},
 {key:'jungle',sky:['#5ccf86','#bdeeb0','#f2fbe2'],fog:'#e3f6d2',ground:'#4fb548',hills:['#3fa23e','#4fb548','#2f8f35'],light:{sun:[0xfff3d0,1.05],hemi:[0xf4ffe8,0x4c7a3a,.6],rim:[0xd8ff9f,.4]},
  pave:'stone',curb:['#3fae4a','#ffd84a'],road:['#8a5a35',null,'#a77a4f'],ghost:'#b6ff8f',
  shops:['building-small-b','building-small-d','building-small-a','building-small-c'],tint:[{h:28,s:1.1,l:0},{h:95,s:.95,l:-.02},{h:50,s:1.1,l:.02},{h:15,s:.9,l:-.06}],
  lamp:{pole:'#6b4a2a',glow:'#ff9a3c',head:'lantern',bamboo:true},clouds:'#ffffff'},
 {key:'cloud',sky:['#5f95ff','#a9c8ff','#e4eeff'],fog:'#d6e4ff',ground:'#c9d6ff',hills:['#ffffff','#f2f5ff','#e6edff'],light:{sun:[0xffffff,1],hemi:[0xffffff,0x8f9fd8,.6],rim:[0xffc6ec,.45]},
  pave:['#e3e8ff','#bfc9ef'],curb:['#ff8fb1','#ffd36b','#8fe39a','#7fc7ff','#c3a6ff'],road:['#aab8e6','rainbow','#eef2ff'],ghost:'#ffd6ff',
  shops:['building-small-d','building-small-c','building-small-b','building-small-a'],tint:[{h:330,s:.6,l:.1},{h:205,s:.6,l:.1},{h:270,s:.55,l:.1},{h:45,s:.7,l:.1}],
  lamp:{pole:'#f4f6ff',glow:'#aee3ff',head:'orb'},clouds:'#ffffff'},
 {key:'galaxy',sky:['#03010f','#120838','#3a1d78'],fog:'#140a36',ground:'#1f1745',hills:['#191238','#1f1745','#140e2e'],light:{sun:[0xe6ecff,.95],hemi:[0xb9a6ff,0x2a1d55,.6],rim:[0xff6fd8,.7]},
  pave:'metal',curb:['#ff4fd8','#3fe8ff'],road:['#231d45','neon','#3fe8ff'],ghost:'#ff8fe8',night:true,
  shops:['building-small-a','building-small-c','building-small-d','building-small-b'],tint:[{h:285,s:1.1,l:-.05},{h:190,s:1.1,l:-.04},{h:320,s:1.1,l:-.04},{h:250,s:1,l:-.04}],
  lamp:{pole:'#8f9bc2',glow:'#7fe3ff',head:'planet',metal:.6,ring:'#ff9ec7'},clouds:null}];

/* Shop dressing per area (part of the building, so it is greyed and rebuilt with it). b = the building's box. */
function dress(th,g,b,i,lin){const cx=(b.min.x+b.max.x)/2,top=b.max.y,fz=b.max.z,w=b.max.x-b.min.x,d=b.max.z-b.min.z,cz=(b.min.z+b.max.z)/2;const cols=['#e5333d','#2a8fe0','#ffc93c','#39b86a'];
  if(th.key==='sea'){const m=grp(b.max.x-.5,top,b.min.z+.5,g);mesh(new THREE.CylinderGeometry(.05,.06,1.9,8),M('#7a4a2a',.6),0,.95,0,m);
      const f=mesh(new THREE.PlaneGeometry(.8,.5),new THREE.MeshStandardMaterial({color:cols[i],side:THREE.DoubleSide,roughness:.6}),.42,1.62,0,m);f.userData.flag=1;
      const ring=mesh(new THREE.TorusGeometry(.28,.09,10,20),new THREE.MeshStandardMaterial({map:stripes(lin,['#e5333d','#ffffff'],8),roughness:.5}),b.min.x+.55,1.55,fz+.06,g)}
  if(th.key==='jungle'){const r=mesh(new THREE.ConeGeometry(Math.max(w,d)*.78,1.5,4,1,true),new THREE.MeshStandardMaterial({map:stripes(lin,['#d9b25a','#c49a42','#e6c46e'],12,true),roughness:.9,side:THREE.DoubleSide}),cx,top+.62,cz,g);r.rotation.y=Math.PI/4;
      mesh(new THREE.SphereGeometry(.16,8,6),M('#ff6f3c',.5),cx,top+1.38,cz,g);for(const s of[-1,1])mesh(new THREE.BoxGeometry(.12,1.5,.12),M('#8a6a3a',.7),cx+s*(w/2-.1),.9,fz+.08,g)}
  if(th.key==='cloud'){for(const s of[-1,1])puffs(g,cx+s*(w/2-.2),.25,fz-.2,2,.7);puffs(g,cx,top+.15,cz,3,.55);const st=starMesh(.32,.14,'#ffd84a');st.position.set(cx,top+.95,cz);g.add(st)}
  if(th.key==='galaxy'){const dish=grp(b.max.x-.6,top,cz-.3,g);mesh(new THREE.CylinderGeometry(.05,.05,.6,8),M('#c9d2e0',.3,.6),0,.3,0,dish);
      const dm=mesh(new THREE.SphereGeometry(.45,18,10,0,Math.PI*2,0,Math.PI/2.6),new THREE.MeshStandardMaterial({color:'#e8ecf5',roughness:.3,metalness:.5,side:THREE.DoubleSide}),0,.7,0,dish);dm.rotation.x=Math.PI*.75;
      const ant=mesh(new THREE.CylinderGeometry(.025,.025,1.2,6),M('#c9d2e0',.3,.6),b.min.x+.5,top+.6,cz,g);const tip=mesh(new THREE.SphereGeometry(.09,10,8),glowMat('#ff3c6e'),b.min.x+.5,top+1.22,cz,g);tip.userData.blink=1;
      const neon=glowMat(['#3fe8ff','#ff4fd8','#ffe066','#7fff9a'][i]);mesh(new THREE.BoxGeometry(w+.08,.08,.08),neon,cx,top-.05,fz+.02,g);g.userData.neon=neon}}

/* The four decorations: spot 4 (plaza centre), 5 (between the shops, a pair), 6 (street lamps), 7 (east end). Each returns {objs, glows, tick}. */
function deco(th,k,lin){const glows=[];let tick=null,objs=[],wideShot=false;const T_=th.key;
  if(k===6){objs=[];for(const x of[-15,-9,9,15])objs.push(lampPost(x,.2,1,th.lamp));for(const x of[-12,-3,3,12])objs.push(lampPost(x,7.7,-1,th.lamp));objs.forEach(o=>glows.push(o.userData.head.material));wideShot=true;
    return{objs,glows,wideShot,lamps:true,tick:(dt,t,on)=>{objs.forEach((o,i)=>{if(o.userData.ring&&on)o.userData.ring.emissiveIntensity=.6+Math.sin(t*2+i)*.3})}}}
  if(T_==='candy'){
    if(k===4){const f=kenney('pavement-fountain',lin);f.position.set(0,.004,-2.7);objs=[f];let acc=0;tick=(dt,t,on,sc,s)=>{if(!on)return;acc+=dt;while(acc>.04){acc-=.04;const c=new THREE.Vector3();s.bb.getCenter(c);
      const m=new THREE.Mesh(geo.drop||(geo.drop=new THREE.SphereGeometry(.07,8,6)),new THREE.MeshBasicMaterial({color:'#9fdcff',transparent:true}));m.position.set(c.x,s.h*.95,c.z);addPart(m,sc,{v:v3(rnd(1.6),2.6+Math.random(),rnd(1.6)),g:6,life:.8,fade:true})}}}
    if(k===5){for(const x of[-9,9]){const t=kenney('grass-trees-tall',lin);t.position.set(x,.004,-3);objs.push(t);const gm=grp(x,0,-3);['#ff6fa8','#ffd166','#7fe3ff','#b8f27a','#c3a6ff','#ff9a3c'].forEach((c,i)=>{const a=i*1.05;mesh(new THREE.SphereGeometry(.17,12,10),M(c,.25),Math.cos(a)*.75,1.9+(i%3)*.5,Math.sin(a)*.75+.2,gm)});objs.push(gm)}}
    if(k===7){const g=grp(EXTRA_X,H0,-2.6);[['#ff4f9a',-.7,0,2.4],['#7b4dff',.6,-.4,2.9],['#2ec4b6',.1,.6,2]].forEach(([c,x,z,h])=>{const l=grp(x,0,z,g);mesh(new THREE.CylinderGeometry(.07,.07,h,10),M('#ffffff',.4),0,h/2,0,l);
      const hd=mesh(new THREE.CylinderGeometry(.6,.6,.2,32),[M('#ffffff',.3),new THREE.MeshStandardMaterial({map:swirlTex(c),roughness:.3}),new THREE.MeshStandardMaterial({map:swirlTex(c),roughness:.3})],0,h+.6,0,l);hd.rotation.x=Math.PI/2});objs=[g]}}
  if(T_==='sea'){
    if(k===4){const g=grp(0,0,-2.7);const pl=new THREE.MeshStandardMaterial({map:stripes(lin,['#c98b4a','#b87a3c','#d39a5a'],9),roughness:.8});mesh(new THREE.BoxGeometry(5,.18,2.6),pl,0,.62,0,g);
      for(const x of[-2.3,-.8,.8,2.3])for(const z of[-1.15,1.15])mesh(new THREE.CylinderGeometry(.1,.12,1.3,8),M('#7a4a2a',.7),x,.2,z,g);
      for(const z of[-1.2,1.2]){mesh(new THREE.CylinderGeometry(.035,.035,4.6,6),M('#f6e2b3',.8),0,1.35,z,g).rotation.z=Math.PI/2;for(const x of[-2.3,0,2.3])mesh(new THREE.CylinderGeometry(.05,.05,.75,8),M('#7a4a2a',.7),x,1.05,z,g)}
      for(const x of[-2.1,2.1]){mesh(new THREE.CylinderGeometry(.16,.2,.4,12),M('#2a3a5c',.5),x,.92,.9,g)}
      const boat=grp(0,.18,1.9,g);mesh(new THREE.BoxGeometry(2,.4,.8),M('#ffffff',.5),0,0,0,boat);mesh(new THREE.BoxGeometry(2.05,.12,.84),M('#e5333d',.5),0,.18,0,boat);boat.userData.bob=1;objs=[g];
      tick=(dt,t,on)=>{boat.position.y=.18+Math.sin(t*1.6)*.05;boat.rotation.z=Math.sin(t*1.2)*.04}}
    if(k===5){for(const[x,c]of[[-9,'#e5333d'],[9,'#2a8fe0']]){const g=grp(x,H0,-3);for(const s of[-.6,.6])mesh(new THREE.BoxGeometry(.3,.5,1.2),M('#8a5a2a',.8),s,.25,0,g);
        const hull=mesh(new THREE.CylinderGeometry(.55,.4,2.6,12,1,false,0,Math.PI),M(c,.45),0,.85,0,g);hull.rotation.set(0,0,Math.PI/2);hull.rotation.y=0;hull.rotation.x=Math.PI;hull.scale.set(1,1,.9);
        mesh(new THREE.BoxGeometry(2.5,.08,.9),M('#d39a5a',.7),0,.85,0,g);mesh(new THREE.CylinderGeometry(.05,.06,3.4,8),M('#7a4a2a',.6),0,2.5,0,g);
        const sh=new THREE.Shape();sh.moveTo(0,0);sh.lineTo(1.3,0);sh.lineTo(0,2.6);sh.closePath();const sail=mesh(new THREE.ShapeGeometry(sh),new THREE.MeshStandardMaterial({map:stripes(lin,['#ffffff','#ffffff',c],6,true),side:THREE.DoubleSide,roughness:.6}),.08,1.1,0,g);
        const sh2=new THREE.Shape();sh2.moveTo(0,0);sh2.lineTo(-.9,0);sh2.lineTo(0,2);sh2.closePath();mesh(new THREE.ShapeGeometry(sh2),new THREE.MeshStandardMaterial({color:'#ffffff',side:THREE.DoubleSide,roughness:.6}),-.08,1.15,0,g);
        const fl=mesh(new THREE.PlaneGeometry(.5,.3),new THREE.MeshStandardMaterial({color:'#ffc93c',side:THREE.DoubleSide}),.25,4.05,0,g);g.userData.flag=fl;objs.push(g)}
      tick=(dt,t,on)=>{objs.forEach((g,i)=>{g.rotation.z=on?Math.sin(t*1.1+i)*.025:0;g.userData.flag.rotation.y=Math.sin(t*5+i)*.4})}}
    if(k===7){const g=grp(EXTRA_X,H0,-3);const sm=new THREE.MeshStandardMaterial({map:sandTex(lin,'#e8c27a',['#d9ad5f','#f2d59a']),roughness:.95});
      mesh(new THREE.BoxGeometry(3.4,.35,3),sm,0,.17,0,g);mesh(new THREE.BoxGeometry(1.8,1.6,1.6),sm,0,1.15,0,g);for(let i=0;i<4;i++)mesh(new THREE.BoxGeometry(.32,.3,.32),sm,-.7+i*.47,2.1,.65,g);
      for(const[x,z]of[[-1.3,-1.1],[1.3,-1.1],[-1.3,1.1],[1.3,1.1]]){mesh(new THREE.CylinderGeometry(.38,.5,1.5,12),sm,x,1.1,z,g);mesh(new THREE.ConeGeometry(.42,.6,12),sm,x,2.15,z,g)}
      mesh(new THREE.CylinderGeometry(.55,.7,2.6,14),sm,0,2.4,-.2,g);mesh(new THREE.ConeGeometry(.6,.9,14),sm,0,4.15,-.2,g);mesh(new THREE.CylinderGeometry(.02,.02,.8,6),M('#7a4a2a',.6),0,4.9,-.2,g);
      mesh(new THREE.PlaneGeometry(.45,.28),new THREE.MeshStandardMaterial({color:'#e5333d',side:THREE.DoubleSide}),.23,5.15,-.2,g);mesh(new THREE.BoxGeometry(.5,.7,.05),M('#9a6a3a',.7),0,.75,.82,g);
      for(const[x,c]of[[-1.2,'#ff6fa8'],[1.1,'#7fe3ff']])mesh(new THREE.SphereGeometry(.14,10,8),M(c,.4),x,.45,1.4,g);objs=[g]}}
  if(T_==='jungle'){
    if(k===4){const g=grp(0,0,-2.7);const wd=M('#8a5a2a',.8);for(const sx of[-2.7,2.7]){for(const dx of[-.45,.45])for(const dz of[-.6,.6])mesh(new THREE.CylinderGeometry(.09,.1,2.4,8),wd,sx+dx,1.2,dz,g);
        mesh(new THREE.BoxGeometry(1.2,.12,1.5),M('#b8864a',.8),sx,1.25,0,g);const r=mesh(new THREE.ConeGeometry(1,.7,4),M('#d9b25a',.9),sx,2.7,0,g);r.rotation.y=Math.PI/4}
      const N=12;for(let i=0;i<N;i++){const t=(i+.5)/N,x=-2.1+t*4.2,y=1.25-Math.sin(t*Math.PI)*.45;const p=mesh(new THREE.BoxGeometry(.3,.07,1.1),M(i%2?'#c49a5a':'#b8864a',.8),x,y,0,g);p.rotation.z=Math.cos(t*Math.PI)*.33}
      for(const z of[-.55,.55]){const pts=[];for(let i=0;i<=12;i++){const t=i/12;pts.push(v3(-2.25+t*4.5,2.05-Math.sin(t*Math.PI)*.4,z))}mesh(new THREE.TubeGeometry(new THREE.CatmullRomCurve3(pts),24,.03,5),M('#f0d9a0',.8),0,0,0,g)}
      objs=[g]}
    if(k===5){objs=[palm(-9,-3,1.05,null,true),palm(9,-3.2,1.15,null,true)];objs.forEach(o=>o.position.y=H0);tick=(dt,t)=>objs.forEach(o=>o.rotation.z=Math.sin(t*1.3+o.userData.sway)*.03)}
    if(k===7){const g=grp(EXTRA_X,H0,-3.6);const rm=M('#6f7f6a',.9),rm2=M('#5c6e58',.9);[[-1.1,.9,0,1.3],[1.1,1,0,1.3],[-.9,2.5,-.2,1.1],[1,2.6,-.2,1.15],[0,3.9,-.4,1.2],[-.2,1.2,-.5,1.2]].forEach(([x,y,z,r],i)=>{const m=mesh(new THREE.DodecahedronGeometry(r,0),i%2?rm:rm2,x,y,z,g);m.scale.z=.7});
      for(const[x,y]of[[-1.3,1.9],[1.4,3.1],[.6,4.6]])mesh(new THREE.SphereGeometry(.45,10,8),M('#3fae4a',.7),x,y,.5,g);
      const wt=tex(lin,64,256,(x,w,h)=>{x.fillStyle='#5fc8ff';x.fillRect(0,0,w,h);for(let i=0;i<40;i++){x.fillStyle=`rgba(255,255,255,${.3+Math.random()*.5})`;x.fillRect(Math.random()*w,Math.random()*h,2+Math.random()*4,20+Math.random()*40)}});wt.wrapT=THREE.RepeatWrapping;
      const water=mesh(new THREE.PlaneGeometry(1.3,4.2),new THREE.MeshStandardMaterial({map:wt,transparent:true,opacity:.9,roughness:.2}),0,2.4,.65,g);water.castShadow=false;
      const pool=mesh(new THREE.CylinderGeometry(1.7,1.8,.16,24),M('#3fb6e8',.15),0,.08,1.3,g);pool.castShadow=false;objs=[g];let acc=0;
      tick=(dt,t,on,sc)=>{if(!on)return;wt.offset.y=(wt.offset.y+dt*1.4)%1;acc+=dt;while(acc>.05){acc-=.05;const m=new THREE.Mesh(geo.drop||(geo.drop=new THREE.SphereGeometry(.07,8,6)),new THREE.MeshBasicMaterial({color:'#e8fbff',transparent:true}));
        m.position.set(EXTRA_X+rnd(.6),H0+.25,-3.6+1.1);addPart(m,sc,{v:v3(rnd(1.6),1.5+Math.random(),.6+Math.random()),g:6,life:.6,fade:true})}}}}
  if(T_==='cloud'){
    if(k===4){const g=grp(0,H0,-1.6);for(let i=0;i<5;i++){const st=puffs(g,0,.1+i*.42,-i*.75,4,.62);st.children.forEach(p=>p.scale.y=.55)}
      const arch=mesh(new THREE.TorusGeometry(1.15,.09,10,28,Math.PI),M('#ffc93c',.3,.5),0,2.2,-3.1,g);for(const s of[-1,1])mesh(new THREE.CylinderGeometry(.09,.09,2.2,10),M('#ffc93c',.3,.5),s*1.15,1.1,-3.1,g);
      const st=starMesh(.3,.13,'#ffe066');st.position.set(0,3.55,-3.1);g.add(st);objs=[g]}
    if(k===5){for(const x of[-9,9]){const g=grp(x,H0,-3);const gm=M('#ffc93c',.3,.6);mesh(new THREE.CylinderGeometry(.07,.09,3.4,10),M('#f4f6ff',.4),0,1.7,0,g);mesh(new THREE.BoxGeometry(1.8,.09,.09),gm,0,3.35,0,g);
        const tubes=[];for(let i=0;i<5;i++){const piv=grp(-.72+i*.36,3.3,0,g);const L=.6+((i*7)%5)*.14;mesh(new THREE.CylinderGeometry(.008,.008,.25,4),M('#ffffff',.5),0,-.12,0,piv);
          mesh(new THREE.CylinderGeometry(.045,.045,L,10),M('#d9e4f5',.2,.8),0,-.25-L/2,0,piv);tubes.push(piv)}g.userData.tubes=tubes;objs.push(g)}
      tick=(dt,t,on)=>objs.forEach((g,j)=>g.userData.tubes.forEach((p,i)=>{p.rotation.z=Math.sin(t*(on?2.4:1)+i*1.3+j)*(on?.18:.05);p.rotation.x=Math.cos(t*1.7+i)*(on?.1:.02)}))}
    if(k===7){const g=grp(EXTRA_X,H0,-3);mesh(new THREE.CylinderGeometry(1.8,1.9,.3,28),M('#f4f6ff',.5),0,.15,0,g);mesh(new THREE.TorusGeometry(1.8,.06,8,40),M('#ffc93c',.3,.6),0,.32,0,g).rotation.x=Math.PI/2;
      const bl=[];[[-.7,3.3,.2,['#ff6fa8','#ffffff']],[.8,4.4,-.4,['#7fc7ff','#ffe066']]].forEach(([x,y,z,cs2])=>{const b=grp(x,y,z,g);const bm=new THREE.MeshStandardMaterial({map:stripes(lin,cs2,10),roughness:.5});
        const s=mesh(new THREE.SphereGeometry(1,24,18),bm,0,0,0,b);s.scale.y=1.12;mesh(new THREE.BoxGeometry(.5,.4,.5),M('#a0622d',.7),0,-1.75,0,b);
        for(const[a,c]of[[-1,-1],[1,-1],[-1,1],[1,1]]){const r=mesh(new THREE.CylinderGeometry(.012,.012,.9,4),M('#ffffff',.5),a*.3,-1.25,c*.3,b);r.rotation.set(c*.25,0,-a*.25)}
        const rope=mesh(new THREE.CylinderGeometry(.015,.015,y-2,4),M('#ffffff',.5),0,-1.95-(y-2)/2,0,b);bl.push(b);b.userData.y0=y});objs=[g];
      tick=(dt,t,on)=>bl.forEach((b,i)=>{b.position.y=b.userData.y0+(on?Math.sin(t*1.3+i*2)*.2:0)})}}
  if(T_==='galaxy'){
    if(k===4){const g=grp(0,H0,-2.7);mesh(new THREE.BoxGeometry(3.6,.5,2.8),M('#7b4dff',.5),0,.25,0,g);for(const[x,z]of[[-1.7,-1.3],[1.7,-1.3],[-1.7,1.3],[1.7,1.3]]){mesh(new THREE.CylinderGeometry(.22,.22,1.9,12),M('#ffd23c',.4),x,1.2,z,g);mesh(new THREE.SphereGeometry(.3,12,10),M('#ff4fd8',.4),x,2.25,z,g)}
      const dome=mesh(new THREE.SphereGeometry(1.75,28,14,0,Math.PI*2,0,Math.PI/2),new THREE.MeshStandardMaterial({map:stripes(lin,['#b07bff','#ffe066'],12),roughness:.5}),0,.5,0,g);
      const net=new THREE.MeshBasicMaterial({color:'#3fe8ff',wireframe:true,transparent:true,opacity:.5});const nm=mesh(new THREE.BoxGeometry(3.4,1.4,2.6,6,3,5),net,0,1.2,0,g);nm.castShadow=false;objs=[g];
      tick=(dt,t,on)=>{nm.visible=on;if(on){const s=1+Math.abs(Math.sin(t*3))*.06;dome.scale.set(1/Math.sqrt(s),s,1/Math.sqrt(s))}}}
    if(k===5){for(const x of[-9,9]){const g=grp(x,H0,-3);mesh(new THREE.CylinderGeometry(.9,1,.3,20),M('#4a3f78',.6,.3),0,.15,0,g);
        ['#ffe066','#ff8fd0','#7fe3ff','#b8f27a','#ffb347'].forEach((c,i)=>{const a=i*1.26,r=i?.55:0,h=1.5+((i*3)%4)*.35;mesh(new THREE.CylinderGeometry(.035,.045,h,6),M('#3fd1a0',.5),Math.cos(a)*r,h/2+.3,Math.sin(a)*r,g);
          const st=starMesh(.3,.13,c);st.material=glowMat(c);st.position.set(Math.cos(a)*r,h+.35,Math.sin(a)*r);st.userData.ph=i;g.add(st);glows.push(st.material)});objs.push(g)}
      tick=(dt,t,on)=>objs.forEach(g=>g.children.forEach(c=>{if(c.userData.ph!=null)c.rotation.y=on?t*1.5+c.userData.ph:0}))}
    if(k===7){const g=grp(EXTRA_X,H0,-3.2);mesh(new THREE.CylinderGeometry(1.35,1.45,1.8,24),M('#d9def0',.4,.3),0,.9,0,g);
      const dome=mesh(new THREE.SphereGeometry(1.36,24,14,0,Math.PI*2,0,Math.PI/2),M('#c9d2e0',.25,.7),0,1.8,0,g);mesh(new THREE.BoxGeometry(.5,1.2,.1),M('#1b1640',.4),0,2.45,1.08,g).rotation.x=-.5;
      const tube=grp(0,2.4,.3,g);mesh(new THREE.CylinderGeometry(.22,.3,2.6,16),M('#ffffff',.3,.4),0,1.2,0,tube);mesh(new THREE.CylinderGeometry(.33,.33,.2,16),M('#ff4fd8',.3,.4),0,2.45,0,tube);tube.rotation.x=.8;
      mesh(new THREE.BoxGeometry(.6,.9,.05),M('#3a3168',.5),0,.45,1.38,g);objs=[g];tick=(dt,t,on)=>{if(on)tube.rotation.y=Math.sin(t*.4)*.4}}}
  return{objs,glows,tick,wideShot}}

/* The big building of each area, centred at (0, BIG_Z). */
function bigObj(th,lin){const T_=th.key;const g=grp(0,0,BIG_Z);const glows=[];let tick=null;
  if(T_==='candy'){const f=kenney('building-garage',lin);f.scale.set(TS*1.7,TS*2.4,TS*1.6);f.position.set(0,.004,0);g.add(f);g.userData.pinkFactory=f}
  if(T_==='sea'){[[0,.6,0,1.9],[-1.2,.4,.6,1.1],[1.1,.35,.7,1]].forEach(([x,y,z,r])=>{const m=mesh(new THREE.DodecahedronGeometry(r,0),M('#8f96a3',.9),x,y,z,g);m.scale.y=.6});
    const st=stripes(lin,['#e5333d','#ffffff'],6,true);mesh(new THREE.CylinderGeometry(.95,1.3,6.4,28),new THREE.MeshStandardMaterial({map:st,roughness:.5}),0,4.3,0,g);
    mesh(new THREE.CylinderGeometry(1.35,1.35,.18,28),M('#2a3a5c',.5),0,7.55,0,g);mesh(new THREE.TorusGeometry(1.3,.04,6,32),M('#2a3a5c',.5),0,8.05,0,g).rotation.x=Math.PI/2;
    const lamp=glowMat('#ffe27a');mesh(new THREE.CylinderGeometry(.75,.75,1,20),lamp,0,8.15,0,g);glows.push(lamp);mesh(new THREE.ConeGeometry(1,1.1,20),M('#e5333d',.45),0,9.2,0,g);mesh(new THREE.SphereGeometry(.15,10,8),M('#ffc93c',.3,.5),0,9.8,0,g);
    mesh(new THREE.BoxGeometry(.8,1.3,.1),M('#2a3a5c',.5),0,1.75,1.18,g);
    const beam=new THREE.Group();beam.position.set(0,8.15,0);g.add(beam);const bm=new THREE.MeshBasicMaterial({color:'#fff3a8',transparent:true,opacity:.0,blending:THREE.AdditiveBlending,depthWrite:false,side:THREE.DoubleSide});
    for(const s of[-1,1]){const c=new THREE.Mesh(new THREE.ConeGeometry(1.1,7,20,1,true),bm);c.rotation.z=s*Math.PI/2;c.position.x=s*3.6;beam.add(c)}g.userData.noGhost=[beam];
    tick=(dt,t,on)=>{beam.rotation.y=t*.9;bm.opacity=on?.13:0}}
  if(T_==='jungle'){const bark=M('#7a5030',.9);mesh(new THREE.CylinderGeometry(.95,1.3,4.6,14),bark,0,2.3,0,g);for(let i=0;i<5;i++){const r=mesh(new THREE.ConeGeometry(.35,1.6,8),bark,Math.cos(i*1.26)*1.2,.4,Math.sin(i*1.26)*1.2,g);r.rotation.set(Math.sin(i*1.26)*.9,0,-Math.cos(i*1.26)*.9)}
    mesh(new THREE.CylinderGeometry(2.6,2.6,.25,24),M('#b8864a',.8),0,4.4,0,g);for(let i=0;i<10;i++)mesh(new THREE.CylinderGeometry(.06,.06,.7,6),M('#8a5a2a',.8),Math.cos(i*.628)*2.5,4.85,Math.sin(i*.628)*2.5,g);
    mesh(new THREE.BoxGeometry(3,2,2.4),new THREE.MeshStandardMaterial({map:stripes(lin,['#c98b4a','#b87a3c'],10,true),roughness:.8}),0,5.55,0,g);const win=glowMat('#ffd36b');mesh(new THREE.BoxGeometry(.7,.6,.06),win,-.75,5.7,1.22,g);mesh(new THREE.BoxGeometry(.7,1.2,.06),M('#5a3a1e',.7),.6,5.15,1.22,g);glows.push(win);
    const rf=mesh(new THREE.ConeGeometry(2.5,1.6,4),M('#d9b25a',.9),0,7.35,0,g);rf.rotation.y=Math.PI/4;
    [[-1.6,8.2,-1.2,1.7],[1.7,8.5,-1,1.6],[0,9.3,-1.6,1.9],[0,7.6,-2.2,1.8]].forEach(([x,y,z,r],i)=>mesh(new THREE.SphereGeometry(r,16,12),M(i%2?'#2e9e48':'#3fbf5a',.7),x,y,z,g));
    for(let i=0;i<6;i++)mesh(new THREE.BoxGeometry(.7,.07,.1),M('#8a5a2a',.8),1.6,.6+i*.65,1.6,g);for(const s of[-.32,.32])mesh(new THREE.CylinderGeometry(.04,.04,4.2,6),M('#8a5a2a',.8),1.6+s,2.2,1.6,g)}
  if(T_==='cloud'){puffs(g,0,.2,0,6,1.1);const wall=M('#f7f8ff',.6),roof=M('#5b7cff',.4);mesh(new THREE.BoxGeometry(3.6,3.4,2.8),wall,0,2.2,0,g);
    for(let i=0;i<5;i++)mesh(new THREE.BoxGeometry(.4,.4,.4),wall,-1.6+i*.8,4.1,1.2,g);
    for(const[x,z]of[[-1.9,-1.3],[1.9,-1.3],[-1.9,1.3],[1.9,1.3]]){mesh(new THREE.CylinderGeometry(.62,.62,4.6,18),wall,x,2.8,z,g);mesh(new THREE.ConeGeometry(.8,1.5,18),roof,x,5.85,z,g)}
    mesh(new THREE.CylinderGeometry(.95,.95,6.6,20),wall,0,3.8,-.4,g);mesh(new THREE.ConeGeometry(1.2,2.2,20),roof,0,8.2,-.4,g);
    const win=glowMat('#ffe9a8');for(const x of[-.9,.9])mesh(new THREE.BoxGeometry(.5,.7,.06),win,x,3,1.42,g);mesh(new THREE.CylinderGeometry(.3,.3,.06,16),win,0,6,.56,g).rotation.x=Math.PI/2;glows.push(win);
    const gate=mesh(new THREE.BoxGeometry(1,1.5,.08),M('#7b5cff',.5),0,1.25,1.42,g);mesh(new THREE.CylinderGeometry(.03,.03,1.2,6),M('#ffc93c',.3,.6),0,9.8,-.4,g);
    const fl=mesh(new THREE.PlaneGeometry(.7,.4),new THREE.MeshStandardMaterial({color:'#ffc93c',side:THREE.DoubleSide}),.36,10.2,-.4,g);tick=(dt,t,on)=>{fl.rotation.y=Math.sin(t*4)*.4}}
  if(T_==='galaxy'){const met=M('#e6e9f5',.3,.5);for(const[x,z]of[[-1.4,-1.4],[1.4,-1.4],[-1.4,1.4],[1.4,1.4]]){const l=mesh(new THREE.CylinderGeometry(.12,.16,3.2,8),M('#8f9bc2',.4,.6),x*1.1,1.5,z*1.1,g);l.rotation.set(z*.12,0,-x*.12)}
    mesh(new THREE.CylinderGeometry(1.4,1.4,2.8,24),met,0,3.9,0,g);const glass=new THREE.MeshStandardMaterial({color:'#7fe3ff',emissive:'#3fe8ff',emissiveIntensity:0,roughness:.1,metalness:.3,transparent:true,opacity:.85});
    mesh(new THREE.SphereGeometry(1.35,24,14,0,Math.PI*2,0,Math.PI/2),glass,0,5.3,0,g);glows.push(glass);const ringG=grp(0,4,0,g);mesh(new THREE.TorusGeometry(3,.3,12,48),met,0,0,0,ringG).rotation.x=Math.PI/2;
    const lights=glowMat('#ff4fd8');for(let i=0;i<12;i++)mesh(new THREE.SphereGeometry(.1,8,6),lights,Math.cos(i*.52)*3,.3,Math.sin(i*.52)*3,ringG);glows.push(lights);
    for(const s of[-1,1])mesh(new THREE.BoxGeometry(.12,.12,1),met,s*1.4,3.6,0,g).rotation.y=Math.PI/2;
    const panel=new THREE.MeshStandardMaterial({map:tex(lin,64,64,(x,w,h)=>{x.fillStyle='#1f4fd0';x.fillRect(0,0,w,h);x.strokeStyle='#9fd0ff';x.lineWidth=2;for(let i=0;i<=4;i++){x.beginPath();x.moveTo(i*16,0);x.lineTo(i*16,h);x.stroke();x.beginPath();x.moveTo(0,i*16);x.lineTo(w,i*16);x.stroke()}}),roughness:.3,metalness:.4});
    for(const s of[-1,1])mesh(new THREE.BoxGeometry(2.2,.06,1.2),panel,s*4.3,4,0,g);mesh(new THREE.CylinderGeometry(.04,.04,1.6,6),met,0,7.4,0,g);const tip=glowMat('#ff3c6e');mesh(new THREE.SphereGeometry(.13,10,8),tip,0,8.2,0,g);
    mesh(new THREE.BoxGeometry(.9,1.2,.1),M('#3a3168',.5),0,3.2,1.4,g);tick=(dt,t,on)=>{ringG.rotation.y=on?t*.4:0;tip.emissiveIntensity=on?(Math.sin(t*5)>0?1:.1):0}}
  return{g,glows,tick}}

/* ---------------------------------------------------------------- street, ground and sky */
function streetTex(th,lin){const P=th.pave;let pave;
  if(P==='planks')pave=tex(lin,256,256,(x,w,h)=>{for(let i=0;i<8;i++){x.fillStyle=['#d8a96a','#c99858','#e0b478','#cf9f60'][i%4];x.fillRect(0,i*h/8,w,h/8);x.fillStyle='#8a5a2a';x.fillRect(0,i*h/8,w,3);x.fillRect(((i*97)%w),i*h/8,3,h/8)}});
  else if(P==='stone')pave=tex(lin,256,256,(x,w,h)=>{x.fillStyle='#a8916a';x.fillRect(0,0,w,h);for(let r=0;r<6;r++)for(let c=0;c<5;c++){x.fillStyle=['#e3cfa0','#d8c290','#ecdcb2'][(r+c)%3];const ox=(r%2)*25;rr(x,c*52+ox+3-26,r*43+3,46,37,8);x.fill()}});
  else if(P==='metal')pave=tex(lin,256,256,(x,w,h)=>{x.fillStyle='#4a4280';x.fillRect(0,0,w,h);for(let i=0;i<4;i++)for(let j=0;j<4;j++){x.fillStyle=(i+j)%2?'#544a8e':'#4f4688';x.fillRect(i*64+3,j*64+3,58,58);x.fillStyle='#9a92d0';for(const[a,b]of[[8,8],[54,8],[8,54],[54,54]])x.fillRect(i*64+a,j*64+b,3,3)}});
  else pave=tex(lin,256,256,(x,w,h)=>{x.fillStyle=P[0];x.fillRect(0,0,w,h);x.strokeStyle=P[1];x.lineWidth=5;for(let i=0;i<=8;i++){x.beginPath();x.moveTo(i*w/8,0);x.lineTo(i*w/8,h);x.stroke();x.beginPath();x.moveTo(0,i*h/8);x.lineTo(w,i*h/8);x.stroke()}});
  pave.wrapS=pave.wrapT=THREE.RepeatWrapping;
  const curb=stripes(lin,th.curb,th.curb.length>2?10:8);curb.wrapS=THREE.RepeatWrapping;
  const[rb,line,edge]=th.road;const road=tex(lin,512,256,(x,w,h)=>{x.fillStyle=rb;x.fillRect(0,0,w,h);
    if(th.key==='jungle'){for(let i=0;i<1400;i++){x.fillStyle=`rgba(${Math.random()<.5?'60,35,15':'190,150,100'},${Math.random()*.25})`;x.fillRect(Math.random()*w,Math.random()*h,4,4)}x.fillStyle='rgba(60,35,15,.25)';x.fillRect(0,h*.28,w,14);x.fillRect(0,h*.66,w,14)}
    else if(th.key==='sea'){for(let r=0;r<10;r++)for(let c=0;c<20;c++){x.fillStyle=['#d6b886','#c9a874','#e0c597','#bf9c66'][(r*3+c)%4];rr(x,c*26+(r%2)*13-13,r*26+2,23,22,6);x.fill()}x.fillStyle='#ffffff';x.fillRect(0,h/2-5,w*.45,10)}
    else{for(let i=0;i<900;i++){x.fillStyle=`rgba(255,255,255,${Math.random()*.06})`;x.fillRect(Math.random()*w,Math.random()*h,3,3)}
      if(line==='rainbow')['#ff8fb1','#ffd36b','#8fe39a','#7fc7ff','#c3a6ff'].forEach((c,i)=>{x.fillStyle=c;x.fillRect(0,h/2-25+i*10,w,10)});
      else if(line==='neon'){x.fillStyle='#ff4fd8';x.fillRect(0,h/2-5,w*.5,10)}else if(line){x.fillStyle=line;x.fillRect(0,h/2-6,w*.5,12)}}
    if(edge){x.fillStyle=edge;x.fillRect(0,16,w,6);x.fillRect(0,h-22,w,6)}});road.wrapS=THREE.RepeatWrapping;
  return{pave,curb,road}}
function slab(sc,x0,x1,z0,z1,y0,y1,mat){const m=new THREE.Mesh(new THREE.BoxGeometry(x1-x0,y1-y0,z1-z0),mat);m.position.set((x0+x1)/2,(y0+y1)/2,(z0+z1)/2);m.receiveShadow=true;sc.add(m);return m}
/** Sidewalks, curbs and the road from x0-half to x0+half. back: also the plaza behind the shops. */
function street(sc,th,x0,half,lin,o={}){const t=streetTex(th,lin);const L=half*2,em=th.night;
  const pm=(rx,ry)=>{const tt=t.pave.clone();tt.needsUpdate=true;tt.repeat.set(rx,ry);return new THREE.MeshStandardMaterial({map:tt,roughness:.85,metalness:em?.3:0})};
  slab(sc,x0-half,x0+half,-5,.6,0,H0,pm(L/3.2,1.75));if(o.plaza!==false)slab(sc,x0-5.2,x0+5.2,-9.6,-5,0,H0,pm(3.3,1.5));slab(sc,x0-half,x0+half,7.3,10.3,0,H0,pm(L/3.2,1));
  const cm=()=>{const tt=t.curb.clone();tt.needsUpdate=true;tt.repeat.set(L/2,1);const m=new THREE.MeshStandardMaterial({map:tt,roughness:.5});if(em){m.emissive=new THREE.Color('#ffffff');m.emissiveMap=tt;m.emissiveIntensity=.55}return m};
  slab(sc,x0-half,x0+half,.6,1,0,H0+.08,cm()).castShadow=true;slab(sc,x0-half,x0+half,6.9,7.3,0,H0+.08,cm()).castShadow=true;
  const rt=t.road.clone();rt.needsUpdate=true;rt.repeat.set(L/6,1);const rm=new THREE.MeshStandardMaterial({map:rt,roughness:.9});
  if(em){const glow=ctex(512,256,(x,w,h)=>{x.fillStyle='#000';x.fillRect(0,0,w,h);x.fillStyle='#ff4fd8';x.fillRect(0,h/2-5,w*.5,10);x.fillStyle='#3fe8ff';x.fillRect(0,16,w,6);x.fillRect(0,h-22,w,6)});glow.wrapS=THREE.RepeatWrapping;glow.repeat.set(L/6,1);rm.emissive=new THREE.Color('#ffffff');rm.emissiveMap=glow}
  slab(sc,x0-half,x0+half,1,6.9,0,.1,rm)}
function skyTex(th,lin){const t=tex(lin,8,512,(x,w,h)=>{const g=x.createLinearGradient(0,0,0,h);g.addColorStop(0,th.sky[0]);g.addColorStop(.42,th.sky[1]);g.addColorStop(.62,th.sky[2]);g.addColorStop(1,th.sky[2]);x.fillStyle=g;x.fillRect(0,0,w,h)});
  if(th.night){const s=tex(lin,1024,512,(x,w,h)=>{const g=x.createLinearGradient(0,0,0,h);g.addColorStop(0,th.sky[0]);g.addColorStop(.45,th.sky[1]);g.addColorStop(.7,th.sky[2]);g.addColorStop(1,th.sky[2]);x.fillStyle=g;x.fillRect(0,0,w,h);
    for(let i=0;i<500;i++){const y=Math.random()*h*.6;x.fillStyle=`rgba(255,255,255,${(.3+Math.random()*.7)*(1-y/(h*.6))})`;const r=Math.random()<.08?2:1;x.fillRect(Math.random()*w,y,r,r)}});return s}return t}
/** Hills, background trees, water, clouds: everything that is never built. */
function backdrop(sc,th,lin,o={}){const out={clouds:[],sway:[],tick:null};const T_=th.key;const X0=o.x0||0,half=o.half||110;
  const NC=c=>{const x=new THREE.Color(c);if(th.night&&!lin)x.convertSRGBToLinear();return x};const ground=new THREE.Mesh(new THREE.PlaneGeometry(o.groundW||400,300),M(NC(th.ground),.95));ground.rotation.x=-Math.PI/2;ground.position.x=X0;ground.receiveShadow=true;if(!o.noGround)sc.add(ground);
  if(T_!=='cloud'&&!o.noHills)for(const[x,z,r,i]of[[-30,-46,16,0],[8,-55,22,1],[42,-48,15,2],[-62,-42,14,1],[70,-46,16,0]]){const h=mesh(new THREE.SphereGeometry(r,32,16,0,Math.PI*2,0,Math.PI/2),M(NC(th.hills[i]),.95),X0+x,-1,z,sc);h.scale.y=T_==='galaxy'?.25:.5;h.castShadow=false}
  const rng=(()=>{let s=7+T_.length;return()=>((s=(s*9301+49297)%233280)/233280)})();
  const near=(x,z)=>Math.abs(x)<6.5&&z>-12;
  if(T_==='candy'){for(let x=-30;x<=30;x+=3)if(rng()<.75){const t=kenney(rng()<.5?'grass-trees':'grass-trees-tall',lin);t.scale.setScalar(TS*(.9+rng()*.4));t.position.set(X0+x+rnd(1),-.2,(Math.abs(x)<7?-12:-7.5)-rng()*4);t.rotation.y=rng()*6;sc.add(t)}
    for(let x=-33;x<=33;x+=6){if(Math.abs(x)<26)continue;const t=kenney('grass-trees',lin);t.position.set(X0+x+1.5,-.2,12.5+rng());sc.add(t)}}
  if(T_==='sea'){const wt=tex(lin,256,256,(x,w,h)=>{x.fillStyle='#2fb3ee';x.fillRect(0,0,w,h);for(let i=0;i<160;i++){x.fillStyle=`rgba(255,255,255,${.15+rng()*.3})`;x.fillRect(rng()*w,rng()*h,10+rng()*24,2)}});wt.wrapS=wt.wrapT=THREE.RepeatWrapping;wt.repeat.set(30,6);
    const sea=new THREE.Mesh(new THREE.PlaneGeometry(o.groundW||400,120),new THREE.MeshStandardMaterial({map:wt,roughness:.15,metalness:.1}));sea.rotation.x=-Math.PI/2;sea.position.set(X0,.06,-12-60);sc.add(sea);
    const wm=new THREE.MeshStandardMaterial({map:wt,roughness:.15});const pool=new THREE.Mesh(new THREE.PlaneGeometry(5.6,3.9),wm);pool.rotation.x=-Math.PI/2;pool.position.set(X0,H0+.012,-2.7);if(o.plaza!==false)sc.add(pool);
    for(let x=-30;x<=30;x+=5){if(Math.abs(x)<4)continue;out.sway.push(palm(X0+x+rnd(1),-9-rng()*2,.9+rng()*.4,sc))}
    for(let i=0;i<5;i++){const b=grp(X0-24+i*12+rnd(3),.1,-20-rng()*10,sc);mesh(new THREE.BoxGeometry(2,.5,.8),M(['#e5333d','#ffffff','#2a8fe0'][i%3],.5),0,.2,0,b);mesh(new THREE.CylinderGeometry(.04,.04,2,6),M('#7a4a2a',.6),0,1.3,0,b);
      mesh(new THREE.PlaneGeometry(.9,1.3),new THREE.MeshStandardMaterial({color:'#ffffff',side:THREE.DoubleSide}),.45,1.4,0,b);b.userData.bob=i;out.sway.push(b)}
    out.tick=(dt,t)=>{wt.offset.x=(wt.offset.x+dt*.01)%1;out.sway.forEach(b=>{if(b.userData.bob!=null){b.position.y=.1+Math.sin(t*1.4+b.userData.bob)*.08;b.rotation.z=Math.sin(t+b.userData.bob)*.05}})}}
  if(T_==='jungle'){const dark=paletteTex({h:110,s:1,l:-.08,k:.1},lin);for(let x=-34;x<=34;x+=3)if(rng()<.8){const t=kenney(rng()<.5?'grass-trees':'grass-trees-tall',lin);t.traverse(m=>{if(m.isMesh&&m.material.map)m.material.map=dark});
      t.scale.setScalar(TS*(1.1+rng()*.5));t.position.set(X0+x+rnd(1),-.2,(Math.abs(x)<7?-12.5:-8)-rng()*4);t.rotation.y=rng()*6;sc.add(t)}
    for(let x=-30;x<=30;x+=7)out.sway.push(palm(X0+x+rnd(2),-10-rng()*3,1.3+rng()*.4,sc));
    for(let x=-33;x<=33;x+=6){if(Math.abs(x)<24)continue;out.sway.push(palm(X0+x,12.5+rng(),1.1,sc))}
    const st=new THREE.Mesh(new THREE.PlaneGeometry(2.4,3.9),M('#3fb6e8',.15));st.rotation.x=-Math.PI/2;st.position.set(X0,H0+.012,-2.7);if(o.plaza!==false)sc.add(st);
    out.tick=(dt,t)=>out.sway.forEach(p=>p.rotation.z=Math.sin(t*1.3+p.userData.sway)*.03)}
  if(T_==='cloud'){for(let x=-60;x<=60;x+=4.5)puffs(sc,X0+x,-.6,-12-rng()*6,3,1.8+rng()*1.2);for(let x=-60;x<=60;x+=6)puffs(sc,X0+x,-.4,13+rng()*3,3,1.6);
    for(let i=0;i<5;i++)puffs(sc,X0-40+i*20+rnd(5),-1,-40-rng()*10,5,4+rng()*2);
    const rb=grp(X0+6,-1,-48,sc);['#ff4d4d','#ff9a3c','#ffd84a','#5cd65c','#4d9dff','#8e5bff'].forEach((c,k)=>{const r=new THREE.Mesh(new THREE.TorusGeometry(26-k*.9,.45,8,64,Math.PI),new THREE.MeshBasicMaterial({color:c,transparent:true,opacity:.7,fog:false}));rb.add(r)})}
  if(T_==='galaxy'){for(let i=0;i<26;i++){let x=rnd(34),z=-9-rng()*16;if(near(x,z))continue;const c=['#7fe3ff','#ff8fd0','#ffe066','#b8f27a'][i%4];const g=grp(X0+x,0,z,sc);
      for(let k=0;k<3;k++){const m=mesh(new THREE.OctahedronGeometry(.4+rng()*.4),new THREE.MeshStandardMaterial({color:c,emissive:c,emissiveIntensity:.45,roughness:.15}),k*.5-.5,.5,rnd(.4),g);m.scale.y=1.8+rng()}}
    for(let i=0;i<10;i++){const c=mesh(new THREE.TorusGeometry(1+rng()*1.5,.25,8,24),M('#2e2358',.9),X0+rnd(40),.05,-14-rng()*20,sc);c.rotation.x=Math.PI/2;c.scale.z=.4}
    [[-18,20,-60,'#ff9ec7',5,1],[22,26,-70,'#7fe3ff',3,0],[4,30,-80,'#ffe066',2,1]].forEach(([x,y,z,c,r,ring])=>{const p=mesh(new THREE.SphereGeometry(r,32,24),new THREE.MeshStandardMaterial({color:c,emissive:c,emissiveIntensity:.25,roughness:.6,fog:false}),X0+x,y,z,sc);
      if(ring){const rg=mesh(new THREE.TorusGeometry(r*1.6,r*.1,8,48),new THREE.MeshStandardMaterial({color:'#fff3c4',emissive:'#fff3c4',emissiveIntensity:.3,fog:false}),0,0,0,p);rg.rotation.x=1.25}})}
  if(th.clouds&&!o.noClouds)for(let i=0;i<9;i++){const g=grp(X0-60+i*15,13+rng()*5,-38-rng()*8,sc);for(let k=0;k<5;k++)mesh(new THREE.SphereGeometry(1.2+rng(),16,12),M(th.clouds,1),k*1.5-3,rng()*.7,rnd(1),g).castShadow=false;out.clouds.push(g)}
  return out}

/* ---------------------------------------------------------------- signs */
function signTexture(text,col,broken,lin){return tex(lin,512,128,(x,w,h)=>{x.fillStyle=broken?'#d9dbe0':'#fff8ec';rr(x,6,6,w-12,h-12,30);x.fill();x.lineWidth=10;x.strokeStyle=broken?'#8a8f9c':col;x.stroke();
  x.font=`${text.length>8?(text.length>11?44:54):72}px "Lilita One",sans-serif`;x.textAlign='center';x.textBaseline='middle';x.fillStyle=broken?'#6b6f7a':col;
  if(!broken){x.fillText(text,w/2,h/2+5);return}const cw=x.measureText(text).width/text.length;const sx=w/2-cw*text.length/2+cw/2;
  [...text].forEach((ch,i)=>{x.save();x.translate(sx+i*cw,h/2+5+(i%2?8:-7));x.rotate((i%3-1)*.25);x.fillText(ch,0,0);x.restore()})})}
const SIGN_COLS=[['#e2347f','#2e9b5f','#7b4dff','#ff7a1a'],['#1f6fd0','#0f8a8a','#e0602a','#b07a00'],['#e0602a','#2e8b3a','#c28a00','#9a5a2a'],['#e8457a','#4757c9','#7b4dff','#d99a00'],['#c23bd8','#1f9bd0','#e8457a','#5a4ad8']];

/** One shop (Kenney building + the area's dressing), standing on (0,0,0), front facing +z. */
function shopObj(w,i,lin){const th=THEMES[w];const g=new THREE.Group();const m=kenney(th.shops[i],lin);const sp=th.tint[i];
  if(sp||lin){const t=paletteTex(sp,lin);m.traverse(o=>{if(o.isMesh&&o.material.map)o.material.map=t})}
  m.position.y=.004;g.add(m);const b=bbox(g);dress(th,g,b,i,lin);return g}

/* ================================================================ the town scene */
/** opt: world (0-4), shops [{name,bad,fixed}], deco [b4,b5,b6,b7], big {name,fixed}, locked, linear (no sRGB), hooks {sfx,say,banner,coins}, lotHost (element), use (fn). */
function town(scene,cam,opt){const w=opt.world|0,th=THEMES[w],lin=!!opt.linear,U=opt.use||(()=>{}),HK=opt.hooks||{};const sfx=n=>{try{HK.sfx&&HK.sfx(n)}catch(e){}};
  const P=fn=>new Promise(fn).then(v=>{U();return v});const sleep=s=>P(r=>wait(s,r));
  scene.fog=new THREE.Fog(th.fog,40,115);if(th.night&&!lin)scene.fog.color.convertSRGBToLinear();scene.background=skyTex(th,lin);
  const L=th.light;scene.add(new THREE.HemisphereLight(L.hemi[0],L.hemi[1],L.hemi[2]));const rim=new THREE.DirectionalLight(L.rim[0],L.rim[1]);rim.position.set(-10,8,-16);scene.add(rim);
  const sun=new THREE.DirectionalLight(L.sun[0],L.sun[1]);sun.position.set(16,15,9);sun.castShadow=true;sun.shadow.mapSize.set(opt.shadow||2048,opt.shadow||2048);Object.assign(sun.shadow.camera,{left:-30,right:30,top:22,bottom:-22,near:1,far:80});sun.shadow.bias=-.0008;scene.add(sun);
  if(th.night){const amb=new THREE.AmbientLight(0x6a5aa8,.35);scene.add(amb)}
  const bgd=backdrop(scene,th,lin);street(scene,th,0,110,lin);
  if(w===0)[[-3.6,-.3,'#ff4f9a'],[3.6,-.3,'#7b4dff'],[-27,-.3,'#2ec4b6'],[27,-.3,'#ff9f1c']].forEach(([x,z,c],i)=>{const g=grp(x,H0,z,scene);const h=2.2+(i%2)*.4;mesh(new THREE.CylinderGeometry(.07,.07,h,10),M('#ffffff',.4),0,h/2,0,g);
    const hd=mesh(new THREE.CylinderGeometry(.6,.6,.2,32),[M('#ffffff',.3),new THREE.MeshStandardMaterial({map:swirlTex(c),roughness:.3}),new THREE.MeshStandardMaterial({map:swirlTex(c),roughness:.3})],0,h+.6,0,g);hd.rotation.x=Math.PI/2});
  /* spots */
  const spots=[];
  function makeSpot(objs,kind){const plane=new THREE.Plane(new THREE.Vector3(0,-1,0),999);const u={value:1};const mats=[];
    objs.forEach(o=>{scene.add(o);o.traverse(m=>{if(m.isMesh){
      (Array.isArray(m.material)?m.material:[m.material]).forEach(mt=>{if(mt.isMeshBasicMaterial&&mt.wireframe)return;mt.clippingPlanes=[plane];mt.clipShadows=true;if(!mt.transparent)mt.side=THREE.DoubleSide;if(mt.isMeshStandardMaterial)greyable(mt,u);mats.push(mt)})}})});
    const bb=new THREE.Box3();objs.forEach(o=>bb.union(bbox(o)));
    const s={objs,plane,u,mats,bb,kind,h:bb.max.y,rubble:[],built:false};s.cut=f=>{plane.constant=Math.max(.001,s.h*f)};
    s.colour=k=>{u.value=1-k;(s.glows||[]).forEach(m=>m.emissiveIntensity=k>.99?.95:0)};return s}
  function addRubble(s){const cols=['#8f8a9c','#a9a3b6','#b0855d','#7d778a'];const n=s.kind==='shop'?22:12;const{min,max}=s.bb;
    for(let i=0;i<n;i++){const k=.12+Math.random()*.2;const m=new THREE.Mesh(new THREE.BoxGeometry(k,k*.7,k),M(cols[i%4],.8));
      m.position.set(min.x+Math.random()*(max.x-min.x),H0+k*.3+(Math.random()<.35?s.h*.4:0),min.z+Math.random()*(max.z-min.z)+(i%3==0?.8:0));m.rotation.set(Math.random(),Math.random()*3,Math.random());m.castShadow=true;scene.add(m);s.rubble.push(m)}
    if(s.kind==='shop'){const p=new THREE.Mesh(new THREE.BoxGeometry(.12,1.6,.12),M('#c98a4b',.7));p.position.set((min.x+max.x)/2+.6,.5,max.z+.3);p.rotation.z=1;scene.add(p);s.rubble.push(p)}}
  const sc2=SIGN_COLS[w];
  const scr=(b,g)=>{if(b&&b!==g)return b;const a=[...(g||'')];if(a.length>2){const t=a[1];a[1]=a[a.length-2];a[a.length-2]=t}if(a.join('')===g&&a.length>1){const t=a[0];a[0]=a[1];a[1]=t}return a.join('')};
  (opt.shops||[]).slice(0,4).forEach((d,i)=>{d={name:d.name,bad:scr(d.bad,d.name),fixed:d.fixed};const g=shopObj(w,i,lin);g.position.set(SHOP_X[i],0,SHOP_Z);const s=makeSpot([g],'shop');s.d={good:d.name,bad:d.bad,col:sc2[i]};s.neon=g.userData.neon;spots[i]=s;
    const sg=new THREE.Mesh(new THREE.PlaneGeometry(2.3,.58),new THREE.MeshBasicMaterial({map:signTexture(d.bad,s.d.col,true,lin),transparent:true}));scene.add(sg);s.sign=sg;s.signMat=sg.material;s.home=()=>{sg.position.set(SHOP_X[i]-.2,H0+.2,s.bb.max.z+.75);sg.rotation.set(-1.2,0,.35)}});
  const decos=[4,5,6,7].map(k=>{const d=deco(th,k,lin);const s=makeSpot(d.objs,'deco');s.glows=d.glows;s.tick=d.tick;s.wideShot=d.wideShot;s.lamps=d.lamps;spots[k]=s;return s});
  {const b=bigObj(th,lin);const ng=b.g.userData.noGhost||[];ng.forEach(o=>b.g.remove(o));const s=makeSpot([b.g],'shop');ng.forEach(o=>b.g.add(o));s.glows=b.glows;s.tick=b.tick;s.big=true;spots[8]=s;
    if(b.g.userData.pinkFactory){s.mats.forEach(m=>{m.userData.pink=1});const base=s.colour;s.colour=k=>{base(k);s.mats.forEach(m=>{m.color.set(k>.5?'#ffd6ea':'#ffffff');if(m.emissive)m.emissive.set(k>.5?'#5a1838':'#000000')})}}
    const nm=(opt.big&&opt.big.name||'').toUpperCase();s.d={good:nm,col:sc2[0]};const sg=new THREE.Mesh(new THREE.PlaneGeometry(3.8,.95),new THREE.MeshBasicMaterial({map:signTexture(nm,sc2[0],false,lin),transparent:true}));
    sg.position.set(0,.35,s.bb.max.z+.75);sg.visible=false;scene.add(sg);s.sign=sg;s.signMat=sg.material;s.noSignStart=true}
  const CUT=[.42,.42,.42,.42,.3,.25,.12,.3,.4];
  function setState(k,on){const s=spots[k];if(!s)return;s.built=on;s.colour(on?1:0);s.cut(on?10:CUT[k]);s.rubble.forEach(r=>scene.remove(r));s.rubble=[];if(!on&&k!==6)addRubble(s);
    if(s.sign){if(on){if(s.big){s.sign.visible=true;s.sign.position.set(0,s.h*.62,s.bb.max.z+.08);s.sign.rotation.set(0,0,0)}else{s.signMat.map=signTexture(s.d.good,s.d.col,false,lin);s.sign.position.set((s.bb.min.x+s.bb.max.x)/2,s.h*.62,s.bb.max.z+.08);s.sign.rotation.set(0,0,0)}}
      else if(s.big)s.sign.visible=false;else{s.signMat.map=signTexture(s.d.bad,s.d.col,true,lin);s.home()}s.signMat.needsUpdate=true}}
  const fixedOf=k=>opt.locked?false:k<4?!!(opt.shops[k]&&opt.shops[k].fixed):k<8?(k===7&&th.extraBuilt)||!!(opt.deco&&opt.deco[k-4]):!!(opt.big&&opt.big.fixed);
  for(let k=0;k<9;k++)setState(k,fixedOf(k));
  /* Tiko */
  const tiko=makeTiko();tiko.scale.setScalar(1.05);tiko.position.set(-14,H0,-.3);scene.add(tiko);const hammer=mkHammer();hammer.scale.setScalar(.38);hammer.rotation.z=Math.PI;hammer.position.y=-.3;tiko.userData.armR.add(hammer);
  /* camera rig */
  const WIDE={t:v3(1.5,1.6,-3),yaw:-.62,pitch:.2,dist:21};const C={t:WIDE.t.clone(),yaw:WIDE.yaw,pitch:WIDE.pitch,dist:WIDE.dist};let drift=0,busy=false,userYaw=0;
  function camTo(o,dur,ease=E.inOut){const a={t:C.t.clone(),yaw:C.yaw,pitch:C.pitch,dist:C.dist};const b={t:o.t||a.t,yaw:o.yaw!=null?o.yaw:a.yaw,pitch:o.pitch!=null?o.pitch:a.pitch,dist:o.dist!=null?o.dist:a.dist};
    return P(res=>tween(dur,e=>{C.t.lerpVectors(a.t,b.t,e);C.yaw=a.yaw+(b.yaw-a.yaw)*e;C.pitch=a.pitch+(b.pitch-a.pitch)*e;C.dist=a.dist+(b.dist-a.dist)*e},{ease,done:res}))}
  const wide=(dur=1.3)=>{drift=0;return camTo(WIDE,dur)};
  function shotFor(s){const c=new THREE.Vector3();s.bb.getCenter(c);const span=Math.max(5,s.bb.max.x-s.bb.min.x);const side=c.x>=0?.55:-.55;return{t:v3(c.x,s.h*.42+.7,c.z+.6),yaw:side,pitch:.2,dist:8.5+span*.75+s.h*.7,side}}
  function focus(s){if(s.wideShot){drift=0;return camTo({t:v3(0,1.6,3.5),yaw:-.75,pitch:.16,dist:19},1.2)}const o=shotFor(s);drift=-o.side*.07;return camTo(o,1.2)}
  /* screen effects */
  const host=opt.lotHost;function toScreen(v){const p=v.clone().project(cam);return[(p.x+1)/2*100,(1-p.y)/2*100]}
  const lots=[];function lottieAt(name,wp,size){if(!host||!window.lottie||!window.LOTJ||!LOTJ[name])return;const[x,y]=toScreen(wp);const d=document.createElement('div');d.style.cssText=`position:absolute;pointer-events:none;z-index:3;transform:translate(-50%,-50%);left:${x}%;top:${y}%;width:${size}%;aspect-ratio:1`;host.appendChild(d);
    try{const a=lottie.loadAnimation({container:d,renderer:'svg',loop:false,autoplay:true,animationData:JSON.parse(JSON.stringify(LOTJ[name]))});lots.push(d);a.addEventListener('complete',()=>{a.destroy();d.remove()})}catch(e){d.remove()}}
  function puff(p,n=7){for(let i=0;i<n;i++){const m=new THREE.Mesh(geo.puff||(geo.puff=new THREE.SphereGeometry(.22,10,8)),new THREE.MeshBasicMaterial({color:th.night?'#d9c8ff':'#fff7fb',transparent:true,opacity:.9}));
    m.position.copy(p);addPart(m,scene,{v:v3(rnd(3),Math.random()*1.6,rnd(2)+.6),drag:2,life:.7,fade:true,grow:1.8})}}
  /* Tiko moves */
  let hopping=false;
  function walkTo(x,z){return P(res=>{const a=tiko.position.clone(),b=v3(x,H0,z);const d=a.distanceTo(b);if(d<.2){res();return}const dur=Math.min(2.2,.45+d*.11);const hops=Math.max(2,Math.round(d/1.4));
    tiko.rotation.y=Math.atan2(b.x-a.x,b.z-a.z);hopping=true;let last=-1;tween(dur,(e,k)=>{tiko.position.lerpVectors(a,b,k);const ph=k*hops;tiko.position.y=H0+Math.abs(Math.sin(ph*Math.PI))*.55;const n=Math.floor(ph);if(n!==last){last=n;sfx('hop')}},{ease:E.lin,done:()=>{hopping=false;tiko.position.y=H0;res()}})})}
  function faceTo(x,z){const a=tiko.rotation.y,b=Math.atan2(x-tiko.position.x,z-tiko.position.z);tween(.25,e=>tiko.rotation.y=a+(b-a)*e)}
  function swing(){const arm=tiko.userData.armR;tween(.22,(e,k)=>{arm.rotation.x=-Math.sin(k*Math.PI)*2.2},{ease:E.lin})}
  function brickArc(from,to,dur=.5){return P(res=>{const br=new THREE.Mesh(geo.brick2||(geo.brick2=new THREE.BoxGeometry(.5,.25,.25)),M('#d9563b',.6));br.castShadow=true;scene.add(br);const sp=rnd(14);
    tween(dur,(e,k)=>{br.position.lerpVectors(from,to,k);br.position.y+=Math.sin(k*Math.PI)*3;br.rotation.set(sp*k,sp*.6*k,0)},{ease:E.lin,done:()=>{scene.remove(br);res()}})})}
  function scaffold(s){const g=new THREE.Group();const wd=M('#c98b4a',.7);const{min,max}=s.bb;const ww=max.x-min.x+.5,dz=max.z-min.z+.5,hh=s.h+.4;
    for(const sx of[-ww/2,ww/2])for(const sz of[-dz/2,dz/2])mesh(new THREE.BoxGeometry(.12,hh,.12),wd,sx,hh/2,sz,g);
    for(let y=.9;y<hh;y+=1.1){for(const sz of[-dz/2,dz/2])mesh(new THREE.BoxGeometry(ww+.25,.1,.32),wd,0,y,sz,g);for(const sx of[-ww/2,ww/2])mesh(new THREE.BoxGeometry(.32,.1,dz+.25),wd,sx,y,0,g)}
    g.position.set((min.x+max.x)/2,0,(min.z+max.z)/2);g.scale.y=.001;scene.add(g);return g}
  let buildLine=null;
  function makeGhost(s){const g=new THREE.Group();const plane=new THREE.Plane(v3(0,1,0),0);
    const fill=new THREE.MeshBasicMaterial({color:th.ghost,transparent:true,opacity:.07,depthWrite:false,blending:THREE.AdditiveBlending,clippingPlanes:[plane],side:THREE.DoubleSide});
    const line=new THREE.LineBasicMaterial({color:'#ffffff',transparent:true,opacity:.85,clippingPlanes:[plane]});line.color.set(th.ghost).lerp(new THREE.Color('#ffffff'),.5);
    s.objs.forEach(o=>{const c=o.clone(true);c.traverse(m=>{if(m.isMesh){m.material=fill;m.castShadow=false;m.receiveShadow=false;m.add(new THREE.LineSegments(new THREE.EdgesGeometry(m.geometry,28),line))}});g.add(c)});
    scene.add(g);g.userData={plane,fill,line};return g}
  function makeRing(s){const{min,max}=s.bb;const pad=.12;const pts=[[min.x-pad,min.z-pad],[max.x+pad,min.z-pad],[max.x+pad,max.z+pad],[min.x-pad,max.z+pad]].map(p=>v3(p[0],0,p[1]));
    const r=new THREE.LineLoop(new THREE.BufferGeometry().setFromPoints(pts),new THREE.LineBasicMaterial({color:'#ffffff',transparent:true}));
    const glow=new THREE.Mesh(new THREE.PlaneGeometry(max.x-min.x+.4,max.z-min.z+.4),new THREE.MeshBasicMaterial({color:th.ghost,transparent:true,opacity:.35,blending:THREE.AdditiveBlending,depthWrite:false,side:THREE.DoubleSide}));
    glow.rotation.x=-Math.PI/2;glow.position.set((min.x+max.x)/2,0,(min.z+max.z)/2);const g=new THREE.Group();g.add(r);g.add(glow);scene.add(g);g.userData={min,max,acc:0};return g}
  function emitSparks(dt){if(!buildLine)return;const{min,max}=buildLine.userData;buildLine.userData.acc+=dt;
    while(buildLine.userData.acc>.025){buildLine.userData.acc-=.025;const e=Math.random()*4|0,k=Math.random();const x=e<2?min.x+(max.x-min.x)*k:(e===2?min.x:max.x),z=e>=2?min.z+(max.z-min.z)*k:(e===0?min.z:max.z);
      const sp=sprite(glowTex,Math.random()<.5?0xfff1a8:new THREE.Color(th.ghost).getHex(),.28);sp.position.set(x,buildLine.position.y,z);addPart(sp,scene,{v:v3(rnd(2.5),1+Math.random()*2.5,rnd(2.5)),g:7,life:.55,fade:true,shrink:true})}}
  function say(t){try{HK.say&&HK.say(t)}catch(e){}}
  /* the build */
  async function buildSpot(s,cost,line){const c=new THREE.Vector3();s.bb.getCenter(c);const front=s.bb.max.z;
    sfx('whoosh');focus(s);const side=c.x>8?-1:1;
    await walkTo(c.x+side*(s.kind==='shop'?(s.bb.max.x-s.bb.min.x)/2+1.1:1.6),Math.min(front+1.3,6));faceTo(c.x,c.z);if(line)say(line);
    s.rubble.forEach(r=>addPart(r,scene,{v:v3(rnd(5),4+Math.random()*3,rnd(3)+1),av:v3(rnd(9),rnd(9),rnd(9)),g:12,life:.9,shrink:true}));s.rubble=[];
    if(s.sign&&!s.noSignStart)tween(.4,e=>{s.sign.position.y=.35+e*.6;s.signMat.opacity=1-e},{done:()=>{s.sign.visible=false}});
    puff(v3(c.x,.4,front),12);lottieAt('dust',v3(c.x,.2,front),22);sfx('thunk');await sleep(.45);
    tween(.5,e=>s.colour(e*.999));let cur=.06;const ghost=makeGhost(s);const ring=makeRing(s);buildLine=ring;
    const setCut=f=>{s.cut(f);ghost.userData.plane.constant=-s.h*f;ring.position.y=s.h*f};setCut(cur);
    ghost.scale.y=.01;sfx('whoosh');tween(.6,(e,k)=>{ghost.scale.y=Math.max(.01,e);ghost.userData.fill.opacity=.16*(k<.6?(Math.random()<.5?.4:1):1)},{ease:E.outBack});
    let sc=null;if(s.kind==='shop'){sc=scaffold(s);sfx('clack');tween(.55,e=>sc.scale.y=Math.max(.001,e),{ease:E.outBack})}
    await sleep(.5);
    for(let i=1;i<=cost;i++){const from=tiko.position.clone().add(v3(0,1.4,0));const to=v3(c.x+rnd(1.2),Math.max(.5,s.h*cur),front-.2);
      try{HK.brick&&HK.brick(i)}catch(e){}await brickArc(from,to,.5);sfx('thunk');swing();puff(to,6);lottieAt('dust',to,16);
      const a=cur,b=i===cost?1:.06+(.94*i/cost);cur=b;tween(.5,e=>setCut(a+(b-a)*e),{ease:E.outCubic});shake=Math.max(shake,.05);await sleep(.42)}
    if(sc)tween(.45,e=>{sc.position.y=-e;sc.scale.y=Math.max(.001,1-e)},{ease:E.inCubic,done:()=>scene.remove(sc)});
    s.cut(10);buildLine=null;scene.remove(ring);flash(v3(c.x,s.h*.6,c.z+1),9,new THREE.Color(th.ghost).getHex());s.colour(1);
    tween(.6,e=>{ghost.userData.fill.opacity=.16*(1-e);ghost.userData.line.opacity=.85*(1-e);ghost.scale.setScalar(1+e*.06)},{done:()=>scene.remove(ghost)});
    const objs=s.objs;const base=objs.map(o=>o.scale.clone());
    tween(.8,(e,k)=>{const q=1+Math.sin(k*Math.PI*3)*(1-k)*.12;objs.forEach((o,j)=>o.scale.set(base[j].x/Math.sqrt(q),base[j].y*q,base[j].z/Math.sqrt(q)))},{ease:E.lin,done:()=>objs.forEach((o,j)=>o.scale.copy(base[j]))});
    if(!s.wideShot){const o=shotFor(s);drift=0;camTo({t:o.t,yaw:o.yaw*.35,pitch:.1,dist:o.dist*.82},1.1,E.outCubic)}
    sfx('pop');starsAt(v3(c.x,s.h*.7,front),16,0xffe27a,1);lottieAt('sparkle',v3(c.x,s.h*.6,c.z),30);
    if(s.sign&&s.d){s.sign.visible=true;s.signMat.opacity=1;s.signMat.map=signTexture(s.d.good,s.d.col,false,lin);s.signMat.needsUpdate=true;const to=v3(c.x,s.h*.62,front+.08);const from=s.sign.position.clone();
      tween(.7,(e,k)=>{s.sign.position.lerpVectors(from,to,e);s.sign.rotation.set(-1.2*(1-e),k*Math.PI*2,.35*(1-e))},{ease:E.outBack})}
    s.built=true;tiko.userData.cheer=T;await sleep(.9);
    sfx('fanfare');confettiAt(v3(c.x,s.h+1.5,front+1),120,3);lottieAt('confetti',v3(c.x,s.h*.5,front),36);try{HK.coins&&HK.coins()}catch(e){}}
  /** Already built (a second task on the same shop): Tiko paints it, colours sweep up. */
  async function repaint(s,cost,line){const c=new THREE.Vector3();s.bb.getCenter(c);const front=s.bb.max.z;sfx('whoosh');focus(s);tween(.5,e=>s.u.value=.6*e);
    await walkTo(c.x+(c.x>8?-1:1)*((s.bb.max.x-s.bb.min.x)/2+1.1),Math.min(front+1.3,6));faceTo(c.x,c.z);if(line)say(line);
    for(let i=1;i<=cost;i++){await brickArc(tiko.position.clone().add(v3(0,1.4,0)),v3(c.x+rnd(1.2),s.h*(i/cost)*.8+.4,front-.2),.45);sfx('thunk');swing();puff(v3(c.x+rnd(1),s.h*.5,front),5);
      const a=.6*(1-(i-1)/cost),b=.6*(1-i/cost);tween(.4,e=>s.u.value=a+(b-a)*e);shake=Math.max(shake,.04);await sleep(.35)}
    s.colour(1);flash(v3(c.x,s.h*.6,c.z+1),9,new THREE.Color(th.ghost).getHex());sfx('pop');starsAt(v3(c.x,s.h*.7,front),16,0xffe27a,1);lottieAt('sparkle',v3(c.x,s.h*.6,c.z),30);
    const o=shotFor(s);camTo({t:o.t,yaw:o.yaw*.35,pitch:.1,dist:o.dist*.82},1.1,E.outCubic);tiko.userData.cheer=T;await sleep(.9);sfx('fanfare');confettiAt(v3(c.x,s.h+1.5,front+1),120,3);lottieAt('confetti',v3(c.x,s.h*.5,front),36)}
  async function build(k,cost,line){const s=spots[k];if(!s||busy)return;busy=true;try{if(s.built)await repaint(s,Math.max(1,cost|0),line);else await buildSpot(s,Math.max(1,cost|0),line);
      await sleep(1.4);wide(1.4);walkTo(tiko.position.x,-.3);await sleep(1.2)}finally{busy=false;U()}}
  async function finale(o={}){const s=spots[8];if(!s||s.built)return;busy=true;try{await sleep(.3);
      try{HK.banner&&HK.banner('ALL TASKS DONE',(s.d.good||'').replace(/^./,m=>m),'Opening now!')}catch(e){}await sleep(1.4);
      await buildSpot(s,6,o.line||'We did it! The whole street is fixed!');drift=0;walkTo(2.2,-1.4).then(()=>faceTo(2.2,30));
      await camTo({t:v3(0,2.8,-4.5),yaw:-.7,pitch:.1,dist:15.5},1.1);camTo({yaw:.7,pitch:.16,dist:15},5.5,E.inOut);sfx('grand');
      try{HK.banner&&HK.banner('STREET COMPLETE',o.street||'',o.small||'')}catch(e){}
      for(let i=0;i<10;i++){wait(i*.5,()=>{const p=v3(rnd(26),6+Math.random()*5,-3+rnd(4));confettiAt(p,90,2.5);starsAt(p,14,[0xffe27a,0xff7eb6,0x7fe3ff][i%3],1.4);flash(p,6)})}
      lottieAt('confetti',v3(-8,4,0),40);lottieAt('confetti',v3(8,4,0),40);tiko.userData.cheer=T;await sleep(4.6);await camTo({t:v3(0,1.5,1),yaw:0,pitch:.55,dist:30},2.2);await sleep(.8);await wide(1.8)}finally{busy=false;U()}}
  /* per frame */
  const lampGlows=[];let lampsOn=false;const camP=v3(),camT=v3();
  function update(dt,t){if(!hopping){tiko.position.y=H0+Math.abs(Math.sin(t*2.2))*.06;const ch=T-(tiko.userData.cheer||-9);
      if(ch<1.6){tiko.position.y=H0+Math.abs(Math.sin(ch*Math.PI*2.5))*.7;tiko.userData.armL.rotation.z=-2.4;tiko.userData.armR.rotation.z=2.4}else{tiko.userData.armL.rotation.z=-.5;tiko.userData.armR.rotation.z=.5}}
    bgd.clouds.forEach((c,i)=>{c.position.x+=dt*(.4+i*.05);if(c.position.x>50)c.position.x=-50});bgd.tick&&bgd.tick(dt,t);
    spots.forEach(s=>{if(s&&s.tick)s.tick(dt,t,s.built,scene,s);if(s&&s.neon&&s.built)s.neon.emissiveIntensity=.7+Math.sin(t*3)*.25});
    const L6=spots[6];if(L6&&L6.built&&!lampsOn){lampsOn=true;L6.objs.forEach(o=>{const g=sprite(glowTex,new THREE.Color(th.lamp.glow).getHex(),2.2);o.userData.head.getWorldPosition(g.position);scene.add(g);lampGlows.push(g)})}
    lampGlows.forEach((g,i)=>g.material.opacity=.75+Math.sin(t*3+i)*.15);
    emitSparks(dt);C.yaw+=drift*dt;const sway=busy?0:Math.sin(t*.18)*.07;const y=C.yaw+sway+userYaw,p=C.pitch+(busy?0:Math.sin(t*.13)*.015);
    const D=C.dist*Math.max(1,Math.min(1.25,1+(cam.aspect-1.78)*.3));camT.copy(C.t);camP.set(C.t.x+Math.sin(y)*Math.cos(p)*D,C.t.y+Math.sin(p)*D,C.t.z+Math.cos(y)*Math.cos(p)*D);cam.position.copy(camP);cam.lookAt(camT)}
  function dispose(){lots.forEach(d=>d.remove())}
  return{spots,tiko,build,finale,update,dispose,walkTo,wide,isBusy:()=>busy,drag(dx){userYaw=Math.max(-.6,Math.min(.6,userYaw-dx*.006))},
    hello(){wait(.6,()=>walkTo(-3,-.3))},theme:th}}

return{load,ready:()=>!!BASE,town,shopObj,bigObj,deco,street,backdrop,skyTex,greyable,paletteTex,signTexture,THEMES,SHOP_X,SHOP_Z,BIG_Z,H0,TS}})();
setTimeout(()=>{try{T3.load().catch(()=>{})}catch(e){}},30);
