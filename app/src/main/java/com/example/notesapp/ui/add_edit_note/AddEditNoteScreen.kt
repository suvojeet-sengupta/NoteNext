
package com.example.notesapp.ui.add_edit_note

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notesapp.di.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    factory: ViewModelFactory,
    onNoteSaved: () -> Unit
) {
    val viewModel: AddEditNoteViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isNoteSaved) {
        if (state.isNoteSaved) {
            onNoteSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNewNote) "Add Note" else "Edit Note") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(AddEditNoteEvent.OnSaveNoteClick) }) {
                Icon(imageVector = Icons.Default.Save, contentDescription = "Save note")
            }
        }
    ) { padding ->
        AnimatedVisibility(
            visible = true, // We can control visibility here
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { viewModel.onEvent(AddEditNoteEvent.OnTitleChange(it)) },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.content,
                    onValueChange = { viewModel.onEvent(AddEditNoteEvent.OnContentChange(it)) },
                    label = { Text("Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                Text(
                    text = "${state.content.length} characters",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
