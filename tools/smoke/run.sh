#!/usr/bin/env bash
# Crash repro: Level 3 with the Power Meter almost full, make HOP, watch for crash/ANR.
set -x
OUT=smoke-out; PKG=com.retroraja.wordnoid; mkdir -p $OUT
pip install -q pillow >/dev/null 2>&1 || true
adb root || true; sleep 3
adb install -r Wordnoid.apk
adb logcat -c
adb shell settings put secure immersive_mode_confirmations confirmed
cat > $OUT/prefs.xml <<X
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <int name="unlocked" value="3" />
    <int name="coins" value="300" />
    <int name="meter" value="7" />
    <int name="birth_year" value="1990" />
    <int name="stars_1" value="3" />
    <int name="stars_2" value="3" />
    <boolean name="seen_first_time" value="true" />
    <boolean name="seen_welcome_build3" value="true" />
    <boolean name="seen_town_build_tut" value="true" />
    <boolean name="seen_intro_level_3" value="true" />
</map>
X
adb shell am start -n $PKG/.MainActivity; sleep 3; adb shell am force-stop $PKG
adb push $OUT/prefs.xml /data/local/tmp/w.xml; adb shell chmod 644 /data/local/tmp/w.xml
adb shell run-as $PKG mkdir -p shared_prefs
adb shell run-as $PKG cp /data/local/tmp/w.xml shared_prefs/wordnoid_v2.xml
adb shell am start -n $PKG/.MainActivity
sleep 30
N=0
shot() { N=$((N+1)); adb exec-out screencap -p > "$OUT/$(printf %02d $N)-$1.png"; }
dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml $OUT/ui.xml >/dev/null 2>&1; }
tap_text() {
  dump
  python3 - "$1" <<'PY'
import re,sys,subprocess
t=sys.argv[1]
x=open('smoke-out/ui.xml',encoding='utf-8').read()
for m in re.finditer(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',x):
    if m.group(1)==t or (len(t)>3 and t in m.group(1)):
        a,b,c,d=map(int,m.groups()[1:])
        subprocess.run(['adb','shell','input','tap',str((a+c)//2),str((b+d)//2)])
        print('tapped',repr(t)); break
else:
    print('not found',repr(t))
PY
}
shot town
tap_text "PLAY Level 3"; sleep 4; shot start
tap_text "▶ PLAY"; sleep 6; shot board
tap_text "Let's go!"; sleep 3
adb shell input tap 1072 388; sleep 1; adb shell input tap 1072 544
for i in 1 2 3 4 5 6 7 8; do sleep 1.5; shot "word-$i"; done
sleep 10; shot later
adb shell pidof $PKG; echo "pid above (empty = dead)"
adb logcat -d > $OUT/logcat.txt
echo "===== CRASH ====="
grep -E "FATAL EXCEPTION|ANR in|AndroidRuntime|chromium.*(Fatal|crash)|Render process|libc.*Fatal" -A 40 $OUT/logcat.txt | head -200
echo "===== END CRASH ====="
adb shell ls -l /data/anr/ || true
adb shell "cat /data/anr/* 2>/dev/null" | grep -A 60 '"main"' | head -90
python3 - <<'PY'
import glob, base64, io
from PIL import Image
for f in sorted(glob.glob('smoke-out/*.png')):
    name = f.split('/')[-1][:-4]
    im = Image.open(f).convert('RGB'); im.thumbnail((1170, 1170))
    b = io.BytesIO(); im.save(b, 'JPEG', quality=70)
    s = base64.b64encode(b.getvalue()).decode()
    print(f'===== IMG {name} =====')
    for i in range(0, len(s), 3000): print(s[i:i+3000])
    print('===== END IMG =====')
PY
