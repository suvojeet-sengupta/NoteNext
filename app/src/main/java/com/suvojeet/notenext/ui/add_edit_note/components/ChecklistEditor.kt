package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
        val uncheckedItems = state.editingChecklist.filter { !it.isChecked }
        val checkedItems = state.editingChecklist.filter { it.isChecked }

        items(uncheckedItems, key = { it.id }) { item ->
            val focusRequester = remember { FocusRequester() }
            AnimatedVisibility(
                visible = true,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
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
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = { Text("List item") }
                    )
                    IconButton(onClick = { onEvent(NotesEvent.DeleteChecklistItem(item.id)) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete item")
                    }
                }
            }
            LaunchedEffect(state.newlyAddedChecklistItemId) {
                if (item.id == state.newlyAddedChecklistItemId) {
                    focusRequester.requestFocus()
                    onEvent(NotesEvent.ClearNewlyAddedChecklistItemId)
                }
            }
        }

        item {
            TextButton(onClick = { onEvent(NotesEvent.AddChecklistItem) }) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
                Text("Add item")
            }
        }

        items(checkedItems, key = { it.id }) { item ->
            val focusRequester = remember { FocusRequester() }
            AnimatedVisibility(
                visible = true,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
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
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = { Text("List item") }
                    )
                    IconButton(onClick = { onEvent(NotesEvent.DeleteChecklistItem(item.id)) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete item")
                    }
                }
            }
            LaunchedEffect(state.newlyAddedChecklistItemId) {
                if (item.id == state.newlyAddedChecklistItemId) {
                    focusRequester.requestFocus()
                    onEvent(NotesEvent.ClearNewlyAddedChecklistItemId)
                }
            }
        }
    }
}