
package com.example.notenext.ui.add_edit_note

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Archive


import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.selection.TextSelectionColors
import com.example.notenext.ui.notes.NotesEvent
import com.example.notenext.ui.notes.NotesState
import com.example.notenext.ui.settings.ThemeMode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onDismiss: () -> Unit,
    themeMode: ThemeMode
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    BackHandler {
        onDismiss()
    }

    val lightNoteColors = listOf(
        Color.White.toArgb(),
        Color(0xFFF28B82).toArgb(), // Red
        Color(0xFFFCBC05).toArgb(), // Orange
        Color(0xFFFFF475).toArgb(), // Yellow
        Color(0xFFCCFF90).toArgb(), // Green
        Color(0xFFA7FFEB).toArgb(), // Teal
        Color(0xFFCBF0F8).toArgb(), // Blue
        Color(0xFFAFCBFA).toArgb(), // Dark Blue
        Color(0xFFD7AEFB).toArgb(), // Purple
        Color(0xFFFDCFE8).toArgb(), // Pink
        Color(0xFFE6C9A8).toArgb(), // Brown
        Color(0xFFE8EAED).toArgb()  // Gray
    )

    val darkNoteColors = listOf(
        Color(0xFF424242).toArgb(), // Dark Gray (default for dark mode)
        Color(0xFFB71C1C).toArgb(), // Dark Red
        Color(0xFFE65100).toArgb(), // Dark Orange
        Color(0xFFF57F17).toArgb(), // Dark Yellow
        Color(0xFF2E7D32).toArgb(), // Dark Green
        Color(0xFF006064).toArgb(), // Dark Teal
        Color(0xFF01579B).toArgb(), // Dark Blue
        Color(0xFF1A237E).toArgb(), // Very Dark Blue
        Color(0xFF4A148C).toArgb(), // Dark Purple
        Color(0xFF880E4F).toArgb(), // Dark Pink
        Color(0xFF3E2723).toArgb(), // Dark Brown
        Color(0xFF212121).toArgb()  // Very Dark Gray
    )

    val colors = when (themeMode) {
        ThemeMode.DARK -> darkNoteColors
        else -> lightNoteColors
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editingIsNewNote) "Add Note" else "") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(state.editingColor),
                    titleContentColor = contentColorFor(backgroundColor = Color(state.editingColor)),
                ),
                actions = {
                    if (!state.editingIsNewNote) {
                        IconButton(onClick = { onEvent(NotesEvent.OnTogglePinClick) }) {
                            Icon(
                                imageVector = if (state.isPinned) Icons.Filled.PushPin else Icons.Filled.PushPin,
                                contentDescription = if (state.isPinned) "Unpin note" else "Pin note"
                            )
                        }
                        IconButton(onClick = { onEvent(NotesEvent.OnToggleArchiveClick) }) {
                            Icon(
                                imageVector = if (state.isArchived) Icons.Filled.Archive else Icons.Filled.Archive,
                                contentDescription = if (state.isArchived) "Unarchive note" else "Archive note"
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete note")
                        }
                    }
                }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(padding)
                    .background(Color(state.editingColor))
            ) {
                item {
                    TextField(
                        value = state.editingTitle,
                        onValueChange = { onEvent(NotesEvent.OnTitleChange(it)) },
                        placeholder = { Text("Title", color = contentColorFor(backgroundColor = Color(state.editingColor))) },
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = contentColorFor(backgroundColor = Color(state.editingColor)),
                            selectionColors = TextSelectionColors(
                                handleColor = contentColorFor(backgroundColor = Color(state.editingColor)),
                                backgroundColor = contentColorFor(backgroundColor = Color(state.editingColor)).copy(alpha = 0.4f)
                            )
                        ),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(color = contentColorFor(backgroundColor = Color(state.editingColor)))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = state.editingContent,
                        onValueChange = { onEvent(NotesEvent.OnContentChange(it)) },
                        placeholder = { Text("Note", color = contentColorFor(backgroundColor = Color(state.editingColor))) },
                                            modifier = Modifier
                                                .fillMaxWidth(),                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = contentColorFor(backgroundColor = Color(state.editingColor)),
                            selectionColors = TextSelectionColors(
                                handleColor = contentColorFor(backgroundColor = Color(state.editingColor)),
                                backgroundColor = contentColorFor(backgroundColor = Color(state.editingColor)).copy(alpha = 0.4f)
                            )
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColorFor(backgroundColor = Color(state.editingColor)))
                    )
                    if (!state.editingIsNewNote) {
                        
                    }
                }
            }

            AnimatedVisibility(
                visible = showColorPicker,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(colors) { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(color), CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (state.editingColor == color) contentColorFor(backgroundColor = Color(color)) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onEvent(NotesEvent.OnColorChange(color)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.editingColor == color) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = contentColorFor(backgroundColor = Color(color))
                                )
                            }
                        }
                    }
                }
            }

            BottomAppBar(
                containerColor = Color(state.editingColor)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = { showColorPicker = !showColorPicker },
                        shape = CircleShape,
                        modifier = Modifier.size(45.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = "Toggle color picker")
                    }
                    if (state.editingHistory.size > 1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Undo Button
                            FloatingActionButton(
                                onClick = { onEvent(NotesEvent.OnUndoClick) },
                                shape = CircleShape,
                        modifier = Modifier.size(45.dp),
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                contentColor = if (state.editingHistoryIndex > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Undo,
                                    contentDescription = "Undo"
                                )
                            }

                            // Redo Button
                            FloatingActionButton(
                                onClick = { onEvent(NotesEvent.OnRedoClick) },
                                shape = CircleShape,
                                modifier = Modifier.size(45.dp),
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                contentColor = if (state.editingHistoryIndex < state.editingHistory.size - 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Redo,
                                    contentDescription = "Redo"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(NotesEvent.OnDeleteNoteClick)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
