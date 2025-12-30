package com.suvojeet.notenext.ui.add_edit_note.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

/**
 * A modal bottom sheet displaying more options for the current note.
 * Includes actions like delete, copy, share, add labels, and save as.
 * Also shows the last edited timestamp of the note.
 *
 * @param state The current [NotesState] containing information about the note.
 * @param onEvent Lambda to dispatch [NotesEvent]s for various actions.
 * @param onDismiss Lambda to be invoked when the bottom sheet is dismissed.
 * @param showDeleteDialog Lambda to show/hide the delete confirmation dialog.
 * @param showSaveAsDialog Lambda to show/hide the save as dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsSheet(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onDismiss: () -> Unit,
    showDeleteDialog: (Boolean) -> Unit,
    showSaveAsDialog: (Boolean) -> Unit,
    showHistoryDialog: (Boolean) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()) }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display last edited time if available and not a new note.
            if (!state.editingIsNewNote && state.editingLastEdited != 0L) {
                Text(
                    text = stringResource(id = R.string.last_edited, dateFormat.format(Date(state.editingLastEdited))),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Divider()
            }

            // Define the list of options with their labels and icons.
            val lockLabel = if (state.editingIsLocked) "Unlock" else "Lock"
            val lockIcon = if (state.editingIsLocked) Icons.Default.LockOpen else Icons.Default.Lock

            val convertLabel = if (state.editingNoteType == "TEXT") "Convert to List" else "Convert to Text"
            // Using Check as a placeholder icon for convert if a specific one isn't available, or toggle list
            val convertIcon = Icons.Default.Check 

            // Create a list of options using a data structure
            data class OptionItem(val label: String, val icon: ImageVector, val action: () -> Unit)

            val options = mutableListOf<OptionItem>()
            
            options.add(OptionItem(lockLabel, lockIcon) { onEvent(NotesEvent.OnToggleLockClick) })
            options.add(OptionItem(convertLabel, convertIcon) { onEvent(NotesEvent.OnToggleNoteType) })
            options.add(OptionItem(stringResource(id = R.string.delete), Icons.Default.Delete) { showDeleteDialog(true) })
            options.add(OptionItem(stringResource(id = R.string.make_a_copy), Icons.Default.ContentCopy) { onEvent(NotesEvent.OnCopyCurrentNoteClick) })
            options.add(OptionItem(stringResource(id = R.string.share), Icons.Default.Share) {
                 val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, state.editingTitle + "\n\n" + state.editingContent.text)
                    putExtra(Intent.EXTRA_SUBJECT, state.editingTitle)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            })
            options.add(OptionItem(stringResource(id = R.string.labels), Icons.AutoMirrored.Filled.Label) { onEvent(NotesEvent.OnAddLabelsToCurrentNoteClick) })
            options.add(OptionItem(stringResource(id = R.string.save_as), Icons.Default.Check) { showSaveAsDialog(true) })
            
            if (!state.editingIsNewNote) {
                options.add(OptionItem(stringResource(id = R.string.history), Icons.Default.History) { showHistoryDialog(true) })
            }

            // Display using Column
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEach { option ->
                    MoreOptionsItem(
                        icon = option.icon,
                        label = option.label,
                        onClick = {
                            onDismiss()
                            option.action()
                        }
                    )
                }
            }
        }
    }
}

/**
 * A single item displayed within the [MoreOptionsSheet].
 *
 * @param icon The [ImageVector] to display for the option.
 * @param label The text label for the option.
 * @param onClick Lambda to be invoked when the option item is clicked.
 * @param modifier The modifier to be applied to the item.
 */
@Composable
private fun MoreOptionsItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
