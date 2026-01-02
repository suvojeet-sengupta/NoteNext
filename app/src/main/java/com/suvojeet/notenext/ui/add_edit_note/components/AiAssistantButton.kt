package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AiAssistantButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Track if we're in the animated intro phase (first 3 seconds)
    var isIntroPhase by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(3000) // 3 seconds intro animation
        isIntroPhase = false
    }
    
    val accentColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    
    if (isIntroPhase) {
        // Animated gradient phase - white and accent color shimmer
        val infiniteTransition = rememberInfiniteTransition(label = "gradient_shift")
        
        val gradientOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "offset"
        )
        
        // White and accent gradient
        val animatedBrush = Brush.horizontalGradient(
            colors = listOf(
                Color.White,
                accentColor,
                Color.White.copy(alpha = 0.8f),
                accentColor.copy(alpha = 0.8f),
                Color.White
            ),
            startX = gradientOffset,
            endX = gradientOffset + 400f,
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
                    tint = accentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create Checklist with AI",
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor
                )
            }
        }
    } else {
        // Normal static button after 3 seconds
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
            tonalElevation = 4.dp,
            modifier = modifier
                .padding(8.dp)
                .height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = accentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create Checklist with AI",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
