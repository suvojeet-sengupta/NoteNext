package com.suvojeet.notenext.ui.setup

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.setup.components.PermissionItem
import com.suvojeet.notenext.ui.settings.BackupRestoreViewModel
import com.suvojeet.notenext.ui.settings.PasswordInputDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private enum class SignInAction {
    RESTORE, ENABLE_BACKUP, CONNECT_ONLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit
) {
    val viewModel: SetupViewModel = hiltViewModel()
    val backupViewModel: BackupRestoreViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val backupState by backupViewModel.state.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val exactAlarmPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onEvent(SetupEvent.ExactAlarmPermissionResult)
    }

    var postNotificationsGranted by remember { mutableStateOf(false) }
    val postNotificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) postNotificationsGranted = true
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) {
            postNotificationsGranted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
    
    // Sync BackupViewModel with current Google Account status
    LaunchedEffect(Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        backupViewModel.setGoogleAccount(account)
    }
    
    // Google Sign-In Handling
    var signInAction by remember { mutableStateOf<SignInAction?>(null) }
    
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
        .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_APPDATA))
        .build()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.let {
                backupViewModel.setGoogleAccount(it)
                when (signInAction) {
                    SignInAction.RESTORE -> { /* Handle restore if needed, or let user click button */ }
                    SignInAction.ENABLE_BACKUP -> backupViewModel.toggleAutoBackup(true, it.email)
                    else -> { /* Just connected */ }
                }
            }
        } catch (e: ApiException) {
            e.printStackTrace()
            // Handle error (show snackbar ideally, but simple for now)
        }
        signInAction = null
    }

    if (backupState.isPasswordRequired) {
        PasswordInputDialog(
            onDismiss = { backupViewModel.cancelPasswordEntry() },
            onConfirm = { password ->
                backupViewModel.restoreEncryptedBackup(password)
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val canContinue = (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || postNotificationsGranted) && state.exactAlarmGranted
            
            Box(
                 modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .imePadding()
            ) {
                 Button(
                    onClick = {
                        viewModel.onEvent(SetupEvent.CompleteSetup)
                        onSetupComplete()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = canContinue,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                         text = stringResource(id = R.string.continue_button),
                         style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Header Section
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(100.dp) // Slightly smaller for better balance
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.5f), RoundedCornerShape(32.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Welcome to NoteNext",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Secure, Organized, and Synced.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))

            // Google Drive Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                         Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Google Drive Sync",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if(backupState.googleAccountEmail != null) "Linked to ${backupState.googleAccountEmail}" else "Connect to enable cloud backup & restore.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (backupState.googleAccountEmail == null) {
                        Button(
                            onClick = {
                                signInAction = SignInAction.CONNECT_ONLY
                                val signInIntent = GoogleSignIn.getClient(context, gso).signInIntent
                                googleSignInLauncher.launch(signInIntent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Connect Google Account")
                        }
                    } else {
                        // Options when connected
                        
                        // 1. Auto Backup Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto Backup", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Daily backup", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = backupState.isAutoBackupEnabled,
                                onCheckedChange = { enabled -> 
                                    backupState.googleAccountEmail?.let { 
                                         backupViewModel.toggleAutoBackup(enabled, it)
                                    }
                                }
                            )
                        }
                        
                        // HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(12.dp))

                        // 2. Restore Action (Restyle)
                        if (backupState.isRestoring) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(backupState.restoreResult ?: "Restoring...", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else if (backupState.restoreResult?.contains("successful", true) == true) {
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Restore Completed", style = MaterialTheme.typography.bodyMedium, color = Color.Green, fontWeight = FontWeight.Bold)
                            }
                        } else {
                             OutlinedButton(
                                onClick = {
                                    val account = GoogleSignIn.getLastSignedInAccount(context)
                                    account?.let { backupViewModel.restoreFromDrive(it) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Restore Existing Data")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Required Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionItem(
                    title = "Notifications",
                    description = "Get notified about reminders & backups.",
                    isGranted = postNotificationsGranted,
                    onRequestClick = { postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            PermissionItem(
                title = "Exact Alarms",
                description = "Required for precise reminder timing.",
                isGranted = state.exactAlarmGranted,
                onRequestClick = {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                        exactAlarmPermissionLauncher.launch(it)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(100.dp)) // Padding for bottom bar
        }
    }
}
