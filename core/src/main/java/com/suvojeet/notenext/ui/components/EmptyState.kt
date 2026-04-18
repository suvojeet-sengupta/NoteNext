@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Playful empty state — a wobbling mascot peeks at the user with a pastel
 * bubble background and a bouncy reveal. The optional icon from callers is
 * ignored on purpose: the mascot is the character now. Message + description
 * still drive the copy, so the call sites don't need to change.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    message: String,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(modifier = modifier.fillMaxSize()) {
        // Soft pastel blobs in the background
        PastelBlobBackground(alpha = 0.35f)

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Bouncy mascot entry
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                        scaleIn(
                            initialScale = 0.4f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioHighBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
            ) {
                CuteMascot(size = 160.dp)
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, delayMillis = 180)) +
                        slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) { it / 3 }
            ) {
                Text(
                    text = playfulHeadline(message),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (description != null) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500, delayMillis = 340)) +
                            slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) { it / 3 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = playfulSubcopy(description),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            // A row of pastel bubbles as a decorative footer "constellation"
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700, delayMillis = 500))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PastelBubble(tint = PlayfulPalette.pink()) {
                            Text("✨", style = MaterialTheme.typography.bodyLarge)
                        }
                        PastelBubble(tint = PlayfulPalette.mint()) {
                            Text("🌱", style = MaterialTheme.typography.bodyLarge)
                        }
                        PastelBubble(tint = PlayfulPalette.sky()) {
                            Text("💭", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Map the developer-supplied message to a friendlier phrasing where we can
 * recognise it. Unknown messages fall through unchanged so localised strings
 * from callers still work.
 */
private fun playfulHeadline(raw: String): String {
    val lower = raw.lowercase()
    return when {
        lower.contains("no notes yet") -> "It's a bit quiet in here…"
        lower.contains("no notes found") && lower.contains("label") -> "Nothing under that label yet"
        lower.contains("no notes found") -> "Hmm, nothing matches"
        else -> raw
    }
}

private fun playfulSubcopy(raw: String): String {
    val lower = raw.lowercase()
    return when {
        lower.contains("create your first note") -> "Tap the + to jot down your first thought ✏️"
        else -> raw
    }
}
