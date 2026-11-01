package com.retroraja.wordnoid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.core.PrizeKind
import com.retroraja.wordnoid.core.Tile
import com.retroraja.wordnoid.core.TileKind

private val LetterShadow = Shadow(Color(0x99000000), Offset(0f, 3f), 4f)

/** Base colour of a tile's face. */
fun tileColor(t: Tile): Color = when (t.kind) {
    TileKind.CRATE -> Color(0xFFB07A3E)
    TileKind.JOKER -> Color(0xFFFF4FA3)
    else -> letterColor(t.letter)
}

/**
 * A thick, glossy 3D tile: a soft shadow, a darker side for depth, a shiny top face with a highlight,
 * and a bold white letter. Gold, crate, ice, chain, joker, gift and lock each add their own look.
 */
@Composable
fun Tile3D(tile: Tile, size: Dp, modifier: Modifier = Modifier, glow: Color? = null, dim: Boolean = false) {
    val sizePx = size.value
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawTile(tile, glow, dim) }
        if (tile.kind != TileKind.CRATE) {
            val text = if (tile.isJoker) "★" else tile.letter.toString()
            Text(
                text,
                modifier = Modifier.offset(y = -(size * 0.05f)),
                style = TextStyle(
                    fontFamily = ArcadeFont, fontWeight = Heavy, color = if (tile.isGold) Color(0xFFFFF3B0) else Color.White,
                    fontSize = (sizePx * 0.52f).sp, shadow = LetterShadow,
                ),
            )
        } else {
            val icon = when (tile.prize?.kind) {
                PrizeKind.COINS -> "🪙"
                PrizeKind.MOVES -> "➕"
                PrizeKind.BOOSTER -> "🎁"
                null -> ""
            }
            Text(icon, modifier = Modifier.offset(y = -(size * 0.05f)), style = TextStyle(fontSize = (sizePx * 0.32f).sp, color = Color.White.copy(alpha = 0.55f)))
        }
        if (tile.locked) Text("🔒", Modifier.align(Alignment.TopEnd), style = TextStyle(fontSize = (sizePx * 0.24f).sp))
        if (tile.isGift) Text("🎀", Modifier.align(Alignment.TopCenter).offset(y = -(size * 0.08f)), style = TextStyle(fontSize = (sizePx * 0.3f).sp))
    }
}

fun DrawScope.drawTile(t: Tile, glow: Color?, dim: Boolean) {
    val w = size.width
    val h = size.height
    val depth = h * 0.09f
    val r = CornerRadius(w * 0.18f)
    val base = tileColor(t).let { if (dim) it.darker(0.5f) else it }
    val inset = w * 0.04f
    val faceH = h - depth - inset * 2
    // Glow ring (selected, hint, word).
    if (glow != null) {
        drawRoundRect(glow.copy(alpha = 0.55f), Offset(-w * 0.06f, -h * 0.06f), Size(w * 1.12f, h * 1.12f), CornerRadius(w * 0.24f))
    }
    // Shadow on the board.
    drawRoundRect(Color(0x55000000), Offset(inset, inset + depth * 1.6f), Size(w - inset * 2, faceH), r)
    // Side (depth).
    drawRoundRect(base.darker(0.55f), Offset(inset, inset + depth), Size(w - inset * 2, faceH), r)
    // Top face.
    val face = if (t.isJoker) Brush.linearGradient(listOf(Color(0xFFFF4FA3), Color(0xFFFFC93C), Color(0xFF39E07A), Color(0xFF3FD8FF), Color(0xFF7B4DFF)), Offset.Zero, Offset(w, h))
    else Brush.verticalGradient(listOf(base.lighter(0.28f), base, base.darker(0.85f)), startY = inset, endY = inset + faceH)
    drawRoundRect(face, Offset(inset, inset), Size(w - inset * 2, faceH), r)
    // Gloss highlight.
    drawRoundRect(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.04f)), startY = inset, endY = inset + faceH * 0.5f),
        Offset(inset + w * 0.07f, inset + h * 0.04f), Size(w - inset * 2 - w * 0.14f, faceH * 0.42f), CornerRadius(w * 0.14f),
    )
    when (t.kind) {
        TileKind.GOLD -> {
            drawRoundRect(
                Brush.linearGradient(listOf(Color(0xFFFFF1A6), Color(0xFFFFC93C), Color(0xFFB8860B), Color(0xFFFFE680)), Offset.Zero, Offset(w, h)),
                Offset(inset, inset), Size(w - inset * 2, faceH), r, style = Stroke(width = w * 0.09f),
            )
            drawCircle(Color.White.copy(alpha = 0.9f), w * 0.04f, Offset(w * 0.78f, h * 0.2f))
        }
        TileKind.CRATE -> {
            val plank = Color(0xFF7A4A1E)
            for (i in 1..2) drawLine(plank, Offset(inset, inset + faceH * i / 3), Offset(w - inset, inset + faceH * i / 3), strokeWidth = w * 0.03f)
            drawLine(plank, Offset(inset + w * 0.1f, inset + faceH * 0.1f), Offset(w - inset - w * 0.1f, inset + faceH * 0.9f), strokeWidth = w * 0.06f)
            drawRoundRect(Color(0xFF5A3410), Offset(inset, inset), Size(w - inset * 2, faceH), r, style = Stroke(width = w * 0.06f))
            if (t.hp >= 2) {
                // Metal corners on strong crates.
                val m = Color(0xFFB0BEC5)
                for ((x, y) in listOf(inset to inset, w - inset - w * 0.2f to inset, inset to inset + faceH - w * 0.2f, w - inset - w * 0.2f to inset + faceH - w * 0.2f)) {
                    drawRoundRect(m, Offset(x, y), Size(w * 0.2f, w * 0.2f), CornerRadius(w * 0.04f))
                }
            }
        }
        TileKind.ICE -> {
            drawRoundRect(Color(0x99CFF4FF), Offset(inset, inset), Size(w - inset * 2, faceH), r)
            drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(inset, inset), Size(w - inset * 2, faceH), r, style = Stroke(width = w * 0.05f))
            val c = Color.White.copy(alpha = 0.8f)
            drawLine(c, Offset(w * 0.2f, h * 0.25f), Offset(w * 0.42f, h * 0.45f), strokeWidth = 2f)
            drawLine(c, Offset(w * 0.42f, h * 0.45f), Offset(w * 0.35f, h * 0.7f), strokeWidth = 2f)
            drawLine(c, Offset(w * 0.42f, h * 0.45f), Offset(w * 0.75f, h * 0.38f), strokeWidth = 2f)
        }
        TileKind.CHAIN -> {
            val link = Color(0xFFCFD8DC)
            val dark = Color(0xFF455A64)
            for (diag in 0..1) {
                for (i in 0..4) {
                    val f = i / 4f
                    val x = inset + (w - inset * 2) * (if (diag == 0) f else 1 - f)
                    val y = inset + faceH * f
                    drawRoundRect(dark, Offset(x - w * 0.08f, y - w * 0.05f + 1.5f), Size(w * 0.16f, w * 0.1f), CornerRadius(w * 0.05f), style = Stroke(width = w * 0.035f))
                    drawRoundRect(link, Offset(x - w * 0.08f, y - w * 0.05f), Size(w * 0.16f, w * 0.1f), CornerRadius(w * 0.05f), style = Stroke(width = w * 0.03f))
                }
            }
        }
        TileKind.GIFT -> {
            val ribbon = Color(0xFFFF4F8B)
            drawRect(ribbon, Offset(w / 2 - w * 0.05f, inset), Size(w * 0.1f, faceH))
            drawRect(ribbon, Offset(inset, inset + faceH / 2 - w * 0.05f), Size(w - inset * 2, w * 0.1f))
        }
        else -> {}
    }
}
