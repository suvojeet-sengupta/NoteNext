@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.core.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesEditState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoteTitleEditor(
    state: NotesEditState,
    onEvent: (NotesEvent) -> Unit,
    onReminderClick: () -> Unit,
    scrollOffset: Float = 0f
) {
    val parallaxOffset = (-scrollOffset * 0.2f).coerceIn(-20f, 20f)
    
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .graphicsLayer {
                translationY = parallaxOffset
            }
    ) {
        val titleTextColor = MaterialTheme.colorScheme.onSurface

        TextField(
            value = state.editingTitle,
            onValueChange = { newTitle: String -> onEvent(NotesEvent.OnTitleChange(newTitle)) },
            placeholder = { 
                Text(
                    stringResource(id = R.string.title), 
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                selectionColors = TextSelectionColors(
                    handleColor = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ),
            textStyle = MaterialTheme.typography.displaySmall.copy(
                color = titleTextColor,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            singleLine = false,
            maxLines = 3
        )

        ReminderDisplay(
            reminderTime = state.editingReminderTime,
            repeatOption = state.editingRepeatOption,
            onClick = onReminderClick
        )
    }
}

fun LazyListScope.NoteContentItems(
    state: NotesEditState,
    splitOffsets: List<Int>,
    onEvent: (NotesEvent) -> Unit,
    onUrlClick: (String) -> Unit,
    onSlashCommand: () -> Unit,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val globalContent = state.editingContent
    val text = globalContent.text

    itemsIndexed(
        items = splitOffsets,
        key = { _, startOffset -> startOffset }
    ) { index, startOffset ->
        val endOffset = if (index + 1 < splitOffsets.size) splitOffsets[index + 1] else text.length
        
        NoteContentChunkEditor(
            index = index,
            startOffset = startOffset,
            endOffset = endOffset,
            state = state,
            onEvent = onEvent,
            onUrlClick = onUrlClick,
            onSlashCommand = onSlashCommand,
            onTextLayout = onTextLayout
        )
    }

    item {
        MentionPopup(
            isVisible = state.isMentionPopupVisible,
            notes = state.mentionableNotes,
            onNoteClick = { id, title -> onEvent(NotesEvent.InsertMention(id, title)) },
            onDismiss = { onEvent(NotesEvent.CloseMentionPopup) }
        )
    }
}

@Composable
fun NoteContentChunkEditor(
    index: Int,
    startOffset: Int,
    endOffset: Int,
    state: NotesEditState,
    onEvent: (NotesEvent) -> Unit,
    onUrlClick: (String) -> Unit,
    onSlashCommand: () -> Unit,
    onTextLayout: (TextLayoutResult) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Hold latest references
    val currentOnUrlClick by rememberUpdatedState(onUrlClick)
    val currentOnEvent by rememberUpdatedState(onEvent)
    
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    
    val cursorColor = MaterialTheme.colorScheme.primary
    val glowBrush = Brush.radialGradient(
        colors = listOf(
            cursorColor.copy(alpha = glowAlpha),
            cursorColor.copy(alpha = glowAlpha * 0.4f),
            Color.Transparent
        ),
        radius = 60f
    )

    val contentTextColor = MaterialTheme.colorScheme.onSurface
    val contentTextStyle = when (state.activeHeadingStyle) {
        1 -> MaterialTheme.typography.headlineLarge.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
        2 -> MaterialTheme.typography.headlineMedium.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
        3 -> MaterialTheme.typography.headlineSmall.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
        4 -> MaterialTheme.typography.titleLarge.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
        5 -> MaterialTheme.typography.titleMedium.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
        6 -> MaterialTheme.typography.titleSmall.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
        else -> MaterialTheme.typography.bodyLarge.copy(color = contentTextColor, lineHeight = 28.sp)
    }

    var chunkLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    // Auto-scroll to cursor
    LaunchedEffect(state.editingContent.selection) {
        val selection = state.editingContent.selection
        if (selection.collapsed) {
            val globalCursor = selection.start
            if (globalCursor >= startOffset && globalCursor <= endOffset) {
                chunkLayoutResult?.let { layout ->
                    val localCursor = globalCursor - startOffset
                    if (localCursor >= 0 && localCursor <= layout.layoutInput.text.length) {
                        try {
                            val cursorRect = layout.getCursorRect(localCursor)
                            scope.launch {
                                bringIntoViewRequester.bringIntoView(cursorRect)
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        }
    }

    // Derive the chunk from global state
    val chunk = remember(state.editingContent, startOffset, endOffset) {
        val globalContent = state.editingContent
        val text = globalContent.text
        val safeEnd = endOffset.coerceAtMost(text.length)
        val safeStart = startOffset.coerceAtMost(safeEnd)
        
        val chunkAnnotated = try {
            globalContent.annotatedString.subSequence(safeStart, safeEnd)
        } catch (e: Exception) {
            androidx.compose.ui.text.AnnotatedString("")
        }
        
        val globalSelection = globalContent.selection
        val localSelection = androidx.compose.ui.text.TextRange(
            (globalSelection.start - startOffset).coerceIn(0, safeEnd - safeStart),
            (globalSelection.end - startOffset).coerceIn(0, safeEnd - safeStart)
        )
        
        TextFieldValue(chunkAnnotated, localSelection)
    }

    // Apply search highlighting
    val highlightedChunk = remember(chunk, state.noteSearchQuery, state.searchResultIndices, state.currentSearchResultIndex, startOffset) {
        if (state.isSearchingInNote && state.noteSearchQuery.isNotBlank()) {
            val annotatedString = chunk.annotatedString
            val builder = androidx.compose.ui.text.AnnotatedString.Builder(annotatedString)
            
            val query = state.noteSearchQuery
            val chunkText = chunk.text
            val chunkEndOffset = startOffset + chunkText.length
            
            state.searchResultIndices.forEachIndexed { resultIndex, globalIndex ->
                if (globalIndex >= startOffset && globalIndex < chunkEndOffset) {
                    val localIndex = globalIndex - startOffset
                    val isCurrent = resultIndex == state.currentSearchResultIndex
                    
                    builder.addStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            background = if (isCurrent) 
                                Color(0xFFFFD54F) // Brighter yellow for current
                            else 
                                Color(0xFFFFD54F).copy(alpha = 0.4f),
                            color = Color.Black
                        ),
                        start = localIndex,
                        end = (localIndex + query.length).coerceAtMost(chunkText.length)
                    )
                }
            }
            chunk.copy(annotatedString = builder.toAnnotatedString())
        } else {
            chunk
        }
    }

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        BasicTextField(
            value = highlightedChunk,
            onValueChange = { newHighlightedChunk ->
                val globalContent = state.editingContent
                val originalAnnotatedString = globalContent.annotatedString
                
                // Calculate global selection
                val newGlobalSelection = androidx.compose.ui.text.TextRange(
                    newHighlightedChunk.selection.start + startOffset,
                    newHighlightedChunk.selection.end + startOffset
                )

                // If text changed, merge it back
                if (newHighlightedChunk.text != chunk.text) {
                    val updatedAnnotatedString = try {
                        originalAnnotatedString.subSequence(0, startOffset) + 
                        newHighlightedChunk.annotatedString + 
                        originalAnnotatedString.subSequence(endOffset.coerceAtMost(originalAnnotatedString.length), originalAnnotatedString.length)
                    } catch (e: Exception) {
                        originalAnnotatedString
                    }
                    
                    onEvent(NotesEvent.OnContentChange(TextFieldValue(
                        annotatedString = updatedAnnotatedString,
                        selection = newGlobalSelection
                    )))
                } else {
                    // Only selection changed
                    onEvent(NotesEvent.OnContentChange(globalContent.copy(selection = newGlobalSelection)))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .drawBehind {
                    chunkLayoutResult?.let { layout ->
                        val globalCursor = state.editingContent.selection.start
                        val localCursor = globalCursor - startOffset
                        if (localCursor >= 0 && localCursor <= chunk.text.length && layout.layoutInput.text.isNotEmpty()) {
                            try {
                                val cursorRect = layout.getCursorRect(localCursor.coerceIn(0, layout.layoutInput.text.length))
                                drawCircle(
                                    brush = glowBrush,
                                    radius = 40f,
                                    center = Offset(cursorRect.left, cursorRect.top + cursorRect.height / 2)
                                )
                            } catch (e: Exception) {}
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                         chunkLayoutResult?.let { layoutResult ->
                             val position = layoutResult.getOffsetForPosition(offset)
                             val annotatedString = chunk.annotatedString

                             val urlAnnotation = annotatedString.getStringAnnotations("URL", position, position).firstOrNull()
                                 ?: annotatedString.getStringAnnotations("EMAIL", position, position).firstOrNull()
                                 ?: annotatedString.getStringAnnotations("PHONE", position, position).firstOrNull()

                             if (urlAnnotation != null) {
                                 currentOnUrlClick(urlAnnotation.item)
                             } else {
                                 annotatedString.getStringAnnotations("NOTE_LINK", position, position)
                                     .firstOrNull()?.let { annotation ->
                                         currentOnEvent(NotesEvent.NavigateToNoteByTitle(annotation.item))
                                     }
                             }
                         }
                    }
                },
            onTextLayout = { 
                chunkLayoutResult = it 
                onTextLayout(it)
            },
            textStyle = contentTextStyle,
            cursorBrush = SolidColor(cursorColor),
            decorationBox = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = chunk.text,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = false,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    placeholder = { 
                        if (index == 0 && chunk.text.isEmpty()) {
                            Text(stringResource(id = R.string.note), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), style = contentTextStyle)
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = cursorColor,
                        selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                            handleColor = cursorColor,
                            backgroundColor = cursorColor.copy(alpha = 0.3f)
                        )
                    ),
                    contentPadding = PaddingValues(horizontal = 0.dp) 
                )
            }
        )
    }
}

@Composable
fun ReminderDisplay(
    reminderTime: Long?,
    repeatOption: String?,
    onClick: () -> Unit
) {
    if (reminderTime != null) {
        val dateTime = java.time.Instant.ofEpochMilli(reminderTime).atZone(java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, HH:mm")
        val formattedTime = formatter.format(dateTime)
        val repeatText = if (repeatOption != null && repeatOption != "NEVER") ", $repeatOption" else ""

        AssistChip(
            onClick = onClick,
            label = { Text(text = "$formattedTime$repeatText") },
            leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = "Reminder") },
            shape = MaterialTheme.shapes.medium,
            colors = AssistChipDefaults.assistChipColors(
                labelColor = MaterialTheme.colorScheme.primary,
                leadingIconContentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.springPress()
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
