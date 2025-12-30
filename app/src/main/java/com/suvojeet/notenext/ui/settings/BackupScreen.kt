package com.suvojeet.notenext.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
        viewModel.getBackupDetails()
        val account = GoogleSignIn.getLastSignedInAccount(context)
        viewModel.setGoogleAccount(account)
    }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUnlinkDialog by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            viewModel.createBackup(it)
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
                viewModel.backupToDrive(account)
            } catch (e: ApiException) {
                // Handle error or log it
            }
        }
    }

            val sdCardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.setSdCardLocation(it)
        }
    }

    Scaffold(
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                state.backupDetails?.let { details ->
                    StorageUsageCard(details)
                } ?: run {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            item {
                Text(
                    text = "Backup Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }
            
            item {
                 AutoBackupCard(
                    isEnabled = state.isAutoBackupEnabled || state.isSdCardAutoBackupEnabled,
                    frequency = state.backupFrequency,
                    onToggle = { enabled -> 
                        // This logic is a bit complex now with two toggles.
                        // Let's simplify: This card now controls the frequency. 
                        // The individual toggles for Drive and SD Card should probably be in their respective cards or a dedicated section.
                        // But for now, let's keep it simple: Changing frequency affects both if enabled.
                        // But the master toggle here is ambiguous.
                        // Let's change the AutoBackupCard to be just a "Backup Frequency" setting, and move toggles to specific cards.
                        // OR, we keep this as "Google Drive Auto Backup" and add "SD Card Auto Backup" elsewhere.
                        
                        // Current implementation assumes this toggle is for Drive.
                         if (enabled) {
                             GoogleSignIn.getLastSignedInAccount(context)?.email?.let { email ->
                                 viewModel.toggleAutoBackup(true, email, state.backupFrequency)
                             }
                        } else {
                            viewModel.toggleAutoBackup(false)
                        }
                    },
                    onFrequencyChange = { newFrequency ->
                         GoogleSignIn.getLastSignedInAccount(context)?.email?.let { email ->
                            viewModel.toggleAutoBackup(state.isAutoBackupEnabled, email, newFrequency)
                        }
                        // Also update for SD card if relevant, though toggleSdCardAutoBackup doesn't take frequency (it uses shared prefs)
                        // Ideally we save frequency globally.
                         val sharedPrefs = context.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)
                         sharedPrefs.edit().putString("backup_frequency", newFrequency).apply()
                    },
                    title = "Drive Auto Backup"
                )
            }

            item {
                BackupActionCard(
                    title = "SD Card / Local Folder",
                    subtitle = state.sdCardFolderUri?.let { 
                        try {
                            // Decode to be readable if possible, though raw URI is okay
                            android.net.Uri.parse(it).path?.substringAfterLast(":") ?: it
                        } catch(e: Exception) { it }
                    } ?: "Select a folder for backups",
                    icon = Icons.Default.SdStorage,
                    buttonText = if (state.sdCardFolderUri != null) "Backup Now" else "Select Folder",
                    isLoading = state.isBackingUp && state.backupResult?.contains("SD Card") == true,
                    onClick = {
                        if (state.sdCardFolderUri != null) {
                            viewModel.backupToSdCard()
                        } else {
                            sdCardLauncher.launch(null)
                        }
                    },
                    secondaryAction = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.sdCardFolderUri != null) {
                                TextButton(onClick = { sdCardLauncher.launch(null) }) {
                                    Text("Change")
                                }
                                Switch(
                                    checked = state.isSdCardAutoBackupEnabled,
                                    onCheckedChange = { viewModel.toggleSdCardAutoBackup(it) },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }
                    }
                )
            }

            item {
                BackupActionCard(
                    title = stringResource(id = R.string.create_backup),
                    subtitle = "Save a .zip file to a specific location",
                    icon = Icons.Default.Save,
                    buttonText = "Save As...",
                    isLoading = state.isBackingUp && state.backupResult?.contains("Local") == true,
                    onClick = {
                        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        createDocumentLauncher.launch("NoteNext_Backup_$timeStamp.zip")
                    }
                )
            }

            item {
                val driveSubtitle = state.googleAccountEmail?.let { email ->
                    val metadata = state.driveBackupMetadata
                    if (metadata != null) {
                        val size = formatSize(metadata.size)
                        val date = metadata.modifiedTime?.let {
                             val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                             sdf.format(Date(it.value))
                        } ?: "Unknown date"
                        "Linked: $email\nLast Backup: $date ($size)"
                    } else {
                        "Linked: $email"
                    }
                } ?: "Upload your data securely to cloud"

                BackupActionCard(
                    title = "Google Drive Backup",
                    subtitle = driveSubtitle,
                    icon = Icons.Default.CloudUpload,
                    buttonText = if (state.googleAccountEmail != null) "Backup to Drive" else "Sign In & Backup",
                    isLoading = state.isBackingUp && state.backupResult?.contains("Drive") == true,
                    isPrimary = true,
                    onClick = {
                        val account = GoogleSignIn.getLastSignedInAccount(context)
                        if (account != null) {
                             viewModel.backupToDrive(account)
                        } else {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestScopes(
                                    Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE),
                                    Scope(com.google.api.services.drive.DriveScopes.DRIVE_APPDATA)
                                )
                                .build()
                            val client = GoogleSignIn.getClient(context, gso)
                            googleSignInLauncher.launch(client.signInIntent)
                        }
                    },
                    secondaryAction = if (state.googleAccountEmail != null) {
                        { 
                             // Show restore button explicitly if backup exists
                             Row {
                                 if (state.driveBackupExists) {
                                     TextButton(onClick = { 
                                          val account = GoogleSignIn.getLastSignedInAccount(context)
                                          account?.let { viewModel.restoreFromDrive(it) }
                                     }) {
                                         Text("Restore")
                                     }
                                     Spacer(modifier = Modifier.width(8.dp))
                                 }
                                 TextButton(onClick = { showUnlinkDialog = true }) {
                                     Text("Unlink", color = MaterialTheme.colorScheme.error)
                                 }
                             }
                        }
                    } else null,
                    progressText = state.uploadProgress
                )
            }

            item {
                if (state.driveBackupExists) {
                    DeleteBackupCard(
                        isLoading = state.isDeleting,
                        onClick = { showDeleteDialog = true }
                    )
                }
            }

            item {
                state.backupResult?.let { result ->
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Drive Backup") },
            text = { Text("Are you sure you want to permanently delete the backup from Google Drive? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        GoogleSignIn.getLastSignedInAccount(context)?.let {
                            viewModel.deleteDriveBackup(it)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            title = { Text("Unlink Account") },
            text = { Text("Are you sure you want to unlink your Google account? This will stop automatic backups to Drive.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnlinkDialog = false
                        viewModel.signOut(context)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Unlink")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
fun BackupActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    buttonText: String,
    isLoading: Boolean,
    isPrimary: Boolean = false,
    containerColor: androidx.compose.ui.graphics.Color = if (isPrimary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    contentColor: androidx.compose.ui.graphics.Color = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    secondaryAction: (@Composable () -> Unit)? = null,
    progressText: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(16.dp),
        elevation = if (isPrimary) CardDefaults.cardElevation(defaultElevation = 2.dp) else CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (!isPrimary) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                             if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                             RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                     Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                         tint = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                     )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium.copy(color = contentColor.copy(alpha = 0.8f)))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Button(
                    onClick = onClick,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = if (isPrimary) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) else ButtonDefaults.outlinedButtonColors()
                ) {
                    if (isLoading) {
                         CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                         Spacer(modifier = Modifier.width(8.dp))
                         // If progress text is provided via a lambda or simple loading text
                         Text(progressText ?: "Processing...")
                    } else {
                         Text(buttonText)
                    }
                }
                
                secondaryAction?.invoke()
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

@Composable
fun AutoBackupCard(
    isEnabled: Boolean,
    frequency: String,
    onToggle: (Boolean) -> Unit,
    onFrequencyChange: (String) -> Unit,
    title: String = "Auto Backup"
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                         AnimatedVisibility(visible = isEnabled) {
                            Text(
                                text = "Frequency: $frequency",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Switch(checked = isEnabled, onCheckedChange = onToggle)
            }

            AnimatedVisibility(visible = isEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Backup Frequency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Daily", "Weekly").forEach { item ->
                            FilterChip(
                                selected = frequency == item,
                                onClick = { onFrequencyChange(item) },
                                label = { Text(item) },
                                leadingIcon = if (frequency == item) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Danger Zone",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Permanently delete backup from Drive",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                 if (isLoading) {
                     CircularProgressIndicator(
                         modifier = Modifier.size(16.dp),
                         strokeWidth = 2.dp, 
                         color = MaterialTheme.colorScheme.onError
                     )
                     Spacer(modifier = Modifier.width(8.dp))
                     Text("Deleting...")
                } else {
                     Text("Delete Backup")
                }
            }
        }
    }
}
