# M7 Reports — Benchmark, Model Validation, and Privacy Audit

> ROADMAP M7 deliverables 1–10. Evidence collected on **2026-09-24** unless noted.
> Source of truth for status remains `docs/ROADMAP.md` (M7 section annotated).

## 1. Device matrix benchmark report

| Device | SoC | RAM | SDK | In hand | Detector P95 | OCR P95 | Notes |
|---|---|---:|---:|---|---:|---:|---|
| Samsung SM-A226BR (Galaxy A22 5G) | Dimensity 700 (MT6833) | 4 GB | 33 | Yes (QA #1) | **62 ms** | **132 ms** | Registry: `DeviceRegistry.KNOWN` |

**How to reproduce**

1. Install demo APK (`./gradlew :app:installDemo`).
2. Open Settings → **Run detector benchmark** / **Run OCR benchmark**.
3. Open Settings → **Export diagnostics report** (full payload-free dump in logcat).

```bash
adb logcat -d -s VisionRT | grep -E 'subject=|=== VisionRT diagnostics'
```

**Evidence (SM-A226BR, 2026-09-24, versionCode=7 / 0.7.0-M7):**

- Detector: **P50=53 ms, P95=62 ms, P99=77 ms** (n=50) — within 450 ms budget (`pass=true`).
- OCR: **P50=84 ms, P95=132 ms** (n=10) — within 8000 ms budget (`pass=true`).
- Diagnostics export: UI *“Diagnóstico exportado. Perfil LOW_END, nivel NORMAL…”*;
  logcat `=== VisionRT diagnostics v1 ===` with `memPass=true`.
- Device: SM-A226BR, thermal=0, battery≈24%, no FATAL.

**Release blocker:** exit criteria require thresholds on **≥ 3 low-end devices**.
Only SM-A226BR is available (`docs/DEVICE_PROCUREMENT.md`). Devices #2–#3 must be
procured before the M7 performance exit can be marked fully met.

## 2. Model validation report

**Tool:** `tools/dataset_validation/evaluate_detections.py`  
**Schema:** `tools/dataset_validation/schema/annotation_schema.json`  
**Fixture smoke:** annotations + detections fixtures under `fixtures/` (TP/FP path).

```bash
python3 tools/dataset_validation/evaluate_detections.py \
  --annotations tools/dataset_validation/fixtures/annotations_fixture.json \
  --detections tools/dataset_validation/fixtures/detections_fixture.json \
  --enforce
```

| Metric | Target (ROADMAP M7) | Status |
|---|---|---|
| Priority object recall | ≥ 0.85 | **Pending real dataset** (DAT-001) |
| Priority object precision | ≥ 0.75 | **Pending real dataset** (DAT-001) |
| Direction accuracy | ≥ 0.90 | Pending labeled routes |
| Proximity accuracy | ≥ 0.80 | Pending labeled routes |
| False alerts ≤ 2/10 min (low clutter) | ≤ 2 | Pending field protocol |
| False alerts ≤ 5/10 min (high clutter) | ≤ 5 | Pending field protocol |

**Static INT8 / QAT:** `tools/model_conversion/README.md` documents that full-integer
static quantization collapses class scores on this graph; **static INT8 or QAT
requires human review before any conversion run** (AGENTS stop condition).
Runtime asset remains dynamic weight-only INT8 (`dynamic_wi8_afp32`).

**Release blocker:** model accuracy targets not closed until a licensed dataset
(DAT-001) is annotated and evaluations are attached with `--enforce`.

## 3. Memory soak report

**Tool:** `tools/scripts/memory_soak.sh [minutes]` (adb; default 5 min, use 30 for full OR-003).

```bash
# Full OR-003 continuous session (30 min):
./tools/scripts/memory_soak.sh 30
```

Budgets: peak PSS ≤ 800 MB (OR-003.1), growth ≤ 10% (OR-003.3), zero OOM (OR-003.2).

**Evidence (SM-A226BR, 2026-09-24 short window):**

- CSV: `tools/reports/memory_soak_20260924_115916.csv` (and `...115603.csv` with mid-run peak)
- **peak ≤ 134.7 MB** (≤ 800 MB OR-003.1), OOM=0; latest short window peak 116.2 MB.
- Growth % is **informational** until a full **30-minute** run (`./tools/scripts/memory_soak.sh 30`)
  because OR-003.3 defines growth over continuous 30-min use.

**Full 30-minute soak:** *still required before MVP 1.0 release checklist.*

## 4. Battery usage report

**Tool:** `tools/scripts/battery_thermal_report.sh [samples] [interval_sec]`

```bash
./tools/scripts/battery_thermal_report.sh 18 20   # ~6 min sample window
```

Records battery %, charger status, thermal status, power-save flag to
`tools/reports/battery_thermal_*.csv`.

**Evidence (SM-A226BR, 2026-09-24 short window):**

- CSV: `tools/reports/battery_thermal_20260924_115806.csv`
- Battery 24% stable, thermal status **0 (NONE)** over 6 samples, power-save 0, no package crashes.
- Longer discharge profile under continuous assistance: *pending longer run.*

## 5. Thermal behavior report

**Tool:** same as battery (`FORCE_THERMAL=1` injects SEVERE, then clears).

```bash
FORCE_THERMAL=1 ./tools/scripts/battery_thermal_report.sh 6 5
# Manual TalkBack check (M6): override to SEVERE, start assistance, expect
# critical degradation announcement when applicable:
adb shell cmd thermalservice override-status 3
# clear:
adb shell cmd thermalservice override-status 1
```

Expected (M6 unit-tested): SEVERE → CRITICAL (continuous detect stop);
MODERATE → REDUCED (half FPS); recovery restores NORMAL with announcement.

**Evidence (2026-09-24):**

- Idle/app background thermal = **0** in `battery_thermal_*.csv`.
- M6 governor unit tests cover SEVERE→CRITICAL / MODERATE→REDUCED / recovery.
- TalkBack announcement under thermal override: **manual check still pending**
  (`adb shell cmd thermalservice override-status 3` then start assistance).

## 6. Privacy audit

**Tool:** `tools/scripts/privacy_audit.sh` (static, CI-safe)

Checks:

- Manifest permissions only `CAMERA` + `VIBRATE` (no INTERNET/location/mic/media).
- No network client APIs or Gradle network deps.
- `SafeLogger.logRaw` still returns `Nothing`.
- No forbidden log patterns (OCR text, frames, base64, etc.).
- `android.util.Log` only inside SafeLogger wrappers.

**Run result (2026-09-24): PASS** (`./tools/scripts/privacy_audit.sh` exit 0).

Manual privacy verification still required for exit:

- [ ] Airplane mode: obstacle + object + text reading still work.
- [ ] `adb shell dumpsys netstats` / airplane: no app network sockets while core features run.
- [ ] Logcat soak: no OCR text, no frames.

## 7. Security scan report

Static review (no new third-party scanner dependency; MVP offline posture):

| Check | Result |
|---|---|
| Exported activities | Only `MainActivity` launcher (required). |
| Permissions | Minimal (camera, vibrate). |
| Cleartext traffic | No network usage; no `INTERNET` permission. |
| Secrets / tokens | None in repo assets. |
| ProGuard | Release minify enabled (`app/build.gradle.kts`). |
| Dependency surface | Approved stack only (AGENTS §3). |

`./tools/scripts/package_size_report.sh` optional size budget check.

## 8. Accessibility audit report

| Check | Evidence |
|---|---|
| Automated Espresso AccessibilityChecks | `./tools/scripts/accessibility_scan.sh` (needs device; Ci uses `connectedDebugAndroidTest` when available). |
| Content descriptions on Settings benchmarks | `fragment_settings.xml` (benchmark + export buttons). |
| Diagnostics export non-visual result | Settings announces via `AnnouncementUtil` + TTS. |
| TalkBack start/stop/pause + audio | **Manual pending** (M4 checklist). |
| OCR accessibility flow | **Manual pending** (M5 checklist). |
| Degradation TalkBack announcement | **Manual pending** (M6 + thermal override). |

## 9. Crash stability report

**Tool:** `tools/scripts/crash_stability.sh [watch_sec] [relaunch]`

```bash
./tools/scripts/crash_stability.sh 120 1
```

Writes `tools/reports/crash_stability_*.log` + logcat dumps. Fails on package
`FATAL EXCEPTION` / `OutOfMemoryError`.

**Evidence (SM-A226BR, 2026-09-24):** `tools/reports/crash_stability_20260924_115508.log`
— **PASS**, fatal_exceptions=0, package_fatal=0, anr=0, oom=0 (40 s watch after cold start).

## 10. Requirement traceability update

| Requirement area | M7 evidence |
|---|---|
| NFR-PE-002 detector P95 ≤ 450 ms | Settings detector benchmark **62 ms P95** (SM-A226BR) + `DiagnosticsReportTest` |
| NFR-PE-001 / OR-003 memory ≤ 800 MB | `memory_soak.sh` peak **≤ 135 MB** short window; `memoryBudgetPass` |
| FR-009.5 OCR P95 ≤ 8 s | Settings OCR benchmark **132 ms P95** (synthetic mid-gray page) |
| OR-006/007 thermal & battery | M6 `ResourceGovernor` + thermal report tool |
| Privacy: no image/OCR log | `SafeLogger` + `privacy_audit.sh` |
| Offline core | No INTERNET permission + airplane mode test |
| Model quality targets | `evaluate_detections.py` + DAT-001 (pending) |

Roadmap status table: M7 → **Done (with release blockers listed)** after quality gate.

---

## Release blockers (documented per exit criteria)

1. Only **1 of 3** low-end reference devices in hand — multi-device perf exit unmet.
2. **DAT-001** labeled dataset not yet available — model recall/precision/closed-loop metrics pending.
3. **Static INT8 / QAT** conversion pending **human review** (stop condition).
4. Manual TalkBack flows (M4/M5/M6) and full 30-min soak + airplane traffic inspection must be checked before MVP 1.0 release checklist.
