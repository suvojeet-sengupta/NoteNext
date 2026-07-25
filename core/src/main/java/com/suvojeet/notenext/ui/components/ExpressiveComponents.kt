@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.suvojeet.notenext.core.R
import kotlinx.coroutines.delay

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale

@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    delay: Int = 0,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    // Trigger the entrance animation after the specified delay.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        isVisible = true
    }

    // Animate the scale for the entrance.
    val entranceScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "IconEntranceScale"
    )

    // Animate the scale for the press effect.
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "IconPressScale"
    )

    IconButton(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier.scale(entranceScale * pressScale)
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + 
                 scaleIn(initialScale = 0.7f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                    .togetherWith(fadeOut(animationSpec = spring()) + scaleOut(targetScale = 0.7f))
            },
            label = "IconChange"
        ) { targetIcon ->
            Icon(
                imageVector = targetIcon,
                contentDescription = contentDescription,
                tint = tint
            )
        }
    }

    // Reset the press state after a short delay to create a 'pop' effect.
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
fun AnimatedDropdownItem(
    text: String,
    onClick: () -> Unit,
    textColor: Color = Color.Unspecified
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        },
        onClick = onClick,
        modifier = Modifier.animateContentSize() // Animates size changes.
    )
}

@Composable
fun ExpressiveSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
fun Modifier.springPress(
    dampingRatio: Float = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
    stiffness: Float = 400f
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness),
        label = "SpringPressScale"
    )
    return this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

@Composable
fun ExpressiveLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Subtle pulsing background for the loading indicator
                val infiniteTransition = rememberInfiniteTransition(label = "LoadingBg")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "PulseScale"
                )
                
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                )
                
                LoadingIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            var loadingText by remember { mutableStateOf("Loading...") }
            LaunchedEffect(Unit) {
                val texts = listOf("Gathering your notes...", "Organizing...", "Almost there...", "Setting things up...")
                var index = 0
                while(true) {
                    loadingText = texts[index]
                    delay(2000)
                    index = (index + 1) % texts.size
                }
            }
            
            AnimatedContent(
                targetState = loadingText,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 2 }).togetherWith(fadeOut() + slideOutVertically { -it / 2 })
                },
                label = "LoadingText"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * ButtonGroup graduated to stable in material3 1.5.0-alpha22, which changed its
 * shape: `overflowIndicator` is now required, and `content` is a plain scope
 * lambda (you declare items via [ButtonGroupScope]) rather than a @Composable
 * one. The indicator defaults to Material's own so callers keep the original
 * modifier-plus-content ergonomics.
 */
@Composable
fun ExpressiveButtonGroup(
    modifier: Modifier = Modifier,
    overflowIndicator: @Composable (ButtonGroupMenuState) -> Unit = { menuState ->
        ButtonGroupDefaults.OverflowIndicator(menuState)
    },
    content: ButtonGroupScope.() -> Unit
) {
    ButtonGroup(
        overflowIndicator = overflowIndicator,
        modifier = modifier,
        content = content
    )
}

@Composable
fun MorphingLogo(modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "MorphingLogoInfinite")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MorphProgress"
    )

    val shapeA = remember { RoundedPolygon(numVertices = 30) } // Circle approx
    val shapeB = remember { RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.4f)) } // Rounded square
    
    val morph = remember { Morph(shapeA, shapeB) }
    
    val containerColor = MaterialTheme.colorScheme.primaryContainer

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val androidPath = morph.toPath(progress)
            
            // Re-scale the path to fit the canvas
            val scaleMatrix = android.graphics.Matrix()
            scaleMatrix.setScale(size.toPx(), size.toPx())
            scaleMatrix.postTranslate(0f, 0f) 
            androidPath.transform(scaleMatrix)

            drawPath(
                path = androidPath.asComposePath(),
                color = containerColor
            )
        }
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.8f)
        )
    }
}

@Composable
fun AnimatedSetupStep(
    visible: Boolean,
    delay: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(dampingRatio = 0.6f, stiffness = 400f)) +
                slideInVertically(spring(dampingRatio = 0.6f, stiffness = 400f)) { it / 3 },
        exit = fadeOut()
    ) {
        content()
    }
}

@Composable
fun SetupLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MorphingLogo(size = 140.dp)
            Spacer(modifier = Modifier.height(32.dp))
            LoadingIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Setting things up...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
