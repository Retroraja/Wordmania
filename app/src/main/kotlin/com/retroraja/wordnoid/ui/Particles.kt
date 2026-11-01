package com.retroraja.wordnoid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class Shape { DOT, CONFETTI, STAR, SPARK, COIN, SHARD, PLANK, RING }

class Particle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val color: Color, val size: Float, val shape: Shape,
    val life: Float, val gravity: Float = 900f, var rot: Float = 0f, val spin: Float = 0f,
    /** For coins: where they fly to. */
    val target: Offset? = null,
) {
    var age = 0f
}

/** All the bursts, sparks, confetti and fireworks. Positions are in pixels of the full screen. */
class ParticleField {
    val items = ArrayList<Particle>()
    var frame by mutableLongStateOf(0L)
    private val rnd = Random(42)
    var onCoinArrive: (() -> Unit)? = null

    fun active() = items.isNotEmpty()

    private fun angle() = rnd.nextFloat() * 6.2832f

    /** Tiles bursting: confetti in the tile's colour, white sparks and a few stars. */
    fun burst(c: Offset, color: Color, power: Float = 1f) {
        repeat((16 * power).toInt()) {
            val a = angle(); val sp = (250 + rnd.nextFloat() * 450) * power
            items += Particle(c.x, c.y, cos(a) * sp, sin(a) * sp - 250, if (rnd.nextBoolean()) color else color.lighter(0.4f), 7f + rnd.nextFloat() * 7, Shape.CONFETTI, 0.9f + rnd.nextFloat() * 0.5f, 1100f, angle(), (rnd.nextFloat() - 0.5f) * 900)
        }
        repeat((8 * power).toInt()) {
            val a = angle(); val sp = 350 + rnd.nextFloat() * 400
            items += Particle(c.x, c.y, cos(a) * sp, sin(a) * sp, Color.White, 3f + rnd.nextFloat() * 3, Shape.SPARK, 0.45f, 300f)
        }
        repeat((3 * power).toInt()) {
            val a = angle(); val sp = 150 + rnd.nextFloat() * 250
            items += Particle(c.x, c.y, cos(a) * sp, sin(a) * sp - 200, Palette.gold, 10f + rnd.nextFloat() * 6, Shape.STAR, 1.1f, 500f, 0f, 300f)
        }
        items += Particle(c.x, c.y, 0f, 0f, Color.White, 10f, Shape.RING, 0.35f, 0f)
    }

    fun sparkle(c: Offset, color: Color = Palette.gold, n: Int = 10) {
        repeat(n) {
            val a = angle(); val sp = 80 + rnd.nextFloat() * 220
            items += Particle(c.x, c.y, cos(a) * sp, sin(a) * sp, color, 6f + rnd.nextFloat() * 6, Shape.STAR, 0.8f, 100f, 0f, 200f)
        }
    }

    fun splinters(c: Offset) {
        repeat(14) {
            val a = angle(); val sp = 200 + rnd.nextFloat() * 400
            items += Particle(c.x, c.y, cos(a) * sp, sin(a) * sp - 300, Color(0xFF9C6A35), 12f + rnd.nextFloat() * 10, Shape.PLANK, 1.0f, 1400f, angle(), (rnd.nextFloat() - 0.5f) * 1200)
        }
    }

    fun shards(c: Offset) {
        repeat(16) {
            val a = angle(); val sp = 200 + rnd.nextFloat() * 450
            items += Particle(c.x, c.y, cos(a) * sp, sin(a) * sp - 200, if (rnd.nextBoolean()) Palette.ice else Color.White, 8f + rnd.nextFloat() * 8, Shape.SHARD, 0.9f, 1300f, angle(), (rnd.nextFloat() - 0.5f) * 1000)
        }
    }

    fun links(c: Offset) {
        repeat(10) {
            val a = angle(); val sp = 200 + rnd.nextFloat() * 300
            items += Particle(c.x, c.y, cos(a) * sp, sin(a) * sp - 250, Color(0xFFB0BEC5), 9f, Shape.RING, 0.9f, 1300f)
        }
    }

    fun coins(from: Offset, to: Offset, n: Int) {
        repeat(n.coerceAtMost(12)) {
            val a = angle(); val sp = 150 + rnd.nextFloat() * 250
            items += Particle(from.x, from.y, cos(a) * sp, sin(a) * sp - 200, Palette.gold, 11f, Shape.COIN, 1.6f + it * 0.05f, 0f, target = to)
        }
    }

    /** One firework: a rising rocket and a big coloured bloom. */
    fun firework(at: Offset) {
        val colors = listOf(Palette.gold, Palette.green, Palette.pink, Palette.cyan, Palette.purple, Color.White, Palette.red)
        val c = colors[rnd.nextInt(colors.size)]
        val c2 = colors[rnd.nextInt(colors.size)]
        repeat(46) { i ->
            val a = i / 46f * 6.2832f; val sp = 380 + rnd.nextFloat() * 140
            items += Particle(at.x, at.y, cos(a) * sp, sin(a) * sp, if (i % 3 == 0) c2 else c, 5f + rnd.nextFloat() * 3, Shape.DOT, 1.3f + rnd.nextFloat() * 0.4f, 260f)
        }
        repeat(14) {
            val a = angle(); val sp = 120 + rnd.nextFloat() * 160
            items += Particle(at.x, at.y, cos(a) * sp, sin(a) * sp, Color.White, 3f, Shape.SPARK, 0.6f, 200f)
        }
        items += Particle(at.x, at.y, 0f, 0f, c.lighter(0.5f), 20f, Shape.RING, 0.45f, 0f)
    }

    /** Confetti raining over the whole screen (wins). */
    fun confettiRain(width: Float, n: Int = 70) {
        val colors = listOf(Palette.gold, Palette.green, Palette.pink, Palette.cyan, Palette.purple, Palette.red)
        repeat(n) {
            items += Particle(rnd.nextFloat() * width, -20f - rnd.nextFloat() * 300, (rnd.nextFloat() - 0.5f) * 120, 150f + rnd.nextFloat() * 200, colors[rnd.nextInt(colors.size)], 8f + rnd.nextFloat() * 8, Shape.CONFETTI, 3.5f, 120f, angle(), (rnd.nextFloat() - 0.5f) * 700)
        }
    }

    fun step(dt: Float) {
        val it = items.iterator()
        var arrived = 0
        while (it.hasNext()) {
            val p = it.next()
            p.age += dt
            if (p.target != null) {
                // Coins: a short scatter, then they zoom to the counter.
                if (p.age > 0.35f) {
                    val dx = p.target.x - p.x; val dy = p.target.y - p.y
                    val k = 6f + p.age * 10f
                    p.vx = dx * k; p.vy = dy * k
                    if (dx * dx + dy * dy < 400f) { arrived++; it.remove(); continue }
                } else { p.vx *= 0.92f; p.vy *= 0.92f }
            } else {
                p.vy += p.gravity * dt
                p.vx *= 0.99f
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rot += p.spin * dt
            if (p.age >= p.life) it.remove()
        }
        if (arrived > 0) onCoinArrive?.invoke()
        frame++
    }
}

@Composable
fun ParticleLayer(field: ParticleField, modifier: Modifier = Modifier) {
    LaunchedEffect(field) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                if (field.active()) field.step(dt) else last = 0L
            }
        }
    }
    Canvas(modifier.fillMaxSize()) {
        @Suppress("UNUSED_VARIABLE") val f = field.frame
        for (p in field.items) {
            val fade = (1f - p.age / p.life).coerceIn(0f, 1f)
            val alpha = if (p.shape == Shape.COIN) 1f else fade
            val col = p.color.copy(alpha = p.color.alpha * alpha)
            when (p.shape) {
                Shape.DOT -> drawCircle(col, p.size * (0.5f + fade * 0.5f), Offset(p.x, p.y))
                Shape.SPARK -> drawLine(col, Offset(p.x, p.y), Offset(p.x - p.vx * 0.03f, p.y - p.vy * 0.03f), strokeWidth = p.size)
                Shape.CONFETTI, Shape.PLANK, Shape.SHARD -> rotate(p.rot, Offset(p.x, p.y)) {
                    val w = if (p.shape == Shape.PLANK) p.size * 1.8f else p.size
                    val h = if (p.shape == Shape.PLANK) p.size * 0.5f else p.size * 0.6f
                    if (p.shape == Shape.SHARD) {
                        val path = Path().apply { moveTo(p.x, p.y - h); lineTo(p.x + w * 0.5f, p.y + h); lineTo(p.x - w * 0.5f, p.y + h * 0.6f); close() }
                        drawPath(path, col)
                    } else drawRect(col, Offset(p.x - w / 2, p.y - h / 2), Size(w, h))
                }
                Shape.STAR -> rotate(p.rot, Offset(p.x, p.y)) {
                    val path = Path()
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) p.size else p.size * 0.45f
                        val a = i * 0.6283f - 1.5708f
                        val x = p.x + r * cos(a); val y = p.y + r * sin(a)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    drawPath(path, col)
                }
                Shape.COIN -> {
                    drawCircle(Color(0xFFB8860B), p.size, Offset(p.x, p.y + 2f))
                    drawCircle(Palette.gold, p.size, Offset(p.x, p.y))
                    drawCircle(Color(0xFFFFF1A6), p.size * 0.45f, Offset(p.x - p.size * 0.2f, p.y - p.size * 0.2f))
                }
                Shape.RING -> drawCircle(col, p.size + (1f - fade) * 90f, Offset(p.x, p.y), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f * fade + 1f))
            }
        }
    }
}
