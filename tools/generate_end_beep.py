#!/usr/bin/env python3
"""Génère le bip de fin de minuteur (app/src/main/res/raw/end_beep.ogg).

Son original : une sinusoïde à 880 Hz (plus un harmonique à 1760 Hz très atténué), 0,45 s, avec une
attaque de 30 ms et un fondu de sortie de 250 ms. Aucun échantillon tiers.
Dépendances : bibliothèque standard + ffmpeg (encodeur libvorbis).

Usage : python3 -I tools/generate_end_beep.py app/src/main/res/raw/end_beep.ogg
"""
import math
import struct
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

RATE = 44100
DURATION = 0.45
ATTACK = 0.03
RELEASE = 0.25
PEAK = 0.45


def samples():
    total = int(RATE * DURATION)
    for i in range(total):
        t = i / RATE
        tone = math.sin(2 * math.pi * 880 * t) + 0.15 * math.sin(2 * math.pi * 1760 * t)
        attack = min(1.0, t / ATTACK)
        release = min(1.0, (DURATION - t) / RELEASE)
        envelope = attack * math.sin(release * math.pi / 2)
        yield int(32767 * PEAK * envelope * tone / 1.15)


def main(target: Path) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        wav_path = Path(tmp) / "beep.wav"
        with wave.open(str(wav_path), "wb") as wav:
            wav.setnchannels(1)
            wav.setsampwidth(2)
            wav.setframerate(RATE)
            wav.writeframes(b"".join(struct.pack("<h", s) for s in samples()))
        target.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(
            [
                "ffmpeg", "-y", "-loglevel", "error", "-i", str(wav_path),
                "-c:a", "libvorbis", "-q:a", "4", "-map_metadata", "-1", str(target),
            ],
            check=True,
        )


if __name__ == "__main__":
    main(Path(sys.argv[1]))
