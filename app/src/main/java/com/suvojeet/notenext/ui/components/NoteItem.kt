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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.util.HtmlConverter
import com.suvojeet.notenext.data.NoteWithAttachments
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.theme.NoteGradients
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    modifier: Modifier = Modifier,
    note: NoteWithAttachments,
    isSelected: Boolean,
    searchQuery: String = "",
    onNoteClick: () -> Unit,
    onNoteLongClick: () -> Unit,
    binnedDaysRemaining: Int? = null
) {
    // Check if the note has a custom color or is using the default (0)
    val isDefaultColor = note.note.color == 0

    // ... (rest of the color logic)

    // Determine colors based on whether it's default or custom
    val contentColor = if (isDefaultColor) {
        MaterialTheme.colorScheme.onSurface // Default Theme Text Color
    } else {
        NoteGradients.getContentColor(note.note.color) // Dynamic Text Color for Gradient
    }
    
    val tintColor = if (isDefaultColor) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        contentColor.copy(alpha = 0.7f)
    }

    // Determine border stroke
    val borderStroke = if (isSelected) {
        BorderStroke(3.dp, MaterialTheme.colorScheme.primary) // Thick primary border when selected
    } else if (isDefaultColor) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) // Default thin border for uncolored notes
    } else {
        BorderStroke(0.dp, Color.Transparent) // No border for colored/gradient notes
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onNoteClick,
                onLongClick = onNoteLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            // If default, use standard Surface color. If custom, make transparent to show gradient Box.
            containerColor = if (isDefaultColor) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent
        ),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDefaultColor) 2.dp else 0.dp // Elevation only for default flat cards
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isDefaultColor) {
                        Modifier.background(brush = NoteGradients.getGradientBrush(note.note.color))
                    } else {
                        Modifier
                    }
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
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

                // Note Title (Always Visible)
                if (note.note.title.isNotEmpty()) {
                    val titleText = if (searchQuery.isNotEmpty()) {
                        buildAnnotatedString {
                            val text = note.note.title
                            append(text)
                            val lowerText = text.lowercase()
                            val lowerQuery = searchQuery.lowercase()
                            var index = lowerText.indexOf(lowerQuery)
                            while (index >= 0) {
                                addStyle(
                                    style = SpanStyle(
                                        background = MaterialTheme.colorScheme.primaryContainer,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    start = index,
                                    end = index + searchQuery.length
                                )
                                index = lowerText.indexOf(lowerQuery, index + searchQuery.length)
                            }
                        }
                    } else {
                        androidx.compose.ui.text.AnnotatedString(note.note.title)
                    }

                    Text(
                        text = titleText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (note.note.isLocked) {
                    // Locked State Display
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Locked Content",
                            modifier = Modifier.size(24.dp),
                            tint = tintColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Content is locked",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    // Note Content Preview (Dynamic Sizing)
                    if ((note.note.noteType == "TEXT" && note.note.content.isNotEmpty()) || (note.note.noteType == "CHECKLIST" && note.checklistItems.isNotEmpty())) {
                        if (note.note.noteType == "TEXT") {
                            val plainTextState = androidx.compose.runtime.produceState(initialValue = "", note.note.content) {
                                value = HtmlConverter.htmlToPlainText(note.note.content)
                            }
                            val plainText = plainTextState.value
                            val contentLength = plainText.length
                            
                            val (fontSize, lineHeight, maxLines) = when {
                                contentLength < 50 -> Triple(22.sp, 28.sp, 6)
                                contentLength < 120 -> Triple(16.sp, 22.sp, 8)
                                else -> Triple(14.sp, 20.sp, 10)
                            }
    
                            val fontWeight = if (note.note.title.isEmpty() && contentLength < 50) FontWeight.SemiBold else FontWeight.Normal
    
                            val annotatedContentState = androidx.compose.runtime.produceState(initialValue = androidx.compose.ui.text.AnnotatedString(""), note.note.content) {
                                value = HtmlConverter.htmlToAnnotatedString(note.note.content)
                            }
                            val annotatedContent = annotatedContentState.value
                            val highlightedContent = if (searchQuery.isNotEmpty()) {
                                buildAnnotatedString {
                                    append(annotatedContent)
                                    val lowerText = annotatedContent.text.lowercase()
                                    val lowerQuery = searchQuery.lowercase()
                                    var index = lowerText.indexOf(lowerQuery)
                                    while (index >= 0) {
                                        addStyle(
                                            style = SpanStyle(
                                                background = MaterialTheme.colorScheme.primaryContainer,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            start = index,
                                            end = index + searchQuery.length
                                        )
                                        index = lowerText.indexOf(lowerQuery, index + searchQuery.length)
                                    }
                                }
                            } else {
                                annotatedContent
                            }
    
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                            androidx.compose.foundation.text.ClickableText(
                                text = highlightedContent,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = fontSize,
                                    lineHeight = lineHeight,
                                    fontWeight = fontWeight,
                                    color = if (isDefaultColor) MaterialTheme.colorScheme.onSurfaceVariant else contentColor.copy(alpha = 0.9f)
                                ),
                                maxLines = maxLines,
                                overflow = TextOverflow.Ellipsis,
                                onClick = { offset ->
                                    var isLink = false
                                    highlightedContent.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let { annotation ->
                                        isLink = true
                                        try { uriHandler.openUri(annotation.item) } catch (e: Exception) { e.printStackTrace() }
                                    }
                                    if (!isLink) {
                                        highlightedContent.getStringAnnotations(tag = "EMAIL", start = offset, end = offset).firstOrNull()?.let { annotation ->
                                            isLink = true
                                            try { uriHandler.openUri(annotation.item) } catch (e: Exception) { e.printStackTrace() }
                                        }
                                    }
                                    if (!isLink) {
                                        highlightedContent.getStringAnnotations(tag = "PHONE", start = offset, end = offset).firstOrNull()?.let { annotation ->
                                            isLink = true
                                            try { uriHandler.openUri(annotation.item) } catch (e: Exception) { e.printStackTrace() }
                                        }
                                    }
                                    if (!isLink) {
                                        highlightedContent.getStringAnnotations(tag = "NOTE_LINK", start = offset, end = offset).firstOrNull()?.let { annotation ->
                                            // Handle internal note link if needed, or just open the note
                                            // For now, treat as normal click (open note)
                                            // isLink = true 
                                        }
                                    }
                                    
                                    if (!isLink) {
                                        onNoteClick()
                                    }
                                }
                            )
                        } else {
                            // Checklist Preview
                            ChecklistPreview(note.checklistItems, if (isDefaultColor) MaterialTheme.colorScheme.onSurface else contentColor, searchQuery)
                        }
                    }
                    // Footer Section
                    if (note.attachments.isNotEmpty() || !note.note.label.isNullOrEmpty() || note.note.reminderTime != null || binnedDaysRemaining != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (note.attachments.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Attachment,
                                    contentDescription = stringResource(id = R.string.attachment_icon_description),
                                    modifier = Modifier.size(16.dp),
                                    tint = tintColor
                                )
                            }

                            note.note.reminderTime?.let {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = stringResource(id = R.string.reminder_icon_description),
                                    modifier = Modifier.size(16.dp),
                                    tint = tintColor
                                )
                            }

                            val label = note.note.label
                            if (!label.isNullOrEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDefaultColor) MaterialTheme.colorScheme.secondaryContainer else contentColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDefaultColor) MaterialTheme.colorScheme.onSecondaryContainer else contentColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (binnedDaysRemaining != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.days_left, binnedDaysRemaining),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
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
}

@Composable
private fun ChecklistPreview(checklistItems: List<ChecklistItem>, contentColor: Color, searchQuery: String = "") {
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
                
                val itemText = if (searchQuery.isNotEmpty()) {
                    buildAnnotatedString {
                        val text = item.text
                        append(text)
                        val lowerText = text.lowercase()
                        val lowerQuery = searchQuery.lowercase()
                        var index = lowerText.indexOf(lowerQuery)
                        while (index >= 0) {
                            addStyle(
                                style = SpanStyle(
                                    background = MaterialTheme.colorScheme.primaryContainer,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                start = index,
                                end = index + searchQuery.length
                            )
                            index = lowerText.indexOf(lowerQuery, index + searchQuery.length)
                        }
                    }
                } else {
                    androidx.compose.ui.text.AnnotatedString(item.text)
                }

                Text(
                    text = itemText,
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


