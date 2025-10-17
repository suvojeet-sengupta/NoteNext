package com.example.notenext.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as StaggeredGridItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notenext.data.Note
import com.example.notenext.dependency_injection.ViewModelFactory
import com.example.notenext.ui.add_edit_note.AddEditNoteScreen
import com.example.notenext.ui.settings.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    factory: ViewModelFactory,
    onSettingsClick: () -> Unit,
    onArchiveClick: () -> Unit,
    themeMode: ThemeMode
) {
    val viewModel: NotesViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val isSelectionModeActive = state.selectedNoteIds.isNotEmpty()
    var showLabelDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NotesUiEvent.SendNotes -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, event.title)
                        putExtra(Intent.EXTRA_TEXT, event.content)
                    }
                    val chooser = Intent.createChooser(intent, "Send notes via")
                    context.startActivity(chooser)
                }
            }
        }
    }

    BackHandler(enabled = isSearchActive || isSelectionModeActive || drawerState.isOpen || state.expandedNoteId != null) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            isSelectionModeActive -> viewModel.onEvent(NotesEvent.ClearSelection)
            isSearchActive -> {
                isSearchActive = false
                searchQuery = ""
            }
            state.expandedNoteId != null -> viewModel.onEvent(NotesEvent.CollapseNote)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.8f)) {
                    Text(
                        text = "NoteNext",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Archive, contentDescription = "Archive") },
                        label = { Text("Archive") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onArchiveClick()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onSettingsClick()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "LABELS",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Label, contentDescription = "All Notes") },
                        label = { Text("All Notes") },
                        selected = state.filteredLabel == null,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.onEvent(NotesEvent.FilterByLabel(null))
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    state.labels.forEach { label ->
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Outlined.Label, contentDescription = label) },
                            label = { Text(label) },
                            selected = state.filteredLabel == label,
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.onEvent(NotesEvent.FilterByLabel(label))
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    if (isSelectionModeActive) {
                        ContextualTopAppBar(
                            selectedItemCount = state.selectedNoteIds.size,
                            onClearSelection = { viewModel.onEvent(NotesEvent.ClearSelection) },
                            onTogglePinClick = { viewModel.onEvent(NotesEvent.TogglePinForSelectedNotes) },
                            onReminderClick = { viewModel.onEvent(NotesEvent.SetReminderForSelectedNotes(null)) }, // Placeholder
                            onColorClick = { /* TODO */ },
                            onArchiveClick = { viewModel.onEvent(NotesEvent.ArchiveSelectedNotes) },
                            onDeleteClick = { viewModel.onEvent(NotesEvent.DeleteSelectedNotes) },
                            onCopyClick = { viewModel.onEvent(NotesEvent.CopySelectedNotes) },
                            onSendClick = { viewModel.onEvent(NotesEvent.SendSelectedNotes) },
                            onLabelClick = { showLabelDialog = true }
                        )
                    } else {
                        AnimatedContent(
                            targetState = isSearchActive,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                                        fadeOut(animationSpec = tween(90))
                            }, label = ""
                        ) { searchTargetState ->
                            if (searchTargetState) {
                                ExpandedSearchView(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { searchQuery = it },
                                    onCloseSearch = {
                                        isSearchActive = false
                                        searchQuery = ""
                                    }
                                )
                            } else {
                                CollapsedTopAppBar(
                                    onSearchClick = { isSearchActive = true },
                                    onMenuClick = { scope.launch { drawerState.open() } }
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { viewModel.onEvent(NotesEvent.ExpandNote(-1)) },
                        content = {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add note")
                        }
                    )
                }
            ) { padding ->
                if (showLabelDialog) {
                    LabelDialog(
                        labels = state.labels,
                        onDismiss = { showLabelDialog = false },
                        onConfirm = { label ->
                            viewModel.onEvent(NotesEvent.SetLabelForSelectedNotes(label))
                            showLabelDialog = false
                        }
                    )
                }
                Column(modifier = Modifier.padding(padding)) {
                    val notesToDisplay = if (state.filteredLabel == null) {
                        state.notes
                    } else {
                        state.notes.filter { it.label == state.filteredLabel }
                    }

                    if (notesToDisplay.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(96.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No notes yet. Tap the '+' button to add one.",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    } else {
                        val filteredNotes = notesToDisplay.filter { note ->
                            !note.isArchived && (note.title.contains(searchQuery, ignoreCase = true) || note.content.contains(searchQuery, ignoreCase = true))
                        }
                        val pinnedNotes = filteredNotes.filter { it.isPinned }
                        val otherNotes = filteredNotes.filter { !it.isPinned }

                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalItemSpacing = 8.dp
                        ) {
                            if (pinnedNotes.isNotEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    Text(
                                        text = "PINNED",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StaggeredGridItems(pinnedNotes, key = { it.id }) { note ->
                                    val isExpanded = state.expandedNoteId == note.id
                                    NoteItem(
                                        modifier = Modifier.graphicsLayer { alpha = if (isExpanded) 0f else 1f },
                                        note = note,
                                        isSelected = state.selectedNoteIds.contains(note.id),
                                        onNoteClick = {
                                            if (isSelectionModeActive) {
                                                viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.id))
                                            } else {
                                                viewModel.onEvent(NotesEvent.ExpandNote(note.id))
                                            }
                                        },
                                        onNoteLongClick = {
                                            viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.id))
                                        }
                                    )
                                }
                            }

                            if (otherNotes.isNotEmpty()) {
                                if (pinnedNotes.isNotEmpty()) {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        Text(
                                            text = "OTHERS",
                                            modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                StaggeredGridItems(otherNotes, key = { it.id }) { note ->
                                    val isExpanded = state.expandedNoteId == note.id
                                    NoteItem(
                                        modifier = Modifier.graphicsLayer { alpha = if (isExpanded) 0f else 1f },
                                        note = note,
                                        isSelected = state.selectedNoteIds.contains(note.id),
                                        onNoteClick = {
                                            if (isSelectionModeActive) {
                                                viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.id))
                                            } else {
                                                viewModel.onEvent(NotesEvent.ExpandNote(note.id))
                                            }
                                        },
                                        onNoteLongClick = {
                                            viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.id))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.expandedNoteId != null,
            enter = scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = scaleOut(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            AddEditNoteScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onDismiss = { viewModel.onEvent(NotesEvent.CollapseNote) },
                themeMode = themeMode
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    modifier: Modifier = Modifier,
    note: Note,
    isSelected: Boolean,
    onNoteClick: () -> Unit,
    onNoteLongClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onNoteClick,
                onLongClick = onNoteLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (note.isPinned) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = "Pinned note",
                    modifier = Modifier.size(16.dp).align(Alignment.End),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Title
            if (note.title.isNotEmpty()) {
                Text(
                    text = note.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Content
            if (note.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!note.label.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = note.label,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextualTopAppBar(
    selectedItemCount: Int,
    onClearSelection: () -> Unit,
    onTogglePinClick: () -> Unit,
    onReminderClick: () -> Unit,
    onColorClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onSendClick: () -> Unit,
    onLabelClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(text = "$selectedItemCount selected") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Clear selection")
            }
        },
        actions = {
            IconButton(onClick = onTogglePinClick) {
                Icon(Icons.Outlined.PushPin, contentDescription = "Pin note")
            }
            IconButton(onClick = onReminderClick) {
                Icon(Icons.Default.Notifications, contentDescription = "Set reminder")
            }
            IconButton(onClick = onColorClick) {
                Icon(Icons.Default.Palette, contentDescription = "Change color")
            }
            IconButton(onClick = onLabelClick) {
                Icon(Icons.Outlined.Label, contentDescription = "Add label")
            }
            Box {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Archive") },
                        onClick = {
                            onArchiveClick()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDeleteClick()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Make a copy") },
                        onClick = {
                            onCopyClick()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            onSendClick()
                            showMenu = false
                        }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsedTopAppBar(
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    TopAppBar(
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onSearchClick() }
                        .padding(vertical = 12.dp, horizontal = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Search your notes",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Open navigation drawer")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedSearchView(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    TopAppBar(
        title = {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = { Text("Search your notes") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .padding(end = 16.dp),                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseSearch) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Close Search")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
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
                    onConfirm(newLabel)
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