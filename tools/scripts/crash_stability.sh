#!/usr/bin/env bash
set -euo pipefail

# M7 crash stability (ROADMAP deliverable 9 / REQUIREMENTS Reliability).
# Clears logcat, optionally relaunches the app, waits, then greps for fatal
# crashes. Writes a summary under tools/reports/.
#
# Usage:
#   tools/scripts/crash_stability.sh [watch_sec] [relaunch=1|0]

PKG="${PKG:-com.visionrt.app}"
ADB="${ADB:-adb}"
WATCH_SEC="${1:-120}"
RELAUNCH="${2:-1}"
OUT_DIR="${OUT_DIR:-tools/reports}"
SUMMARY="$OUT_DIR/crash_stability_$(date +%Y%m%d_%H%M%S).log"

mkdir -p "$OUT_DIR"

if ! "$ADB" get-state >/dev/null 2>&1; then
  echo "ERROR: no adb device." >&2
  exit 2
fi

"$ADB" logcat -c || true

if [[ "$RELAUNCH" == "1" ]]; then
  "$ADB" shell am force-stop "$PKG" || true
  sleep 1
  "$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null || true
fi

echo "Watching $PKG for ${WATCH_SEC}s ..."
sleep "$WATCH_SEC"

CRASH_DUMP="$OUT_DIR/crash_logcat_$(date +%s).txt"
"$ADB" logcat -d -b crash > "$CRASH_DUMP" 2>/dev/null || true
MAIN_DUMP="$OUT_DIR/main_logcat_$(date +%s).txt"
"$ADB" logcat -d > "$MAIN_DUMP" 2>/dev/null || true

FATAL=$(grep -c 'FATAL EXCEPTION' "$CRASH_DUMP" 2>/dev/null || true)
FATAL="${FATAL:-0}"
PKG_FATAL=$(grep -E 'FATAL EXCEPTION|AndroidRuntime' "$CRASH_DUMP" 2>/dev/null | grep -c "$PKG" || true)
PKG_FATAL="${PKG_FATAL:-0}"
ANR=$(grep -c 'ANR in' "$MAIN_DUMP" 2>/dev/null || true)
ANR="${ANR:-0}"
OOM=$( { grep -c 'OutOfMemoryError' "$CRASH_DUMP" 2>/dev/null || true; grep -c 'OutOfMemoryError' "$MAIN_DUMP" 2>/dev/null || true; } | awk '{s+=$1} END {print s+0}' )
OOM="${OOM:-0}"

STATUS=0
{
  echo "=== crash stability report ==="
  echo "package=$PKG watch_sec=$WATCH_SEC"
  echo "fatal_exceptions=$FATAL package_fatal=$PKG_FATAL anr=$ANR oom=$OOM"
  echo "crash_dump=$CRASH_DUMP"
  echo "main_dump=$MAIN_DUMP"
  if [[ "${PKG_FATAL:-0}" -gt 0 ]]; then
    echo "result=FAIL (package crash)"
    STATUS=1
  elif [[ "${OOM:-0}" -gt 0 ]]; then
    echo "result=FAIL (OutOfMemoryError)"
    STATUS=1
  else
    echo "result=PASS"
  fi
} | tee "$SUMMARY"

# FATAL count includes other packages; only package crashes fail above.
# Still surface global fatals as info when > 0 but package clean.
if [[ "${FATAL:-0}" -gt 0 && "${PKG_FATAL:-0}" -eq 0 ]]; then
  echo "note: unrelated fatal exceptions present; VisionRT package clean."
fi

exit $STATUS
