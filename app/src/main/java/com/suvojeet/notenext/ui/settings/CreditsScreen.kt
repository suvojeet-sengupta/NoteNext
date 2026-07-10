@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.util.NetworkUtils
import com.suvojeet.notenext.credits.CreditsProvider
import com.suvojeet.notenext.credits.CreditsData

@Composable
fun CreditsScreen(
    onBackClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isInternetAvailable = NetworkUtils.isInternetAvailable(context)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val creditsData = remember { CreditsProvider.getCredits(context) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Credits",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            creditsData?.let { data ->
                if (data.contributors.isNotEmpty()) {
                    item {
                        ExpressiveSection(
                            title = "Contributors",
                            description = "Special thanks to those who helped NoteNext grow"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                data.contributors.forEach { contributor ->
                                    ModernTeamMemberCard(
                                        name = contributor.name,
                                        role = contributor.role,
                                        avatarUrl = contributor.avatarUrl,
                                        githubUrl = contributor.githubUrl,
                                        telegramUrl = contributor.telegramUrl,
                                        isInternetAvailable = isInternetAvailable,
                                        uriHandler = uriHandler
                                    )
                                }
                            }
                        }
                    }
                }

                if (data.libraries.isNotEmpty()) {
                    item {
                        ExpressiveSection(
                            title = "Open Source Libraries",
                            description = "NoteNext wouldn't be possible without these amazing projects"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                data.libraries.forEach { library ->
                                    LibraryCreditItem(library.name, library.description)
                                }
                            }
                        }
                    }
                }

                if (data.resources.isNotEmpty()) {
                    item {
                        ExpressiveSection(
                            title = "Resources",
                            description = "Design and asset attributions"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                data.resources.forEach { resource ->
                                    LibraryCreditItem(resource.name, resource.description)
                                }
                            }
                        }
                    }
                }
            } ?: item {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading credits...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun LibraryCreditItem(name: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
