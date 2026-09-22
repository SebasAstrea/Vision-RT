#!/usr/bin/env bash
set -euo pipefail

# M1 accessibility scan (UX-002.5): runs the Espresso AccessibilityChecks suite
# against the app shell. The scanner is wired into every screen interaction in
# app/src/androidTest (UiTestHelpers.enableAccessibilityChecks), so a critical
# defect (contentDescription, touch-target sizes, TalkBack traversal) fails the
# build. Requires an attached device or emulator.

# On KVM-less CI runners the emulator can flip sys.boot_completed before the
# system services (package/settings) are ready; installing APKs then fails with
# "Can't find service: package". Wait until they are actually usable.
for i in $(seq 1 300); do
  if adb shell getprop sys.boot_completed 2>/dev/null | grep -q "1" &&
     adb shell cmd package list packages -p com.android.shell >/dev/null 2>&1; then
    break
  fi
  [ "$i" -eq 300 ] && { echo "Emulator did not become ready" >&2; exit 1; }
  sleep 2
done
# Settle a bit more once the package service answers.
sleep 10

if ./gradlew :app:connectedDebugAndroidTest --stacktrace; then
  echo "Accessibility scan: PASS"
else
  echo "Accessibility scan: FAILED - instrumented results:"
  python3 - <<'EOF'
import glob, re
for p in glob.glob('app/build/outputs/androidTest-results/connected/**/*.xml', recursive=True):
    t = open(p, encoding='utf-8', errors='ignore').read()
    for tc in re.finditer(r'<testcase name="([^"]+)"[^>]*>(.*?)</testcase>', t, re.S):
        if '<failure' in tc.group(2) or '<error' in tc.group(2):
            print('FAIL', tc.group(1))
            print(re.sub(r'<[^>]+>', '', tc.group(2)).strip()[:1500])
            print('====')
EOF
  exit 1
fi