package com.suvojeet.notenext.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
    amplitude: Dp = 4.dp,
    frequency: Float = 1f,
    speed: Float = 1000f // Pixels per second
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    
    // Animate phase shift for the movement effect
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(amplitude * 4) // Ensure enough height for the wave
    ) {
        val width = size.width
        val height = size.height
        val midHeight = height / 2f
        val amp = amplitude.toPx()
        
        val path = Path()
        
        // Calculate points for the sine wave
        // y = A * sin(wx + phase)
        // w = 2*PI * frequency / width
        
        val angularFrequency = 2 * PI * frequency / 100f // Adjust spatial frequency
        
        path.moveTo(0f, midHeight + amp * sin(phase + 0f))
        
        var x = 0f
        val step = 5f // Precision of the curve
        
        while (x <= width) {
            val y = (midHeight + amp * sin((x * angularFrequency / width * 20) - phase * 2)).toFloat() 
            path.lineTo(x, y)
            x += step
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}
