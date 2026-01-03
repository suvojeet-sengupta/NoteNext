package com.suvojeet.notenext.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb

object NoteGradients {

    // Unified light theme colors
    val lightNoteColors = listOf(
        Color(0xFFF28B82).toArgb(), // Red
        Color(0xFFFCBC05).toArgb(), // Orange
        Color(0xFFFFF475).toArgb(), // Yellow
        Color(0xFFCCFF90).toArgb(), // Green
        Color(0xFFA7FFEB).toArgb(), // Teal
        Color(0xFFCBF0F8).toArgb(), // Light Blue
        Color(0xFFAFCBFA).toArgb(), // Blue
        Color(0xFFD7AEFB).toArgb(), // Purple
        Color(0xFFFDCFE8).toArgb(), // Pink
        Color(0xFFE6C9A8).toArgb(), // Brown
        Color(0xFFE8EAED).toArgb()  // Gray
    )

    // Unified dark theme colors
    val darkNoteColors = listOf(
        Color(0xFFB71C1C).toArgb(), // Dark Red
        Color(0xFFE65100).toArgb(), // Dark Orange
        Color(0xFFF57F17).toArgb(), // Dark Yellow
        Color(0xFF2E7D32).toArgb(), // Dark Green
        Color(0xFF006064).toArgb(), // Dark Teal
        Color(0xFF01579B).toArgb(), // Dark Blue
        Color(0xFF1A237E).toArgb(), // Very Dark Blue
        Color(0xFF4A148C).toArgb(), // Dark Purple
        Color(0xFF880E4F).toArgb(), // Dark Pink
        Color(0xFF3E2723).toArgb(), // Dark Brown
        Color(0xFF424242).toArgb()  // Dark Gray
    )

    fun getNoteColors(isDarkTheme: Boolean): List<Int> {
        return if (isDarkTheme) darkNoteColors else lightNoteColors
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
