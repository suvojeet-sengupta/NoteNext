@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.settings.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.ai.AIFeature
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.SettingsGroupCard
import com.suvojeet.notenext.ui.components.springPress

/**
 * Per-feature AI toggles. Every entry shows:
 *   - the friendly name
 *   - a one-liner of what it does
 *   - a help line explaining what data leaves the device when it runs
 *
 * If the master switch is off, the whole list is dimmed and toggles are disabled
 * — this makes the privacy promise visually concrete.
 */
@Composable
fun AIFeaturesScreen(
    onBackClick: () -> Unit,
    viewModel: AIFeaturesViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val masterEnabled by viewModel.masterEnabled.collectAsStateWithLifecycle()
    val states by viewModel.states.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.ai_features_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.0).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.springPress()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!masterEnabled) {
                item { MasterDisabledBanner() }
            }

            item {
                ExpressiveSection(
                    title = stringResource(id = R.string.ai_features_quick_actions),
                    description = stringResource(id = R.string.ai_features_quick_actions_desc)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::disableAll,
                            modifier = Modifier.weight(1f).springPress(),
                            shape = MaterialTheme.shapes.large,
                            enabled = masterEnabled
                        ) {
                            Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(id = R.string.ai_features_disable_all))
                        }
                        Button(
                            onClick = viewModel::enableAll,
                            modifier = Modifier.weight(1f).springPress(),
                            shape = MaterialTheme.shapes.large,
                            enabled = masterEnabled
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(id = R.string.ai_features_enable_all))
                        }
                    }
                }
            }

            // Group by category for readability
            item {
                FeatureGroup(
                    title = stringResource(id = R.string.ai_features_group_ondemand),
                    description = stringResource(id = R.string.ai_features_group_ondemand_desc),
                    features = listOf(
                        AIFeature.SUMMARIZE,
                        AIFeature.CHECKLIST,
                        AIFeature.TODOS,
                        AIFeature.GRAMMAR,
                        AIFeature.TONE_REWRITE,
                        AIFeature.CUSTOM_PROMPT
                    ),
                    masterEnabled = masterEnabled,
                    states = states,
                    onToggle = viewModel::toggle
                )
            }

            item {
                FeatureGroup(
                    title = stringResource(id = R.string.ai_features_group_suggestions),
                    description = stringResource(id = R.string.ai_features_group_suggestions_desc),
                    features = listOf(
                        AIFeature.AUTO_TAG,
                        AIFeature.SMART_REMINDER
                    ),
                    masterEnabled = masterEnabled,
                    states = states,
                    onToggle = viewModel::toggle
                )
            }

            item {
                FeatureGroup(
                    title = stringResource(id = R.string.ai_features_group_ondevice),
                    description = stringResource(id = R.string.ai_features_group_ondevice_desc),
                    features = listOf(
                        AIFeature.LINKED_NOTES
                    ),
                    masterEnabled = true, // Always enabled for on-device features
                    states = states,
                    onToggle = viewModel::toggle
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun MasterDisabledBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(id = R.string.ai_features_master_off_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun FeatureGroup(
    title: String,
    description: String,
    features: List<AIFeature>,
    masterEnabled: Boolean,
    states: Map<AIFeature, Boolean>,
    onToggle: (AIFeature, Boolean) -> Unit
) {
    ExpressiveSection(title = title, description = description) {
        SettingsGroupCard {
            features.forEachIndexed { index, feature ->
                FeatureToggleRow(
                    feature = feature,
                    enabled = states[feature] == true,
                    masterEnabled = masterEnabled,
                    onToggle = { onToggle(feature, it) }
                )
                if (index < features.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureToggleRow(
    feature: AIFeature,
    enabled: Boolean,
    masterEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val (icon, color) = iconFor(feature)
    var expanded by remember { mutableStateOf(false) }

    val effectiveEnabled = enabled && masterEnabled
    val rowAlpha = if (masterEnabled) 1f else 0.45f

    Column(modifier = Modifier.alpha(rowAlpha)) {
        ListItem(
            modifier = Modifier
                .springPress()
                .clickable(enabled = masterEnabled) { expanded = !expanded },
            headlineContent = {
                Text(feature.displayName, fontWeight = FontWeight.SemiBold)
            },
            supportingContent = {
                Text(
                    feature.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(color.copy(alpha = 0.12f), MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color)
                }
            },
            trailingContent = {
                Switch(
                    checked = effectiveEnabled,
                    onCheckedChange = onToggle,
                    enabled = masterEnabled
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = feature.helpText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun iconFor(feature: AIFeature): Pair<ImageVector, Color> = when (feature) {
    AIFeature.SUMMARIZE -> Icons.Rounded.Summarize to Color(0xFF1976D2)
    AIFeature.CHECKLIST -> Icons.Rounded.Checklist to Color(0xFF7B1FA2)
    AIFeature.TODOS -> Icons.Rounded.PlaylistAddCheck to Color(0xFFE65100)
    AIFeature.GRAMMAR -> Icons.Rounded.Spellcheck to Color(0xFF00897B)
    AIFeature.AUTO_TAG -> Icons.Rounded.Sell to Color(0xFFC2185B)
    AIFeature.SMART_REMINDER -> Icons.Rounded.NotificationsActive to Color(0xFFE53935)
    AIFeature.LINKED_NOTES -> Icons.Rounded.Hub to Color(0xFF6750A4)
    AIFeature.TONE_REWRITE -> Icons.Rounded.AutoFixHigh to Color(0xFF43A047)
    AIFeature.CUSTOM_PROMPT -> Icons.Rounded.Code to Color(0xFF455A64)
}
