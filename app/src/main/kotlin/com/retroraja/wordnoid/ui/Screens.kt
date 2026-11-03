package com.retroraja.wordnoid.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.ads.Ads
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Booster
import com.retroraja.wordnoid.core.Themes
import com.retroraja.wordnoid.core.Level
import com.retroraja.wordnoid.data.Progress
import com.retroraja.wordnoid.data.Rewards
import kotlinx.coroutines.delay

// ---------------------------------------------------------------- shared bits

@Composable
fun CoinBadge(progress: Progress, modifier: Modifier = Modifier) {
    Row(
        modifier.background(Color(0xCC1A3A9C), RoundedCornerShape(14.dp)).border(2.dp, Palette.gold, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🪙", fontSize = 16.sp)
        ArcadeText(" ${progress.coins}", 17.sp, Palette.gold)
    }
}

@Composable
fun BrickBadge(progress: Progress, modifier: Modifier = Modifier) {
    Row(
        modifier.background(Color(0xCC1A3A9C), RoundedCornerShape(14.dp)).border(2.dp, Color(0xFFFF8A65), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🧱", fontSize = 16.sp)
        ArcadeText(" ${progress.bricksToSpend}", 17.sp, Color(0xFFFFCCBC))
    }
}

/** Top bar for the simple screens: back button, title, coins. */
@Composable
fun ScreenTop(title: String, progress: Progress, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ArcadeButton("◀", color = Palette.slate, textSize = 18.sp, onClick = onBack)
        Spacer(Modifier.width(12.dp))
        ArcadeText(title, 26.sp, Color.White)
        Spacer(Modifier.weight(1f))
        BrickBadge(progress)
        Spacer(Modifier.width(8.dp))
        CoinBadge(progress)
    }
}

// ---------------------------------------------------------------- level start card

@Composable
fun LevelStartCard(level: Level, progress: Progress, sounds: SoundFx, onPlay: (List<Booster>) -> Unit, onClose: () -> Unit) {
    val options = listOf(Booster.PLUS_MOVES, Booster.JOKER, Booster.FREE_MOVE).filter { it.introLevel <= maxOf(level.number, 8) }
    val chosen = remember { mutableStateListOf<Booster>() }
    val canPick = level.number >= 8
    val firstFree = canPick && !progress.seen("prelevel_free")
    val world = worldOf(level.number)
    DimBox {
        Column(Modifier.width(540.dp).arcadePanel().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArcadeText("Level ${level.number}", 28.sp, Palette.gold)
                Text("  ${world.emoji} ${world.name}", color = Color.White, fontSize = 14.sp)
            }
            if (level.boss) Text("${world.villainEmoji} BOSS: ${world.villain} · ${world.bossTitle}", color = Color(0xFFFFCDD2), fontSize = 14.sp, fontWeight = Heavy)
            Text("${level.moves} moves", color = Palette.cream, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            for (o in level.targets) Row(
                Modifier.padding(vertical = 2.dp).softCard().padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(o.icon, fontSize = 20.sp)
                Text("  ${o.text}", color = Color.White, fontSize = 14.sp, fontWeight = Heavy)
            }
            // Theme goal: say what the theme means, with 3 example words as tiles.
            level.themeName()?.let { Themes.byName(it) }?.let { th -> ThemeHelp(th) }
            if (level.introBoosters.isNotEmpty()) Text(
                "New booster: " + level.introBoosters.joinToString { "${it.emoji} ${it.title}" } + " (first one free!)",
                color = Palette.green, fontSize = 13.sp, fontWeight = Heavy, modifier = Modifier.padding(top = 4.dp),
            )
            if (canPick) {
                Spacer(Modifier.height(8.dp))
                Text(if (firstFree) "NEW! Start with a booster. Your first one is FREE!" else "Start with a booster?", color = Palette.green, fontSize = 14.sp, fontWeight = Heavy)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                    for (b in options) {
                        val on = b in chosen
                        val owned = progress.count(b)
                        val price = Rewards.price(b, progress.unlocked)
                        Column(
                            Modifier
                                .background(if (on) Palette.greenDeep else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                .border(if (on) 3.dp else 1.dp, Palette.gold, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (on) chosen.remove(b)
                                    else if (firstFree && chosen.isEmpty() || owned > 0 || progress.coins >= price) chosen.add(b)
                                    else sounds.play("nope")
                                    sounds.play("tick")
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(b.emoji, fontSize = 26.sp)
                            Text(b.title, color = Color.White, fontSize = 11.sp, fontWeight = Heavy)
                            Text(if (firstFree && (chosen.isEmpty() || chosen.first() == b)) "FREE" else if (owned > 0) "x$owned" else "🪙$price", color = Palette.gold, fontSize = 11.sp, fontWeight = Heavy)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ArcadeButton("✕", color = Palette.slate, textSize = 18.sp, onClick = onClose)
                ArcadeButton("▶ PLAY", textSize = 22.sp) {
                    // Pay for the chosen boosters: free first time, then owned ones, then coins.
                    val paid = ArrayList<Booster>()
                    for ((i, b) in chosen.withIndex()) {
                        val ok = when {
                            firstFree && i == 0 -> { progress.markSeen("prelevel_free"); true }
                            progress.count(b) > 0 -> progress.useBooster(b)
                            else -> progress.spend(Rewards.price(b, progress.unlocked))
                        }
                        if (ok) paid += b
                    }
                    onPlay(paid)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- shop

/** Real-money items. Until Google Play Billing is set up these run in TEST mode (they are free and marked as test). */
data class ShopPack(val id: String, val title: String, val emoji: String, val price: String, val coins: Int, val boosters: List<Booster> = emptyList(), val once: Boolean = false, val noAds: Boolean = false)

val PACKS = listOf(
    ShopPack("starter", "Starter Pack", "🎒", "₹99", 1000, listOf(Booster.HAMMER, Booster.HAMMER, Booster.JOKER, Booster.PLUS_MOVES), once = true),
    ShopPack("no_ads", "No Ads", "🚫", "₹249", 0, noAds = true, once = true),
    ShopPack("coins_s", "Pile of coins", "🪙", "₹79", 800),
    ShopPack("coins_m", "Bag of coins", "💰", "₹199", 2400),
    ShopPack("coins_l", "Chest of coins", "🧰", "₹449", 6000),
)

@Composable
fun ShopScreen(progress: Progress, sounds: SoundFx, onBack: () -> Unit) {
    var bought by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    val activity = LocalContext.current as Activity
    val today = Rewards.boosterOfTheDay(progress.unlocked)
    LivingBackground(progress.unlocked.coerceAtMost(50)) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            ScreenTop("🛒 Shop", progress, onBack)
            Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArcadeButton("Boosters", color = if (tab == 0) Palette.royal else Palette.slate, textSize = 15.sp) { tab = 0 }
                ArcadeButton("Coins & packs", color = if (tab == 1) Palette.royal else Palette.slate, textSize = 15.sp) { tab = 1 }
                Spacer(Modifier.weight(1f))
                WatchButton("Free 25 🪙", Ads.Spot.FREE_COINS, activity) { progress.addCoins(25); bought = "+25 🪙" }
            }
            if (tab == 0) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    for (row in Booster.entries.chunked(6)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        for (b in row) {
                            val unlocked = b.introLevel <= progress.unlocked
                            val price = Rewards.price(b, progress.unlocked)
                            Column(
                                Modifier
                                    .weight(1f)
                                    .arcadePanel()
                                    .padding(6.dp)
                                    .alpha(if (unlocked) 1f else 0.5f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(b.emoji, fontSize = 26.sp)
                                Text(b.title, color = Color.White, fontSize = 12.sp, fontWeight = Heavy, textAlign = TextAlign.Center)
                                Text(if (unlocked) "You have ${progress.count(b)}" else "🔒 Level ${b.introLevel}", color = Palette.cream, fontSize = 10.sp)
                                if (b == today && unlocked) Text("½ PRICE TODAY", color = Palette.gold, fontSize = 10.sp, fontWeight = Heavy)
                                if (unlocked) ArcadeButton("🪙$price", textSize = 13.sp, enabled = progress.coins >= price) {
                                    if (progress.spend(price)) { progress.addBooster(b); sounds.play("cash"); bought = "+1 ${b.emoji} ${b.title}" }
                                }
                            }
                        }
                        repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            } else {
                Text("Test mode: purchases are free until the Play Store account is ready.", color = Palette.cream, fontSize = 12.sp)
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (p in PACKS) {
                        val done = p.once && progress.bought(p.id) || (p.noAds && progress.noAds)
                        Column(Modifier.weight(1f).arcadePanel().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(p.emoji, fontSize = 34.sp)
                            Text(p.title, color = Palette.gold, fontSize = 15.sp, fontWeight = Heavy)
                            if (p.coins > 0) Text("🪙 ${p.coins}", color = Color.White, fontSize = 14.sp, fontWeight = Heavy)
                            if (p.boosters.isNotEmpty()) Text(p.boosters.joinToString(" ") { it.emoji }, fontSize = 16.sp)
                            if (p.noAds) Text("Removes picture ads.\nVideos for rewards stay.", color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(6.dp))
                            ArcadeButton(if (done) "Owned" else p.price, enabled = !done, textSize = 15.sp) {
                                progress.addCoins(p.coins)
                                p.boosters.forEach { progress.addBooster(it) }
                                if (p.noAds) progress.setNoAds()
                                progress.markBought(p.id)
                                sounds.play("cash")
                                bought = "${p.emoji} ${p.title}!"
                            }
                        }
                    }
                }
            }
        }
        bought?.let { b ->
            LaunchedEffect(b) { delay(1400); bought = null }
            MessageBar(b)
        }
    }
}

// ---------------------------------------------------------------- wordbook and boosters guide

@Composable
fun WordbookScreen(progress: Progress, onBack: () -> Unit) {
    val words = remember { progress.wordbook() }
    LivingBackground(progress.unlocked.coerceAtMost(50)) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            ScreenTop("📖 Wordbook: ${words.size} words", progress, onBack)
            Spacer(Modifier.height(6.dp))
            if (words.isEmpty()) Text("Every new word you make is saved here, with +${Progress.NEW_WORD_COINS} coins the first time!", color = Color.White, fontSize = 16.sp)
            Column(Modifier.verticalScroll(rememberScrollState()).arcadePanel().padding(10.dp).fillMaxWidth()) {
                for ((letter, group) in words.groupBy { it.first() }) {
                    ArcadeText("$letter", 18.sp, Palette.gold)
                    Text(group.joinToString("  ·  "), color = Color.White, fontSize = 14.sp, fontWeight = Heavy)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

/** All boosters with what they do. Locked ones show the level that unlocks them. */
@Composable
fun BoostersGuide(progress: Progress, onBack: () -> Unit) {
    LivingBackground(progress.unlocked.coerceAtMost(50)) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            ScreenTop("🎒 Boosters", progress, onBack)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in Booster.entries.chunked(3)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (b in row) {
                        val open = b.introLevel <= progress.unlocked
                        Row(Modifier.weight(1f).arcadePanel().padding(8.dp).alpha(if (open) 1f else 0.6f), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (open) b.emoji else "🔒", fontSize = 28.sp)
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(b.title + if (open) "  ×${progress.count(b)}" else "", color = Palette.gold, fontSize = 14.sp, fontWeight = Heavy)
                                Text(if (open) b.description else "Unlocks at level ${b.introLevel}", color = Color.White, fontSize = 12.sp, lineHeight = 14.sp)
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** "Animals: Animals, birds and bugs" and 3 example words as small tiles. */
@Composable
fun ThemeHelp(th: com.retroraja.wordnoid.core.Themes.Theme) {
    Column(Modifier.padding(top = 6.dp).softCard().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${th.emoji} ${th.name} words: ${th.about}", color = Palette.gold, fontSize = 14.sp, fontWeight = Heavy)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
            for (w in th.examples) Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                w.forEachIndexed { i, ch -> Tile3D(com.retroraja.wordnoid.core.Tile(i.toLong(), ch), 22.dp) }
            }
        }
        Text("Like these!", color = Palette.cream, fontSize = 11.sp)
    }
}
