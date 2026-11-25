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
adb shell wm density 280 || true
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

FROM=0
st() { tail -n +$FROM $OUT/wn.log | grep ' WNVID' | tail -n1 | sed 's/.*WNVID *: //'; }
# Wait until the game is ready for a move (or ended / shows cards). Up to $1 seconds.
wait_ready() { for k in $(seq 1 $(( $1 * 5 ))); do S=$(st); case "$S" in MOVE*|END*|CARDS|NOMOVE) echo "$S"; return;; esac; sleep 0.2; done; echo "$S"; }
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
    <int name="meter" value="0" />
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
adb shell am start -n $PKG/.MainActivity; sleep 12; adb shell am force-stop $PKG
adb logcat -c
adb logcat -v epoch -s WNVID:I WNSND:I > $OUT/wn.log &
LOGCAT=$!
for LV in $LEVELS; do
  prefs $LV
  adb shell am start -n $PKG/.MainActivity
  sleep 20
  for t in "Let's go!" "Got it" "OK" "Close"; do tap_text "$t"; sleep 1; done
  FROM=$(( $(wc -l < $OUT/wn.log) + 1 ))
  adb shell screenrecord --bit-rate 10000000 --time-limit 180 /sdcard/rec-$LV.mp4 &
  REC=$!
  ev REC_START $LV
  sleep 1
  tap_text "▶ PLAY Level $LV"; ev TAP play; sleep 2.5
  tap_text "Let's go!"; ev TAP letsgo
  N=0
  LSTART=$(date +%s)
  for i in $(seq 1 45); do
    [ $(( $(date +%s) - LSTART )) -gt 170 ] && break
    S=$(wait_ready 10); echo "state: $S"
    case "$S" in
      MOVE*)
        set -- $S
        N=$((N+1))
        if [ $LV = 8 ] && [ $N = 3 ]; then
          tap_above "Hammer"; ev BOOST Hammer; sleep 1.2
          adb shell input tap $6 $7; ev HIT hammer; sleep 2.5; continue
        fi
        if [ $LV = 10 ] && [ $N = 5 ]; then
          tap_above "Hammer"; ev BOOST Hammer; sleep 1.2
          adb shell input tap $6 $7; ev HIT hammer; sleep 2.5; continue
        fi
        sleep 0.5
        ev SWAP $N
        adb shell input swipe $2 $3 $4 $5 260
        for k in $(seq 1 15); do [ "$(st)" != "$S" ] && break; sleep 0.2; done
        ;;
      CARDS)
        sleep 1.5; ev CARDS
        for y in 380 330 430 300; do
          adb shell input tap 640 $y; sleep 2.2
          tap_text "Collect!"; sleep 1.5
          [ "$(st)" = CARDS ] || break
        done
        ;;
      "END WON") ev WIN $LV; sleep 11; break;;
      "END LOST") ev LOSE $LV; sleep 3; break;;
      *)
        for t in "Let's go!" "Got it" "Nice" "OK"; do tap_text "$t"; done; sleep 1;;
    esac
  done
  adb shell pkill -INT screenrecord
  for k in 1 2 3 4 5 6 7 8; do sleep 1; [ -z "$(adb shell pidof screenrecord)" ] && break; done
  adb shell pkill -9 screenrecord; sleep 1; kill $REC 2>/dev/null
  ev REC_END $LV
  adb pull /sdcard/rec-$LV.mp4 $OUT/rec-$LV.mp4
done
kill $LOGCAT
# The game's own sounds and music (debug builds save them), for the soundtrack.
mkdir -p $OUT/sfx
for f in $(adb shell run-as $PKG ls cache/sfx); do adb exec-out run-as $PKG cat cache/sfx/$f > $OUT/sfx/$f; done
for p in HOME MAP LEVEL; do adb exec-out run-as $PKG cat cache/music_$p.wav > $OUT/sfx/music_$p.wav; done
ls -la $OUT $OUT/sfx
cat $OUT/events.txt
grep -c WNSND $OUT/wn.log
