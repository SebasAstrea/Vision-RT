# Dataset Annotation Schema

> M0 deliverable (ROADMAP M0, item 7: "Initial dataset annotation schema").
> Applies requirements `DAT-002` (schema) and `DAT-003` (only licensed images —
> CC0 / CC-BY-4.0, no scraped personal photos). Machine-readable copy:
> `tools/dataset_validation/schema/annotation_schema.json`.

## Purpose
A single, versioned annotation format shared by `perception/detection` training,
`perception/ocr` capture, and the model export tooling in `tools/`. It is the
contract between the ML team and the perception/android team.

## Versioning
- Schema itself is versioned (`schemaVersion`). Bump on breaking field changes.
- `modelVersion` identifies the YOLOv8n export (including quant = INT8) so every
  detection can be traced to the exact weights via `Detection.sourceModelVersion`.

## Top-level record

```jsonc
{
  "schemaVersion": "1.0",
  "split": "train|val|test",
  "annotations": [ { /* DetectionAnnotation */ } ]
}
```

## `DetectionAnnotation` fields (v1.0)

| Field | Type | Required | Constraints |
|---|---|---|---|
| `annotationId` | string | yes | UUID, stable across re-exports |
| `image` | object | yes | `source`(url/file), `license`(`CC0`/`CC-BY-4.0`), `width`, `height`, `camera`(`false`) |
| `objects` | array | yes | ≥0; see `ObjectAnnotation` |
| `sceneContext` | object | no | `indoor`/`outdoor`, `lighting.lux` (approx.), `note` (free text, ≤500 chars) |

## `ObjectAnnotation` fields (v1.0)

| Field | Type | Required | Constraints |
|---|---|---|---|
| `classId` | int | yes | Must exist in `COCO80_LITE_REDUCED` class list (see below) |
| `label` | string | yes | Stable name matching `classId` |
| `bbox` | object | yes | `xMin`, `yMin`, `xMax`, `yMax` ∈ [0,1], xMax>xMin, yMax>yMin; TFLite-compatible normalized |
| `confidence` | float | no | Ground-truth quality `∈ [0,1]`; `1.0` for clean auto-generated labels |
| `occluded` | boolean | no | default `false` |
| `truncated` | boolean | no | default `false` |
| `attribute` | object | no | `motion.blurred` (`boolean`) |
| `note` | string | no | ≤500 chars, human review remarks |

## Class list (COCO80 reduced for assistive day use)
`0 person, 1 bicycle, 2 car, 3 motorcycle, 4 bus, 5 truck, 6 bench, 7 stop_sign,
8 dog, 9 cat, 10 chair, 11 table, 12 backpack, 13 umbrella, 14 handbag, 15 vase`
(kept intentionally small; see REQUIREMENTS `DAT-003`/`OR-003`).

## OCR captures (`TextCapture` records)
Reuse the same record with an `objects: []` array and a top-level
`ocr: { text: string, blockBox: {...} }` only for **curated synthetic OCR
validation sets**; runtime OCR text is never stored or logged (see
`SafeLogger` and `OR-002`).

## Validation (CI)
`tools/dataset_validation/` must reject:
- non-CC0/CC-BY-4.0 sources,
- bounding boxes outside [0,1] or with inverted axes,
- unknown `classId`s,
- `text` fields in perception datasets (privacy).

See `docs/REQUIREMENTS.md` §DAT for the full rules.