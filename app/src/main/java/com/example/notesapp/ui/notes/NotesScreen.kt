
package com.example.notesapp.ui.notes

import androidx.compose.material3.OutlinedTextFieldDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    factory: ViewModelFactory,
    onNoteClick: (Int) -> Unit,
    onAddNoteClick: () -> Unit
) {
    val viewModel: NotesViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SearchAppBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onAddNoteClick = onAddNoteClick
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
                    items(state.notes.filter { it.title.contains(searchQuery, ignoreCase = true) }) { note ->
                        NoteItem(
                            note = note,
                            onNoteClick = { onNoteClick(note.id) },
                            onDeleteClick = { viewModel.onEvent(NotesEvent.DeleteNote(note)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoteItem(
    note: Note,
    onNoteClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onNoteClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(note.color))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = note.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = note.content, style = MaterialTheme.typography.bodyMedium, maxLines = 5)
            Spacer(modifier = Modifier.height(8.dp))
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete note")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddNoteClick: () -> Unit
) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search your notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        actions = {
            IconButton(onClick = onAddNoteClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add note")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
