package com.retroraja.wordnoid.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.core.Aim
import com.retroraja.wordnoid.core.Pos
import kotlin.math.abs
import kotlin.math.roundToInt

/** The board: a dark wooden tray with a gold rim, holding 3D tiles that fall, swap, lift and flip. */
@Composable
fun BoardView(c: GameController, cell: Dp, modifier: Modifier = Modifier) {
    val board = c.board
    val density = LocalDensity.current
    val cellPx = with(density) { cell.toPx() }
    val pad = 8.dp
    val padPx = with(density) { pad.toPx() }
    val anim = rememberInfiniteTransition(label = "board")
    val pulse by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")
    val wiggle by anim.animateFloat(-6f, 6f, infiniteRepeatable(tween(140, easing = LinearEasing), RepeatMode.Reverse), label = "wiggle")
    val wobble = remember { Animatable(0f) }
    LaunchedEffect(c.wobbleKey) {
        if (c.wobbleKey > 0) {
            wobble.snapTo(0f)
            wobble.animateTo(-25f, tween(180))
            wobble.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow))
        }
    }
    val armed = c.armed
    Box(
        modifier
            .graphicsLayer { rotationY = wobble.value; cameraDistance = 14f * density.density }
            .background(Brush.verticalGradient(listOf(Color(0xCC1A2E7A), Color(0xCC0F1C52))), RoundedCornerShape(18.dp))
            .border(4.dp, Brush.verticalGradient(listOf(Palette.gold, Palette.goldDeep, Palette.gold)), RoundedCornerShape(18.dp))
            .padding(pad)
            .size(cell * board.cols, cell * board.rows)
            .onGloballyPositioned { coords ->
                c.boardOrigin = coords.positionInRoot()
                c.cellPx = cellPx
            }
            .pointerInput(board.cols, board.rows, cellPx) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val start = down.position
                    val from = Pos((start.y / cellPx).toInt(), (start.x / cellPx).toInt())
                    var swiped = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val ch = event.changes.firstOrNull() ?: break
                        val d = ch.position - start
                        // A quick flick may only report where the finger lifted, so the lift counts too.
                        if (!swiped && (abs(d.x) > cellPx * 0.35f || abs(d.y) > cellPx * 0.35f)) {
                            swiped = true
                            val to = if (abs(d.x) > abs(d.y)) Pos(from.row, from.col + if (d.x > 0) 1 else -1)
                            else Pos(from.row + if (d.y > 0) 1 else -1, from.col)
                            if (c.armed == null && board.contains(from)) c.drag(from, to)
                        }
                        if (ch.positionChange() != Offset.Zero) ch.consume()
                        if (!ch.pressed) break
                    }
                    if (!swiped && board.contains(from)) c.tap(from)
                }
            },
    ) {
        // Empty slots under the tiles. Sand cells hide treasure; dug sand turns light, a found chest shows its gem.
        val sand = c.level.chests.flatten().toSet()
        val dug = c.game.dug
        val found = c.game.chestsFound.flatMap { c.level.chests[it] }.toSet()
        for (p in board.positions()) {
            val color = when {
                p in found -> Color(0xFFFFE9A8)
                p in dug -> Color(0xFFF7DFA0)
                p in sand -> Color(0xFFE2B866)
                else -> Color(0x33000000)
            }
            Box(
                Modifier
                    .offset { IntOffset((p.col * cellPx).roundToInt(), (p.row * cellPx).roundToInt()) }
                    .size(cell)
                    .padding(2.dp)
                    .background(color, RoundedCornerShape(10.dp)),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                if (p in sand && p !in found) Text("·", color = Color(0xFF9A6B1E), fontSize = (cell.value * 0.3f).sp)
            }
        }
        val tut = c.tutorial
        val handCells = tut?.swap?.toList() ?: emptyList()
        if (!c.boardHidden) for (p in board.positions()) {
            val tile = board[p]
            key(tile.id) {
                val target = Offset(p.col * cellPx, p.row * cellPx)
                val pos = remember { Animatable(Offset(target.x, target.y - tile.dropFrom * cellPx - (if (tile.dropFrom > 0) cellPx * 0.6f else 0f) - (if (tile.id < 1000) (board.rows - p.row) * cellPx * 1.2f else 0f)), Offset.VectorConverter) }
                LaunchedEffect(target) {
                    val falling = pos.value.y < target.y - 1f && abs(pos.value.x - target.x) < 1f
                    if (falling) pos.animateTo(target, spring(dampingRatio = 0.55f, stiffness = 380f))
                    else pos.animateTo(target, tween(190))
                }
                val popIndex = c.popping.indexOf(tile.id)
                val lifted = popIndex >= 0
                val lift by animateFloatAsState(if (lifted) 1f else 0f, spring(dampingRatio = 0.45f, stiffness = 500f), label = "lift")
                val flip = remember { Animatable(0f) }
                val flipping = tile.id in c.flipping
                LaunchedEffect(flipping, lifted) {
                    if (flipping || lifted) { flip.snapTo(0f); flip.animateTo(360f, tween(if (lifted) 420 else 520)) }
                }
                val isSelected = c.selected == p || c.aimFirst == p
                val sel by animateFloatAsState(if (isSelected) 1f else 0f, spring(dampingRatio = 0.5f), label = "sel")
                val inHint = c.hint?.let { it.first == p || it.second == p } == true
                val inScan = p in c.scan
                val inTut = p in (tut?.cells ?: emptyList()) || p in handCells
                val glow = when {
                    tile.id in c.glowing -> Palette.gold
                    isSelected -> Palette.green
                    inTut -> Color.White.copy(alpha = 0.4f + 0.6f * pulse)
                    inHint -> Palette.cyan
                    inScan -> Palette.cyan.copy(alpha = 0.4f + 0.6f * pulse)
                    else -> null
                }
                val dim = armed != null && !aimable(armed.aim, tile)
                Tile3D(
                    tile, cell,
                    Modifier
                        .offset { IntOffset(pos.value.x.roundToInt(), pos.value.y.roundToInt()) }
                        .graphicsLayer {
                            val s = 1f + 0.22f * lift + 0.1f * sel + (if (inTut) 0.06f * pulse else 0f)
                            scaleX = s; scaleY = s
                            translationY = -cellPx * 0.18f * lift - cellPx * 0.06f * sel
                            rotationX = if (lifted) flip.value else 0f
                            rotationY = if (!lifted && flipping) flip.value else 0f
                            rotationZ = if (inHint) wiggle else 0f
                            cameraDistance = 10f * density.density
                            shadowElevation = 0f
                        },
                    glow = glow,
                    dim = dim,
                )
            }
        }
        // The "🔓 Unlock" bubble over a tapped locked letter. Tapping it unlocks; tapping anywhere else closes it.
        c.unlockAsk?.let { p ->
            if (board.contains(p) && board[p].locked) {
                val bw = cellPx * 2.2f
                val bh = cellPx * 0.8f
                val bx = ((p.col + 0.5f) * cellPx - bw / 2).coerceIn(0f, board.cols * cellPx - bw)
                val by = if (p.row == 0) (p.row + 1.05f) * cellPx else (p.row - 0.85f) * cellPx
                val bob = 1f + 0.06f * pulse
                Box(
                    Modifier
                        .offset { IntOffset(bx.roundToInt(), by.roundToInt()) }
                        .size(with(density) { bw.toDp() }, with(density) { bh.toDp() })
                        .graphicsLayer { scaleX = bob; scaleY = bob }
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFE27A), Palette.goldDeep)), RoundedCornerShape(50))
                        .border(3.dp, Color.White, RoundedCornerShape(50))
                        .clickable { c.unlockTapped() },
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text("🔓 Unlock", color = Color(0xFF5A3300), fontFamily = ArcadeFont, style = TextStyle(fontSize = (cell.value * 0.3f).sp))
                }
            }
        }
        // Tutorial hand moving between the two tiles to swap.
        if (tut?.swap != null) {
            val (a, b) = tut.swap
            val handT by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "hand")
            val x = (a.col + (b.col - a.col) * handT + 0.45f) * cellPx
            val y = (a.row + (b.row - a.row) * handT + 0.55f) * cellPx
            Text("👆", Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }, style = TextStyle(fontSize = (cell.value * 0.6f).sp))
        }
        // Booster tutorial: a hand taps the tile to use it on (the second tile once the first is picked).
        if (tut != null && tut.swap == null && tut.cells.isNotEmpty()) {
            val p = if (c.aimFirst != null && tut.cells.size > 1) tut.cells[1] else tut.cells[0]
            val tapT by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "tap")
            val x = (p.col + 0.4f) * cellPx
            val y = (p.row + 0.45f + 0.18f * tapT) * cellPx
            Text("👆", Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }, style = TextStyle(fontSize = (cell.value * 0.6f).sp))
        }
    }
}

fun aimable(aim: Aim, t: com.retroraja.wordnoid.core.Tile): Boolean = when (aim) {
    Aim.NONE -> true
    Aim.TILE -> !t.isGold && !t.isBuddy
    Aim.MOVABLE, Aim.TWO_TILES, Aim.SQUARE -> t.movable
    Aim.ROW, Aim.COLUMN -> true
    Aim.OBSTACLE -> t.isCrate || t.isIce || t.isChain
    Aim.GOLD -> t.isGold
}
