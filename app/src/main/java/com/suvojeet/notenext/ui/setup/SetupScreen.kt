
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.suvojeet.notenext.dependency_injection.ViewModelFactory
import com.suvojeet.notenext.ui.setup.components.PermissionItem

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
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
            Text(
                text = "Let's set up NoteNext for the best experience!",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Runtime Permissions
            val readExternalStoragePermissionState = rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
            PermissionItem(
                title = "Storage Permission",
                description = "Allow NoteNext to access your device storage to save and load notes, attachments, and other data.",
                isGranted = readExternalStoragePermissionState.status.isGranted,
                onRequestClick = { readExternalStoragePermissionState.launchPermissionRequest() }
            )
            Spacer(modifier = Modifier.height(16.dp))

            val postNotificationsPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                null
            }

            if (postNotificationsPermissionState != null) {
                PermissionItem(
                    title = "Notification Permission",
                    description = "Allow NoteNext to send you notifications for reminders and other important updates.",
                    isGranted = postNotificationsPermissionState.status.isGranted,
                    onRequestClick = { postNotificationsPermissionState.launchPermissionRequest() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Exact Alarm Permission
            val exactAlarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            PermissionItem(
                title = "Exact Alarm Permission",
                description = "Allow NoteNext to schedule exact alarms for precise reminders.",
                isGranted = exactAlarmGranted,
                onRequestClick = {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                        exactAlarmPermissionLauncher.launch(it)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            val canContinue = readExternalStoragePermissionState.status.isGranted && 
                              (postNotificationsPermissionState?.status?.isGranted ?: true) && 
                              exactAlarmGranted

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
