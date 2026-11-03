package com.retroraja.wordnoid.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Booster
import com.retroraja.wordnoid.core.Levels
import com.retroraja.wordnoid.data.Daily
import com.retroraja.wordnoid.data.Progress
import com.retroraja.wordnoid.data.Rewards
import kotlinx.coroutines.delay
import kotlin.math.sin

/** One town task: costs Letter Bricks, fixes one spot of the district, gives a reward and a story line. */
data class TownTask(val title: String, val cost: Int, val spot: Int, val story: String, val coins: Int, val booster: Booster? = null)

/** Each district has 8 tasks; the big building opens when the world's boss is beaten. */
data class District(val world: Int, val name: String, val building: String, val buildingGift: String, val signs: List<Pair<String, String>>, val tasks: List<TownTask>)

val DISTRICTS = listOf(
    District(
        0, "Candy Street", "Candy Factory", "a free booster",
        listOf("BAKREY" to "BAKERY", "SWEETS" to "SWEETS", "TYOS" to "TOYS", "CFAE" to "CAFE"),
        listOf(
            TownTask("Fix the Bakery sign", 2, 0, "Tiko: \"BAKREY? Let's put those letters back!\"", 20),
            TownTask("Paint the Bakery", 3, 0, "Tiko: \"Pink walls, fresh bread. Smells great!\"", 25, Booster.HAMMER),
            TownTask("Build the lollipop fountain", 3, 4, "Tiko: \"A fountain of fizzy pop!\"", 30),
            TownTask("Fix the Sweet Shop", 3, 1, "Tiko: \"Professor Scramble mixed up every jar!\"", 30),
            TownTask("Plant candy trees", 3, 5, "Tiko: \"Gumdrops grow on these trees.\"", 30, Booster.JOKER),
            TownTask("Open the Toy Shop", 4, 2, "Tiko: \"Teddies, trains and tops are back!\"", 35),
            TownTask("Light the street lamps", 3, 6, "Tiko: \"Now the street shines at night.\"", 35),
            TownTask("Open the Cafe", 4, 3, "Tiko: \"Hot cocoa for everyone!\"", 40, Booster.PLUS_MOVES),
        ),
    ),
    District(
        1, "Coral Harbour", "Lighthouse", "free coins",
        listOf("BAIT" to "BAIT", "FSIH" to "FISH", "SAILS" to "SAILS", "DOCKS" to "DOCKS"),
        listOf(
            TownTask("Fix the Bait Shop", 2, 0, "Tiko: \"Captain Inkbeard squirted ink everywhere!\"", 25),
            TownTask("Repair the pier", 3, 4, "Tiko: \"Strong boards for strong sailors.\"", 30, Booster.HAMMER),
            TownTask("Open the Fish Market", 3, 1, "Tiko: \"FSIH? I mean FISH! Fresh today!\"", 30),
            TownTask("Raise the sails", 3, 5, "Tiko: \"The boats can sail again!\"", 30),
            TownTask("Paint the boat house", 4, 2, "Tiko: \"Sea blue and shell white.\"", 35, Booster.MAGIC_SWAP),
            TownTask("Hang the shell lamps", 3, 6, "Tiko: \"They glow like little moons.\"", 35),
            TownTask("Fix the Dock Office", 4, 3, "Tiko: \"Every ship is back on time.\"", 40),
            TownTask("Build the sandcastle", 3, 7, "Tiko: \"The biggest one on the beach!\"", 40, Booster.PLUS_MOVES),
        ),
    ),
    District(
        2, "Jungle Park", "Tree House", "a free booster",
        listOf("JUCIE" to "JUICE", "ZOO" to "ZOO", "HUTS" to "HUTS", "MAPS" to "MAPS"),
        listOf(
            TownTask("Fix the Juice Bar", 3, 0, "Tiko: \"Bonkers drank all the mango juice!\"", 30),
            TownTask("Build the rope bridge", 3, 4, "Tiko: \"Hold on tight!\"", 30, Booster.HAMMER),
            TownTask("Open the petting zoo", 3, 1, "Tiko: \"Hello, little goats!\"", 35),
            TownTask("Plant banana palms", 3, 5, "Tiko: \"Bananas for the monkeys.\"", 35),
            TownTask("Fix the jungle huts", 4, 2, "Tiko: \"Cosy huts with leaf roofs.\"", 40, Booster.SHUFFLE),
            TownTask("Light the lanterns", 3, 6, "Tiko: \"Fireflies love these.\"", 40),
            TownTask("Open the map shop", 4, 3, "Tiko: \"Every path is on the map now.\"", 45),
            TownTask("Build the waterfall", 4, 7, "Tiko: \"Splash!\"", 45, Booster.PLUS_MOVES),
        ),
    ),
    District(
        3, "Cloud Hill", "Sky Castle", "free coins",
        listOf("KTIES" to "KITES", "RAINBOW" to "RAINBOW", "STARS" to "STARS", "BELLS" to "BELLS"),
        listOf(
            TownTask("Fix the Kite Shop", 3, 0, "Tiko: \"Grumble Cloud tangled every string!\"", 35),
            TownTask("Build the cloud steps", 3, 4, "Tiko: \"Soft as cotton candy.\"", 35, Booster.HAMMER),
            TownTask("Paint the rainbow", 3, 1, "Tiko: \"Seven colours, all in order.\"", 40),
            TownTask("Hang the wind chimes", 3, 5, "Tiko: \"Ding, ding!\"", 40),
            TownTask("Open the star shop", 4, 2, "Tiko: \"Catch a falling star!\"", 45, Booster.FREE_MOVE),
            TownTask("Light the sky lamps", 3, 6, "Tiko: \"Bright even in a storm.\"", 45),
            TownTask("Ring the bell tower", 4, 3, "Tiko: \"Everyone, come and see!\"", 50),
            TownTask("Build the balloon port", 4, 7, "Tiko: \"Up, up and away!\"", 50, Booster.PLUS_MOVES),
        ),
    ),
    District(
        4, "Star Square", "Space Station", "a free booster",
        listOf("ROCKTE" to "ROCKET", "MOON" to "MOON", "COMET" to "COMET", "ORBIT" to "ORBIT"),
        listOf(
            TownTask("Fix the Rocket Shop", 3, 0, "Tiko: \"The Word Eater ate the letters!\"", 40),
            TownTask("Build the moon bounce", 3, 4, "Tiko: \"Boing, boing!\"", 40, Booster.HAMMER),
            TownTask("Open the Moon Cafe", 4, 1, "Tiko: \"Cheese pizza, of course.\"", 45),
            TownTask("Plant star flowers", 3, 5, "Tiko: \"They twinkle!\"", 45),
            TownTask("Fix the comet ride", 4, 2, "Tiko: \"Fastest ride in the galaxy.\"", 50, Booster.MAGIC_SWAP),
            TownTask("Light the planet lamps", 3, 6, "Tiko: \"Every lamp is a little planet.\"", 50),
            TownTask("Open Orbit Hall", 4, 3, "Tiko: \"A party for the whole town!\"", 55),
            TownTask("Build the telescope", 4, 7, "Tiko: \"Look, I can see the next world!\"", 55, Booster.PLUS_MOVES),
        ),
    ),
)

/** Spots 0-3 are shops, 4-7 are decorations (fountain, trees, lamps, extra). A spot is fixed when any of its tasks is done. */
private fun spotFixed(progress: Progress, d: District, spot: Int): Boolean =
    d.tasks.withIndex().any { (i, t) -> t.spot == spot && progress.taskDone(d.world, i) }

/** The current district: the first one whose world isn't finished, or whose tasks aren't all done. */
fun currentDistrict(progress: Progress): Int =
    DISTRICTS.indexOfFirst { d -> !progress.districtOpen(d.world) || progress.tasksDone(d.world) < d.tasks.size }.let { if (it < 0) DISTRICTS.size - 1 else it }
        .coerceAtMost(((progress.unlocked - 1) / 10).coerceIn(0, DISTRICTS.size - 1))

@Composable
fun TownScreen(
    ready: Boolean,
    progress: Progress,
    sounds: SoundFx,
    onPlay: () -> Unit,
    onMap: () -> Unit,
    onShop: () -> Unit,
    onBoosters: () -> Unit,
    onWordbook: () -> Unit,
    onDaily: () -> Unit,
    onSettings: () -> Unit,
    onQuit: () -> Unit = {},
) {
    var tasksOpen by remember { mutableStateOf(false) }
    var fixing by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var buildingGift by remember { mutableStateOf<Int?>(null) }
    val current = currentDistrict(progress)
    val anim = rememberInfiniteTransition(label = "town")
    val bounce by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "bounce")
    val time by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "time")
    val nextLevel = progress.unlocked.coerceAtMost(Levels.all.size)
    val world = WORLDS[current]
    @Suppress("UNUSED_VARIABLE") val v = progress.version
    val use3d = Fx.ok
    // 3D town: one street at a time, the arrows walk to the other streets. Rebuilt after each fix.
    var view by remember { mutableIntStateOf(current) }
    var rev by remember { mutableIntStateOf(0) }
    LaunchedEffect(use3d, view, rev) { if (use3d) Fx.backdrop("town", townData(DISTRICTS[view], progress, DISTRICTS[view].world <= current)) }
    BoxWithConstraints(Modifier.fillMaxSize().then(if (use3d) Modifier else Modifier.background(Brush.verticalGradient(listOf(world.top, world.bottom))))) {
        val screenW = maxWidth
        val scroll = rememberScrollState()
        val density = LocalDensity.current
        LaunchedEffect(current) { scroll.scrollTo(with(density) { (screenW * current).toPx() }.toInt()) }
        if (use3d) {
            // Drag to turn the street a little.
            Box(Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures { _, d -> Fx.act("drag", Fx.obj("dx" to (d.x / density.density).toDouble())) }
            })
            val vd = DISTRICTS[view]
            if (vd.world > current) Text(
                "🔒 ${vd.name}\nComing after ${WORLDS[vd.world - 1].name}", Modifier.align(Alignment.Center).background(Color(0xAAFFFFFF), RoundedCornerShape(14.dp)).padding(12.dp),
                color = Color(0xFF1B2140), fontSize = 20.sp, fontWeight = Heavy, textAlign = TextAlign.Center,
            )
            if (view > 0) StreetArrow("◀", DISTRICTS[view - 1], progress, current, Modifier.align(Alignment.CenterStart).padding(start = 96.dp)) { view-- ; sounds.play("whoosh") }
            if (view < DISTRICTS.size - 1) StreetArrow("▶", DISTRICTS[view + 1], progress, current, Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)) { view++; sounds.play("whoosh") }
        }
        // The long street: one screen-wide district per world. Swipe left and right.
        if (!use3d) Row(Modifier.fillMaxSize().horizontalScroll(scroll)) {
            for (d in DISTRICTS) {
                val unlocked = d.world <= current
                DistrictView(d, progress, unlocked, time, Modifier.width(screenW).fillMaxHeight()) {
                    if (progress.buildingGiftReady(d.world)) buildingGift = d.world
                }
            }
        }
        // Buttons stay fixed while the town scrolls.
        Row(Modifier.align(Alignment.TopStart).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoinBadge(progress)
            BrickBadge(progress)
        }
        Column(Modifier.align(Alignment.TopEnd).padding(10.dp), horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (use3d) "${WORLDS[view].emoji} ${DISTRICTS[view].name}" else "${world.emoji} ${DISTRICTS[current].name}", color = Color.White, fontSize = 16.sp, fontWeight = Heavy, modifier = Modifier.background(Color(0x661A3A9C), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                Spacer(Modifier.width(8.dp))
                RoundButton("⚙", 34.dp, onClick = onSettings)
                Spacer(Modifier.width(6.dp))
                RoundButton("⏻", 34.dp, onClick = onQuit)
            }
        }
        Column(Modifier.align(Alignment.CenterStart).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val dailyReady = progress.dailyGiftDay() > 0 || progress.freeSpinReady
            SideButton("🎁", "Daily Gifts", badge = if (dailyReady) "!" else null, onClick = onDaily)
            SideButton("🛒", "Shop", onClick = onShop)
            SideButton("🎒", "Boosters", onClick = onBoosters)
        }
        Row(Modifier.align(Alignment.BottomStart).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val canBuild = DISTRICTS[current].tasks.withIndex().any { (i, t) -> !progress.taskDone(current, i) && t.cost <= progress.bricksToSpend }
            Box(Modifier.graphicsLayer { translationY = if (canBuild) -6f * bounce * density.density else 0f }) {
                SideButton("🔨", "Tasks", badge = if (canBuild) "${progress.bricksToSpend}🧱" else null) { tasksOpen = true }
            }
            SideButton("🗺️", "Level Map", onClick = onMap)
            SideButton("📖", "Wordbook", onClick = onWordbook)
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(14.dp)) {
            ArcadeButton(
                if (ready) "▶ PLAY Level $nextLevel" else "Loading…", enabled = ready, textSize = 26.sp,
                modifier = Modifier.scale(1f + 0.04f * bounce),
            ) { sounds.play("pop"); onPlay() }
        }
        if (tasksOpen) TasksDialog(DISTRICTS[current], progress, sounds, onClose = { tasksOpen = false }, onDo = { i -> tasksOpen = false; view = current; fixing = current to i })
        fixing?.let { (d, i) ->
            if (use3d) FixScene3D(DISTRICTS[d], i, sounds, onDone = { fixing = null; rev++ })
            else FixScene(DISTRICTS[d], i, sounds, onDone = { fixing = null })
        }
        buildingGift?.let { w ->
            DimBox {
                Column(Modifier.width(400.dp).arcadePanel().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ArcadeText("${DISTRICTS[w].building} gift!", 24.sp, Palette.gold)
                    val prize = remember { if (w % 2 == 0) Rewards.randomBooster(progress.unlocked) else null }
                    Text(if (prize != null) "${prize.emoji} ${prize.title}" else "🪙 50", color = Color.White, fontSize = 22.sp, fontWeight = Heavy)
                    Spacer(Modifier.height(10.dp))
                    ArcadeButton("Collect") {
                        if (prize != null) progress.addBooster(prize) else progress.addCoins(50)
                        progress.claimBuildingGift(w)
                        sounds.play("cash")
                        buildingGift = null
                    }
                }
            }
        }
    }
}

@Composable
fun SideButton(emoji: String, label: String, badge: String? = null, onClick: () -> Unit) {
    Box {
        Column(
            Modifier
                .width(78.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF4A7BFF), Palette.royalDeep)), RoundedCornerShape(16.dp))
                .border(2.dp, Palette.gold, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, fontSize = 24.sp)
            Text(label, fontSize = 11.sp, color = Color.White, fontWeight = Heavy, textAlign = TextAlign.Center)
        }
        badge?.let {
            Text(it, Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp).background(Palette.red, RoundedCornerShape(10.dp)).padding(horizontal = 5.dp), color = Color.White, fontSize = 11.sp, fontWeight = Heavy)
        }
    }
}

/** One district: 3D-style buildings on a street. Unfixed spots are grey; fixed ones are in full colour. */
@Composable
private fun DistrictView(d: District, progress: Progress, unlocked: Boolean, time: Float, modifier: Modifier, onBuilding: () -> Unit) {
    val world = WORLDS[d.world]
    val open = progress.districtOpen(d.world)
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val street = h * 0.78f
            // Sky glow and ground.
            drawRect(Brush.verticalGradient(listOf(world.top, world.bottom.lighter(0.2f)), endY = street))
            drawRect(world.ground.darker(0.9f), Offset(0f, street - h * 0.06f), Size(w, h * 0.06f))
            drawRect(Color(0xFF9AA5C9), Offset(0f, street), Size(w, h - street))
            for (i in 0 until 12) drawRect(Color.White.copy(alpha = 0.7f), Offset(w * i / 12f + 10f, street + (h - street) * 0.45f), Size(w / 24f, 5f))
            // Big building in the middle back (opens after the boss).
            drawBuilding(Offset(w * 0.4f, street - h * 0.06f), w * 0.2f, h * 0.48f, world.bottom, open && unlocked, roof = Palette.gold, time = time, big = true)
            // 4 shops.
            val xs = listOf(0.12f, 0.26f, 0.64f, 0.78f)
            val colors = listOf(Color(0xFFFF7EB6), Color(0xFF7ED957), Color(0xFFFFB84D), Color(0xFF6FC3FF))
            for (i in 0 until 4) {
                val fixed = unlocked && spotFixed(progress, d, i)
                drawBuilding(Offset(w * xs[i], street - h * 0.02f), w * 0.12f, h * (0.26f + (i % 2) * 0.05f), colors[i], fixed, roof = colors[(i + 2) % 4].darker(0.8f), time = time)
            }
            // Decorations: fountain (4), trees (5), lamps (6), extra (7).
            val fountain = unlocked && spotFixed(progress, d, 4)
            val fx = w * 0.5f
            drawOval(if (fountain) Color(0xFF6FC3FF) else Color(0xFFB0B0B0), Offset(fx - 50f, street + 6f), Size(100f, 26f))
            if (fountain) for (k in 0 until 5) {
                val t = (time * 3 + k / 5f) % 1f
                drawCircle(Color.White.copy(alpha = 1 - t), 4f, Offset(fx + (k - 2) * 12f * t, street - 40f * sin(t * 3.14f)))
            }
            val trees = unlocked && spotFixed(progress, d, 5)
            for (tx in listOf(0.05f, 0.34f, 0.92f)) {
                drawRect(Color(0xFF8D6E63).let { if (trees) it else it.grey() }, Offset(w * tx - 5f, street - 50f), Size(10f, 50f))
                drawCircle((if (trees) Color(0xFF43A047) else Color(0xFF9E9E9E)), 30f, Offset(w * tx, street - 60f))
            }
            val lamps = unlocked && spotFixed(progress, d, 6)
            for (lx in listOf(0.21f, 0.58f, 0.86f)) {
                drawRect(Color(0xFF455A64), Offset(w * lx, street - 70f), Size(5f, 70f))
                drawCircle(if (lamps) Color(0xFFFFF59D) else Color(0xFF757575), 9f, Offset(w * lx + 2f, street - 72f))
                if (lamps) drawCircle(Color(0x55FFF59D), 22f, Offset(w * lx + 2f, street - 72f))
            }
            if (unlocked && spotFixed(progress, d, 7)) {
                drawCircle(Color(0xFFFF5FA2), 16f, Offset(w * 0.7f, street - 120f - 10f * sin(time * 6.28f)))
                drawLine(Color.White, Offset(w * 0.7f, street - 104f - 10f * sin(time * 6.28f)), Offset(w * 0.7f, street - 20f), strokeWidth = 2f)
            }
            if (!unlocked) drawRect(Color(0x99FFFFFF))
        }
        // Shop signs: scrambled until fixed.
        Row(Modifier.fillMaxWidth().align(Alignment.Center).offset(y = 10.dp)) {
            val xs = listOf(0.12f, 0.26f, 0.64f, 0.78f)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                for (i in 0 until 4) {
                    val fixed = unlocked && spotFixed(progress, d, i)
                    Text(
                        if (fixed) d.signs[i].second else d.signs[i].first,
                        Modifier.offset(x = maxWidth * xs[i]).width(maxWidth * 0.12f).background(if (fixed) Color.White else Color(0xFFBDBDBD), RoundedCornerShape(6.dp)),
                        textAlign = TextAlign.Center, fontFamily = ArcadeFont, fontSize = 12.sp,
                        color = if (fixed) Color(0xFF1B2140) else Color(0xFF616161),
                    )
                }
            }
        }
        // Big building label and gift.
        Column(Modifier.align(Alignment.TopCenter).padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (open) "${d.building} 🎉" else "🔒 ${d.building}",
                color = Color.White, fontSize = 14.sp, fontWeight = Heavy,
                modifier = Modifier.background(Color(0x661A3A9C), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp),
            )
            if (!open && unlocked) Text("Beat ${world.villain} at level ${(d.world + 1) * 10} to open it", color = Color.White, fontSize = 11.sp)
            if (progress.buildingGiftReady(d.world)) Text(
                "🎁 Daily gift ready! Tap", Modifier.padding(top = 4.dp).background(Palette.red, RoundedCornerShape(8.dp)).clickable(onClick = onBuilding).padding(horizontal = 8.dp, vertical = 2.dp),
                color = Color.White, fontSize = 13.sp, fontWeight = Heavy,
            )
        }
        // Tiko, the builder, standing in the street of the current district.
        if (unlocked) Tiko(Modifier.align(Alignment.BottomCenter).offset(x = (-90).dp, y = (-24).dp).size(64.dp), outfit = world.outfit)
        if (!unlocked) Text("🔒 ${d.name}\nComing after ${WORLDS[d.world - 1].name}", Modifier.align(Alignment.Center), color = Color(0xFF1B2140), fontSize = 20.sp, fontWeight = Heavy, textAlign = TextAlign.Center)
    }
}

private fun Color.grey(): Color { val g = (red * 0.3f + green * 0.59f + blue * 0.11f); return Color(g, g, g, alpha) }

/** A cartoon 3D building: front, shaded side, roof, glowing windows, awning and a smoking chimney. */
private fun DrawScope.drawBuilding(base: Offset, bw: Float, bh: Float, color0: Color, fixed: Boolean, roof: Color, time: Float, big: Boolean = false) {
    val color = if (fixed) color0 else color0.grey()
    val roofC = if (fixed) roof else roof.grey()
    val depth = bw * 0.18f
    val top = base.y - bh
    // Shadow.
    drawOval(Color(0x33000000), Offset(base.x - 10f, base.y - 8f), Size(bw + depth + 20f, 18f))
    // Side (darker), drawn as a slanted shape.
    val side = Path().apply {
        moveTo(base.x + bw, base.y); lineTo(base.x + bw + depth, base.y - depth * 0.6f)
        lineTo(base.x + bw + depth, top - depth * 0.6f); lineTo(base.x + bw, top); close()
    }
    drawPath(side, color.darker(0.65f))
    // Front.
    drawRoundRect(Brush.verticalGradient(listOf(color.lighter(0.2f), color), startY = top, endY = base.y), Offset(base.x, top), Size(bw, bh), CornerRadius(6f))
    // Roof.
    val r = Path().apply {
        moveTo(base.x - bw * 0.08f, top); lineTo(base.x + bw / 2, top - bh * (if (big) 0.25f else 0.3f))
        lineTo(base.x + bw * 1.08f + depth, top - depth * 0.6f); lineTo(base.x + bw + depth, top - depth * 0.6f); close()
    }
    drawPath(r, roofC)
    // Chimney with smoke.
    drawRect(roofC.darker(0.7f), Offset(base.x + bw * 0.7f, top - bh * 0.28f), Size(bw * 0.1f, bh * 0.15f))
    if (fixed) for (k in 0 until 3) {
        val t = (time * 2 + k / 3f) % 1f
        drawCircle(Color.White.copy(alpha = 0.6f * (1 - t)), 6f + 10f * t, Offset(base.x + bw * 0.75f + 10f * t, top - bh * 0.3f - 50f * t))
    }
    // Windows (glowing when fixed).
    val win = if (fixed) Color(0xFFFFF59D) else Color(0xFF8A8A8A)
    val rows = if (big) 4 else 2
    for (ry in 0 until rows) for (cx in 0 until 2) {
        drawRoundRect(win, Offset(base.x + bw * (0.15f + cx * 0.45f), top + bh * (0.12f + ry * (0.6f / rows))), Size(bw * 0.25f, bh * 0.1f), CornerRadius(4f))
    }
    // Awning stripes and door.
    if (!big) for (k in 0 until 5) drawRect(if (k % 2 == 0) Color.White else roofC, Offset(base.x + bw * k / 5f, base.y - bh * 0.42f), Size(bw / 5f, bh * 0.08f))
    drawRoundRect(color.darker(0.5f), Offset(base.x + bw * 0.38f, base.y - bh * 0.28f), Size(bw * 0.24f, bh * 0.28f), CornerRadius(6f))
}

@Composable
private fun TasksDialog(d: District, progress: Progress, sounds: SoundFx, onClose: () -> Unit, onDo: (Int) -> Unit) {
    DimBox {
        Column(Modifier.width(560.dp).fillMaxHeight(0.92f).arcadePanel().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArcadeText("🔨 ${d.name} tasks", 22.sp, Palette.gold)
                Spacer(Modifier.weight(1f))
                BrickBadge(progress)
            }
            Text("Win levels to earn Letter Bricks 🧱, then fix up the town!", color = Color.White, fontSize = 12.sp)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                d.tasks.forEachIndexed { i, t ->
                    val done = progress.taskDone(d.world, i)
                    Row(Modifier.fillMaxWidth().softCard(if (done) 0.12f else 0.22f).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (done) "✅" else "🧱${t.cost}", color = Color.White, fontSize = 15.sp, fontWeight = Heavy, modifier = Modifier.width(56.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, color = Color.White, fontSize = 15.sp, fontWeight = Heavy)
                            Text("Reward: 🪙${t.coins}" + (t.booster?.let { " + ${it.emoji}" } ?: ""), color = Palette.gold, fontSize = 12.sp)
                        }
                        if (!done) ArcadeButton("Build", enabled = progress.bricksToSpend >= t.cost, textSize = 14.sp) {
                            if (progress.doTask(d.world, i, t.cost)) {
                                progress.addCoins(t.coins)
                                t.booster?.let { progress.addBooster(it) }
                                sounds.play("thunk")
                                onDo(i)
                            }
                        }
                    }
                }
            }
            ArcadeButton("Close", color = Palette.slate, textSize = 16.sp, modifier = Modifier.align(Alignment.CenterHorizontally), onClick = onClose)
        }
    }
}

/** Unfinished tasks in a street the player can build in. */
private fun openTasks(d: District, progress: Progress, current: Int): Int =
    if (d.world > current) 0 else d.tasks.indices.count { !progress.taskDone(d.world, it) }

/** Arrow at the screen edge to walk to the next street, with how many tasks are waiting there. */
@Composable
private fun StreetArrow(arrow: String, d: District, progress: Progress, current: Int, modifier: Modifier, onClick: () -> Unit) {
    val n = openTasks(d, progress, current)
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(arrow, color = Color.White, fontSize = 30.sp, modifier = Modifier.background(Color(0x881A3A9C), CircleShape).padding(10.dp))
        Text(d.name, color = Color.White, fontSize = 11.sp, fontWeight = Heavy, modifier = Modifier.background(Color(0x881A3A9C), RoundedCornerShape(6.dp)).padding(horizontal = 4.dp))
        if (n > 0) Text("🔨 $n tasks", color = Color.White, fontSize = 11.sp, fontWeight = Heavy, modifier = Modifier.padding(top = 2.dp).background(Palette.red, RoundedCornerShape(6.dp)).padding(horizontal = 4.dp))
    }
}

private val SHOP_COLORS = listOf("#ff7eb6", "#7ed957", "#ffb84d", "#6fc3ff")

/** What the 3D street shows: shops are grey until their task is built; the big building gets its colours when all 8 tasks are done. */
private fun townData(d: District, progress: Progress, unlocked: Boolean): org.json.JSONObject {
    val w = WORLDS[d.world]
    val shops = org.json.JSONArray()
    for (i in 0 until 4) shops.put(Fx.obj(
        "name" to d.signs[i].second, "bad" to d.signs[i].first, "fixed" to (unlocked && spotFixed(progress, d, i)),
        "col" to SHOP_COLORS[i], "roof" to SHOP_COLORS[(i + 2) % 4],
    ))
    val allDone = unlocked && progress.tasksDone(d.world) >= d.tasks.size
    return Fx.obj(
        "shops" to shops,
        "big" to Fx.obj("name" to d.building, "fixed" to allDone, "label" to if (allDone) null else d.building, "col" to Fx.hex(w.bottom), "roof" to "#ffc93c"),
        "deco" to Fx.obj("fountain" to (unlocked && spotFixed(progress, d, 4)), "trees" to (unlocked && spotFixed(progress, d, 5)),
            "lamps" to (unlocked && spotFixed(progress, d, 6)), "extra" to (unlocked && spotFixed(progress, d, 7))),
        "skyTop" to Fx.hex(w.top), "sky" to Fx.hex(w.bottom), "grass" to Fx.hex(w.ground),
    )
}

/** 3D: Tiko hops over, bricks fly in, the colours and the sign come back. Then the story card. */
@Composable
private fun FixScene3D(d: District, task: Int, sounds: SoundFx, onDone: () -> Unit) {
    val t = d.tasks[task]
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        if (t.spot < 4) {
            Fx.act("fix", Fx.obj("spot" to t.spot))
            Fx.await("townFixDone", 9000)
        } else {
            Fx.act("cheer")
            sounds.play("fanfare")
            delay(900)
        }
        step = 1
    }
    if (step == 0) Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) {})
    else DimBox(0.35f) {
        Column(Modifier.width(440.dp).arcadePanel().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ArcadeText(t.title, 22.sp, Palette.gold)
            Text(t.story, color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center)
            Text("+🪙${t.coins}" + (t.booster?.let { "  +${it.emoji} ${it.title}" } ?: ""), color = Palette.gold, fontSize = 18.sp, fontWeight = Heavy)
            Spacer(Modifier.height(8.dp))
            ArcadeButton("Yay!", onClick = onDone)
        }
    }
}

/** Tiko runs over and fixes the spot: hammering, sparkles, grey turns to colour, then a story line and the reward. */
@Composable
private fun FixScene(d: District, task: Int, sounds: SoundFx, onDone: () -> Unit) {
    val t = d.tasks[task]
    var step by remember { mutableIntStateOf(0) }
    val run = remember { Animatable(-300f) }
    LaunchedEffect(Unit) {
        run.animateTo(0f, tween(600))
        repeat(5) { sounds.play("thunk"); delay(380) }
        sounds.play("fanfare")
        step = 1
    }
    val anim = rememberInfiniteTransition(label = "fix")
    val hit by anim.animateFloat(-20f, 20f, infiniteRepeatable(tween(190), RepeatMode.Reverse), label = "hit")
    DimBox(0.35f) {
        Column(Modifier.width(460.dp).arcadePanel().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ArcadeText(t.title, 22.sp, Palette.gold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tiko(Modifier.size(80.dp).graphicsLayer { translationX = run.value; rotationZ = if (step == 0) hit * 0.3f else 0f })
                Text(if (step == 0) "💥" else "✨🏠✨", fontSize = 40.sp, modifier = Modifier.scale(if (step == 0) 1f + hit / 100f else 1.2f))
            }
            if (step == 1) {
                Text(t.story, color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center)
                Text("+🪙${t.coins}" + (t.booster?.let { "  +${it.emoji} ${it.title}" } ?: ""), color = Palette.gold, fontSize = 18.sp, fontWeight = Heavy)
                Spacer(Modifier.height(8.dp))
                ArcadeButton("Yay!", onClick = onDone)
            } else Text("Tiko is building…", color = Color.White, fontSize = 14.sp)
        }
    }
}

/** "Leave Wordnoid?" when Back is pressed (or Quit tapped) on the home screen. */
@Composable
fun LeaveDialog(onYes: () -> Unit, onNo: () -> Unit) {
    DimBox {
        Column(Modifier.width(360.dp).arcadePanel().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Tiko(Modifier.size(64.dp))
            ArcadeText("Leave Wordnoid?", 24.sp, Palette.gold)
            Text("Tiko will miss you! Your town and levels are saved.", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ArcadeButton("Yes", color = Color(0xFFE5533D), onClick = onYes)
                ArcadeButton("Cancel", onClick = onNo)
            }
        }
    }
}
