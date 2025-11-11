package com.suvojeet.notenext.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R

/**
 * A dialog for managing labels, allowing users to create new labels or select existing ones.
 *
 * @param labels A list of existing labels to display.
 * @param onDismiss Lambda to be invoked when the dialog is dismissed.
 * @param onConfirm Lambda to be invoked when a label is created or selected.
 *                  The selected/created label string is passed as a parameter.
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
        title = { Text(text = stringResource(id = R.string.manage_labels)) },
        text = {
            Column {
                // Input field for creating a new label and a "Create" button.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        label = { Text(stringResource(id = R.string.new_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (newLabel.isNotBlank()) {
                                onConfirm(newLabel)
                                newLabel = "" // Clear the input after creating
                            }
                        }
                    ) {
                        Text(stringResource(id = R.string.create))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // List of existing labels.
                LazyColumn {
                    items(labels) { label ->
                        ListItem(
                            headlineContent = { Text(text = label) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Label,
                                    contentDescription = stringResource(id = R.string.label_icon)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onConfirm(label)
                                    onDismiss()
                                }
                        )
                    }
                }
            }
        },
        confirmButton = { /* No confirm button needed here, actions are within content */ },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
