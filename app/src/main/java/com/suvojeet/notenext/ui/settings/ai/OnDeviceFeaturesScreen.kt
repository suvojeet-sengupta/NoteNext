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
import com.suvojeet.notenext.data.ai.AIFeature
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.SettingsGroupCard
import com.suvojeet.notenext.ui.components.springPress

/**
 * Features that run entirely on your device. These do not require an internet
 * connection, an AI provider, or the AI master switch.
 */
@Composable
fun OnDeviceFeaturesScreen(
    onBackClick: () -> Unit,
    viewModel: AIFeaturesViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val states by viewModel.states.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "On-Device Features",
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
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { OnDevicePrivacyBanner() }

            item {
                ExpressiveSection(
                    title = "Local Intelligence",
                    description = "These features use algorithms that stay on your phone. No data is ever sent to the cloud."
                ) {
                    SettingsGroupCard {
                        FeatureToggleRow(
                            feature = AIFeature.LINKED_NOTES,
                            enabled = states[AIFeature.LINKED_NOTES] == true,
                            masterEnabled = true, // On-device features ignore master switch
                            onToggle = { viewModel.toggle(AIFeature.LINKED_NOTES, it) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun OnDevicePrivacyBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "On-device features process your notes locally. They work offline and protect your privacy by keeping everything on your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
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
    AIFeature.LINKED_NOTES -> Icons.Rounded.Hub to Color(0xFF6750A4)
    else -> Icons.Rounded.AutoAwesome to Color.Gray
}
