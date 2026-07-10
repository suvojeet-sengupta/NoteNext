package com.suvojeet.notenext.ui.drawing

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

sealed class DrawingEvent {
    data class PathAdded(val path: Path) : DrawingEvent()
    object Undo : DrawingEvent()
    object Redo : DrawingEvent()
    object ClearAll : DrawingEvent()
    data class ChangeColor(val color: Color) : DrawingEvent()
    data class ChangeStrokeWidth(val width: Float) : DrawingEvent()
    data class ToggleEraserMode(val isEraser: Boolean) : DrawingEvent()
    object ToggleBrushSettings : DrawingEvent()
    data class SaveDrawing(val context: Context, val onSaveComplete: (Uri?) -> Unit) : DrawingEvent()
}
