#!/usr/bin/env bash
# Quick check on an emulator: install, open, play through screens and boosters, take screenshots, collect crashes.
# Screenshots are printed into the log as small JPEGs (base64), so they can be read without downloading artifacts.
set -x
OUT=smoke-out
PKG=com.retroraja.wordnoid
mkdir -p $OUT
pip install -q pillow >/dev/null 2>&1 || true
adb install -r Wordnoid.apk
adb logcat -c
adb shell settings put secure immersive_mode_confirmations confirmed
adb shell am start -n $PKG/.MainActivity
sleep 25
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
texts() { dump; grep -o 'text="[^"]*"' $OUT/ui.xml | head -40; }

# ---- First run: home, daily gift, level 1 tutorial.
shot home-daily
texts
tap_text "Open!"; sleep 2; shot daily-opened
tap_text "Great!"; sleep 2
tap_text "PLAY"; sleep 4; shot map
tap_text "1"; sleep 3; shot level-start
tap_text "▶ PLAY"; sleep 4; shot intro
tap_text "Let's go!"; sleep 5; shot board-tutorial
texts
W=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1); echo "size $W"
# Swipes across the board area (centre of a landscape screen).
for s in "1000 450 1150 450" "1150 600 1150 750" "900 350 1050 350" "1300 450 1300 600" "800 600 950 600"; do
  adb shell input swipe $s 150; sleep 3
done
shot after-swaps
texts
tap_text "🔨"; sleep 1; adb shell input tap 1170 520; sleep 0.5; shot hammer-mid; sleep 3; shot after-hammer
tap_text "💡"; sleep 1; shot hint

# ---- Second run: a late level with everything unlocked.
adb shell am force-stop $PKG
DAY=$(( $(date +%s) / 86400 ))
cat > $OUT/prefs.xml <<EOF
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <int name="unlocked" value="50" />
    <int name="coins" value="2000" />
    <int name="meter" value="6" />
    <long name="daily_last" value="$DAY" />
    <int name="daily_count" value="1" />
$(for b in HINT HAMMER MAGIC_SWAP JOKER UNDO SHUFFLE PAINT FREE_MOVE PLUS_MOVES SPIN ROW_SLIDE COLUMN_SLIDE VOWEL_FLIP DICE SCANNER AXE LOCK DOUBLE MAGNET LETTER_RAIN; do echo "    <int name=\"b_$b\" value=\"3\" />"; done)
$(for i in $(seq 1 49); do echo "    <int name=\"stars_$i\" value=\"$(( (i % 3) + 1 ))\" />"; done)
</map>
EOF
adb push $OUT/prefs.xml /data/local/tmp/wordnoid_v2.xml
adb shell chmod 644 /data/local/tmp/wordnoid_v2.xml
adb shell run-as $PKG mkdir -p shared_prefs
adb shell run-as $PKG cp /data/local/tmp/wordnoid_v2.xml shared_prefs/wordnoid_v2.xml
adb shell am start -n $PKG/.MainActivity
sleep 20
shot home-late
tap_text "PLAY"; sleep 5; shot map-late
texts
tap_text "50"; sleep 3; shot start-50
tap_text "+3 Moves"; sleep 1
tap_text "▶ PLAY"; sleep 4; shot intro-50
tap_text "Let's go!"; sleep 5; shot board-50
texts
tap_text "⋯"; sleep 2; shot more-boosters
tap_text "🔍"; sleep 2; shot scanner
for s in "1000 450 1150 450" "1150 600 1150 750" "900 350 1050 350"; do adb shell input swipe $s 150; sleep 3; done
shot after-swaps-50
tap_text "🔨"; sleep 1; adb shell input tap 1100 700; sleep 0.4; shot hammer-50; sleep 3
adb shell input keyevent 4; sleep 3; shot back-to-map

adb logcat -d > $OUT/logcat.txt
grep -E "FATAL EXCEPTION" -A 30 $OUT/logcat.txt > $OUT/crash.txt || true
echo "crash lines: $(wc -l < $OUT/crash.txt)"
echo "===== CRASH ====="
head -120 $OUT/crash.txt
echo "===== END CRASH ====="
grep -E "Wordnoid|Choreographer.*Skipped" $OUT/logcat.txt | head -30
python3 - <<'PY'
import glob, base64, io
try:
    from PIL import Image
except Exception:
    Image = None
for f in sorted(glob.glob('smoke-out/*.png')):
    name = f.split('/')[-1][:-4]
    data = open(f, 'rb').read()
    if Image:
        im = Image.open(io.BytesIO(data)).convert('RGB')
        im.thumbnail((1170, 1170))
        b = io.BytesIO(); im.save(b, 'JPEG', quality=70); data = b.getvalue()
    s = base64.b64encode(data).decode()
    print(f'===== IMG {name} =====')
    for i in range(0, len(s), 3000):
        print(s[i:i+3000])
    print('===== END IMG =====')
PY
