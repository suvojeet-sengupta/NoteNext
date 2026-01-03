package com.suvojeet.notenext.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb

object NoteGradients {

    // Unified light theme colors (index-matched with darkNoteColors)
    val lightNoteColors = listOf(
        Color(0xFFF28B82).toArgb(), // 0: Red
        Color(0xFFFCBC05).toArgb(), // 1: Orange
        Color(0xFFFFF475).toArgb(), // 2: Yellow
        Color(0xFFCCFF90).toArgb(), // 3: Green
        Color(0xFFA7FFEB).toArgb(), // 4: Teal
        Color(0xFFCBF0F8).toArgb(), // 5: Light Blue
        Color(0xFFAFCBFA).toArgb(), // 6: Blue
        Color(0xFFD7AEFB).toArgb(), // 7: Purple
        Color(0xFFFDCFE8).toArgb(), // 8: Pink
        Color(0xFFE6C9A8).toArgb(), // 9: Brown
        Color(0xFFE8EAED).toArgb()  // 10: Gray
    )

    // Unified dark theme colors (index-matched with lightNoteColors)
    val darkNoteColors = listOf(
        Color(0xFFB71C1C).toArgb(), // 0: Dark Red
        Color(0xFFE65100).toArgb(), // 1: Dark Orange
        Color(0xFFF57F17).toArgb(), // 2: Dark Yellow
        Color(0xFF2E7D32).toArgb(), // 3: Dark Green
        Color(0xFF006064).toArgb(), // 4: Dark Teal
        Color(0xFF01579B).toArgb(), // 5: Dark Blue
        Color(0xFF1A237E).toArgb(), // 6: Very Dark Blue
        Color(0xFF4A148C).toArgb(), // 7: Dark Purple
        Color(0xFF880E4F).toArgb(), // 8: Dark Pink
        Color(0xFF3E2723).toArgb(), // 9: Dark Brown
        Color(0xFF424242).toArgb()  // 10: Dark Gray
    )

    fun getNoteColors(isDarkTheme: Boolean): List<Int> {
        return if (isDarkTheme) darkNoteColors else lightNoteColors
    }

    /**
     * Maps the stored color to the appropriate color for the current theme.
     * If a dark color is stored but user is in light mode, returns the light equivalent.
     */
    fun getAdaptiveColor(storedColor: Int, isDarkTheme: Boolean): Int {
        if (storedColor == 0) return 0 // No color set
        
        // Check if color exists in light palette
        val lightIndex = lightNoteColors.indexOf(storedColor)
        if (lightIndex != -1) {
            // Color is from light palette, return appropriate for current theme
            return if (isDarkTheme) darkNoteColors[lightIndex] else storedColor
        }
        
        // Check if color exists in dark palette
        val darkIndex = darkNoteColors.indexOf(storedColor)
        if (darkIndex != -1) {
            // Color is from dark palette, return appropriate for current theme
            return if (isDarkTheme) storedColor else lightNoteColors[darkIndex]
        }
        
        // Color not in our palettes (custom color), return as-is
        return storedColor
    }

    // Simple solid color brush instead of gradient
    fun getColorBrush(colorInt: Int): Brush {
        return SolidColor(Color(colorInt))
    }

    fun getContentColor(colorInt: Int): Color {
        if (colorInt == 0) return Color.Unspecified // Default color, let theme handle it
        
        val color = Color(colorInt)
        val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
        
        // For light backgrounds (luminance > 0.5), use dark text
        // For dark backgrounds (luminance <= 0.5), use light text
        return if (luminance > 0.5) {
            Color(0xFF1C1B1F) // Dark text for light backgrounds
        } else {
            Color(0xFFFFFBFE) // Light text for dark backgrounds
        }
    }
}
