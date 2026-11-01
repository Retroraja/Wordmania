# Wordmania: Rewards and Engagement Plan

*Version 0.1, 1 November 2026. Companion to [GAME_DESIGN.md](GAME_DESIGN.md).*

## 1. The goal

Every word the player makes should feel good, straight away. Not just long words. A player who only ever finds 3-letter words should still feel progress, while a player who plans bigger plays should feel clever.

The plan works in four layers:

| Layer | When it pays out | Purpose |
|---|---|---|
| **A. Every word** | On every single word | Instant "yes!" feeling |
| **B. Good plays** | Longer words, combos, streaks, special tiles | Rewards skill and planning |
| **C. End of level** | When a level is won | A big, satisfying finish |
| **D. Long term** | Across many levels and days | Reasons to come back tomorrow |

---

## 2. Layer A: rewards on every word

### 2.1 Coins 🪙

- Every word gives coins: **1 coin per letter** (CAT = 3 coins, BEAR = 4).
- Coins fly from the word to the coin counter with a small "clink" sound.
- Coins buy boosters before a level (section 6), so they always have a use.

### 2.2 The Power Meter ⚡

A bar at the bottom of the game screen that fills with **every word**.

- 3-letter word: +1 segment. 4-letter: +2. 5+ letters: +3. Combo: +1 extra.
- The meter has 5 segments. When it is full, the player **picks 1 of 3 power-ups** (for example +3 moves, Joker, Bomb). The meter then empties and starts again.
- This is the most important idea in this document: even small words build toward something big, so no word is wasted.

### 2.3 Letter values 🔤

- Common letters (A, E, S, T…) are worth 1 point. Harder letters are worth more: K/W/Y = 3, J/X = 5, Q/Z = 8.
- Tiles show their value in a small corner number.
- Using a rare letter in a word gives a "Rare letter!" pop-up and a bonus.
- This also gets hard letters off the board, which players want anyway.

### 2.4 Praise and feedback 🎉

- Each word shows a short praise word that grows with quality: **Good → Great → Awesome → Amazing → Wordmania!**
- The word's tiles light up one by one, with a rising musical note for each letter.
- Light haptic buzz on each word, stronger for bigger plays.
- Small confetti for 4+ letters and big confetti for 5+.

### 2.5 Wordbook 📖

- Every new word the player ever finds is saved in a personal **Wordbook**.
- When a word is found for the first time, a "NEW WORD!" tag appears and gives +5 coins.
- The Wordbook shows the total ("142 words found") and rewards milestones (25, 50, 100, 250, 500…), for example a chest or a new tile theme.
- This rewards variety, so players try new words instead of repeating CAT.

---

## 3. Layer B: rewards for good plays

### 3.1 Word length

| Word length | Reward |
|---|---|
| 3 letters | Coins + 1 Power Meter segment |
| 4 letters | **+3 moves** + 2 segments |
| 5 letters | A **Joker tile** is placed on the board + 3 segments |
| 6+ letters | A **Bomb tile** + a **Shuffle** token + 3 segments |

### 3.2 Streaks (back-to-back words) 🔥

- If the player makes a word on **two moves in a row**, a streak starts.
- Streak 2: +1 bonus coin per letter. Streak 3: **1 move refunded**. Streak 4+: a free Power Meter segment each time.
- A flame icon next to the move counter shows the current streak. One move without a word ends it.

### 3.3 Combos (cascades) 💥

- When falling tiles make a word by themselves, it's a combo.
- Each combo step doubles the points: ×2, ×4, ×8.
- Each combo also gives **+1 move**.

### 3.4 Double word ✌️

- One swap that makes two words at once (one across, one down) gives a **Free swap** token.

### 3.5 Special tiles that drop onto the board

These appear in the refills and give a reward when used in a word.

| Tile | Look | Reward when used in a word |
|---|---|---|
| **Golden tile** | Gold border | Acts as a Joker for that word, + bonus coins |
| **Gift tile** 🎁 | Small ribbon | Random prize: coins, +2 moves, or a power-up |
| **Clock tile** ⏱️ | Clock icon | +2 moves |
| **Gem tile** 💎 | Sparkle | Collected for the long-term Gem collection (section 5.4) |
| **×2 tile** | "×2" badge | Doubles the points of that word |

### 3.6 Theme words 🏷️

- Some levels have a theme (Animals, Food, Colours, Body…). It is shown at the top of the screen.
- A word that fits the theme (CAT, DOG, COW in an Animals level) gives **double points** and a special sound.
- The game keeps a small list of words for each theme.

### 3.7 Lucky word 🍀

- Each level has a hidden **Lucky word** that is possible to make on that board.
- If the player spells it, they get a big reward (a chest, or +5 moves) and a special animation.
- After the level, the Lucky word is revealed ("The lucky word was RAT!"), which makes players want to replay.

---

## 4. Layer C: the end of the level

### 4.1 Word Rain finale 🌧️

When the goal is reached with moves left over, every remaining move turns into a random **letter bomb** that pops across the board, and each pop gives coins and points. This turns leftover moves into a fun firework show and rewards efficient play.

### 4.2 Stars ⭐

- 1 star for finishing, 2 stars and 3 stars for score targets.
- Stars fill a **Star Chest**. Every 10 stars opens a chest with coins and boosters.

### 4.3 Level summary

The win screen shows: words found, longest word, best combo, new words added to the Wordbook, coins earned. Every line is a small "look what you did" moment.

---

## 5. Layer D: long term (coming back every day)

### 5.1 Daily reward calendar 📅

- Open the game each day to collect a gift. Day 1: 20 coins … Day 7: a big chest.
- Missing a day doesn't reset the calendar completely, so it isn't punishing.

### 5.2 Daily puzzle 🧩

- One special board per day, the same for every player (it works offline because the day's date picks the puzzle).
- Its own reward, plus a "daily puzzle streak" counter.

### 5.3 The Word Tree 🌳 (meta progression)

- Stars earned in levels are used to grow and decorate a **Word Tree** on the home screen: new branches, leaves, birds, a swing, lanterns.
- Each decoration costs a few stars, so every level won makes something visible grow.
- This is the "decorate the garden" feeling, made our own and kept simple.

### 5.4 Collections 💎

- Gem tiles collected in levels fill sets (5 gems of each colour).
- A finished set unlocks a new **tile theme** (wood, candy, space, ocean).

### 5.5 Achievements 🏆

Examples: "Find 10 four-letter words", "Use a Q", "Make a ×4 combo", "Win a level with 5 moves left", "Find 100 words". Each gives coins.

---

## 6. Boosters (bought with coins)

### 6.1 Before a level

| Booster | Effect | Price |
|---|---|---|
| +5 Moves | Start with 5 extra moves | 50 coins |
| Starting Joker | One Joker tile already on the board | 60 coins |
| Starting Bomb | One Bomb tile already on the board | 60 coins |

### 6.2 During a level

| Booster | Effect | Price |
|---|---|---|
| Hint 💡 | Highlights one swap that makes a word | 20 coins |
| Free swap | Next swap costs no move | 30 coins |
| Shuffle | Mixes the board, costs no move | 40 coins |
| Letter change | Change any one tile to a letter you choose | 80 coins |

New players get a few free of each to learn them. Early levels give a free Hint if the player doesn't move for 15 seconds.

---

## 7. Keeping it fair (no frustration)

- **No lives or waiting timers.** The player can always play again straight away.
- **Vowel rescue:** a low-vowel board always gets a vowel in the next refill.
- **Dead board:** if no swap can make a word, the board shuffles for free.
- **Near-miss help:** after losing the same level 3 times, the next try starts with a free +3 moves.
- **Last chance:** when out of moves, offer +3 moves for coins, or later for watching a short video ad. It's always optional.

---

## 8. Example: what one move feels like

The player swaps A into C _ T:

1. **CAT** lights up letter by letter with rising notes. "Good!" appears.
2. 3 coins fly to the counter. The Power Meter fills one segment.
3. CAT is new, so "NEW WORD! +5" appears and it's saved to the Wordbook.
4. CAT fits today's Animals theme, so points double with a special sound.
5. The tiles fall and a Gift tile drops in. The player now has a reason to aim for it next.
6. The streak flame lights up, telling the player another word next move refunds a move.

One 3-letter word, six small rewards, and a reason to make the next move.

---

## 9. What to build first

Not everything is needed at once. Suggested order:

| Priority | Features | Why |
|---|---|---|
| **1 (first playable)** | Coins, Power Meter, praise text and sounds, word length rewards, combos | Biggest effect on feel, simple to build |
| **2** | Streaks, Golden/Gift/Clock tiles, Word Rain finale, stars | Rewards skill, makes levels exciting |
| **3** | Wordbook, boosters shop, Hint, theme words, Lucky word | Gives coins a use, adds variety |
| **4** | Daily reward, daily puzzle, Word Tree, collections, achievements | Reasons to return each day |
| **5 (optional)** | Video ad for Last chance | Only if releasing publicly |

## 10. Open questions

1. Should the Power Meter let the player **choose** the power-up (more control) or give a **random** one (more surprise)? This plan says choose.
2. Is the Word Tree the right home-screen theme, or would you like something else to build up (a house, a city, a library)?
3. Should found words show a short **meaning** when tapped? It's nice for learning, but it needs a bigger offline dictionary.
