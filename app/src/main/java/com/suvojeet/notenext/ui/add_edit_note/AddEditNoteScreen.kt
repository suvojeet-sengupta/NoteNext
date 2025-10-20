
package com.suvojeet.notenext.ui.add_edit_note



import android.content.ContentValues

import android.content.Context

import android.content.Intent

import android.graphics.Canvas

import android.graphics.Paint

import android.graphics.pdf.PdfDocument

import android.os.Environment

import android.provider.MediaStore

import android.text.StaticLayout

import android.text.TextPaint

import android.widget.Toast

import androidx.activity.compose.BackHandler

import androidx.compose.animation.AnimatedVisibility

import androidx.compose.animation.core.tween

import androidx.compose.animation.slideInHorizontally

import androidx.compose.animation.slideOutHorizontally

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.LazyRow

import androidx.compose.foundation.lazy.grid.GridCells

import androidx.compose.foundation.lazy.grid.LazyVerticalGrid

import androidx.compose.foundation.lazy.grid.items

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.text.ClickableText

import androidx.compose.foundation.text.selection.TextSelectionColors

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material.icons.automirrored.filled.Label

import androidx.compose.material.icons.automirrored.rounded.Redo

import androidx.compose.material.icons.automirrored.rounded.Undo

import androidx.compose.material.icons.filled.*

import androidx.compose.material.icons.outlined.Archive

import androidx.compose.material.icons.outlined.PushPin

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.runtime.collectAsState

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.toArgb

import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.platform.LocalClipboardManager

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.platform.LocalUriHandler

import androidx.compose.ui.text.AnnotatedString

import androidx.compose.ui.text.SpanStyle

import androidx.compose.ui.text.buildAnnotatedString

import androidx.compose.ui.text.font.FontStyle

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextDecoration

import androidx.compose.ui.text.withStyle

import androidx.compose.ui.unit.dp

import androidx.compose.ui.window.Dialog

import coil.compose.AsyncImage

import com.suvojeet.notenext.data.LinkPreview

import com.suvojeet.notenext.ui.notes.NotesEvent

import com.suvojeet.notenext.ui.notes.NotesState

import com.suvojeet.notenext.ui.settings.SettingsRepository

import com.suvojeet.notenext.ui.settings.ThemeMode

import kotlinx.coroutines.launch

import java.io.IOException

import java.text.SimpleDateFormat

import java.util.*



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

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()) }

    val scrollState = rememberScrollState()

    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val enableRichLinkPreview by settingsRepository.enableRichLinkPreview.collectAsState(initial = false)

    val sheetState = rememberModalBottomSheetState()



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

                        Box(

                            modifier = Modifier

                                .border(

                                    width = 1.dp,

                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),

                                    shape = MaterialTheme.shapes.medium

                                )

                                .clip(MaterialTheme.shapes.medium)

                                .clickable { onEvent(NotesEvent.OnTogglePinClick) }

                                .background(Color.Transparent)

                                .padding(8.dp),

                            contentAlignment = Alignment.Center

                        ) {

                            Icon(

                                imageVector = Icons.Filled.PushPin,

                                contentDescription = if (state.isPinned) "Unpin note" else "Pin note",

                                tint = if (state.isPinned) MaterialTheme.colorScheme.primary else contentColorFor(backgroundColor = Color(state.editingColor))

                            )

                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(

                            modifier = Modifier

                                .border(

                                    width = 1.dp,

                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),

                                    shape = MaterialTheme.shapes.medium

                                )

                                .clip(MaterialTheme.shapes.medium)

                                .clickable { onEvent(NotesEvent.OnToggleArchiveClick) }

                                .background(Color.Transparent)

                                .padding(8.dp),

                            contentAlignment = Alignment.Center

                        ) {

                            Icon(

                                imageVector = Icons.Filled.Archive,

                                contentDescription = if (state.isArchived) "Unarchive note" else "Archive note",

                                tint = if (state.isArchived) MaterialTheme.colorScheme.primary else contentColorFor(backgroundColor = Color(state.editingColor))

                            )

                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(

                            modifier = Modifier

                                .border(

                                    width = 1.dp,

                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),

                                    shape = MaterialTheme.shapes.medium

                                )

                                .clip(MaterialTheme.shapes.medium)

                                .clickable { showDeleteDialog = true }

                                .background(Color.Transparent)

                                .padding(8.dp),

                            contentAlignment = Alignment.Center

                        ) {

                            Icon(

                                Icons.Default.Delete,

                                contentDescription = "Delete note",

                                tint = contentColorFor(backgroundColor = Color(state.editingColor))

                            )

                        }

                    }

                }

            )

        },

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

                val titleTextColor = contentColorFor(backgroundColor = Color(state.editingColor))

                TextField(

                    value = state.editingTitle,

                    onValueChange = { newTitle: String -> onEvent(NotesEvent.OnTitleChange(newTitle)) },

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

                    textStyle = MaterialTheme.typography.headlineMedium.copy(color = titleTextColor)

                )

                Spacer(modifier = Modifier.height(8.dp))

                val contentTextColor = contentColorFor(backgroundColor = Color(state.editingColor))

                TextField(

                    value = state.editingContent,

                    onValueChange = { onEvent(NotesEvent.OnContentChange(it)) },

                    placeholder = { Text("Note", color = contentColorFor(backgroundColor = Color(state.editingColor))) },

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

                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentTextColor)

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

                                    LazyRow(

                                        modifier = Modifier

                                            .fillMaxWidth()

                                            .background(MaterialTheme.colorScheme.surface)

                                            .padding(vertical = 8.dp),

                                        horizontalArrangement = Arrangement.spacedBy(16.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        item {

                                            IconButton(

                                                onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(fontWeight = FontWeight.Bold))) },

                                                colors = IconButtonDefaults.iconButtonColors(

                                                    containerColor = if (state.isBoldActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

                                                )

                                            ) {

                                                Icon(Icons.Default.FormatBold, contentDescription = "Bold")

                                            }

                                        }

                                        item {

                                            IconButton(

                                                onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(fontStyle = FontStyle.Italic))) },

                                                colors = IconButtonDefaults.iconButtonColors(

                                                    containerColor = if (state.isItalicActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

                                                )

                                            ) {

                                                Icon(Icons.Default.FormatItalic, contentDescription = "Italic")

                                            }

                                        }

                                        item {

                                            IconButton(

                                                onClick = { onEvent(NotesEvent.ApplyStyleToContent(SpanStyle(textDecoration = TextDecoration.Underline))) },

                                                colors = IconButtonDefaults.iconButtonColors(

                                                    containerColor = if (state.isUnderlineActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

                                                )

                                            ) {

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

                                        items(colors) {

                                            color ->

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

                                    containerColor = Color(state.editingColor),

                                    windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)

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

                    

                                                if (showMoreOptions) {

                                                    ModalBottomSheet(

                                                        onDismissRequest = {

                                                            scope.launch { sheetState.hide() }.invokeOnCompletion {

                                                                if (!sheetState.isVisible) {

                                                                    showMoreOptions = false

                                                                }

                                                            }

                                                        },

                                                        sheetState = sheetState

                                                    ) {

                                                        Column(

                                                            modifier = Modifier.fillMaxWidth(),

                                                            horizontalAlignment = Alignment.CenterHorizontally

                                                        ) {

                                                            if (!state.editingIsNewNote && state.editingLastEdited != 0L) {

                                                                Text(

                                                                    text = "Last edited: ${dateFormat.format(Date(state.editingLastEdited))}",

                                                                    style = MaterialTheme.typography.labelSmall,

                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),

                                                                    modifier = Modifier.padding(vertical = 16.dp)

                                                                )

                                                                Divider()

                                                            }



                                                            val options = listOf(

                                                                "Delete" to Icons.Default.Delete,

                                                                "Make a copy" to Icons.Default.ContentCopy,

                                                                "Share" to Icons.Default.Share,

                                                                "Labels" to Icons.AutoMirrored.Filled.Label,

                                                                "Save as" to Icons.Default.Check

                                                            )



                                                            LazyVerticalGrid(

                                                                columns = GridCells.Adaptive(minSize = 100.dp),

                                                                contentPadding = PaddingValues(16.dp),

                                                                horizontalArrangement = Arrangement.spacedBy(16.dp),

                                                                verticalArrangement = Arrangement.spacedBy(16.dp),

                                                                modifier = Modifier.fillMaxHeight(0.45f)

                                                            ) {

                                                                items(options) { (label, icon) ->

                                                                    MoreOptionsItem(

                                                                        icon = icon,

                                                                        label = label,

                                                                        onClick = {

                                                                            scope.launch { sheetState.hide() }.invokeOnCompletion {

                                                                                if (!sheetState.isVisible) {

                                                                                    showMoreOptions = false

                                                                                }

                                                                            }

                                                                            when (label) {

                                                                                "Delete" -> showDeleteDialog = true

                                                                                "Make a copy" -> onEvent(NotesEvent.OnCopyCurrentNoteClick)

                                                                                "Share" -> {

                                                                                    val sendIntent: Intent = Intent().apply {

                                                                                        action = Intent.ACTION_SEND

                                                                                        putExtra(Intent.EXTRA_TEXT, "${state.editingTitle}\n\n${state.editingContent.text}")

                                                                                        putExtra(Intent.EXTRA_SUBJECT, state.editingTitle)

                                                                                        type = "text/plain"

                                                                                    }

                                                                                    val shareIntent = Intent.createChooser(sendIntent, null)

                                                                                    context.startActivity(shareIntent)

                                                                                }

                                                                                "Labels" -> onEvent(NotesEvent.OnAddLabelsToCurrentNoteClick)

                                                                                "Save as" -> showSaveAsDialog = true

                                                                            }

                                                                        }

                                                                    )

                                                                }

                                                            }

                                                        }

                                                    }

                                                }

                                            }

                                        }

                                    }

                                }

                            }

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



@Composable

private fun SaveAsDialog(

    onDismiss: () -> Unit,

    onSaveAsPdf: () -> Unit,

    onSaveAsTxt: () -> Unit

) {

    Dialog(onDismissRequest = onDismiss) {

        Card(

            shape = MaterialTheme.shapes.large

        ) {

            Column(modifier = Modifier.padding(24.dp)) {

                Text(text = "Save note as", style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.height(16.dp))

                Text("Select format to save the note in Documents folder.")

                Spacer(modifier = Modifier.height(24.dp))

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.End

                ) {

                    TextButton(onClick = onDismiss) {

                        Text("Cancel")

                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = { onSaveAsTxt(); onDismiss() }) {

                        Text("TXT")

                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = { onSaveAsPdf(); onDismiss() }) {

                        Text("PDF")

                    }

                }

            }

        }

    }

}



private fun saveAsTxt(context: Context, title: String, content: String) {

    val contentResolver = context.contentResolver

    val contentValues = ContentValues().apply {

        put(MediaStore.MediaColumns.DISPLAY_NAME, "${title.ifBlank { "Untitled" }}.txt")

        put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")

        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)

    }



    val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)



    uri?.let {

        try {

            contentResolver.openOutputStream(it)?.use { outputStream ->

                outputStream.write("$title\n\n$content".toByteArray())

            }

        } catch (e: IOException) {

            e.printStackTrace()

        }

    }

}



private fun saveAsPdf(context: Context, title: String, content: String) {

    val pdfDocument = PdfDocument()

    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size

    val page = pdfDocument.startPage(pageInfo)

    val canvas: Canvas = page.canvas

    val titlePaint = Paint()

    titlePaint.textSize = 18f

    titlePaint.isFakeBoldText = true

    canvas.drawText(title, 40f, 60f, titlePaint)



    val textPaint = TextPaint()

    textPaint.textSize = 12f



    val contentToSave = content.ifBlank { " " } // StaticLayout crashes on empty string



    val staticLayout = StaticLayout.Builder.obtain(

        contentToSave, 0, contentToSave.length, textPaint, canvas.width - 80

    ).build()



    canvas.save()

    canvas.translate(40f, 90f)

    staticLayout.draw(canvas)

    canvas.restore()



    pdfDocument.finishPage(page)



    val contentResolver = context.contentResolver

    val contentValues = ContentValues().apply {

        put(MediaStore.MediaColumns.DISPLAY_NAME, "${title.ifBlank { "Untitled" }}.pdf")

        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")

        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)

    }



    val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)



    uri?.let {

        try {

            contentResolver.openOutputStream(it)?.use { outputStream ->

                pdfDocument.writeTo(outputStream)

            }

        } catch (e: IOException) {

            e.printStackTrace()

        } finally {

            pdfDocument.close()

        }

    }

}
