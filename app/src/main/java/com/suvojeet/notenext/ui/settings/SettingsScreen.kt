@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.settings

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import org.acra.ACRA
import androidx.core.content.ContextCompat
import android.app.ActivityManager
import android.content.Context
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.theme.ThemeMode
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.SettingsGroupCard
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.R
import com.suvojeet.notenext.util.UpdateChecker
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.biometric.BiometricManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

import com.suvojeet.notenext.util.LogcatManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit, 
    onNavigate: (String) -> Unit,
    viewModel: com.suvojeet.notenext.ui.MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settingsRepository = viewModel.settingsRepository
    val scope = rememberCoroutineScope()

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    val selectedThemeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val autoDeleteDays by settingsRepository.autoDeleteDays.collectAsStateWithLifecycle(initialValue = 7)
    val enableRichLinkPreview by settingsRepository.enableRichLinkPreview.collectAsStateWithLifecycle(initialValue = false)
    val enableAppLock by settingsRepository.enableAppLock.collectAsStateWithLifecycle(initialValue = false)
    val selectedLanguage by settingsRepository.language.collectAsStateWithLifecycle(initialValue = "en")
    val disallowScreenshots by settingsRepository.disallowScreenshots.collectAsStateWithLifecycle(initialValue = false)

    var searchQuery by remember { mutableStateOf("") }

    var showThemeSheet by remember { mutableStateOf(false) }
    var showAutoDeleteDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showBugReportDialog by remember { mutableStateOf(false) }
    var issueDescription by remember { mutableStateOf("") }
    var showImportSourceDialog by remember { mutableStateOf(false) }
    var showKeepInstructionsDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    
    var isLoggingActive by remember { mutableStateOf(LogcatManager.isLogging()) }

    val backupRestoreViewModel: BackupRestoreViewModel = hiltViewModel()
    val importKeepLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { backupRestoreViewModel.importFromGoogleKeep(it) }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Pre-capture colors to avoid @Composable invocation inside remember
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val sections = remember(
        selectedThemeMode, autoDeleteDays, enableRichLinkPreview, enableAppLock, 
        selectedLanguage, disallowScreenshots, showBugReportDialog, isLoggingActive,
        primaryColor, secondaryColor, tertiaryColor, errorColor, surfaceVariantColor
    ) {
        listOf(
            SettingsSectionData(
                title = context.getString(R.string.display_section_title),
                description = context.getString(R.string.settings_section_display_subtitle),
                items = listOf(
                    SettingsItemData(
                        icon = Icons.Rounded.Palette,
                        title = context.getString(R.string.theme),
                        subtitle = selectedThemeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                        iconColor = primaryColor,
                        onClick = { showThemeSheet = true }
                    ),
                    SettingsItemData(
                        icon = Icons.Rounded.Link,
                        title = context.getString(R.string.rich_link_preview),
                        subtitle = context.getString(R.string.rich_link_preview_subtitle),
                        hasSwitch = true,
                        checked = enableRichLinkPreview,
                        iconColor = secondaryColor,
                        onCheckedChange = { scope.launch { settingsRepository.saveEnableRichLinkPreview(it) } }
                    ),
                    SettingsItemData(
                        icon = Icons.Rounded.Language,
                        title = context.getString(R.string.language),
                        subtitle = when (selectedLanguage) {
                            "hi" -> context.getString(R.string.settings_lang_subtitle_hi)
                            "ta" -> context.getString(R.string.settings_lang_subtitle_ta)
                            "bn" -> context.getString(R.string.settings_lang_subtitle_bn)
                            "mr" -> context.getString(R.string.settings_lang_subtitle_mr)
                            "es" -> context.getString(R.string.settings_lang_subtitle_es)
                            else -> context.getString(R.string.settings_lang_subtitle_en)
                        },
                        iconColor = tertiaryColor,
                        onClick = { showLanguageSheet = true }
                    )
                )
            ),
            SettingsSectionData(
                title = context.getString(R.string.settings_section_data),
                description = context.getString(R.string.settings_section_data_subtitle),
                items = listOf(
                    SettingsItemData(
                        icon = Icons.Rounded.Delete,
                        title = context.getString(R.string.settings_auto_cleanup),
                        subtitle = context.getString(R.string.settings_auto_cleanup_subtitle, autoDeleteDays),
                        iconColor = secondaryColor,
                        onClick = { showAutoDeleteDialog = true }
                    ),
                    SettingsItemData(
                        icon = Icons.Rounded.Backup,
                        title = context.getString(R.string.backup_restore_section_title),
                        subtitle = context.getString(R.string.settings_backup_restore_subtitle),
                        iconColor = primaryColor,
                        onClick = { onNavigate("backup") }
                    ),
                    SettingsItemData(
                        icon = Icons.Rounded.Security,
                        title = context.getString(R.string.settings_privacy_security),
                        subtitle = context.getString(R.string.settings_privacy_security_subtitle),
                        iconColor = errorColor,
                        onClick = { onNavigate("privacy") }
                    ),
                    SettingsItemData(
                        icon = Icons.Rounded.ImportExport,
                        title = context.getString(R.string.settings_import_notes),
                        subtitle = context.getString(R.string.settings_import_notes_subtitle),
                        iconColor = tertiaryColor,
                        onClick = { showImportSourceDialog = true }
                    ),
                    SettingsItemData(
                        icon = Icons.Rounded.AutoAwesome,
                        title = context.getString(R.string.settings_ai_label),
                        subtitle = context.getString(R.string.settings_ai_subtitle),
                        iconColor = secondaryColor,
                        onClick = { onNavigate("ai") }
                    )
                )
            ),
            SettingsSectionData(
                title = context.getString(R.string.settings_section_support_logging),
                description = context.getString(R.string.settings_section_support_logging_subtitle),
                items = listOf(
                    SettingsItemData(
                        icon = Icons.Rounded.VolunteerActivism,
                        title = context.getString(R.string.support_notenext),
                        subtitle = context.getString(R.string.donate_small_label),
                        iconColor = errorColor,
                        onClick = { onNavigate("donate") }
                    ),
                    SettingsItemData(
                        icon = Icons.Rounded.Info,
                        title = context.getString(R.string.settings_app_info),
                        subtitle = context.getString(R.string.settings_app_info_subtitle, versionName),
                        iconColor = primaryColor,
                        onClick = { onNavigate("about") }
                    ),
                    SettingsItemData(
                        icon = if (isLoggingActive) Icons.Rounded.StopCircle else Icons.Rounded.PlayCircle,
                        title = if (isLoggingActive) context.getString(R.string.settings_stop_logging) else context.getString(R.string.settings_start_logging),
                        subtitle = if (isLoggingActive) context.getString(R.string.settings_logging_active_subtitle) else context.getString(R.string.settings_start_logging_subtitle),
                        iconColor = if (isLoggingActive) errorColor else primaryColor,
                        onClick = {
                            if (isLoggingActive) {
                                val logFile = LogcatManager.stopLogging()
                                if (logFile != null) {
                                    LogcatManager.shareLogFile(context, logFile)
                                    Toast.makeText(context, context.getString(R.string.settings_log_saved_toast), Toast.LENGTH_SHORT).show()
                                }
                                isLoggingActive = false
                            } else {
                                LogcatManager.startLogging(context)
                                isLoggingActive = true
                                Toast.makeText(context, context.getString(R.string.settings_logging_started_toast), Toast.LENGTH_LONG).show()
                            }
                        }
                    ),
                    SettingsItemData(
                        icon = Icons.Rounded.Code,
                        title = context.getString(R.string.settings_source_code),
                        subtitle = context.getString(R.string.settings_source_code_subtitle),
                        iconColor = surfaceVariantColor,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/suvojeet-sengupta/NoteNext"))
                            context.startActivity(intent)
                        }
                    )
                )
            )
        )
    }

    val filteredSections = remember(searchQuery, sections) {
        if (searchQuery.isEmpty()) sections
        else {
            sections.mapNotNull { section ->
                val filteredItems = section.items.filter { 
                    it.title.contains(searchQuery, ignoreCase = true) || 
                    (it.subtitle?.contains(searchQuery, ignoreCase = true) == true) 
                }
                if (filteredItems.isNotEmpty()) section.copy(items = filteredItems) else null
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.0).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.springPress()) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                androidx.compose.material3.SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text(stringResource(id = R.string.settings_search_placeholder), style = MaterialTheme.typography.bodyLarge) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { 
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.springPress()) {
                                Icon(Icons.Rounded.Close, contentDescription = null)
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {}
            }

            if (searchQuery.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeaturedCard(
                            title = stringResource(id = R.string.settings_card_privacy),
                            subtitle = if (enableAppLock) stringResource(id = R.string.settings_card_protected) else stringResource(id = R.string.settings_card_secure_now),
                            icon = Icons.Rounded.Security,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("privacy") }
                        )
                        FeaturedCard(
                            title = stringResource(id = R.string.settings_card_sync),
                            subtitle = stringResource(id = R.string.settings_card_sync_subtitle),
                            icon = Icons.Rounded.CloudUpload,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("backup") }
                        )
                    }
                }
            }

            filteredSections.forEach { section ->
                item {
                    ExpressiveSection(
                        title = section.title,
                        description = section.description
                    ) {
                        SettingsGroupCard {
                            section.items.forEach { item ->
                                SettingsItem(
                                    icon = item.icon,
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    hasSwitch = item.hasSwitch,
                                    checked = item.checked,
                                    iconColor = item.iconColor,
                                    onCheckedChange = item.onCheckedChange,
                                    onClick = item.onClick
                                )
                            }
                        }
                    }
                }
            }

            if (searchQuery.isEmpty()) {
                item {
                    ExpressiveSection(
                        title = stringResource(id = R.string.settings_section_support_updates),
                        description = stringResource(id = R.string.settings_section_support_updates_subtitle)
                    ) {
                        SettingsGroupCard {
                            SettingsItem(
                                icon = Icons.Rounded.Star,
                                title = stringResource(id = R.string.settings_rate_app),
                                subtitle = stringResource(id = R.string.settings_rate_app_subtitle),
                                iconColor = MaterialTheme.colorScheme.primary,
                                onClick = { showRateDialog = true }
                            )
                            CheckForUpdateItem(viewModel = viewModel)
                            SettingsItem(
                                icon = Icons.Rounded.NewReleases,
                                title = stringResource(id = R.string.settings_whats_new),
                                subtitle = stringResource(id = R.string.settings_whats_new_subtitle),
                                iconColor = MaterialTheme.colorScheme.secondary,
                                onClick = { onNavigate("changelog") }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showThemeSheet) {
        SelectionBottomSheet(
            title = stringResource(id = R.string.choose_theme),
            items = ThemeMode.values().toList(),
            selectedItem = selectedThemeMode,
            onDismiss = { showThemeSheet = false },
            onItemSelected = { theme ->
                scope.launch { settingsRepository.saveThemeMode(theme) }
                showThemeSheet = false
            },
            itemLabel = { mode -> 
                if (mode == ThemeMode.AMOLED) stringResource(id = R.string.theme_amoled) 
                else mode.name.lowercase().replaceFirstChar { it.uppercase() }
            }
        )
    }

    if (showLanguageSheet) {
        val languages = listOf(
            "en" to R.string.language_english,
            "hi" to R.string.language_hindi,
            "ta" to R.string.language_tamil,
            "bn" to R.string.language_bengali,
            "mr" to R.string.language_marathi,
            "es" to R.string.language_spanish,
        )
        SelectionBottomSheet(
            title = stringResource(id = R.string.choose_language),
            items = languages,
            selectedItem = languages.find { it.first == selectedLanguage },
            onDismiss = { showLanguageSheet = false },
            onItemSelected = { (code, _) ->
                scope.launch { settingsRepository.saveLanguage(code) }
                // Apply the locale immediately. AppCompatDelegate triggers an Activity
                // recreate; the MainActivity collect block is a fallback for cold starts.
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
                showLanguageSheet = false
            },
            itemLabel = { stringResource(id = it.second) }
        )
    }

    if (showAutoDeleteDialog) AutoDeleteDialog(autoDeleteDays, { days -> scope.launch { settingsRepository.saveAutoDeleteDays(days) }; showAutoDeleteDialog = false }, { showAutoDeleteDialog = false })
    if (showImportSourceDialog) ImportSourceDialog({ showImportSourceDialog = false }, { showImportSourceDialog = false; showKeepInstructionsDialog = true })
    if (showKeepInstructionsDialog) KeepInstructionsDialog({ showKeepInstructionsDialog = false }, { showKeepInstructionsDialog = false; importKeepLauncher.launch(arrayOf("application/zip")) })
    if (showRateDialog) RateAppDialog(context) { showRateDialog = false }
    if (showBugReportDialog) BugReportDialog(issueDescription, { issueDescription = it }, { 
        showBugReportDialog = false 
        ACRA.errorReporter.putCustomData("IssueDescription", issueDescription)
        ACRA.errorReporter.handleSilentException(Exception("Manual Bug Report: $issueDescription"))
        issueDescription = ""
    }, { showBugReportDialog = false })
}

@Composable
private fun <T> SelectionBottomSheet(
    title: String,
    items: List<T>,
    selectedItem: T?,
    onDismiss: () -> Unit,
    onItemSelected: (T) -> Unit,
    itemLabel: @Composable (T) -> String
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            LazyColumn {
                items(items) { item ->
                    ListItem(
                        modifier = Modifier
                            .clickable { onItemSelected(item) }
                            .padding(horizontal = 8.dp),
                        headlineContent = { 
                            Text(
                                text = itemLabel(item),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (item == selectedItem) FontWeight.Bold else FontWeight.Medium
                            ) 
                        },
                        leadingContent = {
                            RadioButton(
                                selected = (item == selectedItem),
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

private data class SettingsSectionData(
    val title: String,
    val description: String,
    val items: List<SettingsItemData>
)

private data class SettingsItemData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val hasSwitch: Boolean = false,
    val checked: Boolean = false,
    val iconColor: Color,
    val onCheckedChange: ((Boolean) -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
private fun FeaturedCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .springPress()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.8f))
            }
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
            .padding(horizontal = 8.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = onClick != null || hasSwitch) {
                if (hasSwitch && onCheckedChange != null) onCheckedChange(!checked) else onClick?.invoke()
            },
        headlineContent = { Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) },
        supportingContent = subtitle?.let { { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)) } },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = iconColor)
            }
        },
        trailingContent = {
            if (hasSwitch && onCheckedChange != null) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    thumbContent = if (checked) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                    } else null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = iconColor
                    )
                )
            } else if (onClick != null) {
                Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun AutoDeleteDialog(currentDays: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var pos by remember { mutableFloatStateOf(currentDays.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(id = R.string.auto_delete_after)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.days, pos.roundToInt()),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = pos,
                    onValueChange = { pos = it },
                    valueRange = 1f..60f,
                    steps = 58,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                Text(
                    text = stringResource(id = R.string.settings_auto_delete_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = { 
            Button(
                onClick = { onConfirm(pos.roundToInt()) },
                modifier = Modifier.springPress(),
                shape = CircleShape
            ) { 
                Text(stringResource(id = R.string.save)) 
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) { 
                Text(stringResource(id = R.string.cancel)) 
            } 
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun CheckForUpdateItem(viewModel: com.suvojeet.notenext.ui.MainViewModel) {
    val context = LocalContext.current
    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()
    
    val isChecking = updateStatus is UpdateChecker.UpdateStatus.Checking
    
    val currentVersionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown" }
        catch (e: Exception) { "Unknown" }
    }

    SettingsItem(
        icon = Icons.Rounded.Update,
        title = stringResource(R.string.check_for_updates),
        subtitle = if (isChecking) stringResource(R.string.checking_for_updates) else stringResource(R.string.settings_current_version, currentVersionName),
        iconColor = MaterialTheme.colorScheme.primary,
        onClick = {
            if (!isChecking) {
                viewModel.checkForUpdate()
            }
        }
    )
}

@Composable
fun ImportSourceDialog(onDismiss: () -> Unit, onSelectKeep: () -> Unit) {
    val keepColor = Color(0xFFF4B400) // Keep Yellow
    val evernoteColor = Color(0xFF00A82D) // Evernote Green

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Icons.Rounded.ImportExport, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(id = R.string.settings_import_from_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ImportOptionItem("Google Keep", Icons.Rounded.Description, keepColor, onSelectKeep)
                ImportOptionItem("Evernote", Icons.Rounded.Description, evernoteColor, enabled = false)
            }
        },
        confirmButton = {},
        dismissButton = { 
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) { 
                Text(stringResource(id = R.string.cancel)) 
            } 
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
fun ImportOptionItem(text: String, icon: ImageVector, color: Color, onClick: () -> Unit = {}, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = if (enabled) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().springPress()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = if (enabled) color else Color.Gray, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = text, 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            if (!enabled) {
                Spacer(Modifier.weight(1f))
Text(stringResource(id = R.string.settings_coming_soon), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun KeepInstructionsDialog(onDismiss: () -> Unit, onImport: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Icons.Rounded.Help, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(id = R.string.settings_keep_import_title)) },
        text = { Text(stringResource(id = R.string.settings_keep_import_message)) },
        confirmButton = { 
            Button(onClick = onImport, modifier = Modifier.springPress(), shape = CircleShape) { 
                Text(stringResource(id = R.string.settings_select_zip)) 
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) { 
                Text(stringResource(id = R.string.cancel)) 
            } 
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun BugReportDialog(desc: String, onDescChange: (String) -> Unit, onSend: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Icons.Rounded.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(id = R.string.settings_bug_report_title)) },
        text = { 
            Column {
                Text(stringResource(id = R.string.settings_bug_report_message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = desc, 
                    onValueChange = onDescChange, 
                    placeholder = { Text(stringResource(id = R.string.settings_bug_report_placeholder)) }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) 
            }
        },
        confirmButton = { 
            Button(
                onClick = onSend, 
                modifier = Modifier.springPress(), 
                enabled = desc.isNotBlank(),
                shape = CircleShape
            ) { 
                Text(stringResource(id = R.string.settings_send_report)) 
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) { 
                Text(stringResource(id = R.string.cancel)) 
            } 
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
fun RateAppDialog(context: android.content.Context, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFFFB300)) },
        title = { Text(stringResource(id = R.string.settings_rate_app)) },
        text = { Text(stringResource(id = R.string.settings_rate_dialog_message)) },
        confirmButton = { 
            Button(onClick = { 
                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))) }
                catch (e: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))) }
                onDismiss()
            }, modifier = Modifier.springPress(), shape = CircleShape) { 
                Text(stringResource(id = R.string.settings_rate_now)) 
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) { 
                Text(stringResource(id = R.string.settings_later)) 
            } 
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
