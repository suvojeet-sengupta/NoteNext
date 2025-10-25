package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState

@Composable
fun ChecklistEditor(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        items(state.editingChecklist) { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { isChecked ->
                        onEvent(NotesEvent.OnChecklistItemCheckedChange(item.id, isChecked))
                    }
                )
                OutlinedTextField(
                    value = item.text,
                    onValueChange = { text ->
                        onEvent(NotesEvent.OnChecklistItemTextChange(item.id, text))
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("List item") }
                )
                IconButton(onClick = { onEvent(NotesEvent.DeleteChecklistItem(item.id)) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete item")
                }
            }
        }
        item {
            TextButton(onClick = { onEvent(NotesEvent.AddChecklistItem) }) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
                Text("Add item")
            }
        }
    }
}