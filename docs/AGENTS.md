# AGENTS.md

## Autonomous Coding Agent Guide

> **Executive summary:** Operational methodology for autonomous coding agents. Global constraints: Android native, Kotlin-only, offline-first, low-end priority (4 GB RAM), no bypass of accessibility or safety language. Prescribes a single approved technology stack (Views + ViewBinding, Hilt, CameraX, LiteRT INT8 YOLOv8n, ML Kit OCR, system TTS, SoundPool, VibrationEffect, DataStore), a 6-step task workflow (read context → minimal change → vertical slice → tests → verify constraints → update docs), strict performance budgets, Definition of Done, and 10 stop conditions that require human review. Development priorities: safety > accessibility > low-end performance > reliability > privacy > features > elegance. Golden rule: build the simplest accessible, safe, offline, honest supplement; never optimize for demo impressiveness.

This file is the operational methodology for autonomous coding agents working on this project.

Source of truth:

1. `requirements.md`
2. `ARCHITECTURE.md`
3. This file
4. `ROADMAP.md`

If there is a conflict, safety and accessibility requirements win over feature speed.

> **RAG service (mandatory).** This project ships a local RAG that curates the
> docs for AI agents. Read and follow the root `AGENTS.md` (sections 1-5): call
> `http://localhost:8767/methodology` before modifying anything, use `/query`
> for context, and re-index (`POST /reindex`, verify `drift == 0`) after
> changing files. It runs in its own containers `rag-visionrt-ollama` /
> `rag-visionrt-rag` (port 8767; 8765/8766 are used by other projects).

---

## 1. Product Summary

Android accessibility-first assistance app for people with visual disability.

Core functions:

- Obstacle/object awareness using camera.
- On-demand object summary.
- Text reading using OCR.
- Audio and haptic feedback.
- Offline-first operation.
- Low-end hardware support.

The app is a complement to a white cane, guide dog, human guide, or mobility training. It is not a replacement.

---

## 2. Hard Constraints

Agents must always respect:

- Android native only.
- Kotlin as primary app language.
- No Flutter, React Native, or cross-platform UI frameworks.
- Low-end first: 4 GB RAM, entry-level MediaTek/Qualcomm devices.
- Offline core features.
- No cloud calls by default.
- No continuous heavy VLM inference on low-end devices.
- No inference on main thread.
- No camera processing on main thread.
- No unbounded frame queues.
- No raw camera images in logs.
- No OCR text in logs.
- No personal data in telemetry.
- No unsafe claims like “prevents accidents” or “detects everything”.
- All critical UI must work with TalkBack.

---

## 3. Technology Stack

Use only the approved stack unless explicitly changed by a human architect.

| Area | Technology |
|---|---|
| Platform | Android native |
| Language | Kotlin |
| UI | Android View system + ViewBinding |
| Async | Kotlin Coroutines + Flow |
| DI | Hilt |
| Camera | CameraX |
| Inference | LiteRT/TFLite with CPU/XNNPACK |
| Detector | YOLOv8n INT8 |
| OCR | ML Kit Text Recognition v2 |
| TTS | Android TextToSpeech |
| Sound | SoundPool |
| Haptics | Vibrator / VibrationEffect |
| Settings | DataStore Preferences |
| Optional fallback | ONNX Runtime Mobile only if validated |
| Offline tooling | Python for model conversion/evaluation only |

---

## 4. Architecture Rules

Follow the architecture in `ARCHITECTURE.md`.

Main layers:

```text
UI/Accessibility
Application Services
Orchestration Core
Perception Pipeline
Inference Adapters
Feedback Pipeline
Diagnostics
```

Key rules:

1. UI never calls inference directly.
2. Perception never talks to TTS directly.
3. Orchestration coordinates all modes and resources.
4. Camera frames use latest-only strategy.
5. Only one heavy model is loaded unless benchmark proves otherwise.
6. OCR must unload after idle timeout.
7. Feedback uses priority queue.
8. Critical alerts interrupt non-critical feedback.
9. Degradation is a feature, not an error.
10. Every mode must fail safely.

---

## 5. Development Priorities

When making decisions, use this priority order:

```text
1. Safety and honest limitations
2. Accessibility
3. Performance on low-end devices
4. Reliability
5. Privacy
6. Feature completeness
7. Code elegance
```

Never sacrifice accessibility or low-end stability for extra features.

---

## 6. Mandatory Coding Rules

Agents must:

- Write Kotlin code compatible with Android API 30+.
- Use coroutines safely with structured concurrency.
- Keep all strings localizable.
- Add `contentDescription` to all interactive controls.
- Maintain minimum touch target of 48x48 dp.
- Avoid custom gestures unless they have accessible alternatives.
- Avoid allocating Bitmaps per camera frame.
- Reuse buffers where possible.
- Keep modules decoupled.
- Prefer small, testable components.
- Add unit tests for domain/orchestration logic.
- Add fake/mock adapters for detector, OCR, TTS, and camera.
- Preserve offline behavior.
- Update affected documentation when behavior changes.

Agents must not:

- Add new dependencies without justification.
- Introduce network calls in core assistance.
- Store raw images or OCR text.
- Run model inference on the UI thread.
- Create visual-only status indicators.
- Disable accessibility checks.
- Disable tests to make CI pass.
- Hard-code model thresholds in multiple places.
- Use uncertain UI text that implies guaranteed safety.

---

## 7. Required Task Workflow

For every task, agents must follow this sequence:

### Step 1: Read context

Read:

- `requirements.md`
- `ARCHITECTURE.md`
- Related module code
- Existing tests
- Benchmark or accessibility notes

### Step 2: Define minimal change

Before coding, identify:

- Affected modules.
- Affected requirements.
- Affected architecture constraints.
- Performance risk.
- Accessibility risk.
- Privacy risk.

### Step 3: Implement smallest vertical slice

Prefer:

- Small PRs.
- One responsibility per change.
- No mixed refactors and features.
- No speculative abstractions.

### Step 4: Add tests

Minimum tests:

- Unit tests for logic.
- Fake-based integration tests for orchestration.
- UI/accessibility assertions where relevant.
- Performance-sensitive code must include benchmark note.

### Step 5: Verify constraints

Before considering a task complete, check:

- Compiles.
- Existing tests pass.
- No new main-thread blocking.
- No new memory leak pattern.
- No new network call in offline core.
- No accessibility regression.
- No log leakage.
- No unsafe wording.

### Step 6: Update documentation

If task changes behavior, update:

- Architecture notes.
- Requirement traceability.
- Benchmark assumptions.
- Accessibility notes.
- Known limitations.

---

## 8. Implementation Order

Unless instructed otherwise, agents should build in this order:

1. Project skeleton and module structure.
2. Accessible navigation shell.
3. Settings and user preferences.
4. Orchestration state machine.
5. Fake detector and fake OCR.
6. Feedback dispatcher with TTS, earcons, and haptics.
7. CameraX frame pipeline with latest-frame-only strategy.
8. LiteRT detector adapter.
9. Alert policy and temporal filtering.
10. OCR flow.
11. Resource governor: thermal, battery, memory, latency.
12. Degradation states and accessible warnings.
13. Benchmark and diagnostics mode.
14. Accessibility hardening and user-test fixes.

---

## 9. Definition of Done

A task is done only if:

- Code compiles.
- Unit tests pass.
- Integration tests pass where applicable.
- No critical lint errors.
- No critical accessibility defects in changed screens.
- No obvious performance regression.
- Offline core remains functional.
- No new privacy leak.
- No unsafe product claims introduced.
- Documentation updated.
- Change can be explained in one short paragraph.

---

## 10. Performance Guardrails

Agents must preserve these budgets from `requirements.md`:

- App peak memory <= 800 MB on low-end reference device.
- Detector inference P95 <= 450 ms at 320x320.
- End-to-end critical alert P95 <= 800 ms.
- TTS start <= 500 ms after alert generation.
- Haptic start <= 300 ms after alert generation.
- OCR on-demand P95 <= 8 s.
- No OutOfMemoryError during 30-minute continuous session.

If a change risks violating these budgets, stop and flag it.

---

## 11. Accessibility Guardrails

Every UI change must preserve:

- TalkBack compatibility.
- Logical focus order.
- Accessible labels.
- Accessible roles and states.
- Large touch targets.
- Non-visual state announcements.
- Accessible error messages.
- Accessible pause/mute control.
- No color-only meaning.
- No transient-only critical information.

If an accessibility regression is detected, it blocks the task.

---

## 12. Safety and Product Language

Agents must use safe wording.

Allowed examples:

- “Possible obstacle ahead.”
- “Person ahead.”
- “Chair on left.”
- “I am not sure.”
- “Assistance is limited due to device heat.”

Forbidden examples:

- “Safe path ahead.”
- “No obstacles detected.”
- “You can walk now.”
- “The app will protect you.”
- “Detects everything.”

Always preserve the complement-only positioning.

---

## 13. Testing Requirements

Agents must include or update:

- Unit tests for orchestration, policies, and utilities.
- Fake adapters for detector, OCR, TTS, and camera.
- Integration tests for state transitions.
- Accessibility checks for changed screens.
- Benchmark notes for performance-sensitive changes.
- Manual test checklist when camera, feedback, or accessibility changes.

Minimum manual checklist:

1. Start assistance with TalkBack.
2. Stop assistance with TalkBack.
3. Pause/mute feedback.
4. Trigger object summary.
5. Trigger OCR.
6. Enable airplane mode and verify offline core.
7. Simulate low memory or thermal degradation if possible.
8. Verify no crash.

---

## 14. Stop Conditions

Agents must stop and request human review when:

- A requirement is ambiguous.
- A change affects safety messaging.
- A change affects accessibility core flows.
- A performance budget may be violated.
- A new dependency is needed.
- A new permission is needed.
- Model conversion or quantization is required.
- Network/cloud behavior is introduced.
- A change affects privacy or logging.
- A change affects disclaimer or limitation text.

---

## 15. Output Format for Agent Tasks

For every completed task, agents should report:

```text
Summary:
What changed and why.

Files changed:
List of main files.

Requirements affected:
Requirement IDs if known.

Architecture impact:
None / Low / Medium / High.

Accessibility impact:
None / Low / Medium / High.

Performance impact:
None / Low / Medium / High.

Tests added:
List.

Manual verification needed:
Yes/No and steps.

Risks:
Any new risk.
```

---

## 16. Golden Rule

Build the simplest thing that is:

- Accessible.
- Safe.
- Offline.
- Fast enough on low-end hardware.
- Honest about limitations.
- Useful for real users with visual disability.

Do not optimize for demo impressiveness. Optimize for reliable assistance under real constraints.
