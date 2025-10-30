
package com.suvojeet.notenext.ui.project

import com.suvojeet.notenext.ui.notes.NotesEvent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectNotesScreen(
    factory: ViewModelFactory,
    onBackClick: () -> Unit,
    themeMode: ThemeMode,
    settingsRepository: SettingsRepository
) {
    val viewModel: ProjectNotesViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val isSelectionModeActive = state.selectedNoteIds.isNotEmpty()
    var showLabelDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReminderSetDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isSearchActive || isSelectionModeActive || state.expandedNoteId != null) {
        when {
            isSelectionModeActive -> viewModel.onEvent(ProjectNotesEvent.ClearSelection)
            isSearchActive -> {
                isSearchActive = false
                searchQuery = ""
            }
            state.expandedNoteId != null -> viewModel.onEvent(ProjectNotesEvent.CollapseNote)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                AnimatedContent(
                    targetState = isSelectionModeActive,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220, delayMillis = 90)).togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "TopAppBar Animation"
                ) { targetState ->
                    if (targetState) {
                        ContextualTopAppBar(
                            selectedItemCount = state.selectedNoteIds.size,
                            onClearSelection = { viewModel.onEvent(ProjectNotesEvent.ClearSelection) },
                            onTogglePinClick = { viewModel.onEvent(ProjectNotesEvent.TogglePinForSelectedNotes) },
                            onReminderClick = { showReminderSetDialog = true },
                            onColorClick = { /* TODO */ },
                            onArchiveClick = { viewModel.onEvent(ProjectNotesEvent.ArchiveSelectedNotes) },
                            onDeleteClick = { showDeleteDialog = true },
                            onCopyClick = { viewModel.onEvent(ProjectNotesEvent.CopySelectedNotes) },
                            onSendClick = { viewModel.onEvent(ProjectNotesEvent.SendSelectedNotes) },
                            onLabelClick = { showLabelDialog = true }
                        )
                    } else {
                        AnimatedContent(
                            targetState = isSearchActive,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220, delayMillis = 90)).togetherWith(fadeOut(animationSpec = tween(90)))
                            },
                            label = "SearchAppBar Animation"
                        ) { searchActive ->
                            if (searchActive) {
                                SearchTopAppBar(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { searchQuery = it },
                                    onBackClick = { isSearchActive = false; searchQuery = "" }
                                )
                            } else {
                                TopAppBar(
                                    title = { Text(state.projectName) },
                                    navigationIcon = {
                                        IconButton(onClick = onBackClick) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { viewModel.onEvent(ProjectNotesEvent.ExpandNote(-1)) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add note")
                }
            }
        ) { padding ->
            val autoDeleteDays by settingsRepository.autoDeleteDays.collectAsState(initial = 7)
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Move notes to bin?") },
                    text = { Text("Selected notes will be moved to the bin and permanently deleted after ${autoDeleteDays} days.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.onEvent(ProjectNotesEvent.DeleteSelectedNotes)
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
            if (showLabelDialog) {
                LabelDialog(
                    labels = state.labels,
                    onDismiss = { showLabelDialog = false },
                    onConfirm = { label ->
                        viewModel.onEvent(ProjectNotesEvent.SetLabelForSelectedNotes(label))
                        showLabelDialog = false
                    }
                )
            }
            if (showReminderSetDialog) {
                ReminderSetDialog(
                    onDismissRequest = { showReminderSetDialog = false },
                    onConfirm = { date, time, repeatOption ->
                        viewModel.onEvent(ProjectNotesEvent.SetReminderForSelectedNotes(date, time, repeatOption))
                        showReminderSetDialog = false
                    }
                )
            }

            Column(modifier = Modifier.padding(padding)) {
                Spacer(modifier = Modifier.height(8.dp))

                if (state.notes.isEmpty()) {
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
                    val filteredNotes = state.notes.filter { note ->
                        !note.note.isArchived && (note.note.title.contains(searchQuery, ignoreCase = true) || note.note.content.contains(searchQuery, ignoreCase = true))
                    }
                    val pinnedNotes = filteredNotes.filter { it.note.isPinned }
                    val otherNotes = filteredNotes.filter { !it.note.isPinned }

                    when (state.layoutType) {
                        LayoutType.GRID -> {
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
                                    StaggeredGridItems(pinnedNotes, key = { it.note.id }) { note ->
                                        val isExpanded = state.expandedNoteId == note.note.id
                                        NoteItem(
                                            modifier = Modifier.graphicsLayer { alpha = if (isExpanded) 0f : 1f },
                                            note = note,
                                            isSelected = state.selectedNoteIds.contains(note.note.id),
                                            onNoteClick = {
                                                if (isSelectionModeActive) {
                                                    viewModel.onEvent(ProjectNotesEvent.ToggleNoteSelection(note.note.id))
                                                } else {
                                                    viewModel.onEvent(ProjectNotesEvent.ExpandNote(note.note.id))
                                                }
                                            },
                                            onNoteLongClick = {
                                                viewModel.onEvent(ProjectNotesEvent.ToggleNoteSelection(note.note.id))
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
                                    StaggeredGridItems(otherNotes, key = { it.note.id }) { note ->
                                        val isExpanded = state.expandedNoteId == note.note.id
                                        NoteItem(
                                            modifier = Modifier.graphicsLayer { alpha = if (isExpanded) 0f : 1f },
                                            note = note,
                                            isSelected = state.selectedNoteIds.contains(note.note.id),
                                            onNoteClick = {
                                                if (isSelectionModeActive) {
                                                    viewModel.onEvent(ProjectNotesEvent.ToggleNoteSelection(note.note.id))
                                                } else {
                                                    viewModel.onEvent(ProjectNotesEvent.ExpandNote(note.note.id))
                                                }
                                            },
                                            onNoteLongClick = {
                                                viewModel.onEvent(ProjectNotesEvent.ToggleNoteSelection(note.note.id))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        LayoutType.LIST -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (pinnedNotes.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "PINNED",
                                            modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    items(pinnedNotes, key = { it.note.id }) { note ->
                                        val isExpanded = state.expandedNoteId == note.note.id
                                        NoteItem(
                                            modifier = Modifier.graphicsLayer { alpha = if (isExpanded) 0f : 1f },
                                            note = note,
                                            isSelected = state.selectedNoteIds.contains(note.note.id),
                                            onNoteClick = {
                                                if (isSelectionModeActive) {
                                                    viewModel.onEvent(ProjectNotesEvent.ToggleNoteSelection(note.note.id))
                                                } else {
                                                    viewModel.onEvent(ProjectNotesEvent.ExpandNote(note.note.id))
                                                }
                                            },
                                            onNoteLongClick = {
                                                viewModel.onEvent(ProjectNotesEvent.ToggleNoteSelection(note.note.id))
                                            }
                                        )
                                    }
                                }

                                if (otherNotes.isNotEmpty()) {
                                    if (pinnedNotes.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "OTHERS",
                                                modifier = Modifier.padding(8.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    items(otherNotes, key = { it.note.id }) { note ->
                                        val isExpanded = state.expandedNoteId == note.note.id
                                        NoteItem(
                                            modifier = Modifier.graphicsLayer { alpha = if (isExpanded) 0f : 1f },
                                            note = note,
                                            isSelected = state.selectedNoteIds.contains(note.note.id),
                                            onNoteClick = {
                                                if (isSelectionModeActive) {
                                                    viewModel.onEvent(ProjectNotesEvent.ToggleNoteSelection(note.note.id))
                                                } else {
                                                    viewModel.onEvent(ProjectNotesEvent.ExpandNote(note.note.id))
                                                }
                                            },
                                            onNoteLongClick = {
                                                viewModel.onEvent(ProjectNotesEvent.ToggleNoteSelection(note.note.id))
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

        AnimatedVisibility(
            visible = state.expandedNoteId != null,
            enter = scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = scaleOut(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            AddEditNoteScreen(
                state = state.toNotesState(),
                onEvent = { viewModel.onEvent(it.toProjectNotesEvent()) },
                onDismiss = { viewModel.onEvent(ProjectNotesEvent.CollapseNote) },
                themeMode = themeMode,
                settingsRepository = settingsRepository,
                events = viewModel.events
            )
        }
    }
}