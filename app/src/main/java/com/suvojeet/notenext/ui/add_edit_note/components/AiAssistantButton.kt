package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp

@Composable
fun AiAssistantButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient_shift")
    
    // Animated offset for shifting gradient
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )
    
    // Get theme colors
    val accentColor = MaterialTheme.colorScheme.primary
    val accentDark = MaterialTheme.colorScheme.primaryContainer
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val tertiaryDark = MaterialTheme.colorScheme.tertiaryContainer
    
    // Animated shifting gradient
    val animatedBrush = Brush.horizontalGradient(
        colors = listOf(
            accentColor,
            accentDark,
            tertiaryColor,
            tertiaryDark,
            accentColor  // Loop back for seamless animation
        ),
        startX = gradientOffset,
        endX = gradientOffset + 500f,
        tileMode = TileMode.Mirror
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = modifier
            .padding(8.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(animatedBrush)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Create Checklist with AI",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}
