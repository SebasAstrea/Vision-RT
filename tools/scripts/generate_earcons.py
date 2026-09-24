#!/usr/bin/env python3
"""Generate short monophonic earcon WAVs for SoundPool (M4 UX-005)."""
import math
import struct
import wave
from pathlib import Path

OUT_DIR = Path(__file__).resolve().parents[2] / "feedback" / "src" / "main" / "res" / "raw"
RATE = 22050


def write_tone(path: Path, freq: float, duration_ms: int, volume: float = 0.55) -> None:
    n = int(RATE * duration_ms / 1000)
    frames = bytearray()
    for i in range(n):
        # Linear fade-in/out avoids clicks that startle users with hearing aids.
        t = i / n
        env = min(1.0, t * 10, (1.0 - t) * 10)
        sample = volume * env * math.sin(2 * math.pi * freq * i / RATE)
        frames += struct.pack("<h", int(sample * 32767))
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(RATE)
        wav.writeframes(frames)


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    # Distinct pitches: left lower, center mid, right higher (UX-005.1).
    write_tone(OUT_DIR / "earcon_left.wav", freq=440.0, duration_ms=90)
    write_tone(OUT_DIR / "earcon_center.wav", freq=587.0, duration_ms=90)
    write_tone(OUT_DIR / "earcon_right.wav", freq=740.0, duration_ms=90)
    print(f"Wrote earcons to {OUT_DIR}")


if __name__ == "__main__":
    main()
