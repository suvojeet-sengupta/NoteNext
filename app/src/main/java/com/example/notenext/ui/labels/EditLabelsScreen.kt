package com.example.notenext.ui.labels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notenext.data.Label
import com.example.notenext.dependency_injection.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLabelsScreen(
    factory: ViewModelFactory,
    onBackPressed: () -> Unit
) {
    val viewModel: EditLabelsViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Labels") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(EditLabelsEvent.ShowAddLabelDialog) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Label")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(state.labels) { label ->
                LabelItem(
                    label = label,
                    onEditClick = { viewModel.onEvent(EditLabelsEvent.ShowEditLabelDialog(label)) }
                )
            }
        }

        if (state.showAddLabelDialog) {
            AddLabelDialog(
                onDismiss = { viewModel.onEvent(EditLabelsEvent.HideDialog) },
                onConfirm = { name ->
                    viewModel.onEvent(EditLabelsEvent.AddLabel(name))
                }
            )
        }

        if (state.showEditLabelDialog) {
            state.selectedLabel?.let { label ->
                EditLabelDialog(
                    label = label,
                    onDismiss = { viewModel.onEvent(EditLabelsEvent.HideDialog) },
                    onConfirm = { newName ->
                        viewModel.onEvent(EditLabelsEvent.UpdateLabel(label, newName))
                    },
                    onDelete = {
                        viewModel.onEvent(EditLabelsEvent.DeleteLabel(label))
                    }
                )
            }
        }
    }
}

@Composable
fun LabelItem(
    label: Label,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label.name, modifier = Modifier.weight(1f))
        Icon(Icons.Default.Edit, contentDescription = "Edit Label")
    }
}

@Composable
fun AddLabelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Label") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Label Name") }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditLabelDialog(
    label: Label,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(label.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Label") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Label Name") }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
