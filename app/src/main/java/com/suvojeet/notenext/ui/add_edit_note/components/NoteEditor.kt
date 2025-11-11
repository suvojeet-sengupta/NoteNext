package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * Composable for editing the title and content of a note.
 * The content text style dynamically adjusts based on the active heading style.
 *
 * @param state The current [NotesState] containing the note's title, content, and active heading style.
 * @param onEvent Lambda to dispatch [NotesEvent]s for title and content changes.
 */
@Composable
fun NoteEditor(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit
) {
    Column {
        // Determine text color for the title based on the note's background color.
        val titleTextColor = contentColorFor(backgroundColor = Color(state.editingColor))

        // TextField for editing the note's title.
        TextField(
            value = state.editingTitle,
            onValueChange = { newTitle: String -> onEvent(NotesEvent.OnTitleChange(newTitle)) },
            placeholder = { Text(stringResource(id = R.string.title), color = contentColorFor(backgroundColor = Color(state.editingColor))) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = contentColorFor(backgroundColor = Color(state.editingColor)),
                selectionColors = TextSelectionColors(
                    handleColor = contentColorFor(backgroundColor = Color(state.editingColor)),
                    backgroundColor = contentColorFor(backgroundColor = Color(state.editingColor)).copy(alpha = 0.4f)
                )
            ),
            textStyle = MaterialTheme.typography.headlineMedium.copy(color = titleTextColor),
            singleLine = true,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Determine text color for the content based on the note's background color.
        val contentTextColor = contentColorFor(backgroundColor = Color(state.editingColor))

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
        
        // TextField for editing the note's content.
        TextField(
            value = state.editingContent,
            onValueChange = { onEvent(NotesEvent.OnContentChange(it)) },
            placeholder = { Text(stringResource(id = R.string.content), color = contentColorFor(backgroundColor = Color(state.editingColor))) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = contentColorFor(backgroundColor = Color(state.editingColor)),
                selectionColors = TextSelectionColors(
                    handleColor = contentColorFor(backgroundColor = Color(state.editingColor)),
                    backgroundColor = contentColorFor(backgroundColor = Color(state.editingColor)).copy(alpha = 0.4f)
                )
            ),
            textStyle = contentTextStyle
        )
    }
}