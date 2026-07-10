@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.donate

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.billing.BillingState
import com.suvojeet.notenext.billing.PurchaseState
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.springPress

@Composable
fun DonationScreen(
    onBackClick: () -> Unit,
    viewModel: DonationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val billingState by viewModel.billingState.collectAsStateWithLifecycle()
    val purchaseState by viewModel.purchaseState.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    var showThankYouDialog by remember { mutableStateOf(false) }

    LaunchedEffect(purchaseState) {
        if (purchaseState is PurchaseState.Success) {
            showThankYouDialog = true
            viewModel.resetPurchaseState()
        }
    }

    // Thank You Dialog
    if (showThankYouDialog) {
        AlertDialog(
            onDismissRequest = { showThankYouDialog = false },
            icon = {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    stringResource(R.string.thank_you_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    stringResource(R.string.thank_you_msg),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showThankYouDialog = false },
                    modifier = Modifier.springPress()
                ) {
                    Text(stringResource(R.string.thank_you_confirm))
                }
            }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val failedState = purchaseState as? PurchaseState.Failed
    LaunchedEffect(failedState) {
        failedState?.let {
            snackbarHostState.showSnackbar(context.getString(R.string.purchase_failed, it.message))
            viewModel.resetPurchaseState()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.support_notenext),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineLarge,
                        letterSpacing = (-1).sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.springPress()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item {
                DonationHeroSection()
            }

            item {
                ExpressiveSection(
                    title = stringResource(R.string.why_donate),
                    description = stringResource(R.string.donation_description)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DonationFeatureCard(
                            icon = Icons.Rounded.Person,
                            title = stringResource(R.string.reason_independent_title),
                            description = stringResource(R.string.reason_independent_desc),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        DonationFeatureCard(
                            icon = Icons.Rounded.Security,
                            title = stringResource(R.string.reason_privacy_title),
                            description = stringResource(R.string.reason_privacy_desc),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        DonationFeatureCard(
                            icon = Icons.Rounded.AutoAwesome,
                            title = stringResource(R.string.reason_updates_title),
                            description = stringResource(R.string.reason_updates_desc),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            item {
                ExpressiveSection(
                    title = stringResource(R.string.choose_amount),
                    description = stringResource(R.string.secure_google_play)
                ) {
                    when (billingState) {
                        is BillingState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    LoadingIndicator(modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text(stringResource(R.string.connecting_play_store))
                                }
                            }
                        }

                        is BillingState.Error -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(Icons.Rounded.CloudOff, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                    Column {
                                        Text(stringResource(R.string.could_not_connect_play_store), fontWeight = FontWeight.Bold)
                                        Text(stringResource(R.string.check_internet_try_again), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        is BillingState.Ready -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (products.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(stringResource(R.string.loading_options))
                                    }
                                }
                                
                                products.forEach { product ->
                                    val priceStr = product.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                                    val (emoji, label, subtitle) = when (product.productId) {
                                        "donate_small" -> Triple("☕", stringResource(R.string.donate_small_label), stringResource(R.string.donate_small_desc))
                                        "donate_medium" -> Triple("🍕", stringResource(R.string.donate_medium_label), stringResource(R.string.donate_medium_desc))
                                        "donate_large" -> Triple("🚀", stringResource(R.string.donate_large_label), stringResource(R.string.donate_large_desc))
                                        else -> Triple("💙", product.name, "")
                                    }

                                    val isPurchasing = purchaseState is PurchaseState.Pending

                                    DonationActionCard(
                                        emoji = emoji,
                                        title = label,
                                        description = subtitle,
                                        price = priceStr,
                                        isLoading = isPurchasing,
                                        onClick = {
                                            if (!isPurchasing && activity != null) {
                                                viewModel.donate(activity, product)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DonationHeroSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
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
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.VolunteerActivism,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(56.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                stringResource(R.string.made_with_love_free),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            ) {
                Text(
                    "Support NoteNext Development",
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
private fun DonationFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth().springPress(),
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
private fun DonationActionCard(
    emoji: String,
    title: String,
    description: String,
    price: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .springPress()
            .clickable(enabled = !isLoading, onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (description.isNotEmpty()) {
                    Text(
                        text = description, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isLoading) {
                LoadingIndicator(modifier = Modifier.size(24.dp))
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(36.dp),
                    shadowElevation = 2.dp
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            price,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
