package com.suvojeet.notenext.ui.add_edit_note

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.add_edit_note.components.AddEditNoteBottomAppBar
import com.suvojeet.notenext.ui.add_edit_note.components.AddEditNoteTopAppBar
import com.suvojeet.notenext.ui.add_edit_note.components.ColorPicker
import com.suvojeet.notenext.ui.add_edit_note.components.FormatToolbar
import com.suvojeet.notenext.ui.add_edit_note.components.LabelDialog
import com.suvojeet.notenext.ui.add_edit_note.components.LinkPreviewCard
import com.suvojeet.notenext.ui.add_edit_note.components.MoreOptionsSheet
import com.suvojeet.notenext.ui.add_edit_note.components.NoteEditor
import com.suvojeet.notenext.ui.add_edit_note.components.SaveAsDialog
import com.suvojeet.notenext.ui.add_edit_note.components.InsertLinkDialog
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import com.suvojeet.notenext.ui.settings.SettingsRepository
import com.suvojeet.notenext.ui.settings.ThemeMode
import com.suvojeet.notenext.util.saveAsPdf
import com.suvojeet.notenext.util.saveAsTxt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onDismiss: () -> Unit,
    themeMode: ThemeMode,
    settingsRepository: SettingsRepository
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showFormatBar by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showInsertLinkDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val enableRichLinkPreview by settingsRepository.enableRichLinkPreview.collectAsState(initial = false)

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(state.editingContent) {
        if (state.editingContent.selection.end == state.editingContent.text.length) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
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
        modifier = Modifier.imePadding(),
        topBar = {
            AddEditNoteTopAppBar(
                state = state,
                onEvent = onEvent,
                onDismiss = onDismiss,
                showDeleteDialog = { showDeleteDialog = it }
            )
        },
        bottomBar = {
            AddEditNoteBottomAppBar(
                state = state,
                onEvent = onEvent,
                showColorPicker = { showColorPicker = !showColorPicker },
                showFormatBar = { showFormatBar = !showFormatBar },
                showMoreOptions = { showMoreOptions = it }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(state.editingColor))
                    .verticalScroll(scrollState)
            ) {
                NoteEditor(state = state, onEvent = onEvent)

                if (enableRichLinkPreview && state.linkPreviews.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    state.linkPreviews.forEach { linkPreview ->
                        LinkPreviewCard(linkPreview = linkPreview, onEvent = onEvent)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            AnimatedVisibility(
                visible = showFormatBar,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
            ) {
                FormatToolbar(state = state, onEvent = onEvent)
            }

            AnimatedVisibility(
                visible = showColorPicker,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
            ) {
                ColorPicker(
                    colors = colors,
                    editingColor = state.editingColor,
                    onEvent = onEvent
                )
            }
        }
    }

    if (showMoreOptions) {
        MoreOptionsSheet(
            state = state,
            onEvent = onEvent,
            onDismiss = { showMoreOptions = false },
            showDeleteDialog = { showDeleteDialog = it },
            showSaveAsDialog = { showSaveAsDialog = it },
            showInsertLinkDialog = { showInsertLinkDialog = it }
        )
    }

    if (showInsertLinkDialog) {
        InsertLinkDialog(
            onDismiss = { showInsertLinkDialog = false },
            onConfirm = { url ->
                onEvent(NotesEvent.OnInsertLink(url))
                showInsertLinkDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        val autoDeleteDays by settingsRepository.autoDeleteDays.collectAsState(initial = 7)
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Move note to bin?") },
            text = { Text("This note will be moved to the bin and will be permanently deleted after $autoDeleteDays days.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(NotesEvent.OnDeleteNoteClick)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Move to bin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSaveAsDialog) {
        SaveAsDialog(
            onDismiss = { showSaveAsDialog = false },
            onSaveAsPdf = {
                saveAsPdf(context, state.editingTitle, state.editingContent.text)
                Toast.makeText(context, "Note saved to Documents as PDF", Toast.LENGTH_SHORT).show()
            },
            onSaveAsTxt = {
                saveAsTxt(context, state.editingTitle, state.editingContent.text)
                Toast.makeText(context, "Note saved to Documents as TXT", Toast.LENGTH_SHORT).show()
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
    }
}