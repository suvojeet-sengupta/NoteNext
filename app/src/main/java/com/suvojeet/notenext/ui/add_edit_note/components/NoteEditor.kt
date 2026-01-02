package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation

import androidx.compose.material.icons.filled.Alarm

/**
 * Enhanced title editor with parallax effect based on scroll position.
 * @param scrollOffset The current scroll offset for parallax calculation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteTitleEditor(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onReminderClick: () -> Unit,
    scrollOffset: Float = 0f  // For parallax effect
) {
    // Parallax factor - title moves slower than content
    val parallaxOffset = (-scrollOffset * 0.3f).coerceIn(-30f, 30f)
    
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                translationY = parallaxOffset
            }
    ) {
        val titleTextColor = MaterialTheme.colorScheme.onSurface

        TextField(
            value = state.editingTitle,
            onValueChange = { newTitle: String -> onEvent(NotesEvent.OnTitleChange(newTitle)) },
            placeholder = { Text(stringResource(id = R.string.title), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
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
            textStyle = MaterialTheme.typography.headlineMedium.copy(color = titleTextColor),
            singleLine = true,
            maxLines = 1
        )

        ReminderDisplay(
            reminderTime = state.editingReminderTime,
            repeatOption = state.editingRepeatOption,
            onClick = onReminderClick
        )
    }
}

/**
 * Enhanced content editor with animated cursor glow effect.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteContentEditor(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onUrlClick: (String) -> Unit
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    
    // Cursor glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    
    val cursorColor = MaterialTheme.colorScheme.primary
    val glowBrush = Brush.radialGradient(
        colors = listOf(
            cursorColor.copy(alpha = glowAlpha),
            cursorColor.copy(alpha = glowAlpha * 0.5f),
            Color.Transparent
        ),
        radius = 40f
    )

    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        val contentTextColor = MaterialTheme.colorScheme.onSurface
        val contentTextStyle = when (state.activeHeadingStyle) {
            1 -> MaterialTheme.typography.headlineLarge.copy(color = contentTextColor)
            2 -> MaterialTheme.typography.headlineMedium.copy(color = contentTextColor)
            3 -> MaterialTheme.typography.headlineSmall.copy(color = contentTextColor)
            4 -> MaterialTheme.typography.titleLarge.copy(color = contentTextColor)
            5 -> MaterialTheme.typography.titleMedium.copy(color = contentTextColor)
            6 -> MaterialTheme.typography.titleSmall.copy(color = contentTextColor)
            else -> MaterialTheme.typography.bodyLarge.copy(color = contentTextColor)
        }

        BasicTextField(
            value = state.editingContent,
            onValueChange = { onEvent(NotesEvent.OnContentChange(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Draw soft glow behind cursor area
                    textLayoutResult?.let { layout ->
                        val cursorPosition = state.editingContent.selection.start
                        if (cursorPosition >= 0 && layout.layoutInput.text.isNotEmpty() && cursorPosition <= layout.layoutInput.text.length) {
                            try {
                                val cursorRect = layout.getCursorRect(cursorPosition.coerceIn(0, layout.layoutInput.text.length))
                                drawCircle(
                                    brush = glowBrush,
                                    radius = 30f,
                                    center = Offset(cursorRect.left, cursorRect.top + cursorRect.height / 2)
                                )
                            } catch (e: Exception) {
                                // Ignore cursor position errors during rapid typing
                            }
                        }
                    }
                }
                .pointerInput(state.editingContent) {
                    detectTapGestures { offset ->
                            textLayoutResult?.let { layoutResult ->
                                val position = layoutResult.getOffsetForPosition(offset)
                                
                                val urlAnnotation = state.editingContent.annotatedString.getStringAnnotations("URL", position, position).firstOrNull()
                                    ?: state.editingContent.annotatedString.getStringAnnotations("EMAIL", position, position).firstOrNull()
                                    ?: state.editingContent.annotatedString.getStringAnnotations("PHONE", position, position).firstOrNull()

                                if (urlAnnotation != null) {
                                    onUrlClick(urlAnnotation.item)
                                } else {
                                    state.editingContent.annotatedString.getStringAnnotations("NOTE_LINK", position, position)
                                        .firstOrNull()?.let { annotation ->
                                            onEvent(NotesEvent.NavigateToNoteByTitle(annotation.item))
                                        }
                                }
                            }
                    }
                },
            onTextLayout = { textLayoutResult = it },
            textStyle = contentTextStyle,
            cursorBrush = SolidColor(cursorColor),
            decorationBox = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = state.editingContent.text,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = false,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    placeholder = { Text(stringResource(id = R.string.note), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = cursorColor,
                        selectionColors = TextSelectionColors(
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
        val repeatText = if (repeatOption != null && repeatOption != "DOES_NOT_REPEAT") ", $repeatOption" else ""

        androidx.compose.material3.AssistChip(
            onClick = onClick,
            label = { Text(text = "$formattedTime$repeatText") },
            leadingIcon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Alarm, contentDescription = "Reminder") },
            colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                labelColor = MaterialTheme.colorScheme.primary,
                leadingIconContentColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
    }
}