package com.suvojeet.notenext.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.ui.draw.scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.suvojeet.notenext.R
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBackClick: () -> Unit
) {
    val viewModel: BackupRestoreViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.getBackupDetails()
        val account = GoogleSignIn.getLastSignedInAccount(context)
        viewModel.setGoogleAccount(account)
    }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUnlinkDialog by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.createBackup(it) }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.setGoogleAccount(account)
                viewModel.backupToDrive(account)
            } catch (e: ApiException) { }
        }
    }

    val sdCardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.setSdCardLocation(it) }
    }

    // Snackbar Host State
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Listen for Backup Results
    LaunchedEffect(state.backupResult) {
        state.backupResult?.let { result ->
            snackbarHostState.showSnackbar(result)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.backup_title)) },
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
            // 1. Storage Summary
            item {
                state.backupDetails?.let { details ->
                    StorageUsageCard(details)
                } ?: Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // 2. Sections: Manual Backup
            item {
                SectionHeader("Manual Backup")
            }

            // Google Drive Backup Card
            item {
                ManualDriveBackupCard(
                    state = state,
                    onSignInBackup = {
                        val account = GoogleSignIn.getLastSignedInAccount(context)
                        if (account != null) {
                             viewModel.backupToDrive(account)
                        } else {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
                                .build()
                            val client = GoogleSignIn.getClient(context, gso)
                            googleSignInLauncher.launch(client.signInIntent)
                        }
                    },
                    onUnlink = { showUnlinkDialog = true },
                    onRestore = {
                        val account = GoogleSignIn.getLastSignedInAccount(context)
                        account?.let { viewModel.restoreFromDrive(it) }
                    }
                )
            }

            // Local Backup Card
            item {
                 ManualLocalBackupCard(
                     state = state,
                     onBackupToSd = {
                         if (state.sdCardFolderUri != null) {
                             viewModel.backupToSdCard()
                         } else {
                             sdCardLauncher.launch(null)
                         }
                     },
                     onSaveToFile = {
                         val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                         createDocumentLauncher.launch("NoteNext_Backup_$timeStamp.zip")
                     }
                 )
            }

            // 3. Sections: Settings (Auto Backup)
            item {
                SectionHeader("Backup Settings")
            }

            item {
                AutoBackupSettingsCard(
                    state = state,
                    onToggleAutoBackup = { enabled, account -> 
                         viewModel.toggleAutoBackup(enabled, account, state.backupFrequency)
                    },
                    onToggleSdBackup = { viewModel.toggleSdCardAutoBackup(it) },
                    onFrequencyChange = { 
                        GoogleSignIn.getLastSignedInAccount(context)?.email?.let { email ->
                             viewModel.toggleAutoBackup(state.isAutoBackupEnabled, email, it)
                        } ?: run {
                             // Just update pref if not signed in / disabled
                             val sharedPrefs = context.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
                             sharedPrefs.edit().putString("backup_frequency", it).apply()
                        }
                    },
                    onChangeSdLocation = { sdCardLauncher.launch(null) },
                    context = context
                )
            }

            // 4. Danger Zone
            item {
                if (state.driveBackupExists) {
                    Spacer(Modifier.height(8.dp))
                    DeleteBackupCard(
                        isLoading = state.isDeleting,
                        onClick = { showDeleteDialog = true }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Drive Backup") },
            text = { Text("Are you sure you want to permanently delete the backup from Google Drive? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        GoogleSignIn.getLastSignedInAccount(context)?.let { viewModel.deleteDriveBackup(it) }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            title = { Text("Unlink Account") },
            text = { Text("Unlinking will stop automatic backups to Drive. Local backups will not be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnlinkDialog = false
                        viewModel.signOut(context)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Unlink") }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// SectionHeader removed; using shared definition

@Composable
fun ManualDriveBackupCard(
    state: BackupRestoreState,
    onSignInBackup: () -> Unit,
    onUnlink: () -> Unit,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
         Column(modifier = Modifier.padding(20.dp)) {
             Row(verticalAlignment = Alignment.Top) {
                 Icon(
                     imageVector = Icons.Default.CloudUpload,
                     contentDescription = null,
                     tint = MaterialTheme.colorScheme.primary,
                     modifier = Modifier.size(28.dp)
                 )
                 Spacer(Modifier.width(16.dp))
                 Column {
                     Text("Google Drive", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                     Spacer(Modifier.height(4.dp))
                     if (state.googleAccountEmail != null) {
                         Text("Linked: ${state.googleAccountEmail}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                         
                         state.driveBackupMetadata?.let { meta ->
                             val size = formatSize(meta.size)
                             val date = meta.modifiedTime?.let {
                                  SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(it.value))
                             } ?: "Unknown"
                             Spacer(Modifier.height(4.dp))
                             Text("Last Backup: $date ($size)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                         }
                     } else {
                         Text("Sign in to backup your data to the cloud.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                     }
                 }
             }

             Spacer(Modifier.height(20.dp))

             if (state.googleAccountEmail != null) {
                 Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                     Button(
                         onClick = onSignInBackup,
                         modifier = Modifier.weight(1f),
                         shape = RoundedCornerShape(12.dp)
                     ) {
                         if (state.isBackingUp && state.backupResult?.contains("Drive") == true) {
                             CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                             Spacer(Modifier.width(8.dp))
                             Text(state.uploadProgress ?: "Backing up...")
                         } else {
                             Text("Backup Now")
                         }
                     }
                     if (state.driveBackupExists) {
                         OutlinedButton(onClick = onRestore, shape = RoundedCornerShape(12.dp)) {
                             Text("Restore")
                         }
                     }
                 }
                 Spacer(Modifier.height(8.dp))
                 TextButton(onClick = onUnlink, modifier = Modifier.align(Alignment.End)) {
                     Text("Unlink Account", color = MaterialTheme.colorScheme.error)
                 }
             } else {
                 Button(
                     onClick = onSignInBackup,
                     modifier = Modifier.fillMaxWidth(),
                     shape = RoundedCornerShape(12.dp)
                 ) {
                     Text("Sign In & Backup")
                 }
             }
         }
    }
}

@Composable
fun ManualLocalBackupCard(
    state: BackupRestoreState,
    onBackupToSd: () -> Unit,
    onSaveToFile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SdStorage, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Local Backup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Save to device storage or SD card", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(20.dp))
            
            // SD Card Action
            OutlinedButton(
                onClick = onBackupToSd,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                 if (state.isBackingUp && state.backupResult?.contains("SD Card") == true) {
                     CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                     Spacer(Modifier.width(8.dp))
                     Text("Backing up...")
                 } else {
                     Text(if (state.sdCardFolderUri != null) "Backup to Selected Folder" else "Select Folder & Backup")
                 }
            }
            
            Spacer(Modifier.height(12.dp))

            // Save As Action
            OutlinedButton(
                onClick = onSaveToFile,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                 Text("Save as .zip File")
            }
            
            if (state.backupResult != null && (state.backupResult.contains("Local") || state.backupResult.contains("SD Card"))) {
                 Spacer(Modifier.height(12.dp))
                 Text(
                     text = state.backupResult, 
                     style = MaterialTheme.typography.bodySmall, 
                     color = if (state.backupResult.contains("failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                 )
            }
        }
    }
}

@Composable
fun AutoBackupSettingsCard(
    state: BackupRestoreState,
    onToggleAutoBackup: (Boolean, String?) -> Unit,
    onToggleSdBackup: (Boolean) -> Unit,
    onFrequencyChange: (String) -> Unit,
    onChangeSdLocation: () -> Unit,
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
             Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Text("Automatic Backup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            
            Spacer(Modifier.height(24.dp))

            // Frequency
            Text("Frequency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Daily", "Weekly").forEach { freq ->
                    FilterChip(
                        selected = state.backupFrequency == freq,
                        onClick = { onFrequencyChange(freq) },
                        label = { Text(freq) },
                        leadingIcon = if (state.backupFrequency == freq) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            // Google Drive Toggle
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Google Drive", style = MaterialTheme.typography.bodyLarge)
                    if (state.googleAccountEmail == null && state.isAutoBackupEnabled) {
                        Text("Sign in required", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                Switch(
                    checked = state.isAutoBackupEnabled,
                    onCheckedChange = { 
                        val account = GoogleSignIn.getLastSignedInAccount(context)
                        onToggleAutoBackup(it, account?.email)
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // SD Card Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("SD Card / Local Folder", style = MaterialTheme.typography.bodyLarge)
                    state.sdCardFolderUri?.let { uri ->
                        val path = try { android.net.Uri.parse(uri).path?.substringAfterLast(":") ?: "Selected" } catch(e:Exception){"Selected"}
                        Text(path, style = MaterialTheme.typography.labelSmall, maxLines=1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = state.isSdCardAutoBackupEnabled,
                    onCheckedChange = { onToggleSdBackup(it) }
                )
            }
            
            if (state.sdCardFolderUri != null || state.isSdCardAutoBackupEnabled) {
                TextButton(onClick = onChangeSdLocation) {
                    Text("Change Folder")
                }
            }
        }
    }
}


@Composable
fun StorageUsageCard(details: BackupDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Usage",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatSize(details.totalSize),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    UsageStatItem(
                        icon = Icons.Default.Description,
                        count = details.notesCount.toString(),
                        label = stringResource(id = R.string.notes_count),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                     UsageStatItem(
                        icon = Icons.Default.Folder,
                        count = details.projectsCount.toString(),
                        label = stringResource(id = R.string.projects_count),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    UsageStatItem(
                        icon = Icons.Default.Label,
                        count = details.labelsCount.toString(),
                        label = stringResource(id = R.string.labels_count),
                        color = MaterialTheme.colorScheme.secondary
                    )
                     Spacer(modifier = Modifier.height(16.dp))
                    UsageStatItem(
                        icon = Icons.Default.AttachFile,
                        count = details.attachmentsCount.toString(),
                        label = stringResource(id = R.string.attachments_count),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun UsageStatItem(
    icon: ImageVector,
    count: String,
    label: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = count,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DeleteBackupCard(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            if (isLoading) {
                 CircularProgressIndicator(Modifier.size(16.dp), color = MaterialTheme.colorScheme.error)
            } else {
                 Text("Delete Backup from Drive", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$size B"
    }
}
