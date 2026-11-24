#!/usr/bin/env bash
# Boss check: open boss levels 10, 20 and 50 and take small screenshots of the board (printed into the log).
set -x
OUT=smoke-out
PKG=com.retroraja.wordnoid
mkdir -p $OUT
pip install -q pillow >/dev/null 2>&1 || true
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
texts() { dump; grep -o 'text="[^"]*"' $OUT/ui.xml | head -30; }

for L in 10 20 30 40 50; do
  adb shell am force-stop $PKG
  adb logcat -c
  cat > $OUT/p$L.xml <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <int name="unlocked" value="$L" />
    <int name="coins" value="500" />
    <int name="save_version" value="4" />
    <int name="birth_year" value="1990" />
    <boolean name="seen_first_time" value="true" />
    <boolean name="seen_welcome_build3" value="true" />
    <boolean name="seen_welcome_update1" value="true" />
    <boolean name="seen_welcome_update11" value="true" />
    <boolean name="seen_town_build_tut" value="true" />
    <boolean name="seen_intro_gift" value="true" />
    <boolean name="seen_intro_spin" value="true" />
    <boolean name="seen_intro_challenges" value="true" />
    <boolean name="seen_prelevel_free" value="true" />
$(for i in 0 1 2 3 4; do echo "    <boolean name=\"seen_story_intro_$i\" value=\"true\" />"; done)
$(for i in $(seq 1 $((L-1))); do echo "    <int name=\"stars_$i\" value=\"3\" />"; done)
</map>
XML
  adb push $OUT/p$L.xml /data/local/tmp/wordnoid_v2.xml
  adb shell chmod 644 /data/local/tmp/wordnoid_v2.xml
  adb shell run-as $PKG mkdir -p shared_prefs
  adb shell run-as $PKG cp /data/local/tmp/wordnoid_v2.xml shared_prefs/wordnoid_v2.xml
  adb shell am start -n $PKG/.MainActivity
  sleep 22
  texts
  tap_text "▶ PLAY Level $L"; sleep 4; shot L$L-start
  tap_text "▶ PLAY"; sleep 1; shot L$L-0s; sleep 3; shot L$L-intro
  texts
  for k in 1 2 3; do tap_text "Let's go!"; sleep 3; done
  sleep 6; shot L$L-board
  sleep 5; shot L$L-board-later
  texts
  adb logcat -d > $OUT/logcat$L.txt
  echo "== FX log L$L =="; grep -iE "FX error|fx loop|Uncaught|chromium.*Console" $OUT/logcat$L.txt | head -20
  grep -E "FATAL EXCEPTION" -A 20 $OUT/logcat$L.txt | head -40
done

python3 - <<'PY'
import glob, base64, io
from PIL import Image
for f in sorted(glob.glob('smoke-out/*.png')):
    name = f.split('/')[-1][:-4]
    im = Image.open(f).convert('RGB'); im.thumbnail((640, 640))
    b = io.BytesIO(); im.save(b, 'JPEG', quality=55); s = base64.b64encode(b.getvalue()).decode()
    print(f'===== IMG {name} =====')
    for i in range(0, len(s), 3000): print(s[i:i+3000])
    print('===== END IMG =====')
PY
