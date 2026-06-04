package com.suvojeet.notenext.ui.drawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
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

@HiltViewModel
class DrawingViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(DrawingState())
    val state: StateFlow<DrawingState> = _state.asStateFlow()

    fun onEvent(event: DrawingEvent) {
        when (event) {
            is DrawingEvent.PathAdded -> {
                _state.update { currentState ->
                    val newPath = DrawingPath(
                        path = event.path,
                        color = if (currentState.isEraserMode) Color.Transparent else currentState.currentColor,
                        strokeWidth = currentState.currentStrokeWidth,
                        isEraser = currentState.isEraserMode
                    )
                    currentState.copy(
                        paths = currentState.paths + newPath,
                        undonePaths = emptyList() // Clear redo stack on new action
                    )
                }
            }
            DrawingEvent.Undo -> {
                _state.update { currentState ->
                    if (currentState.paths.isNotEmpty()) {
                        val lastPath = currentState.paths.last()
                        currentState.copy(
                            paths = currentState.paths.dropLast(1),
                            undonePaths = currentState.undonePaths + lastPath
                        )
                    } else currentState
                }
            }
            DrawingEvent.Redo -> {
                _state.update { currentState ->
                    if (currentState.undonePaths.isNotEmpty()) {
                        val pathToRestore = currentState.undonePaths.last()
                        currentState.copy(
                            paths = currentState.paths + pathToRestore,
                            undonePaths = currentState.undonePaths.dropLast(1)
                        )
                    } else currentState
                }
            }
            DrawingEvent.ClearAll -> {
                _state.update { it.copy(paths = emptyList(), undonePaths = emptyList()) }
            }
            is DrawingEvent.ChangeColor -> {
                _state.update { it.copy(currentColor = event.color, isEraserMode = false) }
            }
            is DrawingEvent.ChangeStrokeWidth -> {
                _state.update { it.copy(currentStrokeWidth = event.width) }
            }
            is DrawingEvent.ToggleEraserMode -> {
                _state.update { it.copy(isEraserMode = event.isEraser) }
            }
            DrawingEvent.ToggleBrushSettings -> {
                _state.update { it.copy(showBrushSettings = !it.showBrushSettings) }
            }
            is DrawingEvent.SaveDrawing -> {
                saveDrawing(event.context, event.onSaveComplete)
            }
            DrawingEvent.ErrorShown -> {
                _state.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun saveDrawing(context: Context, onSaveComplete: (Uri?) -> Unit) {
        val currentPaths = _state.value.paths
        if (currentPaths.isEmpty()) {
            onSaveComplete(null)
            return
        }

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Smart Cropping: Calculate bounding box of all paths
                val bounds = RectF()
                val pathBounds = RectF()
                var isFirst = true

                currentPaths.forEach { drawingPath ->
                    val androidPath = drawingPath.path.asAndroidPath()
                    androidPath.computeBounds(pathBounds, true)
                    
                    // Expand bounds to account for stroke width
                    val padding = drawingPath.strokeWidth
                    pathBounds.set(
                        pathBounds.left - padding,
                        pathBounds.top - padding,
                        pathBounds.right + padding,
                        pathBounds.bottom + padding
                    )

                    if (isFirst) {
                        bounds.set(pathBounds)
                        isFirst = false
                    } else {
                        bounds.union(pathBounds)
                    }
                }

                // Ensure minimum dimensions and add slight padding
                val minSize = 100f
                val padding = 40f
                bounds.set(
                    bounds.left - padding,
                    bounds.top - padding,
                    bounds.right + padding,
                    bounds.bottom + padding
                )

                // Clamp to a sane range. A pathological path can produce a huge or
                // non-finite width/height; toInt() on Float.MAX_VALUE overflows to a
                // negative Int and createBitmap() then throws. Cap at 4096px per side.
                val maxSize = 4096f
                val rawWidth = bounds.width().let { if (it.isFinite()) it else minSize }
                val rawHeight = bounds.height().let { if (it.isFinite()) it else minSize }
                val width = rawWidth.coerceIn(minSize, maxSize).toInt()
                val height = rawHeight.coerceIn(minSize, maxSize).toInt()

                // Create bitmap just large enough for the drawing
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                
                // Match the white background of the drawing screen for better visibility
                canvas.drawColor(android.graphics.Color.WHITE)

                // Translate canvas so the drawing starts at 0,0
                canvas.translate(-bounds.left, -bounds.top)

                val paint = android.graphics.Paint().apply {
                    style = android.graphics.Paint.Style.STROKE
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    isAntiAlias = true
                }

                currentPaths.forEach { drawingPath ->
                    if (drawingPath.isEraser) {
                        // On a white background, eraser should draw with white color
                        paint.xfermode = null
                        paint.color = android.graphics.Color.WHITE
                    } else {
                        paint.xfermode = null
                        paint.color = drawingPath.color.toArgb()
                    }
                    paint.strokeWidth = drawingPath.strokeWidth
                    val androidPath = drawingPath.path.asAndroidPath()
                    canvas.drawPath(androidPath, paint)
                }

                // Must live under the cache-path mapped in file_paths.xml ("drawings/")
                // so FileProvider can grant a content:// URI for it.
                val drawingsDir = File(context.cacheDir, "drawings").apply { mkdirs() }
                val file = File(drawingsDir, "drawing_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                // Share via FileProvider (content:// + temporary read grant) rather than
                // a raw file:// URI, which throws FileUriExposedException on Android 7+.
                val sharedUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isSaving = false, errorMessage = null) }
                    onSaveComplete(sharedUri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Surface the failure instead of losing the drawing silently.
                    _state.update { it.copy(isSaving = false, errorMessage = "Couldn't save drawing. Please try again.") }
                    onSaveComplete(null)
                }
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isSaving = false, errorMessage = "Drawing is too large to save.") }
                    onSaveComplete(null)
                }
            }
        }
    }
}
