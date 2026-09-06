package com.suvojeet.notenext.ui.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Geometry + painting shared by the live canvas and the PNG exporter, so what
 * lands in the note is pixel-for-pixel what the user drew.
 */

/** Base spacing of the printed ruling, in page units. */
private const val RULED_SPACING = 46f
private const val GRID_SPACING = 40f
private const val DOT_SPACING = 32f

/** Chaikin-style quadratic smoothing through the sample midpoints. */
fun List<StrokePoint>.toSmoothPath(): Path {
    val path = Path()
    if (isEmpty()) return path
    val first = this[0]
    path.moveTo(first.x, first.y)
    if (size == 1) {
        path.lineTo(first.x + 0.01f, first.y)
        return path
    }
    for (i in 1 until size - 1) {
        val current = this[i]
        val next = this[i + 1]
        path.quadraticTo(current.x, current.y, (current.x + next.x) / 2f, (current.y + next.y) / 2f)
    }
    val last = this[size - 1]
    path.lineTo(last.x, last.y)
    return path
}

fun List<StrokePoint>.toPolylinePath(): Path {
    val path = Path()
    if (isEmpty()) return path
    path.moveTo(this[0].x, this[0].y)
    for (i in 1 until size) path.lineTo(this[i].x, this[i].y)
    if (size == 1) path.lineTo(this[0].x + 0.01f, this[0].y)
    return path
}

/**
 * Width multiplier for a sample: at sensitivity 0 the nib is constant, at 1 it
 * follows the digitiser one-for-one. Never collapses to nothing.
 */
fun pressureFactor(pressure: Float, sensitivity: Float): Float {
    val p = pressure.coerceIn(0f, 1f)
    val s = sensitivity.coerceIn(0f, 1f)
    return ((1f - s) + s * p).coerceAtLeast(0.12f)
}

private fun DrawingTool.strokeCap(): StrokeCap = when (this) {
    DrawingTool.MARKER, DrawingTool.HIGHLIGHTER -> StrokeCap.Square
    else -> StrokeCap.Round
}

private fun DrawingTool.blendMode(): BlendMode = when (this) {
    DrawingTool.ERASER -> BlendMode.Clear
    DrawingTool.HIGHLIGHTER -> BlendMode.Multiply
    else -> BlendMode.SrcOver
}

/** Paints one committed (or in-flight) stroke into the current draw scope. */
fun DrawScope.drawInkStroke(
    tool: DrawingTool,
    color: Color,
    width: Float,
    alpha: Float,
    points: List<StrokePoint>,
    pressureSensitivity: Float,
    smoothing: Boolean
) {
    if (points.isEmpty()) return
    val blend = tool.blendMode()
    val cap = tool.strokeCap()
    val ink = if (tool.isEraser) Color.Black else color

    if (tool.isShape) {
        drawShapeStroke(tool, ink, width, alpha, points, blend, cap)
        return
    }

    // A tap with no travel still deserves a dot.
    if (points.size == 1) {
        val p = points[0]
        val r = max(0.5f, width * pressureFactor(p.pressure, if (tool.supportsPressure) pressureSensitivity else 0f) / 2f)
        drawCircle(color = ink, radius = r, center = p.offset, alpha = alpha, blendMode = blend)
        return
    }

    if (!tool.supportsPressure) {
        val path = if (smoothing) points.toSmoothPath() else points.toPolylinePath()
        drawPath(
            path = path,
            color = ink,
            alpha = alpha,
            style = Stroke(width = width, cap = cap, join = StrokeJoin.Round),
            blendMode = blend
        )
        return
    }

    // Pressure tools are stroked segment by segment so the nib can breathe.
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        val factor = pressureFactor((a.pressure + b.pressure) / 2f, pressureSensitivity)
        val segmentAlpha = if (tool == DrawingTool.PENCIL) {
            (alpha * (0.55f + 0.45f * b.pressure.coerceIn(0f, 1f))).coerceIn(0.05f, 1f)
        } else {
            alpha
        }
        drawLine(
            color = ink,
            start = a.offset,
            end = b.offset,
            strokeWidth = max(0.5f, width * factor),
            cap = StrokeCap.Round,
            alpha = segmentAlpha,
            blendMode = blend
        )
    }
}

private fun DrawScope.drawShapeStroke(
    tool: DrawingTool,
    color: Color,
    width: Float,
    alpha: Float,
    points: List<StrokePoint>,
    blend: BlendMode,
    cap: StrokeCap
) {
    val start = points.first().offset
    val end = points.last().offset
    val style = Stroke(width = width, cap = cap, join = StrokeJoin.Round)
    when (tool) {
        DrawingTool.LINE -> drawLine(color, start, end, width, cap, alpha = alpha, blendMode = blend)
        DrawingTool.ARROW -> {
            drawLine(color, start, end, width, cap, alpha = alpha, blendMode = blend)
            val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
            val head = max(width * 3.5f, hypot(end.x - start.x, end.y - start.y) * 0.18f)
            val spread = 0.42
            for (side in intArrayOf(-1, 1)) {
                val a = angle + Math.PI + spread * side
                drawLine(
                    color = color,
                    start = end,
                    end = Offset(end.x + (head * cos(a)).toFloat(), end.y + (head * sin(a)).toFloat()),
                    strokeWidth = width,
                    cap = cap,
                    alpha = alpha,
                    blendMode = blend
                )
            }
        }
        DrawingTool.RECTANGLE -> {
            val topLeft = Offset(min(start.x, end.x), min(start.y, end.y))
            val size = Size(abs(end.x - start.x), abs(end.y - start.y))
            drawRect(color, topLeft, size, alpha, style, blendMode = blend)
        }
        DrawingTool.OVAL -> {
            val topLeft = Offset(min(start.x, end.x), min(start.y, end.y))
            val size = Size(abs(end.x - start.x), abs(end.y - start.y))
            drawOval(color, topLeft, size, alpha, style, blendMode = blend)
        }
        else -> Unit
    }
}

/**
 * Prints the page ruling in screen space, so the lines stay crisp at any zoom
 * and never end up inside the erasable ink layer.
 */
fun DrawScope.drawPaperRuling(
    style: PaperStyle,
    tint: PaperTint,
    scale: Float,
    offset: Offset
) {
    if (style == PaperStyle.PLAIN) return
    val line = tint.line
    val hairline = max(0.75f, 1f * scale)

    fun firstTick(spacingPx: Float, translate: Float): Float {
        val phase = translate % spacingPx
        return if (phase > 0f) phase - spacingPx else phase
    }

    when (style) {
        PaperStyle.RULED -> {
            val spacing = RULED_SPACING * scale
            if (spacing < 6f) return
            var y = firstTick(spacing, offset.y)
            while (y <= size.height) {
                if (y >= 0f) drawLine(line, Offset(0f, y), Offset(size.width, y), hairline)
                y += spacing
            }
        }
        PaperStyle.GRID -> {
            val spacing = GRID_SPACING * scale
            if (spacing < 6f) return
            var y = firstTick(spacing, offset.y)
            while (y <= size.height) {
                if (y >= 0f) drawLine(line, Offset(0f, y), Offset(size.width, y), hairline)
                y += spacing
            }
            var x = firstTick(spacing, offset.x)
            while (x <= size.width) {
                if (x >= 0f) drawLine(line, Offset(x, 0f), Offset(x, size.height), hairline)
                x += spacing
            }
        }
        PaperStyle.DOTS -> {
            val spacing = DOT_SPACING * scale
            if (spacing < 10f) return
            val radius = max(1f, 1.6f * scale)
            var y = firstTick(spacing, offset.y)
            while (y <= size.height) {
                var x = firstTick(spacing, offset.x)
                while (x <= size.width) {
                    if (x >= 0f && y >= 0f) drawCircle(line, radius, Offset(x, y))
                    x += spacing
                }
                y += spacing
            }
        }
        PaperStyle.PLAIN -> Unit
    }
}

// ── Geometry used by trimming, fit-to-content and stroke erasing ───────────

/** Bounding box of a stroke including its nib, in page units. */
fun DrawingStroke.bounds(): Rect? {
    if (points.isEmpty()) return null
    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    points.forEach {
        left = min(left, it.x); top = min(top, it.y)
        right = max(right, it.x); bottom = max(bottom, it.y)
    }
    val pad = width / 2f + 1f
    return Rect(left - pad, top - pad, right + pad, bottom + pad)
}

fun List<DrawingStroke>.contentBounds(): Rect? {
    var result: Rect? = null
    forEach { stroke ->
        val b = stroke.bounds() ?: return@forEach
        result = result?.let {
            Rect(min(it.left, b.left), min(it.top, b.top), max(it.right, b.right), max(it.bottom, b.bottom))
        } ?: b
    }
    return result
}

/** True when the eraser nib at [center] overlaps any segment of this stroke. */
fun DrawingStroke.hitBy(center: Offset, radius: Float): Boolean {
    val reach = radius + width / 2f
    val box = bounds() ?: return false
    if (center.x < box.left - reach || center.x > box.right + reach ||
        center.y < box.top - reach || center.y > box.bottom + reach
    ) return false

    if (points.size == 1) return (points[0].offset - center).getDistance() <= reach
    for (i in 1 until points.size) {
        if (distanceToSegment(center, points[i - 1].offset, points[i].offset) <= reach) return true
    }
    // Shapes only store two anchors; approximate the outline by its box edges.
    if (tool == DrawingTool.RECTANGLE || tool == DrawingTool.OVAL) {
        val inflated = Rect(box.left - reach, box.top - reach, box.right + reach, box.bottom + reach)
        return inflated.contains(center)
    }
    return false
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val lengthSq = abx * abx + aby * aby
    if (lengthSq <= 0.0001f) return (p - a).getDistance()
    var t = ((p.x - a.x) * abx + (p.y - a.y) * aby) / lengthSq
    t = t.coerceIn(0f, 1f)
    return hypot(p.x - (a.x + t * abx), p.y - (a.y + t * aby))
}

/** Screen point -> page point under the current viewport. */
fun toPageSpace(screen: Offset, scale: Float, offset: Offset): Offset =
    Offset((screen.x - offset.x) / scale, (screen.y - offset.y) / scale)
