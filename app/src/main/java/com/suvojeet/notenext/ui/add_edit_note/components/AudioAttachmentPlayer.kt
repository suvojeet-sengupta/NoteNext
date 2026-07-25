@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.suvojeet.notenext.ui.add_edit_note.components

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress
import kotlinx.coroutines.delay

/**
 * Inline player for a voice note. Owns one MediaPlayer for the lifetime of the
 * row and releases it on dispose, so scrolling a note with several recordings
 * never leaks decoders.
 */
@Composable
fun AudioAttachmentPlayer(
    uri: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableIntStateOf(0) }
    var positionMs by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }

    // Prepared once up front so the duration is known before the first play.
    DisposableEffect(uri) {
        val created = runCatching {
            MediaPlayer().apply {
                setDataSource(context, Uri.parse(uri))
                prepare()
                setOnCompletionListener {
                    seekTo(0)
                }
            }
        }.getOrNull()

        if (created == null) failed = true
        player = created
        durationMs = created?.duration ?: 0

        onDispose {
            runCatching { created?.release() }
            player = null
        }
    }

    // Drive the progress bar only while actually playing.
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val active = player
            if (active == null || !active.isPlaying) {
                isPlaying = false
                positionMs = 0
                break
            }
            positionMs = active.currentPosition
            delay(200)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (failed) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    IconButton(
                        onClick = {
                            val active = player ?: return@IconButton
                            if (active.isPlaying) {
                                active.pause()
                                isPlaying = false
                            } else {
                                active.start()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier.springPress()
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(
                                if (isPlaying) R.string.audio_pause else R.string.audio_play
                            ),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.audio_recording),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (failed) {
                    Text(
                        stringResource(R.string.audio_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${formatDuration(positionMs.toLong())} / ${formatDuration(durationMs.toLong())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onRemove, modifier = Modifier.springPress()) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.audio_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
