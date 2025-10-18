
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.PushPin


import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.TextFields

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import android.content.Intent

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
    var showFormatBar by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val context = LocalContext.current

    BackHandler {
        onDismiss()
    }

    val darkNoteColors = listOf(
        Color(0xFF212121).toArgb(), // Very Dark Gray
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
        Color(0xFF424242).toArgb()  // Dark Gray
    )

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

    val systemInDarkTheme = isSystemInDarkTheme()
    val colors = when (themeMode) {
        ThemeMode.DARK -> darkNoteColors
        ThemeMode.SYSTEM -> if (systemInDarkTheme) darkNoteColors else lightNoteColors
        else -> lightNoteColors
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editingIsNewNote) "Add Note" else "") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = if (state.isPinned) "Unpin note" else "Pin note",
                                tint = if (state.isPinned) MaterialTheme.colorScheme.primary else contentColorFor(backgroundColor = Color(state.editingColor))
                            )
                        }
                        IconButton(onClick = { onEvent(NotesEvent.OnToggleArchiveClick) }) {
                            Icon(
                                imageVector = Icons.Filled.Archive,
                                contentDescription = if (state.isArchived) "Unarchive note" else "Archive note",
                                tint = if (state.isArchived) MaterialTheme.colorScheme.primary else contentColorFor(backgroundColor = Color(state.editingColor))
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
                    if (!state.editingIsNewNote && !state.editingLabel.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = state.editingLabel,
                                    color = contentColorFor(backgroundColor = Color(state.editingColor)),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showFormatBar,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                        }
                    }
                    item {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                        }
                    }
                    item {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline")
                        }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FloatingActionButton(
                            onClick = { showColorPicker = !showColorPicker },
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = "Toggle color picker")
                        }
                        FloatingActionButton(
                            onClick = { showFormatBar = !showFormatBar },
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(Icons.Default.TextFields, contentDescription = "Toggle format bar")
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.editingHistory.size > 1) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Undo Button
                                FloatingActionButton(
                                    onClick = { onEvent(NotesEvent.OnUndoClick) },
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                    contentColor = if (state.editingHistoryIndex > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Undo,
                                        contentDescription = "Undo"
                                    )
                                }

                                // Redo Button
                                FloatingActionButton(
                                    onClick = { onEvent(NotesEvent.OnRedoClick) },
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                    contentColor = if (state.editingHistoryIndex < state.editingHistory.size - 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Redo,
                                        contentDescription = "Redo"
                                    )
                                }
                            }
                        }

                        // 3-dot button
                        Box {
                            FloatingActionButton(
                                onClick = { showMoreOptions = true },
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp),
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options"
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreOptions,
                                onDismissRequest = { showMoreOptions = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        showDeleteDialog = true
                                        showMoreOptions = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Make a copy") },
                                    onClick = { onEvent(NotesEvent.OnCopyCurrentNoteClick); showMoreOptions = false },
                                    leadingIcon = {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Make a copy")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    onClick = {
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "${state.editingTitle}\n\n${state.editingContent}")
                                            putExtra(Intent.EXTRA_SUBJECT, state.editingTitle)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                        showMoreOptions = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = "Share")
                                    }
                                                                 )
                                                                DropdownMenuItem(
                                                                    text = { Text("Labels") },
                                                                    onClick = { showMoreOptions = false; onEvent(NotesEvent.OnAddLabelsToCurrentNoteClick) },
                                                                    leadingIcon = {
                                                                        Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Labels")
                                                                    }
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
                                
                                if (state.showLabelDialog) {
                                    LabelDialog(
                                        labels = state.labels,
                                        onDismiss = { onEvent(NotesEvent.DismissLabelDialog) },
                                        onConfirm = { label ->
                                            onEvent(NotesEvent.OnLabelChange(label))
                                            onEvent(NotesEvent.DismissLabelDialog)
                                        }
                                    )
                                }                                }
                                
                                @Composable
                                fun LabelDialog(
                                    labels: List<String>,
                                    onDismiss: () -> Unit,
                                    onConfirm: (String) -> Unit
                                ) {
                                    var newLabel by remember { mutableStateOf("") }
                                
                                    AlertDialog(
                                        onDismissRequest = onDismiss,
                                        title = { Text(text = "Add Label") },
                                        text = {
                                            Column {
                                                OutlinedTextField(
                                                    value = newLabel,
                                                    onValueChange = { newLabel = it },
                                                    label = { Text("New Label") }
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                LazyColumn {
                                                    items(labels) { label ->
                                                        Text(
                                                            text = label,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { onConfirm(label) }
                                                                .padding(8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    if (newLabel.isNotBlank()) {
                                                        onConfirm(newLabel)
                                                    }
                                                    onDismiss()
                                                }
                                            ) {
                                                Text("OK")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = onDismiss) {
                                                Text("Cancel")
                                            }
                                        }
                                    )
                                }
