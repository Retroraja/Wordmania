package com.retroraja.wordnoid.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
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

/**
 * Update 1.3: some levels wrap their blocked tiles in yarn (Crackers Match "thread" art) instead of chains.
 * Same rule: make a word next to it (or with it) to set it free.
 */
object BlockStyle {
    private val state = androidx.compose.runtime.mutableStateOf(false)
    var thread: Boolean
        get() = state.value
        set(v) { state.value = v }
    fun isThread(level: Int) = level % 3 == 2
    /** Goal and help texts with the right name for this level's blocked tiles. */
    fun words(text: String): String = if (!thread) text else text
        .replace("chained tiles", "yarn-wrapped tiles").replace("Chained tiles", "Yarn-wrapped tiles")
        .replace("Chains", "Yarn").replace("chains", "yarn").replace("chain", "yarn")
}

/** Obstacle pictures (from the Crackers Match art): wooden crate, Iron Crate, chains, yarn, ice. Loaded once. */
class TileArt(val crate: ImageBitmap?, val iron: ImageBitmap?, val chain: ImageBitmap?, val ice: ImageBitmap?, val thread: ImageBitmap? = null) {
    companion object {
        @Volatile private var cached: TileArt? = null
        fun get(context: android.content.Context): TileArt = cached ?: synchronized(this) {
            cached ?: run {
                fun load(name: String): ImageBitmap? = runCatching {
                    context.assets.open("art/$name.webp").use { android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap() }
                }.getOrNull()
                TileArt(load("crate"), load("crate2"), load("chain"), load("ice"), load("thread")).also { cached = it }
            }
        }
    }
}

/** Base colour of a tile's face. */
fun tileColor(t: Tile): Color = when (t.kind) {
    TileKind.CRATE -> Color(0xFFB0703A)
    TileKind.BUDDY -> Color(0xFF3FA9FF)
    TileKind.JOKER -> Color(0xFFFF4FA3)
    TileKind.MYSTERY -> Color(0xFF5B3A9E)
    else -> letterColor(t.letter)
}

/**
 * A thick, glossy 3D tile: a soft shadow, a darker side for depth, a shiny top face with a highlight,
 * and a bold white letter. Gold, crate, ice, chain, joker, gift and lock each add their own look.
 */
@Composable
fun Tile3D(tile: Tile, size: Dp, modifier: Modifier = Modifier, glow: Color? = null, dim: Boolean = false) {
    val sizePx = size.value
    // Gold and ice have a moving shine and twinkles so they stand out from normal tiles.
    val shine = if (tile.isGold || tile.isIce || tile.locked || tile.isPower || tile.isTwin || tile.isMystery) {
        val t = rememberInfiniteTransition(label = "shine")
        val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(if (tile.isIce) 2600 else if (tile.isPower) 1400 else 1800, easing = LinearEasing)), label = "shineT")
        v
    } else -1f
    val art = TileArt.get(LocalContext.current)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawTile(tile, glow, dim, shine, art) }
        if (tile.isBuddy) {
            Tiko(Modifier.fillMaxSize(0.92f), color = buddyColor(tile.id), letter = null, hat = false)
        } else if (tile.kind != TileKind.CRATE) {
            val text = if (tile.isJoker) "★" else if (tile.isMystery) "?" else tile.letter.toString()
            Text(
                text,
                modifier = Modifier.offset(y = -(size * 0.05f)),
                style = TextStyle(
                    fontFamily = ArcadeFont, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                    color = when { tile.isGold -> Color(0xFF7A4A00); tile.isIce -> Color(0xFF0D3B66); tile.isMystery -> Color(0xFFFFE27A); else -> Color.White },
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
        // Small badges: ice costs 2 moves to swap, an Iron Crate needs 2 hits, x2 tiles count twice, power tiles blast a row.
        if (tile.isIce) Badge("2", Color(0xFF1E88E5), size, Modifier.align(Alignment.TopStart))
        if (tile.isIronCrate && tile.hp >= 2) Badge("2", Color(0xFF455A64), size, Modifier.align(Alignment.TopStart))
        if (tile.isTwin) Badge("x2", Color(0xFFE0457B), size, Modifier.align(Alignment.TopEnd))
        if (tile.isPower) Text(if (tile.hp >= 2) "✚" else "⚡", Modifier.align(Alignment.TopEnd).offset(x = size * 0.04f, y = -(size * 0.06f)), style = TextStyle(fontSize = (sizePx * 0.26f).sp, color = Color.White, shadow = LetterShadow))
        if (tile.locked) Text("🔒", Modifier.align(Alignment.TopEnd).offset(x = size * 0.1f, y = -(size * 0.12f)), style = TextStyle(fontSize = (sizePx * 0.36f).sp))
        if (tile.isBalloon) Text("🎈", Modifier.align(Alignment.TopEnd).offset(x = size * 0.08f, y = -(size * 0.16f)), style = TextStyle(fontSize = (sizePx * 0.36f).sp))
        if (tile.isGift) Text("🎀", Modifier.align(Alignment.TopCenter).offset(y = -(size * 0.08f)), style = TextStyle(fontSize = (sizePx * 0.3f).sp))
    }
}

/** A small round badge in a tile corner. */
@Composable
private fun Badge(text: String, color: Color, size: Dp, modifier: Modifier) {
    Box(
        modifier.offset(x = if (text == "x2") size * 0.06f else -(size * 0.06f), y = -(size * 0.08f))
            .size(size * 0.36f)
            .drawBehind { drawCircle(Color.White, this.size.minDimension / 2); drawCircle(color, this.size.minDimension / 2 * 0.82f) },
        contentAlignment = Alignment.Center,
    ) { Text(text, style = TextStyle(fontFamily = ArcadeFont, fontSize = (size.value * (if (text.length > 1) 0.15f else 0.19f)).sp, color = Color.White)) }
}

fun DrawScope.drawTile(t: Tile, glow: Color?, dim: Boolean, shine: Float = -1f, art: TileArt? = null) {
    val w = size.width
    val h = size.height
    if (t.isBuddy) return
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
    if (t.locked) {
        // A locked letter gets a thick gold frame and darker face, so it clearly can't make words.
        drawRoundRect(Color.Black.copy(alpha = 0.25f), Offset(inset, inset), Size(w - inset * 2, faceH), r)
        drawRoundRect(
            Brush.linearGradient(listOf(Color(0xFFFFF1A6), Color(0xFFFFC93C), Color(0xFFB8860B), Color(0xFFFFE680)), Offset.Zero, Offset(w, h)),
            Offset(inset, inset), Size(w - inset * 2, faceH), r, style = Stroke(width = w * 0.1f),
        )
    }
    when (t.kind) {
        TileKind.GOLD -> {
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFFFFE680), Color(0xFFFFB300), Color(0xFFB8860B)), startY = inset, endY = inset + faceH),
                Offset(inset, inset), Size(w - inset * 2, faceH), r,
            )
            drawRoundRect(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), startY = inset, endY = inset + faceH * 0.5f),
                Offset(inset + w * 0.07f, inset + h * 0.04f), Size(w - inset * 2 - w * 0.14f, faceH * 0.42f), CornerRadius(w * 0.14f),
            )
            drawRoundRect(
                Brush.linearGradient(listOf(Color(0xFFFFF1A6), Color(0xFFFFC93C), Color(0xFFB8860B), Color(0xFFFFE680)), Offset.Zero, Offset(w, h)),
                Offset(inset, inset), Size(w - inset * 2, faceH), r, style = Stroke(width = w * 0.09f),
            )
            drawCircle(Color.White.copy(alpha = 0.9f), w * 0.04f, Offset(w * 0.78f, h * 0.2f))
            if (shine >= 0f) {
                // A bright band sweeps across the metal, then a star twinkles.
                val x = -w * 0.6f + shine * w * 2.2f
                clipRect(inset, inset, w - inset, inset + faceH) {
                    drawLine(Color.White.copy(alpha = 0.65f), Offset(x, inset + faceH), Offset(x + w * 0.45f, inset), strokeWidth = w * 0.16f)
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(x + w * 0.2f, inset + faceH), Offset(x + w * 0.65f, inset), strokeWidth = w * 0.06f)
                }
                val tw = (1f - abs(shine * 2f - 1f))
                sparkle(Offset(w * 0.8f, h * 0.18f), w * 0.12f * tw, Color.White)
            }
        }
        TileKind.CRATE -> {
            val img = if (t.iron && t.hp >= 2) art?.iron else art?.crate
            if (img != null) {
                // Wooden crate / Iron Crate (dark wood, iron bands and bolts) pictures.
                drawArt(img, inset, faceH, if (dim) 0.5f else 1f)
                if (t.iron && t.hp <= 1) {
                    // Its bands snapped off: a big crack shows it has 1 hit left.
                    val k = Color(0xFF3E2210)
                    drawLine(k, Offset(w * 0.18f, h * 0.2f), Offset(w * 0.48f, h * 0.48f), strokeWidth = w * 0.04f)
                    drawLine(k, Offset(w * 0.48f, h * 0.48f), Offset(w * 0.82f, h * 0.36f), strokeWidth = w * 0.035f)
                    drawLine(k, Offset(w * 0.48f, h * 0.48f), Offset(w * 0.42f, h * 0.82f), strokeWidth = w * 0.035f)
                }
                return
            }
            // A candy-striped gift box with a bow (no wood).
            val stripe = Color.White.copy(alpha = 0.35f)
            for (i in 0..4) {
                val x = inset + (w - inset * 2) * i / 4f
                drawLine(stripe, Offset(x, inset), Offset(x - w * 0.18f, inset + faceH), strokeWidth = w * 0.06f)
            }
            drawRect(Color(0xFFFFE066), Offset(w / 2 - w * 0.06f, inset), Size(w * 0.12f, faceH))
            drawRect(Color(0xFFFFE066), Offset(inset, inset + faceH * 0.45f), Size(w - inset * 2, w * 0.1f))
            drawRoundRect(Color(0xFFC2185B), Offset(inset, inset), Size(w - inset * 2, faceH), r, style = Stroke(width = w * 0.05f))
            if (t.hp >= 2) {
                // Strong boxes have shiny metal corners.
                val m = Color(0xFFCFD8DC)
                for ((x, y) in listOf(inset to inset, w - inset - w * 0.2f to inset, inset to inset + faceH - w * 0.2f, w - inset - w * 0.2f to inset + faceH - w * 0.2f)) {
                    drawRoundRect(m, Offset(x, y), Size(w * 0.2f, w * 0.2f), CornerRadius(w * 0.04f))
                }
            }
        }
        TileKind.ICE -> {
            art?.ice?.let { img ->
                drawArt(img, inset, faceH, 0.88f)
                if (shine >= 0f) for (i in 0..2) {
                    val ph = (shine + i / 3f) % 1f
                    val a = 1f - abs(ph * 2f - 1f)
                    sparkle(listOf(Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.3f), Offset(w * 0.3f, h * 0.74f))[i], w * 0.1f * a, Color.White)
                }
                if (t.hp <= 1) {
                    val k = Color(0xFF1E5A8C)
                    drawLine(k, Offset(w * 0.15f, h * 0.15f), Offset(w * 0.5f, h * 0.5f), strokeWidth = 3f)
                    drawLine(k, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.85f, h * 0.3f), strokeWidth = 3f)
                    drawLine(k, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.45f, h * 0.85f), strokeWidth = 3f)
                }
                return
            }
            // A frosty blue cover: pale ice gradient, frost on the edges, a thick white rim and twinkling sparkles.
            drawRoundRect(
                Brush.linearGradient(listOf(Color(0xCCE6FAFF), Color(0xB37FD6FF), Color(0xCC4FB4F0)), Offset(0f, 0f), Offset(w, h)),
                Offset(inset, inset), Size(w - inset * 2, faceH), r,
            )
            drawRoundRect(
                Brush.radialGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.55f)), Offset(w / 2, inset + faceH / 2), w * 0.62f),
                Offset(inset, inset), Size(w - inset * 2, faceH), r,
            )
            drawRoundRect(Color.White, Offset(inset, inset), Size(w - inset * 2, faceH), r, style = Stroke(width = w * 0.07f))
            drawRoundRect(Color(0xFF7FD6FF), Offset(inset + w * 0.05f, inset + w * 0.05f), Size(w - inset * 2 - w * 0.1f, faceH - w * 0.1f), CornerRadius(w * 0.12f), style = Stroke(width = w * 0.02f))
            if (shine >= 0f) {
                for (i in 0..2) {
                    val ph = (shine + i / 3f) % 1f
                    val a = 1f - abs(ph * 2f - 1f)
                    val pts = listOf(Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.3f), Offset(w * 0.3f, h * 0.74f))
                    sparkle(pts[i], w * 0.1f * a, Color.White)
                }
            }
            val c = Color.White.copy(alpha = 0.8f)
            drawLine(c, Offset(w * 0.2f, h * 0.25f), Offset(w * 0.42f, h * 0.45f), strokeWidth = 2f)
            drawLine(c, Offset(w * 0.42f, h * 0.45f), Offset(w * 0.35f, h * 0.7f), strokeWidth = 2f)
            drawLine(c, Offset(w * 0.42f, h * 0.45f), Offset(w * 0.75f, h * 0.38f), strokeWidth = 2f)
            if (t.hp <= 1) {
                // Cracked by the Hammer: one more hit melts it.
                val k = Color(0xFF1E5A8C)
                drawLine(k, Offset(w * 0.15f, h * 0.15f), Offset(w * 0.5f, h * 0.5f), strokeWidth = 3f)
                drawLine(k, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.85f, h * 0.3f), strokeWidth = 3f)
                drawLine(k, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.45f, h * 0.85f), strokeWidth = 3f)
            }
        }
        TileKind.BALLOON -> {
            drawRoundRect(Color(0xFFFF4F8B), Offset(inset, inset), Size(w - inset * 2, faceH), r, style = Stroke(width = w * 0.05f))
        }
        TileKind.CHAIN -> {
            (if (BlockStyle.thread) art?.thread ?: art?.chain else art?.chain)?.let { img -> drawArt(img, inset * 0.2f, faceH + inset * 1.6f, 0.95f); return }
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
        TileKind.POWER -> {
            // A glowing rainbow frame that slowly turns: this letter blasts its row when used in a new word.
            val rb = listOf(Color(0xFFFF4D6D), Color(0xFFFFB020), Color(0xFFFFE14A), Color(0xFF4CD964), Color(0xFF3FA9FF), Color(0xFFB66BFF), Color(0xFFFF4D6D))
            val sh = if (shine < 0f) 0f else shine
            drawRoundRect(Brush.sweepGradient(rb.drop((sh * 6).toInt()) + rb.take((sh * 6).toInt() + 1), Offset(w / 2, inset + faceH / 2)),
                Offset(inset, inset), Size(w - inset * 2, faceH), r, style = Stroke(width = w * 0.11f))
            drawRoundRect(Color.White.copy(alpha = 0.25f + 0.25f * (1f - abs(sh * 2f - 1f))), Offset(inset, inset), Size(w - inset * 2, faceH), r)
        }
        TileKind.TWIN -> {
            // A shiny pink-purple double frame.
            drawRoundRect(Color(0xFFE0457B), Offset(inset, inset), Size(w - inset * 2, faceH), r, style = Stroke(width = w * 0.07f))
            drawRoundRect(Color(0xFFB66BFF), Offset(inset + w * 0.07f, inset + w * 0.07f), Size(w - inset * 2 - w * 0.14f, faceH - w * 0.14f), CornerRadius(w * 0.12f), style = Stroke(width = w * 0.035f))
        }
        TileKind.MYSTERY -> {
            // A dark purple tile with swirls and a gold rim; the "?" is drawn on top.
            drawRoundRect(Color(0xFFFFC93C), Offset(inset, inset), Size(w - inset * 2, faceH), r, style = Stroke(width = w * 0.05f))
            if (shine >= 0f) sparkle(Offset(w * (0.2f + 0.6f * shine), h * 0.22f), w * 0.08f * (1f - abs(shine * 2f - 1f)), Color.White)
        }
        TileKind.GIFT -> {
            val ribbon = Color(0xFFFF4F8B)
            drawRect(ribbon, Offset(w / 2 - w * 0.05f, inset), Size(w * 0.1f, faceH))
            drawRect(ribbon, Offset(inset, inset + faceH / 2 - w * 0.05f), Size(w - inset * 2, w * 0.1f))
        }
        else -> {}
    }
}

/** A 4-point twinkle star. */
private fun DrawScope.sparkle(c: Offset, r: Float, color: Color) {
    if (r <= 0.5f) return
    drawLine(color, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = r * 0.28f)
    drawLine(color, Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = r * 0.28f)
    drawCircle(color, r * 0.22f, c)
}

private val BUDDY_COLORS = listOf(Color(0xFFFF7EB6), Color(0xFF7ED957), Color(0xFFFFB84D), Color(0xFFB98CFF))
fun buddyColor(id: Long): Color = BUDDY_COLORS[(id % BUDDY_COLORS.size).toInt()]

/** Draws an obstacle picture over the tile face. */
private fun DrawScope.drawArt(img: ImageBitmap, inset: Float, faceH: Float, alpha: Float = 1f) {
    drawImage(
        img, IntOffset.Zero, IntSize(img.width, img.height),
        IntOffset(inset.toInt(), inset.toInt()), IntSize((size.width - inset * 2).toInt().coerceAtLeast(1), faceH.toInt().coerceAtLeast(1)), alpha = alpha,
    )
}
