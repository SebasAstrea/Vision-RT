# Device Procurement — Low-End Reference Matrix

> M0 deliverable (ROADMAP M0, item 8: "Device matrix / procurement list"). Targets
> the low-end reference hardware defined in `docs/REQUIREMENTS.md` §NFR-HW (4 GB
> RAM, older SoC, near-stock OS) and `docs/ARCHITECTURE.md` §5.

## Goal
Provide 3 devices with distinct SoC vendors and form factors so CameraX,
LiteRT (TFLite API), and ThreadPoolExecutor tuning are validated on real
weak hardware — never only on a studio device.

## Procurement list (3 units)

| Tier | Device (recommended) | SoC | RAM | Android | Why it is on the list |
|---|---|---|---|---|---|
| Very low | Samsung Galaxy A14 5G | MediaTek (MT6833 Dimensity 700) | 4 GB | 13/14 (near-stock OneUI Core) | The 4 GB entry point; TFT-like general-purpose weak GPU-less real-world budget phone. |

> **In hand (QA instrument #1):** Samsung **SM-A226BR** (Galaxy A22 5G) — MediaTek
> Dimensity 700 (MT6833), 4 GB RAM (~3.5 GiB usable), Android 13 (API 33),
> arm64-v8a, connected via USB. Registered in `DeviceRegistry.KNOWN`. It is the
> reference low-end device for all smoke tests and benchmark replays.
| Low | Moto G Power 2022 (XT2217) | MediaTek Helio G37 | 4 GB | 11/12 (near-stock) | Stock-OS reference; weakest GPU/CPU in the matrix; maximum thermal throttling realism. |
| Low | Redmi 9A / POCO relative | MediaTek Helio G25 | 2–4 GB | 10+ | Alternative: cheapest retail unit; guards against single-vendor bias. |

> Fallback if a vendor is unavailable: a mid-range Samsung A15/A24 or a cheap
> Qualcomm Snapdragon 4 Gen 1 device. At least one unit **must be a MediaTek** and
> one **must be near-stock Android (Moto/e-series)** to cover the OEM-skin gap.

## Acceptance
- OS version covers API 30+ (REQUIREMENTS §2.3 minSdk 30).
- `adb push`/benchmark tooling works out of the box.
- Unit has **no bt/etc. that could mask our latency targets** (no flagship SoC).
- The devices are added to `benchmark/src/main/java/com/visionrt/benchmark/DeviceMatrixEntry` as they arrive.

## Triggers for expansion
- A new OEM skin or chipset family in the top 3 of the local user base.
- A model-size or latency slip on M4/M7 that only appears on one vendor.