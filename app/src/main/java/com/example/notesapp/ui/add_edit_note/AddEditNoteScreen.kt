
package com.example.notesapp.ui.add_edit_note

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(AddEditNoteEvent.OnSaveNoteClick) }) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Save note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
        }
    }
}
