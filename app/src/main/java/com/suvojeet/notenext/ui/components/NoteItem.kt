package com.suvojeet.notenext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.ui.notes.HtmlConverter
import com.suvojeet.notenext.data.NoteWithAttachments
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.theme.NoteGradients
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Displays a single note item in a card format with dynamic styling.
 * * Updates:
 * - Gradient backgrounds
 * - Dynamic font sizing for "Poster" effect on short notes
 * - Softer, larger rounded corners
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
    // Fetch dynamic gradient and content color
    val backgroundBrush = NoteGradients.getGradientBrush(note.note.color)
    val contentColor = NoteGradients.getContentColor(note.note.color)
    val tintColor = contentColor.copy(alpha = 0.7f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onNoteClick,
                onLongClick = onNoteLongClick
            ),
        shape = RoundedCornerShape(24.dp), // Increased radius for bubbly look
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent // Transparent to show Box gradient
        ),
        border = if (isSelected) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(0.dp, Color.Transparent) // No border for cleaner look
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp // Flat look (gradient provides depth)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundBrush)
        ) {
            Column(
                modifier = Modifier.padding(20.dp) // Increased padding
            ) {
                // Pin Icon
                if (note.note.isPinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = stringResource(id = R.string.pinned_note_description),
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.End),
                        tint = tintColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Note Title
                if (note.note.title.isNotEmpty()) {
                    Text(
                        text = note.note.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Note Content Preview (Dynamic Sizing)
                if (note.note.content.isNotEmpty()) {
                    if (note.note.noteType == "TEXT") {
                        val plainText = HtmlConverter.htmlToPlainText(note.note.content)
                        val contentLength = plainText.length
                        
                        // Dynamic Font Size Logic:
                        // Short text (< 50 chars) -> Large Font (Poster style)
                        // Medium text (< 100 chars) -> Medium Font
                        // Long text -> Normal Font
                        val (fontSize, lineHeight, maxLines) = when {
                            contentLength < 50 -> Triple(22.sp, 28.sp, 6)
                            contentLength < 120 -> Triple(16.sp, 22.sp, 8)
                            else -> Triple(14.sp, 20.sp, 10)
                        }

                        // If title is empty and text is short, make it even bolder
                        val fontWeight = if (note.note.title.isEmpty() && contentLength < 50) FontWeight.SemiBold else FontWeight.Normal

                        Text(
                            text = HtmlConverter.htmlToAnnotatedString(note.note.content),
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            fontWeight = fontWeight,
                            color = contentColor.copy(alpha = 0.9f),
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        // Checklist Preview
                        ChecklistPreview(note.note.content, contentColor)
                    }
                }

                // Footer Section (Attachments, Labels, Reminders)
                if (note.attachments.isNotEmpty() || !note.note.label.isNullOrEmpty() || note.note.reminderTime != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Attachment Icon
                        if (note.attachments.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Attachment,
                                contentDescription = stringResource(id = R.string.attachment_icon_description),
                                modifier = Modifier.size(16.dp),
                                tint = tintColor
                            )
                        }

                        // Reminder Icon & Text
                        note.note.reminderTime?.let { reminderTime ->
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = stringResource(id = R.string.reminder_icon_description),
                                modifier = Modifier.size(16.dp),
                                tint = tintColor
                            )
                        }

                        // Label Pill
                        if (!note.note.label.isNullOrEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = contentColor.copy(alpha = 0.1f) // Semi-transparent pill
                            ) {
                                Text(
                                    text = note.note.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = contentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistPreview(content: String, contentColor: Color) {
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
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.text,
                    fontSize = 14.sp,
                    color = contentColor.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = if (item.isChecked) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle()
                )
            }
        }
        if (checklistItems.size > 5) {
            Text(
                text = "...",
                fontSize = 14.sp,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

