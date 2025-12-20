package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

/**
 * A composable for editing a checklist, allowing users to add, check/uncheck,
 * edit text, and delete checklist items. It also manages focus for newly added items.
 *
 * @param state The current [NotesState] containing the checklist data.
 * @param onEvent Lambda to dispatch [NotesEvent]s for checklist modifications.
 */
@Composable
fun ChecklistEditor(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // Separate unchecked and checked items for display order.
        val uncheckedItems = state.editingChecklist.filter { !it.isChecked }
        val checkedItems = state.editingChecklist.filter { it.isChecked }

        // Display unchecked items first.
        items(uncheckedItems, key = { it.id }) { item ->
            val focusRequester = remember { FocusRequester() }
            // Animated visibility for smooth item addition/removal.
            AnimatedVisibility(
                visible = true, // Always visible once in the list.
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween // Distribute items evenly.
                ) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { isChecked ->
                            onEvent(NotesEvent.OnChecklistItemCheckedChange(item.id, isChecked))
                        },
                        // Provide a content description for accessibility.
                        // The actual text of the item can be read by screen readers.
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    OutlinedTextField(
                        value = item.text,
                        onValueChange = { text ->
                            onEvent(NotesEvent.OnChecklistItemTextChange(item.id, text))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester), // Request focus for this item.
                        placeholder = { Text(stringResource(id = R.string.list_item)) },
                        singleLine = true // Ensure single line for checklist items.
                    )
                    
                    // Move Up Button
                    val visualIndex = uncheckedItems.indexOf(item)
                    IconButton(
                        onClick = { 
                            if (visualIndex > 0) {
                                val prevItem = uncheckedItems[visualIndex - 1]
                                val fromIndex = state.editingChecklist.indexOfFirst { it.id == item.id }
                                val toIndex = state.editingChecklist.indexOfFirst { it.id == prevItem.id }
                                onEvent(NotesEvent.SwapChecklistItems(fromIndex, toIndex))
                            }
                        },
                        enabled = visualIndex > 0
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(20.dp))
                    }

                    // Move Down Button
                    IconButton(
                        onClick = { 
                            if (visualIndex < uncheckedItems.size - 1) {
                                val nextItem = uncheckedItems[visualIndex + 1]
                                val fromIndex = state.editingChecklist.indexOfFirst { it.id == item.id }
                                val toIndex = state.editingChecklist.indexOfFirst { it.id == nextItem.id }
                                onEvent(NotesEvent.SwapChecklistItems(fromIndex, toIndex))
                            }
                        },
                        enabled = visualIndex < uncheckedItems.size - 1
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(20.dp))
                    }

                    IconButton(onClick = { onEvent(NotesEvent.DeleteChecklistItem(item.id)) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_item))
                    }
                }
            }
            // Request focus for newly added items.
            LaunchedEffect(state.newlyAddedChecklistItemId) {
                if (item.id == state.newlyAddedChecklistItemId) {
                    focusRequester.requestFocus()
                    onEvent(NotesEvent.ClearNewlyAddedChecklistItemId) // Clear the ID after focusing.
                }
            }
        }

        // Button to add a new checklist item.
        item {
            TextButton(onClick = { onEvent(NotesEvent.AddChecklistItem) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_item))
                Text(stringResource(id = R.string.add_item))
            }
        }

        // Display checked items after the "Add Item" button.
        items(checkedItems, key = { it.id }) { item ->
            val focusRequester = remember { FocusRequester() }
            AnimatedVisibility(
                visible = true, // Always visible once in the list.
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween // Distribute items evenly.
                ) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { isChecked ->
                            onEvent(NotesEvent.OnChecklistItemCheckedChange(item.id, isChecked))
                        },
                        // Provide a content description for accessibility.
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    OutlinedTextField(
                        value = item.text,
                        onValueChange = { text ->
                            onEvent(NotesEvent.OnChecklistItemTextChange(item.id, text))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester), // Request focus for this item.
                        placeholder = { Text(stringResource(id = R.string.list_item)) },
                        singleLine = true // Ensure single line for checklist items.
                    )
                    IconButton(onClick = { onEvent(NotesEvent.DeleteChecklistItem(item.id)) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_item))
                    }
                }
            }
            // Request focus for newly added items (though typically checked items are not newly added).
            LaunchedEffect(state.newlyAddedChecklistItemId) {
                if (item.id == state.newlyAddedChecklistItemId) {
                    focusRequester.requestFocus()
                    onEvent(NotesEvent.ClearNewlyAddedChecklistItemId) // Clear the ID after focusing.
                }
            }
        }
    }
}