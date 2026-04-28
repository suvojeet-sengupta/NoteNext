@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
package com.suvojeet.notenext.ui.project

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as StaggeredGridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.ui.add_edit_note.AddEditNoteScreen
import com.suvojeet.notenext.ui.components.*
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.ui.notes.LayoutType
import com.suvojeet.notenext.core.util.SortType
import com.suvojeet.notenext.ui.reminder.ReminderSetDialog
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.theme.ThemeMode
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import com.suvojeet.notenext.util.findActivity
import com.suvojeet.notenext.ui.add_edit_note.components.AiSummarySheet
import com.suvojeet.notenext.todo.TodoItemCard

import androidx.navigation.NavController
import com.suvojeet.notenext.navigation.Destination

@Composable
fun ProjectNotesScreen(
    navController: NavController,
    onBackClick: () -> Unit,
    themeMode: ThemeMode,
    settingsRepository: SettingsRepository,
    isDecoySession: Boolean = false
) {
    val viewModel: ProjectNotesViewModel = hiltViewModel()
    
    LaunchedEffect(isDecoySession) {
        viewModel.setDecoyMode(isDecoySession)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val isSelectionModeActive = state.selectedNoteIds.isNotEmpty()
    var showLabelDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReminderSetDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context.findActivity() as? androidx.fragment.app.FragmentActivity
    val biometricAuthManager = if (activity != null) {
        remember(activity) {
            com.suvojeet.notenext.util.BiometricAuthManager(context, activity)
        }
    } else {
        null
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProjectNotesUiEvent.SendNotes -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, event.title)
                        putExtra(Intent.EXTRA_TEXT, event.content)
                    }
                    val chooser = Intent.createChooser(intent, context.getString(R.string.send_notes_via))
                    context.startActivity(chooser)
                }
                is ProjectNotesUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is ProjectNotesUiEvent.LinkPreviewRemoved -> {
                    Toast.makeText(context, context.getString(R.string.link_preview_removed), Toast.LENGTH_SHORT).show()
                }
                is ProjectNotesUiEvent.NavigateToNoteByTitle -> {
                    val foundNoteId = viewModel.getNoteIdByTitle(event.title)
                    if (foundNoteId != null) {
                        viewModel.onEvent(ProjectNotesEvent.ExpandNote(noteId = foundNoteId))
                    } else {
                        Toast.makeText(context, "Note \"${event.title}\" not found", Toast.LENGTH_SHORT).show()
                    }
                }
                is ProjectNotesUiEvent.ScrollToSearchResult -> {}
            }
        }
    }

    BackHandler(enabled = isSearchActive || isSelectionModeActive || state.expandedNoteId != null) {
        when {
            isSearchActive -> {
                isSearchActive = false
                focusManager.clearFocus()
            }
            isSelectionModeActive -> viewModel.onEvent(ProjectNotesEvent.ClearSelection)
            state.expandedNoteId != null -> viewModel.onEvent(ProjectNotesEvent.CollapseNote)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SharedTransitionLayout {
            AnimatedContent(
                targetState = state.expandedNoteId,
                label = "NoteTransition",
                transitionSpec = {
                    val springSpec = spring<Float>(dampingRatio = 0.8f, stiffness = 300f)
                    if (targetState != null) {
                        (fadeIn(spring()) + scaleIn(initialScale = 0.85f, animationSpec = springSpec))
                            .togetherWith(fadeOut(spring()))
                    } else {
                        fadeIn(spring())
                            .togetherWith(fadeOut(spring()) + scaleOut(targetScale = 0.85f, animationSpec = springSpec))
                    }
                }
            ) { expandedId ->
                if (expandedId == null) {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            AnimatedContent(
                                targetState = isSelectionModeActive,
                                transitionSpec = {
                                    fadeIn(animationSpec = spring()).togetherWith(fadeOut(animationSpec = spring()))
                                },
                                label = "TopAppBar Animation"
                            ) { targetState ->
                                if (targetState) {
                                    val isAllPinned = state.selectedNoteIds.all { id ->
                                        state.notes.any { it.note.id == id && it.note.isPinned }
                                    }
                                    ContextualTopAppBar(
                                        selectedItemCount = state.selectedNoteIds.size,
                                        isPinned = isAllPinned,
                                        onClearSelection = { viewModel.onEvent(ProjectNotesEvent.ClearSelection) },
                                        onTogglePinClick = { viewModel.onEvent(ProjectNotesEvent.TogglePinForSelectedNotes) },
                                        onReminderClick = { showReminderSetDialog = true },
                                        onColorClick = { showColorPickerDialog = true },
                                        onArchiveClick = { viewModel.onEvent(ProjectNotesEvent.ArchiveSelectedNotes) },
                                        onDeleteClick = { showDeleteDialog = true },
                                        onCopyClick = { viewModel.onEvent(ProjectNotesEvent.CopySelectedNotes) },
                                        onSendClick = { viewModel.onEvent(ProjectNotesEvent.SendSelectedNotes) },
                                        onLabelClick = { showLabelDialog = true },
                                        onMoveToProjectClick = { },
                                        onLockClick = {
                                            val selectedNotes = state.notes.filter { state.selectedNoteIds.contains(it.note.id) }
                                            val isAnyNoteLocked = selectedNotes.any { it.note.isLocked }
                                            if (isAnyNoteLocked) {
                                                biometricAuthManager?.showBiometricPrompt(
                                                    onAuthSuccess = { viewModel.onEvent(ProjectNotesEvent.ToggleLockForSelectedNotes) },
                                                    onAuthError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                                )
                                            } else {
                                                viewModel.onEvent(ProjectNotesEvent.ToggleLockForSelectedNotes)
                                            }
                                        },
                                        onSelectAllClick = { viewModel.onEvent(ProjectNotesEvent.SelectAllNotes) }
                                    )
                                } else {
                                    TopAppBar(
                                        title = {
                                            SearchBar(
                                                searchQuery = searchQuery,
                                                onSearchQueryChange = { searchQuery = it },
                                                isSearchActive = isSearchActive,
                                                onSearchActiveChange = { isSearchActive = it },
                                                onLayoutToggleClick = { viewModel.onEvent(ProjectNotesEvent.ToggleLayout) },
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
                                                    viewModel.onEvent(ProjectNotesEvent.SortNotes(newSortType))
                                                },
                                                currentSortType = state.sortType
                                            )
                                        },
                                        navigationIcon = {
                                            IconButton(onClick = onBackClick, modifier = Modifier.springPress()) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                    )
                                }
                            }
                        },
                        floatingActionButton = {
                            var isFabExpanded by remember { mutableStateOf(false) }
                            MultiActionFab(
                                isExpanded = isFabExpanded,
                                onExpandedChange = { isFabExpanded = it },
                                onNoteClick = {
                                    viewModel.onEvent(ProjectNotesEvent.ExpandNote(-1))
                                    isFabExpanded = false
                                },
                                onChecklistClick = {
                                    viewModel.onEvent(ProjectNotesEvent.ExpandNote(-1, NoteType.CHECKLIST))
                                    isFabExpanded = false
                                },
                                onProjectClick = { },
                                showProjectButton = false,
                                themeMode = themeMode
                            )
                        }
                    ) { padding ->
                        val autoDeleteDays by settingsRepository.autoDeleteDays.collectAsStateWithLifecycle(initialValue = 7)
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                shape = MaterialTheme.shapes.extraLarge,
                                title = { Text(stringResource(id = R.string.move_to_bin_question)) },
                                text = { Text(stringResource(id = R.string.move_to_bin_message, autoDeleteDays)) },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.onEvent(ProjectNotesEvent.DeleteSelectedNotes)
                                            showDeleteDialog = false
                                        },
                                        modifier = Modifier.springPress()
                                    ) {
                                        Text(stringResource(id = R.string.move_to_bin), fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }, modifier = Modifier.springPress()) {
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

                        if (showColorPickerDialog) {
                            val colorPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                            ModalBottomSheet(
                                onDismissRequest = { showColorPickerDialog = false },
                                sheetState = colorPickerSheetState,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                dragHandle = { BottomSheetDefaults.DragHandle() }
                            ) {
                                val systemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
                                val isDarkTheme = when (themeMode) {
                                    ThemeMode.DARK, ThemeMode.AMOLED -> true
                                    ThemeMode.SYSTEM -> systemInDarkTheme
                                    else -> false
                                }
                                val colors = com.suvojeet.notenext.ui.theme.NoteGradients.getNoteColors(isDarkTheme)
                                Text(
                                    text = "Change Color",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                com.suvojeet.notenext.ui.add_edit_note.components.ColorPicker(
                                    colors = colors,
                                    editingColor = 0, // Not used for multiple notes
                                    onEvent = { event ->
                                        if (event is com.suvojeet.notenext.ui.notes.NotesEvent.OnColorChange) {
                                            viewModel.onEvent(ProjectNotesEvent.ChangeColorForSelectedNotes(event.color))
                                            showColorPickerDialog = false
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(48.dp))
                            }
                        }

                        Column(modifier = Modifier.padding(padding)) {
                            Spacer(modifier = Modifier.height(8.dp))

                            var showEditDescriptionDialog by remember { mutableStateOf(false) }
                            var editingDescription by remember(state.projectDescription) { mutableStateOf(state.projectDescription ?: "") }

                            if (showEditDescriptionDialog) {
                                AlertDialog(
                                    onDismissRequest = { showEditDescriptionDialog = false },
                                    shape = MaterialTheme.shapes.extraLarge,
                                    title = { Text(stringResource(id = R.string.edit_description), fontWeight = FontWeight.Bold) },
                                    text = {
                                        OutlinedTextField(
                                            value = editingDescription,
                                            onValueChange = { editingDescription = it },
                                            label = { Text(stringResource(id = R.string.project_description)) },
                                            singleLine = false,
                                            maxLines = 5,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = MaterialTheme.shapes.extraSmall
                                        )
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            viewModel.onEvent(ProjectNotesEvent.UpdateProjectDescription(editingDescription.ifBlank { null }))
                                            showEditDescriptionDialog = false
                                        }, modifier = Modifier.springPress()) {
                                            Text(stringResource(id = R.string.save), fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showEditDescriptionDialog = false }, modifier = Modifier.springPress()) {
                                            Text(stringResource(id = R.string.cancel))
                                        }
                                    }
                                )
                            }

                            ExpressiveSection(
                                title = "Workspace Info",
                                description = "Manage details about this project"
                            ) {
                                Card(
                                    onClick = { showEditDescriptionDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .springPress(),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Description",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            state.projectDescription?.let { description ->
                                                Text(
                                                    text = description,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 5,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            } ?: Text(
                                                text = stringResource(id = R.string.project_description),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        }
                                        IconButton(onClick = { showEditDescriptionDialog = true }, modifier = Modifier.springPress()) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = stringResource(id = R.string.edit_description),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                if (state.notes.isEmpty() && state.todos.isEmpty()) {
                                    EmptyState(
                                        icon = Icons.Default.Info,
                                        message = stringResource(id = R.string.no_notes_yet)
                                    )
                                } else {
                                    val filteredNotes = state.notes.filter { note ->
                                        !note.note.isArchived && (note.note.title.contains(searchQuery, ignoreCase = true) || note.note.content.contains(searchQuery, ignoreCase = true))
                                    }
                                    val filteredTodos = state.todos.filter { todoWithSubtasks ->
                                        todoWithSubtasks.todo.title.contains(searchQuery, ignoreCase = true) || todoWithSubtasks.todo.description.contains(searchQuery, ignoreCase = true)
                                    }

                                    val pinnedNotes = filteredNotes.filter { it.note.isPinned }
                                    val otherNotes = filteredNotes.filter { !it.note.isPinned }

                                    when (state.layoutType) {
                                        LayoutType.GRID -> {
                                            LazyVerticalStaggeredGrid(
                                                columns = StaggeredGridCells.Fixed(2),
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalItemSpacing = 12.dp
                                            ) {
                                                if (filteredTodos.isNotEmpty()) {
                                                    item(span = StaggeredGridItemSpan.FullLine) {
                                                        Text(
                                                            text = "Todos",
                                                            modifier = Modifier.padding(8.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    StaggeredGridItems(
                                                        filteredTodos,
                                                        key = { "todo-${it.todo.id}" }
                                                    ) { todoWithSubtasks ->
                                                        com.suvojeet.notenext.todo.TodoItemCard(
                                                            todoWithSubtasks = todoWithSubtasks,
                                                            onToggleComplete = { viewModel.onEvent(ProjectNotesEvent.ToggleTodoComplete(todoWithSubtasks.todo)) },
                                                            onClick = { }, // For now, no edit from project screen or we can implement it
                                                            onDelete = { viewModel.onEvent(ProjectNotesEvent.DeleteTodo(todoWithSubtasks.todo)) },
                                                            onConvertToNote = { /* Implement if needed */ },
                                                            onShare = { 
                                                                viewModel.onEvent(ProjectNotesEvent.ShareTodo(todoWithSubtasks))
                                                            }
                                                        )
                                                    }
                                                }

                                                if (pinnedNotes.isNotEmpty()) {
                                                    item(span = StaggeredGridItemSpan.FullLine) {
                                                        Text(
                                                            text = stringResource(id = R.string.pinned),
                                                            modifier = Modifier.padding(8.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    StaggeredGridItems(
                                                        pinnedNotes,
                                                        key = { it.note.id },
                                                        contentType = { it.note.noteType }
                                                    ) { note ->
                                                        NoteItem(
                                                            modifier = Modifier
                                                                .animateItem()
                                                                .sharedElement(
                                                                    rememberSharedContentState(key = "note-${note.note.id}"),
                                                                    animatedVisibilityScope = this@AnimatedContent
                                                                ),
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
                                                                text = stringResource(id = R.string.others),
                                                                modifier = Modifier.padding(8.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    StaggeredGridItems(
                                                        otherNotes,
                                                        key = { it.note.id },
                                                        contentType = { it.note.noteType }
                                                    ) { note ->
                                                        NoteItem(
                                                            modifier = Modifier
                                                                .animateItem()
                                                                .sharedElement(
                                                                    rememberSharedContentState(key = "note-${note.note.id}"),
                                                                    animatedVisibilityScope = this@AnimatedContent
                                                                ),
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
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                if (filteredTodos.isNotEmpty()) {
                                                    item {
                                                        Text(
                                                            text = "Todos",
                                                            modifier = Modifier.padding(8.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    items(filteredTodos, key = { "todo-${it.todo.id}" }) { todoWithSubtasks ->
                                                        com.suvojeet.notenext.todo.TodoItemCard(
                                                            todoWithSubtasks = todoWithSubtasks,
                                                            onToggleComplete = { viewModel.onEvent(ProjectNotesEvent.ToggleTodoComplete(todoWithSubtasks.todo)) },
                                                            onClick = { },
                                                            onDelete = { viewModel.onEvent(ProjectNotesEvent.DeleteTodo(todoWithSubtasks.todo)) },
                                                            onConvertToNote = { },
                                                            onShare = {
                                                                viewModel.onEvent(ProjectNotesEvent.ShareTodo(todoWithSubtasks))
                                                            }
                                                        )
                                                    }
                                                }

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
                                                        NoteItem(
                                                            modifier = Modifier
                                                                .animateItem()
                                                                .sharedElement(
                                                                    rememberSharedContentState(key = "note-${note.note.id}"),
                                                                    animatedVisibilityScope = this@AnimatedContent
                                                                ),
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
                                                                text = stringResource(id = R.string.others),
                                                                modifier = Modifier.padding(8.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    items(otherNotes, key = { it.note.id }) { note ->
                                                        NoteItem(
                                                            modifier = Modifier
                                                                .animateItem()
                                                                .sharedElement(
                                                                    rememberSharedContentState(key = "note-${note.note.id}"),
                                                                    animatedVisibilityScope = this@AnimatedContent
                                                                ),
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
                    }
                } else {
                    AddEditNoteScreen(
                        state = state.toNotesEditState(),
                        onEvent = { viewModel.onEvent(it.toProjectNotesEvent()) },
                        onDismiss = { viewModel.onEvent(ProjectNotesEvent.CollapseNote) },
                        themeMode = themeMode,
                        settingsRepository = settingsRepository,
                        events = viewModel.events.map { it.toNotesUiEvent() }.shareIn(rememberCoroutineScope(), SharingStarted.WhileSubscribed()),
                        modifier = Modifier.sharedElement(
                            rememberSharedContentState(key = "note-${expandedId}"),
                            animatedVisibilityScope = this@AnimatedContent
                        )
                    )
                }
            }
        }

        if (state.showSummaryDialog) {
            AiSummarySheet(
                summary = state.summaryResult,
                isSummarizing = state.isSummarizing,
                onDismiss = { viewModel.onEvent(ProjectNotesEvent.ClearSummary) },
                onClearSummary = { viewModel.onEvent(ProjectNotesEvent.ClearSummary) }
            )
        }
    }
}
