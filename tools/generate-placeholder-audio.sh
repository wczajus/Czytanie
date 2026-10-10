#!/usr/bin/env bash
# TODO(audio): placeholder recordings. Creates a silent .ogg of fixed length for every phoneme in
# the dictionary that has no recording yet, so the validator, SoundPool preload and the pacing
# engine can be exercised before the real recordings (one speaker, normalised volume) arrive.
# Existing files are never overwritten. Requires ffmpeg (libvorbis) and jq.
set -euo pipefail

ASSETS="$(cd "$(dirname "$0")/../app/src/main/assets" && pwd)"
DURATION_SECONDS="${1:-0.4}"

jq -r '.phonemes[].audio' "$ASSETS/content/dictionary.json" | while read -r path; do
  target="$ASSETS/$path"
  [ -e "$target" ] && continue
  mkdir -p "$(dirname "$target")"
  ffmpeg -nostdin -hide_banner -loglevel error -f lavfi -i anullsrc=r=22050:cl=mono \
    -t "$DURATION_SECONDS" -c:a libvorbis -q:a 0 -map_metadata -1 -fflags +bitexact "$target"
  echo "created $path"
done
