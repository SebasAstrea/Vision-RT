#!/usr/bin/env bash
set -euo pipefail

# M7 battery + thermal report (ROADMAP deliverables 4–5 / OR-006–007).
# Samples battery %, charger state, and thermal status while the app runs;
# optionally injects a thermal override to exercise the degradation ladder.
#
# Usage:
#   tools/scripts/battery_thermal_report.sh [sample_count] [interval_sec]
# Env:
#   PKG=com.visionrt.app  ADB=adb  FORCE_THERMAL=0|1
#   FORCE_THERMAL=1 runs `cmd thermalservice override-status 3` (SEVERE) then
#   clears with `override-status 1` (NONE) at the end — M6/M7 stress evidence.

PKG="${PKG:-com.visionrt.app}"
ADB="${ADB:-adb}"
SAMPLES="${1:-12}"
INTERVAL_SEC="${2:-10}"
FORCE_THERMAL="${FORCE_THERMAL:-0}"
OUT_DIR="${OUT_DIR:-tools/reports}"
CSV="$OUT_DIR/battery_thermal_$(date +%Y%m%d_%H%M%S).csv"

mkdir -p "$OUT_DIR"

if ! "$ADB" get-state >/dev/null 2>&1; then
  echo "ERROR: no adb device." >&2
  exit 2
fi

if ! "$ADB" shell pidof "$PKG" >/dev/null 2>&1; then
  "$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
  sleep 3
fi

if [[ "$FORCE_THERMAL" == "1" ]]; then
  echo "Forcing thermal SEVERE (override-status 3) for degradation evidence..."
  "$ADB" shell cmd thermalservice override-status 3 >/dev/null || true
  sleep 2
fi

echo "epoch_ms,battery_pct,status,thermal_status,power_save" > "$CSV"
for i in $(seq 1 "$SAMPLES"); do
  BAT=$("$ADB" shell dumpsys battery | tr -d '\r')
  LEVEL=$(printf '%s\n' "$BAT" | awk -F': ' '/level:/ {print $2; exit}')
  STATUS=$(printf '%s\n' "$BAT" | awk -F': ' '/status:/ {print $2; exit}')
  THERMAL=$("$ADB" shell dumpsys thermalservice | tr -d '\r' | grep -m1 'Current temperatures from thermal service' -A20 | grep -m1 'mStatus' || true)
  # Prefer mThermalStatus when present (Android 10+)
  TSTAT=$("$ADB" shell dumpsys thermalservice | tr -d '\r' | grep -E 'mThermalStatus|Thermal Status:' | head -1 | awk -F'[=:]' '{print $NF}' | tr -d ' ')
  PSAVE=$("$ADB" shell settings get global low_power 2>/dev/null | tr -d '\r' || echo 0)
  echo "$(date +%s%3N),${LEVEL:-?},${STATUS:-?},${TSTAT:-?},${PSAVE:-0}" >> "$CSV"
  echo "sample $i/$SAMPLES battery=${LEVEL:-?}% thermal=${TSTAT:-?} saver=${PSAVE:-0}"
  sleep "$INTERVAL_SEC"
done

if [[ "$FORCE_THERMAL" == "1" ]]; then
  echo "Clearing thermal override (override-status 1)..."
  "$ADB" shell cmd thermalservice override-status 1 >/dev/null || true
fi

# Crash check for the window
CRASH=$("$ADB" logcat -d -b crash -t 400 2>/dev/null | grep -cE 'FATAL EXCEPTION|AndroidRuntime' || true)
echo "crash_lines_since_start=$CRASH"
echo "csv=$CSV"
if [[ "${CRASH:-0}" -gt 0 ]]; then
  # Only fail if VisionRT package appears in those lines
  if "$ADB" logcat -d -b crash -t 400 2>/dev/null | grep -q "$PKG"; then
    echo "FAIL: crash associated with $PKG" >&2
    exit 1
  fi
fi
echo "battery/thermal report: done"
