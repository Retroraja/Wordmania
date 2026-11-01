# Word lists

The game checks every word against `app/src/main/assets/words.txt`.

| File | What it is | Size |
|---|---|---|
| `words.txt` | **Every word the game accepts.** About 283,000 words, 3 to 10 letters, with American, British, Canadian and Australian spellings (colour and color, realise and realize, lakh, arvo…). | ~2.4 MB |
| `common_words.txt` | About 50,000 everyday words. Used for hints, the Lucky word and designing levels, so the game only *suggests* familiar words. It still *accepts* everything in `words.txt`. | ~420 KB |

## How to change the words

- **Block a word:** add it to `blocklist.txt` and run `tools/wordlist/build_wordlist.sh` from the repo root.
- **Rebuild everything:** run the same script. It downloads the sources below and writes both files.

Rules the script applies: lowercase letters a–z only (so names, abbreviations and words with apostrophes are left out), 3 to 10 letters, and no offensive words.

## Sources and licences

- **SCOWL** (Spell Checker Oriented Word Lists), all sizes and spelling variants, http://wordlist.aspell.net/
  Copyright 2000-2018 by Kevin Atkinson. Permission to use, copy, modify, distribute and sell these word lists, the associated scripts, the output created from the scripts, and its documentation for any purpose is hereby granted without fee, provided that the above copyright notice appears in all copies and that both that copyright notice and this permission notice appear in supporting documentation. Kevin Atkinson makes no representations about the suitability of this array for any purpose. It is provided "as is" without express or implied warranty.
- **ENABLE** word list, public domain.
- **List of Dirty, Naughty, Obscene, and Otherwise Bad Words** (LDNOOBW), used only to remove words. CC BY 4.0.
