package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Floating auto-save indicator that fades in/out
 * when content changes are detected.
 */
@Composable
fun AutoSaveIndicator(
    lastSaveTime: Long,
    modifier: Modifier = Modifier
) {
    var showIndicator by remember { mutableStateOf(false) }
    var displayText by remember { mutableStateOf("Saved") }
    
    // Trigger indicator when save time changes
    LaunchedEffect(lastSaveTime) {
        if (lastSaveTime > 0) {
            displayText = "Saved"
            showIndicator = true
            delay(2000) // Show for 2 seconds
            showIndicator = false
        }
    }
    
    AnimatedVisibility(
        visible = showIndicator,
        enter = fadeIn(animationSpec = tween(300)) + 
                slideInVertically(initialOffsetY = { -20 }),
        exit = fadeOut(animationSpec = tween(500)) + 
               slideOutVertically(targetOffsetY = { -20 }),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            tonalElevation = 4.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Animated checkmark
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.7f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_alpha"
                )
                
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Saved",
                    modifier = Modifier
                        .size(16.dp)
                        .alpha(alpha),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Typing indicator that shows when user is actively typing.
 * Can be used to show real-time sync status.
 */
@Composable
fun TypingIndicator(
    isTyping: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isTyping,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            repeat(3) { index ->
                val animatedAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = index * 200),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot_$index"
                )
                
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .alpha(animatedAlpha)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}
