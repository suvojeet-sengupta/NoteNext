@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.settings

import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.MainViewModel
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.SettingsGroupCard
import com.suvojeet.notenext.ui.components.springPress
import kotlinx.coroutines.launch

@Composable
fun PrivacySecurityScreen(
    onBackClick: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = viewModel.settingsRepository
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val enableAppLock by settingsRepository.enableAppLock.collectAsStateWithLifecycle(initialValue = false)
    val disallowScreenshots by settingsRepository.disallowScreenshots.collectAsStateWithLifecycle(initialValue = false)
    val clipboardTimeout by settingsRepository.clipboardClearTimeout.collectAsStateWithLifecycle(initialValue = 0L)
    val enableDecoyVault by settingsRepository.enableDecoyVault.collectAsStateWithLifecycle(initialValue = false)
    val decoyPin by settingsRepository.decoyPin.collectAsStateWithLifecycle(initialValue = null)

    var showClipboardDialog by remember { mutableStateOf(false) }
    var showDecoyPinDialog by remember { mutableStateOf(false) }
    var showSelfDestructInfo by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Privacy & Security",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.0).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.springPress()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ExpressiveSection(
                    title = "App Access",
                    description = "Control how you and others access your notes"
                ) {
                    SettingsGroupCard {
                        SettingsToggle(
                            icon = Icons.Rounded.Security,
                            title = context.getString(R.string.app_lock),
                            subtitle = context.getString(R.string.app_lock_subtitle),
                            iconColor = MaterialTheme.colorScheme.primary,
                            checked = enableAppLock,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    val biometricManager = BiometricManager.from(context)
                                    val canAuthenticate = biometricManager.canAuthenticate(
                                        BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                                        BiometricManager.Authenticators.BIOMETRIC_WEAK or 
                                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                    )
                                    
                                    when (canAuthenticate) {
                                        BiometricManager.BIOMETRIC_SUCCESS -> {
                                            scope.launch { settingsRepository.saveEnableAppLock(true) }
                                        }
                                        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                                            val enrollIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                                android.content.Intent(android.provider.Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                                    putExtra(
                                                        android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                                        BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                                                        BiometricManager.Authenticators.BIOMETRIC_WEAK or 
                                                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                                    )
                                                }
                                            } else {
                                                android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                                            }
                                            
                                            try {
                                                context.startActivity(enrollIntent)
                                                android.widget.Toast.makeText(context, context.getString(R.string.biometric_setup_required), android.widget.Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "Please enable a screen lock or biometrics in system settings.", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                        else -> {
                                            android.widget.Toast.makeText(context, "Biometric authentication is not available on this device.", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    scope.launch { settingsRepository.saveEnableAppLock(false) }
                                }
                            }
                        )
                    }
                }
            }

            item {
                ExpressiveSection(
                    title = "Decoy Vault",
                    description = "A secondary PIN that reveals a fake set of notes"
                ) {
                    SettingsGroupCard {
                        SettingsToggle(
                            icon = Icons.Rounded.VisibilityOff,
                            title = "Enable Decoy Vault",
                            subtitle = "Use a secondary PIN for coercion situations",
                            iconColor = MaterialTheme.colorScheme.secondary,
                            checked = enableDecoyVault,
                            onCheckedChange = { 
                                if (it && decoyPin == null) {
                                    showDecoyPinDialog = true
                                } else {
                                    scope.launch { settingsRepository.saveEnableDecoyVault(it) }
                                }
                            }
                        )
                        if (enableDecoyVault) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            SettingsItem(
                                icon = Icons.Rounded.Password,
                                title = "Set Decoy PIN",
                                subtitle = if (decoyPin == null) "Not set" else "****",
                                iconColor = MaterialTheme.colorScheme.secondary,
                                onClick = { showDecoyPinDialog = true }
                            )
                        }
                    }
                }
            }

            item {
                ExpressiveSection(
                    title = "Data Privacy",
                    description = "Protect your note content from outside eyes"
                ) {
                    SettingsGroupCard {
                        SettingsToggle(
                            icon = Icons.Rounded.Lock,
                            title = "Disallow Screenshots",
                            subtitle = "Prevent screen capture and hide content in recents",
                            iconColor = MaterialTheme.colorScheme.error,
                            checked = disallowScreenshots,
                            onCheckedChange = { scope.launch { settingsRepository.saveDisallowScreenshots(it) } }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsItem(
                            icon = Icons.Rounded.ContentPasteOff,
                            title = "Smart Clipboard Clear",
                            subtitle = when(clipboardTimeout) {
                                0L -> "Disabled"
                                1L -> "Immediately on background"
                                30_000L -> "After 30 seconds"
                                60_000L -> "After 1 minute"
                                300_000L -> "After 5 minutes"
                                else -> "Custom timeout"
                            },
                            iconColor = MaterialTheme.colorScheme.tertiary,
                            onClick = { showClipboardDialog = true }
                        )
                    }
                }
            }

            item {
                ExpressiveSection(
                    title = "Ephemeral Content",
                    description = "Notes that don't stick around"
                ) {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Rounded.Timer,
                            title = "Self-Destructing Notes",
                            subtitle = "Set notes to automatically delete after a certain time",
                            iconColor = Color(0xFF6750A4),
                            onClick = { showSelfDestructInfo = true }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showClipboardDialog) {
        ClipboardTimeoutDialog(
            currentTimeout = clipboardTimeout,
            onTimeoutSelected = {
                scope.launch { settingsRepository.saveClipboardClearTimeout(it) }
                showClipboardDialog = false
            },
            onDismiss = { showClipboardDialog = false }
        )
    }

    if (showDecoyPinDialog) {
        DecoyPinDialog(
            onPinSelected = {
                scope.launch { 
                    settingsRepository.saveDecoyPin(it)
                    settingsRepository.saveEnableDecoyVault(true)
                }
                showDecoyPinDialog = false
            },
            onDismiss = { showDecoyPinDialog = false }
        )
    }
    if (showSelfDestructInfo) {
        AlertDialog(
            onDismissRequest = { showSelfDestructInfo = false },
            icon = { Icon(Icons.Rounded.Timer, contentDescription = null, tint = Color(0xFF6750A4)) },
            title = { Text("Self-Destructing Notes") },
            text = {
                Text("Self-destruct is a per-note feature. You can set a timer for any note by clicking the 'More' (three-dots) menu while editing a note and selecting 'Self-Destruct Timer'. Once the timer expires, the note will be permanently deleted.")
            },
            confirmButton = {
                TextButton(onClick = { showSelfDestructInfo = false }) {
                    Text("Got it")
                }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}

@Composable
private fun ClipboardTimeoutDialog(
    currentTimeout: Long,
    onTimeoutSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        0L to "Disabled",
        1L to "Immediately",
        30_000L to "30 Seconds",
        60_000L to "1 Minute",
        300_000L to "5 Minutes"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Clipboard Clear Timeout") },
        text = {
            Column {
                options.forEach { (timeout, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onTimeoutSelected(timeout) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (currentTimeout == timeout), onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = Modifier.springPress(),
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.12f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = if (checked) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                } else null
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun DecoyPinDialog(
    onPinSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Decoy PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Enter a 4-digit secondary PIN. Entering this PIN on the lock screen will open a separate, decoy vault.")
                
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text("Secondary PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) confirmPin = it },
                    label = { Text("Confirm Secondary PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pin.length != 4) {
                        error = "PIN must be 4 digits"
                    } else if (pin != confirmPin) {
                        error = "PINs do not match"
                    } else {
                        onPinSelected(pin)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .springPress()
            .clickable(onClick = onClick),
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.12f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
        },
        trailingContent = {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

