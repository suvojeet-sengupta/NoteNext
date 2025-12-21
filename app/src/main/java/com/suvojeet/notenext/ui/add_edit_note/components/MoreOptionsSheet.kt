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
    // Remember SimpleDateFormat for efficient date formatting.
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()) }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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

            val options = mutableListOf(
                lockLabel to lockIcon,
                stringResource(id = R.string.delete) to Icons.Default.Delete,
                stringResource(id = R.string.make_a_copy) to Icons.Default.ContentCopy,
                stringResource(id = R.string.share) to Icons.Default.Share,
                stringResource(id = R.string.labels) to Icons.AutoMirrored.Filled.Label,
                stringResource(id = R.string.save_as) to Icons.Default.Check
            )

            if (!state.editingIsNewNote) {
                options.add(stringResource(id = R.string.history) to Icons.Default.History)
            }

            // Display options in a grid layout.
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxHeight(0.45f)
            ) {
                items(options) { (label, icon) ->
                    MoreOptionsItem(
                        icon = icon,
                        label = label,
                        onClick = {
                            onDismiss() // Dismiss sheet before performing action.
                            when (label) {
                                "Lock", "Unlock" -> onEvent(NotesEvent.OnToggleLockClick)
                                context.getString(R.string.delete) -> showDeleteDialog(true)
                                context.getString(R.string.make_a_copy) -> onEvent(NotesEvent.OnCopyCurrentNoteClick)
                                context.getString(R.string.share) -> {
                                    // Create and launch an Intent for sharing note content.
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, state.editingTitle + "\n\n" + state.editingContent.text)
                                        putExtra(Intent.EXTRA_SUBJECT, state.editingTitle)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }
                                context.getString(R.string.labels) -> onEvent(NotesEvent.OnAddLabelsToCurrentNoteClick)
                                context.getString(R.string.save_as) -> showSaveAsDialog(true)
                                context.getString(R.string.history) -> showHistoryDialog(true)
                            }
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
    Column(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .background(Color.Transparent)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label, // Label serves as content description for accessibility.
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}