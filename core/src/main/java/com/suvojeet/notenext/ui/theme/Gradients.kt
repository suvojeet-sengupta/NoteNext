package com.suvojeet.notenext.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    fun getGradientBrush(colorInt: Int): Brush {
        val color = Color(colorInt)
        
        return when {
            // Blue/Purple Aesthetic
            isSimilar(color, Color(0xFFD7AEFB)) || isSimilar(color, Color(0xFF7986CB)) || isSimilar(color, Color(0xFF9FA8DA)) || isSimilar(color, Color(0xFF4A148C)) || isSimilar(color, Color(0xFF1A237E)) -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFA9C9FF),
                        Color(0xFF99A3FF),
                        Color(0xFFBFA3FF)
                    )
                )
            }
            // Green/Yellow Aesthetic
            isSimilar(color, Color(0xFFCCFF90)) || isSimilar(color, Color(0xFFE6C9A8)) || isSimilar(color, Color(0xFFFFF475)) || isSimilar(color, Color(0xFF2E7D32)) || isSimilar(color, Color(0xFFF57F17)) -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFDFFAE),
                        Color(0xFFDBF3A6),
                        Color(0xFFA7E2A8)
                    )
                )
            }
            // Pink/Red Aesthetic
            isSimilar(color, Color(0xFFF28B82)) || isSimilar(color, Color(0xFFFDCFE8)) || isSimilar(color, Color(0xFFB71C1C)) || isSimilar(color, Color(0xFF880E4F)) -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFD1D1),
                        Color(0xFFFF9E9E),
                        Color(0xFFF48FB1)
                    )
                )
            }
            // Orange Aesthetic
            isSimilar(color, Color(0xFFFCBC05)) || isSimilar(color, Color(0xFFE65100)) -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFE0B2),
                        Color(0xFFFFCC80),
                        Color(0xFFFFB74D)
                    )
                )
            }
            // Teal/Blue Aesthetic
            isSimilar(color, Color(0xFFA7FFEB)) || isSimilar(color, Color(0xFFCBF0F8)) || isSimilar(color, Color(0xFF006064)) || isSimilar(color, Color(0xFF01579B)) -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE0F7FA),
                        Color(0xFF80DEEA),
                        Color(0xFF4DD0E1)
                    )
                )
            }
            // Dark Gradient (If note color is dark gray/black/brown)
            color.red < 0.35f && color.green < 0.35f && color.blue < 0.35f && color.alpha > 0.5f -> {
                 Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3D3D3D),
                        Color(0xFF2B2B2B)
                    )
                )
            }
            // Gray Aesthetic
            isSimilar(color, Color(0xFFE8EAED)) || isSimilar(color, Color(0xFF424242)) -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF5F5F5),
                        Color(0xFFE0E0E0),
                        Color(0xFFBDBDBD)
                    )
                )
            }
            // Fallback for other custom colors
            else -> {
                Brush.linearGradient(
                    colors = listOf(
                        color.copy(alpha = 0.7f),
                        color
                    )
                )
            }
        }
    }

    private fun isSimilar(c1: Color, c2: Color, threshold: Double = 0.15): Boolean {
        val r = c1.red - c2.red
        val g = c1.green - c2.green
        val b = c1.blue - c2.blue
        return (r*r + g*g + b*b) < threshold
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


