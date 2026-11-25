#!/usr/bin/env bash
# YouTube gameplay recording: real play on the emulator, "Show taps" on, game audio captured from the host
# (PulseAudio null sink). A small solver reads the board from the UI dump and makes the best word swap.
# Every action is logged with a host timestamp in events.txt so the edit can cut out idle time.
set -x
OUT=video-out
PKG=com.retroraja.wordnoid
LEVELS=${VIDEO_LEVELS:-"5 8 10"}
mkdir -p $OUT
unzip -p Wordnoid.apk assets/play_words.txt > $OUT/words.txt
adb install -r Wordnoid.apk
adb shell settings put secure immersive_mode_confirmations confirmed
adb shell settings put system show_touches 1
adb shell svc wifi disable; adb shell svc data disable
adb shell media volume --stream 3 --set 15 || true
ev() { echo "$(date +%s.%N) $*" >> $OUT/events.txt; }
dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml $OUT/ui.xml >/dev/null 2>&1; }
tap_text() {
  dump
  python3 - "$1" <<'PY'
import re,sys,subprocess
t=sys.argv[1]
x=open('video-out/ui.xml',encoding='utf-8').read()
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
has_text() { dump; grep -q "text=\"[^\"]*$1" $OUT/ui.xml; }
# MODE=swap: best word swap. MODE=tile: print the centre of a tile in the middle of the board.
solve() {
  dump
  python3 - "$1" <<'PY'
import re,subprocess,sys,time
mode=sys.argv[1]
x=open('video-out/ui.xml',encoding='utf-8').read()
W=set(w.strip().upper() for w in open('video-out/words.txt',encoding='utf-8',errors='ignore') if 3<=len(w.strip())<=9)
m=re.search(r'bounds="\[0,0\]\[(\d+),(\d+)\]"',x); SW=int(m.group(1)) if m else 1920
ts=[(m.group(1),*map(int,m.groups()[1:])) for m in re.finditer(r'text="([A-Z])"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',x)]
ts=[t for t in ts if (t[4]-t[2])>SW*0.013 and SW*0.2<(t[1]+t[3])//2<SW*0.8]
if len(ts)<8: print('no board'); raise SystemExit
def cl(v):
    v=sorted(set(v)); out=[]
    for a in v:
        if not out or a-out[-1][-1]>SW*0.013: out.append([a])
        else: out[-1].append(a)
    return [sum(c)/len(c) for c in out]
xs=cl([(a+c)//2 for _,a,b,c,d in ts]); ys=cl([(b+d)//2 for _,a,b,c,d in ts])
near=lambda v,L:min(range(len(L)),key=lambda i:abs(L[i]-v))
G={}
for ch,a,b,c,d in ts: G[(near((b+d)//2,ys),near((a+c)//2,xs))]=ch
if mode=='tile':
    ks=sorted(G,key=lambda k:abs(k[0]-len(ys)/2)+abs(k[1]-len(xs)/2)); r,c=ks[0]
    print('TILE',int(xs[c]),int(ys[r])); raise SystemExit
def words_at(g,p):
    found=[]
    for dr,dc in ((0,1),(1,0)):
        line=[];r,c=p
        while (r-dr,c-dc) in g: r,c=r-dr,c-dc
        while (r,c) in g: line.append(((r,c),g[(r,c)])); r,c=r+dr,c+dc
        s=''.join(ch for _,ch in line); k=[q for q,_ in line].index(p)
        best=None
        for i in range(len(s)):
            for j in range(i+3,len(s)+1):
                if i<=k<j:
                    for w in (s[i:j],s[i:j][::-1]):
                        if w in W and (not best or len(w)>len(best)): best=w
        if best: found.append(best)
    return found
best=None
for (r,c) in list(G):
    for q in ((r,c+1),(r+1,c)):
        if q not in G or G[q]==G[(r,c)]: continue
        g=dict(G); g[(r,c)],g[q]=G[q],G[(r,c)]
        ws=set(words_at(g,(r,c))+words_at(g,q))
        if ws:
            sc=sum(len(w)**2 for w in ws)
            if not best or sc>best[0]: best=(sc,(r,c),q,'+'.join(sorted(ws,key=len,reverse=True)))
if not best: print('no move'); raise SystemExit
_,p,q,w=best
x1,y1,x2,y2=int(xs[p[1]]),int(ys[p[0]]),int(xs[q[1]]),int(ys[q[0]])
open('video-out/events.txt','a').write('%.3f SWAP %s\n'%(time.time(),w))
subprocess.run(['adb','shell','input','swipe',str(x1),str(y1),str(x2),str(y2),'220'])
print('WORD',w,p,q)
PY
}
tap_above() { dump; python3 - "$1" <<'PY'
import re,sys,subprocess
x=open('video-out/ui.xml',encoding='utf-8').read()
m=re.search(r'text="'+re.escape(sys.argv[1])+r'"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',x)
if m:
    a,b,c,d=map(int,m.groups()); subprocess.run(['adb','shell','input','tap',str((a+c)//2),str(b-int((d-b)*1.6))]); print('tapped above',sys.argv[1])
else: print('not found',sys.argv[1])
PY
}
prefs() {
LV=$1
DAY=$(( $(date +%s) / 86400 ))
cat > $OUT/prefs.xml <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <int name="unlocked" value="$LV" />
    <int name="coins" value="1250" />
    <int name="meter" value="5" />
    <int name="birth_year" value="1990" />
    <int name="save_version" value="4" />
    <boolean name="seen_first_time" value="true" />
$(for k in seen_town_build_tut seen_intro_gift seen_intro_minigames seen_intro_spin seen_intro_challenges seen_mystery_first seen_mystery_voice seen_meter_voice seen_power_tip seen_twin_tip seen_ice_swapped seen_prelevel_free seen_lock_tut seen_welcome_update11 seen_quiz_intro seen_story_prize_0 seen_story_prize_1; do echo "    <boolean name=\"$k\" value=\"true\" />"; done)
$(for p in intro boss; do for l in 0 1; do echo "    <boolean name=\"seen_story_${p}_$l\" value=\"true\" />"; done; done)
$(for i in $(seq 1 $LV); do echo "    <boolean name=\"seen_intro_level_$i\" value=\"true\" />"; done)
$(for b in HINT HAMMER MAGIC_SWAP JOKER UNDO SHUFFLE PAINT FREE_MOVE PLUS_MOVES SPIN ROW_SLIDE COLUMN_SLIDE VOWEL_FLIP DICE SCANNER AXE LOCK DOUBLE MAGNET LETTER_RAIN; do echo "    <int name=\"b_$b\" value=\"3\" /><boolean name=\"seen_intro_b_$b\" value=\"true\" />"; done)
$(for i in $(seq 1 $((LV-1))); do echo "    <int name=\"stars_$i\" value=\"3\" />"; done)
$(for i in 0 1 2; do echo "    <boolean name=\"task_0_$i\" value=\"true\" />"; done)
    <int name="gift_day" value="$DAY" />
</map>
XML
adb shell am force-stop $PKG
adb push $OUT/prefs.xml /data/local/tmp/wordnoid_v2.xml
adb shell chmod 644 /data/local/tmp/wordnoid_v2.xml
adb shell run-as $PKG mkdir -p shared_prefs
adb shell run-as $PKG cp /data/local/tmp/wordnoid_v2.xml shared_prefs/wordnoid_v2.xml
}

# First start creates the app's folders.
adb shell am start -n $PKG/.MainActivity; sleep 10; adb shell am force-stop $PKG

parec -d rec.monitor --file-format=wav --rate=44100 --channels=2 $OUT/audio.wav &
PAREC=$!
ev AUDIO_START
for LV in $LEVELS; do
  prefs $LV
  adb shell am start -n $PKG/.MainActivity
  sleep 22
  for t in "Let's go!" "Got it" "OK" "Close"; do tap_text "$t"; sleep 1; done
  adb shell screenrecord --bit-rate 12000000 --time-limit 180 /sdcard/rec-$LV.mp4 &
  REC=$!
  ev REC_START $LV
  sleep 1
  tap_text "▶ PLAY Level $LV"; ev TAP play; sleep 3
  tap_text "Let's go!"; ev TAP letsgo; sleep 4
  for i in 1 2 3; do has_text "Let's go" && { tap_text "Let's go!"; ev TAP letsgo; sleep 3; }; has_text "Got it" && { tap_text "Got it"; ev TAP gotit; sleep 2; }; done
  for i in $(seq 1 30); do
    solve swap; sleep 3.2
    if has_text "COMPLETE\|You beat"; then ev WIN $LV; sleep 9; break; fi
    if has_text "OUT OF MOVES\|Out of moves"; then ev LOSE $LV; sleep 3; break; fi
    for t in "Let's go!" "Got it" "Pick" "Nice"; do has_text "$t" && { tap_text "$t"; ev TAP $t; sleep 2; }; done
    if [ $LV = 8 ] && [ $i = 3 ]; then
      tap_above "Hammer"; ev BOOST Hammer; sleep 1.5
      T=$(solve tile | grep TILE); adb shell input tap $(echo $T | cut -d' ' -f2) $(echo $T | cut -d' ' -f3); ev HIT hammer; sleep 3
    fi
    if [ $LV = 10 ] && [ $i = 4 ]; then
      tap_above "Joker"; ev BOOST Joker; sleep 1.5
      T=$(solve tile | grep TILE); adb shell input tap $(echo $T | cut -d' ' -f2) $(echo $T | cut -d' ' -f3); ev HIT joker; sleep 3
    fi
  done
  adb shell pkill -INT screenrecord; sleep 2; wait $REC
  ev REC_END $LV
  adb pull /sdcard/rec-$LV.mp4 $OUT/rec-$LV.mp4
done
kill -INT $PAREC; sleep 1
ev AUDIO_END
adb logcat -d > $OUT/logcat.txt
grep -E "FATAL EXCEPTION" -A 30 $OUT/logcat.txt > $OUT/crash.txt || true
rm -f $OUT/ui.xml $OUT/prefs.xml $OUT/words.txt
ls -la $OUT
cat $OUT/events.txt
