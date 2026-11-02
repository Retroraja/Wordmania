package com.retroraja.wordnoid.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retroraja.wordnoid.R
import kotlin.math.cos
import kotlin.math.sin

object Palette {
    val gold = Color(0xFFFFC93C)
    val goldDeep = Color(0xFFE09A12)
    val green = Color(0xFF39E07A)
    val greenDeep = Color(0xFF1C9A4E)
    val cream = Color(0xFFFFF8EC)
    val red = Color(0xFFE5533D)
    val pink = Color(0xFFFF6FA8)
    val purple = Color(0xFF7B4DFF)
    val cyan = Color(0xFF3FD8FF)
    val ice = Color(0xFFBFEFFF)
    /** Royal blue: buttons, panels and the app icon. */
    val royal = Color(0xFF2453D6)
    val royalDeep = Color(0xFF14307F)
    val tiko = Color(0xFF3FA9FF)
    /** Grey-blue for "back" and "close" buttons. */
    val slate = Color(0xFF5468A8)
}

/** One of the 5 worlds (10 levels each). Bright cartoon colours, no wood anywhere. */
data class World(
    val index: Int,
    val name: String,
    val emoji: String,
    val top: Color,
    val bottom: Color,
    val ground: Color,
    val villain: String,
    val villainEmoji: String,
    val bossTitle: String,
    val district: String,
    val outfit: String,
)

val WORLDS = listOf(
    World(0, "Candy Land", "🍭", Color(0xFFFFB3D9), Color(0xFFB66BFF), Color(0xFFFF8CC6), "Professor Scramble", "🧙", "Sugar Rush Showdown", "Candy Street", "🍬"),
    World(1, "Under the Sea", "🐠", Color(0xFF7FE3FF), Color(0xFF1D5FBF), Color(0xFFF2D68A), "Captain Inkbeard", "🐙", "Deep Sea Den", "Coral Harbour", "🤿"),
    World(2, "Fruit Jungle", "🍍", Color(0xFFB8F27A), Color(0xFF1E8F5A), Color(0xFF7BC94A), "Bonkers the Baboon", "🐒", "Jungle Rumble", "Jungle Park", "🌴"),
    World(3, "Cloud Kingdom", "☁️", Color(0xFFCDEBFF), Color(0xFF6F8CFF), Color(0xFFFFFFFF), "Grumble Cloud", "🌩️", "Thunder Tower", "Cloud Hill", "🪁"),
    World(4, "Sunny Galaxy", "🪐", Color(0xFFE6C8FF), Color(0xFF6A4BD8), Color(0xFFB79BFF), "Word Eater", "👾", "The Final Page", "Star Square", "🚀"),
)

fun worldOf(level: Int): World = WORLDS[((level - 1) / 10).coerceIn(0, WORLDS.size - 1)]

fun chapterColors(level: Int): Pair<Color, Color> = worldOf(level).let { it.top to it.bottom }

val CHAPTER_NAMES = WORLDS.map { it.name }

/** Lilita One: tiles, titles and big numbers. */
val ArcadeFont = FontFamily(Font(R.font.lilita_one))
/** Fredoka: all small and instruction text. */
val BodyFont = FontFamily(Font(R.font.fredoka, FontWeight.Normal), Font(R.font.fredoka_semibold, FontWeight.SemiBold), Font(R.font.fredoka_semibold, FontWeight.Bold), Font(R.font.fredoka_semibold, FontWeight.Black))
val Heavy = FontWeight.Bold

/**
 * A living background in the world's colours: slowly turning sunbeams, floating sparkles,
 * and a soft rolling ground with the world's decorations (candy, bubbles, leaves, clouds, stars).
 */
@Composable
fun LivingBackground(level: Int, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val world = worldOf(level)
    val anim = rememberInfiniteTransition(label = "bg")
    val turn by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "turn")
    val drift by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "drift")
    val glow by anim.animateFloat(0.55f, 1f, infiniteRepeatable(tween(3_000), RepeatMode.Reverse), label = "glow")
    val motes = remember { List(26) { i -> Triple((i * 0.137f + 0.05f) % 1f, (i * 0.311f + 0.2f) % 1f, 2f + (i % 4) * 1.5f) } }
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(world.top, world.bottom)))) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width * 0.5f, -size.height * 0.15f)
            rotate(turn, c) {
                for (i in 0 until 12) {
                    val a = Math.toRadians(i * 30.0)
                    val a2 = Math.toRadians(i * 30.0 + 11)
                    val r = size.width * 1.4f
                    val path = Path().apply {
                        moveTo(c.x, c.y)
                        lineTo(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
                        lineTo(c.x + (r * cos(a2)).toFloat(), c.y + (r * sin(a2)).toFloat())
                        close()
                    }
                    drawPath(path, Color.White.copy(alpha = 0.08f * glow))
                }
            }
            drawCircle(Brush.radialGradient(listOf(Color(0x66FFFFFF), Color.Transparent), center = c, radius = size.width * 0.6f), radius = size.width * 0.6f, center = c)
            drawWorldDecor(world, drift)
            // Rolling hills at the bottom.
            val hill = Path().apply {
                moveTo(0f, size.height)
                var x = 0f
                lineTo(0f, size.height * 0.84f)
                while (x <= size.width) { lineTo(x, size.height * 0.84f - 18f * sin(x / 160f) - 10f * sin(x / 57f)); x += 12f }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(hill, world.ground.copy(alpha = 0.75f))
            for ((x0, y0, r) in motes) {
                val y = ((y0 - drift + 1f) % 1f) * size.height * 0.8f
                val x = x0 * size.width + 14f * sin((drift * 6.28f) + x0 * 10f)
                drawCircle(Color.White.copy(alpha = 0.25f + 0.3f * glow), r, Offset(x, y))
            }
        }
        content()
    }
}

/** Little moving decorations for each world, drawn in the background. */
private fun DrawScope.drawWorldDecor(world: World, t: Float) {
    val w = size.width
    val h = size.height
    when (world.index) {
        0 -> for (i in 0 until 7) { // lollipops on the hills
            val x = w * (0.06f + i * 0.15f)
            val y = h * 0.78f
            drawLine(Color.White, Offset(x, y), Offset(x, y - h * 0.1f), strokeWidth = 5f)
            drawCircle(listOf(Color(0xFFFF5FA2), Color(0xFF6BD6FF), Color(0xFFFFD84A))[i % 3], h * 0.035f, Offset(x, y - h * 0.12f))
            drawCircle(Color.White.copy(alpha = 0.5f), h * 0.015f, Offset(x - 4f, y - h * 0.13f))
        }
        1 -> for (i in 0 until 14) { // bubbles rising
            val x = w * ((i * 0.071f + 0.03f) % 1f) + 10f * sin(t * 12f + i)
            val y = h * (1f - ((t * (0.6f + (i % 3) * 0.2f) + i * 0.13f) % 1f))
            drawCircle(Color.White.copy(alpha = 0.35f), 6f + (i % 4) * 4f, Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        }
        2 -> for (i in 0 until 8) { // big leaves at the top
            val x = w * (i / 7f)
            drawOval(Color(0xFF2FA35A).copy(alpha = 0.6f), Offset(x - 50f, -30f + 8f * sin(t * 6.28f + i)), Size(110f, 70f))
        }
        3 -> for (i in 0 until 5) { // drifting clouds
            val x = ((t + i * 0.21f) % 1.2f) * w - w * 0.1f
            val y = h * (0.12f + (i % 3) * 0.12f)
            for (k in 0..2) drawCircle(Color.White.copy(alpha = 0.7f), 26f + k * 6f, Offset(x + k * 30f, y + (if (k == 1) -12f else 0f)))
        }
        else -> for (i in 0 until 20) { // twinkling stars and a ringed planet
            val a = 0.3f + 0.7f * ((sin(t * 20f + i) + 1f) / 2f)
            drawCircle(Color.White.copy(alpha = a), 2.5f, Offset(w * ((i * 0.173f) % 1f), h * ((i * 0.291f) % 0.6f)))
            if (i == 0) {
                drawCircle(Color(0xFFFFB86B), h * 0.07f, Offset(w * 0.86f, h * 0.2f))
                drawOval(Color(0xFFFFE3A8), Offset(w * 0.86f - h * 0.12f, h * 0.2f - h * 0.02f), Size(h * 0.24f, h * 0.04f), style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
            }
        }
    }
}

/** One bright colour for each letter, A to Z. White letters read well on all of them. */
val LetterColors: Map<Char, Color> = listOf(
    0xFFE53935, 0xFF1E88E5, 0xFF8E24AA, 0xFF43A047, 0xFFFB8C00, 0xFF00897B, 0xFFD81B60, 0xFF3949AB,
    0xFFF4511E, 0xFF6D4C41, 0xFF7CB342, 0xFF5E35B1, 0xFF00ACC1, 0xFFC0CA33, 0xFFEC407A, 0xFF546E7A,
    0xFF8D6E63, 0xFF039BE5, 0xFFAB47BC, 0xFF26A69A, 0xFFFFA000, 0xFF5C6BC0, 0xFF9CCC65, 0xFFEF5350,
    0xFF29B6F6, 0xFF7E57C2,
).mapIndexed { i, c -> ('A' + i) to Color(c) }.toMap()

fun letterColor(c: Char): Color = LetterColors[c.uppercaseChar()] ?: Color.Gray

fun Color.darker(f: Float = 0.7f) = Color(red * f, green * f, blue * f, alpha)
fun Color.lighter(f: Float = 0.3f) = Color(red + (1 - red) * f, green + (1 - green) * f, blue + (1 - blue) * f, alpha)

/** A bright cartoon panel: royal blue glass with a gold rim. Used for the side panels and dialogs. */
fun Modifier.arcadePanel(): Modifier = this
    .background(Brush.verticalGradient(listOf(Color(0xEE3566E8), Color(0xEE1A3A9C))), RoundedCornerShape(18.dp))
    .border(3.dp, Brush.verticalGradient(listOf(Palette.gold, Palette.goldDeep)), RoundedCornerShape(18.dp))

/** A white rounded card for lighter content (goals, lists). */
fun Modifier.softCard(alpha: Float = 0.22f): Modifier = this
    .background(Color.White.copy(alpha = alpha), RoundedCornerShape(12.dp))

@Suppress("unused")
private fun DrawScope.roundBox(color: Color, at: Offset, s: Size, r: Float) = drawRoundRect(color, at, s, CornerRadius(r))
