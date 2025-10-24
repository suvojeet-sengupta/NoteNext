package com.suvojeet.notenext.ui.lock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.settings.SettingsRepository
import com.suvojeet.notenext.util.BiometricAuthManager
import com.suvojeet.notenext.util.findActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val activity = context.findActivity() as? FragmentActivity

    val biometricAuthManager = if (activity != null) {
        remember(activity) {
            BiometricAuthManager(
                context = context,
                activity = activity,
                onAuthSuccess = onUnlock,
                onAuthError = { error = it },
                onAuthFailed = { error = "Biometric authentication failed" }
            )
        }
    } else {
        null
    }

    val canAuthenticateResult = biometricAuthManager?.canAuthenticate()

    LaunchedEffect(biometricAuthManager) {
        when (canAuthenticateResult) {
            BiometricManager.BIOMETRIC_SUCCESS -> biometricAuthManager?.showBiometricPrompt()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                error = "Please enroll a biometric or set a device credential to use this feature."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Locked") }
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
            Icon(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "App Icon",
                modifier = Modifier.size(128.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text("Enter PIN to unlock", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // PIN display
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pin.length) {
                    Text(
                        text = "●",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .width(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Numeric Keyboard
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberButton("1") { if(pin.length < 8) pin += "1" }
                    NumberButton("2") { if(pin.length < 8) pin += "2" }
                    NumberButton("3") { if(pin.length < 8) pin += "3" }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberButton("4") { if(pin.length < 8) pin += "4" }
                    NumberButton("5") { if(pin.length < 8) pin += "5" }
                    NumberButton("6") { if(pin.length < 8) pin += "6" }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberButton("7") { if(pin.length < 8) pin += "7" }
                    NumberButton("8") { if(pin.length < 8) pin += "8" }
                    NumberButton("9") { if(pin.length < 8) pin += "9" }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canAuthenticateResult == BiometricManager.BIOMETRIC_SUCCESS) {
                        IconButton(onClick = { biometricAuthManager.showBiometricPrompt() }) {
                            Icon(Icons.Default.Fingerprint, contentDescription = "Use Biometrics", modifier = Modifier.size(48.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    NumberButton("0") { if(pin.length < 8) pin += "0" }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { pin = pin.dropLast(1) }) {
                        Icon(Icons.Default.Backspace, contentDescription = "Backspace")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    scope.launch {
                        val storedPin = settingsRepository.appLockPin.first()
                        if (pin == storedPin) {
                            onUnlock()
                        } else {
                            error = "Incorrect PIN"
                            pin = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Unlock")
            }
        }
    }
}

@Composable
private fun NumberButton(number: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape
    ) {
        Text(number, style = MaterialTheme.typography.headlineMedium)
    }
}
