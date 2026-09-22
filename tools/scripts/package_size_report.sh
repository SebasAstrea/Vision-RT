#!/usr/bin/env bash
set -euo pipefail

APK="${APK_PATH:-app/build/outputs/apk/debug/app-debug.apk}"
MAX_MB="${MAX_APK_MB:-150}"

if [[ ! -f "$APK" ]]; then
  echo "WARNING: $APK not found. Run ./gradlew assembleDebug first." >&2
  exit 0
fi

BYTES=$(stat -c%s "$APK")
MB=$(awk -v b="$BYTES" 'BEGIN { printf "%.2f", b/1024/1024 }')
echo "debug APK size: ${MB} MB ($BYTES bytes)"

if (( BYTES > MAX_MB * 1024 * 1024 )); then
  echo "ERROR: APK exceeds the $MAX_MB MB base install budget (ARCHITECTURE 25.x)." >&2
  exit 1
fi