package com.suvojeet.notenext.ui.add_edit_note.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

import androidx.compose.material.icons.filled.Delete
import com.suvojeet.notenext.ui.notes.NotesEvent

/**
 * A full-screen image viewer that supports zoom, pan, and rotation gestures.
 * It also includes a top app bar with options to go back and delete the image,
 * which can be toggled by tapping the screen.
 *
 * @param imageUri The [Uri] of the image to display.
 * @param attachmentTempId The temporary ID of the attachment, used for deletion.
 * @param onDismiss Lambda to be invoked when the viewer is dismissed (e.g., back button clicked).
 * @param onEvent Lambda to dispatch [NotesEvent]s, specifically for [NotesEvent.RemoveAttachment].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    imageUri: Uri,
    attachmentTempId: String,
    onDismiss: () -> Unit,
    onEvent: (NotesEvent) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableStateOf(0f) }
    var showTopAppBar by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Toggle TopAppBar visibility on single tap.
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showTopAppBar = !showTopAppBar
                    }
                )
            }
    ) {
        // Display the image with zoom, pan, and rotate gestures.
        AsyncImage(
            model = imageUri,
            contentDescription = stringResource(id = R.string.image_content_description), // Accessibility for the displayed image.
            modifier = Modifier
                .fillMaxSize()
                // Handle multi-touch gestures for zoom, pan, and rotate.
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, rotate ->
                        scale = (scale * zoom).coerceIn(1f, 5f) // Limit zoom between 1x and 5x.
                        rotation += rotate
                        offset = if (scale > 1f) {
                            // Apply pan only when zoomed in.
                            val newOffset = offset + pan * scale
                            val maxX = (size.width * (scale - 1f)) / 2f
                            val maxY = (size.height * (scale - 1f)) / 2f
                            Offset(
                                newOffset.x.coerceIn(-maxX, maxX),
                                newOffset.y.coerceIn(-maxY, maxY)
                            )
                        } else {
                            Offset.Zero // Reset offset when not zoomed.
                        }
                    }
                }
                // Apply transformations using graphicsLayer for performance.
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                    rotationZ = rotation
                ),
            contentScale = ContentScale.Fit // Fit the image within the bounds.
        )
        // Animated visibility for the TopAppBar.
        AnimatedVisibility(visible = showTopAppBar) {
            TopAppBar(
                title = { Text(stringResource(id = R.string.image_viewer)) },
                navigationIcon = {
                    // Back button.
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    // Delete image action.
                    IconButton(onClick = {
                        onEvent(NotesEvent.RemoveAttachment(attachmentTempId))
                        onDismiss() // Dismiss viewer after deletion.
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.remove_image), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f), // Semi-transparent black background.
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    }
}