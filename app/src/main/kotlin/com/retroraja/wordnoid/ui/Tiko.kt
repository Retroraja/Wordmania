package com.retroraja.wordnoid.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tiko, the Builder: a sky blue 3D letter tile with a face, an orange hard hat, a little hammer and pink shoes.
 * Tiko's friends (on Rescue levels) use the same drawing in other colours, without the hat.
 */
@Composable
fun Tiko(modifier: Modifier = Modifier, color: Color = Palette.tiko, letter: Char? = 'T', hat: Boolean = true, outfit: String? = null) {
    val anim = rememberInfiniteTransition(label = "tiko")
    val blink by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(3200), RepeatMode.Restart), label = "blink")
    val swing by anim.animateFloat(-18f, 12f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "swing")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val bodyTop = h * (if (hat) 0.24f else 0.08f)
            val bodyH = h * (if (hat) 0.6f else 0.74f)
            val bodyX = w * 0.16f
            val bodyW = w * 0.68f
            // Shoes.
            drawOval(Color(0xFFFF5FA2), Offset(w * 0.22f, bodyTop + bodyH - h * 0.02f), Size(w * 0.22f, h * 0.12f))
            drawOval(Color(0xFFFF5FA2), Offset(w * 0.56f, bodyTop + bodyH - h * 0.02f), Size(w * 0.22f, h * 0.12f))
            // Body: thick glossy tile.
            drawRoundRect(color.darker(0.6f), Offset(bodyX, bodyTop + h * 0.05f), Size(bodyW, bodyH), CornerRadius(w * 0.16f))
            drawRoundRect(
                Brush.verticalGradient(listOf(color.lighter(0.35f), color, color.darker(0.85f)), startY = bodyTop, endY = bodyTop + bodyH),
                Offset(bodyX, bodyTop), Size(bodyW, bodyH), CornerRadius(w * 0.16f),
            )
            drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(bodyX + w * 0.06f, bodyTop + h * 0.03f), Size(bodyW - w * 0.12f, bodyH * 0.25f), CornerRadius(w * 0.1f))
            // Eyes (they blink now and then) and smile.
            val eyeY = bodyTop + bodyH * 0.42f
            val closed = blink > 0.95f
            for (ex in listOf(0.38f, 0.62f)) {
                if (closed) drawLine(Color(0xFF1B2140), Offset(w * ex - w * 0.05f, eyeY), Offset(w * ex + w * 0.05f, eyeY), strokeWidth = w * 0.03f, cap = StrokeCap.Round)
                else {
                    drawCircle(Color.White, w * 0.075f, Offset(w * ex, eyeY))
                    drawCircle(Color(0xFF1B2140), w * 0.045f, Offset(w * ex + w * 0.01f, eyeY + h * 0.005f))
                    drawCircle(Color.White, w * 0.015f, Offset(w * ex + w * 0.025f, eyeY - h * 0.015f))
                }
            }
            drawCircle(Color(0xFFFF8FB1).copy(alpha = 0.7f), w * 0.05f, Offset(w * 0.27f, eyeY + h * 0.08f))
            drawCircle(Color(0xFFFF8FB1).copy(alpha = 0.7f), w * 0.05f, Offset(w * 0.73f, eyeY + h * 0.08f))
            val smile = Path().apply {
                moveTo(w * 0.4f, eyeY + h * 0.09f)
                quadraticBezierTo(w * 0.5f, eyeY + h * 0.18f, w * 0.6f, eyeY + h * 0.09f)
            }
            drawPath(smile, Color(0xFF1B2140), style = Stroke(width = w * 0.03f, cap = StrokeCap.Round))
            if (hat) {
                // Orange hard hat with a brim.
                drawArc(
                    Brush.verticalGradient(listOf(Color(0xFFFFB347), Color(0xFFFF7A00)), startY = h * 0.04f, endY = bodyTop + h * 0.04f),
                    180f, 180f, true, Offset(w * 0.2f, h * 0.04f), Size(w * 0.6f, (bodyTop - h * 0.04f) * 2f),
                )
                drawRoundRect(Color(0xFFE56A00), Offset(w * 0.12f, bodyTop - h * 0.02f), Size(w * 0.76f, h * 0.06f), CornerRadius(w * 0.03f))
                drawRect(Color(0xFFFFD08A), Offset(w * 0.47f, h * 0.05f), Size(w * 0.06f, bodyTop - h * 0.07f))
                // Hammer in the right hand, swinging gently.
                rotate(swing, Offset(w * 0.86f, bodyTop + bodyH * 0.6f)) {
                    drawRoundRect(Color(0xFF8D6E63), Offset(w * 0.84f, bodyTop + bodyH * 0.2f), Size(w * 0.05f, bodyH * 0.45f), CornerRadius(w * 0.02f))
                    drawRoundRect(Color(0xFF90A4AE), Offset(w * 0.77f, bodyTop + bodyH * 0.12f), Size(w * 0.2f, h * 0.09f), CornerRadius(w * 0.02f))
                }
            }
        }
        if (outfit != null) Text(outfit, Modifier.align(Alignment.TopStart), fontSize = 14.sp)
    }
}

/** The strip under the board where Tiko talks: goals at the start, tutorials, hint words. */
@Composable
fun TikoStrip(text: String, modifier: Modifier = Modifier, tikoSize: Dp = 44.dp, highlight: Boolean = false) {
    val anim = rememberInfiniteTransition(label = "strip")
    val bob by anim.animateFloat(0f, -4f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "bob")
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Tiko(Modifier.size(tikoSize).graphicsLayer { translationY = bob * density })
        Box(
            Modifier
                .padding(start = 6.dp)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(14.dp))
                .border(2.dp, if (highlight) Palette.gold else Palette.tiko, RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(text, fontFamily = BodyFont, fontSize = 14.sp, lineHeight = 17.sp, color = Color(0xFF1B2140), fontWeight = Heavy, maxLines = 2)
        }
    }
}

/** The Tiko friends to rescue are pink Tikos. */
val PINK_TIKO = androidx.compose.ui.graphics.Color(0xFFFF7EB6)
