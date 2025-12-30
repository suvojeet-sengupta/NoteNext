package com.suvojeet.notenext.ui.settings

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.Project

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreScreen(
    onBackClick: () -> Unit
) {
    val viewModel: BackupRestoreViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    var showConfirmDialog by remember { mutableStateOf<Uri?>(null) }
    var restoreType by remember { mutableStateOf<RestoreType?>(null) }
    val context = LocalContext.current

    // Launcher for Local Restore (All)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            restoreType = RestoreType.LOCAL
            showConfirmDialog = it
        }
    }

    // Launcher for Selective Restore (Scan first)
    var selectedBackupUri by remember { mutableStateOf<Uri?>(null) }
    val selectiveRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedBackupUri = it
            viewModel.scanBackup(it)
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                 val account = task.getResult(ApiException::class.java)
                 viewModel.restoreFromDrive(account)
            } catch (e: ApiException) { }
        }
    }

    // Selective Restore Dialog
    if (state.foundProjects.isNotEmpty()) {
        ProjectSelectionDialog(
            projects = state.foundProjects,
            onDismiss = { 
                viewModel.clearFoundProjects() 
                selectedBackupUri = null
            },
            onConfirm = { selectedIds ->
                selectedBackupUri?.let { uri ->
                    viewModel.restoreSelectedProjects(uri, selectedIds)
                }
                viewModel.clearFoundProjects()
                selectedBackupUri = null
            }
        )
    }

    if (showConfirmDialog != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(id = R.string.confirm_restore)) },
            text = { Text(stringResource(id = R.string.restore_confirmation_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog?.let { uri ->
                             if (restoreType == RestoreType.LOCAL) {
                                 viewModel.restoreBackup(uri)
                             }
                        }
                        showConfirmDialog = null
                        restoreType = null
                    }
                ) {
                    Text(stringResource(id = R.string.restore_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showConfirmDialog = null 
                    restoreType = null
                }) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
              item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                     Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Restoring will replace all current data inside the app. Please make sure you have a backup of your current data before proceeding.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            item {
                SectionHeader("Restore Sources")
            }

            // Google Drive Restore
            item {
                RestoreSourceCard(
                    title = "Google Drive",
                    subtitle = "Restore all data from Google Drive",
                    icon = Icons.Default.CloudDownload,
                    buttonText = "Restore from Drive",
                    isLoading = state.isRestoring && state.restoreResult?.contains("Drive") == true,
                    isPrimary = true,
                    onClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(client.signInIntent)
                    }
                )
            }

            // Local Restore
            item {
                RestoreSourceCard(
                    title = "Local File",
                    subtitle = "Restore all data from a local .zip file",
                    icon = Icons.Default.Archive,
                    buttonText = "Select File",
                    isLoading = state.isRestoring && state.restoreResult?.contains("Local") == true,
                    onClick = { openDocumentLauncher.launch(arrayOf("application/zip")) }
                )
            }
            
            // Selective Restore
            item {
                 RestoreSourceCard(
                    title = "Selective Restore",
                    subtitle = "Choose specific projects to restore from local backup",
                    icon = Icons.Default.CheckCircle, 
                    buttonText = "Scan Backup File",
                    isLoading = state.isScanning,
                    onClick = { 
                        selectiveRestoreLauncher.launch(arrayOf("application/zip"))
                    }
                )
             }

            item {
                 state.restoreResult?.let { result ->
                     Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (result.contains("failed", ignoreCase = true)) Icons.Default.Error else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (result.contains("failed", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestoreSourceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    buttonText: String,
    isLoading: Boolean,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
     Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
             containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                     tint = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                 )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = if (isPrimary) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) else ButtonDefaults.outlinedButtonColors(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                     CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                     Spacer(modifier = Modifier.width(8.dp))
                     Text("Processing...")
                } else {
                     Text(buttonText)
                }
            }
        }
    }
}

@Composable
fun ProjectSelectionDialog(
    projects: List<Project>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<Int>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Projects to Restore") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(projects) { project ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedIds.contains(project.id)) {
                                    selectedIds.remove(project.id)
                                } else {
                                    selectedIds.add(project.id)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedIds.contains(project.id),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedIds.add(project.id)
                                } else {
                                    selectedIds.remove(project.id)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = project.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty()
            ) {
                Text("Restore Selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// SectionHeader removed; using shared definition

enum class RestoreType {
    LOCAL, DRIVE
}
