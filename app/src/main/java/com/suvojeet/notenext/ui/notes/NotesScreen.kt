package com.suvojeet.notenext.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as StaggeredGridItems
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.suvojeet.notenext.ui.add_edit_note.AddEditNoteScreen
import com.suvojeet.notenext.ui.components.ContextualTopAppBar
import com.suvojeet.notenext.ui.components.EmptyState
import com.suvojeet.notenext.ui.components.LabelDialog
import com.suvojeet.notenext.ui.components.NoteItem
import com.suvojeet.notenext.ui.components.MultiActionFab
import com.suvojeet.notenext.ui.components.SearchBar
import com.suvojeet.notenext.ui.components.ColorSelectionDialog
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.notes.LayoutType
import com.suvojeet.notenext.data.SortType
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

import com.suvojeet.notenext.ui.reminder.ReminderSetDialog
import com.suvojeet.notenext.util.findActivity
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onSettingsClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onEditLabelsClick: () -> Unit,
    onBinClick: () -> Unit,
    themeMode: ThemeMode,
    settingsRepository: SettingsRepository,
    onMenuClick: () -> Unit,
    events: SharedFlow<NotesUiEvent>
) {
    val state by viewModel.state.collectAsState()
    var isFabExpanded by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    val isSelectionModeActive = state.selectedNoteIds.isNotEmpty()
    var showLabelDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReminderSetDialog by remember { mutableStateOf(false) }
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showMoveToProjectDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val currentVersion = 10 // Matches app/build.gradle.kts versionCode
        settingsRepository.lastSeenVersion.collect { lastSeen ->
            if (currentVersion > lastSeen) {
                showWhatsNewDialog = true
                settingsRepository.saveLastSeenVersion(currentVersion)
            }
        }
    }

    val activity = context.findActivity() as? androidx.fragment.app.FragmentActivity
    val biometricAuthManager = if (activity != null) {
        remember(activity) {
            com.suvojeet.notenext.util.BiometricAuthManager(context, activity)
        }
    } else {
        null
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NotesUiEvent.SendNotes -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, event.title)
                        putExtra(Intent.EXTRA_TEXT, event.content)
                    }
                    val chooser = Intent.createChooser(intent, context.getString(R.string.send_notes_via))
                    context.startActivity(chooser)
                }

                is NotesUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is NotesUiEvent.LinkPreviewRemoved -> {
                    Toast.makeText(context, context.getString(R.string.link_preview_removed), Toast.LENGTH_SHORT).show()
                }
                is NotesUiEvent.ProjectCreated -> {
                    Toast.makeText(context, context.getString(R.string.project_created, event.projectName), Toast.LENGTH_SHORT).show()
                }
                is NotesUiEvent.NavigateToNoteByTitle -> {
                    val noteId = viewModel.getNoteIdByTitle(event.title)
                    if (noteId != null) {
                        viewModel.onEvent(NotesEvent.ExpandNote(noteId))
                    } else {
                        Toast.makeText(context, "Note \"${event.title}\" not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    var showSortMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchActive || isSelectionModeActive || state.expandedNoteId != null) {
        when {
            isSearchActive -> {
                isSearchActive = false
                focusManager.clearFocus()
            }
            isSelectionModeActive -> viewModel.onEvent(NotesEvent.ClearSelection)
            state.expandedNoteId != null -> viewModel.onEvent(NotesEvent.CollapseNote)
        }
    }

    val gridState = rememberLazyStaggeredGridState()
    val listState = rememberLazyListState()

    SharedTransitionLayout {
        AnimatedContent(
            targetState = state.expandedNoteId,
            label = "NoteTransition",
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            }
        ) { expandedId ->
            if (expandedId == null) {
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
                                        onClearSelection = { viewModel.onEvent(NotesEvent.ClearSelection) },
                                        onTogglePinClick = { viewModel.onEvent(NotesEvent.TogglePinForSelectedNotes) },
                                        onReminderClick = { showReminderSetDialog = true },
                                        onColorClick = { showColorPickerDialog = true },
                                        onArchiveClick = { viewModel.onEvent(NotesEvent.ArchiveSelectedNotes) },
                                        onDeleteClick = { showDeleteDialog = true },
                                        onCopyClick = { viewModel.onEvent(NotesEvent.CopySelectedNotes) },
                                        onSendClick = { viewModel.onEvent(NotesEvent.SendSelectedNotes) },
                                        onLabelClick = { showLabelDialog = true },
                                        onMoveToProjectClick = { showMoveToProjectDialog = true }
                                    )
                                } else {
                                    TopAppBar(
                                        title = {
                                            SearchBar(
                                                searchQuery = state.searchQuery,
                                                onSearchQueryChange = { viewModel.onEvent(NotesEvent.OnSearchQueryChange(it)) },
                                                isSearchActive = isSearchActive,
                                                onSearchActiveChange = { isSearchActive = it },
                                                onLayoutToggleClick = { viewModel.onEvent(NotesEvent.ToggleLayout) },
                                                onSortClick = { showSortMenu = true },
                                                layoutType = state.layoutType,
                                                sortMenuExpanded = showSortMenu,
                                                onSortMenuDismissRequest = { showSortMenu = false },
                                                onSortOptionClick = { sortType ->
                                                    val newSortType = if (sortType == state.sortType) {
                                                        SortType.DATE_MODIFIED
                                                    } else {
                                                        sortType
                                                    }
                                                    viewModel.onEvent(NotesEvent.SortNotes(newSortType))
                                                },
                                                currentSortType = state.sortType
                                            )
                                        },
                                        navigationIcon = {
                                            IconButton(onClick = onMenuClick) {
                                                Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
                                            }
                                        },
                                        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                                            containerColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        },
                        floatingActionButton = {
                            MultiActionFab(
                                isExpanded = isFabExpanded,
                                onExpandedChange = { isFabExpanded = it },
                                onNoteClick = {
                                    viewModel.onEvent(NotesEvent.ExpandNote(-1))
                                    isFabExpanded = false
                                },
                                onChecklistClick = {
                                    viewModel.onEvent(NotesEvent.ExpandNote(-1, "CHECKLIST"))
                                    isFabExpanded = false
                                },
                                onProjectClick = {
                                    showCreateProjectDialog = true
                                    isFabExpanded = false
                                },
                                themeMode = themeMode
                            )
                        }
                    ) { padding ->
                        val autoDeleteDays by settingsRepository.autoDeleteDays.collectAsState(initial = 7)
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text(stringResource(id = R.string.move_to_bin_question)) },
                                text = { Text(stringResource(id = R.string.move_to_bin_message, autoDeleteDays)) },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.onEvent(NotesEvent.DeleteSelectedNotes)
                                            showDeleteDialog = false
                                        }
                                    ) {
                                        Text(stringResource(id = R.string.move_to_bin))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) {
                                        Text(stringResource(id = R.string.cancel))
                                    }
                                }
                            )
                        }
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
                        if (showReminderSetDialog) {
                            ReminderSetDialog(
                                onDismissRequest = { showReminderSetDialog = false },
                                onConfirm = { date, time, repeatOption ->
                                    viewModel.onEvent(NotesEvent.SetReminderForSelectedNotes(date, time, repeatOption))
                                    showReminderSetDialog = false
                                }
                            )
                        }

                        if (showColorPickerDialog) {
                            ColorSelectionDialog(
                                onDismiss = { showColorPickerDialog = false },
                                onColorSelected = { color ->
                                    viewModel.onEvent(NotesEvent.ChangeColorForSelectedNotes(color))
                                    showColorPickerDialog = false
                                },
                                themeMode = themeMode
                            )
                        }

                        if (showCreateProjectDialog) {
                            CreateProjectDialog(
                                onDismiss = { showCreateProjectDialog = false },
                                onConfirm = { projectName ->
                                    viewModel.onEvent(NotesEvent.CreateProject(projectName))
                                    showCreateProjectDialog = false
                                }
                            )
                        }

                        if (showMoveToProjectDialog) {
                            MoveToProjectDialog(
                                projects = state.projects,
                                onDismiss = { showMoveToProjectDialog = false },
                                onConfirm = { projectId ->
                                    viewModel.onEvent(NotesEvent.MoveSelectedNotesToProject(projectId))
                                    showMoveToProjectDialog = false
                                }
                            )
                        }

                        if (showWhatsNewDialog) {
                            WhatsNewDialog(onDismiss = { showWhatsNewDialog = false })
                        }

                        Column(modifier = Modifier.padding(padding).clickable(
                            onClick = {
                                if (isFabExpanded) {
                                    isFabExpanded = false
                                }
                            },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val notesToDisplay = if (state.filteredLabel == null) {
                                state.notes
                            } else {
                                state.notes.filter { it.note.label == state.filteredLabel }
                            }

                            if (state.isLoading) {
                                when (state.layoutType) {
                                    LayoutType.GRID -> {
                                        LazyVerticalStaggeredGrid(
                                            columns = StaggeredGridCells.Fixed(2),
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalItemSpacing = 8.dp
                                        ) {
                                            items(10) {
                                                com.suvojeet.notenext.ui.components.SkeletonNoteItem()
                                            }
                                        }
                                    }
                                    LayoutType.LIST -> {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(10) {
                                                com.suvojeet.notenext.ui.components.SkeletonNoteItem()
                                            }
                                        }
                                    }
                                }
                            } else if (notesToDisplay.isEmpty()) {
                                val currentLabel = state.filteredLabel
                                val emptyMessage = if (currentLabel != null) {
                                    stringResource(id = R.string.no_notes_found_label, currentLabel)
                                } else if (state.searchQuery.isNotEmpty()) {
                                    stringResource(id = R.string.no_notes_found)
                                } else {
                                    stringResource(id = R.string.no_notes_yet)
                                }
                                
                                val emptyIcon = if (state.searchQuery.isNotEmpty()) Icons.Default.Search else Icons.Default.Note

                                EmptyState(
                                    icon = emptyIcon,
                                    message = emptyMessage,
                                    description = if (state.searchQuery.isEmpty()) stringResource(id = R.string.create_your_first_note) else null
                                )
                            } else {
                                val filteredNotes = notesToDisplay
                                val pinnedNotes = filteredNotes.filter { it.note.isPinned }
                                val otherNotes = filteredNotes.filter { !it.note.isPinned }

                                val onNoteClickAction: (com.suvojeet.notenext.data.NoteWithAttachments) -> Unit = { note ->
                                    if (isSelectionModeActive) {
                                        viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                    } else {
                                        if (note.note.isLocked) {
                                            biometricAuthManager?.showBiometricPrompt(
                                                onAuthSuccess = { viewModel.onEvent(NotesEvent.ExpandNote(note.note.id)) },
                                                onAuthError = { Toast.makeText(context, "Authentication Failed", Toast.LENGTH_SHORT).show() }
                                            ) ?: Toast.makeText(context, "Biometrics not available", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.onEvent(NotesEvent.ExpandNote(note.note.id))
                                        }
                                    }
                                }

                                when (state.layoutType) {
                                    LayoutType.GRID -> {
                                        LazyVerticalStaggeredGrid(
                                            columns = StaggeredGridCells.Fixed(2),
                                            modifier = Modifier.fillMaxSize(),
                                            state = gridState,
                                            contentPadding = PaddingValues(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalItemSpacing = 8.dp
                                        ) {
                                            if (pinnedNotes.isNotEmpty()) {
                                                item(span = StaggeredGridItemSpan.FullLine) {
                                                    Text(
                                                        text = stringResource(id = R.string.pinned),
                                                        modifier = Modifier.padding(8.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                StaggeredGridItems(pinnedNotes, key = { it.note.id }) { note ->
                                                    val noteModifier = Modifier.sharedElement(
                                                        rememberSharedContentState(key = "note-${note.note.id}"),
                                                        animatedVisibilityScope = this@AnimatedContent
                                                    )
                                                    NoteItem(
                                                        modifier = noteModifier,
                                                        note = note,
                                                        isSelected = state.selectedNoteIds.contains(note.note.id),
                                                        searchQuery = state.searchQuery,
                                                        onNoteClick = { onNoteClickAction(note) },
                                                        onNoteLongClick = {
                                                            viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                                        }
                                                    )
                                                }
                                            }

                                            if (otherNotes.isNotEmpty()) {
                                                if (pinnedNotes.isNotEmpty()) {
                                                    item(span = StaggeredGridItemSpan.FullLine) {
                                                        Text(
                                                            text = stringResource(id = R.string.others),
                                                            modifier = Modifier.padding(8.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                StaggeredGridItems(otherNotes, key = { it.note.id }) { note ->
                                                    val noteModifier = Modifier.sharedElement(
                                                        rememberSharedContentState(key = "note-${note.note.id}"),
                                                        animatedVisibilityScope = this@AnimatedContent
                                                    )
                                                    NoteItem(
                                                        modifier = noteModifier,
                                                        note = note,
                                                        isSelected = state.selectedNoteIds.contains(note.note.id),
                                                        searchQuery = state.searchQuery,
                                                        onNoteClick = { onNoteClickAction(note) },
                                                        onNoteLongClick = {
                                                            viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    LayoutType.LIST -> {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            state = listState,
                                            contentPadding = PaddingValues(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (pinnedNotes.isNotEmpty()) {
                                                item {
                                                    Text(
                                                        text = stringResource(id = R.string.pinned),
                                                        modifier = Modifier.padding(8.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                items(pinnedNotes, key = { it.note.id }) { note ->
                                                    val noteModifier = Modifier.sharedElement(
                                                        rememberSharedContentState(key = "note-${note.note.id}"),
                                                        animatedVisibilityScope = this@AnimatedContent
                                                    )
                                                    NoteItem(
                                                        modifier = noteModifier,
                                                        note = note,
                                                        isSelected = state.selectedNoteIds.contains(note.note.id),
                                                        searchQuery = state.searchQuery,
                                                        onNoteClick = { onNoteClickAction(note) },
                                                        onNoteLongClick = {
                                                            viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                                        }
                                                    )
                                                }
                                            }

                                            if (otherNotes.isNotEmpty()) {
                                                if (pinnedNotes.isNotEmpty()) {
                                                    item {
                                                        Text(
                                                            text = stringResource(id = R.string.others),
                                                            modifier = Modifier.padding(8.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                items(otherNotes, key = { it.note.id }) { note ->
                                                    val noteModifier = Modifier.sharedElement(
                                                        rememberSharedContentState(key = "note-${note.note.id}"),
                                                        animatedVisibilityScope = this@AnimatedContent
                                                    )
                                                    NoteItem(
                                                        modifier = noteModifier,
                                                        note = note,
                                                        isSelected = state.selectedNoteIds.contains(note.note.id),
                                                        searchQuery = state.searchQuery,
                                                        onNoteClick = { onNoteClickAction(note) },
                                                        onNoteLongClick = {
                                                            viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
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
            } else {
                 AddEditNoteScreen(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onDismiss = { viewModel.onEvent(NotesEvent.CollapseNote) },
                    themeMode = themeMode,
                    settingsRepository = settingsRepository,
                    events = viewModel.events,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "note-${expandedId}"),
                        animatedVisibilityScope = this@AnimatedContent
                    )
                )
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.create_new_project)) },
        text = {
            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text(stringResource(id = R.string.project_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(projectName) },
                enabled = projectName.isNotBlank()
            ) {
                Text(stringResource(id = R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun MoveToProjectDialog(
    projects: List<com.suvojeet.notenext.data.Project>,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    var selectedProject by remember { mutableStateOf<com.suvojeet.notenext.data.Project?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.move_to_project)) },
        text = {
            Column {
                projects.forEach { project ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProject = project }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedProject == project),
                            onClick = { selectedProject = project }
                        )
                        Text(text = project.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedProject = null }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedProject == null),
                        onClick = { selectedProject = null }
                    )
                    Text(text = stringResource(id = R.string.none_remove_from_project), modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedProject?.id) }
            ) {
                Text(stringResource(id = R.string.move))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun WhatsNewDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.whats_new),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.whats_new_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                val features = listOf(
                    R.string.whats_new_feature_1,
                    R.string.whats_new_feature_2,
                    R.string.whats_new_feature_3
                )
                
                features.forEach { featureRes ->
                    val fullText = stringResource(id = featureRes)
                    val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
                        // Very simple parser for <b>...</b> in these specific strings
                        val parts = fullText.split("<b>", "</b>")
                        parts.forEachIndexed { index, part ->
                            if (index % 2 == 1) {
                                withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) {
                                    append(part)
                                }
                            } else {
                                append(part)
                            }
                        }
                    }
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dismiss))
            }
        }
    )
}
