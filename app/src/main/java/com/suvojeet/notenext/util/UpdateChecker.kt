package com.suvojeet.notenext.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Robust utility for managing app updates from the Google Play Store.
 * Handles checking, downloading (flexible), and installing updates.
 */
class UpdateChecker(private val context: Context) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    private val TAG = "UpdateChecker"

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private var appUpdateInfo: AppUpdateInfo? = null
    
    private var isChecking = false
    private var lastCheckTime = 0L
    private val CHECK_THROTTLE_MS = 60 * 60 * 1000L // 1 hour

    sealed class UpdateStatus {
        object Idle : UpdateStatus()
        object Checking : UpdateStatus()
        data class UpdateAvailable(val info: AppUpdateInfo, val isImmediate: Boolean) : UpdateStatus()
        object NoUpdateAvailable : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()
        
        // Flexible update specific states
        object Downloading : UpdateStatus()
        data class DownloadProgress(val bytesDownloaded: Long, val totalBytes: Long) : UpdateStatus()
        object Downloaded : UpdateStatus()
        object Installing : UpdateStatus()
    }

    data class UpdateResult(
        val isUpdateAvailable: Boolean,
        val availableVersionCode: Int = 0,
        val currentVersionCode: Int = 0,
        val stalenessDays: Int? = null,
        val updatePriority: Int = 0
    )

    /**
     * Internal listener for installation state changes.
     */
    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytes = state.bytesDownloaded()
                val total = state.totalBytesToDownload()
                _updateStatus.value = UpdateStatus.DownloadProgress(bytes, total)
            }
            InstallStatus.DOWNLOADED -> {
                _updateStatus.value = UpdateStatus.Downloaded
            }
            InstallStatus.INSTALLING -> {
                _updateStatus.value = UpdateStatus.Installing
            }
            InstallStatus.INSTALLED -> {
                _updateStatus.value = UpdateStatus.Idle
                unregisterListener()
            }
            InstallStatus.FAILED -> {
                _updateStatus.value = UpdateStatus.Error("Installation failed")
                unregisterListener()
            }
            InstallStatus.CANCELED -> {
                _updateStatus.value = UpdateStatus.Idle
                unregisterListener()
            }
            else -> {
                Log.d(TAG, "Install status: ${state.installStatus()}")
            }
        }
    }

    /**
     * Flow-based version of update check for more reactive usage.
     */
    fun getUpdateStatusFlow(): Flow<UpdateStatus> = _updateStatus.asStateFlow()

    /**
     * Checks for updates from the Play Store with internal throttling.
     */
    suspend fun checkForUpdate(force: Boolean = false): Result<UpdateResult> {
        val now = System.currentTimeMillis()
        if (!force && isChecking) {
            Log.d(TAG, "Check already in progress, skipping redundant call.")
            return Result.failure(Exception("Check already in progress"))
        }
        
        if (!force && now - lastCheckTime < CHECK_THROTTLE_MS) {
            Log.d(TAG, "Check throttled. Last check was ${(now - lastCheckTime) / 1000}s ago.")
            // Even if throttled, we might already have info
            appUpdateInfo?.let { info ->
                updateStatusFromInfo(info)
            }
            return Result.failure(Exception("Throttled"))
        }

        return suspendCancellableCoroutine { continuation ->
            isChecking = true
            _updateStatus.value = UpdateStatus.Checking
            
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    isChecking = false
                    lastCheckTime = System.currentTimeMillis()
                    this.appUpdateInfo = info
                    
                    updateStatusFromInfo(info)

                    continuation.resume(
                        Result.success(
                            UpdateResult(
                                isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE,
                                availableVersionCode = info.availableVersionCode(),
                                currentVersionCode = getCurrentVersionCode(),
                                stalenessDays = info.clientVersionStalenessDays(),
                                updatePriority = info.updatePriority()
                            )
                        )
                    )
                }
                .addOnFailureListener { exception ->
                    isChecking = false
                    _updateStatus.value = UpdateStatus.Error(exception.message ?: "Check failed")
                    continuation.resume(Result.failure(exception))
                }
        }
    }

    private fun updateStatusFromInfo(info: AppUpdateInfo) {
        // First, handle ongoing background installation/download
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            _updateStatus.value = UpdateStatus.Downloaded
            return
        } 
        
        if (info.installStatus() == InstallStatus.DOWNLOADING) {
             appUpdateManager.registerListener(installStateListener)
             _updateStatus.value = UpdateStatus.Downloading
             return
        }

        val availability = info.updateAvailability()
        val isAvailable = availability == UpdateAvailability.UPDATE_AVAILABLE || 
                          availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
        
        val priority = info.updatePriority() // 0-5, 5 is highest
        val staleness = info.clientVersionStalenessDays() ?: 0
        
        // Recommend immediate if priority > 3 or staleness > 7 days
        val recommendImmediate = priority >= 4 || staleness > 7

        if (isAvailable) {
            _updateStatus.value = UpdateStatus.UpdateAvailable(info, recommendImmediate)
        } else {
            _updateStatus.value = UpdateStatus.NoUpdateAvailable
        }
    }

    /**
     * Starts the update process.
     * @param activity The activity from which to start the flow.
     * @param requestCode The request code for the update result.
     * @param forceImmediate If true, tries to start an IMMEDIATE update even if FLEXIBLE is recommended.
     */
    fun startUpdate(activity: Activity, requestCode: Int = 500, forceImmediate: Boolean = false) {
        val info = appUpdateInfo ?: return
        
        val updateType = if (forceImmediate || info.updatePriority() >= 4) {
            AppUpdateType.IMMEDIATE
        } else {
            AppUpdateType.FLEXIBLE
        }

        if (info.isUpdateTypeAllowed(updateType)) {
            if (updateType == AppUpdateType.FLEXIBLE) {
                appUpdateManager.registerListener(installStateListener)
            }
            try {
                appUpdateManager.startUpdateFlowForResult(info, updateType, activity, requestCode)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start update flow", e)
                _updateStatus.value = UpdateStatus.Error("Failed to start update: ${e.message}")
            }
        } else if (updateType == AppUpdateType.IMMEDIATE && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            // Fallback to flexible if immediate not allowed
            appUpdateManager.registerListener(installStateListener)
            appUpdateManager.startUpdateFlowForResult(info, AppUpdateType.FLEXIBLE, activity, requestCode)
        } else {
             // Fallback to Play Store page
             openPlayStore(activity)
        }
    }

    /**
     * Completes a flexible update that has been downloaded.
     */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    /**
     * Resumes an update that might be in progress (e.g., when the app is restarted).
     */
    fun resumeUpdateCheck(activity: Activity) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _updateStatus.value = UpdateStatus.Downloaded
            } else if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // Resume immediate update
                if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    appUpdateManager.startUpdateFlowForResult(info, AppUpdateType.IMMEDIATE, activity, 500)
                }
            }
        }
    }

    fun unregisterListener() {
        appUpdateManager.unregisterListener(installStateListener)
    }

    private fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                pInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Open Play Store page for the app.
     */
    fun openPlayStore(activity: Activity) {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=${activity.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        }
    }
}
