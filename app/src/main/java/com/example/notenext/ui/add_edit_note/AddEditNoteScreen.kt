
package com.example.notenext.ui.add_edit_note

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notenext.dependency_injection.ViewModelFactory
import com.example.notenext.ui.settings.ThemeMode
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditNoteScreen(
    factory: ViewModelFactory,
    onNoteSaved: () -> Unit,
    themeMode: ThemeMode
) {
    val viewModel: AddEditNoteViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val isKeyboardOpen = WindowInsets.isImeVisible

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is AddEditNoteUiEvent.OnNoteSaved -> onNoteSaved()
            }
        }
    }

    BackHandler {
        viewModel.onEvent(AddEditNoteEvent.OnSaveNoteClick)
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
                title = { Text(if (state.isNewNote) "Add Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.onEvent(AddEditNoteEvent.OnSaveNoteClick)
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(state.color),
                    titleContentColor = contentColorFor(backgroundColor = Color(state.color)),
                ),
                actions = {
                    if (!state.isNewNote) {
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(padding)
                    .background(Color(state.color))
            ) {
                TextField(
                    value = state.title,
                    onValueChange = { viewModel.onEvent(AddEditNoteEvent.OnTitleChange(it)) },
                    placeholder = { Text("Title", color = contentColorFor(backgroundColor = Color(state.color))) },
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = contentColorFor(backgroundColor = Color(state.color)),
                        selectionColors = TextSelectionColors(
                            handleColor = contentColorFor(backgroundColor = Color(state.color)),
                            backgroundColor = contentColorFor(backgroundColor = Color(state.color)).copy(alpha = 0.4f)
                        )
                    ),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(color = contentColorFor(backgroundColor = Color(state.color)))
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = state.content,
                    onValueChange = { viewModel.onEvent(AddEditNoteEvent.OnContentChange(it)) },
                    placeholder = { Text("Note", color = contentColorFor(backgroundColor = Color(state.color))) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = contentColorFor(backgroundColor = Color(state.color)),
                        selectionColors = TextSelectionColors(
                            handleColor = contentColorFor(backgroundColor = Color(state.color)),
                            backgroundColor = contentColorFor(backgroundColor = Color(state.color)).copy(alpha = 0.4f)
                        )
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColorFor(backgroundColor = Color(state.color)))
                )
            }

            AnimatedVisibility(
                visible = showColorPicker,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
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
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = contentColorFor(backgroundColor = Color(state.color)),
                                    shape = CircleShape
                                )
                                .clickable { viewModel.onEvent(AddEditNoteEvent.OnColorChange(color)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.color == color) {
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
                containerColor = Color(state.color)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showColorPicker = !showColorPicker }) {
                        Icon(Icons.Default.Palette, contentDescription = "Toggle color picker", tint = contentColorFor(backgroundColor = Color(state.color)))
                    }
                    if (!state.isNewNote) {
                        Text(
                            text = "Last edited: ${dateFormat.format(Date(state.lastEdited))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColorFor(backgroundColor = Color(state.color))
                        )
                    }
                    Row {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Undo Button
                            FloatingActionButton(
                                onClick = { viewModel.onEvent(AddEditNoteEvent.OnUndoClick) },
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp),
                                containerColor = Color(0xFFb8728f),
                                contentColor = if (state.historyIndex > 0) Color.White else Color.Gray
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Undo,
                                    contentDescription = "Undo"
                                )
                            }

                            // Redo Button
                            FloatingActionButton(
                                onClick = { viewModel.onEvent(AddEditNoteEvent.OnRedoClick) },
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp),
                                containerColor = Color(0xFFb8728f),
                                contentColor = if (state.historyIndex < state.history.size - 1) Color.White else Color.Gray
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
                        viewModel.onEvent(AddEditNoteEvent.OnDeleteNoteClick)
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
