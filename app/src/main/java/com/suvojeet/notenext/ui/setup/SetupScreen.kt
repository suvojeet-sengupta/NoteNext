@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.suvojeet.notenext.ui.setup

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.setup.components.PermissionItem
import com.suvojeet.notenext.ui.settings.BackupRestoreViewModel
import com.suvojeet.notenext.ui.components.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SignInAction {
    RESTORE, ENABLE_BACKUP, CONNECT_ONLY
}

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit
) {
    val viewModel: SetupViewModel = hiltViewModel()
    val backupViewModel: BackupRestoreViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backupState by backupViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1500L)
        showLoading = false
    }

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
    
    LaunchedEffect(Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        backupViewModel.setGoogleAccount(account)
    }
    
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
                    SignInAction.RESTORE -> { }
                    SignInAction.ENABLE_BACKUP -> backupViewModel.toggleAutoBackup(true, it.email)
                    else -> { }
                }
            }
        } catch (e: ApiException) {
            e.printStackTrace()
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

    val pagerState = rememberPagerState(pageCount = { 3 })
    val canContinue = (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || postNotificationsGranted) && state.exactAlarmGranted

    AnimatedContent(
        targetState = showLoading,
        transitionSpec = {
            (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
             scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessLow)))
                .togetherWith(fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)))
        },
        label = "SetupLoadingTransition"
    ) { loading ->
        if (loading) {
            SetupLoadingScreen()
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                                .imePadding()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(3) { index ->
                                    val isSelected = pagerState.currentPage == index
                                    val width by animateDpAsState(
                                        targetValue = if (isSelected) 24.dp else 8.dp,
                                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                                        label = "DotWidth"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .height(8.dp)
                                            .width(width)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedVisibility(
                                    visible = pagerState.currentPage > 0,
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally()
                                ) {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                        },
                                        modifier = Modifier.springPress()
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(id = R.string.setup_back))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.weight(1f))
                                
                                Button(
                                    onClick = {
                                        if (pagerState.currentPage < 2) {
                                            scope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        } else {
                                            viewModel.onEvent(SetupEvent.CompleteSetup)
                                            onSetupComplete()
                                        }
                                    },
                                    modifier = Modifier
                                        .height(56.dp)
                                        .springPress(),
                                    enabled = if (pagerState.currentPage == 2) canContinue else true,
                                    shape = MaterialTheme.shapes.medium,
                                    colors = if (pagerState.currentPage == 2) 
                                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        else ButtonDefaults.buttonColors()
                                ) {
                                    Text(
                                        text = if (pagerState.currentPage == 2) stringResource(id = R.string.setup_get_started) else stringResource(id = R.string.setup_next),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (pagerState.currentPage < 2) {
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    userScrollEnabled = true
                ) { page ->
                    when (page) {
                        0 -> WelcomePage()
                        1 -> CloudSyncPage(backupViewModel, backupState, googleSignInLauncher, gso, context)
                        2 -> PermissionsPage(state, postNotificationsGranted, postNotificationsPermissionLauncher, exactAlarmPermissionLauncher)
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        AnimatedSetupStep(visible = visible, delay = 0) {
            MorphingLogo(size = 140.dp)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        AnimatedSetupStep(visible = visible, delay = 200) {
            Text(
                text = stringResource(id = R.string.setup_welcome_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedSetupStep(visible = visible, delay = 400) {
            Text(
                text = stringResource(id = R.string.setup_welcome_subtitle),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = com.suvojeet.notenext.ui.theme.Fraunces,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val features = listOf(
                stringResource(id = R.string.setup_feature_private) to 600,
                stringResource(id = R.string.setup_feature_cloud) to 700,
                stringResource(id = R.string.setup_feature_reminders) to 800
            )
            
            features.forEach { (text, delay) ->
                AnimatedSetupStep(visible = visible, delay = delay) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = text,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedSetupStep(visible = visible, delay = 900) {
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            androidx.compose.material3.Text(
                text = stringResource(id = R.string.setup_privacy_consent),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { 
                        uriHandler.openUri("https://notenext.suvojeetsengupta.in/privacy-policy")
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun CloudSyncPage(
    backupViewModel: BackupRestoreViewModel,
    backupState: com.suvojeet.notenext.ui.settings.BackupRestoreState,
    googleSignInLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    gso: GoogleSignInOptions,
    context: android.content.Context
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        AnimatedContent(
            targetState = backupState.googleAccountEmail != null,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "CloudIcon"
        ) { connected ->
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (connected) Icons.Rounded.CloudDone else Icons.Rounded.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(id = R.string.setup_cloud_sync_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(id = R.string.setup_cloud_sync_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if(backupState.googleAccountEmail != null) stringResource(id = R.string.setup_connected_to, backupState.googleAccountEmail!!) else stringResource(id = R.string.setup_cloud_sync_pitch),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (backupState.googleAccountEmail == null) {
                    Button(
                        onClick = {
                            val signInIntent = GoogleSignIn.getClient(context, gso).signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).springPress(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(id = R.string.setup_connect_google), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(id = R.string.setup_daily_auto_backup), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text(stringResource(id = R.string.setup_highly_recommended), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        }

                        if (backupState.isRestoring) {
                            ExpressiveLoading(modifier = Modifier.height(80.dp))
                        } else if (backupState.restoreResult?.contains("successful", true) == true) {
                             Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = Color(0xFFE8F5E9)
                            ) {
                                 Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                                    Spacer(Modifier.width(12.dp))
                                    Text(stringResource(id = R.string.setup_data_restored), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                             OutlinedButton(
                                onClick = {
                                    val account = GoogleSignIn.getLastSignedInAccount(context)
                                    account?.let { backupViewModel.restoreFromDrive(it) }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp).springPress(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.CloudDownload, null)
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(id = R.string.setup_restore_previous), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsPage(
    state: SetupState,
    postNotificationsGranted: Boolean,
    postNotificationsLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    exactAlarmLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(id = R.string.setup_permissions_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionItem(
                    title = stringResource(id = R.string.setup_perm_notifications_title),
                    description = stringResource(id = R.string.setup_perm_notifications_desc),
                    isGranted = postNotificationsGranted,
                    onRequestClick = { postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
            }

            PermissionItem(
                title = stringResource(id = R.string.setup_perm_exact_alarms_title),
                description = stringResource(id = R.string.setup_perm_exact_alarms_desc),
                isGranted = state.exactAlarmGranted,
                onRequestClick = {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                        exactAlarmLauncher.launch(it)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        val grantedCount = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) (if (postNotificationsGranted) 1 else 0) else 1) + (if (state.exactAlarmGranted) 1 else 0)
        val totalCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 2 else 1
        
        val progress by animateFloatAsState(
            targetValue = grantedCount.toFloat() / totalCount.toFloat(),
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "PermissionProgress"
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.setup_progress, grantedCount, totalCount),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        AnimatedVisibility(
            visible = grantedCount == totalCount,
            enter = fadeIn() + scaleIn(initialScale = 0.8f) + slideInVertically { it / 2 },
            label = "AllPermissionsGranted"
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFE8F5E9),
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(id = R.string.setup_all_ready), fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                }
            }
        }
    }
}
