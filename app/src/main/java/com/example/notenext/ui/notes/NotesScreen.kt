package com.example.notenext.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notenext.data.Note
import com.example.notenext.dependency_injection.ViewModelFactory
import com.example.notenext.ui.add_edit_note.AddEditNoteScreen
import com.example.notenext.ui.components.ContextualTopAppBar
import com.example.notenext.ui.components.LabelDialog
import com.example.notenext.ui.components.NoteItem
import com.example.notenext.ui.components.SearchBar
import com.example.notenext.ui.settings.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    factory: ViewModelFactory,
    onSettingsClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onEditLabelsClick: () -> Unit,
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "LABELS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = {
                            scope.launch { drawerState.close() }
                            onEditLabelsClick()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Labels",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "All Notes") },
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
                            icon = { Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = label) },
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
                    Column {
                        Spacer(modifier = Modifier.height(48.dp))
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
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                                SearchBar(
                                    searchQuery = searchQuery,
                                    isSearchActive = isSearchActive,
                                    onSearchQueryChange = { searchQuery = it },
                                    onSearchActiveChange = { isSearchActive = it },
                                    onLayoutToggleClick = { /*TODO*/ },
                                    onSortClick = { /*TODO*/ }
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






