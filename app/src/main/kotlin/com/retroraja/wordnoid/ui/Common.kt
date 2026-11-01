package com.retroraja.wordnoid.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val OutlineShadow = Shadow(color = Color(0xAA000000), offset = androidx.compose.ui.geometry.Offset(0f, 4f), blurRadius = 6f)

@Composable
fun ArcadeText(text: String, size: TextUnit, color: Color = Palette.cream, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = TextStyle(fontFamily = ArcadeFont, fontWeight = Heavy, fontSize = size, color = color, shadow = OutlineShadow))
}

/** A chunky glossy button. */
@Composable
fun ArcadeButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Palette.greenDeep,
    enabled: Boolean = true,
    textSize: TextUnit = 20.sp,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "press")
    Box(
        modifier
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .background(Brush.verticalGradient(listOf(color.lighter(0.25f), color, color.darker(0.75f))), RoundedCornerShape(14.dp))
            .border(2.dp, Palette.gold, RoundedCornerShape(14.dp))
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        ArcadeText(text, textSize)
    }
}
