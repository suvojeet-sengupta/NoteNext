package com.suvojeet.notenext.ui.add_edit_note

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.suvojeet.notenext.ui.add_edit_note.components.*
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import com.suvojeet.notenext.ui.notes.NotesUiEvent
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.data.saveAsPdf
import com.suvojeet.notenext.data.saveAsTxt
import com.suvojeet.notenext.data.saveAsMd
import com.suvojeet.notenext.util.ImageUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharedFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import com.suvojeet.notenext.util.HtmlConverter
import com.suvojeet.notenext.data.MarkdownExporter
import com.suvojeet.notenext.util.printNote
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle

data class ImageViewerData(val uri: Uri, val tempId: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onDismiss: () -> Unit,
    themeMode: ThemeMode,
    settingsRepository: SettingsRepository,
    events: SharedFlow<NotesUiEvent>,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showFormatBar by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showInsertLinkDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageData by remember { mutableStateOf<ImageViewerData?>(null) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enableRichLinkPreview by settingsRepository.enableRichLinkPreview.collectAsState(initial = false)
    var isFocusMode by remember { mutableStateOf(false) }
    var showMarkdownPreview by remember { mutableStateOf(false) }

    val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        scope.launch {
            uris.forEach { uri ->
                val compressedUri = ImageUtils.compressImage(context, uri)
                if (compressedUri != null) {
                    val mimeType = context.contentResolver.getType(compressedUri)
                    onEvent(NotesEvent.AddAttachment(compressedUri.toString(), mimeType ?: "image/jpeg"))
                } else {
                    val mimeType = context.contentResolver.getType(uri)
                    try {
                        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (e: Exception) {
                        // Ignore if we can't take permission (e.g. if we don't need it or it's not persistable)
                    }
                    onEvent(NotesEvent.AddAttachment(uri.toString(), mimeType ?: ""))
                }
            }
        }
    }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let { uri ->
                scope.launch {
                    val compressedUri = ImageUtils.compressImage(context, uri)
                    if (compressedUri != null) {
                        val mimeType = context.contentResolver.getType(compressedUri)
                        onEvent(NotesEvent.AddAttachment(compressedUri.toString(), mimeType ?: "image/jpeg"))
                    } else {
                        val mimeType = context.contentResolver.getType(uri)
                         try {
                            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } catch (e: Exception) {
                        }
                        onEvent(NotesEvent.AddAttachment(uri.toString(), mimeType ?: "image/jpeg"))
                    }
                }
            }
        }
    }

    BackHandler {
        if (showImageViewer) {
            showImageViewer = false
        } else if (isFocusMode) {
            isFocusMode = false
        } else {
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        events.collect {
            event ->
            when (event) {
                is NotesUiEvent.LinkPreviewRemoved -> {
                    Toast.makeText(context, "Link preview removed", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
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

    val backgroundColor = MaterialTheme.colorScheme.surface

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                AnimatedVisibility(
                    visible = !isFocusMode,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    AddEditNoteTopAppBar(
                        state = state,
                        onEvent = onEvent,
                        onDismiss = onDismiss,
                        showDeleteDialog = { showDeleteDialog = it },
                        editingNoteType = state.editingNoteType,
                        onToggleFocusMode = { isFocusMode = !isFocusMode },
                        isFocusMode = isFocusMode,
                        onToggleMarkdownPreview = { showMarkdownPreview = !showMarkdownPreview },
                        isMarkdownPreviewVisible = showMarkdownPreview
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = !isFocusMode,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    AddEditNoteBottomAppBar(
                        state = state,
                        onEvent = onEvent,
                        showColorPicker = { showColorPicker = !showColorPicker },
                        showFormatBar = { showFormatBar = !showFormatBar },
                        showMoreOptions = { showMoreOptions = it },
                        onImageClick = {
                            getContent.launch("image/*")
                        },
                        onTakePhotoClick = {
                            val uri = createImageFile(context)
                            photoUri = uri
                            takePictureLauncher.launch(uri)
                        },
                        onAudioClick = {
                            Toast.makeText(context, "Audio recording not implemented yet", Toast.LENGTH_SHORT).show()
                        },
                        themeMode = themeMode
                    )
                }
            }
        ) { padding ->
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (state.editingNoteType == "TEXT") {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(backgroundColor)
                                .verticalScroll(scrollState)
                        ) {
                            val imageAttachments = state.editingAttachments.filter { it.type == "IMAGE" }
                            if (imageAttachments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                val imageCount = imageAttachments.size
                                if (imageCount == 1) {
                                    // Single image: full width
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        AsyncImage(
                                            model = imageAttachments.first().uri,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 400.dp)
                                                .clickable {
                                                    selectedImageData = ImageViewerData(uri = Uri.parse(imageAttachments.first().uri), tempId = imageAttachments.first().tempId)
                                                    showImageViewer = true
                                                },
                                            contentScale = ContentScale.Fit
                                        )
                                        IconButton(
                                            onClick = { onEvent(NotesEvent.RemoveAttachment(imageAttachments.first().tempId)) },
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove image", tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                } else {
                                    // Multiple images: up to 3 per row in a horizontal scrollable row
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        items(imageAttachments, key = { it.uri }) { attachment ->
                                            Box {
                                                AsyncImage(
                                                    model = attachment.uri,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .width(120.dp)
                                                        .height(120.dp)
                                                        .aspectRatio(1f)
                                                        .clickable {
                                                            selectedImageData = ImageViewerData(uri = Uri.parse(attachment.uri), tempId = attachment.tempId)
                                                            showImageViewer = true
                                                        },
                                                    contentScale = ContentScale.Crop
                                                )
                                                IconButton(
                                                    onClick = { onEvent(NotesEvent.RemoveAttachment(attachment.tempId)) },
                                                    modifier = Modifier.align(Alignment.TopEnd)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Remove image", tint = MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            var clickedUrl by remember { mutableStateOf<String?>(null) }
                            
                            if (showMarkdownPreview) {
                                val markdownContent = produceState(initialValue = "") {
                                    val html = HtmlConverter.annotatedStringToHtml(state.editingContent.annotatedString)
                                    value = MarkdownExporter.convertHtmlToMarkdown(html)
                                }
                                MarkdownPreview(content = markdownContent.value)
                            } else {
                                NoteEditor(
                                    state = state, 
                                    onEvent = onEvent,
                                    onUrlClick = { url ->
                                        clickedUrl = url
                                    }
                                )
                            }
                            
                            if (clickedUrl != null) {
                                AlertDialog(
                                    onDismissRequest = { clickedUrl = null },
                                    title = { Text("Open Link") },
                                    text = { Text("Do you want to open this link?\n\n$clickedUrl") },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                openUrl(context, clickedUrl!!)
                                                clickedUrl = null
                                            }
                                        ) {
                                            Text("Open")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { clickedUrl = null }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }

                            if (enableRichLinkPreview && state.linkPreviews.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                state.linkPreviews.forEach { linkPreview ->
                                    LinkPreviewCard(linkPreview = linkPreview, onEvent = onEvent)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .background(backgroundColor)
                        ) {
                            item {
                                val titleTextColor = contentColorFor(backgroundColor = backgroundColor)
                                TextField(
                                    value = state.editingTitle,
                                    onValueChange = { newTitle: String -> onEvent(NotesEvent.OnTitleChange(newTitle)) },
                                    placeholder = { Text("Title", color = contentColorFor(backgroundColor = backgroundColor)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = contentColorFor(backgroundColor = backgroundColor),
                                        selectionColors = TextSelectionColors(
                                            handleColor = contentColorFor(backgroundColor = backgroundColor),
                                            backgroundColor = contentColorFor(backgroundColor = backgroundColor).copy(alpha = 0.4f)
                                        )
                                    ),
                                    textStyle = MaterialTheme.typography.headlineMedium.copy(color = titleTextColor),
                                    singleLine = true,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            item {
                                val imageAttachments = state.editingAttachments.filter { it.type == "IMAGE" }
                                if (imageAttachments.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    val imageCount = imageAttachments.size
                                    if (imageCount == 1) {
                                        // Single image: full width
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            AsyncImage(
                                                model = imageAttachments.first().uri,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 400.dp)
                                                    .clickable {
                                                        selectedImageData = ImageViewerData(uri = Uri.parse(imageAttachments.first().uri), tempId = imageAttachments.first().tempId)
                                                        showImageViewer = true
                                                    },
                                                contentScale = ContentScale.Fit
                                            )
                                            IconButton(
                                                onClick = { onEvent(NotesEvent.RemoveAttachment(imageAttachments.first().tempId)) },
                                                modifier = Modifier.align(Alignment.TopEnd)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove image", tint = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    } else {
                                        // Multiple images: up to 3 per row in a horizontal scrollable row
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            items(imageAttachments, key = { it.uri }) { attachment ->
                                                Box {
                                                    AsyncImage(
                                                        model = attachment.uri,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .width(120.dp)
                                                            .height(120.dp)
                                                            .aspectRatio(1f)
                                                            .clickable {
                                                                selectedImageData = ImageViewerData(uri = Uri.parse(attachment.uri), tempId = attachment.tempId)
                                                                showImageViewer = true
                                                            },
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    IconButton(
                                                        onClick = { onEvent(NotesEvent.RemoveAttachment(attachment.tempId)) },
                                                        modifier = Modifier.align(Alignment.TopEnd)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Remove image", tint = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                            // ChecklistEditor usage
                            ChecklistEditor(
                                state = state,
                                onEvent = onEvent,
                                isCheckedItemsExpanded = state.isCheckedItemsExpanded,
                                onToggleCheckedItems = { onEvent(NotesEvent.ToggleCheckedItemsExpanded) }
                            )

                            if (enableRichLinkPreview && state.linkPreviews.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                items(state.linkPreviews) { linkPreview ->
                                    LinkPreviewCard(linkPreview = linkPreview, onEvent = onEvent)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    // FormatToolbar removed from here and moved to Box overlay below
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
        }
        
        // Floating Toolbar
        AnimatedVisibility(
            visible = showFormatBar && (state.editingNoteType == "TEXT" || state.editingNoteType == "CHECKLIST"),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .padding(bottom = 60.dp) // Sit above the bottom bar
        ) {
            Surface(
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                FormatToolbar(
                    state = state, 
                    onEvent = onEvent, 
                    onInsertLinkClick = { showInsertLinkDialog = true }, 
                    themeMode = themeMode,
                    modifier = Modifier.padding(8.dp)
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
            showHistoryDialog = { showHistoryDialog = it },
            onPrint = {
                scope.launch {
                    val html = HtmlConverter.annotatedStringToHtml(state.editingContent.annotatedString)
                    printNote(context, html)
                }
            }
        )
    }

    if (showHistoryDialog) {
        NoteHistoryDialog(
            versions = state.editingNoteVersions,
            onDismiss = { showHistoryDialog = false },
            onVersionSelected = { version ->
                onEvent(NotesEvent.OnRestoreVersion(version))
            }
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
                saveAsPdf(context, state.editingTitle, state.editingContent.annotatedString, state.editingAttachments, state.editingChecklist)
                Toast.makeText(context, "Note saved to Documents as PDF", Toast.LENGTH_SHORT).show()
            },
            onSaveAsTxt = {
                saveAsTxt(context, state.editingTitle, state.editingContent.text, state.editingChecklist)
                Toast.makeText(context, "Note saved to Documents as TXT", Toast.LENGTH_SHORT).show()
            },
            onSaveAsMd = {
                scope.launch {
                    saveAsMd(context, state.editingTitle, state.editingContent.annotatedString, state.editingChecklist)
                    Toast.makeText(context, "Note saved to Documents as MD", Toast.LENGTH_SHORT).show()
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

    AnimatedVisibility(
        visible = showImageViewer && selectedImageData != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        selectedImageData?.let { data ->
            ImageViewerScreen(
                imageUri = data.uri,
                attachmentTempId = data.tempId,
                onDismiss = { showImageViewer = false },
                onEvent = onEvent
            )
        }
    }
}

private fun createImageFile(context: Context): Uri {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val file = File.createTempFile(
        "JPEG_${timeStamp}_",
        ".jpg",
        storageDir
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}

@Composable
fun MarkdownPreview(content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val lines = content.split("\n")
        var inList = false
        
        lines.forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("# ")),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("## ")),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(line.removePrefix("### ")),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(start = 8.dp)) {
                        Text(text = "• ", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = parseInlineMarkdown(line.substring(2)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                line.startsWith("> ") -> {
                    Surface(
                        modifier = Modifier.padding(start = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = parseInlineMarkdown(line.removePrefix("> ")),
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                line.trim() == "---" -> {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = parseInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        var currentText = text
        
        // Match bold **text** or __text__
        val boldRegex = "(\\s|^)(\\*\\*|__)(.*?)\\2".toRegex()
        // Match italic *text* or _text_
        val italicRegex = "(\\s|^)(\\*|_)(.*?)\\2".toRegex()
        // Match links [text](url)
        val linkRegex = "\\[(.*?)\\]\\((.*?)\\)".toRegex()

        var lastIndex = 0
        val allMatches = (boldRegex.findAll(text) + italicRegex.findAll(text) + linkRegex.findAll(text))
            .sortedBy { it.range.first }

        allMatches.forEach { match ->
            if (match.range.first >= lastIndex) {
                append(text.substring(lastIndex, match.range.first))
                
                when {
                    match.value.startsWith("**") || match.value.startsWith("__") || (match.value.trim().startsWith("**")) -> {
                        val content = match.groupValues[3]
                        val prefix = match.groupValues[1]
                        append(prefix)
                        withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) {
                            append(content)
                        }
                    }
                    match.value.startsWith("*") || match.value.startsWith("_") || (match.value.trim().startsWith("*")) -> {
                        val content = match.groupValues[3]
                        val prefix = match.groupValues[1]
                        append(prefix)
                        withStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                            append(content)
                        }
                    }
                    match.value.startsWith("[") -> {
                        val linkText = match.groupValues[1]
                        val url = match.groupValues[2]
                        pushStringAnnotation(tag = "URL", annotation = url)
                        withStyle(androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                            append(linkText)
                        }
                        pop()
                    }
                }
                lastIndex = match.range.last + 1
        }
        
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}


private fun openUrl(context: Context, url: String) {
    try {
        val finalUrl = if (url.startsWith("www.")) "https://$url" else url
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
    }
}

