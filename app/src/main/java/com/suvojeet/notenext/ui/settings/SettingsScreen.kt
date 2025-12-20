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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import com.suvojeet.notenext.ui.theme.ShapeFamily
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
    val selectedShapeFamily by settingsRepository.shapeFamily.collectAsState(initial = ShapeFamily.EXPRESSIVE)
    val selectedLanguage by settingsRepository.language.collectAsState(initial = "en")

    // -- Dialog States --
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAutoDeleteDialog by remember { mutableStateOf(false) }
    var showShapeFamilyDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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
                            icon = Icons.Default.Category,
                            title = stringResource(id = R.string.shape_family),
                            subtitle = selectedShapeFamily.name.lowercase().replaceFirstChar { it.uppercase() },
                            iconColor = Color(0xFF00BCD4),
                            onClick = { showShapeFamilyDialog = true }
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
                            onCheckedChange = {
                                if (it) {
                                    onNavigate("pin_setup")
                                } else {
                                    scope.launch { settingsRepository.saveEnableAppLock(false) }
                                }
                            }
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
                            title = stringResource(id = R.string.backup),
                            subtitle = "Save your notes locally",
                            iconColor = Color(0xFFFF9800),
                            onClick = { onNavigate("backup") }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsItem(
                            icon = Icons.Default.Backup, 
                            title = stringResource(id = R.string.restore),
                            subtitle = "Restore from local backup",
                            iconColor = Color(0xFFFFC107),
                            onClick = { onNavigate("restore") }
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
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                                    context.startActivity(intent)
                                }
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

    if (showShapeFamilyDialog) {
        ShapeFamilyChooserDialog(
            selectedShapeFamily = selectedShapeFamily,
            onShapeFamilySelected = { shapeFamily ->
                scope.launch {
                    settingsRepository.saveShapeFamily(shapeFamily)
                    showShapeFamilyDialog = false
                }
            },
            onDismiss = { showShapeFamilyDialog = false }
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
    onClick: (() -> Unit)? = null
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
private fun ShapeFamilyChooserDialog(
    selectedShapeFamily: ShapeFamily,
    onShapeFamilySelected: (ShapeFamily) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.choose_shape_family)) },
        text = {
            Column {
                ShapeFamily.values().forEach { shapeFamily ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onShapeFamilySelected(shapeFamily) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (shapeFamily == selectedShapeFamily), onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(shapeFamily.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
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