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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsSheet(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onDismiss: () -> Unit,
    showDeleteDialog: (Boolean) -> Unit,
    showSaveAsDialog: (Boolean) -> Unit,
    showInsertLinkDialog: (Boolean) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()) }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!state.editingIsNewNote && state.editingLastEdited != 0L) {
                Text(
                    text = "Last edited: ${dateFormat.format(Date(state.editingLastEdited))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Divider()
            }

            val options = listOf(
                "Delete" to Icons.Default.Delete,
                "Make a copy" to Icons.Default.ContentCopy,
                "Share" to Icons.Default.Share,
                "Labels" to Icons.AutoMirrored.Filled.Label,
                "Save as" to Icons.Default.Check,
                "Insert link" to Icons.Default.AddLink
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
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
                            onDismiss()
                            when (label) {
                                "Delete" -> showDeleteDialog(true)
                                "Make a copy" -> onEvent(NotesEvent.OnCopyCurrentNoteClick)
                                "Share" -> {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, state.editingTitle + "\n\n" + state.editingContent.text)
                                        putExtra(Intent.EXTRA_SUBJECT, state.editingTitle)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }
                                "Labels" -> onEvent(NotesEvent.OnAddLabelsToCurrentNoteClick)
                                "Save as" -> showSaveAsDialog(true)
                                "Insert link" -> showInsertLinkDialog(true)
                            }
                        }
                    )
                }
            }
        }
    }
}

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
            contentDescription = label,
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