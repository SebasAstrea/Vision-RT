# Model conversion

M3 path used for the MVP asset (float32 first; INT8 calibration lands in M7):

```bash
pip install --user 'ultralytics>=8.3.0' litert-torch
# From repo root (downloads yolov8n.pt on first run):
python3 - <<'PY'
from ultralytics import YOLO
import litert_torch, torch, os, hashlib, json
model = YOLO('yolov8n.pt')
nn = model.model.eval()
lm = litert_torch.convert(nn, sample_args=(torch.zeros(1, 3, 320, 320),), lightweight_conversion=True)
out = 'app/src/main/assets/models/yolov8n_320_float32.tflite'
os.makedirs(os.path.dirname(out), exist_ok=True)
lm.export(out)
h = hashlib.sha256(open(out, 'rb').read()).hexdigest()
# Write app/src/main/assets/models/manifest.json with input 320x320, classes=COCO-80,
# confidence 0.60, nms_iou 0.45, max_detections 10, checksum sha256:{h}.
print(out, os.path.getsize(out), h)
PY
```

Notes:

- Exported IO is NCHW input `[1,3,320,320]` and multi-head outputs; the
  Android adapter consumes only the flat detection head `[1,84,2100]`.
- INT8 quantization + representative calibration dataset: see
  `docs/ARCHITECTURE.md` §14 (M7).
- See `docs/ARCHITECTURE.md` §11.3 for the manifest schema.