package com.suvojeet.notenext.ui.settings

import android.content.Context
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

object PreferencesKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_days")
    val ENABLE_RICH_LINK_PREVIEW = booleanPreferencesKey("enable_rich_link_preview")
}

class SettingsRepository(private val context: Context) {

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map {
            preferences ->
            ThemeMode.valueOf(preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit {
            preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    val autoDeleteDays: Flow<Int> = context.dataStore.data
        .map {
            preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_DAYS] ?: 7
        }

    suspend fun saveAutoDeleteDays(days: Int) {
        context.dataStore.edit {
            preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_DAYS] = days
        }
    }

    val enableRichLinkPreview: Flow<Boolean> = context.dataStore.data
        .map {
            preferences ->
            preferences[PreferencesKeys.ENABLE_RICH_LINK_PREVIEW] ?: false
        }

    suspend fun saveEnableRichLinkPreview(enable: Boolean) {
        context.dataStore.edit {
            preferences ->
            preferences[PreferencesKeys.ENABLE_RICH_LINK_PREVIEW] = enable
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    val selectedThemeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val autoDeleteDays by settingsRepository.autoDeleteDays.collectAsState(initial = 7)
    val enableRichLinkPreview by settingsRepository.enableRichLinkPreview.collectAsState(initial = false)

    var showThemeDialog by remember { mutableStateOf(false) }
    var showAutoDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Display", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.padding(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThemeDialog = true }
                        .padding(vertical = 16.dp)
                ) {
                    Column {
                        Text("Theme", style = MaterialTheme.typography.titleMedium)
                        Text(
                            selectedThemeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Rich Link Preview", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Show preview for links in notes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enableRichLinkPreview,
                        onCheckedChange = {
                            scope.launch {
                                settingsRepository.saveEnableRichLinkPreview(it)
                            }
                        }
                    )
                }
            }

            HorizontalDivider()

            Column(modifier = Modifier.padding(16.dp)) {
                Text("Bin", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.padding(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAutoDeleteDialog = true }
                        .padding(vertical = 16.dp)
                ) {
                    Column {
                        Text("Auto-delete binned notes", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "After $autoDeleteDays days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showThemeDialog,
        enter = scaleIn(animationSpec = tween(200)),
        exit = scaleOut(animationSpec = tween(200))
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
        enter = scaleIn(animationSpec = tween(200)),
        exit = scaleOut(animationSpec = tween(200))
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
}

@Composable
private fun ThemeChooserDialog(
    selectedThemeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
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
                        Text(themeMode.name.lowercase().replaceFirstChar { it.uppercase() })
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
private fun AutoDeleteDialog(
    currentDays: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(currentDays.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto-delete after") },
        text = {
            Column {
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 1f..60f,
                    steps = 58
                )
                Text("${sliderPosition.roundToInt()} days", modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderPosition.roundToInt()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
