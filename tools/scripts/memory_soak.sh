#!/usr/bin/env bash
set -euo pipefail

# M7 memory soak (ROADMAP deliverables 3 / OR-003): samples dumpsys meminfo
# for the app process over DURATION_MIN minutes and checks peak PSS ≤ 800 MB
# and growth ≤ 10% (REQUIREMENTS OR-003.1 / OR-003.3).
#
# Usage:
#   tools/scripts/memory_soak.sh [duration_min]
# Env:
#   PKG=com.visionrt.app  ADB=adb  INTERVAL_SEC=15
# Requires a connected device (adb). Non-interactive; safe to run from CI
# only when a device is attached.

PKG="${PKG:-com.visionrt.app}"
ADB="${ADB:-adb}"
INTERVAL_SEC="${INTERVAL_SEC:-15}"
DURATION_MIN="${1:-${SOAK_MIN:-5}}"
PEAK_BUDGET_MB="${PEAK_BUDGET_MB:-800}"
GROWTH_BUDGET_PCT="${GROWTH_BUDGET_PCT:-10}"
OUT_DIR="${OUT_DIR:-tools/reports}"
CSV="$OUT_DIR/memory_soak_$(date +%Y%m%d_%H%M%S).csv"

mkdir -p "$OUT_DIR"

if ! "$ADB" get-state >/dev/null 2>&1; then
  echo "ERROR: no adb device. Connect the reference phone first." >&2
  exit 2
fi

if ! "$ADB" shell pidof "$PKG" >/dev/null 2>&1; then
  echo "Starting $PKG ..."
  "$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
  sleep 3
fi

PID=$("$ADB" shell pidof "$PKG" | tr -d '\r' | awk '{print $1}')
if [[ -z "${PID:-}" ]]; then
  echo "ERROR: could not resolve pid for $PKG" >&2
  exit 2
fi

echo "soak pid=$PID duration=${DURATION_MIN}min interval=${INTERVAL_SEC}s -> $CSV"
echo "epoch_ms,pss_total_mb,dalvik_heap_mb,native_heap_mb" > "$CSV"

END=$(( $(date +%s) + DURATION_MIN * 60 ))
BASELINE_MB=""
PEAK_MB=0
LAST_MB=0

while (( $(date +%s) < END )); do
  LINE=$("$ADB" shell dumpsys meminfo "$PID" 2>/dev/null | tr -d '\r' || true)
  TOTAL_KB=$(printf '%s\n' "$LINE" | awk '/TOTAL PSS:/ {print $3; exit}')
  # Fallback older format: "TOTAL 12345"
  if [[ -z "${TOTAL_KB:-}" ]]; then
    TOTAL_KB=$(printf '%s\n' "$LINE" | awk '/^\s*TOTAL\s/ {print $2; exit}')
  fi
  DALVIK_KB=$(printf '%s\n' "$LINE" | awk '/Dalvik Heap/ {print $3; exit}')
  NATIVE_KB=$(printf '%s\n' "$LINE" | awk '/Native Heap/ {print $3; exit}')
  MB=$(awk -v k="${TOTAL_KB:-0}" 'BEGIN { printf "%.1f", k/1024 }')
  DH=$(awk -v k="${DALVIK_KB:-0}" 'BEGIN { printf "%.1f", k/1024 }')
  NH=$(awk -v k="${NATIVE_KB:-0}" 'BEGIN { printf "%.1f", k/1024 }')
  echo "$(date +%s%3N),$MB,$DH,$NH" >> "$CSV"
  awk -v p="$PEAK_MB" -v c="$MB" 'BEGIN { exit !(c > p) }' && PEAK_MB=$MB
  [[ -z "$BASELINE_MB" && "$MB" != "0.0" ]] && BASELINE_MB=$MB
  LAST_MB=$MB
  sleep "$INTERVAL_SEC"
done

# Final sample after loop for steady-state last reading
LINE=$("$ADB" shell dumpsys meminfo "$PID" 2>/dev/null | tr -d '\r' || true)
TOTAL_KB=$(printf '%s\n' "$LINE" | awk '/TOTAL PSS:/ {print $3; exit}')
MB=$(awk -v k="${TOTAL_KB:-0}" 'BEGIN { printf "%.1f", k/1024 }')
echo "$(date +%s%3N),$MB,," >> "$CSV"
awk -v p="$PEAK_MB" -v c="$MB" 'BEGIN { exit !(c > p) }' && PEAK_MB=$MB
LAST_MB=$MB

GROWTH=$(awk -v b="${BASELINE_MB:-0}" -v p="$PEAK_MB" 'BEGIN {
  if (b <= 0) { print "0.0"; exit }
  printf "%.1f", ((p - b) / b) * 100
}')
OOM=$("$ADB" logcat -d -b crash -t 200 2>/dev/null | grep -c 'OutOfMemoryError' || true)
OOM="${OOM:-0}"

# OR-003 growth is defined over a 30-minute continuous session. Short
# diagnostic windows (default 5 min) only enforce the peak budget.
ENFORCE_GROWTH=0
if awk -v m="$DURATION_MIN" 'BEGIN { exit !(m >= 30) }'; then
  ENFORCE_GROWTH=1
fi

echo "baseline_mb=${BASELINE_MB:-n/a} peak_mb=$PEAK_MB growth_pct=$GROWTH oom_events=${OOM} last_mb=$LAST_MB enforce_growth=$ENFORCE_GROWTH"
echo "csv=$CSV"

STATUS=0
if awk -v p="$PEAK_MB" -v b="$PEAK_BUDGET_MB" 'BEGIN { exit !(p > b) }'; then
  echo "FAIL: peak PSS ${PEAK_MB} MB > ${PEAK_BUDGET_MB} MB (OR-003.1)" >&2
  STATUS=1
fi
if [[ "$ENFORCE_GROWTH" -eq 1 ]] && awk -v g="$GROWTH" -v b="$GROWTH_BUDGET_PCT" 'BEGIN { exit !(g > b) }'; then
  echo "FAIL: growth ${GROWTH}% > ${GROWTH_BUDGET_PCT}% (OR-003.3)" >&2
  STATUS=1
fi
if [[ "$OOM" -gt 0 ]]; then
  echo "FAIL: OutOfMemoryError observed in crash log (OR-003.2)" >&2
  STATUS=1
fi
if [[ $STATUS -eq 0 ]]; then
  if [[ "$ENFORCE_GROWTH" -eq 1 ]]; then
    echo "memory soak: PASS (peak ${PEAK_MB} MB, growth ${GROWTH}%)"
  else
    echo "memory soak: PEAK PASS (peak ${PEAK_MB} MB); growth ${GROWTH}% informational (run 30 min for OR-003.3)"
  fi
fi
exit $STATUS
