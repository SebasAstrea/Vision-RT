#!/usr/bin/env bash
set -euo pipefail

# M1 accessibility scan (UX-002.5): runs the Espresso AccessibilityChecks suite
# against the app shell. The scanner is wired into every screen interaction in
# app/src/androidTest (UiTestHelpers.enableAccessibilityChecks), so a critical
# defect (contentDescription, touch-target sizes, TalkBack traversal) fails the
# build. Requires an attached device or emulator (e.g. adb-connected phone;
# not part of GitHub Actions CI).
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