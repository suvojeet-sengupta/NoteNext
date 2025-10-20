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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.notes.NotesEvent

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
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(color), CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (editingColor == color) contentColorFor(backgroundColor = Color(color)) else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onEvent(NotesEvent.OnColorChange(color)) },
                contentAlignment = Alignment.Center
            ) {
                if (editingColor == color) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = contentColorFor(backgroundColor = Color(color))
                    )
                }
            }
        }
    }
}