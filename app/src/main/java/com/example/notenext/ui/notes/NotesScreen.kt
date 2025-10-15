package com.example.notenext.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notenext.data.Note
import com.example.notenext.dependency_injection.ViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun NotesScreen(
    factory: ViewModelFactory,
    onNoteClick: (Int) -> Unit,
    onAddNoteClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val viewModel: NotesViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val isSelectionModeActive = state.selectedNoteIds.isNotEmpty()

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

    BackHandler(enabled = isSearchActive || isSelectionModeActive || drawerState.isOpen) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (isSelectionModeActive) {
            viewModel.onEvent(NotesEvent.ClearSelection)
        } else if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        }
    }

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
                Text("Settings", modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        scope.launch { drawerState.close() }
                        onSettingsClick()
                    }
                    .padding(16.dp)
                )
                // Future items can be added here
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
                        onImportantClick = { viewModel.onEvent(NotesEvent.ToggleImportantForSelectedNotes) },
                        onReminderClick = { viewModel.onEvent(NotesEvent.SetReminderForSelectedNotes(null)) }, // Placeholder
                        onColorClick = { /* TODO */ },
                        onArchiveClick = { viewModel.onEvent(NotesEvent.ArchiveSelectedNotes) },
                        onDeleteClick = { viewModel.onEvent(NotesEvent.DeleteSelectedNotes) },
                        onCopyClick = { viewModel.onEvent(NotesEvent.CopySelectedNotes) },
                        onSendClick = { viewModel.onEvent(NotesEvent.SendSelectedNotes) }
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
                    onClick = onAddNoteClick,
                    content = {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add note")
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .animateContentSize(animationSpec = tween(300)),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(state.notes.filter { it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true) }) { note ->
                            NoteItem(
                                note = note,
                                isSelected = state.selectedNoteIds.contains(note.id),
                                onNoteClick = {
                                    if (isSelectionModeActive) {
                                        viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.id))
                                    } else {
                                        onNoteClick(note.id)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    note: Note,
    isSelected: Boolean,
    onNoteClick: () -> Unit,
    onNoteLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(12.dp)
            .combinedClickable(
                onClick = onNoteClick,
                onLongClick = onNoteLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color(note.color)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.isPinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (note.isImportant) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Important",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (note.isArchived) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Archived",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            Text(text = note.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                modifier = Modifier.animateContentSize(animationSpec = tween(300))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextualTopAppBar(
    selectedItemCount: Int,
    onClearSelection: () -> Unit,
    onTogglePinClick: () -> Unit,
    onImportantClick: () -> Unit,
    onReminderClick: () -> Unit,
    onColorClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onSendClick: () -> Unit
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
            IconButton(onClick = onImportantClick) {
                Icon(Icons.Default.Star, contentDescription = "Mark as important")
            }
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
                    text = { Text("Send") },
                    onClick = {
                        onSendClick()
                        showMenu = false
                    }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsedTopAppBar(onSearchClick: () -> Unit, onMenuClick: () -> Unit) {
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
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
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
