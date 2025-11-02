package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.ui.notes.NotesEvent

@Composable
fun ColumnScope.HeadingStylePickerContent(
    onDismissRequest: () -> Unit,
    onEvent: (NotesEvent) -> Unit
) {
    DropdownMenuItem(
        text = { Text("Normal text", style = TextStyle(fontSize = 16.sp), color = MaterialTheme.colorScheme.onSurface) },
        onClick = { onEvent(NotesEvent.ApplyHeadingStyle(0)); onDismissRequest() }
    )
    DropdownMenuItem(
        text = { Text("Heading 1", style = TextStyle(fontSize = 24.sp), color = MaterialTheme.colorScheme.onSurface) },
        onClick = { onEvent(NotesEvent.ApplyHeadingStyle(1)); onDismissRequest() }
    )
    DropdownMenuItem(
        text = { Text("Heading 2", style = TextStyle(fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurface) },
        onClick = { onEvent(NotesEvent.ApplyHeadingStyle(2)); onDismissRequest() }
    )
    DropdownMenuItem(
        text = { Text("Heading 3", style = TextStyle(fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurface) },
        onClick = { onEvent(NotesEvent.ApplyHeadingStyle(3)); onDismissRequest() }
    )
    DropdownMenuItem(
        text = { Text("Heading 4", style = TextStyle(fontSize = 16.sp), color = MaterialTheme.colorScheme.onSurface) },
        onClick = { onEvent(NotesEvent.ApplyHeadingStyle(4)); onDismissRequest() }
    )
    DropdownMenuItem(
        text = { Text("Heading 5", style = TextStyle(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurface) },
        onClick = { onEvent(NotesEvent.ApplyHeadingStyle(5)); onDismissRequest() }
    )
    DropdownMenuItem(
        text = { Text("Heading 6", style = TextStyle(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurface) },
        onClick = { onEvent(NotesEvent.ApplyHeadingStyle(6)); onDismissRequest() }
    )
}
