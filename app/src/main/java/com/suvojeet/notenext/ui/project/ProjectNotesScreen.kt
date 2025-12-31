
package com.suvojeet.notenext.ui.project



import androidx.activity.compose.BackHandler

import androidx.compose.animation.AnimatedContent

import androidx.compose.animation.AnimatedVisibility

import androidx.compose.animation.ExperimentalAnimationApi

import androidx.compose.animation.core.tween

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

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

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

import androidx.compose.material3.AlertDialog

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Scaffold

import androidx.compose.material3.Text

import androidx.compose.material3.TextButton

import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable

import androidx.compose.runtime.collectAsState

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

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.platform.LocalFocusManager

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import androidx.hilt.navigation.compose.hiltViewModel

import com.suvojeet.notenext.ui.add_edit_note.AddEditNoteScreen

import com.suvojeet.notenext.ui.components.ContextualTopAppBar

import com.suvojeet.notenext.ui.components.LabelDialog

import com.suvojeet.notenext.ui.components.NoteItem

import com.suvojeet.notenext.ui.components.SearchBar

import com.suvojeet.notenext.ui.notes.LayoutType

import com.suvojeet.notenext.data.SortType

import com.suvojeet.notenext.ui.reminder.ReminderSetDialog

import com.suvojeet.notenext.data.repository.SettingsRepository

import com.suvojeet.notenext.ui.theme.ThemeMode

import kotlinx.coroutines.flow.map

import kotlinx.coroutines.flow.shareIn

import androidx.compose.runtime.rememberCoroutineScope

import kotlinx.coroutines.flow.SharingStarted

import androidx.compose.material3.TopAppBarDefaults



import com.suvojeet.notenext.ui.components.MultiActionFab



@Composable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)

fun ProjectNotesScreen(

    onBackClick: () -> Unit,

    themeMode: ThemeMode,

    settingsRepository: SettingsRepository

) {

    val viewModel: ProjectNotesViewModel = hiltViewModel()

    val state by viewModel.state.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    var isSearchActive by remember { mutableStateOf(false) }

    val isSelectionModeActive = state.selectedNoteIds.isNotEmpty()

    var showLabelDialog by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    var showReminderSetDialog by remember { mutableStateOf(false) }

    var showSortMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

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

                            onLabelClick = { showLabelDialog = true },

                            onMoveToProjectClick = { /* Not applicable for project notes */ },
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

                                            SortType.DATE_MODIFIED // Revert to default if same option is clicked

                                        } else {

                                            sortType

                                        }

                                        viewModel.onEvent(ProjectNotesEvent.SortNotes(newSortType))

                                    },

                                    currentSortType = state.sortType

                                )

                            },

                            navigationIcon = {

                                IconButton(onClick = onBackClick) {

                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))

                                }

                            },

                            colors = TopAppBarDefaults.topAppBarColors(

                                containerColor = Color.Transparent

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

                        viewModel.onEvent(ProjectNotesEvent.ExpandNote(-1, "CHECKLIST"))

                        isFabExpanded = false

                    },

                    onProjectClick = { /* Do nothing */ },

                    showProjectButton = false,

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

                                viewModel.onEvent(ProjectNotesEvent.DeleteSelectedNotes)

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

                // Description Section
                var showEditDescriptionDialog by remember { mutableStateOf(false) }
                var editingDescription by remember(state.projectDescription) { mutableStateOf(state.projectDescription ?: "") }

                if (showEditDescriptionDialog) {
                    AlertDialog(
                        onDismissRequest = { showEditDescriptionDialog = false },
                        title = { Text(stringResource(id = R.string.edit_description)) },
                        text = {
                            androidx.compose.material3.OutlinedTextField(
                                value = editingDescription,
                                onValueChange = { editingDescription = it },
                                label = { Text(stringResource(id = R.string.project_description)) },
                                singleLine = false,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.onEvent(ProjectNotesEvent.UpdateProjectDescription(editingDescription.ifBlank { null }))
                                showEditDescriptionDialog = false
                            }) {
                                Text(stringResource(id = R.string.save))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditDescriptionDialog = false }) {
                                Text(stringResource(id = R.string.cancel))
                            }
                        }
                    )
                }

                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        state.projectDescription?.let { description ->
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        } ?: Text(
                            text = stringResource(id = R.string.project_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = { showEditDescriptionDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.edit_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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

                                text = stringResource(id = R.string.no_notes_yet),

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

                                            text = stringResource(id = R.string.pinned),

                                            modifier = Modifier.padding(8.dp),

                                            style = MaterialTheme.typography.labelSmall,

                                            color = MaterialTheme.colorScheme.onSurfaceVariant

                                        )

                                    }

                                    StaggeredGridItems(pinnedNotes, key = { it.note.id }) { note ->

                                        val isExpanded = state.expandedNoteId == note.note.id

                                        NoteItem(

                                            modifier = Modifier.graphicsLayer { alpha = if (isExpanded) 0f else 1f },

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

                                    StaggeredGridItems(otherNotes, key = { it.note.id }) { note ->

                                        val isExpanded = state.expandedNoteId == note.note.id

                                        NoteItem(

                                            modifier = Modifier.graphicsLayer { alpha = if (isExpanded) 0f else 1f },

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

                                            text = stringResource(id = R.string.pinned),

                                            modifier = Modifier.padding(8.dp),

                                            style = MaterialTheme.typography.labelSmall,

                                            color = MaterialTheme.colorScheme.onSurfaceVariant

                                        )

                                    }

                                    items(pinnedNotes, key = { it.note.id }) { note ->

                                        val isExpanded = state.expandedNoteId == note.note.id

                                        NoteItem(

                                            modifier = Modifier.graphicsLayer { alpha = if (isExpanded) 0f else 1f },

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

                                        val isExpanded = state.expandedNoteId == note.note.id

                                        NoteItem(

                                            modifier = Modifier.graphicsLayer { alpha = if (isExpanded) 0f else 1f },

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

                events = viewModel.events.map { it.toNotesUiEvent() }.shareIn(rememberCoroutineScope(), SharingStarted.WhileSubscribed())

            )

        }

    }

}
