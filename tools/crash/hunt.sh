#!/usr/bin/env bash
# Crash hunt: install the Play (release) build, open it, then let the monkey tap around. Prints any crash.
set -x
PKG=com.retroraja.wordnoid
adb install -r Wordnoid-release.apk
adb logcat -c
adb shell settings put secure immersive_mode_confirmations confirmed
adb shell am start -W -n $PKG/.MainActivity
sleep 40
adb shell pidof $PKG || echo "NOT RUNNING after start"
adb shell dumpsys window | grep -E "mCurrentFocus|mFocusedApp" | head
adb shell monkey -p $PKG --throttle 250 --pct-syskeys 0 --pct-appswitch 0 -s 7 1500 || true
sleep 5
adb shell pidof $PKG || echo "NOT RUNNING after monkey"
# Rotate / resize once (tablets and foldables ignore the landscape lock on Android 16).
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0; sleep 6
adb shell settings put system user_rotation 1; sleep 6
adb shell am start -n $PKG/.MainActivity; sleep 15
adb shell monkey -p $PKG --throttle 250 --pct-syskeys 0 --pct-appswitch 0 -s 11 800 || true
sleep 5
adb logcat -d > logcat.txt
echo "===== CRASH ====="
grep -nE "FATAL EXCEPTION|Fatal signal|ANR in|Render process|renderer.*(crash|gone|killed)|has died|AndroidRuntime" logcat.txt | head -40
grep -E "FATAL EXCEPTION" -A 40 logcat.txt | head -150
grep -E "Fatal signal" -A 25 logcat.txt | head -60
grep -E "ANR in" -A 15 logcat.txt | head -40
echo "===== END CRASH ====="
