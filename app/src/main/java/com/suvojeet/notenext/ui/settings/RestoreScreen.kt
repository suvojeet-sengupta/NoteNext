package com.suvojeet.notenext.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
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
                enabled = !state.isRestoring,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isRestoring && state.restoreResult?.contains("Local") == true) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(id = R.string.select_backup_file) + " (Local)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            val googleSignInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                        showConfirmDialog = Uri.parse("google_drive") // Mock URI to trigger dialog
                        // Store account temporarily or pass it directly if dialog structure allows.
                        // Since dialog expects URI, let's just launch restoration directly or modify dialog.
                        // For now, let's launch directly to keep it simple, or better, show specific confirmation.
                        viewModel.restoreFromDrive(account) 
                    } catch (e: com.google.android.gms.common.api.ApiException) {
                        // Handle error
                    }
                }
            }

            Button(
                onClick = {
                     val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(
                                com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE),
                                com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_APPDATA)
                            )
                            .build()
                        val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(client.signInIntent)
                },
                enabled = !state.isRestoring,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                if (state.isRestoring && state.restoreResult?.contains("Drive") == true) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                     Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore from Google Drive")
                }
            }
            state.restoreResult?.let {
                Text(it, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
