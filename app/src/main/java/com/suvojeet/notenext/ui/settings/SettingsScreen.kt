package com.suvojeet.notenext.ui.settings

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.ui.theme.ShapeFamily
import com.suvojeet.notenext.ui.settings.SettingsRepository
import com.suvojeet.notenext.ui.settings.ThemeMode
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import com.suvojeet.notenext.util.findActivity

 @OptIn(ExperimentalMaterial3Api::class)
 @Composable
fun SettingsScreen(onBackClick: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    val selectedThemeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val autoDeleteDays by settingsRepository.autoDeleteDays.collectAsState(initial = 7)
    val enableRichLinkPreview by settingsRepository.enableRichLinkPreview.collectAsState(initial = false)
    val enableAppLock by settingsRepository.enableAppLock.collectAsState(initial = false)
    val selectedShapeFamily by settingsRepository.shapeFamily.collectAsState(initial = ShapeFamily.EXPRESSIVE)
    val selectedLanguage by settingsRepository.language.collectAsState(initial = "en")

    var showThemeDialog by remember { mutableStateOf(false) }
    var showAutoDeleteDialog by remember { mutableStateOf(false) }
    var showShapeFamilyDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(id = R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Display Section
            SettingsSectionHeader(
                icon = Icons.Default.Palette,
                title = stringResource(id = R.string.display_section_title)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            SettingsCard(themeMode = selectedThemeMode) {
                Column {
                    SettingsItem(
                        title = stringResource(id = R.string.theme),
                        subtitle = selectedThemeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { showThemeDialog = true }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    SettingsItemWithSwitch(
                        title = stringResource(id = R.string.rich_link_preview),
                        subtitle = stringResource(id = R.string.rich_link_preview_subtitle),
                        checked = enableRichLinkPreview,
                        onCheckedChange = {
                            scope.launch {
                                settingsRepository.saveEnableRichLinkPreview(it)
                            }
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    SettingsItem(
                        title = stringResource(id = R.string.shape_family),
                        subtitle = selectedShapeFamily.name.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { showShapeFamilyDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = stringResource(id = R.string.language),
                        subtitle = stringResource(id = R.string.language_subtitle),
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bin Section
            SettingsSectionHeader(
                icon = Icons.Default.Delete,
                title = stringResource(id = R.string.bin_section_title)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            SettingsCard(themeMode = selectedThemeMode) {
                SettingsItem(
                    title = stringResource(id = R.string.auto_delete_binned_notes),
                    subtitle = stringResource(id = R.string.auto_delete_subtitle, autoDeleteDays),
                    onClick = { showAutoDeleteDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security Section
            SettingsSectionHeader(
                icon = Icons.Default.Security,
                title = stringResource(id = R.string.security_section_title)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            SettingsCard(themeMode = selectedThemeMode) {
                SettingsItemWithSwitch(
                    title = stringResource(id = R.string.app_lock),
                    subtitle = stringResource(id = R.string.app_lock_subtitle),
                    checked = enableAppLock,
                    onCheckedChange = {
                        if (it) {
                            onNavigate("pin_setup")
                        } else {
                            scope.launch {
                                settingsRepository.saveEnableAppLock(false)
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Backup & Restore Section
            SettingsSectionHeader(
                icon = Icons.Default.Backup,
                title = stringResource(id = R.string.backup_restore_section_title)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            SettingsCard(themeMode = selectedThemeMode) {
                Column {
                    SettingsItem(
                        title = stringResource(id = R.string.backup),
                        subtitle = null,
                        onClick = { onNavigate("backup") }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    SettingsItem(
                        title = stringResource(id = R.string.restore),
                        subtitle = null,
                        onClick = { onNavigate("restore") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About Section
            SettingsCard(themeMode = selectedThemeMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("about") }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        stringResource(id = R.string.about),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    AnimatedVisibility(
        visible = showThemeDialog,
        enter = scaleIn(animationSpec = tween(100)),
        exit = scaleOut(animationSpec = tween(100))
    ) {
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

    AnimatedVisibility(
        visible = showAutoDeleteDialog,
        enter = scaleIn(animationSpec = tween(100)),
        exit = scaleOut(animationSpec = tween(100))
    ) {
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

    AnimatedVisibility(
        visible = showShapeFamilyDialog,
        enter = scaleIn(animationSpec = tween(100)),
        exit = scaleOut(animationSpec = tween(100))
    ) {
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

    AnimatedVisibility(
        visible = showLanguageDialog,
        enter = scaleIn(animationSpec = tween(100)),
        exit = scaleOut(animationSpec = tween(100))
    ) {
        LanguageChooserDialog(
            selectedLanguage = selectedLanguage,
            onLanguageSelected = { language ->
                scope.launch {
                    settingsRepository.saveLanguage(language)
                    // Restart activity to apply language change
                    val activity = context.findActivity()
                    activity?.recreate()
                    showLanguageDialog = false
                }
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

 @Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(6.dp)
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

  @Composable
 private fun SettingsCard(
     themeMode: ThemeMode,
     content: @Composable () -> Unit
 ) {
     val isDarkTheme = isSystemInDarkTheme()
     Card(
         modifier = Modifier.fillMaxWidth(),
         shape = RoundedCornerShape(16.dp),
         colors = CardDefaults.cardColors(
             containerColor = if (themeMode == ThemeMode.AMOLED || (isDarkTheme && (themeMode == ThemeMode.DARK || themeMode == ThemeMode.SYSTEM)))
                 Color(0xFF1C1C1C)
             else
                 MaterialTheme.colorScheme.surfaceVariant
         ),
         elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
     ) {
         content()
     }
 }
 @Composable
private fun SettingsItem(
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

 @Composable
private fun SettingsItemWithSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

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
                        .clickable { onLanguageSelected("en") }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedLanguage == "en"),
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(id = R.string.language_english))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLanguageSelected("hi") }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedLanguage == "hi"),
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(id = R.string.language_hindi))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
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
                            .clickable { onShapeFamilySelected(shapeFamily) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (shapeFamily == selectedShapeFamily),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(shapeFamily.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(themeMode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (themeMode == selectedThemeMode),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            when (themeMode) {
                                ThemeMode.AMOLED -> stringResource(id = R.string.theme_amoled)
                                else -> themeMode.name.lowercase().replaceFirstChar { it.uppercase() }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
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
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 1f..60f,
                    steps = 58
                )
                Text(stringResource(id = R.string.days, sliderPosition.roundToInt()), modifier = Modifier.align(Alignment.CenterHorizontally))
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