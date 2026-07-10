@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.core.R
import kotlinx.coroutines.delay

@Composable
fun EmptyState(
    icon: ImageVector,
    message: String,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Expressive Animated Icon Background
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            // Morphing-like background animation using animated size and alpha
            val infiniteTransition = rememberInfiniteTransition(label = "EmptyStateBg")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "BgScale"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "BgAlpha"
            )

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            this@Column.AnimatedVisibility(
                visible = visible,
                enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) + 
                        scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = 0.6f))
            ) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                         Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Animated Message
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600, delayMillis = 200)) + 
                    slideInVertically(tween(600, delayMillis = 200)) { it / 2 }
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Animated Description
        if (description != null) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, delayMillis = 400)) + 
                        slideInVertically(tween(600, delayMillis = 400)) { it / 2 }
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        lineHeight = 24.sp
                    )
                }
            }
        }
        
        // Subtle hint for first time users
        if (description != null && message.contains("No notes", ignoreCase = true)) {
             AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, delayMillis = 1000))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(48.dp))
                    LoadingIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
