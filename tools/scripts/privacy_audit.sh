#!/usr/bin/env bash
set -euo pipefail

# M7 privacy audit (ROADMAP deliverable 6): static checks that core features
# stay offline and that logs never carry images/OCR text/PII. Read-only.
# Exit 0 = pass; non-zero = findings that block the M7 privacy gate.

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
FAIL=0

note() { printf '%s\n' "$*"; }
fail() { printf 'FAIL: %s\n' "$*"; FAIL=1; }
pass() { printf 'PASS: %s\n' "$*"; }

note "=== VisionRT privacy audit ==="

# --- Permissions (allowed: CAMERA, VIBRATE only for MVP) ---
allowed='android.permission.CAMERA|android.permission.VIBRATE'
manifests=(
  app/src/main/AndroidManifest.xml
  app/src/debug/AndroidManifest.xml
  feedback/src/main/AndroidManifest.xml
)
for m in "${manifests[@]}"; do
  [[ -f "$m" ]] || continue
  while IFS= read -r perm; do
    [[ -z "$perm" ]] && continue
    if ! grep -Eq "$allowed" <<<"$perm"; then
      fail "unexpected permission in $m: $perm"
    fi
  done < <(grep -oE 'android\.permission\.[A-Z_]+' "$m" || true)
done
pass "manifest permissions limited to CAMERA + VIBRATE"

# --- No INTERNET / network / location permissions ---
if grep -R --include='*.xml' -nE 'android\.permission\.(INTERNET|ACCESS_NETWORK_STATE|ACCESS_FINE_LOCATION|ACCESS_COARSE_LOCATION|RECORD_AUDIO|READ_MEDIA_|READ_CONTACTS)' \
  app feedback feature core data perception inference benchmark 2>/dev/null | grep -v 'build/' ; then
  fail "forbidden network/location/media permission found"
else
  pass "no INTERNET / location / mic / media permissions"
fi

# --- No network client libraries in Kotlin sources ---
if grep -R --include='*.kt' -nE '\b(okhttp3|retrofit2|Volley|HttpClient|HttpURLConnection|URLConnection|WebSocket)\b' \
  app feature core data perception inference feedback benchmark 2>/dev/null | grep -v 'build/' ; then
  fail "network API reference in Kotlin sources"
else
  pass "no network client APIs in Kotlin sources"
fi

# --- SafeLog rejects raw payloads (compiler + static contract) ---
if grep -R --include='*.kt' -n 'fun logRaw' core/src/main | grep -q 'Nothing'; then
  pass "SafeLogger.logRaw returns Nothing (rejects raw media)"
else
  fail "SafeLogger.logRaw contract missing"
fi

# --- Forbidden log patterns: raw frames / OCR text / PII-ish keys ---
forbidden_log='(Log\.(d|i|w|e)\([^)]*(ocrText|recognizedText|frameBytes|rgb888|cameraFrame|bitmap\.|base64)|printStackTrace\([^)]*ocr)'
if grep -R --include='*.kt' -nE "$forbidden_log" \
  app feature core data perception inference feedback benchmark 2>/dev/null | grep -v 'build/' ; then
  fail "forbidden payload logging pattern found"
else
  pass "no raw image / OCR text log patterns"
fi

# --- Direct android.util.Log outside SafeLogger/SafeLog ---
bad_log=$(grep -R --include='*.kt' -nE '(^|[^.])Log\.[diwev]\(' \
  app feature core data perception inference feedback benchmark \
  2>/dev/null | grep -v 'build/' | grep -v 'SafeLog' | grep -v 'package com.visionrt' || true)
# Allow only SafeLogger.kt / SafeLog.kt wrappers if any Log call remains outside them
if [[ -n "$bad_log" ]]; then
  # Filter to files that are not the SafeLog* wrappers
  filtered=$(printf '%s\n' "$bad_log" | grep -vE 'SafeLog(er)?\.kt:' || true)
  if [[ -n "$filtered" ]]; then
    printf '%s\n' "$filtered"
    fail "android.util.Log used outside SafeLogger/SafeLog"
  else
    pass "android.util.Log only in SafeLogger wrappers"
  fi
else
  pass "android.util.Log only in SafeLogger wrappers"
fi

# --- Core features must not declare INTERNET in gradle deps ---
if grep -R --include='*.gradle.kts' -nE '\b(okhttp|retrofit|volley|kotlinx-coroutines-http)\b' \
  --exclude-dir=build . 2>/dev/null; then
  fail "network dependency declared in Gradle"
else
  pass "no network dependencies in Gradle files"
fi

note "=== privacy audit result: $([[ $FAIL -eq 0 ]] && echo PASS || echo FAIL) ==="
exit "$FAIL"
