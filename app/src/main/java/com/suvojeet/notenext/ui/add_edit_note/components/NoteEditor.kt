package com.suvojeet.notenext.ui.add_edit_note.components

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
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Composable for editing the title and content of a note.
 * The content text style dynamically adjusts based on the active heading style.
 *
 * @param state The current [NotesState] containing the note's title, content, and active heading style.
 * @param onEvent Lambda to dispatch [NotesEvent]s for title and content changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditor(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onUrlClick: (String) -> Unit = {}
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // Determine text color for the title based on the note's background color.
        val titleTextColor = MaterialTheme.colorScheme.onSurface

        // TextField for editing the note's title.
        TextField(
            value = state.editingTitle,
            onValueChange = { newTitle: String -> onEvent(NotesEvent.OnTitleChange(newTitle)) },
            placeholder = { Text(stringResource(id = R.string.title), color = MaterialTheme.colorScheme.onSurface) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onSurface,
                selectionColors = TextSelectionColors(
                    handleColor = MaterialTheme.colorScheme.onSurface,
                    backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            ),
            textStyle = MaterialTheme.typography.headlineMedium.copy(color = titleTextColor),
            singleLine = true,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Determine text color for the content based on the note's background color.
        val contentTextColor = MaterialTheme.colorScheme.onSurface

        // Dynamically determine the text style for the content based on the active heading style.
        val contentTextStyle = when (state.activeHeadingStyle) {
            1 -> MaterialTheme.typography.headlineLarge.copy(color = contentTextColor)
            2 -> MaterialTheme.typography.headlineMedium.copy(color = contentTextColor)
            3 -> MaterialTheme.typography.headlineSmall.copy(color = contentTextColor)
            4 -> MaterialTheme.typography.titleLarge.copy(color = contentTextColor)
            5 -> MaterialTheme.typography.titleMedium.copy(color = contentTextColor)
            6 -> MaterialTheme.typography.titleSmall.copy(color = contentTextColor)
            else -> MaterialTheme.typography.bodyLarge.copy(color = contentTextColor)
        }
        
        // Use BasicTextField to get access to onTextLayout for link detection
        BasicTextField(
            value = state.editingContent,
            onValueChange = { onEvent(NotesEvent.OnContentChange(it)) },
            modifier = Modifier
                .fillMaxWidth()
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
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = state.editingContent.text,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = false,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    placeholder = { Text(stringResource(id = R.string.note), color = MaterialTheme.colorScheme.onSurface) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.onSurface,
                        selectionColors = TextSelectionColors(
                            handleColor = MaterialTheme.colorScheme.onSurface,
                            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                )
            }
        )
    }
}