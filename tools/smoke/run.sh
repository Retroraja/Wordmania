#!/usr/bin/env bash
# Quick check on an emulator: install, open, go through a few screens, take screenshots, collect crashes.
set -x
OUT=smoke-out
mkdir -p $OUT
adb install -r Wordnoid.apk
adb logcat -c
adb shell am start -n com.retroraja.wordnoid/.MainActivity
sleep 12
shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml $OUT/ui.xml >/dev/null 2>&1
  python3 - "$1" <<'PY'
import re,sys,subprocess
t=sys.argv[1]
x=open('smoke-out/ui.xml',encoding='utf-8').read()
for m in re.finditer(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',x):
    if m.group(1)==t:
        a,b,c,d=map(int,m.groups()[1:])
        subprocess.run(['adb','shell','input','tap',str((a+c)//2),str((b+d)//2)])
        print('tapped',t); break
else:
    print('not found',t)
PY
}
shot 1-title
tap_text "PLAY"; sleep 4; shot 2-map
tap_text "1"; sleep 4; shot 3-intro
tap_text "Let's go!"; sleep 3; shot 4-board
# Swipe across the middle of the screen a few times to make moves.
W=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1); echo "size $W"
adb shell input swipe 1000 500 1100 500 150; sleep 4; shot 5-after-swap1
adb shell input swipe 1100 600 1100 700 150; sleep 4; shot 6-after-swap2
adb shell input swipe 900 400 1000 400 150; sleep 4; shot 7-after-swap3
adb shell input keyevent 4; sleep 2; shot 8-pause
adb logcat -d > $OUT/logcat.txt
grep -E "FATAL EXCEPTION|AndroidRuntime" -A 30 $OUT/logcat.txt > $OUT/crash.txt || true
echo "crash lines: $(wc -l < $OUT/crash.txt)"
echo "===== CRASH ====="
cat $OUT/crash.txt | head -120
echo "===== END CRASH ====="
# Small screenshots printed into the log (base64 JPEG), so they can be read without downloading artifacts.
for f in $OUT/*.png; do
  convert "$f" -resize 900x900 -quality 60 "${f%.png}.jpg" 2>/dev/null || continue
  echo "===== IMG $(basename "${f%.png}") ====="
  base64 -w0 "${f%.png}.jpg"; echo
  echo "===== END IMG ====="
done
