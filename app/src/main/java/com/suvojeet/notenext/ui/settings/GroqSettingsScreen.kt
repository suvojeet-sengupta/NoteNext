@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.settings

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.SettingsGroupCard
import com.suvojeet.notenext.ui.components.springPress

@Composable
fun GroqSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: GroqSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val useCustomKey by viewModel.useCustomKey.collectAsStateWithLifecycle(initialValue = false)
    val customKey by viewModel.customKey.collectAsStateWithLifecycle(initialValue = "")
    val customFastModel by viewModel.customFastModel.collectAsStateWithLifecycle(initialValue = "llama-3.1-8b-instant")
    val customLargeModel by viewModel.customLargeModel.collectAsStateWithLifecycle(initialValue = "llama-3.3-70b-versatile")
    
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val isLoadingModels by viewModel.isLoadingModels.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showKeyVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.groq_settings_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.0).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.springPress()) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ExpressiveSection(
                    title = "Key Selection",
                    description = "Choose between app default and your own key"
                ) {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Rounded.VpnKey,
                            title = stringResource(R.string.use_custom_api_key),
                            subtitle = stringResource(R.string.use_custom_api_key_subtitle),
                            hasSwitch = true,
                            checked = useCustomKey,
                            iconColor = MaterialTheme.colorScheme.primary,
                            onCheckedChange = { viewModel.updateUseCustomKey(it) }
                        )
                    }
                }
            }

            if (useCustomKey) {
                item {
                    ExpressiveSection(
                        title = "API Configuration",
                        description = "Enter your Groq API key"
                    ) {
                        SettingsGroupCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = customKey,
                                    onValueChange = { viewModel.updateCustomKey(it) },
                                    label = { Text(stringResource(R.string.enter_groq_api_key)) },
                                    placeholder = { Text(stringResource(R.string.api_key_placeholder)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    visualTransformation = if (showKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    trailingIcon = {
                                        IconButton(onClick = { showKeyVisible = !showKeyVisible }) {
                                            Icon(
                                                imageVector = if (showKeyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    shape = MaterialTheme.shapes.medium
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = { viewModel.refreshModels() },
                                    enabled = !isLoadingModels && customKey.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    if (isLoadingModels) {
                                        LoadingIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(stringResource(R.string.fetching_models))
                                    } else {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(stringResource(R.string.fetch_models))
                                    }
                                }
                                
                                error?.let {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    ExpressiveSection(
                        title = "Model Selection",
                        description = "Choose models for different tasks"
                    ) {
                        SettingsGroupCard {
                            ModelSelector(
                                title = stringResource(R.string.select_fast_model),
                                subtitle = stringResource(R.string.select_fast_model_subtitle),
                                selectedModel = customFastModel,
                                availableModels = availableModels,
                                onModelSelected = { viewModel.updateFastModel(it) },
                                icon = Icons.Rounded.Bolt,
                                iconColor = Color(0xFFFFC107)
                            )
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            
                            ModelSelector(
                                title = stringResource(R.string.select_large_model),
                                subtitle = stringResource(R.string.select_large_model_subtitle),
                                selectedModel = customLargeModel,
                                availableModels = availableModels,
                                onModelSelected = { viewModel.updateLargeModel(it) },
                                icon = Icons.Rounded.AutoAwesome,
                                iconColor = Color(0xFF9C27B0)
                            )
                        }
                    }
                }
            }

            item {
                ExpressiveSection(
                    title = stringResource(R.string.api_key_instructions_title),
                    description = "Follow these steps to get your own key"
                ) {
                    SettingsGroupCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InstructionStep(stringResource(R.string.api_key_step_1))
                            InstructionStep(stringResource(R.string.api_key_step_2))
                            InstructionStep(stringResource(R.string.api_key_step_3))
                            InstructionStep(stringResource(R.string.api_key_step_4))
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(R.string.open_groq_console))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstructionStep(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 4.dp),
        lineHeight = 20.sp
    )
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
            .springPress()
            .clickable(enabled = onClick != null || hasSwitch) {
                if (hasSwitch && onCheckedChange != null) onCheckedChange(!checked) else onClick?.invoke()
            },
        headlineContent = { Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) },
        supportingContent = subtitle?.let { { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.12f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = iconColor)
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
                Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
