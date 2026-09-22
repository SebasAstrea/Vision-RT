# requirements.md

## Software Requirements Specification  
### Accessible Vision and Audio Assistance Mobile Application for Users with Visual Disability

> **Executive summary:** SRS (v1.0 draft) for an accessibility-first, offline Android assistant for people with visual disability, targeting 4 GB RAM low-end devices. Defines 17 mandatory functional requirements (FR-001..017: safety disclaimer, onboarding, three core modes, obstacle awareness over 8 priority classes, concise prioritized alerts, direction/proximity, on-demand summary, OCR text reading, TTS/haptics, preferences, offline core, optional cloud, status notifications, false-alert reporting, shortcuts), 10 orchestration/resource requirements (OR-001..010: device profiling, model/memory budgets, latency limits, adaptive FPS, thermal/battery management, confidence gating, degradation), accessibility (UX-001..008), measurable NFRs mapped to ISO/IEC 25010 (performance, compatibility, reliability, security, privacy, portability), safety limitations (SAF-001..005), dataset rules (DAT-001..003), and 7 acceptance gates. Release-ready only if all MUST/SHALL requirements pass.

**Version:** 1.0  
**Status:** Draft for validation  
**Target platform:** Android mobile application first; iOS parity may be defined later  
**Primary target devices:** Low-end smartphones with limited RAM and entry-level SoCs  

---

## 1. Purpose

This document defines the functional, non-functional, UI/UX, accessibility, safety, and quality requirements for a mobile application designed to support people with visual disability by providing camera-based environmental awareness, object detection, text reading, and audio/haptic feedback.

The product is an assistive complement to existing orientation and mobility methods. It is not a replacement for a white cane, guide dog, human guide, orientation and mobility training, or professional assistance.

The main technical challenge is delivering a useful, safe, and accessible experience on modest hardware, especially devices with approximately 4 GB of RAM and entry-level MediaTek, Qualcomm, or equivalent processors.

All requirements in this document are intended to be non-ambiguous, testable, measurable, and traceable to quality characteristics from the ISO/IEC 25000 family, primarily ISO/IEC 25010.

---

## 2. Product Scope

### 2.1 In Scope

The product shall include:

1. An accessible mobile application for people with blindness or low vision.
2. Offline-first assistance features:
   - Obstacle awareness.
   - Basic object identification.
   - On-demand scene summary.
   - Text reading through OCR.
3. Audio output using system text-to-speech and non-speech auditory cues.
4. Haptic feedback for alerts and state communication.
5. Orchestration of camera input, inference models, audio output, and resource management.
6. Device capability detection and graceful degradation for low-end hardware.
7. Accessibility-first UI/UX design.
8. Privacy-preserving local processing by default.

### 2.2 Out of Scope

The following are out of scope unless separately specified and validated:

1. Replacement for a white cane, guide dog, human guide, or mobility professional.
2. Autonomous navigation.
3. Medical diagnosis or medical-device claims.
4. Guaranteed detection of all obstacles.
5. Reliable detection of transparent surfaces such as glass unless specifically validated.
6. Continuous high-detail semantic scene narration on low-end devices.
7. External WiFi-based surface mapping using ESP32 or similar hardware, unless integrated in a future phase.

---

## 3. Normative and Reference Standards

The following standards and guidelines shall be used as references for quality, usability, accessibility, and measurement.

| Standard / Guideline | Use in this project |
|---|---|
| ISO/IEC 25000 family | General framework for software product quality. |
| ISO/IEC 25010 | Product quality model for functional suitability, performance efficiency, compatibility, usability, reliability, security, maintainability, and portability. |
| ISO/IEC 25023 | Guidance for measurement of software product quality characteristics. |
| ISO 9241-11 | Usability: effectiveness, efficiency, and satisfaction. |
| ISO 9241-171 | Guidance on software accessibility. |
| WCAG 2.2 Level AA | Web and mobile accessibility success criteria where applicable. |
| W3C Mobile Accessibility Guidance | Mobile-specific accessibility considerations. |
| Android Accessibility Guidelines | Platform accessibility requirements for Android. |
| Platform screen reader guidelines | TalkBack on Android; VoiceOver if iOS is supported later. |

Where a platform requirement conflicts with a standard, the stricter user-centered accessibility requirement shall prevail unless a documented safety or technical limitation exists.

---

## 4. Definitions

| Term | Definition |
|---|---|
| Visual disability | Blindness, low vision, or other conditions that significantly limit visual perception. |
| User | A person with visual disability using the application as a mobility or awareness complement. |
| Obstacle awareness mode | Continuous or semi-continuous mode that detects potentially relevant obstacles and alerts the user. |
| Priority object | An object class selected because it is useful for orientation and mobility. |
| Critical hazard | An object or situation with high probability of collision, disorientation, or user inconvenience. |
| Low-End Reference Device | A target smartphone with limited resources used for acceptance testing. |
| Offline core | The set of features that must work without internet access. |
| Optional intelligence | Cloud or heavy on-device features that are not required for core assistance. |
| False alert | An alert generated for an object that is not present, not relevant, or incorrectly classified. |
| Missed alert | Failure to alert for a critical hazard that should have been detected according to validation criteria. |
| Orchestration | The system logic that decides when to capture frames, run models, generate feedback, reduce load, or degrade features. |

---

## 5. User-Centered Context

### 5.1 Primary Users

The system shall be designed for:

1. Adults and older adolescents with blindness or low vision.
2. Users who may use a white cane, guide dog, human guide, or no assistive mobility tool.
3. Users who may have varying levels of technology experience.
4. Users who may rely on screen readers, gestures, haptics, and audio.
5. Users in real environments such as homes, sidewalks, public buildings, stores, and public transport areas.

### 5.2 Usage Principles

The system shall respect the following principles:

1. **Complement, not replacement.**  
   The application shall not claim to replace existing mobility tools or professional training.

2. **Safety humility.**  
   The application shall communicate uncertainty and known limitations.

3. **Non-visual-first design.**  
   The interface shall be fully operable without vision.

4. **Low-end-first engineering.**  
   The system shall be designed for constrained CPU, memory, battery, and thermal budgets.

5. **Minimal cognitive load.**  
   The system shall avoid excessive speech and unnecessary detail.

6. **User control.**  
   The user shall be able to pause, adjust, repeat, or stop feedback.

7. **Offline reliability.**  
   Core assistance shall work without internet access.

---

## 6. Target Environment and Hardware Constraints

### 6.1 Low-End Reference Device

Acceptance testing shall be performed on at least three Low-End Reference Devices.

Each Low-End Reference Device shall have:

| Attribute | Minimum Requirement |
|---|---|
| RAM | 4 GB |
| CPU | 64-bit ARM entry-level Qualcomm Snapdragon 4-series, MediaTek Helio/G-series, or equivalent |
| OS | Android 11 or higher |
| Storage | At least 2 GB free for app and models |
| Camera | Rear camera capable of at least 720p capture |
| Screen reader | TalkBack supported |
| Battery | At least 4,000 mAh for battery acceptance tests |
| Network | WiFi and mobile data available, but core tests shall be performed offline |

At least one device shall use a MediaTek SoC, at least one shall use a Qualcomm SoC, and at least one shall use a near-stock Android distribution where possible.

### 6.2 Hardware Constraints

The system shall be designed under the following constraints:

1. Limited RAM.
2. Weak CPU/GPU performance.
3. Inconsistent or unavailable NPU acceleration.
4. Thermal throttling.
5. Variable camera quality.
6. Limited battery capacity.
7. Possible Bluetooth audio latency.
8. Background process restrictions by Android OEMs.

---

## 7. Functional Requirements

Functional requirements use the following priority levels:

- **MUST**: Mandatory for release.
- **SHOULD**: Strongly recommended; if omitted, requires documented approval.
- **MAY**: Optional.

For this document, all items marked **SHALL** are mandatory unless explicitly labeled optional.

---

### FR-001 Safety Disclaimer and Role Acknowledgment

**Requirement:**  
The application SHALL present an accessible safety disclaimer before first use and SHALL require explicit user acknowledgment.

**Acceptance criteria:**

1. The disclaimer SHALL state that the application is a complement and not a replacement for a white cane, guide dog, human guide, or mobility training.
2. The disclaimer SHALL state that the application may miss obstacles, misidentify objects, or provide incomplete information.
3. The user SHALL not be able to enter core assistance modes without acknowledging the disclaimer.
4. The disclaimer SHALL be fully readable by the screen reader.
5. The acknowledgment control SHALL be accessible and clearly labeled.

**Verification:**  
Inspection, screen reader test, functional test.

**ISO/IEC 25010 mapping:**  
Functional suitability, usability, safety-related functional correctness.

---

### FR-002 Accessible Onboarding and Training

**Requirement:**  
The application SHALL provide an accessible onboarding and training flow for first-time users.

**Acceptance criteria:**

1. Onboarding SHALL explain:
   - Main modes.
   - How alerts work.
   - How to pause or stop.
   - Known limitations.
   - That the app is a complement to existing mobility tools.
2. Onboarding SHALL be operable with screen reader and gestures only.
3. Users SHALL be able to skip onboarding and access it later from settings.
4. Onboarding SHALL include a short interactive practice session.
5. At least 80% of test users with visual disability SHALL complete onboarding without sighted assistance during usability testing.

**Verification:**  
Usability testing, screen reader audit.

**ISO/IEC 25010 mapping:**  
Usability, learnability, accessibility.

---

### FR-003 Core Assistance Modes

**Requirement:**  
The application SHALL provide the following core modes:

1. Obstacle Awareness Mode.
2. On-Demand Object Summary Mode.
3. Text Reading Mode.
4. Settings and Help Mode.

**Acceptance criteria:**

1. All core modes SHALL be accessible from the main screen.
2. Each mode SHALL be identifiable by a clear text label.
3. Each mode SHALL be reachable within no more than two gestures from the home screen after onboarding.
4. The user SHALL always be able to return to the home screen with a consistent accessible action.

**Verification:**  
Functional test, accessibility audit.

**ISO/IEC 25010 mapping:**  
Functional suitability, usability.

---

### FR-004 Start and Stop Control

**Requirement:**  
The application SHALL provide a simple and accessible way to start and stop assistance.

**Acceptance criteria:**

1. A primary start/stop control SHALL be available on the main screen.
2. The control SHALL have a minimum touch target of 48 x 48 dp.
3. The control SHALL be focusable and labeled for screen readers.
4. Starting assistance SHALL produce an audible and haptic confirmation.
5. Stopping assistance SHALL produce an audible and haptic confirmation.
6. The user SHALL be able to stop non-critical speech output within one second using an accessible pause or mute control.

**Verification:**  
Functional test, accessibility audit, timing test.

**ISO/IEC 25010 mapping:**  
Usability, operability.

---

### FR-005 Obstacle Awareness Mode

**Requirement:**  
The application SHALL provide an obstacle awareness mode that detects priority objects in the camera field and informs the user.

**Acceptance criteria:**

1. The system SHALL detect at least the following priority classes in the low-end model configuration:
   - Person.
   - Chair.
   - Table.
   - Door.
   - Wall or barrier.
   - Vehicle.
   - Bicycle.
   - Generic obstacle.
2. Additional classes MAY be added if they do not violate performance or memory requirements.
3. The system SHALL not require internet access for this mode.
4. The system SHALL process camera frames at an adaptive rate controlled by the orchestration layer.
5. The system SHALL prioritize objects located in or near the user’s likely forward path.
6. The system SHALL suppress low-confidence alerts according to orchestration rules.
7. Detection recall for priority objects on the validation dataset SHALL be at least 0.85.
8. Detection precision for priority objects on the validation dataset SHALL be at least 0.75.
9. False alert rate SHALL be:
   - No more than 2 false alerts per 10 minutes in controlled low-clutter indoor routes.
   - No more than 5 false alerts per 10 minutes in complex high-clutter routes.

**Verification:**  
Model evaluation, controlled route testing, field testing.

**ISO/IEC 25010 mapping:**  
Functional suitability, accuracy, reliability.

---

### FR-006 Alert Content and Prioritization

**Requirement:**  
The application SHALL communicate alerts using concise, prioritized, and non-overwhelming feedback.

**Acceptance criteria:**

1. Each obstacle alert SHALL include at least:
   - Object category or generic obstacle label.
   - Horizontal direction: left, center, or right.
   - Relative proximity: near, medium, or far.
2. Default spoken alerts SHALL contain no more than 6 spoken words unless the user requests more detail.
3. Critical alerts SHALL interrupt non-critical speech within 300 ms.
4. The system SHALL avoid repeating the same alert more than once within a configurable cooldown period, default 5 seconds, unless proximity or risk increases.
5. The user SHALL be able to choose verbosity levels:
   - Minimal.
   - Normal.
   - Detailed.
6. Minimal verbosity SHALL provide only critical alerts.

**Verification:**  
Functional test, content audit, timing test.

**ISO/IEC 25010 mapping:**  
Usability, functional suitability, user error protection.

---

### FR-007 Direction and Proximity Estimation

**Requirement:**  
The application SHALL provide simplified directional and proximity information suitable for low-end hardware.

**Acceptance criteria:**

1. Horizontal direction SHALL be classified as:
   - Left.
   - Center.
   - Right.
2. Direction classification accuracy SHALL be at least 90% on the validation dataset.
3. Proximity SHALL be expressed only as:
   - Near.
   - Medium.
   - Far.
4. The system SHALL not state exact metric distance unless a validated depth sensor or validated depth method is available.
5. Proximity category accuracy SHALL be at least 80% on the validation dataset when evaluated against labeled zones.
6. If proximity confidence is below threshold, the system SHALL use a conservative expression such as “possible obstacle” or “I am not sure.”

**Verification:**  
Model evaluation, dataset testing, inspection.

**ISO/IEC 25010 mapping:**  
Functional suitability, accuracy, reliability.

---

### FR-008 On-Demand Object Summary

**Requirement:**  
The application SHALL allow the user to request a brief description of the current camera view.

**Acceptance criteria:**

1. The user SHALL be able to trigger object summary through an accessible control.
2. In low-end mode, the summary SHALL be generated primarily from:
   - Detected object classes.
   - Direction.
   - Relative proximity.
   - Template-based language.
3. The system SHALL NOT require a large vision-language model for core object summary on low-end devices.
4. The system SHALL respond within 5 seconds at P95 on Low-End Reference Devices for detection-based summaries.
5. If processing exceeds 8 seconds, the system SHALL inform the user that the request failed or is taking too long.
6. If a heavy or cloud-based description feature is offered, it SHALL be optional, explicitly enabled by the user, and clearly labeled as non-core.

**Verification:**  
Performance test, functional test.

**ISO/IEC 25010 mapping:**  
Performance efficiency, functional suitability, usability.

---

### FR-009 Text Reading Mode

**Requirement:**  
The application SHALL provide an accessible text reading mode using on-device OCR.

**Acceptance criteria:**

1. The user SHALL be able to capture or analyze text using an accessible action.
2. OCR SHALL work offline for the core language pack.
3. The system SHALL read detected text in logical reading order where possible.
4. The user SHALL be able to:
   - Repeat the text.
   - Move to the next text block if multiple blocks are detected.
   - Stop reading.
5. OCR processing time SHALL be no more than 8 seconds at P95 on Low-End Reference Devices for a standard single-page capture.
6. Under standard test conditions, word accuracy SHALL be at least 80%.
7. Standard test conditions are defined as:
   - Printed text of at least 12 pt font.
   - Distance of approximately 20 to 50 cm.
   - Flat, non-glossy surface.
   - Illumination of at least 300 lux.
8. If text quality is insufficient, the system SHALL provide an accessible message such as “Text is not clear. Try closer or better light.”

**Verification:**  
OCR benchmark, functional test, usability test.

**ISO/IEC 25010 mapping:**  
Functional suitability, accuracy, usability.

---

### FR-010 Audio Feedback and Text-to-Speech

**Requirement:**  
The application SHALL provide accessible audio feedback using system text-to-speech and non-speech cues.

**Acceptance criteria:**

1. All critical alerts SHALL have an audio output.
2. All critical alerts SHALL also have a haptic output unless haptics are disabled by the user.
3. TTS output SHALL start within 500 ms after the alert generation event for local TTS.
4. The application SHALL respect the user’s system TTS engine where possible.
5. The application SHALL allow adjustment of speech rate from 0.5x to 2.0x.
6. The application SHALL allow the user to select verbosity level.
7. The application SHALL support audio focus behavior so that critical alerts can interrupt or duck non-essential audio.
8. If Bluetooth audio introduces latency, the application SHALL still provide haptic feedback simultaneously with critical alerts where supported by the device.

**Verification:**  
Functional test, timing test, device matrix test.

**ISO/IEC 25010 mapping:**  
Usability, performance efficiency, compatibility.

---

### FR-011 Haptic Feedback

**Requirement:**  
The application SHALL provide haptic patterns for critical feedback.

**Acceptance criteria:**

1. Critical obstacle alerts SHALL have distinct haptic patterns.
2. Haptic patterns SHALL differentiate at least:
   - Left.
   - Center.
   - Right.
3. Near obstacles SHALL have stronger or more urgent haptic feedback than far obstacles.
4. Haptic feedback SHALL start within 300 ms of alert generation.
5. The user SHALL be able to adjust haptic intensity where supported by the OS.
6. The user SHALL be able to disable haptics, but the application SHALL warn if disabling haptics may reduce alert awareness.

**Verification:**  
Functional test, accessibility test, device matrix test.

**ISO/IEC 25010 mapping:**  
Usability, accessibility, reliability.

---

### FR-012 User Preferences

**Requirement:**  
The application SHALL provide accessible settings for core behavior.

**Acceptance criteria:**

The user SHALL be able to configure:

1. Speech rate.
2. Verbosity level.
3. Haptic intensity.
4. Sound cues on/off.
5. Camera resolution mode: low or balanced.
6. Alert sensitivity: conservative, normal, responsive.
7. Optional cloud intelligence on/off, if available.
8. Analytics or crash reporting consent.
9. Language, where multiple languages are supported.

All settings SHALL:

1. Be accessible with screen reader.
2. Persist across app restarts.
3. Apply immediately or clearly state when a restart is required.
4. Be reversible.

**Verification:**  
Functional test, persistence test, accessibility audit.

**ISO/IEC 25010 mapping:**  
Usability, functional suitability.

---

### FR-013 Offline Operation

**Requirement:**  
The application SHALL provide core assistance without internet connectivity.

**Acceptance criteria:**

1. Obstacle Awareness Mode SHALL work offline.
2. Text Reading Mode SHALL work offline for the installed language pack.
3. On-Demand Object Summary using template-based logic SHALL work offline.
4. Settings and help SHALL work offline.
5. With airplane mode enabled, all core test cases SHALL pass.
6. The application SHALL not require account creation for offline core features.

**Verification:**  
Offline test suite.

**ISO/IEC 25010 mapping:**  
Functional suitability, reliability, portability.

---

### FR-014 Optional Cloud Intelligence

**Requirement:**  
If cloud-based or heavy vision-language features are offered, they SHALL be optional and transparent.

**Acceptance criteria:**

1. Cloud intelligence SHALL be disabled by default.
2. The application SHALL clearly explain:
   - What data is sent.
   - When data is sent.
   - That cloud use is optional.
3. The application SHALL require explicit user consent before sending any image or frame data.
4. If cloud request fails or times out, the application SHALL fall back to local output or provide a clear failure message.
5. Cloud request timeout SHALL be no more than 8 seconds for optional description features.
6. Core offline features SHALL remain available regardless of cloud availability.

**Verification:**  
Functional test, network test, privacy audit.

**ISO/IEC 25010 mapping:**  
Security, functional suitability, usability.

---

### FR-015 Status and Degradation Notifications

**Requirement:**  
The application SHALL inform the user about important system states.

**Acceptance criteria:**

The application SHALL provide accessible notifications for:

1. Camera unavailable.
2. Camera blocked or covered.
3. Low light condition reducing reliability.
4. Device overheating.
5. Low battery or battery saver mode.
6. Reduced performance mode.
7. Model loading.
8. Model failure.
9. Optional cloud unavailable.
10. Assistance stopped.

Each notification SHALL:

1. Be announced by screen reader or TTS.
2. Include a clear recommended action when applicable.
3. Avoid technical jargon by default.

**Verification:**  
Functional test, edge-case test.

**ISO/IEC 25010 mapping:**  
Usability, reliability, fault tolerance.

---

### FR-016 User Feedback and False Alert Reporting

**Requirement:**  
The application SHALL allow the user to report incorrect alerts or incorrect object identifications.

**Acceptance criteria:**

1. The user SHALL be able to mark the last alert as incorrect using an accessible action.
2. The feedback mechanism SHALL not require camera roll access unless explicitly requested.
3. If images are included in feedback, explicit consent SHALL be required.
4. Feedback metadata MAY include:
   - Object class.
   - Confidence score.
   - Timestamp.
   - Device model.
   - Performance metrics.
5. Feedback metadata SHALL NOT include raw camera images unless the user explicitly opts in.

**Verification:**  
Functional test, privacy audit.

**ISO/IEC 25010 mapping:**  
Usability, maintainability, security.

---

### FR-017 Accessibility Shortcut

**Requirement:**  
The application SHALL provide a fast accessible path to start or stop assistance.

**Acceptance criteria:**

1. The main start/stop action SHALL be available from the home screen.
2. The application SHOULD integrate with Android accessibility shortcut mechanisms where technically feasible.
3. If hardware button shortcuts are supported, they SHALL not conflict with mandatory screen reader gestures.
4. The shortcut state SHALL be announced clearly.

**Verification:**  
Functional test, accessibility audit.

**ISO/IEC 25010 mapping:**  
Usability, accessibility.

---

## 8. Orchestration and Resource Management Requirements

This section addresses the main technical challenge: running useful inference on limited hardware without compromising accessibility, stability, or safety communication.

---

### OR-001 Device Capability Profiling

**Requirement:**  
The application SHALL detect device capability at startup and select an appropriate operating profile.

**Acceptance criteria:**

1. The application SHALL classify devices into at least:
   - Low-End Profile.
   - Standard Profile.
2. Devices with 4 GB RAM or entry-level SoC characteristics SHALL be assigned to Low-End Profile unless benchmark evidence justifies Standard Profile.
3. Low-End Profile SHALL enforce:
   - Detector input resolution no greater than 320 x 320 pixels.
   - Camera analysis resolution no greater than 640 x 480 pixels by default.
   - Continuous inference rate no greater than 8 FPS.
   - Optional heavy vision-language models disabled by default.
4. The selected profile SHALL be visible in settings for debugging and support purposes.

**Verification:**  
Inspection, automated startup test, device matrix test.

**ISO/IEC 25010 mapping:**  
Performance efficiency, compatibility.

---

### OR-002 Model Packaging and Memory Budget

**Requirement:**  
The application SHALL use lightweight models suitable for low-end devices.

**Acceptance criteria:**

1. Core detection model file size SHALL be no more than 20 MB after quantization or compression.
2. Core OCR model assets SHALL be no more than 40 MB for the default language pack.
3. Total core ML assets installed by default SHALL be no more than 100 MB.
4. Optional models SHALL be downloaded only after explicit user action.
5. The application SHALL avoid loading FP32 full-precision vision models in Low-End Profile.
6. The application SHALL load only one heavy inference model at a time unless memory testing proves safe.
7. OCR models SHALL be unloaded after no more than 30 seconds of inactivity.

**Verification:**  
Build inspection, memory profiling, lifecycle test.

**ISO/IEC 25010 mapping:**  
Performance efficiency, maintainability.

---

### OR-003 Memory Usage Limits

**Requirement:**  
The application SHALL remain within safe memory limits on Low-End Reference Devices.

**Acceptance criteria:**

1. Peak process memory, measured as PSS or equivalent, SHALL be no more than 800 MB during continuous assistance mode.
2. The application SHALL not trigger OutOfMemoryError during a 30-minute continuous use test.
3. After steady-state operation, memory growth SHALL be no more than 10% over a 30-minute continuous session.
4. The application SHALL release camera buffers and inference buffers when assistance is stopped.
5. The application SHALL respond to OS low-memory signals by unloading optional components first.

**Verification:**  
Memory profiling, soak test, automated memory audit.

**ISO/IEC 25010 mapping:**  
Performance efficiency, reliability.

---

### OR-004 Latency Budget

**Requirement:**  
The application SHALL meet latency thresholds suitable for real-time assistance on low-end hardware.

**Definitions:**

- **Frame timestamp:** Time when camera frame is captured.
- **Detection result time:** Time when object detection output is available.
- **Alert start time:** Time when haptic or audio alert begins.
- **End-to-end alert latency:** Time from frame timestamp to alert start time.

**Acceptance criteria:**

1. Detector inference latency at 320 x 320 input SHALL be no more than 450 ms at P95 on Low-End Reference Devices.
2. End-to-end critical alert latency SHALL be no more than 800 ms at P95 on Low-End Reference Devices.
3. End-to-end critical alert latency SHALL be no more than 1,000 ms at P99.
4. TTS start latency after alert generation SHALL be no more than 500 ms.
5. Haptic start latency after alert generation SHALL be no more than 300 ms.
6. If latency thresholds are exceeded for more than 10 seconds, the orchestration layer SHALL reduce resolution, FPS, or enabled features.

**Verification:**  
Automated performance logging, device benchmark tests.

**ISO/IEC 25010 mapping:**  
Performance efficiency.

---

### OR-005 Adaptive Frame Rate

**Requirement:**  
The application SHALL adapt camera processing rate to context and device capability.

**Acceptance criteria:**

1. When the device is stationary for more than 5 seconds, continuous detection FPS SHALL reduce to 3 FPS or lower.
2. When motion indicates walking, detection FPS SHALL increase within 3 seconds.
3. The system SHALL not require continuous maximum FPS for core obstacle alerts.
4. Frame skipping SHALL not prevent critical alerts if object persistence or motion indicates approaching hazard.
5. Adaptive FPS changes SHALL not cause audible errors or crashes.

**Verification:**  
Sensor simulation test, field test, performance logs.

**ISO/IEC 25010 mapping:**  
Performance efficiency, reliability.

---

### OR-006 Thermal Management

**Requirement:**  
The application SHALL degrade gracefully under thermal constraints.

**Acceptance criteria:**

1. When Android thermal status reaches MODERATE or equivalent, the application SHALL:
   - Reduce continuous FPS by at least 50%.
   - Disable optional heavy models.
2. When thermal status reaches SEVERE or equivalent, the application SHALL:
   - Stop continuous obstacle detection or switch to very low-frequency mode.
   - Notify the user accessibly.
3. The application SHALL NOT ignore thermal signals in Low-End Profile.
4. The application SHALL resume normal operation after temperature improves, with an accessible status message.

**Verification:**  
Thermal simulation, device stress test, inspection.

**ISO/IEC 25010 mapping:**  
Reliability, performance efficiency.

---

### OR-007 Battery Management

**Requirement:**  
The application SHALL respect battery constraints and provide sustainable usage.

**Acceptance criteria:**

1. In standardized continuous Obstacle Awareness Mode, the application SHALL provide at least 3 hours of operation from 100% battery on a Low-End Reference Device with battery capacity of at least 4,000 mAh.
2. When Android Battery Saver is enabled, the application SHALL:
   - Reduce continuous FPS to 3 FPS or lower.
   - Disable optional heavy models.
   - Notify the user that assistance is in reduced mode.
3. The application SHALL warn the user when battery level falls below 15% and assistance is active.
4. The application SHALL stop non-essential background processing when the app is not in active assistance mode.

**Verification:**  
Battery benchmark, power profiling, functional test.

**ISO/IEC 25010 mapping:**  
Performance efficiency, reliability.

---

### OR-008 Confidence, Persistence, and Alert Suppression

**Requirement:**  
The application SHALL avoid unstable or misleading alerts by applying confidence and temporal filtering.

**Acceptance criteria:**

1. A default critical alert SHALL require:
   - Detection confidence of at least 0.60, and
   - Persistence across at least 2 consecutive frames, or
   - One frame with high confidence plus motion evidence indicating likely proximity.
2. Single-frame detections below high-confidence threshold SHALL NOT trigger critical alerts.
3. The system SHALL suppress repeated identical alerts within a default cooldown of 5 seconds unless risk increases.
4. Confidence thresholds SHALL be configurable for QA and accessibility testing but SHALL NOT be hidden from support/debug settings.
5. If confidence is low but a possible hazard is detected, the system MAY use a cautious message such as “Possible obstacle ahead.”

**Verification:**  
Algorithm inspection, log analysis, controlled test routes.

**ISO/IEC 25010 mapping:**  
Functional suitability, reliability, accuracy.

---

### OR-009 Audio and Inference Concurrency

**Requirement:**  
The application SHALL ensure that audio output does not block perception or safety alerts.

**Acceptance criteria:**

1. TTS playback SHALL NOT block camera frame processing.
2. TTS playback SHALL NOT block detector inference.
3. Critical alerts SHALL interrupt ongoing non-critical speech within 300 ms.
4. If TTS engine is unavailable, the application SHALL fall back to non-speech audio cues and display accessible status.
5. Audio queue SHALL prevent overlapping critical messages in a way that makes them unintelligible.

**Verification:**  
Concurrency test, stress test, accessibility test.

**ISO/IEC 25010 mapping:**  
Performance efficiency, usability, reliability.

---

### OR-010 Graceful Degradation

**Requirement:**  
The application SHALL maintain the safest possible available functionality under resource constraints.

**Acceptance criteria:**

1. If detector FPS drops below 3 FPS for more than 10 seconds, the system SHALL:
   - Reduce input resolution, or
   - Reduce model complexity, or
   - Reduce feature set.
2. If camera is unavailable, the app SHALL:
   - Disable camera-dependent modes.
   - Keep settings, help, and previously captured text accessible where possible.
3. If OCR model fails to load, the app SHALL inform the user and keep other modes available.
4. If memory pressure is detected, the system SHALL unload optional models before core models.
5. Degradation events SHALL be announced accessibly.

**Verification:**  
Failure injection test, performance test.

**ISO/IEC 25010 mapping:**  
Reliability, fault tolerance.

---

## 9. UI/UX and Accessibility Requirements

These requirements are mandatory and shall be validated with real users with visual disability whenever possible.

---

### UX-001 Non-Visual-First Interaction

**Requirement:**  
All critical tasks SHALL be fully operable without vision.

**Acceptance criteria:**

1. A user who is blind SHALL be able to complete all critical tasks using screen reader, gestures, and audio/haptic feedback.
2. No critical task SHALL require visual confirmation.
3. No critical task SHALL require reading small text without screen reader support.
4. Critical tasks include:
   - Start assistance.
   - Stop assistance.
   - Pause or mute speech.
   - Change verbosity.
   - Request object summary.
   - Read text.
   - Access help.
   - Report incorrect alert.

**Verification:**  
Screen reader walkthrough, usability testing with users with visual disability.

**ISO/IEC 25010 mapping:**  
Usability, accessibility.

---

### UX-002 Screen Reader Compatibility

**Requirement:**  
The application SHALL be compatible with the platform screen reader.

**Acceptance criteria:**

1. On Android, the application SHALL work with TalkBack.
2. All interactive elements in critical flows SHALL have:
   - Accessible label.
   - Role.
   - State.
   - Focusability where required.
3. Focus order SHALL be logical and predictable.
4. Modal dialogs SHALL trap focus appropriately and return focus correctly.
5. Automated accessibility scans SHALL report zero critical defects on critical flows.
6. Manual accessibility audit SHALL pass with no unresolved high-severity issues.

**Verification:**  
Automated accessibility scanner, manual TalkBack audit.

**ISO/IEC 25010 mapping:**  
Usability, compatibility.

---

### UX-003 Touch Targets and Gestures

**Requirement:**  
The interface SHALL support accessible touch interaction.

**Acceptance criteria:**

1. Interactive controls in critical flows SHALL have minimum touch target size of 48 x 48 dp.
2. Spacing between critical controls SHOULD be at least 8 dp to reduce mis-taps.
3. No critical function SHALL require pinch, multi-finger, or complex gestures as the only method.
4. All swipe-based critical actions SHALL have an alternative button or control.
5. Double-tap activation SHALL be consistent with platform screen reader behavior.

**Verification:**  
UI inspection, accessibility audit, usability test.

**ISO/IEC 25010 mapping:**  
Usability, operability.

---

### UX-004 Audio Design and Cognitive Load

**Requirement:**  
The application SHALL minimize cognitive load and avoid excessive speech.

**Acceptance criteria:**

1. Default obstacle alerts SHALL use short phrases of no more than 6 words.
2. On-demand summaries SHALL default to no more than 25 words unless the user requests more detail.
3. The system SHALL NOT continuously narrate all detected objects when verbosity is set to Minimal.
4. The user SHALL be able to repeat the last message.
5. The user SHALL be able to request more detail where available.
6. The system SHALL NOT produce overlapping speech that obscures critical alerts.

**Verification:**  
Content audit, usability testing, functional test.

**ISO/IEC 25010 mapping:**  
Usability, user error protection.

---

### UX-005 Haptic and Non-Speech Cues

**Requirement:**  
The application SHALL provide non-speech cues to reduce dependency on continuous voice output.

**Acceptance criteria:**

1. Directional cues SHALL be available through haptics or non-speech audio.
2. Critical alerts SHALL be distinguishable from informational messages.
3. Users SHALL be able to choose between:
   - Speech plus haptics.
   - Speech only.
   - Haptics plus minimal speech.
4. The meaning of each haptic pattern SHALL be explained in onboarding or help.

**Verification:**  
Accessibility test, usability test.

**ISO/IEC 25010 mapping:**  
Usability, accessibility.

---

### UX-006 Low Vision Support

**Requirement:**  
For users with residual vision, the application SHALL support basic low-vision accessibility needs.

**Acceptance criteria:**

1. Text SHALL scale with system font size up to at least 200% without loss of core functionality.
2. Critical text and controls SHALL meet contrast ratio of at least:
   - 4.5:1 for normal text.
   - 3:1 for large text and important graphical controls.
3. Color SHALL NOT be the only means of conveying information.
4. The app SHALL support system dark mode where applicable.
5. Important controls SHALL be visually distinguishable without requiring fine detail perception.

**Verification:**  
Contrast analysis, visual audit, accessibility scan.

**ISO/IEC 25010 mapping:**  
Usability, accessibility.

---

### UX-007 Error Prevention and Recovery

**Requirement:**  
The application SHALL prevent, announce, and recover from user and system errors.

**Acceptance criteria:**

1. Destructive actions SHALL require confirmation.
2. Errors SHALL be announced accessibly.
3. Error messages SHALL include:
   - What happened.
   - What the user can do next.
4. The application SHALL NOT rely only on toast messages that disappear too quickly.
5. If assistance stops unexpectedly, the app SHALL provide an accessible recovery path.
6. Settings changes SHALL be reversible.

**Verification:**  
Error-path testing, usability testing.

**ISO/IEC 25010 mapping:**  
Usability, reliability.

---

### UX-008 Learnability and User Testing

**Requirement:**  
The application SHALL be validated with real users with visual disability.

**Acceptance criteria:**

1. Usability testing SHALL include at least 8 participants with visual disability across at least two rounds before release candidate.
2. At least 90% of participants SHALL complete the following critical tasks successfully without sighted assistance:
   - Start assistance.
   - Stop assistance.
   - Change verbosity.
   - Request object summary.
   - Read text.
   - Pause or mute audio.
3. System Usability Scale score SHALL be at least 75.
4. At least 80% of participants SHALL correctly understand after onboarding that the app is a complement, not a replacement.
5. Critical accessibility blockers discovered in user testing SHALL be resolved before release.

**Verification:**  
Usability test reports, accessibility audit, test logs.

**ISO/IEC 25010 mapping:**  
Usability, quality in use.

---

## 10. Non-Functional Requirements Mapped to ISO/IEC 25010

The following requirements are measurable quality requirements aligned with ISO/IEC 25010.

---

## 10.1 Functional Suitability

### NFR-FS-001 Functional Completeness

**Requirement:**  
All mandatory functional requirements in this document SHALL be implemented and verified.

**Acceptance threshold:**

- 100% of MUST/SHALL requirements pass verification before release candidate.

**Verification:**  
Requirements traceability matrix, test report.

---

### NFR-FS-002 Functional Correctness

**Requirement:**  
The system SHALL produce correct and safe feedback under validated conditions.

**Acceptance threshold:**

- Priority object detection recall >= 0.85.
- Priority object detection precision >= 0.75.
- Direction classification accuracy >= 0.90.
- Proximity category accuracy >= 0.80.
- OCR word accuracy >= 80% under standard conditions.

**Verification:**  
Benchmark dataset, controlled tests.

---

### NFR-FS-003 Functional Appropriateness

**Requirement:**  
The system SHALL provide only information appropriate for mobility support and shall avoid misleading certainty.

**Acceptance threshold:**

- No assertive hazard statement shall be generated when confidence is below alert threshold.
- Content audit shall confirm no absolute safety claims.

**Verification:**  
Algorithm inspection, content audit.

---

## 10.2 Performance Efficiency

### NFR-PE-001 Cold Start

**Requirement:**  
The application SHALL start quickly on low-end hardware.

**Acceptance threshold:**

- Time from app icon activation to accessible home screen SHALL be no more than 5 seconds on Low-End Reference Devices.
- Camera analysis ready time SHALL be no more than 3 seconds after user starts assistance.

**Verification:**  
Performance test.

---

### NFR-PE-002 Inference Latency

**Requirement:**  
The application SHALL meet inference and alert latency budgets.

**Acceptance threshold:**

- Detector inference P95 <= 450 ms at 320 x 320.
- End-to-end alert P95 <= 800 ms.
- End-to-end alert P99 <= 1,000 ms.

**Verification:**  
Automated benchmark.

---

### NFR-PE-003 Resource Utilization

**Requirement:**  
The application SHALL remain within memory and storage limits.

**Acceptance threshold:**

- Peak process memory <= 800 MB on Low-End Reference Devices.
- Core ML assets <= 100 MB.
- Total installed app size including base app and core assets <= 250 MB.
- No OutOfMemoryError during 30-minute continuous session.

**Verification:**  
Memory profiling, package inspection.

---

### NFR-PE-004 Battery Efficiency

**Requirement:**  
The application SHALL provide reasonable continuous use.

**Acceptance threshold:**

- At least 3 hours of continuous Obstacle Awareness Mode on a Low-End Reference Device with battery >= 4,000 mAh.
- Battery saver mode SHALL reduce processing and notify user.

**Verification:**  
Battery benchmark.

---

### NFR-PE-005 Thermal Behavior

**Requirement:**  
The application SHALL reduce load before severe overheating.

**Acceptance threshold:**

- At MODERATE thermal status, reduce FPS by >= 50%.
- At SEVERE thermal status, stop continuous heavy inference and notify user.
- No crash caused by thermal degradation path during stress test.

**Verification:**  
Stress test, thermal simulation.

---

## 10.3 Compatibility

### NFR-COMP-001 Operating System Compatibility

**Requirement:**  
The application SHALL support target Android versions.

**Acceptance threshold:**

- Android 11 or higher supported.
- At least three OS versions tested if available, including the minimum supported version and the latest stable version used in test devices.

**Verification:**  
Device matrix testing.

---

### NFR-COMP-002 Assistive Technology Compatibility

**Requirement:**  
The application SHALL work with screen readers and accessibility services.

**Acceptance threshold:**

- TalkBack compatibility verified for all critical flows.
- No critical function blocked by TalkBack.
- Zero critical accessibility defects in automated scan.
- Manual accessibility audit passed.

**Verification:**  
Accessibility audit.

---

### NFR-COMP-003 Audio Output Compatibility

**Requirement:**  
The application SHALL support common audio routes.

**Acceptance threshold:**

- Audio output works through:
  - Built-in speaker.
  - Wired headphones.
  - Bluetooth audio device.
- Critical alerts still produce haptic feedback where supported, even if Bluetooth audio latency exists.

**Verification:**  
Device matrix test.

---

## 10.4 Usability

### NFR-USE-001 Effectiveness

**Requirement:**  
Users with visual disability SHALL be able to complete critical tasks successfully.

**Acceptance threshold:**

- Task success rate >= 90% across critical tasks in usability testing.

**Verification:**  
Usability testing.

---

### NFR-USE-002 Efficiency

**Requirement:**  
Critical actions SHALL be reachable quickly.

**Acceptance threshold:**

- Start/stop assistance reachable within two gestures from home screen.
- Pause or mute reachable within one gesture from active assistance screen.
- Settings reachable within three gestures from home screen.

**Verification:**  
Interaction audit.

---

### NFR-USE-003 Satisfaction

**Requirement:**  
Users SHALL find the application acceptable and understandable.

**Acceptance threshold:**

- System Usability Scale score >= 75.
- At least 80% of users correctly understand the app’s complementary role after onboarding.

**Verification:**  
User survey, comprehension check.

---

### NFR-USE-004 Error Protection

**Requirement:**  
The system SHALL reduce user errors and support recovery.

**Acceptance threshold:**

- No critical task fails silently.
- 100% of critical error states provide accessible recovery guidance.

**Verification:**  
Error-path testing.

---

## 10.5 Reliability

### NFR-REL-001 Maturity

**Requirement:**  
The application SHALL be stable under normal and stress conditions.

**Acceptance threshold:**

- Crash-free session rate >= 99.5% during beta release.
- No unrecoverable crash during an 8-hour soak test across at least two Low-End Reference Devices.

**Verification:**  
Crash reporting, soak test.

---

### NFR-REL-002 Fault Tolerance

**Requirement:**  
The application SHALL handle failures without losing user safety awareness.

**Acceptance threshold:**

- If detector fails, app informs user and disables only affected camera-dependent features.
- If OCR fails, app informs user and keeps other modes available.
- If TTS fails, app uses fallback audio cue and reports issue.
- If process death occurs, settings persist and app restores to safe state.

**Verification:**  
Failure injection test.

---

### NFR-REL-003 Recoverability

**Requirement:**  
The application SHALL recover from interruptions.

**Acceptance threshold:**

- Incoming phone call does not corrupt app state.
- Screen off/on during assistance returns to expected state or informs user.
- Low-memory kill does not prevent restart into accessible home state.

**Verification:**  
Interruption test.

---

## 10.6 Security and Privacy

### NFR-SEC-001 Local Processing by Default

**Requirement:**  
Core assistance SHALL process camera data locally by default.

**Acceptance threshold:**

- No camera frame leaves the device unless optional cloud intelligence is explicitly enabled.
- Network traffic inspection in offline core mode SHALL show no image upload.

**Verification:**  
Network monitoring, privacy audit.

---

### NFR-SEC-002 Consent and Transparency

**Requirement:**  
The application SHALL obtain clear consent for optional data sharing.

**Acceptance threshold:**

- Cloud features require explicit opt-in.
- Analytics/crash reporting requires explicit opt-in if it includes non-essential data.
- No raw images are uploaded for feedback without explicit user action.

**Verification:**  
Inspection, privacy test.

---

### NFR-SEC-003 Data Minimization

**Requirement:**  
The application SHALL collect only necessary data.

**Acceptance threshold:**

- Logs SHALL NOT contain raw camera images.
- Logs SHALL NOT contain audio recordings.
- Telemetry SHALL be anonymized where possible.
- User settings SHALL be stored locally.

**Verification:**  
Log audit, code inspection.

---

### NFR-SEC-004 Secure Storage and Transmission

**Requirement:**  
If any network feature is enabled, data transmission SHALL be protected.

**Acceptance threshold:**

- All network communication SHALL use TLS.
- Local secrets, if any, SHALL use platform secure storage.
- Static application security testing SHALL show no critical or high severity findings unresolved at release.

**Verification:**  
Security scan, penetration test where applicable.

---

## 10.7 Maintainability

### NFR-MAINT-001 Modularity

**Requirement:**  
The system SHALL separate orchestration, inference, UI, audio, and accessibility services.

**Acceptance threshold:**

- Core orchestration logic is isolated from UI code.
- Model adapters can be replaced without modifying UI layer.
- Architecture review confirms separation of concerns.

**Verification:**  
Code review, architecture inspection.

---

### NFR-MAINT-002 Testability

**Requirement:**  
Critical orchestration logic SHALL be automated testable.

**Acceptance threshold:**

- Unit and integration test coverage for orchestration modules >= 75%.
- Critical resource-management rules have automated regression tests.

**Verification:**  
Coverage report, test suite.

---

### NFR-MAINT-003 Versioning and Traceability

**Requirement:**  
Models, prompts/templates, and configuration SHALL be versioned.

**Acceptance threshold:**

- Each released model has version, checksum, and evaluation report.
- Each requirement has traceability to test cases.
- Template messages are localized and version-controlled.

**Verification:**  
Repository audit, release checklist.

---

### NFR-MAINT-004 Safe Logging

**Requirement:**  
Logs SHALL support debugging without harming privacy.

**Acceptance threshold:**

- Logs may include performance metrics, model latency, memory state, and error codes.
- Logs SHALL NOT include camera frames, audio recordings, or personally identifiable information unless user explicitly opts into debug sharing.

**Verification:**  
Log inspection.

---

## 10.8 Portability

### NFR-PORT-001 Device Portability

**Requirement:**  
The application SHALL run across target device architectures.

**Acceptance threshold:**

- arm64-v8a support mandatory.
- armeabi-v7a support optional if needed for specific markets.
- App SHALL not depend on a single vendor NPU SDK without CPU fallback.

**Verification:**  
Build inspection, device matrix test.

---

### NFR-PORT-002 Installability

**Requirement:**  
The application SHALL be installable within reasonable storage limits.

**Acceptance threshold:**

- Base install size including core app and core assets <= 250 MB.
- Optional large models are downloadable separately and not required for core functionality.

**Verification:**  
Package inspection.

---

### NFR-PORT-003 Runtime Adaptability

**Requirement:**  
The application SHALL adapt to different hardware capabilities.

**Acceptance threshold:**

- CPU fallback available for all mandatory inference features.
- GPU/NPU acceleration optional and only enabled after benchmark validation.
- App SHALL NOT crash when NPU delegate is unavailable.

**Verification:**  
Device matrix test.

---

## 11. Safety, Limitations, and Ethical Requirements

### SAF-001 Complement-Only Positioning

**Requirement:**  
All user-facing materials SHALL present the application as a complement to existing mobility methods.

**Acceptance criteria:**

1. Onboarding, help, app store description, and marketing materials SHALL state that the app does not replace:
   - White cane.
   - Guide dog.
   - Human guide.
   - Orientation and mobility training.
2. The product SHALL NOT claim to prevent all accidents.
3. The product SHALL NOT claim to detect every obstacle.

**Verification:**  
Content audit.

---

### SAF-002 Uncertainty Communication

**Requirement:**  
The system SHALL communicate uncertainty instead of false certainty.

**Acceptance criteria:**

1. If detection confidence is below alert threshold, the system SHALL NOT make assertive hazard statements.
2. If the user requests a description and confidence is low, the system SHALL say it is not sure or provide only high-confidence information.
3. The system SHALL distinguish between detected objects and inferred context.

**Verification:**  
Algorithm inspection, controlled low-confidence test cases.

---

### SAF-003 Known Limitations Disclosure

**Requirement:**  
The application SHALL disclose known limitations.

**Acceptance criteria:**

Help and onboarding SHALL mention possible difficulty with:

1. Transparent surfaces such as glass.
2. Low light.
3. Highly reflective surfaces.
4. Small or partially hidden obstacles.
5. Fast-moving objects.
6. Overhead obstacles outside camera view.
7. Wet or irregular ground surfaces.

**Verification:**  
Content audit.

---

### SAF-004 No Autonomous Navigation Claims

**Requirement:**  
The application SHALL NOT provide autonomous navigation unless separately validated.

**Acceptance criteria:**

1. Core version SHALL NOT instruct the user to move into hazardous paths.
2. Core version SHALL NOT claim to guide the user through unknown environments safely.
3. If directional information is given, it SHALL be advisory and concise.

**Verification:**  
Content audit, functional test.

---

### SAF-005 User Training Recommendation

**Requirement:**  
The application SHALL recommend appropriate training and responsible use.

**Acceptance criteria:**

1. Onboarding SHALL recommend that users practice in safe environments first.
2. Help content SHALL advise users not to rely solely on the app in high-risk environments.
3. The app SHALL encourage consultation with orientation and mobility professionals where appropriate.

**Verification:**  
Content audit.

---

## 12. Model and Validation Data Requirements

### DAT-001 Validation Dataset

**Requirement:**  
The project SHALL maintain a validation dataset representative of target use conditions.

**Acceptance criteria:**

The dataset SHALL include at least:

1. 1,000 annotated images.
2. 20 recorded walking sequences totaling at least 30 minutes.
3. Indoor and outdoor scenes.
4. Low-light samples.
5. Cluttered environments.
6. Partially occluded objects.
7. Different camera angles corresponding to hand-held and chest-level use.

**Verification:**  
Dataset audit.

---

### DAT-002 Annotation Schema

**Requirement:**  
Annotations SHALL support measurable evaluation.

**Acceptance criteria:**

Each annotated object SHALL include:

1. Object class.
2. Bounding box.
3. Horizontal sector: left, center, right.
4. Proximity category: near, medium, far.
5. Criticality flag.
6. Occlusion flag where applicable.

**Verification:**  
Dataset schema inspection.

---

### DAT-003 Reproducible Evaluation

**Requirement:**  
Model evaluation SHALL be reproducible.

**Acceptance criteria:**

1. Each model release SHALL include:
   - Model version.
   - Dataset version/hash.
   - Evaluation script version.
   - Metric results.
2. Evaluation metrics SHALL include:
   - mAP@0.5.
   - Recall.
   - Precision.
   - False alert rate.
   - Direction accuracy.
   - Proximity accuracy.
   - Latency.
   - Memory usage.

**Verification:**  
Release audit.

---

## 13. Acceptance Gates and Release Criteria

The product SHALL NOT be considered release-ready unless all of the following gates are passed.

### Gate 1: Functional Completeness

- 100% of mandatory functional requirements pass verification.
- No known critical functional defect unresolved.

### Gate 2: Accessibility

- Zero critical automated accessibility defects.
- Manual accessibility audit passed.
- Critical tasks are fully operable with screen reader.
- Accessibility testing with users with visual disability completed.

### Gate 3: Low-End Performance

- All performance thresholds pass on all designated Low-End Reference Devices.
- No OutOfMemoryError in 30-minute continuous test.
- Thermal and battery degradation behavior verified.

### Gate 4: Model Quality

- Detection, direction, proximity, and OCR thresholds pass.
- False alert rate within defined limits.
- Uncertainty behavior verified.

### Gate 5: Safety and Ethics

- Disclaimer and limitation messaging approved.
- No unsafe or misleading claims.
- Privacy defaults verified.

### Gate 6: Reliability

- Crash-free session threshold met.
- Soak test passed.
- Failure injection tests passed.

### Gate 7: User Validation

- At least 8 users with visual disability participated in validation.
- Task success rate >= 90%.
- SUS score >= 75.
- No unresolved critical usability or accessibility blocker.

---

## 14. Verification Methods

The following verification methods shall be used:

| Method | Meaning |
|---|---|
| Inspection | Review of code, content, configuration, or documentation. |
| Analysis | Static analysis, log analysis, memory analysis, network analysis. |
| Demonstration | Observable behavior under controlled conditions. |
| Test | Automated or manual execution of defined test cases. |
| User Testing | Validation with real users with visual disability. |
| Benchmark | Quantitative measurement using defined datasets and devices. |

Each requirement should be traceable to at least one verification method before release.

---

## 15. Traceability Summary to ISO/IEC 25010

| ISO/IEC 25010 Characteristic | Related Requirements |
|---|---|
| Functional suitability | FR-005, FR-006, FR-007, FR-009, NFR-FS-001, NFR-FS-002 |
| Performance efficiency | OR-002, OR-003, OR-004, OR-005, OR-006, OR-007, NFR-PE-001 to NFR-PE-005 |
| Compatibility | NFR-COMP-001, NFR-COMP-002, NFR-COMP-003, FR-010 |
| Usability | FR-001, FR-002, FR-012, UX-001 to UX-008, NFR-USE-001 to NFR-USE-004 |
| Reliability | OR-010, NFR-REL-001 to NFR-REL-003, FR-015 |
| Security | FR-014, FR-016, NFR-SEC-001 to NFR-SEC-004 |
| Maintainability | NFR-MAINT-001 to NFR-MAINT-004, DAT-003 |
| Portability | NFR-PORT-001 to NFR-PORT-003, OR-001 |

---

## 16. Open Questions for Product Validation

The following questions must be resolved with product, accessibility, and mobility experts before final approval:

1. Which languages are required for first release?
2. Which target countries and regulatory constraints apply?
3. Should the app support external wearables in a future phase?
4. What is the minimum supported Android version for the target market?
5. Which device models will be officially certified as Low-End Reference Devices?
6. Should stairs/elevator detection be included in the first release or deferred?
7. What is the acceptable support policy for devices below 4 GB RAM?
8. Should cloud-based description be included in the first release at all?

---

## 17. Approval

| Role | Name | Signature | Date |
|---|---:|---:|---:|
| Product Owner |  |  |  |
| Accessibility Lead |  |  |  |
| Engineering Lead |  |  |  |
| QA Lead |  |  |  |
| User Research Lead |  |  |  |
| Safety/Compliance Reviewer |  |  |  |
