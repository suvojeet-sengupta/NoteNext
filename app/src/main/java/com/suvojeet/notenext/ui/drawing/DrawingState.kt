package com.suvojeet.notenext.ui.drawing

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * A single digitiser sample, stored in page (document) coordinates so that the
 * artwork survives pan/zoom untouched — the viewport transform is applied only
 * at draw time.
 */
@Immutable
data class StrokePoint(
    val x: Float,
    val y: Float,
    /** Normalised stylus pressure, 0..1. Fingers and pressure-less digitisers report 1f. */
    val pressure: Float = 1f
) {
    val offset: Offset get() = Offset(x, y)
}

/**
 * The tools on the rail. Pen and pencil are pressure-driven, the marker keeps a
 * constant nib, the highlighter multiplies over what is underneath, and the
 * shape tools rubber-band between the first and the last sample.
 */
enum class DrawingTool {
    PEN,
    PENCIL,
    MARKER,
    HIGHLIGHTER,
    ERASER,
    LINE,
    ARROW,
    RECTANGLE,
    OVAL;

    val isShape: Boolean
        get() = this == LINE || this == ARROW || this == RECTANGLE || this == OVAL

    val isEraser: Boolean get() = this == ERASER

    /** Tools whose nib width follows stylus pressure. */
    val supportsPressure: Boolean get() = this == PEN || this == PENCIL

    /** Tools that keep their own remembered width/opacity. */
    val isHighlighter: Boolean get() = this == HIGHLIGHTER
}

/** Pixel erasing rubs out what the nib touches; stroke erasing lifts a whole stroke. */
enum class EraserMode { PIXEL, STROKE }

/** Ruling printed under the artwork. Never erasable, never exported unless the page is. */
enum class PaperStyle { PLAIN, RULED, GRID, DOTS }

/** Page tint presets, each carrying the ruling colour that stays legible on it. */
enum class PaperTint(val paper: Color, val line: Color) {
    WHITE(Color(0xFFFFFFFF), Color(0xFFD6DCE5)),
    CREAM(Color(0xFFFBF3E4), Color(0xFFE2D6BC)),
    SLATE(Color(0xFF2A2F36), Color(0xFF3C444E)),
    BLACK(Color(0xFF101114), Color(0xFF262A31));

    /** Ink that reads well on this page when the user has not picked a colour yet. */
    val defaultInk: Color
        get() = if (this == SLATE || this == BLACK) Color(0xFFF2F3F7) else Color(0xFF15161A)

    val isDark: Boolean get() = this == SLATE || this == BLACK
}

/** A committed stroke. Immutable, so undo/redo can share instances between snapshots. */
@Immutable
data class DrawingStroke(
    val id: Long,
    val tool: DrawingTool,
    val color: Color,
    val width: Float,
    val alpha: Float,
    val points: List<StrokePoint>
)

@Immutable
data class DrawingState(
    val strokes: List<DrawingStroke> = emptyList(),
    val undoStack: List<List<DrawingStroke>> = emptyList(),
    val redoStack: List<List<DrawingStroke>> = emptyList(),

    // ── Active tool ────────────────────────────────────────────────
    val tool: DrawingTool = DrawingTool.PEN,
    /** Tool to fall back to when a stylus' eraser tip lifts off. */
    val toolBeforeEraser: DrawingTool = DrawingTool.PEN,
    val color: Color = Color(0xFF15161A),
    val recentColors: List<Color> = emptyList(),

    // ── Per-tool nib memory ────────────────────────────────────────
    val brushWidth: Float = 6f,
    val highlighterWidth: Float = 36f,
    val eraserWidth: Float = 40f,
    val opacity: Float = 1f,
    val highlighterAlpha: Float = 0.35f,

    // ── Stylus behaviour ───────────────────────────────────────────
    val pressureSensitivity: Float = 0.7f,
    val smoothing: Boolean = true,
    /** Palm rejection: when on, only a stylus draws and fingers navigate. */
    val stylusOnly: Boolean = false,
    /** Set the first time a stylus is seen, so palm rejection can arm itself once. */
    val hasSeenStylus: Boolean = false,
    val eraserMode: EraserMode = EraserMode.PIXEL,

    // ── Page ───────────────────────────────────────────────────────
    val paperStyle: PaperStyle = PaperStyle.PLAIN,
    val paperTint: PaperTint = PaperTint.WHITE,

    // ── Viewport ───────────────────────────────────────────────────
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero,
    /** Size of the drawing surface in px, kept so fit/reset and export know the page. */
    val viewport: Size = Size.Zero,

    // ── Chrome / export ────────────────────────────────────────────
    val showPropertiesPanel: Boolean = true,
    val showBrushSettings: Boolean = false,
    val isImmersive: Boolean = false,
    val trimToContent: Boolean = true,
    val transparentBackground: Boolean = false,
    val isSaving: Boolean = false
) {
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val isEmpty: Boolean get() = strokes.isEmpty()

    /** Nib width for the tool in hand, in page units. */
    val activeWidth: Float
        get() = when {
            tool.isEraser -> eraserWidth
            tool.isHighlighter -> highlighterWidth
            else -> brushWidth
        }

    /** Opacity for the tool in hand. */
    val activeAlpha: Float
        get() = if (tool.isHighlighter) highlighterAlpha else opacity

    val zoomPercent: Int get() = (scale * 100f).toInt()
}

/** Nib presets offered as tap targets next to the fine slider, in page units. */
val BrushWidthPresets: List<Float> = listOf(2f, 4f, 8f, 16f, 32f)
val EraserWidthPresets: List<Float> = listOf(16f, 32f, 64f, 120f)

/** The default ink wells. Row-major so both the phone row and the tablet grid read well. */
val DrawingPalette: List<Color> = listOf(
    Color(0xFF15161A), Color(0xFF5F6368), Color(0xFF9AA0A6), Color(0xFFFFFFFF),
    Color(0xFFD32F2F), Color(0xFFE64A19), Color(0xFFF57C00), Color(0xFFFBC02D),
    Color(0xFF388E3C), Color(0xFF00897B), Color(0xFF0288D1), Color(0xFF1976D2),
    Color(0xFF3F51B5), Color(0xFF7B1FA2), Color(0xFFC2185B), Color(0xFF5D4037)
)

const val MIN_CANVAS_SCALE = 0.25f
const val MAX_CANVAS_SCALE = 8f
