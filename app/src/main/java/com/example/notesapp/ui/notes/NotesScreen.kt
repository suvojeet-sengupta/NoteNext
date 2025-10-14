package com.example.notesapp.ui.notes

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notesapp.data.Note
import com.example.notesapp.dependency_injection.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun NotesScreen(
    factory: ViewModelFactory,
    onNoteClick: (Int) -> Unit,
    onAddNoteClick: () -> Unit
) {
    val viewModel: NotesViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val isSelectionModeActive = state.selectedNoteIds.isNotEmpty()

    BackHandler(enabled = isSearchActive || isSelectionModeActive) {
        if (isSelectionModeActive) {
            viewModel.onEvent(NotesEvent.ClearSelection)
        } else if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionModeActive) {
                ContextualTopAppBar(
                    selectedItemCount = state.selectedNoteIds.size,
                    onClearSelection = { viewModel.onEvent(NotesEvent.ClearSelection) },
                    onPinClick = { viewModel.onEvent(NotesEvent.PinSelectedNotes) },
                    onDeleteClick = { viewModel.onEvent(NotesEvent.DeleteSelectedNotes) }
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
                            onSearchClick = { isSearchActive = true }
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
    onPinClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    TopAppBar(
        title = { Text(text = "$selectedItemCount selected") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Clear selection")
            }
        },
        actions = {
            IconButton(onClick = onPinClick) {
                Icon(Icons.Default.Star, contentDescription = "Pin note")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete selected notes")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsedTopAppBar(onSearchClick: () -> Unit) {
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
                    .focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
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