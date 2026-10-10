#!/usr/bin/env python3
"""TODO(audio): synthetic syllable recordings, to be replaced by a human speaker.

Creates assets/audio/syllables/<name>.ogg for every syllable in the dictionary with an offline
neural Polish voice (Piper, run by sherpa-onnx) and ffmpeg (libvorbis). Files are generated ahead
of time and played as recordings: the app never synthesizes syllables at run time, and nothing in
the app goes through the network. Only this script downloads the voice model once (CC0 voices,
from the sherpa-onnx GitHub releases) into ~/.cache/czytanie-tts.

Piper reads text through espeak-ng's Polish rules, which spell a lone "y" as the letter name
("igrek"). That one syllable is synthesized as "yt" and cut before the "t". File names follow
`Syllable.audioPathOf` in core:domain.

Requires: pip install sherpa-onnx numpy; ffmpeg with libvorbis.
Usage: python3 tools/generate-syllable-audio.py [--voice mc_speech|gosia|darkman] [--force]
"""
import argparse
import json
import pathlib
import re
import subprocess
import tarfile
import tempfile
import urllib.request
import wave

import numpy as np
import sherpa_onnx

ASSETS = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/assets"
TARGET = ASSETS / "audio/syllables"
CACHE = pathlib.Path.home() / ".cache/czytanie-tts"
MODEL_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-pl_PL-{voice}-medium.tar.bz2"
# Slightly slower than the voice's natural pace: the syllables are for children learning to read.
LENGTH_SCALE = 1.15
# Same mapping as Syllable.audioPathOf (core:domain): ASCII, safe on case-insensitive file systems.
ASCII = {"ą": "a1", "ć": "c1", "ę": "e1", "ł": "l1", "ń": "n1", "ó": "o1", "ś": "s1", "ź": "z1", "ż": "z2"}
# Amplitude above which a sample counts as clearly voiced, to compare candidate renditions.
VOICED_LEVEL = 0.02
# Syllables espeak-ng would spell out: text to synthesize, and the cut that removes the helper sound.
SPELLED_OUT = {"y": "yt"}


def audio_name(syllable):
    return "".join(ASCII.get(ch, ch) for ch in syllable.lower() if ch.isalpha())


def load_voice(voice):
    directory = CACHE / f"vits-piper-pl_PL-{voice}-medium"
    if not directory.exists():
        CACHE.mkdir(parents=True, exist_ok=True)
        archive = CACHE / f"{voice}.tar.bz2"
        urllib.request.urlretrieve(MODEL_URL.format(voice=voice), archive)
        with tarfile.open(archive) as tar:
            tar.extractall(CACHE, filter="data")
        archive.unlink()
    vits = sherpa_onnx.OfflineTtsVitsModelConfig(
        model=str(directory / f"pl_PL-{voice}-medium.onnx"),
        tokens=str(directory / "tokens.txt"),
        data_dir=str(directory / "espeak-ng-data"),
        length_scale=LENGTH_SCALE,
    )
    return sherpa_onnx.OfflineTts(sherpa_onnx.OfflineTtsConfig(model=sherpa_onnx.OfflineTtsModelConfig(vits=vits)))


def cut_before_final_closure(samples, sample_rate):
    """Drops a final plosive: everything after the last silent gap (the stop closure)."""
    frame = int(sample_rate * 0.01)
    energy = np.array([np.abs(samples[i:i + frame]).mean() for i in range(0, len(samples) - frame, frame)])
    loud = energy > energy.max() * 0.05
    last_loud = len(loud) - 1 - np.argmax(loud[::-1])
    gap_end = last_loud - np.argmax(~loud[last_loud::-1])  # start of the burst's loud run
    return samples[: max(gap_end, 1) * frame]


def voiced_seconds(samples, sample_rate):
    return float((np.abs(samples) > VOICED_LEVEL).sum()) / sample_rate


def synthesize(tts, syllable, out):
    text = SPELLED_OUT.get(syllable.lower(), syllable)
    # A lone syllable is an unusual input for a neural voice and sometimes comes out mumbled or
    # nearly silent. An exclamation mark or a full stop makes it a short utterance, which the voice
    # pronounces more fully; the candidate with the most clearly voiced sound wins.
    candidates = []
    for candidate in (f"{text}!", f"{text}.", text):
        audio = tts.generate(candidate)
        samples = np.asarray(audio.samples, dtype=np.float32)
        if text != syllable:
            samples = cut_before_final_closure(samples, audio.sample_rate)
        candidates.append((voiced_seconds(samples, audio.sample_rate), samples))
    samples = max(candidates, key=lambda c: c[0])[1]
    with tempfile.NamedTemporaryFile(suffix=".wav") as wav_file:
        with wave.open(wav_file.name, "wb") as wav:
            wav.setnchannels(1)
            wav.setsampwidth(2)
            wav.setframerate(audio.sample_rate)
            wav.writeframes((np.clip(samples, -1, 1) * 32767).astype(np.int16).tobytes())
        # Trim leading and trailing silence, normalise loudness, encode as Vorbis.
        trim = "silenceremove=start_periods=1:start_threshold=-50dB"
        audio_filter = f"{trim},areverse,{trim},areverse,loudnorm=I=-18:TP=-2,aresample={audio.sample_rate}"
        subprocess.run(
            ["ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-y", "-i", wav_file.name,
             "-af", audio_filter, "-ac", "1", "-c:a", "libvorbis", "-q:a", "3",
             "-map_metadata", "-1", "-fflags", "+bitexact", str(out)],
            check=True,
        )


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--voice", default="mc_speech", choices=["mc_speech", "gosia", "darkman"])
    parser.add_argument("--force", action="store_true", help="regenerate existing files")
    args = parser.parse_args()
    dictionary = json.loads((ASSETS / "content/dictionary.json").read_text(encoding="utf-8"))
    # A syllable spanning a preposition ("w do") is one sound: synthesize it without the space.
    syllables = sorted({re.sub(r"[^\w]", "", s.lower()) for word in dictionary["words"] for s in word["syllables"]})
    tts = load_voice(args.voice)
    TARGET.mkdir(parents=True, exist_ok=True)
    wanted = {f"{audio_name(s)}.ogg" for s in syllables}
    for stale in TARGET.glob("*.ogg"):
        if stale.name not in wanted:
            stale.unlink()
    created = 0
    for syllable in syllables:
        out = TARGET / f"{audio_name(syllable)}.ogg"
        if out.exists() and not args.force:
            continue
        synthesize(tts, syllable, out)
        created += 1
    print(f"{len(syllables)} syllables, {created} files created with voice {args.voice}")


if __name__ == "__main__":
    main()
