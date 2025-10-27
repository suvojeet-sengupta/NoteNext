package com.suvojeet.notenext.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.dependency_injection.ViewModelFactory
import com.suvojeet.notenext.ui.add_edit_note.AddEditNoteScreen
import com.suvojeet.notenext.ui.components.ContextualTopAppBar
import com.suvojeet.notenext.ui.components.LabelDialog
import com.suvojeet.notenext.ui.components.NoteItem
import com.suvojeet.notenext.ui.components.MultiActionFab
import com.suvojeet.notenext.ui.components.SearchBar
import com.suvojeet.notenext.ui.settings.ThemeMode
import com.suvojeet.notenext.ui.settings.SettingsRepository
import com.suvojeet.notenext.ui.notes.LayoutType
import com.suvojeet.notenext.ui.notes.SortType
import com.suvojeet.notenext.ui.components.SearchTopAppBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
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
    events: kotlinx.coroutines.flow.SharedFlow<com.suvojeet.notenext.ui.notes.NotesUiEvent>
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val isSelectionModeActive = state.selectedNoteIds.isNotEmpty()
    var showLabelDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

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

                is NotesUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is NotesUiEvent.LinkPreviewRemoved -> {
                    Toast.makeText(context, "Link preview removed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var showSortMenu by remember { mutableStateOf(false) }

    BackHandler(enabled = isSearchActive || isSelectionModeActive || state.expandedNoteId != null) {
        when {
            isSelectionModeActive -> viewModel.onEvent(NotesEvent.ClearSelection)
            isSearchActive -> {
                isSearchActive = false
                searchQuery = ""
            }
            state.expandedNoteId != null -> viewModel.onEvent(NotesEvent.CollapseNote)
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
                            onClearSelection = { viewModel.onEvent(NotesEvent.ClearSelection) },
                            onTogglePinClick = { viewModel.onEvent(NotesEvent.TogglePinForSelectedNotes) },
                            onReminderClick = { viewModel.onEvent(NotesEvent.SetReminderForSelectedNotes(null)) }, // Placeholder
                            onColorClick = { /* TODO */ },
                            onArchiveClick = { viewModel.onEvent(NotesEvent.ArchiveSelectedNotes) },
                            onDeleteClick = { showDeleteDialog = true },
                            onCopyClick = { viewModel.onEvent(NotesEvent.CopySelectedNotes) },
                            onSendClick = { viewModel.onEvent(NotesEvent.SendSelectedNotes) },
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
                                    title = {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            SearchBar(
                                                onSearchActiveChange = { isSearchActive = it },
                                                onLayoutToggleClick = { viewModel.onEvent(NotesEvent.ToggleLayout) },
                                                onSortClick = { showSortMenu = true },
                                                layoutType = state.layoutType,
                                                sortMenuExpanded = showSortMenu,
                                                onSortMenuDismissRequest = { showSortMenu = false },
                                                onSortOptionClick = {
                                                    sortType -> viewModel.onEvent(NotesEvent.SortNotes(sortType))
                                                }
                                            )
                                        }
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = onMenuClick) {
                                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                                        }
                                    },
                                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                MultiActionFab(
                    onNoteClick = { viewModel.onEvent(NotesEvent.ExpandNote(-1)) },
                    onChecklistClick = { viewModel.onEvent(NotesEvent.ExpandNote(-1, "CHECKLIST")) }
                )
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
                                viewModel.onEvent(NotesEvent.DeleteSelectedNotes)
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
                        viewModel.onEvent(NotesEvent.SetLabelForSelectedNotes(label))
                        showLabelDialog = false
                    }
                )
            }
            Column(modifier = Modifier.padding(padding)) {
                Spacer(modifier = Modifier.height(8.dp))
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
                                    items(pinnedNotes, key = { it.id }) { note ->
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
                                        item {
                                            Text(
                                                text = "OTHERS",
                                                modifier = Modifier.padding(8.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    items(otherNotes, key = { it.id }) { note ->
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
                themeMode = themeMode,
                settingsRepository = settingsRepository,
                events = viewModel.events
            )
        }
    }
}






