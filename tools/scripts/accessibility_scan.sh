#!/usr/bin/env bash
set -euo pipefail

# M1 accessibility scan (UX-002.5): runs the Espresso AccessibilityChecks suite
# against the app shell. The scanner is wired into every screen interaction in
# app/src/androidTest (UiTestHelpers.enableAccessibilityChecks), so a critical
# defect (contentDescription, touch-target sizes, TalkBack traversal) fails the
# build. Requires an attached device or emulator.
./gradlew connectedDebugAndroidTest --stacktrace