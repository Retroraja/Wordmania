/* Story cinematics (ported from the approved preview design/animations/story-preview.html). */
'use strict';
function swirlTex(c1){return ctex(128,128,x=>{x.fillStyle='#fff';x.beginPath();x.arc(64,64,64,0,7);x.fill();x.strokeStyle=c1;x.lineWidth=13;x.lineCap='round';x.beginPath();
  for(let a=0;a<6*Math.PI;a+=.1){const r=a*3.1;x.lineTo(64+Math.cos(a)*r,64+Math.sin(a)*r)}x.stroke()})}
function rm2(o){if(o&&o.parent)o.parent.remove(o)}
function sceneStory(w,data){
  const scene=w.scene,cam=w.cam;cam.fov=42;cam.near=.1;cam.far=900;cam.aspect=W/H;cam.updateProjectionMatrix();document.body.classList.add('story');const BR=Math.max(0,data.bricks|0);
  scene.fog=new THREE.Fog(0xffd6ea,80,200);
  const hemi=new THREE.HemisphereLight(0xffffff,0xb8a8d8,.75);scene.add(hemi);
  const sun=new THREE.DirectionalLight(0xffffff,.95);sun.castShadow=true;sun.shadow.mapSize.set(2048,2048);Object.assign(sun.shadow.camera,{left:-34,right:34,top:34,bottom:-34,near:.5,far:90});sun.shadow.bias=-.0006;scene.add(sun);scene.add(sun.target);
  const GAP=60,GREYS=['#b4b0bd','#d2cdd8','#e9e6ec'];
  const box=(w,h,d,c,x,y,z,r=.8)=>{const m=new THREE.Mesh(new THREE.BoxGeometry(w,h,d),typeof c==='string'?std(c,r):c);m.position.set(x,y,z);m.receiveShadow=true;scene.add(m);return m};
  const water=new THREE.Mesh(new THREE.PlaneGeometry(760,320,100,40),new THREE.MeshStandardMaterial({color:'#4cc3f5',roughness:.25,metalness:.1}));water.rotation.x=-Math.PI/2;water.position.set(120,-.8,0);scene.add(water);
  const wpos0=water.geometry.attributes.position.array.slice();
  const LANDS=[
   {key:'candy',name:'Candy Land',street:'Candy Street',levels:'Levels 1–10',ground:'#9be27a',side:'#c98b5a',road:'#5b6178',pave:'#ffd0e4',water:'#6fd0ff',sky:['#ffa9d4','#ffd0e8','#ffe7f3'],
    b:[['SWEETS','SWTEES','#ffd166','#f2861e',3.6,4.0],['BAKERY','BAKREY','#ff9ec7','#e8457a',3.8,3.5],['CANDY FACTORY','CNADY FATCORY','#c3a6ff','#7b4dff',5.4,6.4],['TOYS','TYOS','#7fe3ff','#2a8fe0',3.6,4.7],['CAFE','CEFA','#b8f27a','#1e8f5a',3.6,3.7]],
    v:'Professor Scramble',vc:'#7b4dff',prize:'🏭 Free booster every day',
    say:{hello:'Welcome to Candy Street! The sweetest town in Wordnoid.',taunt:'Ha! Your pretty words belong to ME now!',help:'He scrambled every sign! Help me fix Candy Street!',
      boss:'You will never unscramble my town!',lose:'Nooo! The Word Eater will get you for this!',win:'We did it! The colours are back!'}},
   {key:'sea',name:'Under the Sea',street:'The Harbour',levels:'Levels 11–20',ground:'#f3d9a0',side:'#d9b77a',road:'#8b7d6b',pave:'#ece7dc',water:'#2fb3ee',sky:['#8fd8ff','#c4ecff','#e8f8ff'],
    b:[['FISH','FSIH','#7fe3ff','#1f6fd0',3.4,3.8],['BOATS','BTOAS','#ffd166','#e0602a',3.6,4.4],['LIGHTHOUSE','LGIHTHOSUE','#ffffff','#e5333d',4.6,6.2],['SHELLS','SLEHLS','#ff9ec7','#c2187a',3.6,4.0],['CAFE','CFAE','#b8f27a','#16865a',3.4,3.6]],
    v:'Captain Inkbeard',vc:'#c2187a',prize:'🗼 Lighthouse hint every day',
    say:{hello:'Smell that sea air! Welcome to the Harbour.',taunt:'Har har har! Yer words be MINE now!',help:'He inked every sign! Let’s clean up the Harbour!',
      boss:'Arr! Ye’ll never wash off my ink!',lose:'Blub... The Word Eater will sink ye yet!',win:'The Lighthouse is shining again!'}},
   {key:'jungle',name:'Fruit Jungle',street:'Jungle Park',levels:'Levels 21–30',ground:'#6fcf5a',side:'#9a6a3a',road:'#a0754a',pave:'#f2e2b0',water:'#3fd1c0',sky:['#9ee6a8','#d4f5c9','#f3ffe8'],
    b:[['FRUIT','FIRUT','#ffb347','#e0602a',3.6,3.9],['JUICE','JICUE','#ffe066','#d99a00',3.4,4.3],['TREEHOUSE','TERHEOUSE','#b8f27a','#2e8b3a',5.0,6.3],['SEEDS','SDEES','#ff9ec7','#c2187a',3.4,3.8],['MARKET','MRAKET','#7fe3ff','#1f6fd0',3.8,4.0]],
    v:'Bonkers the Baboon',vc:'#9a5a2a',prize:'🍌 Fruit basket every day',
    say:{hello:'Welcome to Fruit Jungle! Watch out for falling coconuts.',taunt:'Ooh-ooh! Bonkers smash your silly words!',help:'He cracked every sign! Help me fix the jungle!',
      boss:'Bonkers never loses! Ooh-ooh-aah!',lose:'Ow, my head! The Word Eater will get you!',win:'The flowers are blooming again!'}},
   {key:'cloud',name:'Cloud Kingdom',street:'Cloud Hill',levels:'Levels 31–40',ground:'#eef4ff',side:'#cfdcf5',road:'#9fb0d6',pave:'#ffffff',water:'#9fc8ff',sky:['#a9c8ff','#d6e6ff','#f4f8ff'],
    b:[['KITES','KTIES','#ff9ec7','#e8457a',3.4,4.0],['HATS','HTAS','#ffd166','#f2861e',3.4,3.6],['SKY CASTLE','SKY CSATLE','#c3e3ff','#4757c9',5.2,6.6],['BELLS','BLELS','#b8f27a','#1e8f5a',3.4,4.2],['TEA ROOM','TAE ROMO','#c3a6ff','#7b4dff',3.8,3.8]],
    v:'Grumble Cloud',vc:'#4a5068',prize:'🌈 Rainbow spin every day',
    say:{hello:'Up in the clouds! Welcome to the Cloud Kingdom.',taunt:'Grrr... I’ll freeze every word solid!',help:'He froze every sign! Let’s melt this mess!',
      boss:'Grrr! Nobody stops a storm!',lose:'Pffft... the Word Eater... will get you...',win:'The sun is out! Look, a rainbow!'}},
   {key:'galaxy',name:'Sunny Galaxy',street:'Star Square',levels:'Levels 41–50',ground:'#d9c8ff',side:'#a98fe0',road:'#6b5aa8',pave:'#f1e8ff',water:'#8f7bff',sky:['#c9a8ff','#e6d8ff','#fff3fb'],
    b:[['ROCKETS','R_CK_TS','#ff9ec7','#e8457a',3.6,4.2],['COMETS','C_M_TS','#7fe3ff','#2a8fe0',3.4,3.8],['STAR TOWER','ST_R T_W_R','#ffe066','#d99a00',5.0,6.8],['ORBIT','_RB_T','#b8f27a','#1e8f5a',3.4,4.0],['LAB','L_B','#ffb347','#e0602a',3.4,3.6]],
    v:'The Word Eater',vc:'#8e3bc4',prize:'⭐ Star chest every day',
    say:{hello:'The Sunny Galaxy! The last land to save.',taunt:'MMMM... WORDS. I WILL EAT THEM ALL!',help:'He ate the letters off the signs! Last land, let’s go!',
      boss:'NOTHING CAN STOP MY HUNGER!',lose:'Urrp... too... many... words...',win:'Every word is free! We saved Wordnoid!'}}];
  // ---------------- buildings
  const GREY=new THREE.Color('#a3a7b3'),GREY2=new THREE.Color('#7d8291'),D=3.2,BZ=-6.4,flags=[];
  function building(d){const g=new THREE.Group();g.position.set(d.x,0,BZ);scene.add(g);const inner=new THREE.Group();g.add(inner);
    const cB=new THREE.Color(d.col),cR=new THREE.Color(d.roof);const bodyMat=std('#a3a7b3',.75),roofMat=std('#7d8291',.6);
    const add=(m,x,y,z)=>{m.position.set(x,y,z);m.castShadow=true;m.receiveShadow=true;inner.add(m);return m};
    add(new THREE.Mesh(new THREE.BoxGeometry(d.w,d.h,D),bodyMat),0,d.h/2,0);
    const tri=new THREE.Shape();tri.moveTo(-d.w/2-.3,0);tri.lineTo(d.w/2+.3,0);tri.lineTo(0,1.5);tri.closePath();
    const rg=new THREE.ExtrudeGeometry(tri,{depth:D+.5,bevelEnabled:false});rg.translate(0,0,-(D+.5)/2);add(new THREE.Mesh(rg,roofMat),0,d.h,0);
    if(!d.big)add(new THREE.Mesh(new THREE.BoxGeometry(.45,1.2,.45),roofMat),d.w*.28,d.h+.95,-.5);
    add(new THREE.Mesh(new THREE.BoxGeometry(d.w+.2,.35,D+.2),std('#fff4fa',.6)),0,.17,0);
    const door=add(new THREE.Mesh(new THREE.BoxGeometry(d.big?1.6:.9,d.big?2.2:1.4,.12),new THREE.MeshStandardMaterial({color:'#7a4a2a',roughness:.6,emissive:'#ffcf5a',emissiveIntensity:0})),0,d.big?1.25:.85,D/2+.04);
    const awnMat=new THREE.MeshStandardMaterial({map:ctex(128,32,x=>{for(let i=0;i<8;i++){x.fillStyle=i%2?'#ffffff':d.roof;x.fillRect(i*16,0,16,32)}}),roughness:.6,color:'#999999'});
    if(!d.big){const awn=add(new THREE.Mesh(new THREE.BoxGeometry(d.w*.86,.1,.85),awnMat),0,1.85,D/2+.42);awn.rotation.x=.28}
    let flag=null;if(d.big){add(new THREE.Mesh(new THREE.CylinderGeometry(.05,.05,2,8),std('#ffffff',.4)),0,d.h+2.4,0);
      flag=new THREE.Mesh(new THREE.PlaneGeometry(1.1,.65,8,1),new THREE.MeshStandardMaterial({color:'#999999',side:THREE.DoubleSide,roughness:.6}));flag.position.set(.58,d.h+3.05,0);inner.add(flag);flag.userData.p0=flag.geometry.attributes.position.array.slice();flags.push(flag)}
    const glass=[];const fm=std('#ffffff',.5);
    for(let y=2.75;y<d.h-.35;y+=1.15)for(const xx of[-d.w*.26,d.w*.26]){add(new THREE.Mesh(new THREE.BoxGeometry(.8,.8,.08),fm),xx,y,D/2+.04);
      const gm=new THREE.MeshStandardMaterial({color:'#2f3b5c',emissive:'#ffc55a',emissiveIntensity:0,roughness:.2});add(new THREE.Mesh(new THREE.BoxGeometry(.62,.62,.1),gm),xx,y,D/2+.07);glass.push(gm)}
    const sw=Math.min(d.w*.74,3.9);const signMat=new THREE.MeshBasicMaterial({map:signTex(d.sign,d,false),transparent:true});
    const sign=new THREE.Mesh(new THREE.PlaneGeometry(sw,sw*.22),signMat);sign.position.set(0,d.h+.45,D/2+.27);inner.add(sign);
    const api={d,g,inner,sign,signMat,door,set(k){bodyMat.color.copy(GREY).lerp(cB,k);roofMat.color.copy(GREY2).lerp(cR,k);awnMat.color.set('#999999').lerp(new THREE.Color('#ffffff'),k);
      if(flag)flag.material.color.set('#999999').lerp(cR,k);glass.forEach((m,i)=>m.emissiveIntensity=k>=(i+1)/(glass.length+1)?.9:0)},
      setSign(t,broken){if(api.cur===t+broken)return;api.cur=t+broken;api.signMat.map=signTex(t,d,broken);api.signMat.needsUpdate=true},
      front(){return v3(d.x,d.h+.45,BZ+D/2+.3)}};
    return api}
  // ---------------- decorations
  const white=std('#ffffff',.4),sway=[],bob=[],planets=[];
  function lolli(x,z,c,s){const g=new THREE.Group();const st=new THREE.Mesh(new THREE.CylinderGeometry(.06,.06,2.2,8),white);st.position.y=1.1;g.add(st);
    const t=swirlTex(c);const disc=new THREE.Mesh(new THREE.CylinderGeometry(.75,.75,.22,32),[std(c,.3),new THREE.MeshStandardMaterial({map:t,roughness:.3}),new THREE.MeshStandardMaterial({map:t,roughness:.3})]);
    disc.rotation.x=Math.PI/2;disc.position.y=2.6;g.add(disc);g.position.set(x,0,z);g.scale.setScalar(s);g.userData.ph=Math.random()*6;rich(g);scene.add(g);sway.push(g)}
  function gum(x,z,c){const m=new THREE.Mesh(new THREE.SphereGeometry(.45,20,14),std(c,.25));m.scale.y=.75;m.position.set(x,.33,z);m.castShadow=true;scene.add(m)}
  function lamp(x,z,c){box(.12,2.6,.12,c,x,1.3,z);const l=new THREE.Mesh(new THREE.SphereGeometry(.25,16,12),new THREE.MeshStandardMaterial({color:'#fff3c4',emissive:'#ffd36b',emissiveIntensity:.8}));l.position.set(x,2.7,z);scene.add(l)}
  function barrel(x,z){const m=new THREE.Mesh(new THREE.CylinderGeometry(.4,.4,.9,16),std('#a0622d',.7));m.position.set(x,.45,z);m.castShadow=true;scene.add(m);
    for(const y of[.15,.75]){const r=new THREE.Mesh(new THREE.TorusGeometry(.41,.04,6,20),std('#3d3a5c',.5));r.rotation.x=Math.PI/2;r.position.set(x,y,z);scene.add(r)}}
  function palm(x,z,s=1){const g=new THREE.Group();const tm=std('#9a6a3a',.8);let y=0,ox=0;for(let i=0;i<6;i++){const c=new THREE.Mesh(new THREE.CylinderGeometry(.18-i*.015,.22-i*.015,.8,10),tm);ox+=.06*i;c.position.set(ox*.3,y+.4,0);c.rotation.z=-.05*i;g.add(c);y+=.75}
    const lm=std('#2e9e48',.6);for(let i=0;i<7;i++){const l=new THREE.Mesh(new THREE.SphereGeometry(1,12,8),lm);l.scale.set(1.6,.1,.45);const a=i/7*Math.PI*2;l.position.set(ox*.3+Math.cos(a)*1.3,y-.25,Math.sin(a)*1.3);l.rotation.set(0,-a,-.45);g.add(l)}
    for(let i=0;i<3;i++){const c=new THREE.Mesh(new THREE.SphereGeometry(.17,10,8),std('#6b3f1f',.6));c.position.set(ox*.3+Math.cos(i*2)*.25,y-.35,Math.sin(i*2)*.25);g.add(c)}
    g.position.set(x,0,z);g.scale.setScalar(s);g.userData.ph=Math.random()*6;rich(g);scene.add(g);sway.push(g)}
  function bush(x,z,c){const g=new THREE.Group();for(let i=0;i<4;i++){const m=new THREE.Mesh(new THREE.SphereGeometry(.45+Math.random()*.2,14,10),std(c,.7));m.position.set(i*.45-.7,.35,rnd(.3));g.add(m)}
    const f=new THREE.Mesh(new THREE.SphereGeometry(.12,8,6),std('#ff6fa8',.4));f.position.set(0,.85,.3);g.add(f);g.position.set(x,0,z);rich(g);scene.add(g)}
  function puffs(x,z,n,s=1){for(let i=0;i<n;i++){const m=new THREE.Mesh(new THREE.SphereGeometry((.9+Math.random()*.7)*s,16,12),std('#ffffff',.95));m.position.set(x+rnd(2.4),-.4+Math.random()*.5,z+rnd(1.2));m.receiveShadow=true;scene.add(m)}}
  function bell(x,z){box(.14,2.4,.14,'#d99a00',x,1.2,z);const b=new THREE.Mesh(new THREE.ConeGeometry(.32,.5,16,1,true),std('#ffc93c',.25,.6));b.material.side=THREE.DoubleSide;b.position.set(x,2.15,z+.2);scene.add(b);b.userData.ph=Math.random()*6;bob.push(b)}
  function crystal(x,z,c){const g=new THREE.Group();for(let i=0;i<4;i++){const m=new THREE.Mesh(new THREE.OctahedronGeometry(.45+Math.random()*.3),new THREE.MeshStandardMaterial({color:c,emissive:c,emissiveIntensity:.35,roughness:.15,metalness:.2}));
      m.scale.y=1.8+Math.random();m.position.set(i*.4-.6,.6,rnd(.4));m.rotation.z=rnd(.6);g.add(m)}g.position.set(x,0,z);rich(g);scene.add(g)}
  function planet(x,y,z,c,r,ring){const p=new THREE.Mesh(new THREE.SphereGeometry(r,32,24),std(c,.5));p.position.set(x,y,z);scene.add(p);
    if(ring){const rg=new THREE.Mesh(new THREE.TorusGeometry(r*1.6,r*.12,8,48),std('#fff3c4',.4));rg.rotation.x=1.25;p.add(rg)}p.userData.ph=Math.random()*6;bob.push(p);planets.push(p)}
  // ---------------- lands
  LANDS.forEach((L,li)=>{const X=li*GAP;L.X=X;
    box(44,3,34,L.ground,X,-1.5,8);box(44.3,2.6,34.3,L.side,X,-1.76,8);
    box(44,.06,5,L.road,X,.03,1.3);box(44,.2,3.2,L.pave,X,.1,-2.9);
    for(let x=-21;x<=21;x+=3)box(1.4,.07,.18,'#ffffff',X+x,.07,1.3);
    L.blds=L.b.map((a,i)=>building({sign:a[0],bad:a[1],col:a[2],roof:a[3],w:a[4],h:a[5],x:X+[-12,-6.4,0,6.4,12][i],big:i===2}));
    const back=[-18.5,-9.2,9.2,18.5];
    if(L.key==='candy'){back.forEach((x,i)=>lolli(X+x,-4.5,['#e8457a','#7b4dff','#2a8fe0','#39b86a'][i],.95));[[-15.2,'#ff6fa8'],[-3.4,'#ffd166'],[3.4,'#7fe3ff'],[15.2,'#b8f27a']].forEach(([x,c])=>gum(X+x,-4.3,c))}
    if(L.key==='sea'){back.forEach(x=>lamp(X+x,-4.4,'#2a3a5c'));[-15.2,-3.5,3.5,15.2].forEach(x=>barrel(X+x,-4.3))}
    if(L.key==='jungle'){back.forEach(x=>palm(X+x,-4.6,1));[-15,-3.5,3.5,15].forEach(x=>bush(X+x,-4.3,'#3fae4a'));[-17,-8,8,17].forEach(x=>palm(X+x+rnd(2),-8.4,1.5))}
    if(L.key==='cloud'){back.forEach(x=>bell(X+x,-4.4));for(let x=-21;x<=21;x+=3.2)puffs(X+x,-9.4,2);for(let z=-8;z<=24;z+=3.2){puffs(X-22.4,z,1);puffs(X+22.4,z,1)}}
    if(L.key==='galaxy'){back.forEach((x,i)=>crystal(X+x,-4.5,['#7fe3ff','#ff8fd0','#ffe066','#b8f27a'][i]));planet(X-16,17,-46,'#ff9ec7',4,1);planet(X+18,21,-52,'#7fe3ff',2.6,0);planet(X+3,25,-60,'#ffe066',1.6,0)}
    // ribbon on the big building
    const rb=new THREE.Group();const rm=std('#e5333d',.4);for(const s of[-1,1]){const h=new THREE.Mesh(new THREE.BoxGeometry(1.6,.28,.05),rm);h.userData.x0=s*.8;h.position.x=s*.8;rb.add(h)}
    const bw=new THREE.Mesh(new THREE.TorusKnotGeometry(.22,.07,40,8),rm);bw.userData.x0=0;rb.add(bw);rb.position.set(X,1.3,BZ+D/2+.5);rich(rb);scene.add(rb);L.ribbon=rb});
  const boats=[];[[-10,-13],[-1,-14.5],[10,-12.5]].forEach(([x,z])=>{const g=new THREE.Group();const hull=new THREE.Mesh(new THREE.BoxGeometry(2.4,.6,1),std('#e5333d',.5));hull.position.y=.3;g.add(hull);
    const mast=new THREE.Mesh(new THREE.CylinderGeometry(.05,.05,2.2,8),std('#7a4a2a',.6));mast.position.y=1.5;g.add(mast);const sail=new THREE.Mesh(new THREE.PlaneGeometry(1,1.4),new THREE.MeshStandardMaterial({color:'#ffffff',side:THREE.DoubleSide}));sail.position.set(.5,1.6,0);g.add(sail);
    g.position.set(GAP+x,-.75,z);rich(g);scene.add(g);boats.push(g)});
  const clouds=[];for(let i=0;i<26;i++){const g=new THREE.Group();for(let k=0;k<5;k++){const m=new THREE.Mesh(new THREE.SphereGeometry(.9+Math.random()*.6,16,12),std('#ffffff',.9));m.position.set(k*1.1-2.2,Math.random()*.5,rnd(.8));g.add(m)}
    g.position.set(-30+i*11,13+Math.random()*4,-26-Math.random()*10);g.userData.sp=.3+Math.random()*.4;scene.add(g);clouds.push(g)}
  // ---------------- bridges between lands
  const bridges=[0,1,2,3].map(j=>{const B0=j*GAP+21.6,B1=(j+1)*GAP-21.6,NP=18,pm=std('#c98b4a',.7),planks=[],posts=[];
    for(let i=0;i<NP;i++){const p=new THREE.Mesh(new THREE.BoxGeometry((B1-B0)/NP-.08,.16,2.6),pm);p.position.set(B0+(i+.5)*(B1-B0)/NP,-.08,1.3);p.castShadow=true;p.receiveShadow=true;scene.add(p);planks.push(p)}
    for(let i=0;i<=4;i++)for(const z of[0,2.6]){const p=new THREE.Mesh(new THREE.CylinderGeometry(.1,.1,1.4,8),pm);p.position.set(B0+i*(B1-B0)/4,.4,z);scene.add(p);posts.push(p)}
    const ropes=[0,2.6].map(z=>{const r=new THREE.Mesh(new THREE.CylinderGeometry(.04,.04,B1-B0,6),std('#f6e2b3',.8));r.rotation.z=Math.PI/2;r.position.set((B0+B1)/2,1,z);scene.add(r);return r});
    return{planks,posts,ropes,show(on){planks.concat(posts).forEach(p=>{p.visible=on;p.scale.set(1,1,1)});ropes.forEach(r=>{r.visible=on;r.scale.y=1})},
      build(){planks.forEach(p=>p.visible=false);posts.forEach(p=>p.visible=false);ropes.forEach(r=>r.visible=false);
        planks.forEach((p,i)=>wait(i*.09,()=>{p.visible=true;const y0=-.08;p.position.y=y0+3;p.rotation.x=1;tween(.35,e=>{p.position.y=y0+3*(1-e);p.rotation.x=1-e},{ease:E.outBounce,done:()=>{sfx.clack();if(i%3===0)puff(p.position.clone().add(v3(0,.1,1.3)),3)}})}));
        posts.forEach((p,i)=>wait(.2+i*.15,()=>{p.visible=true;p.scale.y=.01;tween(.3,e=>p.scale.y=Math.max(.01,e),{ease:E.outBack})}));
        wait(1.9,()=>{ropes.forEach(r=>{r.visible=true;r.scale.y=.001;tween(.6,e=>r.scale.y=Math.max(.001,e),{ease:E.outCubic})});sfx.shine()})}}});
  // ---------------- small fx
  function puff(p,n=7,col='#fff7fb',s=1){for(let i=0;i<n;i++){const m=new THREE.Mesh(geo.puff||(geo.puff=new THREE.SphereGeometry(.14,10,8)),new THREE.MeshBasicMaterial({color:col,transparent:true,opacity:.9}));
    m.position.copy(p);addPart(m,scene,{v:v3(rnd(2.5*s),Math.random()*1.5*s,Math.random()*1.5*s),drag:2,life:.7,fade:true,grow:1.6*s})}}
  function confetti(p,n,spread=2){const cols=['#ff6fa8','#ffd166','#7fe3ff','#b8f27a','#c3a6ff','#ff9a3c','#ffffff'];for(let i=0;i<n;i++){
    const m=new THREE.Mesh(geo.conf||(geo.conf=new THREE.PlaneGeometry(.16,.26)),new THREE.MeshBasicMaterial({color:cols[i%cols.length],side:THREE.DoubleSide,transparent:true}));
    m.position.copy(p).add(v3(rnd(spread),rnd(.5),rnd(1)));addPart(m,scene,{v:v3(rnd(9),5+Math.random()*6,rnd(5)+1.5),av:v3(rnd(14),rnd(14),rnd(14)),g:7,drag:.7,life:2.6+Math.random(),fade:true})}}
  function firework(p,col){const r=sprite(glowTex,col,.5);r.position.copy(p).setY(1);scene.add(r);tone(300,.5,{vol:.04,to:900});
    tween(.7,e=>r.position.y=1+(p.y-1)*e,{ease:E.outCubic,done:()=>{scene.remove(r);noise(.35,{vol:.14,freq:1800,to:200});flash(p,5,col);
      for(let i=0;i<40;i++){const s=sprite(starTex,col,.4);s.position.copy(p);const a=Math.random()*6.28,b=Math.random()*3.14,sp=5+Math.random()*2.5;
        addPart(s,scene,{v:v3(Math.cos(a)*Math.sin(b)*sp,Math.cos(b)*sp,Math.sin(a)*Math.sin(b)*sp),g:3,drag:1.4,life:1.5,fade:true})}}})}
  function lob(m,from,to,dur,h,cb,spin=8){m.position.copy(from);scene.add(m);tween(dur,(e,k)=>{m.position.lerpVectors(from,to,e);m.position.y+=Math.sin(k*Math.PI)*h;m.rotation.set(k*spin,k*spin*.6,0)},{ease:E.lin,done:()=>{scene.remove(m);cb&&cb(to)}})}
  function bits(p,col,n=10,s=.12,pow=4){for(let i=0;i<n;i++){const d=new THREE.Mesh(new THREE.BoxGeometry(s,s,s),new THREE.MeshStandardMaterial({color:col,roughness:.5}));d.position.copy(p);addPart(d,scene,{v:v3(rnd(pow),Math.random()*pow*.7,1+Math.random()*2),av:v3(rnd(10),rnd(10),rnd(10)),g:9,life:.9,fade:true})}}
  function screenFlash(op=.8){const f=$('#sFlash');f.style.transition='none';f.style.opacity=op;void f.offsetWidth;f.style.transition='opacity .5s';f.style.opacity=0}
  function scramble(b,i,fx){b.setSign(b.d.bad,true);tween(.5,(e,k)=>b.sign.rotation.z=Math.sin(k*Math.PI*3)*.25*(1-k)+(i%2?.1:-.12)*k,{ease:E.lin});
    tween(.9,e=>b.set(1-e),{ease:E.inOut});tween(.4,(e,k)=>b.inner.rotation.z=Math.sin(k*Math.PI*4)*.025*(1-k),{ease:E.lin});tone(200,.25,{type:'square',vol:.05,to:90})}
  // ---------------- sky
  const skyNow=[0,1,2].map(()=>new THREE.Color('#ffffff'));
  function paintSky(){document.body.style.background=`linear-gradient(#${skyNow[0].getHexString()} 0%,#${skyNow[1].getHexString()} 45%,#${skyNow[2].getHexString()} 72%)`;scene.fog.color.copy(skyNow[1])}
  function setSky(c){c.forEach((x,i)=>skyNow[i].set(x));paintSky()}
  function skyTo(c,dur){const a=skyNow.map(x=>x.clone()),b=c.map(x=>new THREE.Color(x));tween(dur,e=>{skyNow.forEach((x,i)=>x.copy(a[i]).lerp(b[i],e));paintSky()},{ease:E.inOut})}
  const mixSky=(L,k)=>L.sky.map((c,i)=>'#'+new THREE.Color(GREYS[i]).lerp(new THREE.Color(c),k).getHexString());
  // ---------------- characters
  const tiko=makeTiko();tiko.scale.setScalar(1.25);scene.add(tiko);
  const friends=['#ff6fa8','#ffd166','#7be08a','#b48cff'].map(c=>{const f=makeTiko();recolor(f,0x3fa9ff,c);f.scale.setScalar(1.05);f.visible=false;scene.add(f);return f});
  let hop=null,cheerT=-9,throwT=-9;
  function walk(to,dur,cb){const a=tiko.position.clone();hop=true;tween(dur,(e,k)=>{tiko.position.lerpVectors(a,to,e);tiko.position.y=.2+Math.abs(Math.sin(k*Math.PI*Math.max(1,Math.round(dur*2.4))))*.4;tiko.rotation.y=to.x>a.x?.9:-.9},{ease:E.lin,done:()=>{hop=null;tiko.rotation.y=-.2;cb&&cb()}})}
  // -- 1 Professor Scramble
  function vScramble(L){const b=makeBoss();b.hands.forEach((h,i)=>{h.position.set((i?1:-1)*2.1,1.5,.4);b.bod.add(h)});const g=b.g;g.scale.setScalar(.95);scene.add(g);const home=v3(L.X,0,-1.4);let laugh=-9,st='idle';
    const head=()=>g.position.clone().add(v3(0,2.4,1.1));
    return{name:'scramble',g,head,cam:{tx:L.X-1.5,ty:3.4,tz:-3,yaw:-.05,pitch:.2,R:[18,11]},
      reset(){g.visible=false;g.position.copy(home);g.rotation.set(0,0,0);g.scale.setScalar(.95);b.xs.forEach(x=>x.visible=false);b.pupils.forEach(p=>p.visible=true);st='idle'},
      place(){this.reset();g.visible=true},
      laugh(){laugh=T;[330,300,270].forEach((f,i)=>tone(f,.14,{type:'square',vol:.06,at:i*.16}))},
      arrive(cb){this.reset();g.visible=true;g.position.set(L.X,22,-1.4);g.rotation.y=-3;sfx.whoosh();
        for(let i=0;i<24;i++)wait(i*.04,()=>{const s=sprite(starTex,0xc3a6ff,.4);s.position.copy(g.position).add(v3(rnd(2),1+rnd(2),rnd(1)));addPart(s,scene,{v:v3(rnd(2),1,0),life:.9,fade:true})});
        tween(1.1,(e,k)=>{g.position.y=22*(1-e);g.rotation.y=-3*(1-e)},{ease:E.inCubic,done:()=>{sfx.thud();shake=.3;puff(g.position.clone().add(v3(0,.2,0)),18,'#efe6ff',2);
          tween(.4,(e,k)=>{const s=1+Math.sin(k*Math.PI)*.18;b.bod.scale.set(s,1/s,s)},{ease:E.lin});wait(.4,cb)}})},
      attack(bd,i,cb){const s=g.position.clone().add(v3(i<2?-2:2,2,1)),t=bd.front();const o=sprite(glowTex,0xb48cff,1.2);
        lob(o,s,t,.45,1.2,()=>{flash(t,3,0xc3a6ff);starsAt(t,10,0xc3a6ff);scramble(bd,i);cb&&cb()},0);sfx.whoosh();
        for(let k=0;k<6;k++)wait(k*.06,()=>{const tr=sprite(starTex,0xd8c3ff,.3);tr.position.copy(o.position);addPart(tr,scene,{life:.5,fade:true})})},
      leave(cb){this.laugh();wait(.8,()=>{sfx.whoosh();const p0=g.position.clone();tween(1.2,(e,k)=>{g.position.set(p0.x+e*6,p0.y+e*22,p0.z-e*14);g.rotation.y=k*8},{ease:E.inCubic,done:()=>{g.visible=false;cb&&cb()}})})},
      hit(){st='hit';b.xs.forEach(x=>x.visible=true);b.pupils.forEach(p=>p.visible=false);b.skin.emissive.set('#aa0000');tween(.3,(e,k)=>b.skin.emissive.setRGB(.67*(1-k),0,0),{ease:E.lin})},
      defeat(cb){st='spin';tone(500,1.4,{type:'sawtooth',vol:.07,to:120});const p0=g.position.clone();tween(1.8,(e,k)=>{g.rotation.y=k*14;g.rotation.z=k*2;g.position.set(p0.x-e*8,p0.y+e*16,p0.z-e*26);g.scale.setScalar(.95*(1-e*.85))},{ease:E.inCubic,done:()=>{
        g.visible=false;const tw=sprite(starTex,0xffffff,.01);tw.position.copy(g.position);scene.add(tw);sfx.ding(2);tween(.6,(e,k)=>{tw.scale.setScalar(Math.sin(k*Math.PI)*2.4);tw.material.rotation=k*3},{ease:E.lin,done:()=>{scene.remove(tw);cb&&cb()}})}})},
      idle(t){if(!g.visible||st==='spin')return;const l=T-laugh<2.4;b.bod.position.y=l?Math.abs(Math.sin(T*10))*.18:Math.sin(t*2)*.08;b.bod.rotation.z=st==='hit'?Math.sin(T*20)*.06:0;b.hands.forEach((h,i)=>h.position.y=1.5+(l?Math.abs(Math.sin(T*10+i))*.3:0))}}}
  // -- 2 Captain Inkbeard
  function vInk(L){const g=new THREE.Group(),skin=std('#e04f6a',.4),spot=std('#ff9aa8',.5),wh=std('#ffffff',.25),bk=std('#15102a',.3);
    const head=new THREE.Mesh(new THREE.SphereGeometry(2,40,30),skin);head.scale.set(1,1.15,.9);head.position.y=3;g.add(head);
    for(let i=0;i<7;i++){const s=new THREE.Mesh(new THREE.SphereGeometry(.22,12,10),spot);const a=i*.9;s.position.set(Math.cos(a)*1.4,3.8+Math.sin(i*2)*.6,Math.sin(a)*.9+.4);s.scale.z=.4;s.lookAt(0,3,0);g.add(s)}
    const eye=new THREE.Mesh(new THREE.SphereGeometry(.45,20,16),wh);eye.position.set(.65,3.2,1.55);eye.scale.z=.6;g.add(eye);const pup=new THREE.Mesh(new THREE.SphereGeometry(.2,14,10),bk);pup.position.set(.65,3.15,1.85);g.add(pup);
    const xg=new THREE.Group();for(const r of[1,-1]){const bb=new THREE.Mesh(new THREE.BoxGeometry(.5,.1,.06),bk);bb.rotation.z=r*Math.PI/4;xg.add(bb)}xg.position.set(.65,3.2,1.88);xg.visible=false;g.add(xg);
    const patch=new THREE.Mesh(new THREE.CylinderGeometry(.42,.42,.1,20),bk);patch.rotation.x=Math.PI/2;patch.position.set(-.65,3.2,1.7);g.add(patch);
    const strap=new THREE.Mesh(new THREE.TorusGeometry(1.95,.06,8,40),bk);strap.position.y=3.35;strap.rotation.set(Math.PI/2-.25,0,.35);g.add(strap);
    const mouth=new THREE.Mesh(new THREE.TorusGeometry(.4,.08,8,20,Math.PI),bk);mouth.rotation.z=Math.PI;mouth.position.set(0,2.35,1.72);g.add(mouth);
    const hm=std('#1b1b2a',.5);const hat=new THREE.Mesh(new THREE.CylinderGeometry(1.2,1.5,1.1,24),hm);hat.position.y=5.25;g.add(hat);
    const brim=new THREE.Mesh(new THREE.CylinderGeometry(2.4,2.4,.12,30),hm);brim.scale.z=.55;brim.position.y=4.75;g.add(brim);
    const skull=new THREE.Mesh(new THREE.CircleGeometry(.32,20),new THREE.MeshBasicMaterial({color:'#ffffff'}));skull.position.set(0,5.3,1.41);g.add(skull);
    const band=new THREE.Mesh(new THREE.CylinderGeometry(1.33,1.4,.18,24),std('#ffc93c',.3,.4));band.position.y=4.95;g.add(band);
    const arms=[];for(let i=0;i<6;i++){const a=new THREE.Group();const ang=(i/6)*Math.PI*2+.3;a.position.set(Math.cos(ang)*1.3,1.2,Math.sin(ang)*1.1);a.rotation.y=-ang;
      const segs=[];let par=a;for(let k=0;k<6;k++){const s=new THREE.Group();s.position.x=k?.42:0;s.add(new THREE.Mesh(new THREE.SphereGeometry(.42-k*.05,14,10),skin));par.add(s);segs.push(s);par=s}a.userData={segs,ph:i};g.add(a);arms.push(a)}
    rich(g);g.scale.setScalar(1.7);scene.add(g);const up=2.2,down=-12,Z=-19;let laugh=-9,st='idle',whirl=null;
    const headP=()=>g.position.clone().add(v3(0,3.3*1.7,1.6*1.7));
    return{name:'ink',g,head:headP,cam:{tx:L.X-1,ty:5.2,tz:-8,yaw:0,pitch:.15,R:[28,18]},
      reset(){g.visible=false;g.position.set(L.X,down,Z);g.rotation.set(0,0,0);g.scale.setScalar(1.7);xg.visible=false;pup.visible=true;st='idle';if(whirl){scene.remove(whirl);whirl=null}},
      place(){this.reset();g.visible=true;g.position.y=up},
      laugh(){laugh=T;[260,240,260,220].forEach((f,i)=>tone(f,.18,{type:'sawtooth',vol:.05,at:i*.2}))},
      arrive(cb){this.reset();g.visible=true;sfx.thud();shake=.25;noise(1.4,{vol:.2,freq:400,to:120});tween(1.6,e=>g.position.y=down+(up-down)*e,{ease:E.outBack,done:cb});
        for(let i=0;i<34;i++)wait(i*.03,()=>{const d=new THREE.Mesh(geo.puff||(geo.puff=new THREE.SphereGeometry(.14,10,8)),new THREE.MeshBasicMaterial({color:'#e8f8ff',transparent:true}));d.position.set(L.X+rnd(7),-.6,Z+2+rnd(3));addPart(d,scene,{v:v3(rnd(3),4+Math.random()*5,rnd(2)),g:8,life:1.2,fade:true,grow:1.5})})},
      attack(bd,i,cb){const s=g.position.clone().add(v3(rnd(.6),2.4*1.7,3)),t=bd.front();const blob=new THREE.Mesh(new THREE.SphereGeometry(.35,14,10),std('#1a1030',.3));tone(180,.2,{vol:.1,to:80});
        lob(blob,s,t,.6,4,()=>{noise(.2,{vol:.18,freq:500,to:100});for(let j=0;j<12;j++){const d=new THREE.Mesh(geo.inkd||(geo.inkd=new THREE.SphereGeometry(.12,8,6)),new THREE.MeshBasicMaterial({color:'#1a1030',transparent:true}));d.position.copy(t);addPart(d,scene,{v:v3(rnd(5),Math.random()*3,1+Math.random()*2),g:9,life:.8,fade:true})}
          scramble(bd,i);cb&&cb()},0)},
      leave(cb){tween(1.4,e=>g.position.y=up+(down-up)*e,{ease:E.inCubic,done:()=>{g.visible=false;cb&&cb()}});puff(v3(L.X,-.6,Z+2),14,'#e8f8ff',2)},
      hit(){st='hit';xg.visible=true;pup.visible=false;skin.emissive.set('#aa0000');tween(.3,(e,k)=>skin.emissive.setRGB(.67*(1-k),0,0),{ease:E.lin})},
      defeat(cb){st='spin';whirl=new THREE.Mesh(new THREE.RingGeometry(1,7,48,1),new THREE.MeshBasicMaterial({map:swirlTex('#1f6fd0'),transparent:true,opacity:.0,side:THREE.DoubleSide,depthWrite:false}));whirl.rotation.x=-Math.PI/2;whirl.position.set(L.X,-.6,Z);scene.add(whirl);
        tween(.5,e=>whirl.material.opacity=e*.85);noise(2.4,{vol:.16,freq:300,to:900,type:'bandpass',q:.6});tone(420,1.6,{type:'sine',vol:.08,to:90});
        tween(2.2,(e,k)=>{g.rotation.y=k*12;g.position.y=up+(down-up)*e;whirl.rotation.z=k*14},{ease:E.inCubic,done:()=>{g.visible=false;
          for(let j=0;j<26;j++){const d=new THREE.Mesh(geo.puff||(geo.puff=new THREE.SphereGeometry(.14,10,8)),new THREE.MeshBasicMaterial({color:'#2a1840',transparent:true,opacity:.8}));d.position.set(L.X+rnd(3),-.2,Z+rnd(2));addPart(d,scene,{v:v3(rnd(3),2+Math.random()*3,rnd(2)),drag:1.4,life:1.4,fade:true,grow:3})}
          tween(.8,e=>whirl.material.opacity=.85*(1-e),{done:()=>{scene.remove(whirl);whirl=null;cb&&cb()}})}})},
      idle(t){if(!g.visible)return;arms.forEach(a=>a.userData.segs.forEach((s,k)=>{s.rotation.z=Math.sin(t*2.4+a.userData.ph+k*.7)*.32-.12;s.rotation.y=Math.cos(t*1.8+a.userData.ph+k)*.18}));
        const l=T-laugh<2.6;head.scale.y=1.15*(1+(l?Math.abs(Math.sin(T*11))*.06:Math.sin(t*2)*.02));if(st!=='spin')g.rotation.z=st==='hit'?Math.sin(T*18)*.05:(l?Math.sin(T*11)*.04:Math.sin(t*.9)*.03)}}}
  // -- 3 Bonkers the Baboon
  function vBaboon(L){const g=new THREE.Group(),body=new THREE.Group();g.add(body);const fur=std('#8a5a3a',.85),face=std('#f2c9a0',.6),wh=std('#ffffff',.25),bk=std('#15102a',.3),red=std('#e5333d',.5);
    const m=(geom,mat,x,y,z,sx=1,sy=1,sz=1)=>{const o=new THREE.Mesh(geom,mat);o.position.set(x,y,z);o.scale.set(sx,sy,sz);body.add(o);return o};
    m(new THREE.SphereGeometry(1.25,32,24),fur,0,1.75,0,1,1.12,.85);m(new THREE.SphereGeometry(.85,24,18),face,0,1.65,.72,1,1.1,.5);
    m(new THREE.SphereGeometry(1,32,24),fur,0,3.5,.1);m(new THREE.SphereGeometry(.62,24,18),face,0,3.28,.72,1.2,.85,.8);
    for(const s of[-1,1]){m(new THREE.SphereGeometry(.3,14,10),face,s*.97,3.72,0,1,1,.5);m(new THREE.SphereGeometry(.05,8,6),bk,s*.14,3.42,1.22)}
    const eyes=[-1,1].map(s=>{m(new THREE.SphereGeometry(.21,16,12),wh,s*.33,3.86,.86);return m(new THREE.SphereGeometry(.1,10,8),bk,s*.33,3.85,1.05)});
    const xg=new THREE.Group();for(const s of[-1,1])for(const r of[1,-1]){const bb=new THREE.Mesh(new THREE.BoxGeometry(.3,.07,.05),bk);bb.rotation.z=r*Math.PI/4;bb.position.set(s*.33,3.86,1.08);xg.add(bb)}xg.visible=false;body.add(xg);
    const brow=m(new THREE.BoxGeometry(1,.14,.2),fur,0,4.1,.86);
    const grin=m(new THREE.TorusGeometry(.24,.05,8,20,Math.PI),bk,0,3.12,1.2);grin.rotation.z=Math.PI;
    const band=m(new THREE.TorusGeometry(.98,.11,10,36),red,0,3.95,.05);band.rotation.x=Math.PI/2-.2;
    const knot=m(new THREE.SphereGeometry(.18,10,8),red,-.7,4.0,-.75);
    const arms=[-1,1].map(s=>{const a=new THREE.Group();a.position.set(s*1.1,2.5,0);body.add(a);const c=new THREE.Mesh(new THREE.CylinderGeometry(.2,.17,1.7,12),fur);c.position.y=-.85;a.add(c);
      const h=new THREE.Mesh(new THREE.SphereGeometry(.3,14,10),face);h.position.y=-1.75;a.add(h);a.rotation.z=s*.35;return a});
    for(const s of[-1,1]){m(new THREE.CylinderGeometry(.22,.2,.6,12),fur,s*.55,.45,0);m(new THREE.SphereGeometry(.3,14,10),face,s*.6,.12,.15,1.2,.5,1.4)}
    const tail=m(new THREE.TorusGeometry(.55,.12,8,20,Math.PI*1.4),fur,0,1.3,-1.05);tail.rotation.y=Math.PI/2;
    rich(g);g.scale.setScalar(1.05);scene.add(g);const big=L.blds[2],roofY=big.d.h+1.5-.15,home=v3(L.X,roofY,BZ);let laugh=-9,st='idle';
    const vine=new THREE.Mesh(new THREE.CylinderGeometry(.06,.06,1,6),std('#2e7d32',.7));vine.visible=false;scene.add(vine);
    const head=()=>g.position.clone().add(v3(0,3.6*1.05,1));
    return{name:'baboon',g,head,cam:{tx:L.X-1,ty:4.6,tz:-4,yaw:-.04,pitch:.16,R:[22,15]},
      reset(){g.visible=false;g.position.copy(home);g.rotation.set(0,0,0);g.scale.setScalar(1.05);vine.visible=false;xg.visible=false;eyes.forEach(e=>e.visible=true);st='idle';arms.forEach((a,i)=>a.rotation.z=(i?1:-1)*.35)},
      place(){this.reset();g.visible=true},
      laugh(){laugh=T;[520,700,520,760].forEach((f,i)=>tone(f,.12,{type:'square',vol:.05,at:i*.14}))},
      arrive(cb){this.reset();g.visible=true;vine.visible=true;const piv=v3(L.X-6,23,BZ),len=14.2;sfx.whoosh();
        tween(1.3,(e,k)=>{const a=-1.25+e*1.67;g.position.set(piv.x+Math.sin(a)*len,piv.y-Math.cos(a)*len-3.4,BZ);g.rotation.z=a*.5;arms[1].rotation.z=Math.PI-.2;
          const hand=g.position.clone().add(v3(0,3.6,0));placeSeg(vine,piv,hand)},{ease:E.inOut,done:()=>{vine.visible=false;arms[1].rotation.z=.35;const p0=g.position.clone();
          tween(.45,(e,k)=>{g.position.lerpVectors(p0,home,e);g.position.y+=Math.sin(k*Math.PI)*1.2;g.rotation.z=(1-e)*.2},{ease:E.lin,done:()=>{sfx.thud();shake=.25;puff(home.clone().add(v3(0,.2,1)),12,'#fff3d6',1.5);wait(.2,cb)}})}})},
      attack(bd,i,cb){const s=g.position.clone().add(v3(i<2?-1.2:1.2,2.6,1)),t=bd.front();arms[i<2?0:1].rotation.z=(i<2?-1:1)*2.6;wait(.25,()=>arms[i<2?0:1].rotation.z=(i<2?-1:1)*.35);
        const c=new THREE.Mesh(new THREE.SphereGeometry(.3,14,10),std('#6b3f1f',.7));sfx.whoosh();lob(c,s,t,.55,3,()=>{tone(150,.15,{type:'square',vol:.12,to:60});bits(t,'#6b3f1f',8,.16);bits(t,'#ffffff',8,.1);scramble(bd,i);cb&&cb()})},
      leave(cb){this.laugh();wait(.6,()=>{const p0=g.position.clone(),to=v3(L.X+4,0,-16);sfx.whoosh();tween(.9,(e,k)=>{g.position.lerpVectors(p0,to,e);g.position.y+=Math.sin(k*Math.PI)*4;g.rotation.x=-k*.6},{ease:E.lin,done:()=>{g.visible=false;cb&&cb()}})})},
      hit(){st='hit';xg.visible=true;eyes.forEach(e=>e.visible=false);fur.emissive.set('#aa0000');tween(.3,(e,k)=>fur.emissive.setRGB(.67*(1-k),0,0),{ease:E.lin})},
      defeat(cb){st='mad';xg.visible=false;eyes.forEach(e=>e.visible=true);this.laugh();const c=new THREE.Mesh(new THREE.SphereGeometry(.34,14,10),std('#6b3f1f',.7));c.castShadow=true;
        arms[1].rotation.z=2.8;const s=g.position.clone().add(v3(1.3,3.4,.3)),top=s.clone().add(v3(-1.2,7,0)),hd=g.position.clone().add(v3(0,4.6,.2));scene.add(c);sfx.whoosh();
        tween(1.4,(e,k)=>{if(k<.5)c.position.lerpVectors(s,top,E.outCubic(k*2));else c.position.lerpVectors(top,hd,E.inQuad(k*2-1));c.rotation.x=k*10},{ease:E.lin,done:()=>{
          tone(140,.25,{type:'square',vol:.16,to:50});shake=.22;starsAt(hd,12);xg.visible=true;eyes.forEach(e=>e.visible=false);st='dizzy';
          const ring=new THREE.Group();for(let i=0;i<5;i++){const s2=sprite(starTex,0xffe27a,.5);s2.position.set(Math.cos(i*1.26)*.9,0,Math.sin(i*1.26)*.9);ring.add(s2)}ring.position.copy(hd).add(v3(0,.3,0));scene.add(ring);
          tween(1.4,(e,k)=>{ring.rotation.y=k*12;c.position.y=hd.y-k*5;c.position.x=hd.x+k*2},{ease:E.lin,done:()=>scene.remove(c)});
          wait(1.6,()=>{scene.remove(ring);const p0=g.position.clone();tone(500,1,{type:'sawtooth',vol:.06,to:100});
            tween(1.1,(e,k)=>{g.rotation.x=-1.5*e;g.position.set(p0.x,p0.y+Math.sin(k*Math.PI)*1.2-e*8,p0.z-e*6)},{ease:E.inCubic,done:()=>{g.visible=false;sfx.thud();puff(v3(L.X,.3,BZ-6),14,'#e9f5d6',2);cb&&cb()}})})}})},
      idle(t){if(!g.visible)return;const l=T-laugh<2.2;body.position.y=l?Math.abs(Math.sin(T*12))*.22:Math.sin(t*2.6)*.05;
        if(st==='dizzy')g.rotation.z=Math.sin(T*6)*.15;else if(st==='hit')g.rotation.z=Math.sin(T*20)*.06;else if(!vine.visible)g.rotation.z=0;
        tail.rotation.x=Math.sin(t*3)*.25;if(l&&st==='idle')arms.forEach((a,i)=>a.rotation.z=(i?1:-1)*(1.2+Math.sin(T*12+i)*.6))}}}
  // -- 4 Grumble Cloud
  function vCloud(L){const g=new THREE.Group(),mat=new THREE.MeshStandardMaterial({color:'#7d8494',roughness:.95}),wh=std('#ffffff',.25),bk=std('#15102a',.3);
    const balls=[[0,0,0,2.2],[-2,-.3,0,1.6],[2,-.3,0,1.7],[-1.1,1.2,-.2,1.5],[1.2,1.3,-.2,1.4],[-3.3,-.6,-.3,1.1],[3.4,-.6,-.3,1.1],[0,-.9,.4,1.6]].map(([x,y,z,r])=>{const b=new THREE.Mesh(new THREE.SphereGeometry(r,28,20),mat);b.position.set(x,y,z);b.userData.r=r;g.add(b);return b});
    const eyes=[-1,1].map(s=>{const e=new THREE.Mesh(new THREE.SphereGeometry(.42,18,14),wh);e.scale.set(1,.75,.45);e.position.set(s*.75,.25,2.05);g.add(e);const p=new THREE.Mesh(new THREE.SphereGeometry(.17,12,10),bk);p.position.set(s*.72,.2,2.24);g.add(p);
      const br=new THREE.Mesh(new THREE.BoxGeometry(.75,.14,.12),std('#3a3f52',.5));br.position.set(s*.72,.75,2.15);br.rotation.z=s*.35;g.add(br);return p});
    const xg=new THREE.Group();for(const s of[-1,1])for(const r of[1,-1]){const bb=new THREE.Mesh(new THREE.BoxGeometry(.4,.08,.05),bk);bb.rotation.z=r*Math.PI/4;bb.position.set(s*.75,.25,2.3);xg.add(bb)}xg.visible=false;g.add(xg);
    const fr=new THREE.Mesh(new THREE.TorusGeometry(.35,.07,8,20,Math.PI),bk);fr.position.set(0,-.55,2.15);g.add(fr);
    rich(g);g.scale.setScalar(1.3);scene.add(g);const home=v3(L.X,11,-3.5);let st='idle',laugh=-9,rainbow=null,sunS=null;
    const head=()=>g.position.clone().add(v3(0,0,2.4));
    return{name:'cloud',g,head,cam:{tx:L.X,ty:6.2,tz:-3,yaw:0,pitch:.1,R:[26,17]},
      reset(){g.visible=false;g.position.copy(home);g.scale.setScalar(1.3);g.rotation.set(0,0,0);xg.visible=false;eyes.forEach(e=>e.visible=true);st='idle';mat.color.set('#7d8494');mat.emissive.set('#000000');balls.forEach(b=>b.visible=true);
        if(rainbow){scene.remove(rainbow);rainbow=null}if(sunS){scene.remove(sunS);sunS=null}},
      place(){this.reset();g.visible=true},
      laugh(){laugh=T;tone(90,.9,{type:'sawtooth',vol:.06,to:60});noise(.9,{vol:.08,freq:200,to:80})},
      arrive(cb){this.reset();g.visible=true;g.position.set(L.X+34,12,-3.5);noise(2,{vol:.1,freq:250,to:120});tween(2,e=>g.position.x=L.X+34*(1-e),{ease:E.outCubic,done:()=>{screenFlash(.85);noise(.8,{vol:.25,freq:900,to:60});shake=.3;wait(.3,cb)}})},
      attack(bd,i,cb){const s=g.position.clone().add(v3(rnd(2),-1.6,1)),t=bd.front();const pts=[s];for(let k=1;k<6;k++){const p=s.clone().lerp(t,k/6);p.x+=rnd(1.4);p.z+=rnd(.6);pts.push(p)}pts.push(t);
        const bolt=new THREE.Group();const bm=new THREE.MeshBasicMaterial({color:'#fff6b0'});for(let k=0;k<pts.length-1;k++){const sgm=new THREE.Mesh(new THREE.BoxGeometry(.14,1,.14),bm);placeSeg(sgm,pts[k],pts[k+1]);bolt.add(sgm)}scene.add(bolt);
        screenFlash(.35);noise(.5,{vol:.22,freq:2400,to:100});flash(t,4,0xbfe8ff);tween(.3,(e,k)=>bolt.visible=Math.sin(k*40)>-.2,{ease:E.lin,done:()=>scene.remove(bolt)});
        for(let k=0;k<8;k++){const h=new THREE.Mesh(geo.hail||(geo.hail=new THREE.IcosahedronGeometry(.13,0)),std('#e8f6ff',.15));h.position.copy(t).add(v3(rnd(2),3+Math.random()*2,rnd(.5)));addPart(h,scene,{v:v3(0,-8,0),g:12,life:.5})}
        bits(t,'#bfe8ff',10,.14);scramble(bd,i);cb&&cb()},
      leave(cb){this.laugh();wait(.6,()=>tween(2,e=>g.position.x=L.X+40*e,{ease:E.inCubic,done:()=>{g.visible=false;cb&&cb()}}))},
      hit(){st='hit';xg.visible=true;eyes.forEach(e=>e.visible=false);mat.emissive.set('#aa0000');tween(.3,(e,k)=>mat.emissive.setRGB(.67*(1-k),0,0),{ease:E.lin})},
      defeat(cb){st='swell';tone(200,1.2,{type:'sine',vol:.1,to:900});tween(1.2,(e,k)=>{const s=1.3*(1+e*.5)+Math.sin(k*40)*.04;g.scale.setScalar(s);mat.color.set('#7d8494').lerp(new THREE.Color('#c9cfdc'),e)},{ease:E.inQuad,done:()=>{
        g.visible=false;noise(.5,{vol:.3,freq:3000,to:200});screenFlash(.6);shake=.25;const c=g.position.clone();
        for(let k=0;k<40;k++){const d=new THREE.Mesh(new THREE.SphereGeometry(.4+Math.random()*.5,12,10),std('#ffffff',.9));d.position.copy(c).add(v3(rnd(4),rnd(3),rnd(2)));addPart(d,scene,{v:v3(rnd(14),rnd(10),rnd(6)),drag:2.2,life:1.6,fade:true,shrink:true})}
        sunS=sprite(glowTex,0xfff2a8,.1);sunS.position.set(L.X+10,16,-30);scene.add(sunS);tween(1.4,e=>sunS.scale.setScalar(.1+e*16),{ease:E.outCubic});
        rainbow=new THREE.Group();['#ff4d4d','#ff9a3c','#ffd84a','#5cd65c','#4d9dff','#8e5bff'].forEach((col,k)=>{const r=new THREE.Mesh(new THREE.TorusGeometry(18-k*.55,.3,8,64,Math.PI),new THREE.MeshBasicMaterial({color:col,transparent:true,opacity:0}));rainbow.add(r)});
        rainbow.position.set(L.X,-.5,-22);scene.add(rainbow);wait(.6,()=>{sfx.shine();rainbow.children.forEach((r,k)=>tween(1.2,e=>r.material.opacity=e*.85,{delay:k*.08}))});wait(1.4,()=>cb&&cb())}})},
      idle(t){if(!g.visible||st==='swell')return;const l=T-laugh<1.4;g.position.y=home.y+Math.sin(t*1.3)*.25;balls.forEach((b,i)=>b.scale.setScalar(1+Math.sin(t*2+i)*.03));g.rotation.z=st==='hit'?Math.sin(T*20)*.05:(l?Math.sin(T*9)*.05:0)}}}
  // -- 5 The Word Eater
  function vEater(L){const g=new THREE.Group(),body=new THREE.Group();g.add(body);const skin=new THREE.MeshStandardMaterial({color:'#8e3bc4',roughness:.18,metalness:.05}),dark=std('#2a0838',.6),wh=std('#ffffff',.2),bk=std('#15102a',.3);
    const blob=new THREE.Mesh(new THREE.SphereGeometry(3,48,36),skin);blob.scale.set(1.12,.92,1);blob.position.y=2.75;body.add(blob);
    for(let i=0;i<9;i++){const s=new THREE.Mesh(new THREE.SphereGeometry(.28+Math.random()*.2,14,10),std('#b56be0',.3));const a=rnd(2.4),b=.2+Math.random()*1.1;s.position.set(Math.sin(a)*3.1*Math.cos(b-.6),2.75+Math.sin(b)*2.4,Math.cos(a)*2.7);s.scale.z=.35;s.lookAt(0,2.75,0);body.add(s)}
    const mouth=new THREE.Group();mouth.position.set(0,2.3,2.72);body.add(mouth);const mo=new THREE.Mesh(new THREE.CircleGeometry(1.7,40,Math.PI,Math.PI),dark);mo.scale.y=.85;mouth.add(mo);
    const tongue=new THREE.Mesh(new THREE.CircleGeometry(.8,24,Math.PI,Math.PI),std('#ff6f8f',.6));tongue.position.set(.2,-1,.02);tongue.scale.y=.5;mouth.add(tongue);
    for(let i=0;i<6;i++){const t=new THREE.Mesh(new THREE.ConeGeometry(.17,.38,4),wh);t.rotation.z=Math.PI;t.position.set(-1.35+i*.54,-.17,.04);mouth.add(t)}
    const eyes=[-1,1].map(s=>{const st=new THREE.Mesh(new THREE.CylinderGeometry(.12,.16,1.1,10),skin);st.position.set(s*1.05,5.3,.6);st.rotation.z=-s*.25;body.add(st);
      const e=new THREE.Mesh(new THREE.SphereGeometry(.55,20,16),wh);e.position.set(s*1.25,5.95,.75);body.add(e);const p=new THREE.Mesh(new THREE.SphereGeometry(.24,14,10),bk);p.position.set(s*1.25,5.92,1.23);body.add(p);return{e,p}});
    const xg=new THREE.Group();for(const s of[-1,1])for(const r of[1,-1]){const bb=new THREE.Mesh(new THREE.BoxGeometry(.6,.11,.06),bk);bb.rotation.z=r*Math.PI/4;bb.position.set(s*1.25,5.95,1.32);xg.add(bb)}xg.visible=false;body.add(xg);
    rich(g);scene.add(g);const home=v3(L.X,0,-1.2);let st='idle',laugh=-9,chomp=-9;
    const head=()=>g.position.clone().add(v3(0,3.6*g.scale.y,2.6));
    return{name:'eater',g,head,cam:{tx:L.X-1.5,ty:3.8,tz:-3,yaw:-.04,pitch:.17,R:[24,14]},
      reset(){g.visible=false;g.position.copy(home);g.scale.setScalar(1);g.rotation.set(0,0,0);xg.visible=false;eyes.forEach(o=>o.p.visible=true);st='idle';skin.color.set('#8e3bc4');skin.emissive.set('#000000');mouth.scale.set(1,1,1)},
      place(){this.reset();g.visible=true},
      laugh(){laugh=T;[110,98,110,82].forEach((f,i)=>tone(f,.3,{type:'sawtooth',vol:.08,at:i*.26}))},
      arrive(cb){this.reset();g.visible=true;g.position.set(L.X,30,-1.2);sfx.whoosh();tween(1.1,e=>g.position.y=30*(1-e),{ease:E.inCubic,done:()=>{tone(60,.6,{vol:.4,to:30});noise(.5,{vol:.3,freq:400,to:60});shake=.5;
        puff(g.position.clone().add(v3(0,.3,1)),26,'#eadcff',3);tween(.7,(e,k)=>{const s=1+Math.sin(k*Math.PI*3)*(1-k)*.22;body.scale.set(s,1/s,s)},{ease:E.lin});wait(.6,cb)}})},
      attack(bd,i,cb){const t=bd.front(),m=g.position.clone().add(v3(0,2.3,3));const word=bd.d.sign.replace(/[^A-Z]/g,''),take=[...word].filter((c,k)=>bd.d.bad[k]==='_'||'AEIOU'.includes(c)).slice(0,3);
        scramble(bd,i);take.forEach((ch,k)=>wait(k*.1,()=>{const tl=makeTile(ch,TILE_COLORS[(i+k*3)%10]);tl.scale.setScalar(.55);lob(tl,t.clone().add(v3(rnd(1),0,.3)),m,.55,1.6,()=>{chomp=T;tone(120,.12,{type:'square',vol:.1,to:70})},6)}));wait(.75,()=>cb&&cb())},
      leave(cb){this.laugh();wait(.8,()=>{const p0=g.position.clone();sfx.whoosh();tween(1,(e,k)=>{g.position.set(p0.x,p0.y+e*28,p0.z-e*10)},{ease:E.inCubic,done:()=>{g.visible=false;cb&&cb()}})})},
      hit(){st='hit';xg.visible=true;eyes.forEach(o=>o.p.visible=false);skin.emissive.set('#aa0000');tween(.3,(e,k)=>skin.emissive.setRGB(.67*(1-k),0,0),{ease:E.lin})},
      defeat(cb){st='ache';tween(1.4,e=>skin.color.set('#8e3bc4').lerp(new THREE.Color('#7fb35a'),e*.6),{ease:E.lin});tone(160,1.4,{type:'sawtooth',vol:.06,to:90});
        wait(1.6,()=>{st='burp';tone(70,.9,{type:'sawtooth',vol:.18,to:40});noise(.8,{vol:.2,freq:300,to:90});shake=.4;mouth.scale.set(1.3,1.6,1);const m=g.position.clone().add(v3(0,2.4,3));
          for(let k=0;k<44;k++)wait(k*.025,()=>{const tl=makeTile(rLetter(),TILE_COLORS[k%10]);tl.scale.setScalar(.6);tl.position.copy(m);addPart(tl,scene,{v:v3(rnd(16),6+Math.random()*9,3+Math.random()*7),av:v3(rnd(10),rnd(10),rnd(10)),g:12,life:2.2})});
          wait(1.3,()=>{mouth.scale.set(1,1,1);const p0=g.position.clone();sfx.pop();tween(.8,e=>g.scale.setScalar(1-e*.86),{ease:E.inOut});
            wait(.9,()=>{const s0=g.position.clone();tween(1.6,(e,k)=>{g.position.set(s0.x+e*12,Math.abs(Math.sin(k*Math.PI*3))*2*(1-k*.5),s0.z+e*16);g.rotation.y=k*4},{ease:E.lin,done:()=>{g.visible=false;cb&&cb()}});
              [0,1,2].forEach(i=>wait(.18+i*.53,()=>tone(500-i*80,.1,{type:'triangle',vol:.05})))})})})},
      idle(t){if(!g.visible)return;const l=T-laugh<2.2,c=T-chomp<.25;if(st!=='burp')mouth.scale.y=c?.35:(l?1+Math.abs(Math.sin(T*9))*.25:1);
        if(st==='idle'||st==='hit'){const s=1+Math.sin(t*2.4)*.025;body.scale.set(s,1/s,s)}if(st==='ache')body.rotation.z=Math.sin(T*14)*.05;else body.rotation.z=st==='hit'?Math.sin(T*20)*.05:0;
        eyes.forEach((o,i)=>{o.e.position.y=5.95+Math.sin(t*3+i*2)*.1})}}}
  const VMAKERS=[vScramble,vInk,vBaboon,vCloud,vEater];const VIL=LANDS.map((L,i)=>VMAKERS[i](L));
  // ---------------- camera rig
  const camP={tx:0,ty:2.6,tz:-3.5,yaw:0,pitch:.3,R:22};
  const fitR=(w,h)=>clamp(fitDist(cam,w,h,W*.95,H*.78),10,300);
  function camSet(p){Object.assign(camP,p,{R:Array.isArray(p.R)?fitR(...p.R):p.R})}
  function camTo(p,dur){const a={...camP},b=Object.assign({},p,{R:Array.isArray(p.R)?fitR(...p.R):(p.R==null?a.R:p.R)});tween(dur,e=>{for(const k in b)camP[k]=a[k]+(b[k]-a[k])*e},{ease:E.inOut})}
  const wide=L=>({tx:L.X,ty:2.6,tz:-3.5,yaw:0,pitch:.28,R:[36,13]});
  // ---------------- UI
  const say=$('#sSay'),card=$('#sCard'),title=$('#sTitle');let sayT=null;
  function talk(who,text,dur=2.8,col='#2a8fe0'){say.innerHTML=`<b style="color:${col}">${who}</b>“${text}”`;say.classList.remove('show');void say.offsetWidth;say.classList.add('show');tone(620,.07,{type:'triangle',vol:.05});tone(820,.07,{type:'triangle',vol:.05,at:.08});
    if(sayT){const i=tweens.indexOf(sayT);if(i>=0)tweens.splice(i,1)}sayT=wait(dur,()=>{say.classList.remove('show');sayT=null})}
  const tikoSay=(t,d)=>talk('Tiko',t,d,'#2a8fe0');const vSay=(L,t,d)=>talk(L.v,t,d,L.vc);
  function cine(on){document.body.classList.toggle('cine',on)}
  function showTitle(k,big,small,dur=3.2){title.innerHTML=`<i>${k}</i><b>${big}</b><span>${small}</span>`;title.classList.remove('show');void title.offsetWidth;title.classList.add('show');sfx.shine();wait(dur,()=>title.classList.remove('show'))}
  function showCard(html){card.innerHTML=html;card.classList.remove('show');void card.offsetWidth;card.classList.add('show');sfx.shine()}
  // ---------------- world state
  function landState(li,s){const L=LANDS[li];L.blds.forEach((b,i)=>{const big=i===2;b.inner.scale.set(1,1,1);b.inner.rotation.set(0,0,0);b.sign.rotation.set(0,0,0);
      if(s==='bright'||s==='complete'){b.set(1);b.setSign(b.d.sign,false)}
      else if(s==='scrambled'){b.set(0);b.setSign(b.d.bad,true);b.sign.rotation.z=i%2?.1:-.12}
      else if(s==='tasks'){if(big){b.set(0);b.setSign('🔒 LEVEL '+(li+1)*10,false)}else{b.set(1);b.setSign(b.d.sign,false)}}
      b.door.material.emissiveIntensity=(s==='complete'&&big)?.9:0});
    L.ribbon.visible=s==='tasks';L.ribbon.children.forEach(c=>{c.position.set(c.userData.x0,0,0);c.rotation.set(0,0,0)})}
  let token=0,curLand=0;
  function setup(li,part){tweens.length=0;parts.forEach(p=>rm2(p.obj));parts.length=0;shake=0;curLand=li;
    card.classList.remove('show');say.classList.remove('show');title.classList.remove('show');
    LANDS.forEach((L,j)=>landState(j,j<li?'complete':j>li?'bright':({intro:'bright',boss:'tasks',complete:'tasks',bridge:'complete',finale:'complete'})[part]));
    bridges.forEach((b,j)=>b.show(j<li));planets.forEach(p=>p.visible=li>=3);VIL.forEach(v=>v.reset());friends.forEach(f=>f.visible=false);
    const L=LANDS[li];water.material.color.set(L.water);setSky(part==='boss'?mixSky(L,.35):L.sky);
    tiko.position.set(L.X-5,.2,-.3);tiko.rotation.y=-.2;tiko.visible=true;hop=null;cheerT=-9;
    cine(true)}
  // ---------------- parts
  function pIntro(li,next){setup(li,'intro');const L=LANDS[li],V=VIL[li],X=L.X,tk=token;
    camSet({tx:X-14,ty:3,tz:-3,yaw:-.45,pitch:.2,R:[18,10]});camTo({tx:X-2,ty:2.8,tz:-3.5,yaw:.05,pitch:.24,R:[30,12]},4.2);
    if(li===0){tiko.position.set(X-12,.2,-.3)}walk(v3(X-5,.2,-.3),li===0?3:1.2);
    wait(1,()=>tikoSay(L.say.hello,2.8));
    wait(4.1,()=>{skyTo(mixSky(L,.6),1.2);camTo(Object.assign({},V.cam,{R:V.cam.R.map(r=>r*1.08)}),1.2)});
    wait(4.6,()=>V.arrive(()=>{V.laugh();vSay(L,L.say.taunt,2.6)}));
    const att={scramble:8,ink:8.4,baboon:8.3,cloud:8.7,eater:8.1}[V.name];
    wait(att,()=>{camTo(wide(L),1.2);[0,1,3,4,2].forEach((bi,k)=>wait(.35+k*.5,()=>V.attack(L.blds[bi],bi)))});
    wait(att+3.4,()=>{skyTo(GREYS,1.4);water.material.color.set('#8fa2b5');L.ribbon.visible=false});
    wait(att+3.9,()=>V.leave());
    wait(att+5.6,()=>{camTo({tx:X-4.6,ty:1.1,tz:-1,yaw:-.1,pitch:.16,R:[10,6]},1.1);wait(.6,()=>tikoSay(L.say.help,3))});
    wait(att+9.2,()=>{camTo(wide(L),1.6);showTitle('LAND '+(li+1),L.name,L.levels)});
    wait(att+12.6,()=>done(tk))}
  function throwWord(V,done){['W','O','R','D','S'].forEach((ch,i)=>wait(i*.3,()=>{const t=makeTile(ch,TILE_COLORS[(i*3)%10]);t.scale.setScalar(.8);const s=tiko.position.clone().add(v3(0,1.5,.3));throwT=T;
      const tgt=V.head().add(v3(rnd(1),rnd(.6),0)),d=s.distanceTo(tgt);sfx.whoosh();
      lob(t,s,tgt,.38+d*.02,1.4+d*.1,()=>{flash(tgt,3,0xffe080);starsAt(tgt,8);chunks(tgt,t.userData.mat.color,6,.8);sfx.blast();shake=.14;V.hit()})}));wait(5*.3+.9,done)}
  function pBoss(li,next){setup(li,'boss');const L=LANDS[li],V=VIL[li],X=L.X,tk=token;V.place();
    camSet(Object.assign({},V.cam));
    wait(.4,()=>{V.laugh();vSay(L,L.say.boss,2.6)});
    wait(2.9,()=>{tikoSay('Let’s finish this!',1.4)});
    wait(3.4,()=>throwWord(V,()=>{}));
    wait(5.9,()=>{vSay(L,L.say.lose,2.6);V.defeat(()=>{})});
    const fl={scramble:9,ink:10.4,baboon:11.4,cloud:9.2,eater:11.2}[V.name];
    wait(fl,()=>{sfx.shine();camTo(wide(L),1.6);const big=L.blds[2];
      const ring=new THREE.Mesh(new THREE.RingGeometry(.8,1.3,48),new THREE.MeshBasicMaterial({color:'#fff2a8',transparent:true,side:THREE.DoubleSide,blending:THREE.AdditiveBlending,depthWrite:false}));ring.rotation.x=-Math.PI/2;ring.position.set(X,.25,-2);scene.add(ring);
      tween(1.8,(e,k)=>{ring.scale.setScalar(1+e*24);ring.material.opacity=1-k},{done:()=>scene.remove(ring)});skyTo(L.sky,1.8);
      tween(1.4,e=>big.set(e),{delay:.5,ease:E.inOut});wait(1.2,()=>{let sw=false;tween(.8,(e,k)=>{big.sign.rotation.y=k*Math.PI*2;if(!sw&&k>.5){sw=true;big.setSign(big.d.sign,false)}},{ease:E.inOut})});
      wait(1.9,()=>{sfx.fanfare();confetti(v3(X,7,-4),140,4);cheerT=T;tikoSay(L.say.win,2.6)});
      wait(2.2,()=>showTitle('BOSS DEFEATED',L.v,L.name+' is safe',2.6))});
    wait(fl+5,()=>done(tk))}
  function pComplete(li,next){setup(li,'complete');const L=LANDS[li],X=L.X,tk=token,big=L.blds[2];big.set(1);big.setSign(big.d.sign,false);
    camSet({tx:X-17,ty:2.4,tz:-4,yaw:-.5,pitch:.2,R:[14,9]});tween(3.6,e=>{camP.tx=X-17+e*14;camP.yaw=-.5+e*.5},{ease:E.inOut});
    tiko.position.set(X-9,.2,-.3);walk(v3(X-1.6,.2,BZ+D/2+1.6),3.2);
    [[-14,8,0xff6fa8],[-9,9,0xffd166],[-4,10,0x7fe3ff],[2,9,0xb8f27a],[7,8,0xc3a6ff]].forEach(([x,y,c],i)=>wait(.6+i*.55,()=>firework(v3(X+x,y,-7),c)));
    wait(3.8,()=>{camTo({tx:X,ty:2.4,tz:-2.5,yaw:.05,pitch:.18,R:[10,8]},1);tikoSay('All 8 tasks done! Time for the grand opening!',2.4)});
    wait(5.4,()=>{sfx.pop();tone(1800,.08,{type:'square',vol:.05});const[l,r,b]=L.ribbon.children;
      tween(.9,(e,k)=>{l.position.x=-.8-e*.6;l.position.y=-e*1.1;l.rotation.z=e*1.2;r.position.x=.8+e*.6;r.position.y=-e*1.1;r.rotation.z=-e*1.2;b.position.y=-e*1.2;b.rotation.y=k*6},{ease:E.inCubic,done:()=>L.ribbon.visible=false});
      tween(.8,e=>big.door.material.emissiveIntensity=e*.9,{delay:.3});starsAt(L.ribbon.position.clone(),16);
      wait(.4,()=>{sfx.fanfare();shake=.12;tween(.8,(e,k)=>{const s=1+Math.sin(k*Math.PI*3)*(1-k)*.12;big.inner.scale.set(1/Math.sqrt(s),s,1/Math.sqrt(s))},{ease:E.lin});
        confetti(v3(X,7,-3),200,5);cheerT=T;[-9,3].forEach((x,i)=>wait(.2+i*.3,()=>firework(v3(X+x,10,-6),i?0xffd166:0xff6fa8)))})});
    const last=li===4;
    wait(7.2,()=>showCard(last?`<div class="k">FINAL LAND COMPLETE</div><div class="t">${L.street} is fixed!</div><div class="fprize"><span>🪙 1,500</span><span>🎁 8 boosters</span></div>`:`<div class="k">LAND ${li+1} COMPLETE</div><div class="t">${L.street} is fixed!</div><div class="fprize"><span>🪙 ${500+li*250}</span><span>🎁 ${3+li} boosters</span><span>${data.prize||L.prize}</span></div>${BR>0?`<div class="s">🧱 ${BR} bricks saved for the next land</div>`:''}`));
    wait(9.6,()=>tikoSay(last?'Every street in Wordnoid is fixed!':(BR>0?`You have ${BR} brick${BR>1?'s':''} left. Save them for the next land!`:'Great building! On to the next land!'),3.2));
    wait(13.2,()=>{card.classList.remove('show');if(tk===token&&next)(li===4?pFinale:pBridge)(li,next);else done(tk)})}
  function pBridge(li,next){setup(li,'bridge');const L=LANDS[li],N=LANDS[li+1],X=L.X,tk=token;tiko.position.set(X-1.6,.2,BZ+D/2+1.6);
    camSet(wide(L));camTo({tx:X+GAP/2,ty:1.5,tz:1,yaw:.55,pitch:.32,R:[34,11]},1.6);walk(v3(X+19.5,.2,1.3),1.8);
    wait(1.3,()=>tikoSay('Look! A bridge to '+N.name+'!',2.2));
    wait(1.8,()=>bridges[li].build());
    wait(4.4,()=>{camTo({tx:X+GAP-17,ty:1.8,tz:0,yaw:.25,pitch:.26,R:[20,9]},3.2);walk(v3(X+GAP-17,.2,1.3),3)});
    wait(7.6,()=>{camTo(wide(N),1.8);skyTo(N.sky,1.8);water.material.color.set(N.water);walk(v3(N.X-5,.2,-.3),1.4);showTitle('NEXT',N.name,N.levels,2.6)});
    wait(10.2,()=>{if(tk===token&&next&&li<4&&!data.skipIntro){emit('storyLand',{land:li+1});pIntro(li+1,next)}else done(tk)})}
  function pFinale(li){setup(4,'finale');const L=LANDS[4],X=L.X,tk=token;
    camSet({tx:X,ty:2,tz:-2,yaw:0,pitch:.2,R:[18,9]});tiko.position.set(X,.2,-.3);
    friends.forEach((f,i)=>{f.visible=true;f.position.set(X-26-i*1.8,.2,1.3);f.rotation.y=.9;const to=v3(X-7+i*2.6+(i>1?4.6:0),.2,.6+(i%2)*.7),d=1.6+i*.25;
      tween(2.6,(e,k)=>{f.position.lerpVectors(v3(X-26-i*1.8,.2,1.3),to,e);f.position.y=.2+Math.abs(Math.sin(k*Math.PI*7))*.35},{delay:.4+i*.2,ease:E.lin,done:()=>f.rotation.y=-.2})});
    wait(.6,()=>tikoSay('Everyone is free! Thank you for saving Wordnoid!',3));
    wait(3.8,()=>{scene.fog.far=900;scene.fog.near=300;camTo({tx:120,ty:10,tz:-6,yaw:.12,pitch:.42,R:[280,40]},4.2);
      LANDS.forEach((Lx,i)=>wait(1.6+i*.5,()=>{firework(v3(Lx.X-6,16,-8),[0xff6fa8,0x7fe3ff,0xb8f27a,0xc3a6ff,0xffd166][i]);firework(v3(Lx.X+6,18,-8),[0xffd166,0xff6fa8,0x7fe3ff,0xb8f27a,0xc3a6ff][i])}));
      wait(2,()=>showTitle('5 LANDS','Wordnoid is saved!','Every word is home again',3.4))});
    wait(9.4,()=>{scene.fog.near=80;scene.fog.far=200;camTo({tx:X,ty:2.2,tz:-1.5,yaw:-.1,pitch:.2,R:[20,10]},2.4);sfx.fanfare()});
    wait(11.4,()=>{cheerT=T+99;for(let i=0;i<22;i++)wait(i*.18,()=>confetti(v3(X+rnd(16),9,-1+rnd(4)),14,3))});
    wait(12.4,()=>showCard(`<div class="k">THE END… FOR NOW</div><div class="t">You saved Wordnoid!</div><div class="fprize"><span>🏆 Word Hero</span><span>🪙 2,000</span></div><div class="s">New lands are on the way. Keep playing daily for surprises!</div>`));
    wait(16,()=>done(tk))}
  const PARTS={intro:pIntro,boss:pBoss,complete:pComplete,bridge:pBridge};
  let land=0;
  function done(tk){if(tk!==token)return;cine(false);say.classList.remove('show');card.classList.remove('show');title.classList.remove('show');emit('storyDone',{})}
  return{onResize(){},start(){token++;const li=clamp(data.land|0,0,4),p=data.part||'intro';if(p==='finale')pFinale(4,true);else if(PARTS[p])PARTS[p](li,!!data.chain);else done(token)},
    dispose(){token++;cine(false);say.classList.remove('show');card.classList.remove('show');title.classList.remove('show');document.body.classList.remove('story');document.body.style.background=''},
    update(dt,t){const yaw=camP.yaw+Math.sin(t*.16)*.035,Rr=camP.R,p=camP.pitch;
      cam.position.set(camP.tx+Math.sin(yaw)*Math.cos(p)*Rr,camP.ty+Math.sin(p)*Rr,camP.tz+Math.cos(yaw)*Math.cos(p)*Rr);cam.lookAt(camP.tx,camP.ty,camP.tz);
      const sx=clamp(camP.tx,-10,250);sun.position.set(sx-10,18,14);sun.target.position.set(sx,0,-3);
      const a=water.geometry.attributes.position;for(let i=0;i<a.count;i++){const x=wpos0[i*3],y=wpos0[i*3+1];a.array[i*3+2]=Math.sin(x*.35+t*1.6)*.12+Math.cos(y*.3+t*1.2)*.1}a.needsUpdate=true;
      sway.forEach(g=>g.rotation.z=Math.sin(t*1.3+g.userData.ph)*.04);bob.forEach(b=>b.rotation.z=Math.sin(t*1.5+b.userData.ph)*.12);
      flags.forEach(f=>{const pa=f.geometry.attributes.position,p0=f.userData.p0;for(let i=0;i<pa.count;i++){const x=p0[i*3];pa.array[i*3+2]=Math.sin(x*4-t*6)*.12*(x+.55)}pa.needsUpdate=true});
      clouds.forEach(c=>{c.position.x+=dt*c.userData.sp;if(c.position.x>260)c.position.x=-40});
      boats.forEach((b,i)=>{b.position.y=-.75+Math.sin(t*1.5+i)*.12;b.rotation.z=Math.sin(t*1.2+i)*.06});
      if(!hop){const ch=T-cheerT<2.4||cheerT>T;tiko.position.y=.2+(ch?Math.abs(Math.sin(t*9))*.45:Math.abs(Math.sin(t*2.2))*.06)}
      friends.forEach((f,i)=>{if(f.visible&&cheerT>T)f.position.y=.2+Math.abs(Math.sin(t*9+i))*.45});
      tiko.userData.armR.rotation.z=T-throwT<.3?-.5-2.2*Math.sin((T-throwT)/.3*Math.PI):.5;
      VIL.forEach(v=>v.idle(t))}};
}


SCENES.story=sceneStory;
