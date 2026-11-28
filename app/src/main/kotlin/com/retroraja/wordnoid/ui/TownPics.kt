package com.retroraja.wordnoid.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.audio.SoundFx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Select + Match update (lag fix): the town is pictures, not live 3D.
 * assets/town/<world>/: base.webp (every spot broken) + s<spot>.webp (that spot fixed, cut out) + town.json (where each goes).
 * The pictures were made from the 3D street, so it looks the same. Building is 2D: the view slides and zooms to the spot,
 * Tiko hops over, bricks fly, the building rises from the bottom, then pops with dust and stars.
 */
object TownPics {
    class Spot(val k: Int, val image: ImageBitmap?, val x: Float, val y: Float, val w: Float, val h: Float, val cx: Float, val cy: Float, val bw: Float, val bh: Float)
    class Pics(val aspect: Float, val base: ImageBitmap, val spots: List<Spot>)

    private val cache = HashMap<Int, Pics?>()

    fun cached(world: Int): Pics? = cache[world]

    suspend fun load(ctx: Context, world: Int): Pics? {
        if (cache.containsKey(world)) return cache[world]
        val p = withContext(Dispatchers.IO) {
            try {
                fun img(name: String) = ctx.assets.open("town/$world/$name").use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
                val j = org.json.JSONObject(ctx.assets.open("town/$world/town.json").bufferedReader().use { it.readText() })
                val arr = j.getJSONArray("spots")
                val spots = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    Spot(
                        o.getInt("k"), if (o.has("f")) img(o.getString("f")) else null,
                        o.optDouble("x", 0.0).toFloat(), o.optDouble("y", 0.0).toFloat(), o.optDouble("w", 0.0).toFloat(), o.optDouble("h", 0.0).toFloat(),
                        o.getDouble("cx").toFloat(), o.getDouble("cy").toFloat(), o.getDouble("bw").toFloat(), o.getDouble("bh").toFloat(),
                    )
                }
                Pics(j.getDouble("aspect").toFloat(), img(j.getString("base"))!!, spots)
            } catch (e: Exception) { null }
        }
        cache[world] = p
        return p
    }
}

/** What the town view shows right now: the camera (picture point [u],[v] at the screen centre, [zoom]), Tiko and the build. */
class TownStage {
    val u = Animatable(-1f)
    val v = Animatable(0.5f)
    val zoom = Animatable(1f)
    val tikoX = Animatable(0.1f)
    val tikoY = Animatable(0.62f)
    val tikoHop = Animatable(0f)
    var building by mutableStateOf<Int?>(null)
    val reveal = Animatable(0f)
    val ghost = Animatable(0f)
    val pop = Animatable(0f)
    var say by mutableStateOf<String?>(null)
    var banner by mutableStateOf<String?>(null)
    val parts = mutableStateListOf<Part>()
    var clock by mutableStateOf(0L)

    class Part(val x: Float, val y: Float, val vx: Float, val vy: Float, val born: Long, val life: Long, val kind: Int, val color: Color, val size: Float)

    /** [kind] 0 = dust puff, 1 = star, 2 = confetti, 3 = brick. Positions are picture fractions. */
    fun burst(x: Float, y: Float, n: Int, kind: Int, spread: Float = 0.04f) {
        val cols = listOf(Color(0xFFFFE27A), Color(0xFFFF7EB6), Color(0xFF7FE3FF), Color(0xFF8CF08C), Color.White)
        repeat(n) {
            val a = Random.nextFloat() * 6.283f
            val sp = spread * (0.4f + Random.nextFloat())
            parts += Part(
                x, y, cos(a) * sp, sin(a) * sp - (if (kind == 0) 0.01f else 0.03f), clock,
                if (kind == 2) 1600L else 700L + Random.nextLong(300), kind,
                if (kind == 0) Color(0xEEFFF7FB) else cols[it % cols.size], if (kind == 0) 0.018f + Random.nextFloat() * 0.015f else 0.008f,
            )
        }
    }
}

@Composable
fun rememberTownStage(): TownStage = remember { TownStage() }

/** The street picture with the fixed spots on top. Swipe left and right to look along the street. */
@Composable
fun TownPicture(world: Int, fixed: Set<Int>, stage: TownStage, grey: Boolean, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var pics by remember(world) { mutableStateOf(TownPics.cached(world)) }
    LaunchedEffect(world) { if (pics == null) pics = TownPics.load(ctx, world) }
    // Particles only tick while there are some (no frame loop when the town is still).
    LaunchedEffect(stage.parts.size > 0) {
        while (stage.parts.isNotEmpty()) {
            withFrameNanos { stage.clock = it / 1_000_000 }
            stage.parts.removeAll { stage.clock - it.born > it.life }
        }
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val p = pics ?: run { Box(modifier.background(Color(0xFF1D1433))); return }
    BoxWithConstraints(modifier.background(Color(0xFF1D1433))) {
        val sw = constraints.maxWidth.toFloat()
        val sh = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        // Start at the left end (where the street begins).
        LaunchedEffect(world) {
            if (stage.u.value < 0f) {
                stage.u.snapTo(sw / 2f / (p.aspect * sh))
                p.spots.firstOrNull()?.let { s0 -> stage.tikoX.snapTo((s0.x + s0.w + 0.015f).coerceAtMost(0.95f)); stage.tikoY.snapTo((s0.y + s0.h).coerceIn(0.45f, 0.92f)) }
            }
        }
        fun layout(): Triple<Float, Float, Float> {
            val ph = sh * stage.zoom.value
            val pw = ph * p.aspect
            val left = (sw / 2f - stage.u.value * pw).coerceIn(sw - pw, 0f)
            val top = (sh / 2f - stage.v.value * ph).coerceIn(sh - ph, 0f)
            return Triple(left, top, ph)
        }
        Canvas(
            Modifier.fillMaxSize().pointerInput(world) {
                detectHorizontalDragGestures { _, dx ->
                    if (stage.building != null) return@detectHorizontalDragGestures
                    val pw = sh * stage.zoom.value * p.aspect
                    val half = sw / 2f / pw
                    val nu = (stage.u.value - dx / pw).coerceIn(half, 1f - half)
                    scope.launch { stage.u.snapTo(nu) }
                }
            },
        ) {
            val (left, top, ph) = layout()
            val pw = ph * p.aspect
            val tint = if (grey) androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0.15f) }) else null
            drawImage(p.base, dstOffset = IntOffset(left.roundToInt(), top.roundToInt()), dstSize = IntSize(pw.roundToInt(), ph.roundToInt()), colorFilter = tint)
            for (s in p.spots) {
                val img = s.image ?: continue
                val building = stage.building == s.k
                if (s.k !in fixed && !building) continue
                val x = left + s.x * pw
                val y = top + s.y * ph
                val w = s.w * pw
                val h = s.h * ph
                if (building) {
                    // A see-through blue "ghost" of the building, then it rises from the bottom brick by brick.
                    if (stage.ghost.value > 0f) drawImage(
                        img, dstOffset = IntOffset(x.roundToInt(), y.roundToInt()), dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                        alpha = 0.35f * stage.ghost.value, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF8FD8FF), androidx.compose.ui.graphics.BlendMode.SrcAtop),
                    )
                    val q = 1f + sin(stage.pop.value * Math.PI.toFloat() * 3f) * (1f - stage.pop.value) * 0.08f
                    clipRect(x - w, y + h * (1f - stage.reveal.value) - 1f, x + 2 * w, y + h + 2f) {
                        withTransform({ scale(1f / kotlin.math.sqrt(q), q, Offset(x + w / 2f, y + h)) }) {
                            drawImage(img, dstOffset = IntOffset(x.roundToInt(), y.roundToInt()), dstSize = IntSize(w.roundToInt(), h.roundToInt()))
                        }
                    }
                } else drawImage(img, dstOffset = IntOffset(x.roundToInt(), y.roundToInt()), dstSize = IntSize(w.roundToInt(), h.roundToInt()), colorFilter = tint)
            }
            // Dust, stars and confetti.
            for (pt in stage.parts) {
                val k = ((stage.clock - pt.born).toFloat() / pt.life).coerceIn(0f, 1f)
                val g = if (pt.kind == 0) 0f else 0.12f
                val px = left + (pt.x + pt.vx * k * 2f) * pw
                val py = top + (pt.y + pt.vy * k * 2f + g * k * k) * ph
                val r = pt.size * ph * (if (pt.kind == 0) 1f + k * 1.6f else 1f)
                val a = (1f - k).coerceIn(0f, 1f)
                when (pt.kind) {
                    0 -> drawCircle(pt.color.copy(alpha = 0.85f * a), r, Offset(px, py))
                    2 -> drawRect(pt.color.copy(alpha = a), Offset(px - r, py - r / 2), androidx.compose.ui.geometry.Size(r * 2, r))
                    else -> drawCircle(pt.color.copy(alpha = a), r, Offset(px, py))
                }
            }
        }
        // Tiko (2D), standing on the street; hops when walking.
        val (left, top, ph) = layout()
        val pw = ph * p.aspect
        val tSize = with(density) { (ph * 0.17f).toDp() }
        val tx = left + stage.tikoX.value * pw
        val ty = top + stage.tikoY.value * ph - abs(sin(stage.tikoHop.value * Math.PI.toFloat())) * ph * 0.05f
        Tiko(
            Modifier.offset { IntOffset((tx - ph * 0.085f).roundToInt(), (ty - ph * 0.17f).roundToInt()) }.size(tSize),
        )
        stage.say?.let { line ->
            Column(Modifier.align(Alignment.TopCenter).padding(top = 64.dp).widthIn(max = 420.dp).background(Color.White, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text("TIKO", color = Color(0xFF2A8FE0), fontSize = 11.sp, fontWeight = Heavy)
                Text("“$line”", color = Color(0xFF1B2140), fontSize = 16.sp, fontWeight = Heavy, textAlign = TextAlign.Center)
            }
        }
        stage.banner?.let { b ->
            Text(
                b, color = Palette.gold, fontSize = 34.sp, fontWeight = Heavy, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).background(Color(0xCC1A1240), RoundedCornerShape(18.dp)).padding(horizontal = 24.dp, vertical = 12.dp)
                    .graphicsLayer { val s = 1f + 0.05f * sin(stage.clock / 160f); scaleX = s; scaleY = s },
            )
        }
    }
}

/** Builds [spot] with [cost] bricks: slide + zoom to it, Tiko hops over, bricks fly, it rises, pops with dust and stars. */
suspend fun TownStage.buildSpot(pics: TownPics.Pics?, spot: Int, cost: Int, line: String?, sounds: SoundFx) {
    val s = pics?.spots?.getOrNull(spot)
    if (s == null || s.image == null) { delay(600); sounds.play("fanfare"); return }
    val zoomTo = 1.55f
    sounds.play("whoosh")
    reveal.snapTo(0f); ghost.snapTo(0f); pop.snapTo(0f)
    building = spot
    coroutineScope {
        launch { u.animateTo(s.cx, tween(1000, easing = FastOutSlowInEasing)) }
        launch { v.animateTo((s.y + s.h * 0.55f).coerceIn(0.3f, 0.7f), tween(1000, easing = FastOutSlowInEasing)) }
        launch { zoom.animateTo(zoomTo, tween(1000, easing = FastOutSlowInEasing)) }
        // Tiko hops to the side of the spot.
        val toX = (s.x + s.w + 0.02f).coerceAtMost(0.97f)
        val toY = (s.y + s.h).coerceIn(0.45f, 0.92f)
        val hops = (abs(toX - tikoX.value) / 0.04f).roundToInt().coerceIn(2, 9)
        launch { tikoX.animateTo(toX, tween(hops * 220, easing = LinearEasing)) }
        launch { tikoY.animateTo(toY, tween(hops * 220, easing = LinearEasing)) }
        launch { repeat(hops) { tikoHop.snapTo(0f); sounds.play("drop"); tikoHop.animateTo(1f, tween(220, easing = LinearEasing)) }; tikoHop.snapTo(0f) }
    }
    say = line
    // Dust: the rubble is cleared.
    sounds.play("thunk")
    burst(s.x + s.w / 2f, s.y + s.h * 0.95f, 14, 0, 0.05f)
    ghost.animateTo(1f, tween(500))
    reveal.snapTo(0.06f)
    for (i in 1..cost.coerceAtLeast(1)) {
        // A brick arcs from Tiko into the building.
        val fromX = tikoX.value; val fromY = tikoY.value - 0.1f
        val toX = s.x + s.w * (0.3f + Random.nextFloat() * 0.4f)
        val toY = s.y + s.h * (1f - reveal.value).coerceIn(0.1f, 0.95f)
        val fly = Animatable(0f)
        fly.animateTo(1f, tween(380, easing = LinearEasing)) {
            parts.removeAll { it.kind == 3 }
            val k = value
            parts += TownStage.Part(fromX + (toX - fromX) * k, fromY + (toY - fromY) * k - sin(k * Math.PI.toFloat()) * 0.12f, 0f, 0f, clock, 60, 3, Color(0xFFD9563B), 0.012f)
        }
        parts.removeAll { it.kind == 3 }
        sounds.play("thunk")
        burst(toX, toY, 6, 0, 0.03f)
        reveal.animateTo(if (i == cost) 1f else 0.06f + 0.94f * i / cost, tween(420))
    }
    ghost.animateTo(0f, tween(300))
    sounds.play("pop")
    burst(s.x + s.w / 2f, s.y + s.h * 0.4f, 16, 1, 0.06f)
    pop.snapTo(0f)
    pop.animateTo(1f, tween(800, easing = LinearEasing))
    sounds.play("fanfare")
    burst(s.x + s.w / 2f, s.y, 40, 2, 0.07f)
    delay(1100)
    say = null
}

/** Back to the street view after a build. */
suspend fun TownStage.zoomOut() {
    coroutineScope {
        launch { zoom.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
        launch { v.animateTo(0.5f, tween(1100, easing = FastOutSlowInEasing)) }
    }
    building = null
}
