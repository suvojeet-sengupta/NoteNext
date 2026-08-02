@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.ui.components.springPress

/**
 * "Related notes" carousel pinned to the bottom of an open note.
 * Each card is tappable and jumps to the related note.
 *
 * Linked-notes computation is local (Jaccard similarity over tokens) so
 * this section is fast and offline. The user controls whether it's shown
 * via the LINKED_NOTES feature toggle.
 */
@Composable
fun AiLinkedNotesSection(
    visible: Boolean,
    notes: List<Note>,
    onOpenNote: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && notes.isNotEmpty(),
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Hub,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Related notes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    LinkedNoteCard(note = note, onClick = { onOpenNote(note) })
                }
            }
        }
    }
}

@Composable
private fun LinkedNoteCard(note: Note, onClick: () -> Unit) {
    val cleanTitle = remember(note.title) {
        com.suvojeet.notenext.util.HtmlConverter.cleanHtmlToPlain(note.title)
    }

    val previewText = remember(note.content) {
        com.suvojeet.notenext.util.HtmlConverter.cleanHtmlToPlain(note.content)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .heightIn(min = 80.dp)
            .springPress(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                cleanTitle.ifBlank { "(untitled)" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                previewText.take(120),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
        }
    }
}
