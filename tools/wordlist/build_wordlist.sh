#!/usr/bin/env bash
# Builds the game's word lists from free public sources.
#   app/src/main/assets/play_words.txt   the words the game accepts: everyday words only
#   tools/wordlist/all_words.txt          every English word (reference, not used by the game)
#   tools/wordlist/common_words.txt       wider common list (reference)
# Usage: tools/wordlist/build_wordlist.sh   (run from the repo root)
set -euo pipefail
export LC_ALL=C  # byte order sorting, the game searches the list by halving

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
OUT="$HERE"
ASSETS="$ROOT/app/src/main/assets"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$OUT"

SCOWL_URL="https://downloads.sourceforge.net/wordlist/scowl-2020.12.07.tar.gz"
ENABLE_URL="https://raw.githubusercontent.com/dolph/dictionary/master/enable1.txt"
BAD_URL="https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/master/en"

curl -sSL -o "$WORK/scowl.tar.gz" "$SCOWL_URL"
curl -sSL -o "$WORK/enable.txt" "$ENABLE_URL"
curl -sSL -o "$WORK/bad.txt" "$BAD_URL"
tar xzf "$WORK/scowl.tar.gz" -C "$WORK"
FINAL="$(echo "$WORK"/scowl-*/final)"

# Words to block: public bad-words list + our own blocklist.
{ tr -d '\r' < "$WORK/bad.txt"; grep -v '^#' "$HERE/blocklist.txt"; } \
  | tr 'A-Z' 'a-z' | grep -E '^[a-z]+$' | sort -u > "$WORK/block.txt"

keep() { grep -E '^[a-z]{3,10}$' | sort -u; }

# All words: every SCOWL size and every spelling variant
# (American, British, Canadian, Australian), plus ENABLE.
# Lowercase letters only, so names, abbreviations and contractions drop out.
{ cat "$FINAL"/*-words.* | iconv -f latin1 -t utf-8; tr -d '\r' < "$WORK/enable.txt"; } \
  | keep | comm -23 - "$WORK/block.txt" > "$OUT/all_words.txt"

# Common words: SCOWL size 50 and below, also found in ENABLE.
tr -d '\r' < "$WORK/enable.txt" | keep > "$WORK/enable_k.txt"
for v in english american british canadian australian; do
  cat "$FINAL"/$v-words.{10,20,35,40,50} 2>/dev/null || true
done | iconv -f latin1 -t utf-8 | keep \
  | comm -12 - "$WORK/enable_k.txt" | comm -23 - "$WORK/block.txt" > "$OUT/common_words.txt"

# Play words: the game only accepts everyday words, so random swaps rarely make words.
# SCOWL sizes 10 and 20 (very common), plus 5+ letter words from size 35.
for v in english american british canadian australian; do
  cat "$FINAL"/$v-words.{10,20} 2>/dev/null || true
done | iconv -f latin1 -t utf-8 | keep > "$WORK/s1020.txt"
for v in english american british canadian australian; do
  cat "$FINAL"/$v-words.35 2>/dev/null || true
done | iconv -f latin1 -t utf-8 | keep | grep -E '^.{5,}$' > "$WORK/s35long.txt"
grep -v '^#' "$HERE/play_blocklist.txt" | sort -u > "$WORK/playblock.txt"
{ sort -u "$WORK/s1020.txt" "$WORK/s35long.txt" | comm -12 - "$OUT/all_words.txt"; grep -v '^#' "$HERE/play_extra.txt"; } | sort -u \
  | comm -23 - "$WORK/playblock.txt" > "$ASSETS/play_words.txt"

wc -l "$OUT/all_words.txt" "$OUT/common_words.txt" "$ASSETS/play_words.txt"
