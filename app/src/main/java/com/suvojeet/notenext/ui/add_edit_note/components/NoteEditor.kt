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
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.unit.sp



@Composable

fun NoteEditor(

    state: NotesState,

    onEvent: (NotesEvent) -> Unit

) {

    Column {

        val titleTextColor = contentColorFor(backgroundColor = Color(state.editingColor))

        TextField(

            value = state.editingTitle,

            onValueChange = { newTitle: String -> onEvent(NotesEvent.OnTitleChange(newTitle)) },

            placeholder = { Text("Title", color = contentColorFor(backgroundColor = Color(state.editingColor))) },

            modifier = Modifier

                .fillMaxWidth(),

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

        val contentTextColor = contentColorFor(backgroundColor = Color(state.editingColor))



        val contentTextStyle = when (state.activeHeadingStyle) {

            1 -> MaterialTheme.typography.headlineLarge.copy(color = contentTextColor)

            2 -> MaterialTheme.typography.headlineMedium.copy(color = contentTextColor)

            3 -> MaterialTheme.typography.headlineSmall.copy(color = contentTextColor)

            4 -> MaterialTheme.typography.titleLarge.copy(color = contentTextColor)

            5 -> MaterialTheme.typography.titleMedium.copy(color = contentTextColor)

            6 -> MaterialTheme.typography.titleSmall.copy(color = contentTextColor)

            else -> MaterialTheme.typography.bodyLarge.copy(color = contentTextColor)

        }

        

        TextField(

            value = state.editingContent,

            onValueChange = { onEvent(NotesEvent.OnContentChange(it)) },

            placeholder = { Text("Note", color = contentColorFor(backgroundColor = Color(state.editingColor))) },

            modifier = Modifier

                .fillMaxWidth(),

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