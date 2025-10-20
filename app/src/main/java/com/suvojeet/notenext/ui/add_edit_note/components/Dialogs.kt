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
                Text(text = "Save note as", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select format to save the note in Documents folder.")
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onSaveAsTxt(); onDismiss() }) {
                        Text("TXT")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSaveAsPdf(); onDismiss() }) {
                        Text("PDF")
                    }
                }
            }
        }
    }
}

@Composable
fun LabelDialog(
    labels: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newLabel by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Label") },
        text = {
            Column {
                OutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    label = { Text("New Label") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(labels) { label ->
                        Text(
                            text = label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onConfirm(label) }
                                .padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newLabel.isNotBlank()) {
                        onConfirm(newLabel)
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}