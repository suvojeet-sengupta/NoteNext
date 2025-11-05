package com.suvojeet.notenext.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suvojeet.notenext.dependency_injection.ViewModelFactory
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreScreen(
    factory: ViewModelFactory,
    onBackClick: () -> Unit
) {
    val viewModel: BackupRestoreViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    var showConfirmDialog by remember { mutableStateOf<Uri?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            showConfirmDialog = it
        }
    }

    if (showConfirmDialog != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            title = { Text("Confirm Restore") },
            text = { Text("Restoring from a backup will delete all your current notes, labels, and projects. This action cannot be undone. Are you sure you want to continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog?.let { viewModel.restoreBackup(it) }
                        showConfirmDialog = null
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restore") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { openDocumentLauncher.launch(arrayOf("application/zip")) },
                enabled = !state.isRestoring
            ) {
                if (state.isRestoring) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Select Backup File")
                }
            }
            state.restoreResult?.let {
                Text(it, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
