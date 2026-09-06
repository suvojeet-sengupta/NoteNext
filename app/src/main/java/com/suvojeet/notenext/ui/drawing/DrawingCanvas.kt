package com.suvojeet.notenext.ui.drawing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

/**
 * The page itself: ruling, committed ink, the stroke in flight and the nib cursor.
 *
 * Input is split three ways so a tablet feels like paper — a stylus always draws
 * (and every finger that lands while it is down is swallowed as palm), a single
 * finger draws only when palm rejection is off, and two fingers always pan and
 * zoom the page.
 */
@Composable
fun DrawingCanvas(
    state: DrawingState,
    onEvent: (DrawingEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // The gesture loop must never restart mid-stroke, so it reads the newest
    // state through these handles instead of through pointerInput keys.
    val liveState by rememberUpdatedState(state)
    val liveOnEvent by rememberUpdatedState(onEvent)

    val livePoints = remember { mutableStateListOf<StrokePoint>() }
    var liveTool by remember { mutableStateOf<DrawingTool?>(null) }
    var nibCursor by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .background(state.paperTint.paper)
            .onSizeChanged { onEvent(DrawingEvent.SetViewport(Size(it.width.toFloat(), it.height.toFloat()))) }
            // Passive hover watch: a stylus hovering over the glass previews its nib.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val hovering = event.changes.firstOrNull {
                            !it.pressed && (it.type == PointerType.Stylus || it.type == PointerType.Eraser)
                        }
                        if (hovering != null) nibCursor = hovering.position
                        else if (event.changes.none { it.pressed }) nibCursor = null
                    }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val snapshot = liveState
                    val penLike = down.type == PointerType.Stylus || down.type == PointerType.Eraser

                    if (penLike && !snapshot.hasSeenStylus) liveOnEvent(DrawingEvent.StylusDetected)

                    val drawsWithThisPointer =
                        penLike || down.type == PointerType.Mouse || !snapshot.stylusOnly

                    if (!drawsWithThisPointer) {
                        nibCursor = null
                        navigateGesture { centroid, pan, zoom ->
                            liveOnEvent(DrawingEvent.TransformCanvas(pan, zoom, centroid))
                        }
                        return@awaitEachGesture
                    }

                    // A stylus flipped to its eraser tip erases whatever tool is armed.
                    val tool = if (down.type == PointerType.Eraser) DrawingTool.ERASER else snapshot.tool
                    val rubbingOut = tool.isEraser && snapshot.eraserMode == EraserMode.STROKE

                    var lastPage: Offset? = null

                    fun pageOf(screen: Offset): Offset {
                        val s = liveState
                        return toPageSpace(screen, s.scale, s.offset)
                    }

                    val onDown: (Offset, Float) -> Unit = { position, pressure ->
                        nibCursor = position
                        val page = pageOf(position)
                        lastPage = page
                        if (rubbingOut) {
                            liveOnEvent(DrawingEvent.BeginStrokeErase)
                            liveOnEvent(
                                DrawingEvent.StrokeEraseAt(page.x, page.y, liveState.eraserWidth / 2f)
                            )
                        } else {
                            liveTool = tool
                            livePoints.clear()
                            livePoints.add(StrokePoint(page.x, page.y, samplePressure(down.type, pressure)))
                        }
                    }

                    val onMove: (Offset, Float) -> Unit = { position, pressure ->
                        nibCursor = position
                        val s = liveState
                        val page = pageOf(position)
                        if (rubbingOut) {
                            liveOnEvent(DrawingEvent.StrokeEraseAt(page.x, page.y, s.eraserWidth / 2f))
                        } else if (tool.isShape) {
                            // Rubber-band: the shape only ever has an anchor and a handle.
                            val anchor = livePoints.firstOrNull()
                            if (anchor != null) {
                                val handle = StrokePoint(page.x, page.y, 1f)
                                if (livePoints.size == 1) livePoints.add(handle) else livePoints[1] = handle
                            }
                        } else {
                            // Drop samples that are sub-pixel on screen; they only cost battery.
                            val minTravel = 1.2f / s.scale
                            val previous = lastPage
                            if (previous == null || (page - previous).getDistance() >= minTravel) {
                                lastPage = page
                                livePoints.add(
                                    StrokePoint(page.x, page.y, samplePressure(down.type, pressure))
                                )
                            }
                        }
                    }

                    val onUp: () -> Unit = {
                        if (rubbingOut) {
                            liveOnEvent(DrawingEvent.EndStrokeErase)
                        } else if (livePoints.isNotEmpty()) {
                            liveOnEvent(DrawingEvent.CommitStroke(livePoints.toList(), tool))
                        }
                        livePoints.clear()
                        liveTool = null
                        if (!penLike) nibCursor = null
                    }

                    val onCancel: () -> Unit = {
                        if (rubbingOut) liveOnEvent(DrawingEvent.EndStrokeErase)
                        livePoints.clear()
                        liveTool = null
                        nibCursor = null
                    }

                    val escalated = inkGesture(down, penLike, onDown, onMove, onUp, onCancel)
                    if (escalated) {
                        navigateGesture { centroid, pan, zoom ->
                            liveOnEvent(DrawingEvent.TransformCanvas(pan, zoom, centroid))
                        }
                    }
                }
            }
    ) {
        // 1 ── the printed page, never erasable
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPaperRuling(state.paperStyle, state.paperTint, state.scale, state.offset)
        }

        // 2 ── committed ink, in its own layer so the pixel eraser can cut through it
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            withTransform({
                translate(state.offset.x, state.offset.y)
                scale(state.scale, state.scale, pivot = Offset.Zero)
            }) {
                state.strokes.forEach { stroke ->
                    drawInkStroke(
                        tool = stroke.tool,
                        color = stroke.color,
                        width = stroke.width,
                        alpha = stroke.alpha,
                        points = stroke.points,
                        pressureSensitivity = state.pressureSensitivity,
                        smoothing = state.smoothing
                    )
                }
            }
        }

        // 3 ── the stroke in flight plus the nib cursor
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tool = liveTool
            if (tool != null && livePoints.isNotEmpty()) {
                withTransform({
                    translate(state.offset.x, state.offset.y)
                    scale(state.scale, state.scale, pivot = Offset.Zero)
                }) {
                    if (tool.isEraser) {
                        // Clearing cannot be previewed outside the ink layer, so show
                        // the swept area instead.
                        drawInkStroke(
                            tool = DrawingTool.MARKER,
                            color = state.paperTint.line,
                            width = state.eraserWidth,
                            alpha = 0.7f,
                            points = livePoints,
                            pressureSensitivity = 0f,
                            smoothing = false
                        )
                    } else {
                        drawInkStroke(
                            tool = tool,
                            color = state.color,
                            width = if (tool.isHighlighter) state.highlighterWidth else state.brushWidth,
                            alpha = state.activeAlpha,
                            points = livePoints,
                            pressureSensitivity = state.pressureSensitivity,
                            smoothing = state.smoothing
                        )
                    }
                }
            }

            val cursor = nibCursor
            if (cursor != null && liveTool == null) {
                val nib = if (state.tool.isEraser) state.eraserWidth else state.activeWidth
                val radius = max(3f, nib * state.scale / 2f)
                drawCircle(
                    color = state.paperTint.line,
                    radius = radius,
                    center = cursor,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                if (!state.tool.isEraser) {
                    drawCircle(color = state.color.copy(alpha = 0.35f), radius = radius, center = cursor)
                }
            }
        }
    }
}

/** Fingers report a meaningless pressure on most panels — treat them as full weight. */
private fun samplePressure(type: PointerType, raw: Float): Float = when (type) {
    PointerType.Stylus, PointerType.Eraser -> raw.coerceIn(0.02f, 1f)
    else -> 1f
}

/**
 * Follows one drawing pointer until it lifts. Returns true when a second finger
 * arrives on a non-pen gesture, meaning the caller should hand over to pan/zoom.
 */
private suspend fun AwaitPointerEventScope.inkGesture(
    down: PointerInputChange,
    penLike: Boolean,
    onDown: (Offset, Float) -> Unit,
    onMove: (Offset, Float) -> Unit,
    onUp: () -> Unit,
    onCancel: () -> Unit
): Boolean {
    onDown(down.position, down.pressure)
    down.consume()
    while (true) {
        val event = awaitPointerEvent()
        val others = event.changes.filter { it.id != down.id && it.pressed }
        if (others.isNotEmpty()) {
            if (penLike) {
                // Palm rejection: while the pen is on the glass, nothing else counts.
                others.forEach { it.consume() }
            } else {
                onCancel()
                return true
            }
        }
        val self = event.changes.firstOrNull { it.id == down.id }
        if (self == null || !self.pressed) {
            onUp()
            return false
        }
        if (self.positionChanged()) onMove(self.position, self.pressure)
        self.consume()
    }
}

/** Two-finger pan and pinch-zoom, modelled on detectTransformGestures. */
private suspend fun AwaitPointerEventScope.navigateGesture(
    onTransform: (centroid: Offset, pan: Offset, zoom: Float) -> Unit
) {
    var pastSlop = false
    val slop = viewConfiguration.touchSlop
    var zoomAccumulator = 1f
    var panAccumulator = Offset.Zero
    do {
        val event = awaitPointerEvent()
        val canceled = event.changes.any { it.isConsumed }
        if (!canceled) {
            val zoom = event.calculateZoom()
            val pan = event.calculatePan()
            if (!pastSlop) {
                zoomAccumulator *= zoom
                panAccumulator += pan
                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                val zoomMotion = abs(1f - zoomAccumulator) * centroidSize
                if (zoomMotion > slop || panAccumulator.getDistance() > slop) pastSlop = true
            }
            if (pastSlop) {
                val centroid = event.calculateCentroid(useCurrent = true)
                if (zoom != 1f || pan != Offset.Zero) onTransform(centroid, pan, zoom)
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        }
    } while (!canceled && event.changes.any { it.pressed })
}
