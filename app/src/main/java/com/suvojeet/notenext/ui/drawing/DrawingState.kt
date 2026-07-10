package com.suvojeet.notenext.ui.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

data class DrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

data class DrawingState(
    val paths: List<DrawingPath> = emptyList(),
    val undonePaths: List<DrawingPath> = emptyList(),
    val currentColor: Color = Color.Black,
    val currentStrokeWidth: Float = 10f,
    val isEraserMode: Boolean = false,
    val isSaving: Boolean = false,
    val showBrushSettings: Boolean = false
)
