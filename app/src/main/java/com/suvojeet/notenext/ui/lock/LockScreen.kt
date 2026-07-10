@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import com.suvojeet.notenext.R
import com.suvojeet.notenext.util.BiometricAuthManager
import com.suvojeet.notenext.util.findActivity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.ui.components.springPress
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.ui.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.view.WindowManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    onUnlock: (Boolean) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settingsRepository = viewModel.settingsRepository
    val isDecoyEnabled by settingsRepository.enableDecoyVault.collectAsStateWithLifecycle(initialValue = false)

    var enteredPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var verifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val activity = context.findActivity() as? FragmentActivity

    // FLAG_SECURE always on this screen — protects PIN entry from screenshots/recents
    // regardless of the user's "Disallow Screenshots" preference.
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            // Restore based on user preference; MainActivity's collector will re-apply
            // FLAG_SECURE if disallowScreenshots is true.
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

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
    // When decoy vault is enabled we hide biometrics on the cold lock screen entirely.
    // Biometric auth would always unlock the real vault — under coercion that bypasses
    // the decoy feature. Forcing PIN entry preserves plausible deniability.
    val isAuthAvailable = !isDecoyEnabled && (
        canAuthenticateResult == BiometricManager.BIOMETRIC_SUCCESS ||
            canAuthenticateResult == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    )

    LaunchedEffect(biometricAuthManager, isDecoyEnabled) {
        if (isAuthAvailable) {
            biometricAuthManager?.showBiometricPrompt(
                onAuthSuccess = { _ -> onUnlock(false) },
                onAuthError = {
                    if (it != "Authentication error: User Canceled" && !it.contains("Canceled")) {
                        error = it
                    }
                },
                onAuthFailed = { error = biometricAuthFailedString }
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.4f))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = if (isDecoyEnabled) "Enter your PIN" else "Protected with device security",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (isAuthAvailable) {
                 FilledTonalButton(
                    onClick = {
                        biometricAuthManager?.showBiometricPrompt(
                            onAuthSuccess = { _ -> onUnlock(false) },
                            onAuthError = {
                                if (it != "Authentication error: User Canceled" && !it.contains("Canceled")) {
                                    error = it
                                }
                            },
                            onAuthFailed = { error = biometricAuthFailedString }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .springPress(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Unlock with Biometrics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val active = index < enteredPin.length
                    Surface(
                        modifier = Modifier.size(16.dp),
                        shape = CircleShape,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = if (!active) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Numeric Keypad
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "DEL")
                )

                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            if (key.isEmpty()) {
                                Spacer(modifier = Modifier.size(64.dp))
                            } else {
                                val keyDescription = if (key == "DEL") "Delete" else "Digit $key"
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                        .semantics {
                                            role = Role.Button
                                            contentDescription = keyDescription
                                        }
                                        .clickable(enabled = !verifying) {
                                            if (key == "DEL") {
                                                if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                            } else if (enteredPin.length < 4) {
                                                val next = enteredPin + key
                                                enteredPin = next
                                                if (next.length == 4) {
                                                    verifying = true
                                                    val attempt = next
                                                    scope.launch {
                                                        // Constant-time-ish: always run both checks.
                                                        // Real first to avoid timing leak about which path matched.
                                                        val realOk = settingsRepository.verifyAppLockPin(attempt)
                                                        val decoyOk = settingsRepository.verifyDecoyPin(attempt)
                                                        // Small intentional delay so attempts feel uniform
                                                        // and a coerced user has more time to react.
                                                        delay(150)
                                                        when {
                                                            realOk -> onUnlock(false)
                                                            isDecoyEnabled && decoyOk -> onUnlock(true)
                                                            else -> {
                                                                error = "Try again"
                                                                enteredPin = ""
                                                            }
                                                        }
                                                        verifying = false
                                                    }
                                                }
                                            }
                                        }
                                        .springPress(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key == "DEL") {
                                        Icon(Icons.Rounded.Backspace, contentDescription = null)
                                    } else {
                                        Text(key, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(animationSpec = spring()),
                exit = fadeOut(animationSpec = spring())
            ) {
                Text(
                    error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(0.6f))

            Text(
                text = "Secure · Private · Local",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
