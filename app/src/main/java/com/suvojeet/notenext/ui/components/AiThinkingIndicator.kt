package com.suvojeet.notenext.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AiThinkingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    primaryColor: Color = Color(0xFF00E5FF), // Cyan
    secondaryColor: Color = Color(0xFFAB47BC), // Purple
    tertiaryColor: Color = Color(0xFF2979FF) // Blue
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Pulsing animation for the main orb
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Rotation for the outer rings
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )

    // Opacity pulse
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val radius = (this.size.width / 2) * 0.8f

        // 1. Core Glowing Orb (Gradient)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.8f), secondaryColor.copy(alpha = 0.2f)),
                center = center,
                radius = radius * scale
            ),
            radius = radius * scale,
            center = center
        )

        // 2. Rotating Outer Ring 1 (Cyan)
        drawCircle(
            color = primaryColor.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(
                width = 4.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 20f), rotation)
            )
        )

        // 3. Rotating Outer Ring 2 (Purple) - Reverse Rotation handled by offset
        drawCircle(
            color = secondaryColor.copy(alpha = alpha),
            radius = radius * 1.15f,
            center = center,
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 30f), -rotation * 1.5f)
            )
        )
    }
}
