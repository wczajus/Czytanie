#!/usr/bin/env python3
"""TODO(audio): synthetic syllable recordings, to be replaced by a human speaker.

Creates assets/audio/syllables/<name>.ogg for every syllable in the dictionary with the offline
espeak-ng synthesizer (pip package `espeakng-loader`, which bundles the library and its data) and
ffmpeg (libvorbis). Files are generated ahead of time and played as recordings: the app never
synthesizes syllables at run time, and nothing goes through the network.

Each syllable is checked against espeak's own phoneme transcription: a syllable that espeak would
spell out as letter names ("y" -> "igrek") or that has no vowel ("w", "z") is synthesized from
explicit phonemes instead. File names follow `Syllable.audioName` in core:domain.

Usage: python3 tools/generate-syllable-audio.py [--force]
"""
import ctypes
import json
import pathlib
import re
import subprocess
import sys
import tempfile
import wave

import espeakng_loader

ASSETS = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/assets"
TARGET = ASSETS / "audio/syllables"
RATE_WPM = 120
PITCH = 55
# espeak-ng phoneme input for syllables it would otherwise spell out or misread.
PHONEME_OVERRIDES = {"y": "[[y]]", "w": "[[v]]", "z": "[[z]]"}
# Same mapping as Syllable.audioName (core:domain): ASCII, case-insensitive file systems safe.
ASCII = {"ą": "a1", "ć": "c1", "ę": "e1", "ł": "l1", "ń": "n1", "ó": "o1", "ś": "s1", "ź": "z1", "ż": "z2"}

AUDIO_OUTPUT_SYNCHRONOUS = 2
POS_CHARACTER = 1
CHARS_UTF8_AND_PHONEMES = 0x101
PARAM_RATE = 1
PARAM_PITCH = 3
PHONEMES_IPA = 0x02

lib = ctypes.CDLL(espeakng_loader.get_library_path())
lib.espeak_TextToPhonemes.restype = ctypes.c_char_p
samples = bytearray()
Callback = ctypes.CFUNCTYPE(ctypes.c_int, ctypes.POINTER(ctypes.c_short), ctypes.c_int, ctypes.c_void_p)


@Callback
def collect(wav, count, _events):
    if count > 0:
        samples.extend(ctypes.string_at(wav, count * 2))
    return 0


def audio_name(syllable):
    return "".join(ASCII.get(ch, ch) for ch in syllable.lower())


def ipa(text):
    pointer = ctypes.c_char_p(text.encode())
    return lib.espeak_TextToPhonemes(ctypes.byref(pointer), 1, PHONEMES_IPA).decode()


def nuclei(syllable):
    # "i" before another vowel only softens the consonant ("pie", "sia").
    return len(re.findall(r"[aąeęioóuy]+", re.sub(r"i(?=[aąeęouóy])", "", syllable)))


def spoken_form(syllable):
    if syllable in PHONEME_OVERRIDES:
        return PHONEME_OVERRIDES[syllable]
    vowels = len(re.findall(r"[aɛiɔuɨ]", ipa(syllable)))
    if nuclei(syllable) != vowels:
        sys.exit(f"espeak misreads '{syllable}' as {ipa(syllable)}; add it to PHONEME_OVERRIDES")
    return syllable


def synthesize(text, sample_rate, out):
    samples.clear()
    data = text.encode()
    lib.espeak_Synth(data, len(data) + 1, 0, POS_CHARACTER, 0, CHARS_UTF8_AND_PHONEMES, None, None)
    with tempfile.NamedTemporaryFile(suffix=".wav") as wav_file:
        with wave.open(wav_file.name, "wb") as wav:
            wav.setnchannels(1)
            wav.setsampwidth(2)
            wav.setframerate(sample_rate)
            wav.writeframes(bytes(samples))
        # Trim leading and trailing silence, normalise loudness, encode as Vorbis.
        trim = "silenceremove=start_periods=1:start_threshold=-50dB"
        audio_filter = f"{trim},areverse,{trim},areverse,loudnorm=I=-18:TP=-2,aresample={sample_rate}"
        subprocess.run(
            ["ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-y", "-i", wav_file.name,
             "-af", audio_filter, "-ac", "1", "-c:a", "libvorbis", "-q:a", "3",
             "-map_metadata", "-1", "-fflags", "+bitexact", str(out)],
            check=True,
        )


def main():
    force = "--force" in sys.argv
    dictionary = json.loads((ASSETS / "content/dictionary.json").read_text(encoding="utf-8"))
    syllables = sorted({s.lower() for word in dictionary["words"] for s in word["syllables"]})
    sample_rate = lib.espeak_Initialize(AUDIO_OUTPUT_SYNCHRONOUS, 0, espeakng_loader.get_data_path().encode(), 0)
    lib.espeak_SetSynthCallback(collect)
    lib.espeak_SetVoiceByName(b"pl")
    lib.espeak_SetParameter(PARAM_RATE, RATE_WPM, 0)
    lib.espeak_SetParameter(PARAM_PITCH, PITCH, 0)
    TARGET.mkdir(parents=True, exist_ok=True)
    created = 0
    for syllable in syllables:
        out = TARGET / f"{audio_name(syllable)}.ogg"
        if out.exists() and not force:
            continue
        synthesize(spoken_form(syllable), sample_rate, out)
        created += 1
    print(f"{len(syllables)} syllables, {created} files created in {TARGET.relative_to(ASSETS.parent.parent.parent.parent)}")


if __name__ == "__main__":
    main()
