package com.retroraja.wordnoid.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.retroraja.wordnoid.core.Pos
import com.retroraja.wordnoid.core.Tile
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BoardView(c: GameController, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val frame = 10.dp
        val tile: Dp = minOf((maxWidth - frame * 2) / c.board.cols, (maxHeight - frame * 2) / c.board.rows)
        val boardW = tile * c.board.cols
        val boardH = tile * c.board.rows
        val density = LocalDensity.current
        val tilePx = with(density) { tile.toPx() }

        // Screen shake for big words and combos.
        val shake = remember { Animatable(0f) }
        LaunchedEffect(c.shakeKey) {
            if (c.shakeKey == 0) return@LaunchedEffect
            for (x in listOf(10f, -9f, 7f, -5f, 3f, 0f)) shake.animateTo(x, tween(35))
        }

        Box(
            Modifier
                .offset { IntOffset(shake.value.roundToInt(), 0) }
                .size(boardW + frame * 2, boardH + frame * 2)
                .background(Brush.verticalGradient(listOf(Color(0xFF5C3418), Palette.frame)), RoundedCornerShape(16.dp))
                .border(3.dp, Brush.verticalGradient(listOf(Palette.gold, Palette.goldDeep)), RoundedCornerShape(16.dp))
                .padding(frame),
        ) {
            Box(
                Modifier
                    .size(boardW, boardH)
                    .background(Color(0xFF2B160A), RoundedCornerShape(8.dp))
                    .clipToBounds()
            ) {
                for (pos in c.board.positions()) {
                    val t = c.board[pos]
                    key(t.id) {
                        TileView(
                            tile = t,
                            pos = pos,
                            size = tile,
                            selected = c.selected == pos,
                            flashing = t.id in c.flashing,
                            hinted = c.hint?.let { it.first == pos || it.second == pos } == true,
                            onTap = { c.tap(it) },
                            onDrag = { p, dr, dc -> c.drag(p, dr, dc) },
                        )
                    }
                }
                for (b in c.bursts) {
                    key(b.id) { BurstView(b, tilePx) { c.removeBurst(b.id) } }
                }
                c.tutorial?.let { TutorialHand(it, tile) }
            }
        }
    }
}

@Composable
private fun TileView(
    tile: Tile,
    pos: Pos,
    size: Dp,
    selected: Boolean,
    flashing: Boolean,
    hinted: Boolean,
    onTap: (Pos) -> Unit,
    onDrag: (Pos, Int, Int) -> Unit,
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    val target = Offset(pos.col * sizePx, pos.row * sizePx)
    val anim = remember { Animatable(Offset(target.x, (pos.row - tile.dropFrom) * sizePx), Offset.VectorConverter) }
    LaunchedEffect(target) {
        anim.animateTo(target, spring(dampingRatio = 0.6f, stiffness = 420f))
    }
    val currentPos by rememberUpdatedState(pos)
    val scale by animateFloatAsState(
        when {
            flashing -> 1.16f
            selected -> 1.1f
            else -> 1f
        },
        spring(dampingRatio = 0.4f, stiffness = 600f), label = "scale",
    )
    val wiggle = if (hinted) {
        val inf = rememberInfiniteTransition(label = "hint")
        inf.animateFloat(-7f, 7f, infiniteRepeatable(tween(180, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "wiggle").value
    } else 0f
    val glow = if (flashing) {
        val inf = rememberInfiniteTransition(label = "flash")
        inf.animateFloat(0.15f, 0.7f, infiniteRepeatable(tween(140), RepeatMode.Reverse), label = "glow").value
    } else 0f

    val color = letterColor(tile.letter)
    val shape = RoundedCornerShape(size * 0.12f)
    Box(
        Modifier
            .offset { IntOffset(anim.value.x.roundToInt(), anim.value.y.roundToInt()) }
            .size(size)
            .padding(size * 0.045f)
            .graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = wiggle }
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap(currentPos) }) }
            .pointerInput(Unit) {
                var total = Offset.Zero
                var fired = false
                detectDragGestures(
                    onDragStart = { total = Offset.Zero; fired = false },
                    onDrag = { change, amount ->
                        change.consume()
                        total += amount
                        val threshold = sizePx * 0.28f
                        if (!fired && (abs(total.x) > threshold || abs(total.y) > threshold)) {
                            fired = true
                            if (abs(total.x) > abs(total.y)) onDrag(currentPos, 0, if (total.x > 0) 1 else -1)
                            else onDrag(currentPos, if (total.y > 0) 1 else -1, 0)
                        }
                    },
                )
            }
    ) {
        // 3D edge
        Box(Modifier.fillMaxSize().background(color.darker(0.55f), shape))
        // Face
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = size * 0.07f)
                .background(Brush.verticalGradient(listOf(color.lighter(0.22f), color)), shape)
                .then(if (selected) Modifier.border(3.dp, Palette.gold, shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            // Gloss
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = size * 0.08f, vertical = size * 0.05f)
                    .size(size * 0.14f)
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
            )
            Text(
                tile.letter.toString(),
                style = TextStyle(
                    fontFamily = ArcadeFont, fontWeight = Heavy, color = Color.White,
                    fontSize = with(density) { (size * 0.52f).toSp() }, shadow = OutlineShadow,
                ),
            )
            if (glow > 0f) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = glow), shape))
        }
    }
}

@Composable
private fun BurstView(burst: Burst, tilePx: Float, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(700))
        onDone()
    }
    Canvas(Modifier.fillMaxSize()) {
        val p = progress.value
        burst.centres.forEachIndexed { ci, (cx, cy, color) ->
            for (i in 0 until 12) {
                val angle = (i / 12f) * 6.283f + ci
                val speed = 0.9f + ((i * 37 + ci * 11) % 7) / 10f
                val dist = p * speed * tilePx * 0.9f
                val x = cx * tilePx + cos(angle) * dist
                val y = cy * tilePx + sin(angle) * dist + p * p * tilePx * 0.6f
                val r = tilePx * 0.09f * (1f - p * 0.7f)
                drawCircle(if (i % 3 == 0) Palette.gold else color.lighter(0.2f), r, Offset(x, y), alpha = 1f - p)
            }
        }
    }
}

@Composable
private fun TutorialHand(swap: Pair<Pos, Pos>, tile: Dp) {
    val inf = rememberInfiniteTransition(label = "hand")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "t")
    val (a, b) = swap
    val x = tile * (a.col + (b.col - a.col) * t + 0.55f)
    val y = tile * (a.row + (b.row - a.row) * t + 0.55f)
    Text("👆", modifier = Modifier.offset(x, y), style = TextStyle(fontSize = with(LocalDensity.current) { (tile * 0.6f).toSp() }))
}
