# ARCHITECTURE.md

## Architecture and Technology Stack  
### Accessible Vision and Audio Assistance Mobile Application

> **Executive summary:** Technical architecture (v1.0) for the Android-native, Kotlin-first assistant. Layered modular system with an Orchestration Core as the center of control (mode state machine, resource governor, model lifecycle, frame scheduling, alert policy, degradation engine, feedback priority). Approved stack: Android Views + ViewBinding, Hilt, CameraX (latest-frame-only, 640×480 analysis, 320×320 detector input), LiteRT/TFLite CPU/XNNPACK with YOLOv8n INT8, ML Kit OCR v2, system TTS + SoundPool + VibrationEffect, DataStore, optional C++17 NDK hot paths and ONNX fallback. Modules: app, core, feature, perception, inference, feedback, data, benchmark, tools, with strict dependency rules. Perf budgets: inference P95 ≤ 450 ms, end-to-end alert ≤ 800 ms, preprocess ≤ 80 ms. Memory budget ≤ 800 MB; degradation ladder L0-L3 for thermal/battery/memory/latency pressure. 14 ADRs document key decisions (native Android, Kotlin, Views, LiteRT, YOLOv8n INT8, ML Kit, templates, no continuous VLM on low-end, orchestration as core value); ADR-010 proposes detector + optional relative-depth fusion for proximity (Proposed).

**Version:** 1.0  
**Status:** Proposed for technical validation  
**Primary platform:** Android  
**Primary target:** Low-end smartphones with 4 GB RAM and entry-level SoCs  
**Key constraints:** Performance, memory, thermal limits, accessibility, offline operation, safety communication  

---

## 1. Purpose

This document defines the architecture and technology stack for the accessible assistance application described in `requirements.md`.

The system must support people with visual disability by providing:

- Obstacle awareness.
- Basic object identification.
- On-demand scene summary.
- Text reading.
- Audio and haptic feedback.
- Full offline core assistance.
- Resource-aware orchestration on low-end mobile hardware.

The architecture is optimized for:

1. Accessibility-first interaction.
2. Low-latency perception where possible.
3. Strict memory and thermal control.
4. Deterministic behavior under resource constraints.
5. Safe, concise, non-overwhelming user feedback.
6. Maintainability and testability.

The application is a complement to a white cane, guide dog, human guide, or mobility training. It is not a replacement for those methods.

---

## 2. Architectural Principles

### 2.1 Accessibility-first

The UI and feedback system must be usable without vision. Accessibility is not a feature layer; it is a core architectural constraint.

Implications:

- All state changes must be announced accessibly.
- All critical controls must be reachable by screen reader.
- Audio and haptics are first-class output channels.
- UI must avoid visual-only indicators.
- Focus management must be deterministic.

### 2.2 Low-end-first

The system must assume constrained CPU, RAM, thermal headroom, and battery.

Implications:

- Use small quantized models.
- Avoid continuous heavy inference.
- Use event-driven and adaptive frame processing.
- Load and unload models dynamically.
- Prefer deterministic template-based language over heavy generative models.
- Design degradation paths as core features.

### 2.3 Offline-first

Core assistance must work without internet.

Implications:

- Detection and OCR must be available on-device.
- Text-to-speech must use local system TTS or local fallback.
- Optional cloud intelligence must be disabled by default.
- No raw camera frames may leave the device unless explicitly enabled.

### 2.4 Safety-aware orchestration

The system must not overpromise. It must handle uncertainty and resource degradation transparently.

Implications:

- Alerts must be confidence-gated.
- Low-confidence outputs must use cautious language.
- Thermal, memory, and battery states must affect behavior.
- The user must be informed when assistance is degraded.

### 2.5 Modular and testable

The system must be separated into clear modules:

- UI/accessibility.
- Orchestration.
- Perception.
- Inference adapters.
- Feedback.
- Settings.
- Telemetry/diagnostics.

This allows independent testing and future replacement of models or runtimes.

---

## 3. Platform Decision

### 3.1 Selected platform

**Android native first.**

Android is selected as the initial platform because:

- Broad global device coverage, including low-end devices.
- Direct control over camera, inference runtimes, TTS, haptics, and accessibility services.
- Mature support for TalkBack.
- Ability to optimize performance with AndroidX, CameraX, LiteRT/ONNX, and native code if necessary.
- Easier support for emerging-market hardware profiles.

iOS may be considered later, but it is not in the initial architectural scope.

### 3.2 Why not cross-platform UI frameworks

Frameworks such as Flutter, React Native, or other cross-platform UI layers are not selected for version 1.

Reasons:

- Accessibility behavior must be highly predictable with TalkBack.
- Native Android accessibility APIs provide more direct control.
- Camera and inference performance require tight integration.
- Low-end device behavior is easier to diagnose with native tooling.
- Additional abstraction layers increase risk in a safety-sensitive assistive product.

If future cross-platform exploration occurs, it must pass strict accessibility and performance benchmarks before adoption.

---

## 4. Programming Languages

### 4.1 Primary application language

**Kotlin**

Kotlin is the primary language for:

- Android application code.
- UI and accessibility behavior.
- Orchestration logic.
- ViewModels and state management.
- Kotlin coroutines and flows.
- Integration with CameraX, LiteRT, ML Kit, TTS, and haptics.

Reasons:

- Official Android language.
- Excellent coroutine support for asynchronous pipelines.
- Strong AndroidX ecosystem.
- Good maintainability.
- Safe null handling and concise domain modeling.

### 4.2 Native performance language

**C++17 via Android NDK**, optional and isolated.

C++ is not mandatory for version 1, but the architecture allows a native module for:

- Frame preprocessing.
- Color conversion.
- Detection post-processing.
- Custom haptic/audio low-latency paths if profiling requires it.

C++ shall only be introduced where profiling proves that Kotlin/JVM overhead violates the performance budget.

The default implementation should remain Kotlin-first for maintainability.

### 4.3 Offline tooling language

**Python 3.11+**

Python is used outside the mobile runtime for:

- Model training and fine-tuning.
- Quantization and conversion.
- Dataset validation.
- Benchmark scripts.
- Metric evaluation.
- Model registry generation.

Python shall not be used inside the production mobile app runtime.

### 4.4 Scripting

**Bash** or **PowerShell** may be used for local automation, CI tasks, and benchmark orchestration.

### 4.5 Language decision summary

| Concern | Selected Language | Notes |
|---|---:|---|
| Android app | Kotlin | Primary production language. |
| UI/accessibility | Kotlin | Native Android UI. |
| Orchestration | Kotlin | Coroutines and state machines. |
| Native hot paths | C++17 | Optional, profile-driven. |
| Model tooling | Python | Offline training/evaluation/conversion. |
| Build automation | Gradle Kotlin DSL | Android build system. |

---

## 5. High-Level Architecture

The architecture is a modular, layered, event-driven system with unidirectional data flow for perception and feedback.

```text
┌─────────────────────────────────────────────────────────────┐
│                        Presentation Layer                   │
│  Onboarding, Home, Assistance Screen, Settings, Help        │
│  Accessibility semantics, focus management, large controls  │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                      Application Services                   │
│  ModeController, SessionManager, UserPreferences,           │
│  AccessibilityAnnouncer, NavigationController               │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                       Orchestration Core                    │
│  State machine, Resource Governor, Priority Scheduler,      │
│  Alert Policy, Model Lifecycle Manager, Degradation Engine  │
└──────┬───────────────┬───────────────┬──────────────┬───────┘
       │               │               │              │
┌──────▼─────┐  ┌──────▼─────┐  ┌──────▼─────┐ ┌──────▼─────┐
│ Perception │  │  Inference │  │  Feedback  │ │ Diagnostics│
│  Pipeline  │  │  Adapters  │  │  Pipeline  │ │ Telemetry  │
│            │  │            │  │            │ │ Benchmarks │
│ CameraX    │  │ Detector   │  │ TTS        │ │ Logs       │
│ FrameGate  │  │ OCR        │  │ Earcons    │ │ Metrics    │
│ Preprocess │  │ Optional   │  │ Haptics    │ │ Privacy    │
│ Motion     │  │ VLM/cloud  │  │ Queue      │ │ Safe logs  │
└────────────┘  └────────────┘  └────────────┘ └────────────┘
```

The Orchestration Core is the center of the system. It decides:

- Which mode is active.
- Which models are loaded.
- How many frames per second are processed.
- When to degrade functionality.
- Which alerts are emitted.
- Which feedback channel is used.
- How to respond to thermal, battery, memory, and latency pressure.

---

## 6. Technology Stack

### 6.1 Android baseline

| Component | Selection |
|---|---|
| Minimum SDK | Android 11, API 30 |
| Target SDK | Latest stable Android API supported by tooling |
| Build system | Gradle Kotlin DSL |
| JDK | JDK 17 |
| Language | Kotlin |
| UI toolkit | Android View system with ViewBinding |
| Architecture pattern | Modular layered architecture with unidirectional data flow |
| Asynchrony | Kotlin Coroutines and Flow |
| Dependency injection | Hilt |
| Local settings storage | Jetpack DataStore Preferences |
| Navigation | AndroidX Navigation |
| Linting | Android Lint, Detekt, ktlint |
| Minification | R8 |
| Startup optimization | Baseline Profiles |

### 6.2 UI toolkit decision

The application will use the classic Android View system for version 1.

Reasons:

- Predictable TalkBack focus behavior.
- Mature accessibility support on older Android versions.
- Lower risk on low-end devices.
- Easier audit of focus, labels, roles, and states.
- Less runtime overhead for very simple screens.

Jetpack Compose may be evaluated later, but only after rigorous accessibility and low-end performance testing.

### 6.3 Camera stack

| Component | Selection |
|---|---|
| Camera API | CameraX |
| Image analysis | ImageAnalysis use case |
| Default analysis resolution | 640 x 480 |
| Detector input resolution | 320 x 320, or 256 x 256 in degraded mode |
| Frame strategy | Latest-frame-only, drop stale frames |
| Image format handling | YUV optimized conversion, avoid unnecessary Bitmap creation |

CameraX is selected because it provides:

- Consistent behavior across devices.
- Lifecycle awareness.
- Easier integration with image analysis.
- Good support for low-level camera configuration.

### 6.4 Inference runtime

Primary runtime:

**LiteRT / TensorFlow Lite with CPU delegation via XNNPACK.**

Secondary optional runtime:

**ONNX Runtime Mobile**, only if detector export or quantization quality proves significantly better.

Default decision:

| Runtime | Use |
|---|---|
| LiteRT | Primary detector runtime. |
| LiteRT / ML Kit internals | OCR where applicable. |
| ONNX Runtime | Fallback or future alternative, not default. |
| Vendor NPU SDKs | Optional, disabled by default until benchmarked. |

Reasons:

- Lightweight mobile footprint.
- Strong Android integration.
- Good support for INT8 models.
- CPU path is more predictable across low-end devices.
- Avoids unstable low-end NPU behavior.

### 6.5 Vision models

| Model | Selected approach |
|---|---|
| Object detection | YOLOv8n, quantized INT8 TFLite/LiteRT. |
| Detection input | 320 x 320 default. |
| Detection classes | Priority mobility classes only. |
| OCR | ML Kit Text Recognition v2, offline model. |
| Depth | Not used by default; optional relative-depth model only after ADR-010 quality gate. |
| VLM | Disabled by default on low-end. Optional cloud or high-end only. |

Detector policy:

- Confidence threshold default: 0.60.
- NMS IoU threshold: 0.45.
- Maximum detections per frame: 10.
- Temporal persistence required before critical alert.
- Direction sectors: left, center, right.
- Proximity categories: near, medium, far.

### 6.6 Text-to-speech

| Component | Selection |
|---|---|
| Speech synthesis | Android TextToSpeech API |
| Voice source | User’s system TTS engine |
| Fallback | Non-speech earcons and haptics |
| Optional future offline voice | Piper or similar, only if system TTS is insufficient |

Reasons:

- System TTS integrates with user accessibility setup.
- Lower memory footprint than bundling a neural TTS engine by default.
- Users with screen readers often already have preferred TTS engines.

### 6.7 Audio and haptics

| Component | Selection |
|---|---|
| Speech | Android TextToSpeech |
| Non-speech cues | SoundPool with preloaded short earcons |
| Low-latency audio fallback | Oboe/AAudio only if SoundPool fails latency tests |
| Haptics | Android Vibrator and VibrationEffect |
| Audio focus | Android AudioManager with accessibility-aware focus handling |

Haptics are mandatory for critical alerts where supported, because Bluetooth audio latency may reduce audio immediacy.

### 6.8 OCR stack

Primary:

**ML Kit Text Recognition v2.**

Reasons:

- Good accuracy for common printed text.
- On-device operation.
- Optimized for Android.
- Lower engineering risk for MVP.

Fallback/future:

**PaddleOCR mobile** or an ONNX-based OCR model behind an OCR abstraction layer.

The architecture must allow replacement of OCR implementation without changing UI or orchestration logic.

---

## 7. Module Structure

The repository should be modularized as follows:

```text
/
├── app/
│   ├── Application class
│   ├── DI wiring
│   ├── Navigation host
│   └── App-level configuration
│
├── core/
│   ├── common/
│   ├── domain/
│   ├── orchestration/
│   └── state/
│
├── feature/
│   ├── onboarding/
│   ├── home/
│   ├── assistance/
│   ├── settings/
│   ├── help/
│   └── feedback/
│
├── perception/
│   ├── camera/
│   ├── framegate/
│   ├── preprocess/
│   ├── motion/
│   ├── detection/
│   └── ocr/
│
├── inference/
│   ├── runtime-api/
│   ├── litert/
│   ├── onnx-optional/
│   └── model-manager/
│
├── feedback/
│   ├── tts/
│   ├── earcons/
│   ├── haptics/
│   └── queue/
│
├── data/
│   ├── settings/
│   ├── telemetry/
│   └── diagnostics/
│
├── benchmark/
│   ├── macrobenchmark/
│   ├── model-eval/
│   └── device-matrix/
│
└── tools/
    ├── model_conversion/
    ├── dataset_validation/
    └── scripts/
```

### 7.1 Dependency rules

1. `feature` modules depend on `core` interfaces, not directly on inference implementations.
2. `perception` modules depend on domain models and runtime APIs.
3. `inference` implementations are injected by the app module.
4. `feedback` modules do not know about vision models.
5. `orchestration` coordinates perception, inference, and feedback.
6. No module may access camera, model files, or TTS directly except through its designated layer.

Enforced by `core` unit test `ModuleBoundaryTest` (allowlist):

| Module | May depend on |
|---|---|
| `core` | — |
| `feature` | `core`, `data` |
| `perception` | `core` |
| `inference` | `core`, `perception` |
| `feedback` | `core` |
| `data` | `core` |
| `benchmark` | `core` |
| `app` | `core`, `feature`, `data`, `perception`, `inference`, `feedback`, `benchmark` |

---

## 8. Core Domain Model

### 8.1 Main states

```text
Idle
Starting
ObstacleAssistanceActive
ObjectQueryActive
TextReadingActive
Degraded
Error
Stopping
```

### 8.2 State transitions

```text
Idle -> Starting
Starting -> ObstacleAssistanceActive
ObstacleAssistanceActive -> ObjectQueryActive
ObjectQueryActive -> ObstacleAssistanceActive
ObstacleAssistanceActive -> TextReadingActive
TextReadingActive -> ObstacleAssistanceActive
ObstacleAssistanceActive -> Degraded
Degraded -> ObstacleAssistanceActive
Degraded -> Error
AnyActiveState -> Stopping
Stopping -> Idle
```

### 8.3 Core entities

```kotlin
data class Detection(
    val id: String,
    val classId: Int,
    val label: String,
    val confidence: Float,
    val boundingBox: BoundingBox,
    val sector: HorizontalSector,
    val proximity: Proximity,
    val timestamp: Long,
    val sourceModel: ModelVersion
)

enum class HorizontalSector {
    LEFT, CENTER, RIGHT
}

enum class Proximity {
    NEAR, MEDIUM, FAR, UNKNOWN
}

data class Alert(
    val id: String,
    val priority: AlertPriority,
    val type: AlertType,
    val message: String,
    val earcon: EarconType?,
    val hapticPattern: HapticPattern?,
    val createdAt: Long
)

enum class AlertPriority {
    CRITICAL_OBSTACLE,
    USER_REQUESTED,
    OCR_RESULT,
    STATUS,
    DEBUG
}
```

---

## 9. Orchestration Architecture

The Orchestration Core is the most important component for low-end devices.

It owns:

- Mode state machine.
- Resource governance.
- Model lifecycle.
- Frame scheduling.
- Alert policy.
- Degradation rules.
- Feedback prioritization.

### 9.1 Orchestration components

| Component | Responsibility |
|---|---|
| ModeController | Manages active mode and transitions. |
| ResourceGovernor | Monitors thermal, battery, memory, FPS, latency. |
| ModelLifecycleManager | Loads/unloads detector and OCR models. |
| FrameScheduler | Decides whether a frame should be processed. |
| AlertPolicy | Converts stable detections into alerts. |
| FeedbackPriorityQueue | Orders feedback and handles interruption. |
| DegradationEngine | Applies reduced-functionality policies. |

### 9.2 Resource governor inputs

The governor monitors:

1. Thermal status.
2. Battery level.
3. Battery saver state.
4. Memory pressure.
5. Frame latency.
6. Inference latency.
7. Device profile.
8. Camera availability.
9. Model loading state.

### 9.3 Resource governor outputs

The governor can:

1. Reduce FPS.
2. Reduce detector input resolution.
3. Disable optional models.
4. Unload OCR after idle.
5. Switch to degraded mode.
6. Notify the user.
7. Pause non-essential telemetry.
8. Force conservative alert thresholds.

### 9.4 Degradation ladder

```text
Level 0: Normal low-end mode
- Detector 320x320
- Adaptive FPS up to 8
- OCR on demand
- Template-based object summary

Level 1: Reduced mode
- Detector 256x256
- FPS reduced by 50%
- OCR still available on demand
- Optional features disabled

Level 2: Minimal mode
- Very low FPS
- Detector may run only on motion triggers
- OCR available but slower
- User notified

Level 3: Critical degradation
- Continuous detection stopped
- Manual capture and OCR only if possible
- User clearly informed assistance is limited
```

### 9.5 Policy thresholds

Default thresholds must be configurable for QA but not hidden from diagnostics.

| Parameter | Default |
|---|---:|
| Detector confidence threshold | 0.60 |
| Minimum frames for stable alert | 2 |
| Alert cooldown for same object | 5 seconds (NORMAL/MINIMAL); 1.5 seconds in DETAILED (user-tuned) |
| OCR idle unload timeout | 30 seconds |
| Thermal moderate action | Reduce FPS >= 50% |
| Thermal severe action | Stop continuous detection |
| Memory pressure action | Unload optional components |
| Battery saver action | Reduce FPS to <= 3 |

---

## 10. Perception Pipeline

### 10.1 Continuous obstacle pipeline

```text
CameraX ImageAnalysis
        │
        ▼
FrameGate
  - keep latest frame only
  - drop stale frames
        │
        ▼
Preprocessor
  - downscale to detector input
  - normalize if needed
  - avoid Bitmap allocation when possible
        │
        ▼
Motion/Context Filter
  - stationary: reduce processing
  - walking: increase processing
        │
        ▼
InferenceScheduler
  - check model loaded
  - check resource budget
  - check thermal/memory state
        │
        ▼
Detector Runtime
  - LiteRT INT8 model
        │
        ▼
PostProcessor
  - NMS
  - sector calculation
  - proximity estimation
        │
        ▼
TemporalFusion
  - track objects over time
  - suppress one-frame false positives
        │
        ▼
AlertPolicy
  - confidence gating
  - cooldown
  - priority assignment
        │
        ▼
FeedbackDispatcher
  - haptic
  - earcon
  - TTS
```

### 10.2 Frame handling rules

1. Never queue all camera frames.
2. Use a conflated/latest-only channel for frames.
3. If inference is busy, drop frames.
4. Keep only one active inference request for continuous detection.
5. Avoid allocating new tensors or Bitmaps per frame.
6. Reuse ByteBuffer or tensor buffers where possible.

### 10.3 Preprocessing strategy

Preferred path:

- Use ImageProxy from CameraX.
- Convert YUV to model input only when needed.
- Use optimized conversion.
- If Kotlin conversion exceeds budget, move to C++ with libyuv.

Preprocessing budget:

- Target: <= 80 ms P95.
- Hard limit: <= 120 ms P95.

If exceeded:

- Reduce resolution.
- Reduce FPS.
- Enable native preprocessing if available.

---

## 11. Inference Architecture

### 11.1 Detector interface

```kotlin
interface ObjectDetector {
    suspend fun load(modelConfig: ModelConfig): Result<Unit>
    suspend fun detect(frame: PerceptionFrame): Result<List<Detection>>
    fun isLoaded(): Boolean
    suspend fun unload()
}
```

### 11.2 OCR interface

```kotlin
interface TextRecognizer {
    suspend fun load(languagePack: LanguagePack): Result<Unit>
    suspend fun recognize(image: TextCapture): Result<RecognizedText>
    fun isLoaded(): Boolean
    suspend fun unload()
}
```

### 11.3 Model manifest

Each model release shall include a manifest:

```json
{
  "model_id": "yolov8n-accessibility",
  "version": "1.3.0",
  "runtime": "litert",
  "quantization": "int8",
  "input_width": 320,
  "input_height": 320,
  "classes": [
    "person",
    "chair",
    "table",
    "door",
    "wall",
    "vehicle",
    "bicycle",
    "obstacle"
  ],
  "confidence_threshold": 0.60,
  "nms_iou_threshold": 0.45,
  "max_detections": 10,
  "checksum": "sha256:..."
}
```

### 11.4 Model loading rules

1. Detector is loaded when Obstacle Awareness Mode starts.
2. OCR is loaded only when Text Reading Mode starts.
3. OCR is unloaded after 30 seconds of inactivity.
4. Optional VLM/cloud modules are never loaded by default on low-end.
5. Only one heavy model may be loaded at a time unless benchmark proves safety.
6. Model loading must occur off the main thread.
7. Model failure must not crash the app.

### 11.5 Quantization strategy

Primary approach:

- INT8 quantization for detector.
- Full integer quantization where possible.
- Calibration dataset representative of target environments.
- QAT considered if post-training quantization accuracy loss is excessive.

Validated MVP recipe (M3):

- Post-training **dynamic INT8** (`dynamic_wi8_afp32`): int8 weights, float32
  activations. Asset: `yolov8n_320_int8.tflite` (~4.1 MB, under the 20 MB
  budget). IO stays float32 NCHW so the LiteRT adapter needs no int8 input path.
- Static full-integer (`static_wi8_ai8`) was attempted on this graph: with
  asymmetric activations the model fails to prepare (`CONV_2D` zero_point);
  with symmetric activations it runs but collapses class scores to 0. QAT or a
  fixed static recipe remains M7 validation work.
- Float source retained at `yolov8n_320_float32.tflite` for parity checks.

Fallback:

- FP16 if INT8 accuracy drops below acceptance thresholds.
- Smaller model if FP16 violates latency/memory budget.

---

## 12. Feedback Architecture

Feedback is as important as inference. For users with visual disability, feedback design is a core safety and usability mechanism.

### 12.1 Feedback channels

| Channel | Use |
|---|---|
| TTS | Spoken concise messages. |
| Earcons | Short non-speech directional or proximity sounds. |
| Haptics | Urgency, direction, and immediate alerting. |
| Accessibility announcements | UI state changes. |

### 12.2 Feedback priority

```text
CRITICAL_OBSTACLE
USER_REQUESTED
OCR_RESULT
STATUS
DEBUG
```

Critical obstacle alerts must interrupt lower-priority feedback.

### 12.3 Feedback dispatcher

```kotlin
interface FeedbackDispatcher {
    suspend fun emit(alert: Alert)
    suspend fun interruptCurrent(priority: AlertPriority)
    suspend fun stopAll()
}
```

The dispatcher must:

1. Maintain a priority queue.
2. Allow preemption.
3. Avoid overlapping speech.
4. Emit haptics as early as possible.
5. Respect user settings.
6. Announce degraded states accessibly.

### 12.4 Message generation

For low-end devices, message generation is template-based.

Examples:

```text
Person ahead.
Chair on left.
Obstacle near.
Door ahead.
Possible obstacle.
```

Rules:

- Default alert max length: 6 words.
- On-demand summary max length: 25 words.
- Avoid uncertain language unless confidence is low.
- Avoid exact distance unless validated sensor exists.
- Avoid continuous narration.

### 12.5 TTS behavior

1. Use system TTS engine.
2. Respect user speech rate where available.
3. Support app-level speech rate override from 0.5x to 2.0x.
4. Critical alerts should use concise utterances.
5. TTS must not block inference.
6. TTS failure must trigger fallback earcons and status message.

---

## 13. Accessibility Architecture

### 13.1 UI accessibility requirements

All screens must provide:

1. Logical focus order.
2. Content descriptions for all interactive elements.
3. Roles and states for controls.
4. Large touch targets.
5. No reliance on color alone.
6. Readable labels for screen readers.
7. Accessible dialogs and modal transitions.
8. Persistent controls for pause/mute.
9. Predictable navigation.
10. No transient-only critical information.

### 13.2 Focus management

The app must explicitly manage focus when:

- Assistance starts.
- Assistance stops.
- An error occurs.
- A mode changes.
- A dialog appears.
- Degraded mode is activated.

Focus must never be trapped without an accessible exit.

### 13.3 Live announcements

Use Android accessibility announcement mechanisms for:

- Assistance started.
- Assistance stopped.
- Camera unavailable.
- Degraded mode.
- Critical errors.
- Settings changes.

### 13.4 Non-visual state representation

The app must maintain a spoken-accessible status line or equivalent mechanism:

```text
Assistance active.
Obstacle mode on.
Reduced mode due to device heat.
Camera unavailable.
Text reading ready.
```

### 13.5 Accessibility testing hooks

The architecture must support:

- Espresso accessibility checks.
- Manual TalkBack scripts.
- UI Automator flows.
- Accessibility audit CI gate.
- User testing session logs.

---

## 14. Threading and Concurrency Model

### 14.1 Thread roles

| Thread / Dispatcher | Responsibility |
|---|---|
| Main/UI thread | UI, accessibility focus, user events. |
| Camera executor | CameraX image analysis callbacks. |
| Perception dispatcher | Frame gating and preprocessing. |
| Inference executor | Single-thread model inference. |
| Feedback dispatcher | TTS, haptics, earcons. |
| Model loader executor | Background model loading/unloading. |
| Telemetry executor | Safe local metric aggregation. |

### 14.2 Coroutine structure

Use structured concurrency:

- `AppScope` only for app-level lifecycle-safe tasks.
- `ViewModelScope` for UI-related state.
- `OrchestratorScope` for assistance session lifecycle.
- Dedicated single-thread executor for inference.
- Dedicated channel for feedback.

### 14.3 Channels

| Channel | Type | Purpose |
|---|---:|---|
| FrameChannel | Conflated | Latest camera frame only. |
| DetectionChannel | Buffered small | Stable detections to policy. |
| AlertChannel | Priority buffered | Alerts to feedback dispatcher. |
| TelemetryChannel | Buffered | Non-sensitive metrics. |

### 14.4 Strict concurrency rules

1. No inference on main thread.
2. No camera processing on main thread.
3. No model loading on main thread.
4. No blocking TTS calls on inference thread.
5. No unbounded frame queue.
6. No concurrent loading of heavy models unless validated.
7. All cancellable pipelines must respect coroutine cancellation.

---

## 15. Performance Budget Allocation

The requirements define:

- Detector inference P95 <= 450 ms.
- End-to-end alert P95 <= 800 ms.
- TTS start <= 500 ms after alert generation.
- Haptic start <= 300 ms after alert generation.

Internal budget allocation:

| Stage | Target P95 |
|---|---:|
| Frame capture + gating | 50 ms |
| Preprocessing | 80 ms |
| Detector inference | 450 ms |
| Post-processing + fusion + policy | 70 ms |
| Feedback dispatch start | 100 ms |
| Haptic onset | <= 300 ms from alert |
| TTS onset | <= 500 ms from alert |
| Total end-to-end | <= 800 ms |

If any stage exceeds budget, the Resource Governor must reduce load.

---

## 16. Memory Architecture

### 16.1 Memory strategy

1. Keep app peak memory below 800 MB.
2. Reuse buffers.
3. Avoid per-frame Bitmap allocation.
4. Use memory-mapped model files where supported.
5. Unload OCR after idle.
6. Avoid holding high-resolution images.
7. Use low-resolution camera analysis.
8. Clear perception caches when assistance stops.

### 16.2 Memory monitoring

The app shall monitor:

- PSS or equivalent.
- Native heap growth.
- Java heap pressure.
- Low-memory callbacks.
- Model loading memory spikes.

**Implementation (M3):** `MemoryBudgetMonitor` (core) tracks baseline, peak PSS
and growth against OR-003 budgets (peak ≤ 800 MB, growth ≤ 10%). The app
samples PSS every ~5 s while obstacle assistance is active
(`AndroidMemoryMonitor` / `ObstacleAssistanceCoordinator`) and logs a
payload-free summary on stop. `Application.onTrimMemory` is forwarded through
`MemoryPressureBus` so low-memory signals can stop non-essential work (AC5).

### 16.3 Memory degradation actions

| Condition | Action |
|---|---|
| PSS > 650 MB | Warn diagnostics, avoid optional loads. |
| PSS > 750 MB | Unload optional modules, reduce FPS. |
| Low-memory callback | Stop non-essential processing. |
| Repeated memory growth | Reduce detector resolution and disable OCR preload. |

---

## 17. Thermal and Battery Architecture

### 17.1 Thermal policy

Use Android thermal status where available.

| Thermal Status | Action |
|---|---|
| None / light | Normal low-end policy. |
| Moderate | Reduce FPS by >= 50%, disable optional modules. |
| Severe | Stop continuous detection, notify user. |
| Critical / emergency | Safely stop assistance and inform user. |

### 17.2 Battery policy

| Condition | Action |
|---|---|
| Battery saver enabled | Reduce FPS to <= 3, disable optional models. |
| Battery < 15% | Notify user, suggest reduced mode. |
| Battery < 5% | Warn that assistance may stop. |

### 17.3 Continuous use target

The architecture must support at least 3 hours of continuous obstacle assistance on the reference low-end device with battery >= 4,000 mAh.

---

## 18. Data Flow for Core Modes

### 18.1 Obstacle Awareness Mode

```text
User starts assistance
→ ModeController enters ObstacleAssistanceActive
→ ModelLifecycleManager loads detector
→ CameraX starts ImageAnalysis
→ FrameGate feeds latest frames
→ Detector produces detections
→ TemporalFusion stabilizes detections
→ AlertPolicy creates alerts
→ FeedbackDispatcher emits haptic/audio
→ ResourceGovernor monitors performance
```

### 18.2 On-Demand Object Summary

```text
User triggers summary
→ Orchestrator pauses non-critical alerts
→ Captures latest frame
→ Runs detector
→ TemplateComposer creates concise text
→ FeedbackDispatcher speaks result
→ Returns to previous mode
```

### 18.3 Text Reading Mode

```text
User enters Text Reading Mode
→ OCR model loaded if needed
→ User captures or points at text
→ High-resolution still captured
→ OCR processes image
→ Text is segmented into readable blocks
→ TTS reads current block
→ User can repeat, next, or stop
→ OCR unloaded after idle timeout
```

---

## 19. Security and Privacy Architecture

### 19.1 Default privacy posture

1. All core perception runs locally.
2. No raw camera frames are stored.
3. No raw audio is stored.
4. No images are uploaded by default.
5. Optional cloud intelligence requires explicit opt-in.
6. Telemetry is opt-in where it includes non-essential data.
7. Logs must not contain images or personal identifiers.

### 19.2 Permission model

The app shall request:

- Camera: required for assistance.
- Vibration: required for haptic feedback.
- Optional notifications: only if used for service status.
- Optional microphone: only if voice commands are implemented later.

Permissions must be requested in accessible context.

### 19.3 Secure storage

- User preferences stored locally.
- No sensitive personal data required for core features.
- If tokens or cloud keys exist, use Android Keystore.
- DataStore preferred over SharedPreferences.

### 19.4 Network policy

- Core offline mode must make no network calls.
- Optional cloud features must use TLS.
- Timeouts must be enforced.
- Failure must degrade gracefully.

---

## 20. Model Management and Release Pipeline

### 20.1 Offline model pipeline

```text
Dataset collection
→ Annotation
→ Training / fine-tuning
→ Evaluation
→ Quantization
→ Conversion to TFLite/LiteRT
→ Device benchmark
→ Model manifest generation
→ Packaging or Play Asset Delivery
→ Release
```

### 20.2 Model distribution

Options:

1. Bundle core detector in app if size remains acceptable.
2. Use Play Asset Delivery install-time for core OCR if needed.
3. Use on-demand delivery for optional models only.

Core assistance must be usable after initial installation without requiring additional internet-dependent setup.

### 20.3 Model versioning

Each model release must include:

- Model ID.
- Semantic version.
- Checksum.
- Runtime.
- Input size.
- Class list.
- Thresholds.
- Benchmark results.
- Known limitations.

---

## 21. Observability and Diagnostics

### 21.1 Safe local metrics

The app may collect locally:

- Inference latency.
- Frame drop rate.
- FPS.
- Memory peaks.
- Thermal state transitions.
- Model load/unload events.
- Alert counts, without content if privacy-sensitive.
- Crash reports, opt-in where applicable.

### 21.2 Forbidden telemetry

Telemetry shall not include:

- Camera frames.
- OCR text content.
- Audio recordings.
- Precise location.
- User identity unless explicit support request.
- Health or disability details beyond app usage context.

### 21.3 Benchmark mode

A diagnostics/benchmark mode shall be available for QA and advanced users.

It should show:

- Device profile.
- Detector latency.
- FPS.
- Memory usage.
- Thermal state.
- Model versions.
- Degradation reasons.

This mode must remain accessible.

---

## 22. Testing Architecture

### 22.1 Unit tests

Cover:

- State machine transitions.
- Alert policy.
- Feedback priority queue.
- Resource governor rules.
- Template message generation.
- Proximity/sector calculation.
- Model manifest parsing.

### 22.2 Integration tests

Cover:

- Orchestrator with fake detector.
- Camera replacement with synthetic frame source.
- OCR fake outputs.
- Feedback dispatcher behavior.
- Degradation transitions.
- Model load/unload lifecycle.

### 22.3 Device benchmarks

Cover:

- Cold start.
- Camera ready time.
- Detector inference latency.
- End-to-end alert latency.
- Memory peak.
- Battery drain.
- Thermal behavior.
- OCR response time.

### 22.4 Accessibility tests

Cover:

- Automated accessibility scans.
- TalkBack manual flows.
- Focus order checks.
- Label completeness.
- Haptic/audio feedback validation.
- User testing with people with visual disability.

### 22.5 Model evaluation

Cover:

- mAP@0.5.
- Recall.
- Precision.
- False alert rate.
- Direction accuracy.
- Proximity accuracy.
- OCR word accuracy.
- Latency per device class.

---

## 23. Key Architectural Decisions

### ADR-001: Native Android first

**Decision:** Build Android-native first.  
**Reason:** Best control over accessibility, camera, inference, and low-end performance.  
**Consequence:** iOS requires separate future implementation.

---

### ADR-002: Kotlin as primary language

**Decision:** Use Kotlin for app code.  
**Reason:** Official Android language, strong coroutine support, maintainability.  
**Consequence:** Python remains offline tooling only.

---

### ADR-003: Android View system for UI

**Decision:** Use classic Android Views for version 1.  
**Reason:** Predictable TalkBack behavior and lower risk on low-end devices.  
**Consequence:** Compose may be evaluated later but is not default.

---

### ADR-004: LiteRT as primary inference runtime

**Decision:** Use LiteRT/TFLite with CPU/XNNPACK as default.  
**Reason:** Lightweight, Android-friendly, suitable for INT8 models.  
**Consequence:** ONNX Runtime is secondary and optional.

---

### ADR-005: YOLOv8n INT8 as default detector

**Decision:** Use YOLOv8n INT8 for obstacle/object detection.  
**Reason:** Good balance between size, accuracy, and mobile support.  
**Consequence:** Model must be fine-tuned and validated for accessibility classes.

---

### ADR-006: ML Kit OCR for version 1

**Decision:** Use ML Kit Text Recognition v2 as primary OCR.  
**Reason:** Stable, on-device, lower implementation risk.  
**Consequence:** Architecture must allow OCR replacement if needed.

---

### ADR-007: Template-based language for low-end

**Decision:** Use deterministic templates for core feedback.  
**Reason:** Faster, safer, lower resource usage than generative models.  
**Consequence:** Rich language description is optional and not core.

---

### ADR-008: No continuous VLM on low-end

**Decision:** Do not run continuous vision-language models on low-end devices.  
**Reason:** Memory, latency, thermal, and reliability constraints.  
**Consequence:** Optional VLM may exist only for high-end or cloud-assisted flows.

---

### ADR-009: Orchestration as core product value

**Decision:** The orchestration layer is a first-class product component.  
**Reason:** Low-end viability depends on scheduling, degradation, and feedback prioritization.  
**Consequence:** Significant engineering and testing effort goes into orchestration, not only models.

---

### ADR-010: Detector + Depth fusion for proximity

**Decision:** Keep a single always-on object detector (upgrade path: YOLOv8n → **YOLO26n** INT8, fallback YOLO11n) and add an optional **relative-depth** model as a second, **conditionally executed** signal that fuses with detector output to produce proximity and criticality. Never run both models unconditionally in the hot loop on low-end devices.

**Reason:**
- Detection answers *what/where* (label, sector, confidence). Depth answers *how deep* (relative distance under the box). Today proximity is a bbox-area heuristic (`NEAR_AREA_FRACTION`), which mis-fires with perspective and object scale — users report warnings only when the obstacle is already “on top of” the camera.
- YOLO26n (NMS-free, DFL-free) is the natural same-ecosystem upgrade: better INT8 story, lower CPU latency, higher mAP than v8n at nano scale, still LiteRT-compatible.
- Running depth only when a candidate exists (conf ≥ threshold and/or provisional NEAR) keeps sequential p95 well under the 450 ms budget and limits thermal/memory pressure; degrading by unloading depth first matches the L0–L3 ladder.

**Consequence:**
1. **Runtime:** `ObjectDetector` stays the primary port. A new `DepthEstimator` port (optional model) is owned by perception/inference; orchestration consumes a fused `Proximity`/`criticality` signal, not raw depth maps.
2. **Fusion (v1):** relative inverse-depth sampled inside the detection box (median/low percentile) combined with bbox area and confidence → `Proximity.NEAR|MEDIUM|FAR`. Fallback to area-only if depth is unloaded, fails, or is below quality gate.
3. **Scheduling:** depth runs only on candidate frames (detector output non-empty and conf/NEAR gate). Not every frame; not two concurrent interpreters by default.
4. **Lifecycle:** depth is an optional model with its own manifest entry (asset, checksum, input size, quantization). Lazy-load on first candidate (or session start on high-end profile only). Unload first under memory/thermal degradation (before detector).
5. **Budgets:** sequential worst case detector+depth must keep P95 ≤ 450 ms and end-to-end critical alert ≤ 800 ms on the SM-A226BR reference; prove with `DetectorBenchmark` + a depth-inclusive path note before enabling by default.
6. **Validation:** A/B on-device for false “cerca/lejos”; no claim of metric distance. If depth quality gate fails, silent fallback to area heuristic — never block alerts.
7. **Explicit non-goals:** continuous VLM/Grounding-DINO/OWLv2 on low-end (ADR-008); stereo depth as default (future extension only); open-vocabulary YOLO-World in the obstacle loop (on-demand query only, later milestone).
8. **Risk:** two model assets increase APK size and conversion surface → keep depth ≤ ~10 MB quantized and total core ML assets within §25.3; conversion spike required before wiring (stop condition: model conversion).

**Status:** Proposed (spike: export YOLO26n + depth-lite, bench on A226; enable default only after quality gate).

**Evaluation plan (spike before wiring):**

| Step | Action | Pass criteria |
|---|---|---|
| S1 | Export YOLO26n 320 → LiteRT + dynamic INT8 (same recipe as v8n) | File ≤ 20 MB; prepares on A226; checksum in manifest |
| S2 | Bench YOLO26n alone via existing `DetectorBenchmark` | P95 ≤ 450 ms (target ≤ 150 ms); no crash / OOM in 30 min |
| S3 | Export depth-lite (relative monocular, e.g. DA-V2-small or FastDepth-class) ≤ 256 px, INT8/FP16 | File ≤ 10 MB; LiteRT prepare OK |
| S4 | Offline eval: fuse inverse-depth-in-box + area + conf vs labeled proximity zones | Proximity accuracy ≥ 0.80 (FR-007); area-only baseline recorded |
| S5 | On-device sequential path: detect → (candidate?) depth → fused proximity | Sequential P95 ≤ 450 ms; thermal stable vs baseline |
| S6 | A/B false “cerca/lejos” with user scenarios on SM-A226BR | Fewer late NEAR alerts; no new false-critical spam |

Order: **S1–S2 first** (detector upgrade alone is shippable). Depth (S3–S6) only if S2 passes and proximity still fails the 0.80 target. Failure at any step → keep v8n + area heuristic; ADR stays Proposed or becomes Rejected with note.

---

## 24. Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Low-end NPU instability | Poor latency or crashes | Default CPU/XNNPACK; optional acceleration only after benchmark. |
| INT8 accuracy loss | Missed obstacles or false alerts | Calibration, QAT if needed, FP16 fallback, domain-specific training. |
| OCR too heavy | Memory pressure | Lazy load, unload after idle, lower resolution capture. |
| Thermal throttling | Degraded assistance | Resource governor, FPS reduction, user notification. |
| TTS latency | Delayed alerts | Use haptics and earcons immediately; TTS follows. |
| Screen reader regressions | Inaccessible app | CI accessibility checks and manual audits. |
| Bluetooth audio latency | Delayed perceived alert | Haptic feedback for critical alerts. |
| False positives | User distrust | Temporal filtering, confidence gating, cooldowns. |
| False negatives | Safety risk | Conservative messaging, known limitations, complement-only positioning. |
| Model conversion issues | Blocked release | Maintain ONNX fallback path and conversion tests. |
| Detector + depth overruns latency/memory (ADR-010) | Missed alerts, heat | Conditional depth only; unload depth first; quality gate before default enable. |

---

## 25. Deployment and Build Strategy

### 25.1 Build configuration

- Use Gradle Kotlin DSL.
- Enable R8 minification.
- Enable resource shrinking where safe.
- Use Baseline Profiles for startup performance.
- Use strict lint rules.
- Use Detekt and ktlint.
- Use dependency vulnerability scanning.

### 25.2 Release variants

| Variant | Purpose |
|---|---|
| debug | Development, leak detection, benchmark hooks. |
| qa | Internal testing, diagnostics enabled. |
| beta | User validation, opt-in telemetry. |
| release | Production, privacy-preserving defaults. |

### 25.3 App size targets

| Item | Target |
|---|---:|
| Base app without optional models | <= 150 MB |
| Core ML assets | <= 100 MB |
| Total installed core experience | <= 250 MB |

If size exceeds target, move optional assets to Play Asset Delivery.

---

## 26. Future Extensions

The architecture should allow future extensions without violating core constraints:

1. External wearable sensors.
2. Stereo camera depth on supported devices.
3. High-end VLM descriptions.
4. Cloud-assisted scene understanding with explicit consent.
5. Voice commands.
6. iOS implementation using Swift, Core ML, AVFoundation, and VoiceOver.
7. Community feedback-driven class expansion.

These extensions must not be enabled by default on low-end devices unless benchmarks and user tests validate them.

---

## 27. Final Stack Summary

| Layer | Selected Technology |
|---|---|
| Platform | Android native |
| Primary language | Kotlin |
| Optional native language | C++17 via NDK |
| Offline model tooling | Python 3.11+ |
| UI toolkit | Android View system with ViewBinding |
| Async model | Kotlin Coroutines and Flow |
| Dependency injection | Hilt |
| Camera | CameraX |
| Detector runtime | LiteRT/TFLite with CPU/XNNPACK |
| Detector model | YOLOv8n INT8 |
| OCR | ML Kit Text Recognition v2 |
| Text-to-speech | Android TextToSpeech API |
| Non-speech audio | SoundPool |
| Haptics | Android Vibrator/VibrationEffect |
| Local settings | DataStore Preferences |
| Optional inference fallback | ONNX Runtime Mobile |
| Optional future OCR | PaddleOCR mobile behind abstraction |
| Optional future VLM | Disabled by default; high-end/cloud only |
| Architecture style | Modular layered architecture with event-driven perception and orchestration core |

---

## 28. Conclusion

The recommended architecture is:

- **Android-native.**
- **Kotlin-first.**
- **View-based accessibility UI.**
- **LiteRT INT8 detection.**
- **ML Kit OCR.**
- **System TTS.**
- **Template-based feedback.**
- **Event-driven orchestration.**
- **Resource-aware degradation.**
- **No continuous heavy VLM on low-end devices.**

This architecture directly supports the project’s core challenge: delivering a useful, accessible, and safe assistance experience on modest hardware, without pretending to replace proven mobility tools such as a white cane, guide dog, or human guide.
