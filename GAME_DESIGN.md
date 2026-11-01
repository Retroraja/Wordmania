# Wordmania: Game Design Document

*Version 0.1, 1 November 2026. Written from Rajarajan's idea discussion.*

## 1. One-line pitch

A level-based word puzzle where the board is a full grid of letter tiles. You swap neighbouring tiles to spell words in a row or column, the word clears, new letters fall in, and you must hit the level goal before your moves run out.

Think of a match-3 game like Gardenscapes or Candy Crush, but instead of matching three of a colour you spell a word.

## 2. Platform and tech

| Item | Choice |
|---|---|
| Platform | Android phones (portrait) |
| Language / UI | Kotlin + Jetpack Compose |
| Build | APK built for free by GitHub Actions |
| Internet | Not needed. The game, levels and dictionary all work offline |
| Language of words | English |

## 3. Core gameplay

### 3.1 The board

- A square grid, like a small chess board. Early levels use 3x3 or 4x4. Later levels grow to 5x5, 6x6 and up.
- Every cell holds exactly one letter tile. There are no empty spaces.

### 3.2 A move

- The player drags a tile one step up, down, left or right (or taps a tile and then a neighbour). The two tiles swap places.
- Every swap costs **1 move**, whether or not it makes a word.
- Diagonal swaps are not allowed.

### 3.3 Making a word

After every swap, the game checks every row (read left to right) and every column (read top to bottom).

- If a run of 3 or more neighbouring letters spells a valid word from the dictionary, that word is **found**.
- The tiles of the word flash, then disappear.
- If more than one word is formed at once, all of them clear. Where two candidate words overlap in the same line, the longer one wins.
- A swap that makes no word does nothing except use up a move.

### 3.4 Refill (cascade)

- When tiles disappear, the tiles above fall down to fill the gaps, and new letter tiles drop in from the top.
- If the falling tiles happen to spell another word on their own, it clears too. This is a **combo** (see rewards).

### 3.5 Winning and losing

- Each level has a **move limit** and a **goal**.
- **Win:** reach the goal before moves run out. Leftover moves add to the score and to the star rating.
- **Lose:** moves reach 0 and the goal is not met. The player can retry, or take the "Last chance" offer (section 6).

### 3.6 Level goals

| Goal type | Example |
|---|---|
| Find N words | "Find 2 words" |
| Find specific words | "Spell CAT and DOG" |
| Find a long word | "Make one 4-letter word" |
| Score target | "Score 500 points" |

Level 1 uses the simplest goal: **find 2 words in 10 moves**.

## 4. Levels

### 4.1 Same for every player (hand-designed)

Every level is a fixed recipe, so every player sees the same Level 1. This keeps the difficulty under control and guarantees each level can be won.

Each level stores:

- the grid size,
- the exact starting letters (for example Level 1: `Z D C / V G I / G Y X` style, but designed so words are reachable),
- the move limit,
- the goal,
- a fixed **seed** for the letters that drop in during refills, so the falling letters are also the same for everyone.

Levels live in a simple data file inside the app (for example `levels.json`), so new levels can be added without changing game code.

### 4.2 Difficulty curve (first draft)

| Levels | Grid | Moves | Goal | New thing introduced |
|---|---|---|---|---|
| 1–3 | 3x3 / 4x4 | 10 | 2–3 three-letter words | Swapping and clearing |
| 4–10 | 4x4 | 10–12 | 3–4 words | Refill and combos |
| 11–20 | 5x5 | 12–15 | 4-letter word goals | +3 moves reward, Golden tile |
| 21–30 | 5x5 | 15 | Specific words | Joker tile, Free swap |
| 31+ | 6x6 | 15–20 | Mixed goals | Shuffle, Edge bonus |

### 4.3 Level design rule

Every level must be tested to be winnable within its move limit. Early levels should be winnable with moves to spare.

## 5. Dictionary

- The app ships with an **offline word list** (a plain text file inside the app).
- Only words of 3 letters or more count.
- The list is cleaned up: offensive words, obscure abbreviations and very rare words are removed, so players only see familiar words accepted.
- Source: a free, public-domain English word list (for example ENABLE), filtered by us.
- Lookup is instant (the list is loaded into memory as a set when the game starts).

## 6. Rewards and power-ups

> The full rewards plan (coins, Power Meter, streaks, special tiles, daily rewards and more) is in [REWARDS_AND_ENGAGEMENT.md](REWARDS_AND_ENGAGEMENT.md). This section is the short version.

The aim is to keep players feeling rewarded so they don't get frustrated and uninstall. Long words are hard on a small board, so rewards come from several kinds of clever play, not only long words.

### 6.1 Rewards for word length

| Achievement | Reward |
|---|---|
| 3-letter word | Counts toward goal, base points |
| 4-letter word | **+3 moves** |
| 5-letter word | A **Joker tile** appears on the board |
| 6+ letter word | A **Shuffle** token, plus big bonus points |

### 6.2 Rewards for smart play

| Trigger | Reward |
|---|---|
| **Combo**: a refill makes a word by itself | +2 moves and a spark effect |
| **Double clear**: one swap makes two words | A **Free swap** token |
| **Edge / corner word**: the word uses tiles on the edge or corner of the grid | A **Free swap** token |
| **Golden tile**: after any word clears, one new falling tile may have a golden border. Use it in your next word | That tile counts as a Joker for that word, plus bonus points |

### 6.3 Power-ups (tokens and special tiles)

| Power-up | What it does |
|---|---|
| **+3 moves** | Adds 3 moves to the counter straight away |
| **Joker tile** 🃏 | A special tile that acts as any letter. When it lines up with other letters, the game picks a letter that makes a valid word |
| **Free swap** 🔄 | Banked token. The next swap does not cost a move |
| **Shuffle** 🔀 | Banked token. Re-scrambles all the letters on the board without costing a move |
| **Bomb** 💣 (later) | Clears a 3x3 area around it. Good for removing hard letters like Q, X, Z |
| **Zap** ⚡ (later) | Removes one useless letter (like a lone Z) and drops a new one in |

### 6.4 Anti-frustration helpers

- **Vowel rescue:** if the board runs too low on vowels (A, E, I, O, U), the next refill always drops a vowel.
- **Dead board check:** if no word can be made in any single swap, the board shuffles automatically for free.
- **Last chance:** when moves hit 0, offer "+3 moves" by watching a short video ad (added in a later version, optional).

## 7. Scoring and stars

- Points per word = letters × 10, doubled for combos.
- Each unused move at the end = bonus points.
- 1 to 3 stars per level based on score. Stars could later unlock things (themes, tile skins).

## 8. Screens

1. **Home**: Play button, level number, settings.
2. **Level map**: a path of level circles showing stars earned. Locked levels are greyed out.
3. **Game screen**:
   - Top: moves left, goal progress (for example "Words 1/2"), score.
   - Middle: the letter grid.
   - Bottom: banked power-ups (Free swap, Shuffle).
4. **Level complete**: stars, score, Next button.
5. **Out of moves**: Retry button, Last chance offer (later).
6. **Settings**: sound, music, vibration, reset progress.

## 9. Look and feel

- Clean, bright, friendly, with big, easy-to-read letters.
- **Tiles are square** (only slightly softened corners), with a light 3D edge.
- **Every letter has its own colour.** A is always the same colour, B is always another, and so on (26 bright colours with white letters). This helps players spot letters quickly. Special tiles (Golden, Gift, Clock, Joker) keep their own look on top.
- Short animations: tiles slide on swap, found words glow then pop, new tiles bounce in from the top.
- Small sound effects for swap, clear, combo, win and lose.
- Haptic tap on swap and on word clear.

## 10. Saved data (on the phone)

- Highest level unlocked
- Stars per level
- Banked power-ups
- Settings

No account or login needed.

## 11. Build plan

| Phase | What gets built | Result |
|---|---|---|
| 1. Prototype | 4x4 grid, swap, dictionary check, clear, refill, move counter, win/lose | A playable APK to try the core feel |
| 2. Levels | Level data file, 10 hand-made levels, level map, saving progress | A real level-by-level game |
| 3. Rewards | +3 moves, Joker, Free swap, Shuffle, Golden tile, combos, vowel rescue | The full reward loop |
| 4. Polish | Animations, sounds, stars, settings, app icon | Ready to share with friends |
| 5. Release (optional) | More levels, ads for "Last chance", Play Store listing | Public release |

## 12. Open questions

1. **Grid size for Level 1:** this document picks 4x4, because it gives more room to find words. 3x3 is the simpler alternative.
2. **Do words count backwards?** This document says no: only left-to-right and top-to-bottom.
3. **Should swaps that make no word be allowed?** This document says yes, they just cost a move (like the original idea). The alternative is to undo swaps that make no word, as many match-3 games do.
4. **Joker letter choice:** should the game pick the letter automatically, or should the player choose it?
5. **Ads:** include the "watch a video for +3 moves" offer in the first release, or later?
