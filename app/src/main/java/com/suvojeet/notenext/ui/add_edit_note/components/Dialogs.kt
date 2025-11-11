package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

/**
 * A dialog that allows the user to choose a format (PDF or TXT) to save the current note.
 *
 * @param onDismiss Lambda to be invoked when the dialog is dismissed (e.g., by clicking cancel or outside).
 * @param onSaveAsPdf Lambda to be invoked when the "PDF" save option is selected.
 * @param onSaveAsTxt Lambda to be invoked when the "TXT" save option is selected.
 */
@Composable
fun SaveAsDialog(
    onDismiss: () -> Unit,
    onSaveAsPdf: () -> Unit,
    onSaveAsTxt: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Dialog title.
                Text(text = stringResource(id = R.string.save_note_as), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                // Dialog message.
                Text(stringResource(id = R.string.select_format_to_save_note))
                Spacer(modifier = Modifier.height(24.dp))
                // Action buttons for Cancel, Save as TXT, and Save as PDF.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onSaveAsTxt(); onDismiss() }) {
                        Text(stringResource(id = R.string.txt))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSaveAsPdf(); onDismiss() }) {
                        Text(stringResource(id = R.string.pdf))
                    }
                }
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