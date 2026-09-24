#!/usr/bin/env python3
"""M7 model validation evaluator (ROADMAP deliverable 2 / model targets).

Compares offline detections against annotation JSON (tools/dataset_validation
schema) using IoU matching. Prints precision/recall and exits non-zero when
--enforce thresholds are not met.

This tool does NOT convert or quantize models (stop condition: human review).

Usage:
  python3 tools/dataset_validation/evaluate_detections.py \
      --annotations path/to/annotations.json \
      --detections path/to/detections.json \
      [--iou 0.5] [--enforce]

Annotation JSON follows schema/annotation_schema.json (normalized 0..1 boxes).
Detection JSON:
  {
    "schemaVersion": "1.0",
    "images": [
      {
        "image": "<same key as annotation image source basename or full uri>",
        "detections": [
          {"classId": 1, "label": "chair", "bbox": {"xMin":0.1,"yMin":0.2,"xMax":0.4,"yMax":0.6}, "confidence": 0.82}
        ]
      }
    ]
  }
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

DEFAULT_IOU = 0.5
# ROADMAP M7 model validation targets (subset applicable to box metrics).
DEFAULT_RECALL = 0.85
DEFAULT_PRECISION = 0.75


def load_json(path: Path) -> Any:
    with path.open(encoding="utf-8") as f:
        return json.load(f)


def iou(a: dict, b: dict) -> float:
    x1 = max(a["xMin"], b["xMin"])
    y1 = max(a["yMin"], b["yMin"])
    x2 = min(a["xMax"], b["xMax"])
    y2 = min(a["yMax"], b["yMax"])
    inter = max(0.0, x2 - x1) * max(0.0, y2 - y1)
    if inter <= 0.0:
        return 0.0
    area_a = max(0.0, a["xMax"] - a["xMin"]) * max(0.0, a["yMax"] - a["yMin"])
    area_b = max(0.0, b["xMax"] - b["xMin"]) * max(0.0, b["yMax"] - b["yMin"])
    union = area_a + area_b - inter
    return inter / union if union > 0 else 0.0


def image_key(image_ref: Any) -> str:
    if isinstance(image_ref, dict):
        return str(image_ref.get("source", ""))
    return str(image_ref)


def evaluate(
    annotations: dict,
    detections: dict,
    iou_thr: float = DEFAULT_IOU,
) -> dict:
    ann_by_key: dict[str, list[dict]] = {}
    for entry in annotations.get("annotations", []):
        key = image_key(entry.get("image"))
        ann_by_key[key] = entry.get("objects", [])

    det_by_key: dict[str, list[dict]] = {}
    for entry in detections.get("images", []):
        key = str(entry.get("image", ""))
        det_by_key[key] = entry.get("detections", [])

    tp = fp = fn = 0
    per_class: dict[int, dict[str, int]] = {}

    all_keys = set(ann_by_key) | set(det_by_key)
    for key in sorted(all_keys):
        gts = list(ann_by_key.get(key, []))
        dets = sorted(
            det_by_key.get(key, []),
            key=lambda d: float(d.get("confidence", 0.0)),
            reverse=True,
        )
        matched_gt = [False] * len(gts)
        for det in dets:
            best_i = -1
            best = 0.0
            det_class = int(det.get("classId", -1))
            det_box = det["bbox"]
            for i, gt in enumerate(gts):
                if matched_gt[i] or int(gt.get("classId", -2)) != det_class:
                    continue
                score = iou(det_box, gt["bbox"])
                if score > best:
                    best = score
                    best_i = i
            cls = per_class.setdefault(det_class, {"tp": 0, "fp": 0, "fn": 0})
            if best_i >= 0 and best >= iou_thr:
                matched_gt[best_i] = True
                tp += 1
                cls["tp"] += 1
            else:
                fp += 1
                cls["fp"] += 1
        for i, gt in enumerate(gts):
            if not matched_gt[i]:
                fn += 1
                gcls = int(gt.get("classId", -1))
                pc = per_class.setdefault(gcls, {"tp": 0, "fp": 0, "fn": 0})
                pc["fn"] += 1

    precision = tp / (tp + fp) if (tp + fp) else 0.0
    recall = tp / (tp + fn) if (tp + fn) else 0.0
    return {
        "tp": tp,
        "fp": fp,
        "fn": fn,
        "precision": precision,
        "recall": recall,
        "iou_thr": iou_thr,
        "per_class": {
            str(c): {
                "precision": (
                    v["tp"] / (v["tp"] + v["fp"]) if (v["tp"] + v["fp"]) else 0.0
                ),
                "recall": (
                    v["tp"] / (v["tp"] + v["fn"]) if (v["tp"] + v["fn"]) else 0.0
                ),
                **v,
            }
            for c, v in sorted(per_class.items())
        },
    }


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--annotations", type=Path, required=True)
    p.add_argument("--detections", type=Path, required=True)
    p.add_argument("--iou", type=float, default=DEFAULT_IOU)
    p.add_argument("--min-recall", type=float, default=DEFAULT_RECALL)
    p.add_argument("--min-precision", type=float, default=DEFAULT_PRECISION)
    p.add_argument(
        "--enforce",
        action="store_true",
        help="Exit 1 when precision/recall targets fail.",
    )
    args = p.parse_args(argv)

    annotations = load_json(args.annotations)
    detections = load_json(args.detections)
    metrics = evaluate(annotations, detections, iou_thr=args.iou)

    print(json.dumps(metrics, indent=2, sort_keys=True))
    print(
        f"precision={metrics['precision']:.3f} "
        f"(min {args.min_precision}) "
        f"recall={metrics['recall']:.3f} "
        f"(min {args.min_recall})",
        file=sys.stderr,
    )

    if not args.enforce:
        return 0

    ok = (
        metrics["precision"] >= args.min_precision
        and metrics["recall"] >= args.min_recall
    )
    if not ok:
        print(
            "FAIL: model validation targets not met "
            "(see ROADMAP M7 model validation targets). "
            "Do not ship unsafe claims; reduce class scope or document limits.",
            file=sys.stderr,
        )
        return 1
    print("PASS: precision/recall targets met at this IoU.", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
