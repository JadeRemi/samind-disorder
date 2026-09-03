#!/usr/bin/env bash
# End-to-end behavior test against a running emulator (adb must be connected).
# Proves the accessibility service reads text from OTHER apps and raises the
# overlay: negative case first (safe text -> no overlay), then positive
# (trigger text -> overlay window appears). Screenshots + window dumps are
# written to the output dir for the CI artifact.
set -euo pipefail

# takes a directory (or the repo root) and locates the APKs itself — passing
# multiple paths through CI yaml proved fragile
APK_DIR="${1:-.}"
OUT="${2:-behavior-artifacts}"
PKG="com.samind.app"
FEED_PKG="com.samind.testfeed"

APK=$(find "$APK_DIR" -name "app-debug.apk" -type f | head -1)
FEED_APK=$(find "$APK_DIR" -name "testfeed-debug.apk" -type f | head -1)
[ -n "$APK" ] || { echo "FAIL: app-debug.apk not found under $APK_DIR"; exit 1; }
[ -n "$FEED_APK" ] || { echo "FAIL: testfeed-debug.apk not found under $APK_DIR"; exit 1; }
echo "app apk:  $APK"
echo "feed apk: $FEED_APK"
SERVICE="$PKG/$PKG.service.ScreenReaderService"
# scores verified against the shipped baseline model: safe 0.10, trigger 0.98
# (the trigger stays above threshold even mixed with the host app's UI text)
SAFE_TEXT="grandmas soup recipe never fails us"
TRIGGER_TEXT="thinspo meanspo pro ana starving purge"

mkdir -p "$OUT"
cat > "$OUT/EVIDENCE.md" <<'HEADER'
# Samind — captured UI evidence

Screenshots taken by `android/scripts/behavior-test.sh` during the automated
end-to-end run on an emulator, in order.

HEADER

# on ANY exit, print what the device actually said — never fail blind
collect_logs() {
  adb logcat -d > "$OUT/logcat_full.txt" 2>/dev/null || true
  echo "===== classifier decisions (what the model actually scored) ====="
  grep -E "ScreenReaderService|OverlayController" "$OUT/logcat_full.txt" | tail -25 \
    || echo "(none — service never classified anything)"
  echo "===== app / accessibility log ====="
  grep -iE "samind|AccessibilityManagerService|AndroidRuntime" "$OUT/logcat_full.txt" \
    | tail -25 || echo "(no matching lines)"
  if grep -q "FATAL EXCEPTION" "$OUT/logcat_full.txt"; then
    echo "===== CRASH ====="
    grep -A 30 "FATAL EXCEPTION" "$OUT/logcat_full.txt" | head -60
  fi
  echo "===== accessibility state ====="
  adb shell settings get secure enabled_accessibility_services || true
  adb shell settings get secure accessibility_enabled || true
  adb shell dumpsys accessibility 2>/dev/null | grep -iA3 samind | head -20 || true
  echo "===== monitoring pref on device ====="
  adb shell "run-as $PKG cat shared_prefs/samind_prefs.xml" 2>/dev/null || echo "(pref file unreadable)"
}
trap collect_logs EXIT

SHOT=0
# snap <slug> <caption> — numbered screenshot + a line in the evidence index
snap() {
  SHOT=$((SHOT + 1))
  local name
  name=$(printf "%02d_%s" "$SHOT" "$1")
  adb exec-out screencap -p > "$OUT/$name.png" || true
  echo "- \`$name.png\` — ${2:-$1}" >> "$OUT/EVIDENCE.md"
}

open_app_screen() { # open_app_screen <home|chat|ground|stats>
  # force-stop first: a running activity would ignore the destination extra
  # (belt and braces — the app also handles onNewIntent now)
  adb shell am force-stop "$PKG" >/dev/null 2>&1 || true
  adb shell am start -n "$PKG/.MainActivity" --es destination "$1" >/dev/null
  sleep 5
}

# count real windows only — "Window{...}" records, excluding the system crash
# dialog, which also carries our package name and would fake a passing count
overlay_windows() {
  adb shell dumpsys window windows \
    | grep -E "Window\{" | grep "$PKG" | grep -vc "Application Error" || true
}

# a crash means the feature is broken; stop immediately instead of measuring windows
assert_no_crash() {
  if adb logcat -d | grep -q "FATAL EXCEPTION"; then
    echo "FAIL: the app crashed ($1)"
    exit 1
  fi
}

service_bound() {
  adb shell dumpsys accessibility 2>/dev/null | grep -q "ScreenReaderService"
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

# renders the given text inside the test-feed app: a separate package, so this
# exercises real cross-app reading, but with deterministic content (stock apps
# ignore intent extras and typing needs a focused field — both proved unreliable)
open_foreign_text() {
  adb shell am force-stop "$FEED_PKG" >/dev/null 2>&1 || true
  adb shell am start -n "$FEED_PKG/.FeedActivity" --es text "$1" >/dev/null
  sleep 5
  # the feed app logs what it rendered; never use `uiautomator dump` here —
  # it seizes the accessibility subsystem and tears down the overlays under test
  if adb logcat -d -s SamindTestFeed | grep -q "showing: ${1%% *}"; then
    echo "confirmed: text is on screen in $FEED_PKG"
  else
    echo "FAIL: the text did not render in the foreign app — harness problem"
    exit 1
  fi
}

echo "=== install"
adb install -r "$APK" >/dev/null
adb install -r "$FEED_APK" >/dev/null
# a package install/replace wipes accessibility grants; let the package manager
# finish its broadcasts before touching those settings, or they get cleared again
echo "waiting for package state to settle"
sleep 20

echo "=== capture: app before monitoring is enabled"
open_app_screen home
snap home_monitoring_off "Home screen, monitoring off (initial state)"

echo "=== enable monitoring pref (debug build, via run-as)"
adb shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 4
printf '<?xml version="1.0" encoding="utf-8" standalone="yes" ?>\n<map><boolean name="monitoring_enabled" value="true" /></map>\n' \
  | adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs; cat > shared_prefs/samind_prefs.xml'"
adb shell am force-stop "$PKG"
adb shell "run-as $PKG cat shared_prefs/samind_prefs.xml" | grep -q 'value="true"' \
  || { echo "FAIL: monitoring pref not written"; exit 1; }
echo "pref written ok"

echo "=== enable accessibility service"
adb logcat -c || true

read_state() { adb shell settings get secure enabled_accessibility_services | tr -d '\r'; }

# retry until the setting STAYS on: the system clears it on package changes and
# blocks it entirely for sideloaded apps without the restricted-settings op
STUCK=0
for attempt in 1 2 3 4 5; do
  adb shell appops set "$PKG" ACCESS_RESTRICTED_SETTINGS allow >/dev/null 2>&1 || true
  adb shell settings put secure enabled_accessibility_services "$SERVICE"
  adb shell settings put secure accessibility_enabled 1
  sleep 8
  STATE=$(read_state)
  echo "attempt $attempt: enabled_accessibility_services = $STATE"
  case "$STATE" in
    *ScreenReaderService*)
      sleep 6
      case "$(read_state)" in
        *ScreenReaderService*) STUCK=1 ;;
        *) echo "  setting was cleared again, retrying" ;;
      esac
      ;;
  esac
  [ "$STUCK" -eq 1 ] && break
done

[ "$STUCK" -eq 1 ] || { echo "FAIL: accessibility setting will not persist"; exit 1; }
echo "accessibility service enabled and persistent"

echo "=== wait for the service to bind and show the mascot"
BASELINE=0
deadline=$((SECONDS + 60))
while [ $SECONDS -lt "$deadline" ]; do
  BASELINE=$(overlay_windows)
  [ "$BASELINE" -ge 1 ] && break
  sleep 3
done

snap mascot_overlay "Floating mascot over another app (monitoring active)"
assert_no_crash "during service startup"
service_bound && echo "service is bound" || echo "warning: service not listed in dumpsys accessibility"
echo "baseline samind windows (mascot expected >= 1): $BASELINE"
[ "$BASELINE" -ge 1 ] || { echo "FAIL: mascot window absent — service not running"; exit 1; }

echo "=== negative: safe text in a foreign app"
open_foreign_text "$SAFE_TEXT" safe
sleep 10
snap safe_text_no_overlay "Safe text in another app — no intervention (correct)"
SAFE_COUNT=$(overlay_windows)
adb shell dumpsys window windows | grep "$PKG" > "$OUT/windows_safe.txt" || true
if [ "$SAFE_COUNT" -gt "$BASELINE" ]; then
  echo "FAIL: overlay fired on safe text ($SAFE_COUNT > $BASELINE)"; exit 1
fi
echo "ok: no overlay on safe text"
adb shell input keyevent KEYCODE_BACK; adb shell input keyevent KEYCODE_BACK; sleep 2

echo "=== positive: trigger text in a foreign app"
open_foreign_text "$TRIGGER_TEXT" trigger
FINAL=$(wait_for_delta "$BASELINE" 30)
snap trigger_intervention "TRIGGER DETECTED in another app — dimmed screen + distraction question"
assert_no_crash "while handling the trigger"
adb shell dumpsys window windows | grep "$PKG" > "$OUT/windows_trigger.txt" || true

# three independent signals, all required: the model flagged it, the overlay
# window was created, and it is present on screen
adb logcat -d -s ScreenReaderService | grep "risky=true" | grep -q "$FEED_PKG" \
  || { echo "FAIL: the classifier did not flag the trigger text"; exit 1; }
echo "ok: classifier flagged the trigger"

adb logcat -d -s OverlayController | grep -q "question overlay shown" \
  || { echo "FAIL: overlay was never shown"; exit 1; }
echo "ok: overlay was shown"

if [ "$FINAL" -le "$BASELINE" ]; then
  echo "FAIL: overlay window not present on screen ($FINAL <= $BASELINE)"; exit 1
fi
echo "ok: overlay window on screen ($BASELINE -> $FINAL windows)"

echo "=== capture: every in-app screen (English, then Russian)"
for lang in en ru; do
  adb shell "setprop persist.sys.locale $lang-US" >/dev/null 2>&1 || true
  for screen in home chat ground stats; do
    open_app_screen "$screen"
    snap "${screen}_$lang" "$screen screen ($lang)"
  done
done

echo "=== capture: grounding exercise in progress"
open_app_screen ground
# the first technique's Start button sits in the upper card; tap by proportion
SIZE=$(adb shell wm size | tr -d '\r' | awk -F': ' '{print $2}')
W=${SIZE%x*}; H=${SIZE#*x}
adb shell input tap $((W / 2)) $((H * 30 / 100)) >/dev/null 2>&1 || true
sleep 3
snap grounding_step "Grounding technique running (step-by-step)"

echo "BEHAVIOR TEST PASSED"
echo "evidence: $(ls "$OUT"/*.png 2>/dev/null | wc -l | tr -d ' ') screenshots + EVIDENCE.md"
