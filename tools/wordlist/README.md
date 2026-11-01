# Word lists

The game accepts only **everyday words**, so players have to think to make words and random swaps rarely spell anything.

| File | What it is | Size |
|---|---|---|
| `app/src/main/assets/play_words.txt` | **The words the game accepts.** About 33,000 everyday words: the most common 3 and 4 letter words, and common 5 to 10 letter words. American, British, Canadian and Australian spellings. | ~300 KB |
| `tools/wordlist/all_words.txt` | Every English word (about 283,000). Reference only, not in the app. | ~2.4 MB |
| `tools/wordlist/common_words.txt` | A wider common list (about 50,000). Reference only. | ~420 KB |

## How to change the words

- **Block a word from the game:** add it to `play_blocklist.txt` (or `blocklist.txt` to drop it everywhere) and run `tools/wordlist/build_wordlist.sh` from the repo root.
- **Rebuild everything:** run the same script. It downloads the sources below and writes both files.

Rules the script applies: lowercase letters a–z only (so names, abbreviations and words with apostrophes are left out), 3 to 10 letters, and no offensive words.

## Sources and licences

- **SCOWL** (Spell Checker Oriented Word Lists), all sizes and spelling variants, http://wordlist.aspell.net/
  Copyright 2000-2018 by Kevin Atkinson. Permission to use, copy, modify, distribute and sell these word lists, the associated scripts, the output created from the scripts, and its documentation for any purpose is hereby granted without fee, provided that the above copyright notice appears in all copies and that both that copyright notice and this permission notice appear in supporting documentation. Kevin Atkinson makes no representations about the suitability of this array for any purpose. It is provided "as is" without express or implied warranty.
- **ENABLE** word list, public domain.
- **List of Dirty, Naughty, Obscene, and Otherwise Bad Words** (LDNOOBW), used only to remove words. CC BY 4.0.
