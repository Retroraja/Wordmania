package com.retroraja.wordnoid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.sin

object Palette {
    val woodDark = Color(0xFF4A2A14)
    val wood = Color(0xFF7A4A24)
    val woodLight = Color(0xFFA8703A)
    val woodGrain = Color(0x33000000)
    val frame = Color(0xFF3A1F0C)
    val gold = Color(0xFFFFC93C)
    val goldDeep = Color(0xFFE09A12)
    val green = Color(0xFF39E07A)
    val greenDeep = Color(0xFF1C9A4E)
    val cream = Color(0xFFFFF4DE)
    val panel = Color(0xCC2A160A)
    val red = Color(0xFFE5533D)
}

val ArcadeFont = FontFamily.SansSerif
val Heavy = FontWeight.Black

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

/** Polished wood background with grain lines. */
@Composable
fun WoodBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.wood, Palette.woodDark)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val lines = 26
            for (i in 0 until lines) {
                val y0 = size.height * i / lines
                val path = Path()
                path.moveTo(0f, y0)
                var x = 0f
                while (x <= size.width) {
                    val y = y0 + 6f * sin(x / 90f + i * 1.7f) + 3f * sin(x / 31f + i)
                    path.lineTo(x, y)
                    x += 12f
                }
                drawPath(path, Palette.woodGrain, style = Stroke(width = if (i % 3 == 0) 2.5f else 1.2f))
            }
            // Soft light from the top, like a polished cabinet.
            drawRect(Brush.radialGradient(listOf(Color(0x33FFE9C0), Color.Transparent), center = Offset(size.width / 2, 0f), radius = size.width * 0.7f))
        }
        content()
    }
}

/** A dark glossy panel with a gold rim, used for the side panels and dialogs. */
fun Modifier.arcadePanel(): Modifier = this
    .background(Brush.verticalGradient(listOf(Color(0xE6331B0B), Color(0xE61E0F05))), RoundedCornerShape(16.dp))
    .border(2.dp, Brush.verticalGradient(listOf(Palette.gold, Palette.goldDeep)), RoundedCornerShape(16.dp))
