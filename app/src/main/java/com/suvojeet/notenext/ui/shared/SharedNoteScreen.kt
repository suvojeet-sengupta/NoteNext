package com.suvojeet.notenext.ui.shared

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SharedNoteScreen(
    shareId: String,
    key: String?,
    onBack: () -> Unit,
    viewModel: SharedNoteViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(shareId, key) { viewModel.start(shareId, key) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SharedNoteEvent.Toast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is SharedNoteEvent.SavedCopy -> { /* handled by snackbar/toast */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shared note", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    EncryptedChip()
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        floatingActionButton = {
            if (!state.loading && state.error == null) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveCopy() },
                    icon = {
                        Icon(
                            if (state.savedLocally) Icons.Default.CheckCircle else Icons.Default.BookmarkAdd,
                            contentDescription = null
                        )
                    },
                    text = { Text(if (state.savedLocally) "Saved" else "Save a copy") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    ErrorState(message = state.error!!, showRetry = !state.gone, onRetry = { viewModel.retry() })
                }
                else -> {
                    SharedNoteContent(state = state)
                }
            }
        }
    }
}

@Composable
private fun SharedNoteContent(state: SharedNoteUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Attribution / metadata card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Shared via NoteNext",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "by ${state.sharedBy}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.expiresAt != null) {
                    Spacer(Modifier.height(10.dp))
                    MetaRow(
                        icon = Icons.Default.Schedule,
                        text = "Expires ${prettyDate(state.expiresAt)}"
                    )
                }
                if (state.burnAfterRead) {
                    Spacer(Modifier.height(6.dp))
                    MetaRow(
                        icon = Icons.Default.LocalFireDepartment,
                        text = "Deleted after this read",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(10.dp))
                MetaRow(
                    icon = Icons.Default.Lock,
                    text = "End-to-end encrypted · only people with the link can read it"
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SelectionContainer {
            Column {
                Text(
                    text = state.title.ifBlank { "Untitled note" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.content.ifBlank { "This note is empty." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (state.content.isBlank())
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(96.dp)) // breathing room above the FAB
    }
}

@Composable
private fun MetaRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
private fun EncryptedChip() {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text("Encrypted", style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = MaterialTheme.colorScheme.primary,
            disabledLeadingIconContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun BoxScope.ErrorState(message: String, showRetry: Boolean, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        SelectionContainer {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showRetry) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

/**
 * Best-effort prettifier for an ISO-8601 timestamp (e.g. "2026-06-29T10:15:30.123Z").
 * Uses plain string ops to stay safe on minSdk 24 (no java.time / desugaring needed).
 */
private fun prettyDate(iso: String): String = try {
    if (iso.length >= 16 && iso[10] == 'T') {
        iso.substring(0, 10) + " " + iso.substring(11, 16)
    } else {
        iso
    }
} catch (e: Exception) {
    iso
}
