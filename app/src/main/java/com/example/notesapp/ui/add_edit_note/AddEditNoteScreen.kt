
package com.example.notesapp.ui.add_edit_note

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notesapp.di.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

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

    val colors = listOf(
        Color.White.toArgb(),
        Color.Red.toArgb(),
        Color.Green.toArgb(),
        Color.Blue.toArgb(),
        Color.Yellow.toArgb()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNewNote) "Add Note" else "Edit Note") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    if (!state.isNewNote) {
                        IconButton(onClick = { /* TODO: Implement delete */ }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete note")
                        }
                    }
                }
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
                    .background(Color(state.color))
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
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    items(colors) { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .border(
                                    width = 2.dp,
                                    color = if (state.color == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.onEvent(AddEditNoteEvent.OnColorChange(color)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (!state.isNewNote) {
                    Text(
                        text = "Last edited: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(state.lastEdited))}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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
