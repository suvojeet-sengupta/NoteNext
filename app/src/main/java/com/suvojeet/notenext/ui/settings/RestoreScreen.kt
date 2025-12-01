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
import androidx.hilt.navigation.compose.hiltViewModel
import android.net.Uri
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreScreen(
    onBackClick: () -> Unit
) {
    val viewModel: BackupRestoreViewModel = hiltViewModel()
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
            title = { Text(stringResource(id = R.string.confirm_restore)) },
            text = { Text(stringResource(id = R.string.restore_confirmation_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog?.let { viewModel.restoreBackup(it) }
                        showConfirmDialog = null
                    }
                ) {
                    Text(stringResource(id = R.string.restore_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.restore_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
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
                    Text(stringResource(id = R.string.select_backup_file))
                }
            }
            state.restoreResult?.let {
                Text(it, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
