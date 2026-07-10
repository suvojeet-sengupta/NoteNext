@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.ExpressiveLoading
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.changelog.ChangelogRepository
import com.suvojeet.notenext.changelog.ChangelogList
import com.suvojeet.notenext.changelog.Release
import com.suvojeet.notenext.changelog.ChangelogEntry

@Composable
fun ChangelogScreen(
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    var changelogResult by remember { mutableStateOf<Result<ChangelogList>?>(null) }
    
    LaunchedEffect(Unit) {
        changelogResult = ChangelogRepository.getChangelog()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "What's New",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineLarge,
                        letterSpacing = (-1).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.springPress()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val result = changelogResult
            when {
                result == null -> {
                    ExpressiveLoading()
                }
                result.isFailure -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.CloudOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Failed to load changelog", style = MaterialTheme.typography.titleMedium)
                        Text(result.exceptionOrNull()?.message ?: "Unknown error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { 
                            changelogResult = null
                            // Trigger re-fetch
                        }, modifier = Modifier.springPress()) {
                            Text("Retry")
                        }
                    }
                }
                result.isSuccess -> {
                    val data = result.getOrThrow()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = data.releases,
                            key = { it.version }
                        ) { release ->
                            ReleaseSection(release)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseSection(release: Release) {
    ExpressiveSection(
        title = "Version ${release.version}",
        description = release.date
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                release.items.forEach { entry ->
                    ChangelogItemRow(entry)
                }
            }
        }
    }
}

@Composable
private fun ChangelogItemRow(entry: ChangelogEntry) {
    val (icon, color) = when (entry.type) {
        "FEATURE" -> Icons.Rounded.AddCircle to MaterialTheme.colorScheme.primary
        "FIX" -> Icons.Rounded.Build to MaterialTheme.colorScheme.error
        "IMPROVEMENT" -> Icons.Rounded.AutoAwesome to MaterialTheme.colorScheme.tertiary
        else -> Icons.Rounded.Info to MaterialTheme.colorScheme.secondary
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(text = entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = entry.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
