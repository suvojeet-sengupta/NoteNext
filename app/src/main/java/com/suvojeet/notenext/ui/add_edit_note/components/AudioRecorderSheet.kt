@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.suvojeet.notenext.ui.add_edit_note.components

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.util.AudioRecorder
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Voice note recorder. Starts recording as soon as it opens — the user already
 * expressed intent by picking "Audio", so making them press record again is a
 * wasted tap and loses the first second of speech.
 *
 * [onSaved] receives the finished clip; [onDismiss] means nothing was kept.
 */
@Composable
fun AudioRecorderSheet(
    onDismiss: () -> Unit,
    onSaved: (uri: Uri, mimeType: String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val recorder = remember { AudioRecorder(context) }

    var elapsedMs by remember { mutableLongStateOf(0L) }
    var amplitude by remember { mutableFloatStateOf(0f) }
    var failed by remember { mutableStateOf(false) }

    // Tick while recording. Sampling amplitude here doubles as the waveform source.
    LaunchedEffect(Unit) {
        if (!recorder.start()) {
            failed = true
            return@LaunchedEffect
        }
        while (true) {
            delay(100)
            elapsedMs += 100
            amplitude = recorder.currentAmplitude()
        }
    }

    // Whatever tears this composable down — back gesture, scrim tap, process
    // shutdown — must not leave the mic held open.
    DisposableEffect(Unit) {
        onDispose { recorder.cancel() }
    }

    val animatedAmplitude by animateFloatAsState(
        targetValue = amplitude,
        animationSpec = spring(),
        label = "amplitude"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.audio_recording),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(24.dp))

            if (failed) {
                Text(
                    stringResource(R.string.audio_record_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                    Text(stringResource(R.string.audio_close))
                }
            } else {
                // Mic disc that swells with input level, so the user can see the
                // recording is actually picking up sound.
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp + (48.dp * animatedAmplitude))
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                    )
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = formatDuration(elapsedMs),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )

                Spacer(Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            recorder.cancel()
                            onDismiss()
                        },
                        modifier = Modifier.springPress()
                    ) {
                        Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.audio_discard))
                    }

                    Button(
                        onClick = {
                            val uri = recorder.stop()
                            if (uri != null) {
                                onSaved(uri, "audio/mp4")
                            } else {
                                // Too short to contain audio — nothing worth attaching.
                                failed = true
                            }
                        },
                        modifier = Modifier.springPress()
                    ) {
                        Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.audio_save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

internal fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
