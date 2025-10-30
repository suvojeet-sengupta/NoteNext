package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.ui.notes.NotesEvent

@Composable
fun HeadingStylePicker(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onEvent: (NotesEvent) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text("Normal text", style = TextStyle(fontSize = 16.sp)) },
            onClick = { onEvent(NotesEvent.ApplyHeadingStyle(0)); onDismissRequest() }
        )
        DropdownMenuItem(
            text = { Text("Heading 1", style = TextStyle(fontSize = 24.sp)) },
            onClick = { onEvent(NotesEvent.ApplyHeadingStyle(1)); onDismissRequest() }
        )
        DropdownMenuItem(
            text = { Text("Heading 2", style = TextStyle(fontSize = 20.sp)) },
            onClick = { onEvent(NotesEvent.ApplyHeadingStyle(2)); onDismissRequest() }
        )
        DropdownMenuItem(
            text = { Text("Heading 3", style = TextStyle(fontSize = 18.sp)) },
            onClick = { onEvent(NotesEvent.ApplyHeadingStyle(3)); onDismissRequest() }
        )
        DropdownMenuItem(
            text = { Text("Heading 4", style = TextStyle(fontSize = 16.sp)) },
            onClick = { onEvent(NotesEvent.ApplyHeadingStyle(4)); onDismissRequest() }
        )
        DropdownMenuItem(
            text = { Text("Heading 5", style = TextStyle(fontSize = 14.sp)) },
            onClick = { onEvent(NotesEvent.ApplyHeadingStyle(5)); onDismissRequest() }
        )
        DropdownMenuItem(
            text = { Text("Heading 6", style = TextStyle(fontSize = 12.sp)) },
            onClick = { onEvent(NotesEvent.ApplyHeadingStyle(6)); onDismissRequest() }
        )
    }
}
