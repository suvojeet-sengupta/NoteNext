@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.components

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cute pastel palette used by the Playful vibe. Each colour has a light and
 * dark variant so it reads cleanly against the theme surface either way.
 */
object PlayfulPalette {
    // Light mode — soft, milky pastels
    val pinkLight      = Color(0xFFFFD1E3)
    val mintLight      = Color(0xFFC8F4DE)
    val lavenderLight  = Color(0xFFE4D1F7)
    val peachLight     = Color(0xFFFFE1C8)
    val skyLight       = Color(0xFFD1E7FF)
    val butterLight    = Color(0xFFFFF4C8)
    val coralLight     = Color(0xFFFFC8C8)

    // Dark mode — deeper but still saturated enough to feel playful
    val pinkDark       = Color(0xFF5A2E44)
    val mintDark       = Color(0xFF2E5A44)
    val lavenderDark   = Color(0xFF44305A)
    val peachDark      = Color(0xFF5A4430)
    val skyDark        = Color(0xFF304A5A)
    val butterDark     = Color(0xFF5A5030)
    val coralDark      = Color(0xFF5A3030)

    val accentPink     = Color(0xFFFF7BA8)
    val accentMint     = Color(0xFF4CCB95)
    val accentLavender = Color(0xFFB583F0)
    val accentPeach    = Color(0xFFFF9E5C)
    val accentSky      = Color(0xFF5CA8FF)

    @Composable
    fun pink(): Color = if (isSystemInDarkTheme()) pinkDark else pinkLight
    @Composable
    fun mint(): Color = if (isSystemInDarkTheme()) mintDark else mintLight
    @Composable
    fun lavender(): Color = if (isSystemInDarkTheme()) lavenderDark else lavenderLight
    @Composable
    fun peach(): Color = if (isSystemInDarkTheme()) peachDark else peachLight
    @Composable
    fun sky(): Color = if (isSystemInDarkTheme()) skyDark else skyLight
    @Composable
    fun butter(): Color = if (isSystemInDarkTheme()) butterDark else butterLight

    /**
     * Pick a stable pastel based on an id — same id always maps to the same colour,
     * giving each note a consistent "mood" without the user picking it.
     */
    @Composable
    fun tintFor(id: Int): Color {
        val palette = listOf(pink(), mint(), lavender(), peach(), sky(), butter())
        val idx = ((id % palette.size) + palette.size) % palette.size
        return palette[idx]
    }
}

/** Bouncy spring used everywhere — the signature "squish" of the Playful vibe. */
fun bouncySpec(): androidx.compose.animation.core.SpringSpec<Float> =
    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

/**
 * A cute blob mascot drawn entirely on Canvas so the app ships with zero new
 * image assets. Blinks, breathes (scale loop) and gently wobbles — like a
 * little creature living on the screen.
 */
@Composable
fun CuteMascot(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    bodyColor: Color = PlayfulPalette.lavender(),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    eyesOpen: Boolean = true
) {
    val infinite = rememberInfiniteTransition(label = "MascotLife")

    // Breathing: slow scale loop so the mascot feels alive.
    val breath by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Breath"
    )

    // Wobble: tiny rotation back and forth.
    val wobble by infinite.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Wobble"
    )

    // Blink: quick scaleY dip every ~3s using a piecewise value.
    val blink by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3200
                1f at 0
                1f at 2800
                0.1f at 2950
                1f at 3100
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "Blink"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = breath
                    scaleY = breath
                    rotationZ = wobble
                }
        ) {
            val w = this.size.width
            val h = this.size.height

            // Body — a squircle blob
            val blob = Path().apply {
                val corner = w * 0.42f
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = w * 0.08f,
                        top = h * 0.12f,
                        right = w * 0.92f,
                        bottom = h * 0.92f,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
                    )
                )
            }
            drawPath(blob, bodyColor)

            // Soft blush cheeks
            drawCircle(
                color = accentColor.copy(alpha = 0.28f),
                radius = w * 0.08f,
                center = Offset(w * 0.28f, h * 0.62f)
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.28f),
                radius = w * 0.08f,
                center = Offset(w * 0.72f, h * 0.62f)
            )

            // Eyes — scaleY controlled by blink
            val eyeR = w * 0.065f
            val eyeY = h * 0.48f
            val leftEye = Offset(w * 0.35f, eyeY)
            val rightEye = Offset(w * 0.65f, eyeY)
            val eyeColor = if (eyesOpen) Color(0xFF1F1B3A) else bodyColor

            // Use scale to "blink"
            val squish = blink
            drawOval(
                color = eyeColor,
                topLeft = Offset(leftEye.x - eyeR, leftEye.y - eyeR * squish),
                size = Size(eyeR * 2, eyeR * 2 * squish)
            )
            drawOval(
                color = eyeColor,
                topLeft = Offset(rightEye.x - eyeR, rightEye.y - eyeR * squish),
                size = Size(eyeR * 2, eyeR * 2 * squish)
            )

            // Smile — small curve below the eyes
            val smilePath = Path().apply {
                moveTo(w * 0.42f, h * 0.72f)
                quadraticBezierTo(
                    w * 0.5f, h * 0.80f,
                    w * 0.58f, h * 0.72f
                )
            }
            drawPath(
                path = smilePath,
                color = Color(0xFF1F1B3A),
                style = Stroke(
                    width = w * 0.028f,
                    pathEffect = PathEffect.cornerPathEffect(w * 0.04f)
                )
            )
        }
    }
}

/**
 * Floating pastel shapes in the background — used behind empty states and
 * headers to add depth without being busy.
 */
@Composable
fun PastelBlobBackground(
    modifier: Modifier = Modifier,
    alpha: Float = 0.5f
) {
    val infinite = rememberInfiniteTransition(label = "BlobBg")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Drift"
    )

    val c1 = PlayfulPalette.pink().copy(alpha = alpha)
    val c2 = PlayfulPalette.mint().copy(alpha = alpha)
    val c3 = PlayfulPalette.sky().copy(alpha = alpha)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = c1,
            radius = w * 0.30f,
            center = Offset(w * (0.15f + drift * 0.08f), h * (0.18f + drift * 0.04f))
        )
        drawCircle(
            color = c2,
            radius = w * 0.22f,
            center = Offset(w * (0.82f - drift * 0.06f), h * (0.32f - drift * 0.05f))
        )
        drawCircle(
            color = c3,
            radius = w * 0.26f,
            center = Offset(w * (0.30f + drift * 0.05f), h * (0.78f - drift * 0.07f))
        )
    }
}

/** Squircle-ish shape used for the Playful vibe's cards + chips. */
val CuteCardShape = RoundedCornerShape(28.dp)
val CuteChipShape = RoundedCornerShape(20.dp)
val CuteButtonShape = RoundedCornerShape(24.dp)

/**
 * A small floating pastel bubble that wraps any content. Handy for cute chips,
 * greetings, and header accents.
 */
@Composable
fun PastelBubble(
    modifier: Modifier = Modifier,
    tint: Color = PlayfulPalette.pink(),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = CuteChipShape,
        color = tint,
        tonalElevation = 0.dp
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) { content() }
    }
}
