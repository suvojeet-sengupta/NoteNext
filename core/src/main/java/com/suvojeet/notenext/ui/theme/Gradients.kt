package com.suvojeet.notenext.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object NoteGradients {

    fun getGradientBrush(colorInt: Int): Brush {
        val color = Color(colorInt)
        
        return when {
            // Blue/Purple Aesthetic
            isSimilar(color, Color(0xFFD7AEFB)) || isSimilar(color, Color(0xFF7986CB)) || isSimilar(color, Color(0xFF9FA8DA)) -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFA9C9FF),
                        Color(0xFF99A3FF),
                        Color(0xFFBFA3FF)
                    )
                )
            }
            // Green/Yellow Aesthetic
            isSimilar(color, Color(0xFFCCFF90)) || isSimilar(color, Color(0xFFE6C9A8)) || isSimilar(color, Color(0xFFFFF475)) -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFDFFAE),
                        Color(0xFFDBF3A6),
                        Color(0xFFA7E2A8)
                    )
                )
            }
            // Pink/Red Aesthetic
            isSimilar(color, Color(0xFFF28B82)) || isSimilar(color, Color(0xFFFDCFE8)) -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFD1D1),
                        Color(0xFFFF9E9E),
                        Color(0xFFF48FB1)
                    )
                )
            }
            // Teal/Blue Aesthetic
            isSimilar(color, Color(0xFFA7FFEB)) || isSimilar(color, Color(0xFFCBF0F8)) -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE0F7FA),
                        Color(0xFF80DEEA),
                        Color(0xFF4DD0E1)
                    )
                )
            }
            // Dark Gradient (If note color is dark gray/black)
            color.red < 0.2f && color.green < 0.2f && color.blue < 0.2f && color.alpha > 0.5f -> {
                 Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2B2B2B),
                        Color(0xFF1F1F1F)
                    )
                )
            }
            // Fallback for other custom colors
            else -> {
                Brush.linearGradient(
                    colors = listOf(
                        color.copy(alpha = 0.6f),
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
        val color = Color(colorInt)
        val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
        return if (luminance > 0.5) Color(0xFF1C1B1F) else Color.White
    }
}


