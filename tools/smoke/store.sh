#!/usr/bin/env bash
# Store screenshots: real gameplay at full size. A small solver reads the board letters from the UI dump
# and makes real word swaps, so the shots show words matching. Images are printed into the log as JPEG (base64).
set -x
OUT=smoke-out
PKG=com.retroraja.wordnoid
LV=${STORE_LEVEL:-9}
mkdir -p $OUT
pip install -q pillow >/dev/null 2>&1 || true
unzip -p Wordnoid.apk assets/play_words.txt > $OUT/words.txt
adb install -r Wordnoid.apk
adb shell settings put secure immersive_mode_confirmations confirmed
N=0
shot() { N=$((N+1)); adb exec-out screencap -p > "$OUT/$(printf %02d $N)-$1.png"; }
dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml $OUT/ui.xml >/dev/null 2>&1; }
tap_text() {
  dump
  python3 - "$1" <<'PY'
import re,sys,subprocess
t=sys.argv[1]
x=open('smoke-out/ui.xml',encoding='utf-8').read()
ms=list(re.finditer(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',x))
ms=[m for m in ms if m.group(1)==t]+[m for m in ms if m.group(1)!=t]
for m in ms:
    if m.group(1)==t or (len(t)>3 and t in m.group(1)):
        a,b,c,d=map(int,m.groups()[1:])
        subprocess.run(['adb','shell','input','tap',str((a+c)//2),str((b+d)//2)])
        print('tapped',repr(t)); break
else:
    print('not found',repr(t))
PY
}
tap_above() { dump; python3 - "$1" <<'PY'
import re,sys,subprocess
x=open('smoke-out/ui.xml',encoding='utf-8').read()
m=re.search(r'text="'+re.escape(sys.argv[1])+r'"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',x)
if m:
    a,b,c,d=map(int,m.groups()); subprocess.run(['adb','shell','input','tap',str((a+c)//2),str(b-90)]); print('tapped above',sys.argv[1])
else: print('not found',sys.argv[1])
PY
}
has_text() { dump; grep -q "text=\"[^\"]*$1" $OUT/ui.xml; }
# Find a swap that makes a real word through a moved tile, and swipe it. Prints the word.
solve() {
  dump
  python3 - <<'PY'
import re,subprocess
x=open('smoke-out/ui.xml',encoding='utf-8').read()
W=set(w.strip().upper() for w in open('smoke-out/words.txt',encoding='utf-8',errors='ignore') if 3<=len(w.strip())<=8)
ts=[(m.group(1),*map(int,m.groups()[1:])) for m in re.finditer(r'text="([A-Z])"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',x)]
# Board tiles are the big single letters (skip small text such as the meter letters).
print('letters',len(ts),[(t[0],t[3]-t[1],t[4]-t[2]) for t in ts[:6]])
ts=[t for t in ts if (t[4]-t[2])>30 and 500<(t[1]+t[3])//2<1850]
if len(ts)<8: print('no board'); raise SystemExit
def cl(v):
    v=sorted(set(v)); out=[]
    for a in v:
        if not out or a-out[-1][-1]>30: out.append([a])
        else: out[-1].append(a)
    return [sum(c)/len(c) for c in out]
xs=cl([(a+c)//2 for _,a,b,c,d in ts]); ys=cl([(b+d)//2 for _,a,b,c,d in ts])
near=lambda v,L:min(range(len(L)),key=lambda i:abs(L[i]-v))
G={}
for ch,a,b,c,d in ts: G[(near((b+d)//2,ys),near((a+c)//2,xs))]=ch
R,C=len(ys),len(xs)
def words_at(g,p):
    found=[]
    for dr,dc in ((0,1),(1,0)):
        line=[];r,c=p
        while (r-dr,c-dc) in g: r,c=r-dr,c-dc
        while (r,c) in g: line.append(((r,c),g[(r,c)])); r,c=r+dr,c+dc
        s=''.join(ch for _,ch in line); k=[q for q,_ in line].index(p)
        for i in range(len(s)):
            for j in range(i+3,len(s)+1):
                if i<=k<j:
                    w=s[i:j]
                    if w in W: found.append(w)
                    if w[::-1] in W: found.append(w[::-1])
    return found
best=None
for (r,c) in list(G):
    for q in ((r,c+1),(r+1,c)):
        if q not in G or G[q]==G[(r,c)]: continue
        g=dict(G); g[(r,c)],g[q]=G[q],G[(r,c)]
        ws=words_at(g,(r,c))+words_at(g,q)
        if ws:
            sc=max(len(w) for w in ws)
            if not best or sc>best[0]: best=(sc,(r,c),q,max(ws,key=len))
if not best: print('no move'); raise SystemExit
_,p,q,w=best
x1,y1,x2,y2=int(xs[p[1]]),int(ys[p[0]]),int(xs[q[1]]),int(ys[q[0]])
subprocess.run(['adb','shell','input','swipe',str(x1),str(y1),str(x2),str(y2),'150'])
print('WORD',w,p,q)
PY
}

DAY=$(( $(date +%s) / 86400 ))
cat > $OUT/prefs.xml <<EOF
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <int name="unlocked" value="$LV" />
    <int name="coins" value="1250" />
    <int name="meter" value="5" />
    <int name="birth_year" value="1990" />
    <boolean name="seen_first_time" value="true" />
    <boolean name="seen_welcome_build3" value="true" />
    <boolean name="seen_welcome_build4" value="true" />
    <boolean name="seen_welcome_build5" value="true" />
    <boolean name="seen_welcome_build6" value="true" />
    <boolean name="seen_town_build_tut" value="true" />
    <boolean name="seen_intro_gift" value="true" />
    <boolean name="seen_intro_spin" value="true" />
    <boolean name="seen_intro_challenges" value="true" />
    <boolean name="seen_mystery_first" value="true" />
    <boolean name="seen_prelevel_free" value="true" />
    <boolean name="seen_lock_tut" value="true" />
$(for p in intro boss; do for l in 0 1; do echo "    <boolean name=\"seen_story_${p}_$l\" value=\"true\" />"; done; done)
$(for i in $(seq 1 $LV); do echo "    <boolean name=\"seen_intro_level_$i\" value=\"true\" />"; done)
$(for b in HINT HAMMER MAGIC_SWAP JOKER UNDO SHUFFLE PAINT FREE_MOVE PLUS_MOVES SPIN ROW_SLIDE COLUMN_SLIDE VOWEL_FLIP DICE SCANNER AXE LOCK DOUBLE MAGNET LETTER_RAIN; do echo "    <int name=\"b_$b\" value=\"3\" /><boolean name=\"seen_intro_b_$b\" value=\"true\" />"; done)
$(for i in $(seq 1 $((LV-1))); do echo "    <int name=\"stars_$i\" value=\"3\" />"; done)
$(for i in 0 1 2; do echo "    <boolean name=\"task_0_$i\" value=\"true\" />"; done)
</map>
EOF
adb shell am start -n $PKG/.MainActivity; sleep 8; adb shell am force-stop $PKG
adb push $OUT/prefs.xml /data/local/tmp/wordnoid_v2.xml
adb shell chmod 644 /data/local/tmp/wordnoid_v2.xml
adb shell run-as $PKG mkdir -p shared_prefs
adb shell run-as $PKG cp /data/local/tmp/wordnoid_v2.xml shared_prefs/wordnoid_v2.xml
adb logcat -c
adb shell am start -n $PKG/.MainActivity
sleep 24
shot town
for t in "Let's go!" "Got it" "OK"; do tap_text "$t"; sleep 1; done
tap_text "▶ PLAY Level $LV"; sleep 4; shot level-start
tap_text "Let's go!"; sleep 6
for i in 1 2 3; do has_text "Let's go" && { tap_text "Let's go!"; sleep 3; }; has_text "Got it" && { tap_text "Got it"; sleep 2; }; done
shot board
# Real word swaps: catch the 3D word pop right after each swap.
for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
  solve; sleep 0.4; shot word-$i
  sleep 2.5
  if has_text "COMPLETE"; then sleep 2; shot win; break; fi
  if has_text "Out of moves\|OUT OF MOVES"; then shot lose; break; fi
  for t in "Let's go!" "Got it" "Pick"; do has_text "$t" && { tap_text "$t"; sleep 2; }; done
  [ $i = 2 ] && { tap_above "Hammer"; sleep 1.5; shot hammer-armed; adb shell input tap 1385 620; sleep 0.5; shot hammer-hit; sleep 3; }
  [ $i = 4 ] && { tap_above "Joker"; sleep 1.5; shot joker-armed; tap_above "Joker"; sleep 1; }
done
sleep 4; shot end

adb logcat -d > $OUT/logcat.txt
grep -E "FATAL EXCEPTION" -A 30 $OUT/logcat.txt > $OUT/crash.txt || true
echo "crash lines: $(wc -l < $OUT/crash.txt)"
python3 - <<'PY'
import glob, base64, io
from PIL import Image
for f in sorted(glob.glob('smoke-out/*.png')):
    name = f.split('/')[-1][:-4]
    im = Image.open(f).convert('RGB')
    b = io.BytesIO(); im.save(b, 'JPEG', quality=92); s = base64.b64encode(b.getvalue()).decode()
    print(f'===== IMG {name} =====')
    for i in range(0, len(s), 3000): print(s[i:i+3000])
    print('===== END IMG =====')
PY
