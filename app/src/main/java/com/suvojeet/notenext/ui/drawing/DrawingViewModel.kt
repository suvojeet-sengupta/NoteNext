package com.suvojeet.notenext.ui.drawing

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

private const val MAX_HISTORY = 60
private const val MAX_RECENT_COLORS = 8
private const val EXPORT_PADDING = 48f
private const val MAX_EXPORT_PX = 3072

@HiltViewModel
class DrawingViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(DrawingState())
    val state: StateFlow<DrawingState> = _state.asStateFlow()

    /** Strokes as they were when the current rub-out began, so it undoes in one step. */
    private var eraseSessionSnapshot: List<DrawingStroke>? = null

    fun onEvent(event: DrawingEvent) {
        when (event) {
            is DrawingEvent.CommitStroke -> commitStroke(event)

            DrawingEvent.BeginStrokeErase -> {
                eraseSessionSnapshot = _state.value.strokes
            }
            is DrawingEvent.StrokeEraseAt -> {
                val center = Offset(event.x, event.y)
                _state.update { current ->
                    val remaining = current.strokes.filterNot { it.hitBy(center, event.radius) }
                    if (remaining.size == current.strokes.size) current
                    else current.copy(strokes = remaining)
                }
            }
            DrawingEvent.EndStrokeErase -> {
                val before = eraseSessionSnapshot
                eraseSessionSnapshot = null
                if (before != null && before.size != _state.value.strokes.size) {
                    _state.update {
                        it.copy(
                            undoStack = it.undoStack.pushHistory(before),
                            redoStack = emptyList()
                        )
                    }
                }
            }

            DrawingEvent.Undo -> _state.update { current ->
                val previous = current.undoStack.lastOrNull() ?: return@update current
                current.copy(
                    strokes = previous,
                    undoStack = current.undoStack.dropLast(1),
                    redoStack = current.redoStack.pushHistory(current.strokes)
                )
            }

            DrawingEvent.Redo -> _state.update { current ->
                val next = current.redoStack.lastOrNull() ?: return@update current
                current.copy(
                    strokes = next,
                    redoStack = current.redoStack.dropLast(1),
                    undoStack = current.undoStack.pushHistory(current.strokes)
                )
            }

            DrawingEvent.ClearAll -> _state.update { current ->
                if (current.strokes.isEmpty()) current
                else current.copy(
                    strokes = emptyList(),
                    undoStack = current.undoStack.pushHistory(current.strokes),
                    redoStack = emptyList()
                )
            }

            is DrawingEvent.SelectTool -> _state.update { current ->
                current.copy(
                    tool = event.tool,
                    toolBeforeEraser = if (event.tool.isEraser) current.toolBeforeEraser else event.tool
                )
            }

            is DrawingEvent.SelectColor -> _state.update { current ->
                current.copy(
                    color = event.color,
                    // Picking ink implies you want to draw, not erase.
                    tool = if (current.tool.isEraser) current.toolBeforeEraser else current.tool,
                    recentColors = (listOf(event.color) + current.recentColors.filter { it != event.color })
                        .take(MAX_RECENT_COLORS)
                )
            }

            is DrawingEvent.SetWidth -> _state.update { current ->
                val w = event.width.coerceIn(1f, 200f)
                when {
                    current.tool.isEraser -> current.copy(eraserWidth = w)
                    current.tool.isHighlighter -> current.copy(highlighterWidth = w)
                    else -> current.copy(brushWidth = w)
                }
            }

            is DrawingEvent.SetOpacity -> _state.update { current ->
                val a = event.alpha.coerceIn(0.05f, 1f)
                if (current.tool.isHighlighter) current.copy(highlighterAlpha = a)
                else current.copy(opacity = a)
            }

            is DrawingEvent.SetEraserMode -> _state.update { it.copy(eraserMode = event.mode) }

            is DrawingEvent.SetPressureSensitivity ->
                _state.update { it.copy(pressureSensitivity = event.value.coerceIn(0f, 1f)) }
            is DrawingEvent.SetSmoothing -> _state.update { it.copy(smoothing = event.enabled) }
            is DrawingEvent.SetStylusOnly -> _state.update { it.copy(stylusOnly = event.enabled) }

            DrawingEvent.StylusDetected -> _state.update { current ->
                // Arm palm rejection automatically the first time a pen touches the glass.
                if (current.hasSeenStylus) current
                else current.copy(hasSeenStylus = true, stylusOnly = true)
            }

            is DrawingEvent.SetPaperStyle -> _state.update { it.copy(paperStyle = event.style) }
            is DrawingEvent.SetPaperTint -> _state.update { current ->
                // Keep black ink off a black page: retint default ink when the page flips.
                val inkWasDefault = current.color == current.paperTint.defaultInk
                current.copy(
                    paperTint = event.tint,
                    color = if (inkWasDefault) event.tint.defaultInk else current.color
                )
            }

            is DrawingEvent.TransformCanvas -> _state.update { current ->
                val newScale = (current.scale * event.zoom).coerceIn(MIN_CANVAS_SCALE, MAX_CANVAS_SCALE)
                val applied = newScale / current.scale
                // Keep the pinch centroid pinned to the same page point.
                val newOffset = (current.offset - event.centroid) * applied + event.centroid + event.pan
                current.copy(scale = newScale, offset = newOffset)
            }

            is DrawingEvent.ZoomBy -> _state.update { current ->
                val newScale = (current.scale * event.factor).coerceIn(MIN_CANVAS_SCALE, MAX_CANVAS_SCALE)
                val applied = newScale / current.scale
                val newOffset = (current.offset - event.pivot) * applied + event.pivot
                current.copy(scale = newScale, offset = newOffset)
            }

            DrawingEvent.ResetView -> _state.update { it.copy(scale = 1f, offset = Offset.Zero) }

            DrawingEvent.FitToContent -> _state.update { current ->
                val bounds = current.strokes.contentBounds() ?: return@update current
                val viewport = current.viewport
                if (viewport.width <= 0f || viewport.height <= 0f) return@update current
                if (bounds.width <= 0f || bounds.height <= 0f) return@update current
                val margin = 0.9f
                val fit = min(
                    viewport.width * margin / bounds.width,
                    viewport.height * margin / bounds.height
                ).coerceIn(MIN_CANVAS_SCALE, MAX_CANVAS_SCALE)
                val centreX = (bounds.left + bounds.right) / 2f
                val centreY = (bounds.top + bounds.bottom) / 2f
                current.copy(
                    scale = fit,
                    offset = Offset(
                        viewport.width / 2f - centreX * fit,
                        viewport.height / 2f - centreY * fit
                    )
                )
            }

            is DrawingEvent.SetViewport -> _state.update { it.copy(viewport = event.size) }

            DrawingEvent.TogglePropertiesPanel ->
                _state.update { it.copy(showPropertiesPanel = !it.showPropertiesPanel) }
            DrawingEvent.ToggleBrushSettings ->
                _state.update { it.copy(showBrushSettings = !it.showBrushSettings) }
            DrawingEvent.ToggleImmersive ->
                _state.update { it.copy(isImmersive = !it.isImmersive) }
            is DrawingEvent.SetTrimToContent -> _state.update { it.copy(trimToContent = event.enabled) }
            is DrawingEvent.SetTransparentBackground ->
                _state.update { it.copy(transparentBackground = event.enabled) }

            is DrawingEvent.SaveDrawing -> saveDrawing(event.context, event.onSaveComplete)
        }
    }

    private fun commitStroke(event: DrawingEvent.CommitStroke) {
        if (event.points.isEmpty()) return
        _state.update { current ->
            val tool = event.tool
            val width = when {
                tool.isEraser -> current.eraserWidth
                tool.isHighlighter -> current.highlighterWidth
                else -> current.brushWidth
            }
            val alpha = if (tool.isHighlighter) current.highlighterAlpha else current.opacity
            val stroke = DrawingStroke(
                id = System.nanoTime(),
                tool = tool,
                color = if (tool.isEraser) Color.Transparent else current.color,
                width = width,
                alpha = if (tool.isEraser) 1f else alpha,
                points = event.points
            )
            current.copy(
                strokes = current.strokes + stroke,
                undoStack = current.undoStack.pushHistory(current.strokes),
                redoStack = emptyList()
            )
        }
    }

    private fun List<List<DrawingStroke>>.pushHistory(snapshot: List<DrawingStroke>): List<List<DrawingStroke>> {
        val next = this + listOf(snapshot)
        return if (next.size > MAX_HISTORY) next.takeLast(MAX_HISTORY) else next
    }

    // ── Export ────────────────────────────────────────────────────────────

    private fun saveDrawing(context: Context, onSaveComplete: (Uri?) -> Unit) {
        val snapshot = _state.value
        if (snapshot.strokes.isEmpty()) {
            onSaveComplete(null)
            return
        }
        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val uri = runCatching { renderToFile(context, snapshot) }
                .onFailure { it.printStackTrace() }
                .getOrNull()
            withContext(Dispatchers.Main) {
                _state.update { it.copy(isSaving = false) }
                onSaveComplete(uri)
            }
        }
    }

    private fun renderToFile(context: Context, snapshot: DrawingState): Uri? {
        val content = snapshot.strokes.contentBounds() ?: return null

        val area: Rect = if (snapshot.trimToContent) {
            Rect(
                content.left - EXPORT_PADDING,
                content.top - EXPORT_PADDING,
                content.right + EXPORT_PADDING,
                content.bottom + EXPORT_PADDING
            )
        } else {
            // "Whole page": everything the user can currently see, plus any ink
            // that has drifted outside the viewport.
            val viewport = snapshot.viewport
            if (viewport.width <= 0f || viewport.height <= 0f) {
                content
            } else {
                val topLeft = toPageSpace(Offset.Zero, snapshot.scale, snapshot.offset)
                val bottomRight = toPageSpace(
                    Offset(viewport.width, viewport.height), snapshot.scale, snapshot.offset
                )
                Rect(
                    min(topLeft.x, content.left),
                    min(topLeft.y, content.top),
                    max(bottomRight.x, content.right),
                    max(bottomRight.y, content.bottom)
                )
            }
        }

        val width = max(1f, area.width)
        val height = max(1f, area.height)
        // Supersample so hairline nibs survive the trip into the note, then pull
        // back if that would blow past the pixel cap.
        var exportScale = 2f
        val longest = max(width, height) * exportScale
        if (longest > MAX_EXPORT_PX) exportScale *= MAX_EXPORT_PX / longest

        val pixelWidth = max(1, (width * exportScale).toInt())
        val pixelHeight = max(1, (height * exportScale).toInt())

        val bitmap = Bitmap.createBitmap(pixelWidth, pixelHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        if (!snapshot.transparentBackground) {
            canvas.drawColor(snapshot.paperTint.paper.toArgb())
            drawExportRuling(canvas, snapshot, area, exportScale)
        }

        // A pixel eraser has to punch through the ink without gouging a hole in the
        // page, which needs its own layer — but that doubles the memory, so only pay
        // for it when the drawing actually contains an eraser stroke.
        val needsLayer = snapshot.strokes.any { it.tool.isEraser }
        val checkpoint = if (needsLayer) canvas.saveLayer(null, null) else canvas.save()
        canvas.scale(exportScale, exportScale)
        canvas.translate(-area.left, -area.top)
        snapshot.strokes.forEach { stroke -> drawExportStroke(canvas, stroke, snapshot) }
        canvas.restoreToCount(checkpoint)

        val file = File(context.cacheDir, "drawing_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        bitmap.recycle()
        return Uri.fromFile(file)
    }

    private fun drawExportRuling(
        canvas: android.graphics.Canvas,
        snapshot: DrawingState,
        area: Rect,
        exportScale: Float
    ) {
        if (snapshot.paperStyle == PaperStyle.PLAIN) return
        val spacing = when (snapshot.paperStyle) {
            PaperStyle.RULED -> 46f
            PaperStyle.GRID -> 40f
            PaperStyle.DOTS -> 32f
            PaperStyle.PLAIN -> return
        }
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = snapshot.paperTint.line.toArgb()
            strokeWidth = 1f * exportScale
            style = android.graphics.Paint.Style.STROKE
        }
        val firstY = kotlin.math.ceil(area.top / spacing) * spacing
        val firstX = kotlin.math.ceil(area.left / spacing) * spacing

        fun px(pageX: Float) = (pageX - area.left) * exportScale
        fun py(pageY: Float) = (pageY - area.top) * exportScale

        when (snapshot.paperStyle) {
            PaperStyle.RULED -> {
                var y = firstY
                while (y <= area.bottom) {
                    canvas.drawLine(0f, py(y), (area.width) * exportScale, py(y), paint)
                    y += spacing
                }
            }
            PaperStyle.GRID -> {
                var y = firstY
                while (y <= area.bottom) {
                    canvas.drawLine(0f, py(y), (area.width) * exportScale, py(y), paint)
                    y += spacing
                }
                var x = firstX
                while (x <= area.right) {
                    canvas.drawLine(px(x), 0f, px(x), (area.height) * exportScale, paint)
                    x += spacing
                }
            }
            PaperStyle.DOTS -> {
                paint.style = android.graphics.Paint.Style.FILL
                var y = firstY
                while (y <= area.bottom) {
                    var x = firstX
                    while (x <= area.right) {
                        canvas.drawCircle(px(x), py(y), 1.6f * exportScale, paint)
                        x += spacing
                    }
                    y += spacing
                }
            }
            PaperStyle.PLAIN -> Unit
        }
    }

    private fun drawExportStroke(
        canvas: android.graphics.Canvas,
        stroke: DrawingStroke,
        snapshot: DrawingState
    ) {
        if (stroke.points.isEmpty()) return
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isDither = true
            style = android.graphics.Paint.Style.STROKE
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeCap = when (stroke.tool) {
                DrawingTool.MARKER, DrawingTool.HIGHLIGHTER -> android.graphics.Paint.Cap.SQUARE
                else -> android.graphics.Paint.Cap.ROUND
            }
            strokeWidth = stroke.width
        }

        when (stroke.tool) {
            DrawingTool.ERASER -> {
                paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                paint.color = android.graphics.Color.BLACK
            }
            DrawingTool.HIGHLIGHTER -> {
                paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY)
                paint.color = stroke.color.copy(alpha = stroke.alpha).toArgb()
            }
            else -> {
                paint.xfermode = null
                paint.color = stroke.color.copy(alpha = stroke.alpha).toArgb()
            }
        }

        if (stroke.tool.isShape) {
            drawExportShape(canvas, stroke, paint)
            return
        }

        if (stroke.points.size == 1) {
            val p = stroke.points[0]
            paint.style = android.graphics.Paint.Style.FILL
            canvas.drawCircle(p.x, p.y, max(0.5f, stroke.width / 2f), paint)
            return
        }

        if (!stroke.tool.supportsPressure) {
            val path = (if (snapshot.smoothing) stroke.points.toSmoothPath() else stroke.points.toPolylinePath())
            canvas.drawPath(path.asAndroidPath(), paint)
            return
        }

        paint.strokeCap = android.graphics.Paint.Cap.ROUND
        val baseAlpha = stroke.alpha
        for (i in 1 until stroke.points.size) {
            val a = stroke.points[i - 1]
            val b = stroke.points[i]
            paint.strokeWidth = max(
                0.5f,
                stroke.width * pressureFactor((a.pressure + b.pressure) / 2f, snapshot.pressureSensitivity)
            )
            val segmentAlpha = if (stroke.tool == DrawingTool.PENCIL) {
                (baseAlpha * (0.55f + 0.45f * b.pressure.coerceIn(0f, 1f))).coerceIn(0.05f, 1f)
            } else {
                baseAlpha
            }
            paint.color = stroke.color.copy(alpha = segmentAlpha).toArgb()
            canvas.drawLine(a.x, a.y, b.x, b.y, paint)
        }
    }

    private fun drawExportShape(
        canvas: android.graphics.Canvas,
        stroke: DrawingStroke,
        paint: android.graphics.Paint
    ) {
        val start = stroke.points.first()
        val end = stroke.points.last()
        when (stroke.tool) {
            DrawingTool.LINE -> canvas.drawLine(start.x, start.y, end.x, end.y, paint)
            DrawingTool.ARROW -> {
                canvas.drawLine(start.x, start.y, end.x, end.y, paint)
                val angle = kotlin.math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
                val head = max(
                    stroke.width * 3.5f,
                    kotlin.math.hypot(end.x - start.x, end.y - start.y) * 0.18f
                )
                for (side in intArrayOf(-1, 1)) {
                    val a = angle + Math.PI + 0.42 * side
                    canvas.drawLine(
                        end.x, end.y,
                        end.x + (head * kotlin.math.cos(a)).toFloat(),
                        end.y + (head * kotlin.math.sin(a)).toFloat(),
                        paint
                    )
                }
            }
            DrawingTool.RECTANGLE -> canvas.drawRect(
                min(start.x, end.x), min(start.y, end.y),
                max(start.x, end.x), max(start.y, end.y),
                paint
            )
            DrawingTool.OVAL -> canvas.drawOval(
                min(start.x, end.x), min(start.y, end.y),
                max(start.x, end.x), max(start.y, end.y),
                paint
            )
            else -> Unit
        }
    }
}
