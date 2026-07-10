package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.suvojeet.notenext.data.NoteSummaryWithAttachments

@Composable
fun MentionPopup(
    isVisible: Boolean,
    notes: List<NoteSummaryWithAttachments>,
    onNoteClick: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible && notes.isNotEmpty()) {
        Popup(
            alignment = Alignment.BottomStart,
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .heightIn(max = 200.dp)
                    .padding(8.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                LazyColumn {
                    items(
                        items = notes,
                        key = { it.note.id }
                    ) { noteWithAttachments ->
                        ListItem(
                            headlineContent = { Text(noteWithAttachments.note.title.ifBlank { "Untitled" }) },
                            modifier = Modifier.clickable {
                                onNoteClick(noteWithAttachments.note.id, noteWithAttachments.note.title)
                            }
                        )
                    }
                }
            }
        }
    }
}
