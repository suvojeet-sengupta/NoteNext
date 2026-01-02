package com.suvojeet.notenext.ui.settings

import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.theme.ThemeMode
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.height
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.suvojeet.notenext.data.backup.GoogleDriveManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R

import com.suvojeet.notenext.util.findActivity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


// ... (previous imports)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit, onNavigate: (String) -> Unit) {
    // ... (state setup remains same)
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    // -- State Collection --
    val selectedThemeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val autoDeleteDays by settingsRepository.autoDeleteDays.collectAsState(initial = 7)
    val enableRichLinkPreview by settingsRepository.enableRichLinkPreview.collectAsState(initial = false)
    val enableAppLock by settingsRepository.enableAppLock.collectAsState(initial = false)

    val selectedLanguage by settingsRepository.language.collectAsState(initial = "en")
    val disallowScreenshots by settingsRepository.disallowScreenshots.collectAsState(initial = false)

    // -- Dialog States --
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAutoDeleteDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAppLockInfoDialog by remember { mutableStateOf(false) }
    var showScreenshotInfoDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showContactUsDialog by remember { mutableStateOf(false) }
    
    // -- Import Logic --
    // We need BackupRestoreViewModel for import logic
    val backupRestoreViewModel: BackupRestoreViewModel = hiltViewModel()
    
    var showImportSourceDialog by remember { mutableStateOf(false) }
    var showKeepInstructionsDialog by remember { mutableStateOf(false) }

    val importKeepLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { backupRestoreViewModel.importFromGoogleKeep(it) }
    }

    // -- Scroll Behavior --
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background, // Restored to background
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp), // Restored padding
            verticalArrangement = Arrangement.spacedBy(24.dp) // Restored spacing
        ) {
            // --- Display Section ---
            item {
                SettingsSection(
                    title = stringResource(id = R.string.display_section_title),
                    color = Color(0xFF2196F3) // Blue
                ) {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Default.Palette,
                            title = stringResource(id = R.string.theme),
                            subtitle = selectedThemeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                            iconColor = Color(0xFF2196F3),
                            onClick = { showThemeDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        
                        SettingsItem(
                            icon = Icons.Default.Link,
                            title = stringResource(id = R.string.rich_link_preview),
                            subtitle = stringResource(id = R.string.rich_link_preview_subtitle),
                            hasSwitch = true,
                            checked = enableRichLinkPreview,
                            iconColor = Color(0xFF03A9F4),
                            onCheckedChange = { scope.launch { settingsRepository.saveEnableRichLinkPreview(it) } }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))



                        SettingsItem(
                            icon = Icons.Default.Language,
                            title = stringResource(id = R.string.language),
                            subtitle = stringResource(id = R.string.language_subtitle),
                            iconColor = Color(0xFF3F51B5),
                            onClick = { showLanguageDialog = true }
                        )
                    }
                }
            }

            // --- Bin Section ---
            item {
                SettingsSection(
                    title = stringResource(id = R.string.bin_section_title),
                    color = Color(0xFFF44336) // Red
                ) {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Default.Delete,
                            title = stringResource(id = R.string.auto_delete_binned_notes),
                            subtitle = stringResource(id = R.string.auto_delete_subtitle, autoDeleteDays),
                            iconColor = Color(0xFFF44336),
                            onClick = { showAutoDeleteDialog = true }
                        )
                    }
                }
            }

            // --- Security Section ---
            item {
                SettingsSection(
                    title = stringResource(id = R.string.security_section_title),
                    color = Color(0xFF4CAF50) // Green
                ) {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Default.Security,
                            title = stringResource(id = R.string.app_lock),
                            subtitle = stringResource(id = R.string.app_lock_subtitle),
                            hasSwitch = true,
                            checked = enableAppLock,
                            iconColor = Color(0xFF4CAF50),
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    val biometricManager = androidx.biometric.BiometricManager.from(context)
                                    val canAuthenticate = biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                                    if (canAuthenticate == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
                                        scope.launch { settingsRepository.saveEnableAppLock(true) }
                                    } else {
                                        android.widget.Toast.makeText(context, context.getString(R.string.biometric_setup_required), android.widget.Toast.LENGTH_LONG).show()
                                        // Optionally open security settings
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Fallback if security settings intent fails
                                        }
                                    }
                                } else {
                                    scope.launch { settingsRepository.saveEnableAppLock(false) }
                                }
                            },
                            onInfoClick = { showAppLockInfoDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsItem(
                            icon = Icons.Default.Lock, // Use a suitable icon like Lock or MobileOff
                            title = "Disallow Screenshots",
                            subtitle = "Prevent screen capture in app",
                            hasSwitch = true,
                            checked = disallowScreenshots,
                            iconColor = Color(0xFFF44336), 
                            onCheckedChange = { scope.launch { settingsRepository.saveDisallowScreenshots(it) } },
                            onInfoClick = { showScreenshotInfoDialog = true }
                        )
                    }
                }
            }

            // --- Backup & Restore Section ---
            item {
                SettingsSection(
                    title = stringResource(id = R.string.backup_restore_section_title),
                    color = Color(0xFFFF9800) // Orange
                ) {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Default.Backup,
                            title = "Backup & Restore",
                            subtitle = "Manage backups and restore data",
                            iconColor = Color(0xFFFF9800),
                            onClick = { onNavigate("backup") }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsItem(
                            icon = Icons.Default.ImportExport, // or generic import icon
                            title = "Import Notes",
                            subtitle = "From Google Keep, etc.",
                            iconColor = Color(0xFFE91E63),
                            onClick = { showImportSourceDialog = true }
                        )
                    }
                }
            }

            // --- Support Section ---
            item {
                SettingsSection(
                    title = "Support",
                    color = Color(0xFF9C27B0) // Purple
                ) {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Default.Star,
                            title = "Rate App",
                            subtitle = "Rate us on Play Store",
                            iconColor = Color(0xFF9C27B0),
                            onClick = {
                                showRateDialog = true
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsItem(
                            icon = Icons.Default.Share,
                            title = "Share App",
                            subtitle = "Share with your friends",
                            iconColor = Color(0xFF673AB7),
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Check out NoteNext")
                                    putExtra(Intent.EXTRA_TEXT, "Hey, check out NoteNext, a cool note-taking app! https://play.google.com/store/apps/details?id=${context.packageName}")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share via"))
                            }
                        )
                    }
                }
            }

            // --- About Section ---
            item {
                SettingsGroupCard(modifier = Modifier.padding(top = 8.dp)) {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = stringResource(id = R.string.about),
                        subtitle = "Version, License & Credits",
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant, // Neutral color for about
                        onClick = { onNavigate("about") }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = androidx.compose.material.icons.Icons.Filled.PrivacyTip,
                        title = "Privacy Policy",
                        subtitle = "Read our privacy policy",
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://suvojeet-sengupta.github.io/NoteNext/"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Handle case where no browser is installed
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }

    // ... (Dialogs remain unchanged)
    if (showThemeDialog) {
        ThemeChooserDialog(
            selectedThemeMode = selectedThemeMode,
            onThemeSelected = { themeMode ->
                scope.launch {
                    settingsRepository.saveThemeMode(themeMode)
                    showThemeDialog = false
                }
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showAutoDeleteDialog) {
        AutoDeleteDialog(
            currentDays = autoDeleteDays,
            onConfirm = { days ->
                scope.launch {
                    settingsRepository.saveAutoDeleteDays(days)
                    showAutoDeleteDialog = false
                }
            },
            onDismiss = { showAutoDeleteDialog = false }
        )
    }



    if (showLanguageDialog) {
        LanguageChooserDialog(
            selectedLanguage = selectedLanguage,
            onLanguageSelected = { language ->
                scope.launch {
                    settingsRepository.saveLanguage(language)
                    val activity = context.findActivity()
                    activity?.recreate()
                    showLanguageDialog = false
                }
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showAppLockInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAppLockInfoDialog = false },
            title = { Text("App Lock") },
            text = { Text("Secure your private notes with a PIN or using your device's biometric authentication (Fingerprint, Face Unlock).") },
            confirmButton = { TextButton(onClick = { showAppLockInfoDialog = false }) { Text("OK") } }
        )
    }

    if (showScreenshotInfoDialog) {
         AlertDialog(
            onDismissRequest = { showScreenshotInfoDialog = false },
            title = { Text("Disallow Screenshots") },
            text = { Text("Prevents taking screenshots or screen recordings while using NoteNext to protect sensitive information.") },
            confirmButton = { TextButton(onClick = { showScreenshotInfoDialog = false }) { Text("OK") } }
        )
    }

    if (showImportSourceDialog) {
        ImportSourceDialog(
            onDismiss = { showImportSourceDialog = false },
            onSelectKeep = {
                showImportSourceDialog = false
                showKeepInstructionsDialog = true
            }
        )
    }

    if (showRateDialog) {
        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            title = { Text(text = "Rate NoteNext") },
            text = { Text("Are you satisfied with NoteNext?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRateDialog = false
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRateDialog = false
                        showContactUsDialog = true
                    }
                ) {
                    Text("No")
                }
            }
        )
    }

    if (showContactUsDialog) {
        AlertDialog(
            onDismissRequest = { showContactUsDialog = false },
            icon = { Icon(Icons.Default.SentimentDissatisfied, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("We are sad to hear that") },
            text = {
                Column {
                    Text("Please let us know how we can improve. Contact us at:")
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Column {
                            Text(
                                text = "suvojitsengupta21@gmail.com",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:suvojitsengupta21@gmail.com")
                                            putExtra(Intent.EXTRA_SUBJECT, "Feedback for NoteNext")
                                        }
                                        try { context.startActivity(intent) } catch (e: Exception) {}
                                    }
                                    .padding(vertical = 4.dp)
                            )
                            Text(
                                text = "suvojeetsengupta@zohomail.in",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:suvojeetsengupta@zohomail.in")
                                            putExtra(Intent.EXTRA_SUBJECT, "Feedback for NoteNext")
                                        }
                                        try { context.startActivity(intent) } catch (e: Exception) {}
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContactUsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showKeepInstructionsDialog) {
        KeepInstructionsDialog(
            onDismiss = { showKeepInstructionsDialog = false },
            onImport = {
                showKeepInstructionsDialog = false
                importKeepLauncher.launch(arrayOf("application/zip"))
            }
        )
    }
}

// --- Custom Components ---

@Composable
private fun SettingsSection(
    title: String,
    color: Color,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = color
            ),
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp) // Adjusted padding
        )
        content()
    }
}

@Composable
private fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow // Restored container color
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    hasSwitch: Boolean = false,
    checked: Boolean = false,
    iconColor: Color,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null
) {
    ListItem(
        modifier = Modifier
            .clickable(enabled = onClick != null || hasSwitch) {
                if (hasSwitch && onCheckedChange != null) {
                    onCheckedChange(!checked)
                } else {
                    onClick?.invoke()
                }
            },
        headlineContent = { 
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
            ) 
        },
        supportingContent = if (subtitle != null) {
            { 
                Text(
                    text = subtitle, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            }
        } else null,
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconColor
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onInfoClick != null) {
                    IconButton(onClick = onInfoClick) {
                        Icon(
                            imageVector = Icons.Default.Info, 
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (hasSwitch && onCheckedChange != null) {
                    Switch(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = iconColor,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline
                        )
                    )
                } else if (onClick != null) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

// --- Dialogs remain mostly the same, just re-included ---

@Composable
private fun LanguageChooserDialog(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.choose_language)) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLanguageSelected("en") }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = (selectedLanguage == "en"), onClick = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(id = R.string.language_english))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLanguageSelected("hi") }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = (selectedLanguage == "hi"), onClick = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(id = R.string.language_hindi))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}



@Composable
private fun ThemeChooserDialog(
    selectedThemeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.choose_theme)) },
        text = {
            Column {
                ThemeMode.values().forEach { themeMode ->
                    ListItem(
                        headlineContent = {
                            Text(
                                when (themeMode) {
                                    ThemeMode.AMOLED -> stringResource(id = R.string.theme_amoled)
                                    else -> themeMode.name.lowercase().replaceFirstChar { it.uppercase() }
                                }
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = (themeMode == selectedThemeMode),
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onThemeSelected(themeMode) },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

@Composable
private fun AutoDeleteDialog(
    currentDays: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(currentDays.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.auto_delete_after)) },
        text = {
            Column {
                Text(
                    text = "${sliderPosition.roundToInt()} days",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
                )
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 1f..60f,
                    steps = 58
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderPosition.roundToInt()) }) {
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun ImportSourceDialog(onDismiss: () -> Unit, onSelectKeep: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose which app to import from") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ImportOptionItem(
                    text = "Google Keep",
                    icon = Icons.Default.Description, // Using generic Description icon
                    color = Color(0xFFFFBB00), // Keep Yellow
                    onClick = onSelectKeep
                )
                ImportOptionItem(
                    text = "Evernote",
                    icon = Icons.Default.Description,
                    color = Color(0xFF00A82D), // Evernote Green
                    enabled = false
                )
                ImportOptionItem(
                    text = "Markdown/Plain Text Files",
                    icon = Icons.Default.Description,
                    enabled = false
                )
                ImportOptionItem(
                    text = "JSON Files",
                    icon = Icons.Default.Description, // Use Code or DataObject if available
                    enabled = false
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ImportOptionItem(
    text: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {},
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) color else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray
        )
    }
}

@Composable
fun KeepInstructionsDialog(onDismiss: () -> Unit, onImport: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from Google Keep") },
        text = {
            Column {
                Text("In order to import your Notes from Google Keep you must download your Google Takeout ZIP file.")
                Spacer(Modifier.height(8.dp))
                
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://takeout.google.com/"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Open Google Takeout", color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(8.dp))
                // Using Description icon as a placeholder for Info if needed, but text is fine.
                Text("Only select the \"Keep\" data. Click Help to get more information.")
                Spacer(Modifier.height(16.dp))
                Text("If you already have a Takeout ZIP file, click Import and choose the ZIP file.")
            }
        },
        confirmButton = {
            TextButton(onClick = onImport) { Text("Import") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://support.google.com/keep/answer/10017039"))
                    context.startActivity(intent)
                }) { Text("Help") }
            }
        }
    )
}