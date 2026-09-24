# Model conversion

M3 path used for the MVP asset (float32 export, then INT8 post-training quantization):

```bash
pip install --user 'ultralytics>=8.3.0' litert-torch ai-edge-quantizer
# From repo root (downloads yolov8n.pt on first run):
python3 - <<'PY'
from ultralytics import YOLO
import litert_torch, torch, os, hashlib
model = YOLO('yolov8n.pt')
nn = model.model.eval()
lm = litert_torch.convert(nn, sample_args=(torch.zeros(1, 3, 320, 320),), lightweight_conversion=True)
out = 'app/src/main/assets/models/yolov8n_320_float32.tflite'
os.makedirs(os.path.dirname(out), exist_ok=True)
lm.export(out)
print(out, os.path.getsize(out), hashlib.sha256(open(out,'rb').read()).hexdigest())
PY

# INT8 (dynamic weight-only; no calibration set required):
python3 - <<'PY'
from pathlib import Path
from ai_edge_quantizer import recipe
from ai_edge_quantizer.quantizer import Quantizer
import hashlib
src = Path('app/src/main/assets/models/yolov8n_320_float32.tflite')
dst = Path('app/src/main/assets/models/yolov8n_320_int8.tflite')
qt = Quantizer(str(src))
qt.load_quantization_recipe(recipe.dynamic_wi8_afp32())
qt.quantize(serialize_to_path=str(dst))
h = hashlib.sha256(dst.read_bytes()).hexdigest()
print(dst, dst.stat().st_size, h)
# Write app/src/main/assets/models/manifest.json: quantization int8,
# model_asset yolov8n_320_int8.tflite, checksum sha256:{h}.
PY
```

Notes:

- Exported IO is NCHW input `[1,3,320,320]` and multi-head outputs; the
  Android adapter consumes only the flat detection head `[1,84,2100]`.
- **INT8 recipe used:** `dynamic_wi8_afp32` (int8 weights, float activations).
  Static full-integer (`static_wi8_ai8`) was validated on this graph but
  collapses class-score outputs (max score → 0); QAT or a static recipe
  remains M7 work. Dynamic keeps float IO so `LiteRtObjectDetector` needs no
  int8 input path.
- Float source kept at `yolov8n_320_float32.tflite` for parity checks; runtime
  default is the int8 asset via `manifest.json`.
- See `docs/ARCHITECTURE.md` §11.3 for the manifest schema.

## ADR-010 spike (Detector + Depth) — not wired yet

Follow `docs/ARCHITECTURE.md` ADR-010 evaluation plan (S1–S6). Sketch:

1. **YOLO26n** — same pipeline as above with `yolo26n.pt` (or Ultralytics
   `YOLO26n` export path when available): float32 → `dynamic_wi8_afp32` →
   bench on SM-A226BR before swapping `manifest.json`.
2. **Depth-lite** — pick a monocular relative-depth model exportable to
   LiteRT (≤ ~10 MB quantized, input ≤ 256). Sample inverse depth inside
   each detection box (median); do not claim metric distance (FR-007.4).
3. Keep depth **out of** the hot loop until S4–S5 pass; fallback remains
   bbox-area proximity.

Stop conditions: model conversion requires human review (root `AGENTS.md`).
