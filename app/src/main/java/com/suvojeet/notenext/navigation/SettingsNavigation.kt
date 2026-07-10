package com.suvojeet.notenext.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.spring
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.suvojeet.notenext.ui.settings.SettingsScreen
import com.suvojeet.notenext.ui.settings.AboutScreen
import com.suvojeet.notenext.ui.settings.ContactScreen
import com.suvojeet.notenext.ui.settings.CreditsScreen
import com.suvojeet.notenext.ui.settings.ChangelogScreen
import com.suvojeet.notenext.ui.settings.BackupScreen
import com.suvojeet.notenext.ui.settings.PrivacySecurityScreen
import com.suvojeet.notenext.ui.settings.GroqSettingsScreen
import com.suvojeet.notenext.ui.settings.AIProviderSettingsScreen
import com.suvojeet.notenext.ui.settings.ai.AISettingsScreen
import com.suvojeet.notenext.ui.settings.ai.AIFeaturesScreen
import com.suvojeet.notenext.ui.settings.ai.OnDeviceFeaturesScreen
import com.suvojeet.notenext.ui.settings.ai.AIUsageDashboardScreen
import com.suvojeet.notenext.ui.donate.DonationScreen

fun NavGraphBuilder.settingsGraph(
    navController: NavHostController
) {
    val slideEnter = slideInHorizontally(initialOffsetX = { it }, animationSpec = spring()) + fadeIn(spring())
    val slideExit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring()) + fadeOut(spring())

    composable<Destination.Settings>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        SettingsScreen(
            onBackClick = { navController.popBackStack() },
            onNavigate = { route ->
                when(route) {
                    "backup" -> navController.navigate(Destination.Backup)
                    "privacy" -> navController.navigate(Destination.PrivacySecurity)
                    "about" -> navController.navigate(Destination.About)
                    "donate" -> navController.navigate(Destination.Donate)
                    "changelog" -> navController.navigate(Destination.Changelog)
                    "credits" -> navController.navigate(Destination.Credits)
                    "groq" -> navController.navigate(Destination.GroqSettings)
                    "ai_provider" -> navController.navigate(Destination.AIProviderSettings)
                    "ai" -> navController.navigate(Destination.AISettings)
                    else -> {}
                }
            }
        )
    }

    composable<Destination.PrivacySecurity>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        PrivacySecurityScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.GroqSettings>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        GroqSettingsScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.AIProviderSettings>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        AIProviderSettingsScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.AISettings>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        AISettingsScreen(
            onBackClick = { navController.popBackStack() },
            onOpenFeatures = { navController.navigate(Destination.AIFeatures) },
            onOpenOnDeviceFeatures = { navController.navigate(Destination.OnDeviceFeatures) },
            onOpenDashboard = { navController.navigate(Destination.AIUsageDashboard) }
        )
    }

    composable<Destination.AIFeatures>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        AIFeaturesScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.OnDeviceFeatures>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        OnDeviceFeaturesScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.AIUsageDashboard>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        AIUsageDashboardScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.Backup>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        BackupScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.About>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        AboutScreen(
            onBackClick = { navController.popBackStack() },
            onDonateClick = { navController.navigate(Destination.Donate) },
            onCreditsClick = { navController.navigate(Destination.Credits) },
            onChangelogClick = { navController.navigate(Destination.Changelog) },
            onContactClick = { navController.navigate(Destination.Contact) }
        )
    }

    composable<Destination.Contact>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        ContactScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.Credits>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        CreditsScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.Changelog>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        ChangelogScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.Donate>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        DonationScreen(onBackClick = { navController.popBackStack() })
    }
}
