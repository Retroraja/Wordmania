#!/usr/bin/env python3
"""Update 1.1: cut the play list to about 8,000 of the most used words (real usage frequency from wordfreq).

Run from the repo root after build_wordlist.sh:  python3 tools/wordlist/trim_by_frequency.py
Needs `pip install wordfreq` (free, open data from Wikipedia, subtitles, books, news and the web).

- 3-letter words: only the hand-picked everyday ones (play_three.txt, play_three_more.txt), at most 600
  (the most used first). Rare ones like CIS or EMS stay out.
- 4+ letter words: the most used words from the current list.
- Theme words (play_extra.txt) always stay, so theme levels keep working.
"""
import os
from wordfreq import zipf_frequency as z

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
ASSET = os.path.join(ROOT, "app/src/main/assets/play_words.txt")

TOTAL = 8000
THREE = 600


def read(name):
    with open(os.path.join(HERE, name)) as f:
        return {w.strip() for w in f if w.strip() and not w.startswith("#")}


current = {w.strip() for w in open(ASSET) if w.strip()}
block = read("play_blocklist.txt") | read("blocklist.txt")
picked3 = (read("play_three.txt") | read("play_three_more.txt")) - block
extra = read("play_extra.txt") - block
# Theme words (Themes.kt) stay too, so every theme level keeps all its words.
import re
themes_src = open(os.path.join(ROOT, "core/src/main/kotlin/com/retroraja/wordnoid/core/Themes.kt")).read()
extra |= {w.lower() for w in re.findall(r'"([A-Z]{3,10})"', themes_src)} & (current | extra)

# 3 letters: hand-picked only, most used first, at most THREE.
three = set(sorted(picked3, key=lambda w: -z(w, "en"))[:THREE])

# 4+ letters: most used first.
longer = sorted((w for w in current if len(w) >= 4 and w not in block), key=lambda w: -z(w, "en"))
keep = set(three) | extra
for w in longer:
    if len(keep) >= TOTAL:
        break
    keep.add(w)

with open(ASSET, "w") as f:
    for w in sorted(keep):  # byte order: the game searches the list by halving
        f.write(w + "\n")
print(len(keep), "words;", sum(1 for w in keep if len(w) == 3), "with 3 letters")
