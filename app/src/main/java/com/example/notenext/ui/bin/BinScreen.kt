package com.example.notenext.ui.bin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.notenext.R
import com.example.notenext.ui.components.NoteItem
import com.example.notenext.ui.notes.NotesEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinScreen(
    viewModel: BinViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.bin_title)) },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(BinEvent.EmptyBin) }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = stringResource(id = R.string.empty_bin))
                    }
                }
            )
        }
    ) {
        paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.notes.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.bin_empty_message),
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.notes) { note ->
                        NoteItem(
                            note = note,
                            onNoteClick = { /* No-op for bin */ },
                            onNoteLongClick = { /* No-op for bin */ },
                            isSelected = false
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { viewModel.onEvent(BinEvent.RestoreNote(note)) }) {
                                Icon(Icons.Default.Restore, contentDescription = stringResource(id = R.string.restore_note))
                            }
                            IconButton(onClick = { viewModel.onEvent(BinEvent.DeleteNotePermanently(note)) }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = stringResource(id = R.string.delete_permanently))
                            }
                        }
                    }
                }
            }
        }
    }
}
