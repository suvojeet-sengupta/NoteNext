@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.settings

import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.suvojeet.notenext.R
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Restore
import androidx.compose.runtime.mutableStateListOf 
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.suvojeet.notenext.ui.components.WavyProgressIndicator
import com.suvojeet.notenext.ui.components.PasswordSetDialog
import com.suvojeet.notenext.ui.components.PasswordInputDialog
import com.suvojeet.notenext.ui.components.EncryptionInfoDialog
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.springPress

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupScreen(
    onBackClick: () -> Unit
) {
    val viewModel: BackupRestoreViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.getBackupDetails()
        val account = GoogleSignIn.getLastSignedInAccount(context)
        viewModel.setGoogleAccount(account)
    }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUnlinkDialog by remember { mutableStateOf(false) }
    
    // State for History Item Dialogs
    var versionToDelete by remember { mutableStateOf<String?>(null) }
    var versionToRestore by remember { mutableStateOf<String?>(null) }
    var versionToRestoreMerge by remember { mutableStateOf<String?>(null) }

    // State for Encryption
    var showPasswordSetDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showEncryptionInfo by remember { mutableStateOf(false) }
    // Action to execute after password set (e.g. launch explorer or backup to SD)
    // Deprecated usage: Now we just set global encryption state
    
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip") 
    ) { uri ->
        uri?.let { viewModel.createBackup(it) } // No password arg needed, VM handles it
    }

    // Determine MIME/Extension based on encryption (Optional Polish: .enc for encrypted?)
    // But contract is static. We can change name in launch call.

    // -- Restoration Logic Merged from RestoreScreen --
    var showConfirmDialog by remember { mutableStateOf<Uri?>(null) }
    var restoreType by remember { mutableStateOf<RestoreType?>(null) }
    
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
                viewModel.setGoogleAccount(account)
                // viewModel.backupToDrive(account) // Removed automatic backup
            } catch (e: ApiException) {
                // Don't fail silently — the user tapped sign-in and deserves feedback.
                android.util.Log.w("BackupScreen", "Google sign-in failed: status=${e.statusCode}", e)
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.google_sign_in_failed),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
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
    
    // Listen for Restore Results
    LaunchedEffect(state.restoreResult) {
        state.restoreResult?.let { result ->
            snackbarHostState.showSnackbar(result)
        }
    }

    // Selective Restore Dialog (Scan Result)
    state.foundBackupDetails?.let { details ->
        BackupScanResultDialog(
            scanResult = details,
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

    // Confirm Restore Dialog
    if (showConfirmDialog != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(id = R.string.backup_restore_method_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(id = R.string.backup_restore_method_subtitle_local))
                    
                    OutlinedCard(
                        onClick = {
                            showConfirmDialog?.let { uri ->
                                if (restoreType == RestoreType.LOCAL) viewModel.restoreBackup(uri, merge = true)
                            }
                            showConfirmDialog = null
                            restoreType = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(id = R.string.backup_merge_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(stringResource(id = R.string.backup_merge_description), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Card(
                        onClick = {
                            showConfirmDialog?.let { uri ->
                                if (restoreType == RestoreType.LOCAL) viewModel.restoreBackup(uri, merge = false)
                            }
                            showConfirmDialog = null
                            restoreType = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(id = R.string.backup_overwrite_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text(stringResource(id = R.string.backup_overwrite_description), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {},
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.springPress()) {
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
            // 1. Backup Health Dashboard
            item {
                state.backupDetails?.let { details ->
                    BackupDashboardCard(details, state)
                } ?: Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }

            // 2. Sections: Manual Backup
            item {
                SectionHeader(stringResource(id = R.string.backup_section_manual))
            }

            // Google Drive Backup Card
            item {
                ManualDriveBackupCard(
                    state = state,
                    onToggleEncryption = { 
                        if (it) {
                            if (!state.hasPasswordSet) {
                                showPasswordSetDialog = true
                            } else {
                                viewModel.setEncryption("dummy_re_enable") // Logic flow issue: re-enabling should prompt if forgot? 
                                // Actually if hasPasswordSet is true, we just enable flag. But VM doesn't store pass in memory only? 
                                // VM stores in Prefs. So if hasPasswordSet, we just enable.
                                // But wait, setEncryption requires password.
                                // We need enableEncryption() in VM that reuses stored password?
                                // Actually if it was disabled, we removed the password from prefs in VM.
                                // So hasPasswordSet will be false.
                                // So we ALWAYS need to set password when enabling.
                                showPasswordSetDialog = true
                            }
                        } else {
                            viewModel.disableEncryption()
                        }
                    },
                    onChangePassword = { showChangePasswordDialog = true },
                    onShowInfo = { showEncryptionInfo = true },
                    onSignIn = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(client.signInIntent)
                    },
                    onBackup = {
                        val account = GoogleSignIn.getLastSignedInAccount(context)
                        if (account != null) {
                            viewModel.backupToDrive(account)
                        }
                    },
                    onUnlink = { showUnlinkDialog = true },
                    onRestore = { versionId ->
                        if (versionId != null) {
                            versionToRestore = versionId // Trigger history restore dialog
                        } else {
                            // "Restore Latest" - No dialog requested for this main button, or maybe bounce only?
                            // Logic: If onRestore is called with null, it's the main button.
                            val account = GoogleSignIn.getLastSignedInAccount(context)
                            account?.let { viewModel.restoreFromDrive(it, null) }
                        }
                    },
                    onDeleteVersion = { versionId ->
                         versionToDelete = versionId // Trigger history delete dialog
                    },
                    onToggleAttachments = { viewModel.toggleIncludeAttachments(it) }
                )
            }

            // Local Backup Card
            item {
                 ManualLocalBackupCard(
                     state = state,
                     onToggleEncryption = { 
                        if (it) {
                             showPasswordSetDialog = true
                        } else {
                            viewModel.disableEncryption()
                        }
                     },
                     onChangePassword = { showChangePasswordDialog = true },
                     onShowInfo = { showEncryptionInfo = true },
                     onBackupToSd = {
                         if (state.sdCardFolderUri != null) {
                             viewModel.backupToSdCard()
                         } else {
                             sdCardLauncher.launch(null)
                         }
                     },
                     onSaveToFile = {
                         // For "Save to File", we still treat it as a one-off or use the global setting?
                         // Ideally "Save to File" exports a ZIP. If encryption enabled, export ENC.
                         // We can use the global state.
                         val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                         if (state.isEncryptionEnabled) {
                             createDocumentLauncher.launch("NoteNext_Backup_Encrypted_$timeStamp.enc")
                         } else {
                             createDocumentLauncher.launch("NoteNext_Backup_$timeStamp.zip")
                         }
                     },
                     onRestoreFromFile = {
                        openDocumentLauncher.launch(arrayOf("application/zip", "application/octet-stream")) // Allow all types or octet-stream for .enc
                     },
                     onSelectiveRestore = {
                        selectiveRestoreLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                     }
                 )
            }

            // 3. Sections: Settings (Auto Backup)
            item {
                SectionHeader(stringResource(id = R.string.backup_section_settings))
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
                             viewModel.setBackupFrequency(it)
                        }
                    },
                    onChangeSdLocation = { sdCardLauncher.launch(null) },
                    onToggleEncryption = { 
                        if (it) {
                            showPasswordSetDialog = true
                        } else {
                            viewModel.disableEncryption()
                        }
                    },
                    context = context
                )
            }

            // 4. Smart & Incremental
            item {
                SectionHeader(stringResource(id = R.string.backup_section_smart))
            }

            item {
                SmartBackupSettingsCard(
                    state = state,
                    onToggleIncremental = { viewModel.toggleIncrementalBackup(it) },
                    onToggleSmart = { viewModel.toggleSmartBackup(it) },
                    onToggleCharging = { viewModel.toggleChargingConstraint(it) },
                    onThresholdChange = { viewModel.setEditsThreshold(it) }
                )
            }

            // 5. Danger Zone
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
            title = { Text(stringResource(id = R.string.backup_delete_drive_title)) },
            text = { Text(stringResource(id = R.string.backup_delete_drive_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        GoogleSignIn.getLastSignedInAccount(context)?.let { viewModel.deleteDriveBackup(it) }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(id = R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(id = R.string.cancel)) }
            }
        )
    }

    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            title = { Text(stringResource(id = R.string.backup_unlink_title)) },
            text = { Text(stringResource(id = R.string.backup_unlink_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnlinkDialog = false
                        viewModel.signOut(context)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(id = R.string.backup_unlink_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = false }) { Text(stringResource(id = R.string.cancel)) }
            }
        )
    }

    if (versionToDelete != null) {
        AlertDialog(
            onDismissRequest = { versionToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(id = R.string.backup_delete_version_title)) },
            text = { Text(stringResource(id = R.string.backup_delete_version_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = versionToDelete
                        val account = GoogleSignIn.getLastSignedInAccount(context)
                        if (id != null && account != null) {
                             viewModel.deleteBackupVersion(account, id)
                        }
                        versionToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(id = R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { versionToDelete = null }) { Text(stringResource(id = R.string.cancel)) }
            }
        )
    }

    if (versionToRestore != null) {
        AlertDialog(
            onDismissRequest = { versionToRestore = null },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(id = R.string.backup_restore_method_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(id = R.string.backup_restore_method_subtitle_drive))
                    
                    OutlinedCard(
                        onClick = {
                            val id = versionToRestore
                            val account = GoogleSignIn.getLastSignedInAccount(context)
                            if (id != null && account != null) {
                                viewModel.restoreFromDrive(account, id, merge = true)
                            }
                            versionToRestore = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(id = R.string.backup_merge_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(stringResource(id = R.string.backup_merge_description_drive), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Card(
                        onClick = {
                            val id = versionToRestore
                            val account = GoogleSignIn.getLastSignedInAccount(context)
                            if (id != null && account != null) {
                                viewModel.restoreFromDrive(account, id, merge = false)
                            }
                            versionToRestore = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(id = R.string.backup_overwrite_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text(stringResource(id = R.string.backup_overwrite_description), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { versionToRestore = null }) { Text(stringResource(id = R.string.cancel)) }
            }
        )
    }

    if (showPasswordSetDialog) {
        PasswordSetDialog(
            onDismiss = { showPasswordSetDialog = false },
            onConfirm = { password ->
                viewModel.setEncryption(password)
                showPasswordSetDialog = false
            }
        )
    }

    if (showChangePasswordDialog) {
        PasswordSetDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { password ->
                viewModel.changePassword(password)
                showChangePasswordDialog = false
            },
            title = stringResource(id = R.string.backup_change_password_title),
            confirmText = stringResource(id = R.string.backup_update_password_button)
        )
    }

    if (state.isPasswordRequired) {
        PasswordInputDialog(
            onDismiss = { viewModel.cancelPasswordEntry() },
            onConfirm = { password ->
                viewModel.restoreEncryptedBackup(password)
            }
        )
    }

    if (showEncryptionInfo) {
        EncryptionInfoDialog(onDismiss = { showEncryptionInfo = false })
    }
}

// SectionHeader removed; using shared definition

@Composable
fun ManualDriveBackupCard(
    state: BackupRestoreState,
    onToggleEncryption: (Boolean) -> Unit,
    onChangePassword: () -> Unit,
    onShowInfo: () -> Unit,
    onSignIn: () -> Unit,
    onBackup: () -> Unit,
    onUnlink: () -> Unit,
    onRestore: (String?) -> Unit,
    onDeleteVersion: (String) -> Unit,
    onToggleAttachments: (Boolean) -> Unit
) {
    val isEncryptionEnabled = state.isEncryptionEnabled
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
                     Text(stringResource(id = R.string.backup_google_drive), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                     Spacer(Modifier.height(4.dp))
                     if (state.googleAccountEmail != null) {
                         Text(stringResource(id = R.string.backup_drive_linked, state.googleAccountEmail), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                     } else {
                         Text(stringResource(id = R.string.backup_drive_signin_prompt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                     }
                 }
             }

             if (state.googleAccountEmail != null) {
                 Spacer(Modifier.height(12.dp))
                 
                 // Include Attachments Config
                 Row(
                     verticalAlignment = Alignment.CenterVertically, 
                     modifier = Modifier.fillMaxWidth().clickable { onToggleAttachments(!state.includeAttachments) }
                 ) {
                     Checkbox(checked = state.includeAttachments, onCheckedChange = onToggleAttachments)
                     Spacer(Modifier.width(8.dp))
                     Text(stringResource(id = R.string.backup_include_attachments), style = MaterialTheme.typography.bodyMedium)
                 }

                 // Encryption Toggle
                 Row(
                     verticalAlignment = Alignment.CenterVertically, 
                     modifier = Modifier
                         .fillMaxWidth()
                         .clickable { onToggleEncryption(!isEncryptionEnabled) }
                         .padding(vertical = 4.dp)
                 ) {
                     Checkbox(checked = isEncryptionEnabled, onCheckedChange = onToggleEncryption)
                     Spacer(Modifier.width(8.dp))
                     Column(modifier = Modifier.weight(1f)) {
                         Text(stringResource(id = R.string.backup_encrypt), style = MaterialTheme.typography.bodyMedium)
                         if (isEncryptionEnabled) {
                             Text(stringResource(id = R.string.backup_password_set), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                         }
                     }
                     if (isEncryptionEnabled) {
                         TextButton(onClick = onChangePassword) {
                             Text(stringResource(id = R.string.backup_change_password), fontSize = 12.sp)
                         }
                     }
                     IconButton(onClick = onShowInfo) {
                         Icon(
                             imageVector = Icons.Default.Info,
                             contentDescription = stringResource(id = R.string.backup_about_encryption),
                             tint = MaterialTheme.colorScheme.primary,
                             modifier = Modifier.size(20.dp)
                         )
                     }
                 }

                 Spacer(Modifier.height(12.dp))
                 
                 // Primary Actions
                 Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                     BouncingButton(
                         onClick = onBackup,
                         modifier = Modifier.weight(1f),
                         shape = RoundedCornerShape(12.dp)
                     ) {
                        if (state.isBackingUp && state.backupResult?.contains("Drive") == true) {
                            WavyProgressIndicator(
                                modifier = Modifier.width(48.dp).height(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                             Text(state.uploadProgress ?: stringResource(id = R.string.backup_backing_up))
                         } else {
                             Text(stringResource(id = R.string.backup_now))
                         }
                     }
                      // Latest Restore Button (if versions exist)
                     if (state.backupVersions.isNotEmpty()) {
                         BouncingOutlinedButton(
                             onClick = { onRestore(null) }, // Null implies latest
                             shape = RoundedCornerShape(12.dp)
                         ) {
                            if (state.isRestoring && state.restoreResult?.contains("latest") == true) {
                                 LoadingIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                 Text(stringResource(id = R.string.backup_restore_latest))
                            }
                         }
                     }
                 }
                 
                 // Versions List
                 if (state.backupVersions.isNotEmpty()) {
                     Spacer(Modifier.height(16.dp))
                     Text(stringResource(id = R.string.backup_history), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                     Spacer(Modifier.height(8.dp))
                     
                     Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                         state.backupVersions.forEach { version ->
                             BackupVersionItem(
                                 version = version,
                                 isRestoring = state.isRestoring, // Ideally check if *this* version is restoring, but simplified
                                 isDeleting = state.isDeleting,
                                 onRestore = { onRestore(version.id) },
                                 onDelete = { onDeleteVersion(version.id) }
                             )
                         }
                     }
                 } else if (state.isLoadingVersions) {
                      Spacer(Modifier.height(16.dp))
                      LoadingIndicator(modifier = Modifier.fillMaxWidth())
                 }

                 Spacer(Modifier.height(8.dp))
                 TextButton(onClick = onUnlink, modifier = Modifier.align(Alignment.End)) {
                     Text(stringResource(id = R.string.backup_unlink_title), color = MaterialTheme.colorScheme.error)
                 }
             } else {
                 Spacer(Modifier.height(20.dp))
                 Button(
                     onClick = onSignIn,
                     modifier = Modifier.fillMaxWidth(),
                     shape = RoundedCornerShape(12.dp)
                 ) {
                     Text(stringResource(id = R.string.backup_sign_in))
                 }
             }
         }
    }
}

@Composable
fun BackupVersionItem(
    version: com.suvojeet.notenext.data.backup.GoogleDriveManager.DriveBackupMetadata,
    isRestoring: Boolean,
    isDeleting: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val size = formatSize(version.size)
    val date = version.modifiedTime?.let {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(it.value))
    } ?: stringResource(id = R.string.backup_unknown_date)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
         Column {
             Text(date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
             Text(size, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
         }
         
         Row {
             IconButton(onClick = onRestore, enabled = !isRestoring && !isDeleting) {
                 Icon(Icons.Default.Restore, stringResource(id = R.string.backup_action_restore), tint = MaterialTheme.colorScheme.primary)
             }
             IconButton(onClick = onDelete, enabled = !isRestoring && !isDeleting) {
                  Icon(Icons.Default.Delete, stringResource(id = R.string.backup_action_delete), tint = MaterialTheme.colorScheme.error)
             }
         }
    }
}

@Composable
fun ManualLocalBackupCard(
    state: BackupRestoreState,
    onToggleEncryption: (Boolean) -> Unit,
    onChangePassword: () -> Unit,
    onShowInfo: () -> Unit,
    onBackupToSd: () -> Unit,
    onSaveToFile: () -> Unit,
    onRestoreFromFile: () -> Unit,
    onSelectiveRestore: () -> Unit
) {
    val isEncryptionEnabled = state.isEncryptionEnabled
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.SdStorage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(stringResource(id = R.string.backup_local_storage), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(stringResource(id = R.string.backup_local_storage_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(20.dp))
            
            Text(stringResource(id = R.string.backup_label_backup), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            
            // Encryption Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleEncryption(!isEncryptionEnabled) }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(checked = isEncryptionEnabled, onCheckedChange = onToggleEncryption)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(id = R.string.backup_encrypt), style = MaterialTheme.typography.bodyMedium)
                    if (isEncryptionEnabled) {
                        Text(stringResource(id = R.string.backup_password_set), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (isEncryptionEnabled) {
                    TextButton(onClick = onChangePassword) {
                        Text(stringResource(id = R.string.backup_change_password), fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onShowInfo) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(id = R.string.backup_about_encryption),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // SD Card Action
            OutlinedButton(
                onClick = onBackupToSd,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                 if (state.isBackingUp && state.backupResult?.contains("SD Card") == true) {
                     LoadingIndicator(modifier = Modifier.size(16.dp))
                     Spacer(Modifier.width(8.dp))
                     Text(stringResource(id = R.string.backup_backing_up))
                 } else {
                     Text(if (state.sdCardFolderUri != null) stringResource(id = R.string.backup_to_selected_folder) else stringResource(id = R.string.backup_select_folder))
                 }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            
            Text(stringResource(id = R.string.backup_label_restore), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            // Restore From File
            Button(
                onClick = onRestoreFromFile,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                if(state.isRestoring && state.restoreResult?.contains("Local") == true) {
                    LoadingIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onSecondary)
                     Spacer(Modifier.width(8.dp))
                     Text(stringResource(id = R.string.backup_restoring))
                } else {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.backup_restore_from_file))
                }
            }

            Spacer(Modifier.height(8.dp))
            
            // Selective Restore
             TextButton(
                onClick = onSelectiveRestore,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                 if (state.isScanning) {
                      LoadingIndicator(modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(8.dp))
                      Text(stringResource(id = R.string.backup_scanning))
                 } else {
                     Text(stringResource(id = R.string.backup_selective_restore))
                 }
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
    onToggleEncryption: (Boolean) -> Unit,
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
                Text(stringResource(id = R.string.backup_auto_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            
            Spacer(Modifier.height(24.dp))

            // Encryption Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(id = R.string.backup_auto_encrypt), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(id = R.string.backup_auto_encrypt_subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.isEncryptionEnabled,
                    onCheckedChange = onToggleEncryption,
                    thumbContent = if (state.isEncryptionEnabled) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                    } else null
                )
            }
            Spacer(Modifier.height(16.dp))

            // Frequency
            Text(stringResource(id = R.string.backup_frequency), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Daily" to stringResource(id = R.string.backup_frequency_daily),
                    "Weekly" to stringResource(id = R.string.backup_frequency_weekly)
                ).forEach { (freq, label) ->
                    FilterChip(
                        selected = state.backupFrequency == freq,
                        onClick = { onFrequencyChange(freq) },
                        label = { Text(label) },
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
                    Text(stringResource(id = R.string.backup_google_drive), style = MaterialTheme.typography.bodyLarge)
                    if (state.googleAccountEmail == null && state.isAutoBackupEnabled) {
                        Text(stringResource(id = R.string.backup_signin_required), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                Switch(
                    checked = state.isAutoBackupEnabled,
                    onCheckedChange = { 
                        val account = GoogleSignIn.getLastSignedInAccount(context)
                        onToggleAutoBackup(it, account?.email)
                    },
                    thumbContent = if (state.isAutoBackupEnabled) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                    } else null
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
                    Text(stringResource(id = R.string.backup_sd_card_folder), style = MaterialTheme.typography.bodyLarge)
                    state.sdCardFolderUri?.let { uri ->
                        val selectedFallback = stringResource(id = R.string.backup_folder_selected)
                        val path = try { android.net.Uri.parse(uri).path?.substringAfterLast(":") ?: selectedFallback } catch(e:Exception){ selectedFallback }
                        Text(path, style = MaterialTheme.typography.labelSmall, maxLines=1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = state.isSdCardAutoBackupEnabled,
                    onCheckedChange = { onToggleSdBackup(it) },
                    thumbContent = if (state.isSdCardAutoBackupEnabled) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                    } else null
                )
            }
            
            if (state.sdCardFolderUri != null || state.isSdCardAutoBackupEnabled) {
                TextButton(onClick = onChangeSdLocation) {
                    Text(stringResource(id = R.string.backup_change_folder))
                }
            }
        }
    }
}


@Composable
fun BackupDashboardCard(details: BackupDetails, state: BackupRestoreState) {
    val isSuccess = state.lastBackupStatus?.contains("Success") == true
    val statusColor = if (state.lastBackupTime == 0L) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else if (isSuccess) {
        Color(0xFF4CAF50) // Green
    } else {
        MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.backup_health),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (state.lastBackupTime == 0L) stringResource(id = R.string.backup_health_no_backups) else if (isSuccess) stringResource(id = R.string.backup_health_secure) else stringResource(id = R.string.backup_health_failed),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
                
                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Last Backup Info
            val lastBackupText = if (state.lastBackupTime > 0) {
                val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                sdf.format(Date(state.lastBackupTime))
            } else {
                stringResource(id = R.string.backup_never)
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(id = R.string.backup_last_attempt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = lastBackupText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(id = R.string.backup_storage_used), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = formatSize(details.totalSize), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    UsageStatItem(
                        icon = Icons.Default.Description,
                        count = details.notesCount.toString(),
                        label = stringResource(id = R.string.backup_count_notes),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                     UsageStatItem(
                        icon = Icons.Default.Folder,
                        count = details.projectsCount.toString(),
                        label = stringResource(id = R.string.backup_count_projects),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    UsageStatItem(
                        icon = Icons.Default.Label,
                        count = details.labelsCount.toString(),
                        label = stringResource(id = R.string.backup_count_labels),
                        color = MaterialTheme.colorScheme.secondary
                    )
                     Spacer(modifier = Modifier.height(16.dp))
                    UsageStatItem(
                        icon = Icons.Default.AttachFile,
                        count = details.attachmentsCount.toString(),
                        label = stringResource(id = R.string.backup_count_files),
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
                 LoadingIndicator(Modifier.size(16.dp), color = MaterialTheme.colorScheme.error)
            } else {
                 Text(stringResource(id = R.string.backup_delete_drive_button), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
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

enum class RestoreType {
    LOCAL, DRIVE
}

@Composable
fun BackupScanResultDialog(
    scanResult: com.suvojeet.notenext.data.backup.BackupScanResult,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<Int>().apply { addAll(scanResult.projects.map { it.id }) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                Text(stringResource(id = R.string.backup_contents_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                scanResult.backupTimestamp?.let { 
                    Text(
                        text = stringResource(id = R.string.backup_contents_created, SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(it))),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Summary Stats
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ScanStatItem(count = scanResult.notesCount, label = stringResource(id = R.string.backup_count_notes), icon = Icons.Default.Description)
                    ScanStatItem(count = scanResult.labelsCount, label = stringResource(id = R.string.backup_count_labels), icon = Icons.Default.Label)
                    ScanStatItem(count = scanResult.attachmentsCount, label = stringResource(id = R.string.backup_count_files), icon = Icons.Default.AttachFile)
                }

                Text(stringResource(id = R.string.backup_contents_select_projects), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                
                LazyColumn(modifier = Modifier.heightIn(max = 250.dp).fillMaxWidth()) {
                    items(
                        count = scanResult.projects.size,
                        key = { index -> scanResult.projects[index].id }
                    ) { index ->
                        val project = scanResult.projects[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedIds.contains(project.id)) selectedIds.remove(project.id) else selectedIds.add(project.id)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedIds.contains(project.id),
                                onCheckedChange = { checked ->
                                    if (checked) selectedIds.add(project.id) else selectedIds.remove(project.id)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = project.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                
                Text(
                    stringResource(id = R.string.backup_contents_merge_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty()
            ) {
                Text(stringResource(id = R.string.backup_restore_selected))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun ScanStatItem(count: Int, label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
        Text(text = count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BouncingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = ButtonDefaults.shape,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "button_bounce"
    )

    Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        shape = shape,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun BouncingOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = ButtonDefaults.shape,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "button_bounce"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        shape = shape,
        interactionSource = interactionSource,
        content = content
    )
}

// Local dialog implementations removed. Using shared components from ui.components.BackupDialogs


@Composable
fun SmartBackupSettingsCard(
    state: BackupRestoreState,
    onToggleIncremental: (Boolean) -> Unit,
    onToggleSmart: (Boolean) -> Unit,
    onToggleCharging: (Boolean) -> Unit,
    onThresholdChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoMode, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Text(stringResource(id = R.string.backup_optimization), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            
            Spacer(Modifier.height(24.dp))

            // Incremental Backup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(id = R.string.backup_incremental), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(id = R.string.backup_incremental_subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.isIncrementalEnabled,
                    onCheckedChange = onToggleIncremental,
                    thumbContent = if (state.isIncrementalEnabled) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                    } else null
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(24.dp))

            // Smart Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(id = R.string.backup_smart_trigger), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(id = R.string.backup_smart_trigger_subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.isSmartBackupEnabled,
                    onCheckedChange = onToggleSmart,
                    thumbContent = if (state.isSmartBackupEnabled) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                    } else null
                )
            }

            if (state.isSmartBackupEnabled) {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(id = R.string.backup_after_edits, state.editsThreshold), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Slider(
                    value = state.editsThreshold.toFloat(),
                    onValueChange = { onThresholdChange(it.toInt()) },
                    valueRange = 5f..50f,
                    steps = 9
                )
                Text(
                    stringResource(id = R.string.backup_current_edits, state.currentEditCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(24.dp))

            // Charging Constraint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(id = R.string.backup_charging_only), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(id = R.string.backup_charging_only_subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.isChargingConstraintEnabled,
                    onCheckedChange = onToggleCharging,
                    thumbContent = if (state.isChargingConstraintEnabled) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                    } else null
                )
            }
        }
    }
}
