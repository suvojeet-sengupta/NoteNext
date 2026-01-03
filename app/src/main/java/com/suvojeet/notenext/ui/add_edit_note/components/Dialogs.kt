package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

/**
 * A redesigned dialog that allows the user to choose a format (PDF, TXT, or Markdown) to export the current note.
 * Features a modern card-based design with icons and format descriptions.
 *
 * @param onDismiss Lambda to be invoked when the dialog is dismissed.
 * @param onSaveAsPdf Lambda to be invoked when the "PDF" save option is selected.
 * @param onSaveAsTxt Lambda to be invoked when the "TXT" save option is selected.
 * @param onSaveAsMd Lambda to be invoked when the "Markdown" save option is selected.
 */
@Composable
fun SaveAsDialog(
    onDismiss: () -> Unit,
    onSaveAsPdf: () -> Unit,
    onSaveAsTxt: () -> Unit,
    onSaveAsMd: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Text(
                    text = stringResource(id = R.string.save_note_as),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.select_format_to_save_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Format Options
                SaveFormatOption(
                    icon = Icons.Default.PictureAsPdf,
                    title = "PDF Document",
                    description = "Best for printing & sharing with formatting",
                    iconColor = Color(0xFFE53935), // Red
                    onClick = { onSaveAsPdf(); onDismiss() }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SaveFormatOption(
                    icon = Icons.Default.Description,
                    title = "Plain Text (.txt)",
                    description = "Simple text, works everywhere",
                    iconColor = Color(0xFF43A047), // Green
                    onClick = { onSaveAsTxt(); onDismiss() }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SaveFormatOption(
                    icon = Icons.Default.Code,
                    title = "Markdown (.md)",
                    description = "For developers & note-taking apps",
                    iconColor = Color(0xFF1E88E5), // Blue
                    onClick = { onSaveAsMd(); onDismiss() }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cancel Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun SaveFormatOption(
    icon: ImageVector,
    title: String,
    description: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A dialog for adding a label to the current note. Users can type a new label
 * or select from a list of existing labels.
 *
 * Note: This [LabelDialog] is specifically for the add/edit note context and
 * differs from `ui/components/LabelDialog.kt` which is for general label management.
 *
 * @param labels A list of existing labels to display for selection.
 * @param onDismiss Lambda to be invoked when the dialog is dismissed.
 * @param onConfirm Lambda to be invoked when a label is confirmed (either new or selected).
 *                  The confirmed label string is passed as a parameter.
 */
@Composable
fun LabelDialog(
    labels: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newLabel by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.add_label)) },
        text = {
            Column {
                // Input field for a new label.
                OutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    label = { Text(stringResource(id = R.string.new_label)) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                // List of existing labels.
                LazyColumn {
                    items(labels) { label ->
                        Text(
                            text = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onConfirm(label) } // Select existing label.
                                .padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // If a new label is typed, confirm it. Otherwise, just dismiss.
                    if (newLabel.isNotBlank()) {
                        onConfirm(newLabel)
                    }
                    onDismiss() // Always dismiss the dialog after confirmation attempt.
                }
            ) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

/**
 * A dialog for inserting a web link into the note content.
 *
 * @param onDismiss Lambda to be invoked when the dialog is dismissed.
 * @param onConfirm Lambda to be invoked when a URL is confirmed. The URL string is passed as a parameter.
 */
@Composable
fun InsertLinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.insert_link)) },
        text = {
            // Input field for the URL.
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(id = R.string.url)) },
                placeholder = { Text(stringResource(id = R.string.example_url)) }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // If a URL is entered, confirm it. Otherwise, just dismiss.
                    if (url.isNotBlank()) {
                        onConfirm(url)
                    }
                    onDismiss() // Always dismiss the dialog after confirmation attempt.
                }
            ) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}