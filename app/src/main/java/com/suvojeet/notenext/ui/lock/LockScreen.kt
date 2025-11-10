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
import com.suvojeet.notenext.ui.settings.SettingsRepository
import com.suvojeet.notenext.util.BiometricAuthManager
import com.suvojeet.notenext.util.findActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    val activity = context.findActivity() as? FragmentActivity

    val biometricAuthFailedString = stringResource(id = R.string.biometric_auth_failed)

    val biometricAuthManager = if (activity != null) {
        remember(activity) {
            BiometricAuthManager(
                context = context,
                activity = activity,
                onAuthSuccess = onUnlock,
                onAuthError = {
                    if (it != "Authentication error: User Canceled") {
                        error = it
                    }
                },
                onAuthFailed = { error = biometricAuthFailedString }
            )
        }
    } else {
        null
    }

    val canAuthenticateResult = biometricAuthManager?.canAuthenticate()

    LaunchedEffect(biometricAuthManager) {
        if (canAuthenticateResult == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricAuthManager?.showBiometricPrompt()
        }
    }

    fun triggerShake() {
        coroutineScope.launch {
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 500
                    -20f at 50
                    20f at 100
                    -20f at 150
                    20f at 200
                    -10f at 250
                    10f at 300
                    -5f at 350
                    5f at 400
                    0f at 450
                }
            )
        }
    }

    // ***FIX 1: Get the string resource here, in the composable context***
    val incorrectPinString = stringResource(id = R.string.incorrect_pin)

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(0.2f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(id = R.string.enter_pin_lock_screen), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.offset(x = offsetX.value.dp, y = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val maxPinLength = 8
                    repeat(maxPinLength) { index ->
                        PinDot(isFilled = index < pin.length, isError = error != null)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NumberButton("1") { if (pin.length < 8) pin += "1"; error = null }
                    NumberButton("2") { if (pin.length < 8) pin += "2"; error = null }
                    NumberButton("3") { if (pin.length < 8) pin += "3"; error = null }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NumberButton("4") { if (pin.length < 8) pin += "4"; error = null }
                    NumberButton("5") { if (pin.length < 8) pin += "5"; error = null }
                    NumberButton("6") { if (pin.length < 8) pin += "6"; error = null }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NumberButton("7") { if (pin.length < 8) pin += "7"; error = null }
                    NumberButton("8") { if (pin.length < 8) pin += "8"; error = null }
                    NumberButton("9") { if (pin.length < 8) pin += "9"; error = null }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(72.dp)) {
                        if (canAuthenticateResult == BiometricManager.BIOMETRIC_SUCCESS) {
                            FilledTonalIconButton(
                                onClick = { biometricAuthManager?.showBiometricPrompt() },
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    Icons.Default.Fingerprint,
                                    contentDescription = stringResource(id = R.string.use_biometrics),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    NumberButton("0") { if (pin.length < 8) pin += "0"; error = null }
                    Spacer(modifier = Modifier.width(16.dp))
                    FilledTonalIconButton(
                        onClick = { pin = pin.dropLast(1); error = null },
                        modifier = Modifier.size(72.dp),
                        enabled = pin.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Backspace,
                            contentDescription = stringResource(id = R.string.back),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(0.2f))
            Button(
                onClick = {
                    scope.launch {
                        val storedPin = settingsRepository.appLockPin.first()
                        if (pin == storedPin) {
                            onUnlock()
                        } else {
                            // ***FIX 2: Use the variable here, inside the non-composable lambda***
                            error = incorrectPinString
                            triggerShake()
                            pin = ""
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(stringResource(id = R.string.unlock))
            }
            Spacer(modifier = Modifier.weight(0.1f))
        }
    }
}

@Composable
private fun PinDot(isFilled: Boolean, isError: Boolean) {
    val color = when {
        isError -> MaterialTheme.colorScheme.error
        isFilled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun NumberButton(number: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape
    ) {
        Text(number, style = MaterialTheme.typography.headlineLarge)
    }
}
