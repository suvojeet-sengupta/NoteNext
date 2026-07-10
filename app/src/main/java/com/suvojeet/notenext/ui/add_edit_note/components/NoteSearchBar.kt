package com.suvojeet.notenext.ui.add_edit_note.components

import com.suvojeet.notenext.R
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NoteSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    currentResult: Int,
    totalResults: Int,
    backgroundColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(id = R.string.note_search_placeholder)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
            
            if (totalResults > 0) {
                Text(
                    text = "$currentResult / $totalResults",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            
            IconButton(onClick = onPrevious, enabled = totalResults > 0) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(id = R.string.note_search_previous_cd))
            }
            
            IconButton(onClick = onNext, enabled = totalResults > 0) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(id = R.string.note_search_next_cd))
            }
            
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.note_search_close_cd))
            }
        }
    }
}
