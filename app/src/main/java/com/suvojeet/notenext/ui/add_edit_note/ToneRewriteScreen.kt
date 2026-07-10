package com.suvojeet.notenext.ui.add_edit_note

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.ai.ToneOption
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.notes.NotesEditState
import com.suvojeet.notenext.ui.notes.NotesEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToneRewriteScreen(
    state: NotesEditState,
    onEvent: (NotesEvent) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.ai_tone_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.springPress()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    if (state.toneRewriteResult != null && !state.isToneRewriting) {
                        IconButton(
                            onClick = {
                                onEvent(NotesEvent.AcceptToneRewrite)
                                onBack()
                            },
                            modifier = Modifier.springPress()
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = stringResource(id = R.string.ai_tone_apply_cd))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AutoFixHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (state.toneRewriteSelectedTone != null)
                            stringResource(id = R.string.ai_tone_rewriting_in, state.toneRewriteSelectedTone.displayName.lowercase())
                            else stringResource(id = R.string.ai_tone_pick_below),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToneOption.values().forEach { tone ->
                    val isSelected = tone == state.toneRewriteSelectedTone
                    FilterChip(
                        selected = isSelected,
                        onClick = { onEvent(NotesEvent.PickToneRewrite(tone)) },
                        label = {
                            Text("${tone.emoji} ${tone.displayName}")
                        },
                        modifier = Modifier.springPress()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(id = R.string.ai_tone_preview),
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.primary, 
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    when {
                        state.isToneRewriting -> Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(id = R.string.ai_tone_in_progress), style = MaterialTheme.typography.bodyMedium)
                        }
                        state.toneRewriteError != null -> Text(
                            state.toneRewriteError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        state.toneRewriteResult != null -> Text(
                            state.toneRewriteResult,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        else -> Text(
                            state.editingContent.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (state.toneRewriteResult != null && !state.isToneRewriting) {
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        onEvent(NotesEvent.AcceptToneRewrite)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).springPress(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.ai_tone_apply_changes))
                }
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = { 
                        state.toneRewriteSelectedTone?.let { onEvent(NotesEvent.PickToneRewrite(it)) }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).springPress(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.ai_tone_try_again))
                }
            }
        }
    }
}
