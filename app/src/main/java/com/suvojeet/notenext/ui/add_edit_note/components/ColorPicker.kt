package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.notes.NotesEvent

/**
 * A horizontal color picker composable that displays a list of colors,
 * allowing the user to select one. The currently selected color is visually indicated.
 *
 * @param colors A list of integer color values to display in the picker.
 * @param editingColor The integer color value of the currently selected color.
 * @param onEvent Lambda to dispatch [NotesEvent]s, specifically for [NotesEvent.OnColorChange].
 */
@Composable
fun ColorPicker(
    colors: List<Int>,
    editingColor: Int,
    onEvent: (NotesEvent) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // No Color Option
        item {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent, CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (editingColor == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clickable { onEvent(NotesEvent.OnColorChange(0)) }, // 0 for default/no color
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.FormatColorReset,
                    contentDescription = "No Color",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        items(colors) { color ->
            // Each color item is a clickable circle.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(color), CircleShape)
                    .border(
                        width = 2.dp,
                        // Highlight the selected color with a border.
                        color = if (editingColor == color) contentColorFor(backgroundColor = Color(color)) else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onEvent(NotesEvent.OnColorChange(color)) }, // Handle color selection.
                contentAlignment = Alignment.Center
            ) {
                // Display a checkmark icon on the selected color.
                if (editingColor == color) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(id = R.string.selected_color_description), // Accessibility for selected color.
                        tint = contentColorFor(backgroundColor = Color(color)) // Ensure checkmark is visible against the background.
                    )
                }
            }
        }
    }
}