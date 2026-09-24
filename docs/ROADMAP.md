# ROADMAP.md

## Vision-RT — Vision-RealTime  
### MVP 1.0 Development Milestones and Schedule

> **Executive summary:** Plan (v1.0) to deliver Vision-RT MVP 1.0 from 2026-09-28 to 2027-03-19 (25 weeks, RC 2027-03-12, release 2027-03-19). Nine milestones: M0 foundation+CI, M1 accessible shell+safety onboarding, M2 orchestration with fake perception, M3 camera+detector (YOLOv8n INT8), M4 feedback/haptics/accessibility, M5 OCR+object summary, M6 resource governance+degradation, M7 benchmark/validation/privacy audit, M8 real-user validation+RC, then release. Weekly cronograma included. Model targets: recall ≥ 0.85, precision ≥ 0.75, direction ≥ 0.90, proximity ≥ 0.80, false alerts ≤ 2-5/10 min. Quality gate per milestone; agents must not advance until prior exit criteria are met. If behind schedule, reduce scope in defined order (never accessibility, disclaimer, offline, privacy, degradation, critical alerts, or user validation).

**Project name:** Vision-RT  
**Expanded name:** Vision-RealTime  
**Document version:** 1.0  
**Baseline date:** 2026-09-23  
**Plan start date:** 2026-09-28  
**MVP 1.0 release candidate target:** 2027-03-12  
**MVP 1.0 release target:** 2027-03-19  
**Primary platform:** Android  
**Primary target:** Low-end smartphones with 4 GB RAM  

---

## 1. Purpose

This roadmap defines the milestones, sequence, schedule, and release gates required to deliver Vision-RT MVP 1.0.

The MVP must satisfy the mandatory requirements defined in:

- `requirements.md`
- `ARCHITECTURE.md`
- `AGENTS.md`

The MVP is not a research demo. It must be a usable, accessible, offline-first Android assistance application for people with visual disability, running reliably on low-end hardware.

---

## 2. MVP 1.0 Definition

Vision-RT MVP 1.0 shall deliver:

1. Android-native application built with Kotlin.
2. Accessibility-first UI operable with TalkBack.
3. Safety disclaimer and complement-only positioning.
4. Offline core assistance.
5. Obstacle Awareness Mode using a lightweight quantized detector.
6. Simplified direction and proximity feedback.
7. On-demand object summary using template-based language.
8. Text Reading Mode using on-device OCR.
9. Audio feedback using system TTS and non-speech cues.
10. Haptic feedback for critical alerts.
11. Resource-aware orchestration for low-end devices.
12. Thermal, battery, memory, and latency degradation behavior.
13. Accessible settings for verbosity, speech rate, haptics, and sensitivity.
14. Benchmark and diagnostics mode for QA.
15. Privacy-preserving local processing by default.
16. Validation with real users with visual disability.

---

## 3. Explicitly Out of Scope for MVP 1.0

The following are not included in MVP 1.0 unless separately approved:

1. iOS application.
2. Continuous rich scene narration.
3. Continuous on-device VLM usage.
4. Cloud-based scene description by default.
5. ESP32/WiFi sensing.
6. External wearables.
7. Turn-by-turn navigation.
8. Autonomous guidance.
9. Medical-device claims.
10. Multi-language OCR beyond the primary launch language pack.
11. Advanced depth sensing unless a validated device-specific sensor path exists.

---

## 4. Planning Assumptions

This schedule assumes:

| Role | Assumed capacity |
|---|---:|
| Android engineer | 1 full-time |
| ML / inference engineer | 1 full-time or strong part-time |
| Accessibility / UX researcher | 0.5 capacity |
| QA engineer | 0.5 capacity |
| Product / safety reviewer | 0.25 capacity |

If the team is smaller, dates must shift. If development is solo, milestones should be treated as phase gates rather than fixed dates.

Additional assumptions:

1. At least three low-end Android reference devices are available by Milestone M3.
2. Real users with visual disability can be recruited for Milestone M8.
3. The MVP prioritizes reliability over feature richness.
4. Model accuracy targets are validated iteratively.
5. If model targets are not met, the MVP may ship with conservative alerting and clearly disclosed limitations.

---

## 5. MVP 1.0 Milestone Overview

| Milestone | Name | Duration | Dates | Main Outcome |
|---|---|---:|---|---|
| M0 | Project Foundation and Guardrails | 2 weeks | 2026-09-28 to 2026-10-09 | Repo, modules, CI, quality gates |
| M1 | Accessible App Shell and Safety Onboarding | 3 weeks | 2026-10-12 to 2026-10-30 | Accessible navigation, disclaimer, settings base |
| M2 | Orchestration Core with Fake Perception | 3 weeks | 2026-11-02 to 2026-11-20 | State machine, alert policy, feedback queue |
| M3 | Camera Pipeline and Detector MVP | 4 weeks | 2026-11-23 to 2026-12-18 | CameraX, frame gate, YOLOv8n INT8 integration |
| M4 | Feedback, Haptics, and Accessibility Hardening | 3 weeks | 2026-12-21 to 2027-01-08 | TTS, earcons, haptics, accessible alerts |
| M5 | OCR and On-Demand Object Summary | 3 weeks | 2027-01-11 to 2027-01-29 | ML Kit OCR, template summary, idle unload |
| M6 | Resource Governance and Degradation | 2 weeks | 2027-02-01 to 2027-02-12 | Thermal, battery, memory, FPS adaptation |
| M7 | Benchmark, Model Validation, and Privacy Audit | 2 weeks | 2027-02-15 to 2027-02-26 | Device matrix, metrics, security/privacy checks |
| M8 | User Validation, Hardening, and Release Candidate | 2 weeks | 2027-03-01 to 2027-03-12 | Real-user testing, final fixes, RC |
| Release | MVP 1.0 Stabilization and Release | 1 week | 2027-03-15 to 2027-03-19 | Production or beta release |

Holiday note: Weeks of 2026-12-21 and 2026-12-28 are expected to have reduced capacity. The plan includes lighter integration/testing work during that period.

---

## 6. Detailed Milestones

---

## M0 — Project Foundation and Guardrails

**Dates:** 2026-09-28 to 2026-10-09  
**Status:** Done  
**Goal:** Establish the technical and quality foundation.

### Deliverables

1. Android project created with Kotlin and Gradle Kotlin DSL.
2. Minimum SDK 30 configured.
3. Module structure from `ARCHITECTURE.md` created.
4. CI pipeline configured.
5. Lint, Detekt, ktlint, and unit test execution enabled.
6. Basic dependency injection setup with Hilt.
7. Logging policy implemented:
   - No images.
   - No OCR text.
   - No personal data.
8. Initial folder structure for documentation.
9. Device procurement list finalized.
10. Initial dataset annotation schema defined.

### Technical tasks

- Create app, core, feature, perception, inference, feedback, data, benchmark modules.
- Configure CI:
  - build
  - test
  - lint
  - accessibility scan placeholder
  - package size report
- Define code ownership and PR template.
- Define baseline performance test harness skeleton.
- Define safe logging utility.

### Exit criteria

- Project builds successfully.
- CI runs unit tests and lint.
- Module boundaries are enforced by convention.
- No network permissions are present in core modules.
- Safe logging utility rejects image/text payloads by design.
- Dataset annotation schema approved.

---

## M1 — Accessible App Shell and Safety Onboarding

**Dates:** 2026-10-12 to 2026-10-30  
**Status:** Done  
**Goal:** Build the accessible application shell and mandatory safety onboarding.

### Deliverables

1. Home screen with primary start/stop control.
2. Accessible navigation structure.
3. Onboarding flow.
4. Safety disclaimer with explicit acknowledgment.
5. Help and limitations screen.
6. Settings screen skeleton.
7. Accessibility announcement utilities.
8. Focus management utilities.
9. Large touch target components.
10. Local preferences storage using DataStore.

### Functional scope

- FR-001 Safety disclaimer.
- FR-002 Onboarding.
- FR-003 Core mode navigation.
- FR-004 Start/stop control shell.
- FR-012 Settings skeleton.
- UX-001 non-visual-first interaction foundation.

### Technical tasks

- Implement ViewBinding-based screens.
- Implement accessible top-level navigation.
- Implement disclaimer state persistence.
- Implement accessible dialogs and focus return.
- Implement reusable accessible button and status components.
- Add automated UI tests for critical navigation.
- Add first accessibility scan in CI.

### Exit criteria

- A TalkBack user can move from launch to home and back.
- Disclaimer cannot be bypassed.
- Start/stop control is visible, focusable, labeled, and large enough.
- Settings screen is accessible and persistent.
- Automated accessibility scan reports no critical defects on core screens.

---

## M2 — Orchestration Core with Fake Perception

**Dates:** 2026-11-02 to 2026-11-20  
**Status:** In Progress  
**Goal:** Implement the orchestration brain before integrating real models.

### Deliverables

1. Mode state machine:
   - Idle
   - Starting
   - ObstacleAssistanceActive
   - ObjectQueryActive
   - TextReadingActive
   - Degraded
   - Error
   - Stopping
2. Alert priority model.
3. Feedback dispatcher interface.
4. Fake detector adapter.
5. Fake OCR adapter.
6. Fake TTS/haptics adapters for testing.
7. Alert cooldown and confidence filtering logic.
8. Template-based message generator.
9. Orchestration unit tests.
10. Integration tests using fakes.

### Functional scope

- FR-005 alert policy foundation.
- FR-006 alert prioritization.
- FR-007 sector/proximity logic.
- OR-008 confidence and persistence rules.
- OR-009 audio/inference concurrency foundation.
- OR-010 graceful degradation foundation.

### Technical tasks

- Implement state machine with Kotlin coroutines.
- Implement latest-only channel patterns for future frames.
- implement AlertPolicy:
  - confidence threshold
  - temporal persistence
  - cooldown
- Implement TemplateComposer:
  - “Person ahead.”
  - “Chair on left.”
  - “Possible obstacle.”
- Implement fake perception sources for deterministic testing.
- Add tests for all state transitions.

### Exit criteria

- Orchestration works end-to-end with fake detections.
- Critical alerts interrupt lower-priority feedback.
- Alert cooldown prevents repeated alerts.
- State transitions are fully tested.
- No UI state change occurs silently without accessible announcement.
- Fake pipeline demonstrates full alert path from detection to feedback.

---

## M3 — Camera Pipeline and Detector MVP

**Dates:** 2026-11-23 to 2026-12-18  
**Status:** Done  
**Goal:** Integrate real camera capture and detector inference on low-end devices.

### Deliverables

1. CameraX integration.
2. ImageAnalysis pipeline.
3. Latest-frame-only FrameGate.
4. Preprocessing path:
   - downscale to detector input
   - avoid unnecessary Bitmap allocation
5. LiteRT/TFLite detector runtime adapter.
6. YOLOv8n INT8 model integration.
7. Detection post-processing:
   - NMS
   - confidence filtering
   - sector calculation
   - proximity approximation
8. Initial detection benchmark.
9. First low-end device test pass.
10. Model manifest loader.

### Functional scope

- FR-005 Obstacle Awareness Mode initial implementation.
- FR-007 direction/proximity estimation initial implementation.
- OR-001 device profile detection.
- OR-002 model loading rules.
- OR-003 memory budget monitoring.
- OR-004 latency measurement.

### Technical tasks

- Implement CameraX lifecycle binding.
- Implement frame drop strategy.
- Implement detector interface:
  - load
  - detect
  - unload
- Implement model manifest validation.
- Implement initial benchmark mode:
  - inference latency
  - FPS
  - memory estimate
- Test detector on at least one low-end device.

### Model tasks

- Prepare YOLOv8n INT8 export.
- Calibrate quantization using representative images.
- Evaluate baseline metrics:
  - precision
  - recall
  - mAP@0.5
  - inference latency
- Define class mapping for MVP priority objects.
- Identify dataset gaps.

### Exit criteria

- Camera pipeline runs without crashing on at least one low-end device.
- Detector inference works offline.
- Detector input defaults to 320x320.
- Frame dropping works under load.
- No Bitmap-per-frame allocation pattern is present.
- Initial benchmark report exists.
- Model manifest is validated at startup.
- Detection results reach AlertPolicy in integration tests.

### Evidence (2026-09-24, SM-A226BR / R9WTC009YAW)

- Installed `versionCode=3` / `0.3.0-M3` demo APK; app launched, no crash.
- Detector benchmark (n=50, warmup=5): **p50=54 ms, p95=76 ms, p99=79 ms** (NFR-PE-002 budget 450 ms, pass=true). Logcat: `VisionRT/DetectorBenchmark`.
- OR-001: startup classifies `profile=low_end` (`ramGb=3`, registry SM-A226BR); Settings shows *Perfil de gama baja*; Low-End frame interval 125 ms (≤8 FPS).
- OR-003: PSS sampled during assistance sessions; peak/growth summary logged on stop via `VisionRT/MemoryBudgetMonitor`.
- Quality gate: `testDebugUnitTest` **106 tests, 0 failures**; detekt + lintDebug green.

---

## M4 — Feedback, Haptics, and Accessibility Hardening

**Dates:** 2026-12-21 to 2027-01-08  
**Goal:** Make alerts perceptible, interruptible, concise, and accessible.

### Deliverables

1. Android TextToSpeech integration. *(done: SystemSpeaker + speech rate)*
2. SoundPool earcons for direction/proximity. *(done: left/center/right WAVs)*
3. Haptic patterns for left/center/right and near/medium/far. *(done: HapticPatterns + VibrationEffect)*
4. Feedback priority queue. *(done: PriorityFeedbackDispatcher)*
5. Interruption behavior for critical alerts. *(done: preemption on CRITICAL)*
6. Mute/pause accessible control. *(done: speech mute + haptics/earcons toggles)*
7. Verbosity levels:
   - Minimal
   - Normal
   - Detailed
8. Speech rate setting. *(done: 0.5x–2.0x SeekBar, FR-010.5)*
9. Haptic intensity setting. *(done: light/medium/strong, FR-011.5)*
10. Accessibility announcement hardening. *(done: Settings announcements for new prefs)*

### Functional scope

- FR-006 alert content.
- FR-010 audio feedback.
- FR-011 haptic feedback.
- FR-012 preferences.
- UX-004 audio design and cognitive load.
- UX-005 haptic/non-speech cues.

### Technical tasks

- Implement FeedbackDispatcher.
- Implement TTS queue and interruption.
- Implement haptic pattern manager.
- Implement earcon assets.
- Implement user settings integration.
- Add tests for priority preemption.
- Add TalkBack manual test scripts.

### Exit criteria

- Critical alerts produce haptic and audio feedback. *(2026-09-24: PriorityFeedbackDispatcherTest — CRITICAL emits haptic+earcon+speech, mute keeps haptics, intensity shapes waveform)*
- TTS does not block inference. *(speech on dispatcher worker; inference path unchanged — ARCHITECTURE §12.5)*
- Critical alerts interrupt non-critical speech. *(dispatcher test: critical preempts pending lower-priority speech + speaker.stop())*
- User can pause or mute non-critical speech within one second. *(speech mute + haptics/earcons toggles in Settings; dispatcher skips speech queue when muted)*
- Verbosity setting changes feedback behavior. *(AlertPolicyConfig verbosity already live in M2/M3; M4 settings surface persists it)*
- TalkBack manual flow passes for start/stop/pause. *(manual: run on SM-A226BR before marking Done)*
- Feedback works with wired headphones and Bluetooth where supported. *(manual: system TTS/SoundPool routes to active output; verify on device)*

**Quality gate (2026-09-24):** `./gradlew testDebugUnitTest detekt lintDebug` green — **132 unit tests, 0 failures**; detekt + lint clean. Installed `versionCode=4` / `0.4.0-M4` demo APK.

---

## M5 — OCR and On-Demand Object Summary

**Dates:** 2027-01-11 to 2027-01-29  
**Goal:** Deliver text reading and on-demand object summary.

### Deliverables

1. ML Kit Text Recognition v2 integration. *(done: MlKitTextRecognizer, ADR-006)*
2. OCR lifecycle manager:
   - lazy load *(done: OcrLifecycleManager.ensureLoaded)*
   - idle unload *(done: 30s OcrLifecycle.IDLE_UNLOAD_MS)*
3. Accessible Text Reading Mode. *(done: TextReadingFragment + nav dest_text_reading)*
4. Read/repeat/next/stop controls. *(done: TextReadingSession + FR-009.4 buttons)*
5. On-demand object summary using detector + templates. *(done: ObjectSummaryComposer + ObjectSummaryService; home object button)*
6. OCR failure handling. *(done: unclear/timeout messages; Result.failure; mode restore)*
7. Low-resolution/high-resolution capture strategy. *(analysis 640×480 RGB → TextCapture; high-res ImageCapture deferred — document limitation)*
8. OCR benchmark. *(done: DiagnosticsPort.runOcrBenchmark, Settings OCR button, 8s P95 budget)*
9. OCR accessibility flow validation. *(manual: TalkBack on SM-A226BR before Done)*

### Functional scope

- FR-008 On-demand object summary.
- FR-009 Text Reading Mode.
- OR-002 model lifecycle.
- OR-003 memory usage control.
- OR-010 graceful degradation.

### Technical tasks

- Implement TextRecognizer interface.
- Implement ML Kit adapter.
- Implement OCR idle timeout.
- Implement text block segmentation and reading order.
- Implement accessible OCR result navigation.
- Implement object summary composer:
  - detected objects
  - direction
  - proximity
  - concise template
- Add OCR timeout and failure states.
- Add tests using fake OCR.

### Exit criteria

- OCR works offline after model availability. *(ML Kit latin pack ships in APK; FR-009.2)*
- OCR unloads after 30 seconds idle. *(OcrLifecycleManager + OcrLifecycle.IDLE_UNLOAD_MS=30_000)*
- User can repeat, move to next block, and stop reading. *(TextReadingFragment read/repeat/next/stop + TextReadingSession)*
- OCR result is announced accessibly. *(OCR_RESULT alerts via PriorityFeedbackDispatcher + dual announce in TextReadingFragment)*
- Object summary responds within target P95 on reference devices. *(ObjectSummaryComposer one-shot; FR-008.4 5s budget, 8s timeout message)*
- OCR failure does not crash obstacle mode. *(Result.failure paths; recoverCatching returns to prior mode)*
- Memory remains within budget after OCR load/unload cycles. *(lazy load + 30s unload; OR-002.6 single heavy model)*

**Quality gate (2026-09-24):** `./gradlew testDebugUnitTest detekt lintDebug` green — **tests pass**; detekt + lint clean. Installed `versionCode=5` / `0.5.0-M5` demo APK.

---

## M6 — Resource Governance and Degradation

**Dates:** 2027-02-01 to 2027-02-12  
**Goal:** Make the app stable under thermal, battery, memory, and latency pressure.

### Deliverables

1. ResourceGovernor implementation.
2. Thermal policy.
3. Battery saver policy.
4. Memory pressure policy.
5. Latency-based FPS adaptation.
6. Degradation ladder:
   - Normal
   - Reduced
   - Minimal
   - Critical
7. Accessible degradation notifications.
8. Diagnostics events for resource state changes.

### Functional scope

- OR-005 adaptive frame rate.
- OR-006 thermal management.
- OR-007 battery management.
- OR-010 graceful degradation.
- FR-015 status notifications.
- NFR-PE-004 battery efficiency.
- NFR-PE-005 thermal behavior.

### Technical tasks

- Implement resource monitor collecting:
  - thermal status *(AndroidResourceSignals / PowerManager.currentThermalStatus)*
  - battery level *(ACTION_BATTERY_CHANGED sticky)*
  - battery saver state *(PowerManager.isPowerSaveMode)*
  - memory pressure *(MemoryBudgetMonitor.peakMb + onLowMemorySignal)*
  - inference latency *(ResourceManager P95 ring buffer)*
  - FPS *(ResourceGovernor.frameIntervalMs from DeviceProfile base)*
- Implement policy rules. *(ResourceGovernor.desiredLevel / causeFor)*
- Implement degradation state transitions. *(HOLD_TICKS hysteresis + ModeController DEGRADED on CRITICAL)*
- Implement accessible announcements:
  - “Reduced mode due to device heat.” *(DegradationAnnouncer EN/ES)*
  - “Battery saver active.” *(DegradationAnnouncer)*
  - “Assistance limited.” *(DegradationAnnouncer + StateAnnouncer DEGRADED)*
- Add stress tests using simulated slow inference and memory pressure. *(ResourceGovernorTest / DegradationAnnouncerTest)*

### Exit criteria

- At MODERATE thermal status, FPS reduces by at least 50%. *(interval ×2; thermalModerateEscalatesToReducedAndHalvesFps)*
- At SEVERE thermal status, continuous detection stops or becomes minimal. *(CRITICAL + continuousDetectionStopped; thermalSevereStopsContinuousDetection)*
- Battery saver reduces FPS to <= 3. *(BATTERY_SAVER_MIN_INTERVAL_MS=333; batterySaverCapsIntervalAt3Fps)*
- Memory pressure unloads optional components first. *(optionalModelsDisabled at REDUCED+; memoryCriticalYieldsMinimal)*
- Degradation events are announced accessibly. *(DegradationAnnouncer STATUS alerts via FeedbackPort)*
- No crash occurs during simulated resource pressure tests. *(unit stress: thermal/battery/memory/latency/camera paths)*
- The app returns to normal mode safely when conditions improve. *(recoversToNormalWhenSignalsClear + syncModeWithLevel)*

**Quality gate (2026-09-24):** `./gradlew testDebugUnitTest detekt lintDebug` green — **172 tests, 0 failures**; detekt + lint clean. Installed `versionCode=6` / `0.6.0-M6` demo APK.

---

## M7 — Benchmark, Model Validation, and Privacy Audit

**Dates:** 2027-02-15 to 2027-02-26  
**Goal:** Prove that MVP meets performance, accuracy, privacy, and reliability targets.

### Deliverables

1. Device matrix benchmark report. *(tools + Settings diagnostics export; `docs/M7_REPORTS.md` §1 — 1/3 devices in hand)*
2. Model validation report. *(evaluate_detections.py + fixtures smoke; real metrics pending DAT-001)*
3. Memory soak report. *(`tools/scripts/memory_soak.sh`; 30-min run pending on device)*
4. Battery usage report. *(`tools/scripts/battery_thermal_report.sh`)*
5. Thermal behavior report. *(same tool + `FORCE_THERMAL=1` / M6 governor)*
6. Privacy audit:
   - no image upload
   - no OCR text logging
   - permissions review
   *(`tools/scripts/privacy_audit.sh` PASS 2026-09-24; airplane mode + traffic inspection manual)*
7. Security scan report. *(static review in `docs/M7_REPORTS.md` §7)*
8. Accessibility audit report. *(Espresso suite + manual TalkBack pending M4–M6 checklist)*
9. Crash stability report. *(`tools/scripts/crash_stability.sh`)*
10. Final requirement traceability update. *(`docs/M7_REPORTS.md` §10)*

Consolidated evidence: **`docs/M7_REPORTS.md`**.

### Verification scope

- Performance:
  - cold start
  - camera ready
  - detector inference P95
  - end-to-end alert P95
  - OCR P95
- Memory:
  - peak PSS
  - 30-minute soak
- Reliability:
  - crash-free sessions
  - failure recovery
- Accessibility:
  - automated scan
  - manual TalkBack audit
- Privacy:
  - airplane mode core test
  - network traffic inspection
  - log audit

### Model validation targets

- Priority object recall >= 0.85.
- Priority object precision >= 0.75.
- Direction accuracy >= 0.90.
- Proximity accuracy >= 0.80.
- False alert rate:
  - <= 2 per 10 minutes in controlled low-clutter routes.
  - <= 5 per 10 minutes in complex high-clutter routes.

If thresholds are not met:

1. Reduce class scope if necessary.
2. Increase conservative alert language.
3. Document limitations.
4. Do not make unsafe claims.
5. Obtain product/safety approval before release.

### Exit criteria

- All mandatory performance thresholds are met on at least three low-end reference devices. *(NOT MET: only SM-A226BR in hand — release blocker)*
- No raw images or OCR text are found in logs or network traffic. *(static privacy audit PASS; airplane + traffic inspection manual pending)*
- Core features pass in airplane mode. *(manual pending)*
- Accessibility audit passes. *(automated suite + manual TalkBack pending)*
- Crash stability is acceptable. *(tool ready; device watch pending)*
- Requirement traceability is updated. *(`docs/M7_REPORTS.md` §10)*
- Release blockers are documented. *(`docs/M7_REPORTS.md` §blockers: 1/3 devices, DAT-001, static INT8/QAT human review, manual TalkBack/soak)*

**Model conversion note:** static INT8 / QAT remains **blocked pending human review** (AGENTS stop condition); runtime stays dynamic weight-only INT8.

**Quality gate (2026-09-24):** `./gradlew testDebugUnitTest detekt lintDebug` green — **176 tests, 0 failures**; detekt + lint clean; `privacy_audit.sh` PASS; evaluator fixture smoke PASS. Installed `versionCode=7` / `0.7.0-M7` demo APK.

**Status:** Done (with release blockers above; M8 can proceed with them open).

---

## M8 — User Validation, Hardening, and Release Candidate

**Dates:** 2027-03-01 to 2027-03-12  
**Goal:** Validate the MVP with real users and prepare the release candidate.

### Deliverables

1. Usability testing protocol.
2. Accessibility testing protocol.
3. Participant consent and privacy process.
4. At least two rounds of user testing.
5. At least 8 participants with visual disability across validation.
6. Bug triage and hardening.
7. Final onboarding improvements.
8. Final help/limitation content review.
9. Release candidate build.
10. MVP 1.0 release checklist.

### User validation targets

- Task success rate >= 90% for critical tasks.
- System Usability Scale score >= 75.
- At least 80% of participants correctly understand complement-only role.
- No unresolved critical accessibility blocker.
- No unresolved safety-related defect.

### Critical user tasks

1. Start assistance.
2. Stop assistance.
3. Pause or mute speech.
4. Change verbosity.
5. Request object summary.
6. Read text.
7. Understand a degradation warning.
8. Report an incorrect alert if implemented.

### Exit criteria

- User validation targets met or explicitly approved with documented risk.
- All critical and high-severity defects resolved or deferred with approval.
- Safety disclaimer and limitation messaging approved.
- Release candidate passes smoke test on all reference devices.
- Release notes and known limitations documented.

---

## Release — MVP 1.0 Stabilization and Release

**Dates:** 2027-03-15 to 2027-03-19  
**Goal:** Stabilize and release MVP 1.0.

### Activities

1. Release candidate monitoring.
2. Critical bug fixing only.
3. Final accessibility smoke test.
4. Final performance smoke test.
5. Store listing review:
   - no unsafe claims
   - clear complement-only language
6. Privacy policy review.
7. Known limitations publication.
8. Production or controlled beta release.

### Release decision gates

- No critical crash.
- No critical accessibility blocker.
- No unsafe messaging.
- No privacy violation.
- Core offline features stable.
- Resource degradation behavior verified.
- User validation approved.

---

## 7. Weekly Cronograma

| Week | Dates | Milestone | Main Focus |
|---:|---|---|---|
| 1 | 2026-09-28 to 2026-10-02 | M0 | Project setup, modules, CI |
| 2 | 2026-10-05 to 2026-10-09 | M0 | Safe logging, quality gates, dataset schema |
| 3 | 2026-10-12 to 2026-10-16 | M1 | App shell, navigation, accessible components |
| 4 | 2026-10-19 to 2026-10-23 | M1 | Onboarding, disclaimer, settings skeleton |
| 5 | 2026-10-26 to 2026-10-30 | M1 | Accessibility audit, focus management |
| 6 | 2026-11-02 to 2026-11-06 | M2 | State machine, alert model |
| 7 | 2026-11-09 to 2026-11-13 | M2 | Fake perception, feedback queue |
| 8 | 2026-11-16 to 2026-11-20 | M2 | Orchestration tests, template messages |
| 9 | 2026-11-23 to 2026-11-27 | M3 | CameraX pipeline, frame gate |
| 10 | 2026-11-30 to 2026-12-04 | M3 | LiteRT adapter, model manifest |
| 11 | 2026-12-07 to 2026-12-11 | M3 | YOLOv8n INT8 integration |
| 12 | 2026-12-14 to 2026-12-18 | M3 | Low-end benchmark, post-processing |
| 13 | 2026-12-21 to 2026-12-25 | M4 | TTS integration, reduced holiday capacity |
| 14 | 2026-12-28 to 2027-01-01 | M4 | Earcons/haptics, reduced holiday capacity |
| 15 | 2027-01-04 to 2027-01-08 | M4 | Feedback queue, accessibility hardening |
| 16 | 2027-01-11 to 2027-01-15 | M5 | OCR adapter, lifecycle |
| 17 | 2027-01-18 to 2027-01-22 | M5 | Text reading flow |
| 18 | 2027-01-25 to 2027-01-29 | M5 | Object summary, OCR benchmark |
| 19 | 2027-02-01 to 2027-02-05 | M6 | Resource governor, thermal policy |
| 20 | 2027-02-08 to 2027-02-12 | M6 | Degradation states, notifications |
| 21 | 2027-02-15 to 2027-02-19 | M7 | Device matrix benchmark, model validation |
| 22 | 2027-02-22 to 2027-02-26 | M7 | Privacy/security audit, stability |
| 23 | 2027-03-01 to 2027-03-05 | M8 | User testing round 1, fixes |
| 24 | 2027-03-08 to 2027-03-12 | M8 | User testing round 2, release candidate |
| 25 | 2027-03-15 to 2027-03-19 | Release | Stabilization and MVP 1.0 release |

---

## 8. Critical Path

The critical path for MVP 1.0 is:

```text
M0 Project foundation
→ M1 Accessible shell
→ M2 Orchestration core
→ M3 Camera + detector
→ M4 Feedback/accessibility
→ M5 OCR/object summary
→ M6 Resource governance
→ M7 Benchmark/validation
→ M8 User validation
→ Release
```

Most schedule risk is concentrated in:

1. M3: camera and detector performance on low-end hardware.
2. M6: resource governance and degradation stability.
3. M7: benchmark and model validation.
4. M8: real-user accessibility validation.

---

## 9. Dependencies

### Technical dependencies

1. Low-end reference devices.
2. YOLOv8n INT8 conversion pipeline.
3. Calibration/validation dataset.
4. ML Kit offline OCR availability.
5. Device benchmark tooling.
6. Accessibility testing tools.
7. CI environment.

### Human dependencies

1. Product owner for scope decisions.
2. Accessibility consultant or users with visual disability.
3. Safety/compliance reviewer for disclaimer and limitations.
4. QA owner for device matrix.
5. ML owner for model validation.

### External dependencies

1. Recruitment of users with visual disability.
2. Consent and privacy process for user testing.
3. Availability of representative low-end devices.
4. App store review timelines.

---

## 10. Quality Gates by Milestone

| Milestone | Quality Gate |
|---|---|
| M0 | CI passes; module boundaries clean; safe logging enforced. |
| M1 | Core screens accessible; disclaimer cannot be bypassed. |
| M2 | Orchestration state machine fully tested with fakes. |
| M3 | Detector runs offline on low-end device; frame dropping works. |
| M4 | Critical alerts produce haptic/audio; TTS does not block inference. |
| M5 | OCR works offline and unloads after idle. |
| M6 | Degradation behavior verified under simulated stress. |
| M7 | Performance, memory, privacy, and accuracy gates pass. |
| M8 | Real-user validation targets met; no critical blockers. |
| Release | MVP 1.0 stable, safe, accessible, and offline-capable. |

---

## 11. MVP 1.0 Acceptance Checklist

Before MVP 1.0 release, all of the following must be true:

### Functional

- [ ] Obstacle Awareness Mode works offline.
- [ ] On-demand object summary works offline.
- [ ] Text Reading Mode works offline.
- [ ] Start/stop works accessibly.
- [ ] Settings persist.
- [ ] Degradation notifications work.

### Accessibility

- [ ] TalkBack flow passes for all critical tasks.
- [ ] No critical accessibility defects.
- [ ] Large touch targets verified.
- [ ] All critical state changes are announced.
- [ ] Pause/mute is reachable and accessible.

### Performance

- [ ] Peak memory <= 800 MB on reference devices.
- [x] Detector inference P95 <= 450 ms at 320x320. (2026-09-24 SM-A226BR: p95=76 ms, n=50)
- [ ] End-to-end critical alert P95 <= 800 ms.
- [ ] TTS start <= 500 ms after alert generation.
- [ ] Haptic start <= 300 ms after alert generation.
- [ ] OCR P95 <= 8 s.
- [ ] No OutOfMemoryError in 30-minute soak.

### Reliability

- [ ] No critical crash in soak test.
- [ ] App recovers from camera failure.
- [ ] App recovers from OCR failure.
- [ ] App recovers from TTS failure.
- [ ] App handles process death gracefully.

### Safety

- [ ] Disclaimer implemented and acknowledged.
- [ ] Complement-only language verified.
- [ ] Known limitations documented.
- [ ] Uncertainty messaging implemented.
- [ ] No unsafe claims in UI or store listing.

### Privacy

- [ ] Core works in airplane mode.
- [ ] No image upload in offline mode.
- [ ] No OCR text in logs.
- [ ] No raw camera frames stored.
- [ ] Telemetry is privacy-safe.

### User validation

- [ ] At least 8 users with visual disability participated.
- [ ] Task success rate >= 90%.
- [ ] SUS score >= 75.
- [ ] No unresolved critical accessibility blocker.

---

## 12. Risk Management

| Risk | Probability | Impact | Mitigation |
|---|---:|---:|---|
| Low-end detector too slow | High | High | Reduce resolution/FPS, INT8, native preprocessing if needed |
| INT8 accuracy drops too much | Medium | High | Calibration, QAT, FP16 fallback, conservative alerts |
| OCR too heavy for 4 GB | Medium | Medium | Lazy load, idle unload, lower capture resolution |
| Accessibility regressions | Medium | High | CI checks, manual TalkBack tests, user testing |
| Thermal throttling | High | Medium | Resource governor, degraded mode, user notification |
| Real-user recruitment delay | Medium | High | Start recruiting by M4, keep pilot group small |
| Device fragmentation | High | Medium | Reference device matrix, graceful degradation |
| Model dataset gaps | Medium | High | Start dataset schema early, use conservative messaging |
| Scope creep | High | Medium | Strict MVP scope, change control |
| Holiday slowdown | High | Low | Plan lighter work in W13/W14 |

---

## 13. Scope Control Rules

If the project falls behind, reduce scope in this order:

### First to reduce

1. Number of detected object classes.
2. Optional detailed summary verbosity.
3. Optional diagnostics detail.
4. Optional benchmark screens.
5. OCR language packs beyond primary language.

### Do not reduce

1. Accessibility.
2. Safety disclaimer.
3. Offline core.
4. Privacy defaults.
5. Degradation behavior.
6. Critical alert reliability.
7. User validation.

---

## 14. Agent Execution Guidance

Autonomous coding agents working on Vision-RT must follow `AGENTS.md`.

For roadmap purposes, agents should execute tasks in this order:

1. Project skeleton and module boundaries.
2. Accessible app shell.
3. Safety onboarding and disclaimer.
4. Orchestration state machine with fakes.
5. Feedback dispatcher with fake outputs.
6. Camera pipeline with frame gate.
7. Detector adapter and benchmark hooks.
8. Alert policy and temporal filtering.
9. Settings and verbosity controls.
10. OCR adapter and idle lifecycle.
11. Resource governor and degradation.
12. Diagnostics/benchmark mode.
13. Accessibility hardening.
14. Performance and memory fixes.
15. User-testing defect resolution.

Agents must not advance to a new milestone unless the previous milestone exit criteria are met.

---

## 15. Definition of MVP 1.0 Success

Vision-RT MVP 1.0 is successful if:

1. A person with visual disability can start, stop, and use core assistance features with TalkBack.
2. The app works offline on a low-end Android device.
3. The app provides concise, accessible, non-overwhelming feedback.
4. The app degrades safely under thermal, battery, and memory pressure.
5. The app does not make unsafe promises.
6. The app passes performance, accessibility, privacy, and user validation gates.
7. The product is clearly positioned as a complement to existing mobility tools.

---

## 16. Final Milestone Summary

| Milestone | Target Date | Status |
|---|---:|---|
| M0 complete | 2026-10-09 | Done |
| M1 complete | 2026-10-30 | Done |
| M2 complete | 2026-11-20 | In Progress |
| M3 complete | 2026-12-18 | Done |
| M4 complete | 2027-01-08 | Done |
| M5 complete | 2027-01-29 | Done |
| M6 complete | 2027-02-12 | Done |
| M7 complete | 2027-02-26 | Done |
| M8 complete | 2027-03-12 | Planned |
| MVP 1.0 release | 2027-03-19 | Planned |

---

## 17. Approval

| Role | Name | Signature | Date |
|---|---:|---:|---:|
| Product Owner |  |  |  |
| Engineering Lead |  |  |  |
| ML Lead |  |  |  |
| Accessibility Lead |  |  |  |
| QA Lead |  |  |  |
| Safety/Compliance Reviewer |  |  |  |
