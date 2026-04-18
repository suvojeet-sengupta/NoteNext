@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.NoteSummaryWithAttachments
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.ui.theme.NoteGradients
import com.suvojeet.notenext.util.HtmlConverter
import com.suvojeet.notenext.ui.components.CuteCardShape
import com.suvojeet.notenext.ui.components.PlayfulPalette

@Composable
fun NoteItem(
    modifier: Modifier = Modifier,
    note: NoteSummaryWithAttachments,
    isSelected: Boolean,
    searchQuery: String = "",
    onNoteClick: () -> Unit,
    onNoteLongClick: () -> Unit,
    binnedDaysRemaining: Int? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val adaptiveColor = NoteGradients.getAdaptiveColor(note.note.color, isDarkTheme)
    val isDefaultColor = adaptiveColor == 0

    // Playful vibe: default-colour notes get a stable pastel tint derived from their id,
    // so the grid reads as a friendly quilt rather than a wall of grey surfaces. Notes
    // the user has explicitly coloured still honour that choice.
    val playfulTint = PlayfulPalette.tintFor(note.note.id)
    val effectiveBackground = if (isDefaultColor) playfulTint else Color(adaptiveColor)

    val contentColor = if (isDefaultColor) {
        MaterialTheme.colorScheme.onSurface
    } else {
        NoteGradients.getContentColor(adaptiveColor)
    }

    val tintColor = if (isDefaultColor) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        contentColor.copy(alpha = 0.7f)
    }

    val decryptedNote = remember(note.note.title, note.note.content, note.note.isEncrypted) {
        if (note.note.isEncrypted) {
            if (note.note.isLocked) {
                // Never attempt to decrypt locked notes without auth.
                // Tap-to-unlock is handled by the click handler in NotesScreen.
                note.note
            } else {
                // Non-locked encrypted notes use the non-auth key — safe to decrypt here.
                com.suvojeet.notenext.util.CryptoUtils.decryptNote(note.note)
            }
        } else {
            note.note
        }
    }
    val motionScheme = MaterialTheme.motionScheme
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 2.dp,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "Elevation"
    )

    // Playful vibe: every card is a squircle. Pinned cards bump up a touch more
    // rounded so they still read as "special" but not via a different shape family.
    val cardShape = CuteCardShape

    // Bouncy selection pulse — cards gently scale up when selected.
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 0.97f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "SelectionScale"
    )

    val borderStroke = when {
        isSelected -> BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        isDefaultColor -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
        else -> null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = selectionScale
                scaleY = selectionScale
            }
            .springPress(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioHighBouncy,
                stiffness = 500f
            )
            .combinedClickable(
                onClick = onNoteClick,
                onLongClick = onNoteLongClick
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = effectiveBackground,
            contentColor = contentColor
        ),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
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

                if (decryptedNote.title.isNotEmpty()) {
                    val unescapedTitle = remember(decryptedNote.title) {
                        androidx.core.text.HtmlCompat.fromHtml(decryptedNote.title, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                    }
                    val titleText = if (searchQuery.isNotEmpty()) {
                        buildAnnotatedString {
                            val text = unescapedTitle
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
                        androidx.compose.ui.text.AnnotatedString(unescapedTitle)
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (decryptedNote.isLocked) {
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
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    if ((decryptedNote.noteType == NoteType.TEXT && decryptedNote.content.isNotEmpty()) || (decryptedNote.noteType == NoteType.CHECKLIST && note.checklistItems.isNotEmpty())) {
                        if (decryptedNote.noteType == NoteType.TEXT) {
                            val rawContentLength = decryptedNote.content.length
                            
                            val (textStyle, maxLines) = when {
                                rawContentLength < 100 -> MaterialTheme.typography.headlineSmall to 6
                                rawContentLength < 250 -> MaterialTheme.typography.bodyLarge to 8
                                else -> MaterialTheme.typography.bodyMedium to 10
                            }
    
                            val fontWeight = if (decryptedNote.title.isEmpty() && rawContentLength < 100) FontWeight.SemiBold else FontWeight.Normal
    
                            val annotatedContent = remember(decryptedNote.content) {
                                // Strip HTML tags for preview and unescape entities
                                val plainText = decryptedNote.content.replace(Regex("<[^>]*>"), "")
                                val unescaped = androidx.core.text.HtmlCompat.fromHtml(plainText, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                                androidx.compose.ui.text.AnnotatedString(unescaped)
                            }

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
    
                            val uriHandler = LocalUriHandler.current
                            var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

                            Text(
                                text = highlightedContent,
                                style = textStyle.copy(
                                    fontWeight = fontWeight,
                                    color = if (isDefaultColor) MaterialTheme.colorScheme.onSurfaceVariant else contentColor.copy(alpha = 0.9f)
                                ),
                                maxLines = maxLines,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { textLayoutResult = it },
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            onNoteLongClick()
                                        },
                                        onTap = { pos ->
                                            val layoutResult = textLayoutResult ?: return@detectTapGestures
                                            val offset = layoutResult.getOffsetForPosition(pos)
                                            
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
                                                onNoteClick()
                                            }
                                        }
                                    )
                                }
                            )
                        } else {
                            ChecklistPreview(note.checklistItems, if (isDefaultColor) MaterialTheme.colorScheme.onSurface else contentColor, searchQuery)
                        }
                    }

                    if (note.note.linkPreviews.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinkPreviewDisplay(
                            linkPreview = note.note.linkPreviews.first(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

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
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isDefaultColor) MaterialTheme.colorScheme.secondaryContainer else contentColor.copy(alpha = 0.15f)
                                ) {
                                    val labelText = if (searchQuery.isNotEmpty()) {
                                        buildAnnotatedString {
                                            append(label)
                                            val lowerText = label.lowercase()
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
                                        androidx.compose.ui.text.AnnotatedString(label)
                                    }

                                    Text(
                                        text = labelText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDefaultColor) MaterialTheme.colorScheme.onSecondaryContainer else contentColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (binnedDaysRemaining != null) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
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
                
                val unescapedItemText = remember(item.text) {
                    androidx.core.text.HtmlCompat.fromHtml(item.text, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                }
                
                val itemText = if (searchQuery.isNotEmpty()) {
                    buildAnnotatedString {
                        val text = unescapedItemText
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
                    androidx.compose.ui.text.AnnotatedString(unescapedItemText)
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
