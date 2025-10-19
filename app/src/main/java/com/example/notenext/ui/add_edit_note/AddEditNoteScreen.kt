
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.material.icons.automirrored.filled.Label
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
import androidx.compose.ui.platform.LocalClipboardManager

import com.example.notenext.ui.notes.NotesEvent
import com.example.notenext.ui.notes.NotesState
import com.example.notenext.ui.settings.ThemeMode
import com.example.notenext.data.LinkPreview
import com.example.notenext.ui.settings.SettingsRepository
import com.example.notenext.ui.settings.PreferencesKeys
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalUriHandler
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

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
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .fillMaxHeight(0.45f)
                                                                .padding(vertical = 16.dp)
                                                        ) {
                                                            if (!state.editingIsNewNote && state.editingLastEdited != 0L) {
                                                                Text(
                                                                    text = "Last edited: ${dateFormat.format(Date(state.editingLastEdited))}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                                                )
                                                                Divider() // Add a divider for separation
                                                            }
                                                            ListItem(
                                                                headlineContent = { Text("Delete") },
                                                                leadingContent = { Icon(Icons.Default.Delete, contentDescription = "Delete") },
                                                                modifier = Modifier.clickable {
                                                                    showDeleteDialog = true
                                                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                                                        if (!sheetState.isVisible) {
                                                                            showMoreOptions = false
                                                                        }
                                                                    }
                                                                }
                                                            )
                                                            ListItem(
                                                                headlineContent = { Text("Make a copy") },
                                                                leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = "Make a copy") },
                                                                modifier = Modifier.clickable {
                                                                    onEvent(NotesEvent.OnCopyCurrentNoteClick)
                                                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                                                        if (!sheetState.isVisible) {
                                                                            showMoreOptions = false
                                                                        }
                                                                    }
                                                                }
                                                            )
                                                            ListItem(
                                                                headlineContent = { Text("Share") },
                                                                leadingContent = { Icon(Icons.Default.Share, contentDescription = "Share") },
                                                                modifier = Modifier.clickable {
                                                                    val sendIntent: Intent = Intent().apply {
                                                                        action = Intent.ACTION_SEND
                                                                        putExtra(Intent.EXTRA_TEXT, "${state.editingTitle}\n\n${state.editingContent}")
                                                                        putExtra(Intent.EXTRA_SUBJECT, state.editingTitle)
                                                                        type = "text/plain"
                                                                    }
                                                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                                                    context.startActivity(shareIntent)
                                                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                                                        if (!sheetState.isVisible) {
                                                                            showMoreOptions = false
                                                                        }
                                                                    }
                                                                }
                                                            )
                                                            ListItem(
                                                                headlineContent = { Text("Labels") },
                                                                leadingContent = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Labels") },
                                                                modifier = Modifier.clickable {
                                                                    onEvent(NotesEvent.OnAddLabelsToCurrentNoteClick)
                                                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                                                        if (!sheetState.isVisible) {
                                                                            showMoreOptions = false
                                                                        }
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
                        }
                    }
                    
                    @Composable
                    fun LinkPreviewCard(linkPreview: LinkPreview, onEvent: (NotesEvent) -> Unit) {
                        val uriHandler = LocalUriHandler.current
                        val clipboardManager = LocalClipboardManager.current
                        var showMenu by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { linkPreview.url.let { uriHandler.openUri(it) } },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    linkPreview.title?.let { title ->
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Box {
                                        IconButton(onClick = { showMenu = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                                        }
                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Remove preview") },
                                                onClick = {
                                                    onEvent(NotesEvent.OnRemoveLinkPreview(linkPreview.url))
                                                    showMenu = false
                                                },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Remove preview") }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Copy URL") },
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(linkPreview.url))
                                                    showMenu = false
                                                },
                                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL") }
                                            )
                                        }
                                    }
                                }
                                linkPreview.imageUrl?.let { imageUrl ->
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Link preview image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                linkPreview.description?.let { description ->
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                ClickableText(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                                            append(linkPreview.url)
                                        }
                                    },
                                    onClick = { uriHandler.openUri(linkPreview.url) },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                                
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
