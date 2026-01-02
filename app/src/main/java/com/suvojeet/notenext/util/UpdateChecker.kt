package com.suvojeet.notenext.util

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Utility class for checking app updates from Play Store.
 */
class UpdateChecker(context: Context) {
    
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    
    data class UpdateResult(
        val isUpdateAvailable: Boolean,
        val availableVersionCode: Int = 0,
        val currentVersionCode: Int = 0,
        val stalenessDays: Int? = null
    )
    
    /**
     * Check if an update is available on Play Store.
     * Returns UpdateResult with update availability info.
     */
    suspend fun checkForUpdate(): Result<UpdateResult> = suspendCancellableCoroutine { continuation ->
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
                val isUpdateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                
                continuation.resume(
                    Result.success(
                        UpdateResult(
                            isUpdateAvailable = isUpdateAvailable,
                            availableVersionCode = if (isUpdateAvailable) appUpdateInfo.availableVersionCode() else 0,
                            currentVersionCode = appUpdateInfo.availableVersionCode(), 
                            stalenessDays = appUpdateInfo.clientVersionStalenessDays()
                        )
                    )
                )
            }
            .addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
    }
    
    /**
     * Open Play Store page for the app.
     */
    fun openPlayStore(activity: Activity) {
        try {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("market://details?id=${activity.packageName}")
            )
            activity.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")
            )
            activity.startActivity(intent)
        }
    }
}
