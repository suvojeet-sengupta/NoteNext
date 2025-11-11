package com.suvojeet.notenext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.ui.notes.HtmlConverter

import com.suvojeet.notenext.data.NoteWithAttachments
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.suvojeet.notenext.R

/**
 * Displays a single note item in a card format, showing its title, content preview,
 * attachments, labels, and reminders. It supports click and long-click interactions.
 *
 * @param modifier The modifier to be applied to the card.
 * @param note The [NoteWithAttachments] object containing the note data.
 * @param isSelected Boolean indicating if the note is currently selected.
 * @param onNoteClick Lambda to be invoked when the note card is clicked.
 * @param onNoteLongClick Lambda to be invoked when the note card is long-clicked.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    modifier: Modifier = Modifier,
    note: NoteWithAttachments,
    isSelected: Boolean,
    onNoteClick: () -> Unit,
    onNoteLongClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onNoteClick,
                onLongClick = onNoteLongClick
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Display pin icon if the note is pinned.
            if (note.note.isPinned) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = stringResource(id = R.string.pinned_note_description),
                    modifier = Modifier.size(16.dp).align(Alignment.End),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Note Title
            if (note.note.title.isNotEmpty()) {
                Text(
                    text = note.note.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Note Content Preview
            if (note.note.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                if (note.note.noteType == "TEXT") {
                    Text(
                        text = HtmlConverter.htmlToAnnotatedString(note.note.content),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // Display checklist preview for checklist notes.
                    ChecklistPreview(note.note.content)
                }
            }

            // Display attachment icon if there are attachments.
            if (note.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.Attachment,
                    contentDescription = stringResource(id = R.string.attachment_icon_description),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Display note label if available.
            if (!note.note.label.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = note.note.label,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Display reminder time if set.
            note.note.reminderTime?.let { reminderTime ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = stringResource(id = R.string.reminder_icon_description),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val sdf = SimpleDateFormat(stringResource(id = R.string.reminder_date_format), Locale.getDefault())
                    Text(
                        text = sdf.format(Date(reminderTime)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Displays a preview of checklist items for a note.
 * Shows up to 5 checklist items, indicating if they are checked or unchecked.
 *
 * @param content The JSON string containing the checklist items.
 */
@Composable
private fun ChecklistPreview(content: String) {
    val checklistItems = try {
        Gson().fromJson<List<ChecklistItem>>(content, object : TypeToken<List<ChecklistItem>>() {}.type)
    } catch (e: Exception) {
        emptyList<ChecklistItem>()
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        checklistItems.take(5).forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (item.isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                    contentDescription = null, // Content description for checklist items can be added if needed.
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.text,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // Indicate if there are more checklist items than shown in the preview.
        if (checklistItems.size > 5) {
            Text(
                text = "...",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
