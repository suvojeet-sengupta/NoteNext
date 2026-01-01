package com.suvojeet.notenext.ui.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.util.BiometricAuthManager
import com.suvojeet.notenext.util.findActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    
    val activity = context.findActivity() as? FragmentActivity
    val biometricAuthFailedString = stringResource(id = R.string.biometric_auth_failed)

    val biometricAuthManager = if (activity != null) {
        remember(activity) {
            BiometricAuthManager(
                context = context,
                activity = activity
            )
        }
    } else {
        null
    }

    val canAuthenticateResult = biometricAuthManager?.canAuthenticate()

    // Auto-trigger biometric prompt on start
    LaunchedEffect(biometricAuthManager) {
        if (canAuthenticateResult == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricAuthManager?.showBiometricPrompt(
                onAuthSuccess = onUnlock,
                onAuthError = {
                    if (it != "Authentication error: User Canceled") {
                        error = it
                    }
                },
                onAuthFailed = { error = biometricAuthFailedString }
            )
        } else {
             error = "Biometric authentication not available"
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.3f))
            
            Icon(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = stringResource(id = R.string.app_name),
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(id = R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            
            Spacer(modifier = Modifier.height(32.dp))

            if (canAuthenticateResult == BiometricManager.BIOMETRIC_SUCCESS) {
                // Unlock Button
                 FilledTonalButton(
                    onClick = {
                        biometricAuthManager?.showBiometricPrompt(
                            onAuthSuccess = onUnlock,
                            onAuthError = {
                                if (it != "Authentication error: User Canceled") {
                                    error = it
                                }
                            },
                            onAuthFailed = { error = biometricAuthFailedString }
                        )
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.unlock_with_biometrics))
                }
            } else {
                Text(
                     "Biometric authentication is not available on this device", 
                     color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.weight(0.7f))
        }
    }
}
