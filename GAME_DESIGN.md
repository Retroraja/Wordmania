# Wordnoid: Game Design Document

*Version 0.3, 1 November 2026. Game name: **Wordnoid** (working repo name: Wordmania). Written from Rajarajan's idea discussion, updated with the decisions made the same day.*

## 1. One-line pitch

A level-based word puzzle where the board is a full grid of letter tiles. You swap neighbouring tiles to spell words across or down (in either direction), the word clears, new letters fall in, and you must hit the level goal before your moves run out.

Think of a match-3 game like Gardenscapes or Candy Crush, but instead of matching three of a colour you spell a word.

## 2. Platform and tech

| Item | Choice |
|---|---|
| Platform | Android phones, **landscape** (held sideways, like Homescapes) |
| Language / UI | Kotlin + Jetpack Compose |
| Build | APK built for free by GitHub Actions |
| Internet | Not needed. The game, levels and dictionary all work offline |
| Language of words | English only for now |
| Login | None in the first version. Progress is saved on the phone and restored by Android's built-in Google backup. Google sign-in through Firebase (free) can come later |

## 3. Core gameplay

### 3.1 The board

- A rectangular grid, wider than it is tall, to suit the landscape screen. Level 1 is 6 across by 4 down. Later levels grow to 7x5, 8x5 and up.
- Every cell holds exactly one letter tile. There are no empty spaces.
- Later levels can have shaped boards with blocked cells, like Homescapes.

### 3.2 A move

- The player drags a tile one step up, down, left or right (or taps a tile and then a neighbour). The two tiles swap places.
- Every swap costs **1 move**, whether or not it makes a word, and **the tiles stay where they were moved**. They never slide back. This is deliberate: in a word game you often need 2 or 3 swaps to set up a word, and that planning is the heart of the game.
- Diagonal swaps are not allowed.

### 3.3 Making a word

After every swap, the game checks every row and every column **in all four directions**: left to right, right to left, top to bottom and bottom to top.

- If a run of 3 or more neighbouring letters spells a valid word from the dictionary, that word is **found**.
- The tiles of the word flash, then disappear.
- **The word is shown in big letters for a few seconds** as it clears, so players always see what they made, including unusual words they may not know.
- If more than one word is formed at once, all of them clear. Where two candidate words overlap in the same line, the longer one wins.
- If the same letters spell a word in both directions (T-A-B is also BAT), it counts as **one** word.
- A swap that makes no word does nothing except use up a move.

### 3.4 Refill (cascade)

- When tiles disappear, the tiles above fall down to fill the gaps, and new letter tiles drop in from the top.
- The new letters are picked so they don't form accidental words straight away.
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

### 3.7 Out of moves

- A friendly "Out of moves" screen shows how close the player was ("Just 1 word to go!").
- **+3 moves for coins**, or **Try again** (always free, with no lives and no waiting).
- After 3 losses on the same level, the next try starts with 3 bonus moves automatically.

## 4. Levels

### 4.1 Same for every player (hand-designed)

Every level is a fixed recipe, so every player sees the same Level 1. This keeps the difficulty under control and guarantees each level can be won.

Each level stores:

- the grid size,
- the exact starting letters (for example Level 1: `Z D C / V G I / G Y X` style, but designed so words are reachable),
- the move limit,
- the goal,
- a fixed **seed** for the letters that drop in during refills, so the falling letters are also the same for everyone.

Levels are kept as simple level data in one place in the code, so new levels can be added without changing the game logic.

### 4.2 Difficulty curve (first draft)

The first 5 levels (first playable version):

| Level | Board (across x down) | Moves | Goal |
|---|---|---|---|
| 1 | 6x4 | 10 | Find 2 words |
| 2 | 6x4 | 10 | Find 3 words |
| 3 | 7x5 | 12 | Find 4 words |
| 4 | 7x5 | 12 | Find 3 words, one with 4+ letters |
| 5 | 8x5 | 15 | Find 5 words |

Later levels (draft):

| Levels | Board | Moves | Goal | New thing introduced |
|---|---|---|---|---|
| 6–10 | 8x5 | 12–15 | 4–5 words | Combos, Golden tile |
| 11–20 | 8x6 | 15 | Long-word goals | Joker tile, Free swap |
| 21–30 | Shaped boards | 15–20 | Specific words, carpet goals | Shuffle, Bomb |

### 4.3 Level design rule

Every level must be tested to be winnable within its move limit. Early levels should be winnable with moves to spare.

## 5. Dictionary

- The app ships with an **offline word list** (a plain text file inside the app).
- Only words of 3 letters or more count.
- The list is **as complete as possible**: about 283,000 words with American, British, Canadian and Australian spellings, because what counts as a word differs between countries. Only offensive words are removed.
- A second, smaller list of about 50,000 everyday words is used when the game *suggests* words (hints, Lucky word), so suggestions are always familiar.
- Sources: SCOWL and ENABLE (free). See `tools/wordlist/README.md`.
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
- **Dead board check:** if no word can be made at all, the board shuffles automatically for free.
- **Hints:** on early levels, after 10 seconds without a move, two tiles that would make a word gently wiggle.
- **Last chance:** when moves hit 0, offer "+3 moves" for coins. If the game is published, an optional "watch a video for +3 moves" can be added. There will never be forced ads between levels.

## 7. Scoring and stars

- Points per word = letters × 10, doubled for combos.
- Each unused move at the end = bonus points.
- 1 to 3 stars per level based on score. Stars could later unlock things (themes, tile skins).

## 8. Screens

1. **Home**: Play button, level number, settings.
2. **Level map**: a path of level circles showing stars earned. Locked levels are greyed out.
3. **Game screen** (landscape, like Homescapes):
   - Left panel: goal progress (for example "Words 1/2"), moves left (big and clear), coins.
   - Middle: the letter board on the wooden frame.
   - Right panel: power-ups and boosters stacked in a column, plus the pause button.
   - Found words and rewards pop up over the board ("CAT +3 coins", "+3 moves!").
4. **Level complete**: stars, score, Next button.
5. **Out of moves**: Retry button, Last chance offer (later).
6. **Settings**: sound, music, vibration, reset progress.
7. **Tutorials**: a pointing hand on Level 1 shows the first swap. **Every new element** (each booster, special tile or goal type) gets its own short tutorial the first time it appears.

## 9. Look and feel

- **Name: Wordnoid**, a made-up word: "Word" plus the classic arcade "-oid" ending. It's not used by any app we could find. Never mention other games' names in the store listing.
- **Style: lively arcade on wood.** The board is still wood, but polished and glossy, like a wooden arcade cabinet. Bright colours, punchy effects and big feedback make clearing words feel exciting.
- Wood frame and background, with **glowing gold and electric green** for highlights, buttons and the score. Combos and big words get a short neon glow.
- Big, easy-to-read letters in a bold, rounded arcade-style font.
- **Tiles are square** (only slightly softened corners), with a light 3D edge.
- **Every letter has its own colour.** A is always the same colour, B is always another, and so on (26 bright colours with white letters). This helps players spot letters quickly. Special tiles (Golden, Gift, Clock, Joker) keep their own look on top.
- Punchy, fast animations:
  - Swap: tiles snap past each other with a quick squash and stretch.
  - Word found: the letters flash white one by one, then **burst into coloured particles** in the letters' own colours. The word itself zooms up big in the middle and stays for a few seconds.
  - Big plays: a small screen shake for 5+ letter words and combos, and a **combo counter** ("COMBO x2!", "x3!") that grows each step.
  - Score: points fly up from the word and the score counter rolls up like an arcade machine.
  - Refill: new tiles drop fast and land with a bounce.
  - Win: fireworks of letter tiles, then Word Rain.
- Sound effects, made by code so there are no copyright issues:
  - Swap: a crisp wooden "clack".
  - Word found: a chime that rises one note per letter, so longer words sound bigger.
  - Combo: a higher, sparkly chime. Coins: a small "clink". Reward: a happy "ding".
  - Win: a short, upbeat arcade-style jingle. Out of moves: a gentle "aww", never harsh.
- Background music: an upbeat, light arcade loop that isn't too loud (free CC0 music), added after the first version.
- Settings has separate on/off switches for sound, music and vibration.
- The app icon is a glossy wooden letter tile showing "W", with a small burst behind it.
- Mascot: not decided yet (maybe later).
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

## 12. Decisions made

| Question | Decision |
|---|---|
| Screen | Landscape, wide board |
| Level 1 board | 6 across x 4 down |
| Word directions | All four: left/right and up/down, both ways |
| Swaps that make no word | Stay, and cost 1 move |
| Unusual words | Accepted, and shown in big letters when cleared |
| Name | Wordnoid |
| Theme | Lively arcade style on a polished wooden board, square tiles, one colour per letter |
| Login | None for now (on-phone save + Google backup) |
| Language | English only for now |
| Ads | None in the first version. Later, optional rewarded ads only |
| Tutorials | For Level 1 and for every new element |

## 13. Open questions

1. **Joker letter choice:** should the game pick the letter automatically, or should the player choose it?
2. **Mascot:** have one or not, and what it looks like.
