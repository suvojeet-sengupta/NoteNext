package com.suvojeet.notenext.ui.drawing

import android.content.Context
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

sealed class DrawingEvent {

    // ── Ink ────────────────────────────────────────────────────────
    /** A finished freehand or shape stroke, already in page coordinates. */
    data class CommitStroke(val points: List<StrokePoint>, val tool: DrawingTool) : DrawingEvent()

    /** Stroke-eraser session boundaries, so a whole rub-out is one undo step. */
    object BeginStrokeErase : DrawingEvent()
    data class StrokeEraseAt(val x: Float, val y: Float, val radius: Float) : DrawingEvent()
    object EndStrokeErase : DrawingEvent()

    object Undo : DrawingEvent()
    object Redo : DrawingEvent()
    object ClearAll : DrawingEvent()

    // ── Tool ───────────────────────────────────────────────────────
    data class SelectTool(val tool: DrawingTool) : DrawingEvent()
    data class SelectColor(val color: Color) : DrawingEvent()
    data class SetWidth(val width: Float) : DrawingEvent()
    data class SetOpacity(val alpha: Float) : DrawingEvent()
    data class SetEraserMode(val mode: EraserMode) : DrawingEvent()

    // ── Stylus ─────────────────────────────────────────────────────
    data class SetPressureSensitivity(val value: Float) : DrawingEvent()
    data class SetSmoothing(val enabled: Boolean) : DrawingEvent()
    data class SetStylusOnly(val enabled: Boolean) : DrawingEvent()
    /** Raised the first time a stylus sample arrives, so palm rejection arms itself once. */
    object StylusDetected : DrawingEvent()

    // ── Page ───────────────────────────────────────────────────────
    data class SetPaperStyle(val style: PaperStyle) : DrawingEvent()
    data class SetPaperTint(val tint: PaperTint) : DrawingEvent()

    // ── Viewport ───────────────────────────────────────────────────
    data class TransformCanvas(val pan: Offset, val zoom: Float, val centroid: Offset) : DrawingEvent()
    data class ZoomBy(val factor: Float, val pivot: Offset) : DrawingEvent()
    object ResetView : DrawingEvent()
    object FitToContent : DrawingEvent()
    data class SetViewport(val size: Size) : DrawingEvent()

    // ── Chrome / export ────────────────────────────────────────────
    object TogglePropertiesPanel : DrawingEvent()
    object ToggleBrushSettings : DrawingEvent()
    object ToggleImmersive : DrawingEvent()
    data class SetTrimToContent(val enabled: Boolean) : DrawingEvent()
    data class SetTransparentBackground(val enabled: Boolean) : DrawingEvent()

    data class SaveDrawing(val context: Context, val onSaveComplete: (Uri?) -> Unit) : DrawingEvent()
}
