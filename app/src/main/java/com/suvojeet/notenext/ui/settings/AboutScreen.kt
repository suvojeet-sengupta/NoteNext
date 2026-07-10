@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.util.NetworkUtils

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onDonateClick: () -> Unit,
    onCreditsClick: () -> Unit,
    onChangelogClick: () -> Unit,
    onContactClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isInternetAvailable = NetworkUtils.isInternetAvailable(context)
    
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.about_screen_title),
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
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item {
                HeroSection()
            }

            item {
                ExpressiveSection(
                    title = stringResource(id = R.string.what_makes_us_different),
                    description = "Key features that define NoteNext"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureCard(
                            icon = Icons.Default.Storage,
                            title = stringResource(id = R.string.local_storage_title),
                            description = stringResource(id = R.string.local_storage_description),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        FeatureCard(
                            icon = Icons.Default.CloudSync,
                            title = "Cloud Backup",
                            description = "Securely backup your notes to Google Drive. Your data, your control.",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        FeatureCard(
                            icon = Icons.Default.Lock,
                            title = "Privacy First",
                            description = "Biometric App Lock and strict privacy. No tracking, no ads.",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            item {
                ExpressiveSection(
                    title = stringResource(id = R.string.about_the_app_title),
                    description = "Our mission and vision"
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = stringResource(id = R.string.about_the_app_description),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 28.sp,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }
                }
            }

            item {
                ExpressiveSection(
                    title = "The Developer",
                    description = "Core maintainer of NoteNext"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ModernTeamMemberCard(
                            name = "Suvojeet Sengupta",
                            role = stringResource(id = R.string.core_developer),
                            avatarUrl = "https://avatars.githubusercontent.com/u/107928380?v=4",
                            githubUrl = "https://github.com/suvojeet-sengupta",
                            websiteUrl = "https://suvojeetsengupta.in",
                            isInternetAvailable = isInternetAvailable,
                            uriHandler = uriHandler
                        )

                        // Contact Button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .springPress()
                                .clickable(onClick = onContactClick),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Contacts, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Contact Me",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Credits Button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .springPress()
                                .clickable(onClick = onCreditsClick),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.Groups, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "View Full Credits",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                ExpressiveSection(
                    title = "Transparency",
                    description = "NoteNext is fully Open Source"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionCard(
                            icon = Icons.Default.Language,
                            title = stringResource(id = R.string.official_website),
                            description = "Visit our official website",
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = { uriHandler.openUri("https://notenext.suvojeetsengupta.in") }
                        )

                        ActionCard(
                            icon = Icons.Default.PrivacyTip,
                            title = stringResource(id = R.string.privacy_policy),
                            description = stringResource(id = R.string.privacy_policy_description),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = { uriHandler.openUri("https://notenext.suvojeetsengupta.in/privacy-policy") }
                        )

                        ActionCard(
                            icon = Icons.Default.Security,
                            title = stringResource(id = R.string.security_details),
                            description = stringResource(id = R.string.security_details_description),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = { uriHandler.openUri("https://notenext.suvojeetsengupta.in/security") }
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .springPress()
                                .clickable { uriHandler.openUri("https://github.com/suvojeet-sengupta/notenext") },
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(id = R.string.open_source_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "View source code on GitHub",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.OpenInNew, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        ActionCard(
                            icon = Icons.Default.NewReleases,
                            title = "What's New",
                            description = "View the latest app changelog",
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = onChangelogClick
                        )
                    }
                }
            }

            item {
                ExpressiveSection(
                    title = "Support Our Work",
                    description = "Help us keep NoteNext free, open & ad-free"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionCard(
                            icon = Icons.Default.Favorite,
                            title = stringResource(id = R.string.support_notenext),
                            description = "Support development via Google Play",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = onDonateClick
                        )
                    }
                }
            }

            item {
                ExpressiveSection(
                    title = "Help & Community",
                    description = "Get involved and spread the word"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionCard(
                            icon = Icons.Default.HelpCenter,
                            title = stringResource(id = R.string.faq),
                            description = stringResource(id = R.string.faq_description),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = { uriHandler.openUri("https://notenext.suvojeetsengupta.in/faq") }
                        )

                        ActionCard(
                            icon = Icons.Default.AutoAwesome,
                            title = stringResource(id = R.string.features_title),
                            description = stringResource(id = R.string.features_description),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = { uriHandler.openUri("https://notenext.suvojeetsengupta.in/features") }
                        )

                        ActionCard(
                            icon = Icons.Default.Share,
                            title = "Share NoteNext",
                            description = "Tell your friends about NoteNext!",
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                val playStoreUrl = "https://play.google.com/store/apps/details?id=com.suvojeet.notenext"
                                val githubUrl = "https://github.com/suvojeet-sengupta/notenext"
                                val websiteUrl = "https://notenext.suvojeetsengupta.in"
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Check out NoteNext, an amazing open-source local-first note app!\n\nWebsite: $websiteUrl\n\nGet it on Play Store: $playStoreUrl\n\nGitHub: $githubUrl")
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, "Share NoteNext via")
                                context.startActivity(shareIntent)
                            }                        )
                        ActionCard(
                            icon = Icons.Default.BugReport,
                            title = "Report a Bug / Request Feature",
                            description = "Help us improve on GitHub",
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = { uriHandler.openUri("https://github.com/suvojeet-sengupta/notenext/issues") }
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        stringResource(id = R.string.made_with_love),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = CircleShape,
                        modifier = Modifier.alpha(0.7f)
                    ) {
                        Text(
                            "Version $versionName Stable",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(id = R.string.notenext_logo),
                    modifier = Modifier.size(80.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                stringResource(id = R.string.notenext),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            ) {
                Text(
                    stringResource(id = R.string.about_subtitle),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .springPress(),
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(contentColor.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold, 
                    color = contentColor
                )
                Text(
                    text = description, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = contentColor.copy(0.8f),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun ModernTeamMemberCard(
    name: String,
    role: String,
    avatarUrl: String,
    githubUrl: String? = null,
    telegramUrl: String? = null,
    websiteUrl: String? = null,
    isInternetAvailable: Boolean,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .springPress()
            .clickable(onClick = { 
                val url = websiteUrl ?: telegramUrl ?: githubUrl
                url?.let { uriHandler.openUri(it) }
            }),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isInternetAvailable) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = role, 
                    style = MaterialTheme.typography.labelLarge, 
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (githubUrl != null || telegramUrl != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(40.dp),
                    shadowElevation = 1.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (telegramUrl != null) Icons.Default.Send else Icons.Default.ArrowOutward, 
                            null, 
                            modifier = Modifier.size(18.dp),
                            tint = if (telegramUrl != null) Color(0xFF24A1DE) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .springPress()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(contentColor.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold, 
                    color = contentColor
                )
                Text(
                    text = description, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = contentColor.copy(0.8f),
                    lineHeight = 20.sp
                )
            }
            Icon(
                imageVector = Icons.Default.OpenInNew, 
                contentDescription = null, 
                modifier = Modifier.size(20.dp),
                tint = contentColor.copy(0.7f)
            )
        }
    }
}
