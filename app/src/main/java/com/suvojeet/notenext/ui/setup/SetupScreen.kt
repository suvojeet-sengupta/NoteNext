
package com.suvojeet.notenext.ui.setup

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SetupScreen(
    factory: ViewModelFactory,
    onSetupComplete: () -> Unit
) {
    val viewModel: SetupViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val exactAlarmPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onEvent(SetupEvent.ExactAlarmPermissionResult)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Welcome to NoteNext") })
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
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier.size(128.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Let's set up NoteNext for the best experience!",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            var postNotificationsGranted by remember { mutableStateOf(false) }
            val postNotificationsPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                postNotificationsGranted = isGranted
            }

            // Runtime Permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                LaunchedEffect(Unit) {
                    postNotificationsGranted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionItem(
                    title = "Notification Permission",
                    description = "Allow NoteNext to send you notifications for reminders and other important updates.",
                    isGranted = postNotificationsGranted,
                    onRequestClick = { postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Exact Alarm Permission
            PermissionItem(
                title = "Exact Alarm Permission",
                description = "Allow NoteNext to schedule exact alarms for precise reminders.",
                isGranted = state.exactAlarmGranted,
                onRequestClick = {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                        exactAlarmPermissionLauncher.launch(it)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            val canContinue = postNotificationsGranted && state.exactAlarmGranted

            Button(
                onClick = {
                    viewModel.onEvent(SetupEvent.CompleteSetup)
                    onSetupComplete()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canContinue
            ) {
                Text("Continue")
            }
        }
    }
}
