# Pull Request

## Summary
<!-- One paragraph. What does this change do and why? -->

## Milestone
<!-- e.g. M0 — Foundation and Guardrails. Link the ROADMAP item. -->

## Guards (mandatory for every PR)
- [ ] No raw camera frames, OCR text or PII in logs (SafeLogger).
- [ ] No network permission added to app or core modules.
- [ ] No Google/CameraX/TTS/MQTT API used outside its owning module.
- [ ] No changes to critical latency budget paths without the architect.
- [ ] Build, tests and lint pass locally and in CI.

## Tests
<!-- List manual/automated verification performed. -->