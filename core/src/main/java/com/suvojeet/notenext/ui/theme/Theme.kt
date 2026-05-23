package com.suvojeet.notenext.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val lightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

private fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color(0xFF121212),
    surfaceContainerHighest = Color(0xFF1E1E1E)
)

// Mocha — warm brown editorial dark mood.
private val mochaColorScheme = darkColorScheme.copy(
    background = Color(0xFF2A1F18),
    onBackground = Color(0xFFEEE2D5),
    surface = Color(0xFF2A1F18),
    onSurface = Color(0xFFEEE2D5),
    surfaceVariant = Color(0xFF3A2C22),
    onSurfaceVariant = Color(0xFFB6A493),
    surfaceContainerLowest = Color(0xFF221A13),
    surfaceContainerLow = Color(0xFF31251C),
    surfaceContainer = Color(0xFF362A20),
    surfaceContainerHigh = Color(0xFF433428),
    surfaceContainerHighest = Color(0xFF503E30),
    primary = Color(0xFFEEE2D5),
    onPrimary = Color(0xFF2A1F18),
    primaryContainer = Color(0xFF433428),
    onPrimaryContainer = Color(0xFFEEE2D5),
    secondary = Color(0xFFD2C0AC),
    onSecondaryContainer = Color(0xFFEEE2D5),
    tertiary = NightAccent,
    onTertiary = Color(0xFF2A1F18),
    outline = Color(0xFF6A5847),
    outlineVariant = Color(0xFF3A2C22),
    inverseSurface = Color(0xFFEEE2D5),
    inverseOnSurface = Color(0xFF31251C),
)

// Sage — muted green editorial dark mood.
private val sageColorScheme = darkColorScheme.copy(
    background = Color(0xFF29382E),
    onBackground = Color(0xFFE6EDE2),
    surface = Color(0xFF29382E),
    onSurface = Color(0xFFE6EDE2),
    surfaceVariant = Color(0xFF36473B),
    onSurfaceVariant = Color(0xFFA9B8A2),
    surfaceContainerLowest = Color(0xFF223025),
    surfaceContainerLow = Color(0xFF2F4034),
    surfaceContainer = Color(0xFF344539),
    surfaceContainerHigh = Color(0xFF405344),
    surfaceContainerHighest = Color(0xFF4C6150),
    primary = Color(0xFFE6EDE2),
    onPrimary = Color(0xFF29382E),
    primaryContainer = Color(0xFF405344),
    onPrimaryContainer = Color(0xFFE6EDE2),
    secondary = Color(0xFFC4D2BC),
    onSecondaryContainer = Color(0xFFE6EDE2),
    tertiary = NightAccent,
    onTertiary = Color(0xFF29382E),
    outline = Color(0xFF647A68),
    outlineVariant = Color(0xFF36473B),
    inverseSurface = Color(0xFFE6EDE2),
    inverseOnSurface = Color(0xFF2F4034),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoteNextTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Bespoke editorial moods always win — they ignore dynamic color so the
    // Ink & Paper identity is preserved.
    val bespoke: ColorScheme? = when (themeMode) {
        ThemeMode.MOCHA -> mochaColorScheme
        ThemeMode.SAGE -> sageColorScheme
        else -> null
    }

    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED, ThemeMode.MOCHA, ThemeMode.SAGE -> true
    }

    val colorScheme = bespoke ?: when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (themeMode == ThemeMode.AMOLED) scheme.amoled() else scheme
        }
        darkTheme -> if (themeMode == ThemeMode.AMOLED) darkColorScheme.amoled() else darkColorScheme
        else -> lightColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
