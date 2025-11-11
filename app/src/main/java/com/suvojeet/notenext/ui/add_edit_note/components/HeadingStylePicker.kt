package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.notes.NotesEvent

/**
 * Content for a dropdown menu that allows users to select different heading styles for text.
 * Represents styles from Normal text to Heading 6, with visual size indicators.
 *
 * This composable is designed to be used within a [DropdownMenu].
 *
 * @param onDismissRequest Lambda to be invoked when a heading style is selected or the menu is dismissed.
 * @param onEvent Lambda to dispatch [NotesEvent]s for applying the selected heading style.
 */
@Composable
fun ColumnScope.HeadingStylePickerContent(
    onDismissRequest: () -> Unit,
    onEvent: (NotesEvent) -> Unit
) {
    // Normal text style.
    DropdownMenuItem(
        text = { Text(stringResource(id = R.string.normal_text), style = TextStyle(fontSize = 16.sp), color = MaterialTheme.colorScheme.onSurface) },
        onClick = { onEvent(NotesEvent.ApplyHeadingStyle(0)); onDismissRequest() }
    )
    // Heading 1 style.
    DropdownMenuItem(
        text = { Text(stringResource(id = R.string.heading_1), style = TextStyle(fontSize = 24.sp), color = MaterialTheme.colorScheme.onSurface) },
        onClick = { onEvent(NotesEvent.ApplyHeadingStyle(1)); onDismissRequest() }
    )
    // Heading 2 style.
    DropdownMenuItem(
        text = { Text(stringResource(id = R.string.heading_2), style = TextStyle(fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurface) },
        onClick = { onEvent(NotesEvent.ApplyHeadingStyle(2)); onDismissRequest() }
    )
    // Heading 3 style.
    DropdownMenuItem(
        text = { Text(stringResource(id = R.string.heading_3), style = TextStyle(fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurface) },
        onClick = { onEvent(NotesEvent.ApplyHeadingStyle(3)); onDismissRequest() }
    )
    // Heading 4 style.
    DropdownMenuItem(
        text = { Text(stringResource(id = R.string.heading_4), style = TextStyle(fontSize = 16.sp), color = MaterialTheme.colorScheme.onSurface) },
        onClick = { onEvent(NotesEvent.ApplyHeadingStyle(4)); onDismissRequest() }
    )
    // Heading 5 style.
    DropdownMenuItem(
        text = { Text(stringResource(id = R.string.heading_5), style = TextStyle(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurface) },
        onClick = { onEvent(NotesEvent.ApplyHeadingStyle(5)); onDismissRequest() }
    )
    // Heading 6 style.
    DropdownMenuItem(
        text = { Text(stringResource(id = R.string.heading_6), style = TextStyle(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurface) },
        onClick = { onEvent(NotesEvent.ApplyHeadingStyle(6)); onDismissRequest() }
    )
}
