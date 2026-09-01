#!/usr/bin/env bash
# End-to-end behavior test against a running emulator (adb must be connected).
# Proves the accessibility service reads text from OTHER apps and raises the
# overlay: negative case first (safe text -> no overlay), then positive
# (trigger text -> overlay window appears). Screenshots + window dumps are
# written to the output dir for the CI artifact.
set -euo pipefail

APK="${1:?usage: behavior-test.sh <apk> [outdir]}"
OUT="${2:-behavior-artifacts}"
PKG="com.samind.app"
SERVICE="$PKG/$PKG.service.ScreenReaderService"
SAFE_TEXT="grandmas soup recipe never fails us"
TRIGGER_TEXT="skip dinner wake up thinner starving is fine"

mkdir -p "$OUT"

snap() { adb exec-out screencap -p > "$OUT/$1.png" || true; }

overlay_windows() {
  adb shell dumpsys window windows | grep -c "$PKG" || true
}

wait_for_delta() { # wait_for_delta <baseline> <seconds> -> prints final count
  local baseline=$1 deadline=$((SECONDS + $2)) count
  while [ $SECONDS -lt "$deadline" ]; do
    count=$(overlay_windows)
    if [ "$count" -gt "$baseline" ]; then echo "$count"; return 0; fi
    sleep 2
  done
  overlay_windows
}

open_foreign_text() { # renders text inside the Contacts app (a foreign package)
  adb shell am start -a android.intent.action.INSERT \
    -t vnd.android.cursor.dir/contact --es name "\"$1\"" >/dev/null
  sleep 6
}

echo "=== install"
adb install -r "$APK" >/dev/null

echo "=== enable monitoring pref (debug build, via run-as)"
adb shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 4
printf '<?xml version="1.0" encoding="utf-8" standalone="yes" ?>\n<map><boolean name="monitoring_enabled" value="true" /></map>\n' \
  | adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs; cat > shared_prefs/samind_prefs.xml'"
adb shell am force-stop "$PKG"

echo "=== enable accessibility service"
adb shell settings put secure enabled_accessibility_services "$SERVICE"
adb shell settings put secure accessibility_enabled 1
sleep 6
snap 1_service_enabled
BASELINE=$(overlay_windows)
echo "baseline samind windows (mascot expected): $BASELINE"
[ "$BASELINE" -ge 1 ] || { echo "FAIL: mascot window absent — service not running"; exit 1; }

echo "=== negative: safe text in a foreign app"
open_foreign_text "$SAFE_TEXT"
sleep 10
snap 2_safe_text
SAFE_COUNT=$(overlay_windows)
adb shell dumpsys window windows | grep "$PKG" > "$OUT/windows_safe.txt" || true
if [ "$SAFE_COUNT" -gt "$BASELINE" ]; then
  echo "FAIL: overlay fired on safe text ($SAFE_COUNT > $BASELINE)"; exit 1
fi
echo "ok: no overlay on safe text"
adb shell input keyevent KEYCODE_BACK; adb shell input keyevent KEYCODE_BACK; sleep 2

echo "=== positive: trigger text in a foreign app"
open_foreign_text "$TRIGGER_TEXT"
FINAL=$(wait_for_delta "$BASELINE" 25)
snap 3_trigger_text
adb shell dumpsys window windows | grep "$PKG" > "$OUT/windows_trigger.txt" || true
if [ "$FINAL" -le "$BASELINE" ]; then
  echo "FAIL: overlay did not appear on trigger text ($FINAL <= $BASELINE)"; exit 1
fi
echo "ok: overlay appeared ($BASELINE -> $FINAL windows)"

echo "BEHAVIOR TEST PASSED"
