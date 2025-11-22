package com.suvojeet.notenext.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object NoteGradients {

    fun getGradientBrush(colorInt: Int): Brush {
        val color = Color(colorInt)
        
        // Special handling for specific vibrant colors to match the reference image style
        // We match roughly based on the hue/values used in the app's palette
        return when {
            // Blue/Purple Aesthetic (Like the "toota jo" note)
            isSimilar(color, Color(0xFFD7AEFB)) || isSimilar(color, Color(0xFF7986CB)) || isSimilar(color, Color(0xFF9FA8DA)) -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFA9C9FF), // Light Blue
                        Color(0xFF99A3FF), // Periwinkle
                        Color(0xFFBFA3FF)  // Soft Purple
                    )
                )
            }
            // Green/Yellow Aesthetic (Like the "National Assembly" note)
            isSimilar(color, Color(0xFFCCFF90)) || isSimilar(color, Color(0xFFE6C9A8)) || isSimilar(color, Color(0xFFFFF475)) -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFDFFAE), // Pale Yellow
                        Color(0xFFDBF3A6), // Lime Greenish
                        Color(0xFFA7E2A8)  // Soft Green
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
            // Default/White/Dark - Subtle gradient
            else -> {
                // If it's very light (default white), give it a subtle surface gradient
                if (color.red > 0.9f && color.green > 0.9f && color.blue > 0.9f) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF5F5F5)
                        )
                    )
                } else if (color.red < 0.2f && color.green < 0.2f && color.blue < 0.2f) {
                    // Dark mode default
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2B2B2B),
                            Color(0xFF1F1F1F)
                        )
                    )
                } else {
                    // Generic gradient for other custom colors
                    Brush.linearGradient(
                        colors = listOf(
                            color.copy(alpha = 0.7f),
                            color
                        )
                    )
                }
            }
        }
    }

    private fun isSimilar(c1: Color, c2: Color, threshold: Double = 0.15): Boolean {
        val r = c1.red - c2.red
        val g = c1.green - c2.green
        val b = c1.blue - c2.blue
        return (r*r + g*g + b*b) < threshold
    }

    // Helper to determine best text color (Black or White) based on background luminance
    fun getContentColor(colorInt: Int): Color {
        val color = Color(colorInt)
        // Calculate luminance
        val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
        return if (luminance > 0.5) Color(0xFF1C1B1F) else Color.White
    }
}

