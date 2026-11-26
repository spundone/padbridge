#!/usr/bin/env bash
# Captures the README screenshots on an emulator with a debug build in demo mode (a fake connection to
# "MacBook Pro", made-up device names, dark theme, clean status bar). Nothing real is shown.
#
#   ./gradlew assembleGithubStandardDebug
#   scripts/screenshots.sh <avd-name> phone    # e.g. a Pixel AVD, portrait
#   scripts/screenshots.sh <avd-name> tablet   # e.g. pixel_tablet (natural landscape)
#
# Needs ANDROID_HOME (default ~/android-sdk). PNGs land in ~/shots/<avd-name>/.
set -u
SDK=${ANDROID_HOME:-$HOME/android-sdk}
REPO=$(cd "$(dirname "$0")/.." && pwd)
AVD=$1; OUT=~/shots/$AVD; rm -rf $OUT; mkdir -p $OUT
A="$SDK/platform-tools/adb -s emulator-5554"; S="$A shell input"  # the emulator only, never a connected phone
cd $SDK && (nohup emulator/emulator -avd $AVD -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect > /tmp/emu.log 2>&1 &)
timeout 300 $A wait-for-device; for i in $(seq 1 80); do [ "$($A shell getprop sys.boot_completed 2>/dev/null | tr -d "\r")" = 1 ] && break; sleep 4; done; sleep 15
$A shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1
$A shell cmd uimode night yes
$A shell settings put global sysui_demo_allowed 1
$A shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
$A shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941 >/dev/null
$A shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
$A shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 >/dev/null
$A shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null
$A uninstall dev.padbridge.dev >/dev/null 2>&1; $A uninstall dev.padbridge >/dev/null 2>&1
$A install -r -g $REPO/app/build/outputs/apk/github/debug/app-github-debug.apk >/dev/null
$A shell settings put system accelerometer_rotation 0; $A shell settings put system user_rotation 0
center() { $A shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; $A shell cat /sdcard/u.xml | grep -oE "(text|content-desc)=\"$1[^\"]*\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" | head -1 | grep -oE "\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]" | tr "[]," "   " | awk '{print int(($1+$3)/2), int(($2+$4)/2)}'; }
P=dev.padbridge.dev/dev.padbridge.MainActivity
$A shell am start -n $P >/dev/null; sleep 8
p=$(center "Skip"); echo "skip=$p"; [ -n "$p" ] && { $S tap $p; sleep 3; }
$A shell am force-stop dev.padbridge.dev
$A shell am start -n $P --ez demo true --es dest CONNECT >/dev/null; sleep 8
shot() { sleep 3; $A exec-out screencap -p > $OUT/$1.png; }
if [ "$2" = tablet ]; then
  sleep 2
  for d in CONNECT KEYBOARD TRACKPAD REMOTE; do $A shell am start -n $P --es dest $d >/dev/null 2>&1; shot tablet-$(echo $d | tr A-Z a-z); done
  $A shell am start -n $P --es dest CONNECT >/dev/null 2>&1; sleep 2; p=$(center "Settings"); echo "gear=$p"; [ -n "$p" ] && { $S tap $p; shot tablet-settings; $A shell input keyevent 4; }
else
  for d in CONNECT TRACKPAD KEYBOARD REMOTE; do $A shell am start -n $P --es dest $d >/dev/null 2>&1; shot $(echo $d | tr A-Z a-z); done
  $A shell am start -n $P --es dest CONNECT >/dev/null 2>&1; sleep 2; p=$(center "Settings"); echo "gear=$p"; [ -n "$p" ] && { $S tap $p; shot settings; $A shell input keyevent 4; }
  $A shell am start -n $P --es dest TRACKPAD >/dev/null 2>&1; sleep 1
  $A shell am start -n $P --es dest GAMEPAD >/dev/null 2>&1; sleep 2; $A shell settings put system user_rotation 1; shot gamepad
  $A shell settings put system user_rotation 0
fi
$A logcat -d | grep FATAL -A5 | head
$A emu kill >/dev/null
ls $OUT
