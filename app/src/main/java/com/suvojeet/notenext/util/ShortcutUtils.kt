package com.suvojeet.notenext.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.suvojeet.notenext.MainActivity
import com.suvojeet.notenext.R

object ShortcutUtils {

    fun pinProjectToHome(context: Context, projectId: Int, projectName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Fallback for older Android versions (Implicit intent to add shortcut)
            val shortcutIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                putExtra("PROJECT_ID", projectId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val addIntent = Intent().apply {
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                putExtra(Intent.EXTRA_SHORTCUT_NAME, projectName)
                putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, 
                    Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_project_folder))
                action = "com.android.launcher.action.INSTALL_SHORTCUT"
            }
            context.sendBroadcast(addIntent)
            return
        }

        // Modern API for Android 8.0+
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)

        if (shortcutManager?.isRequestPinShortcutSupported == true) {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("PROJECT_ID", projectId)
            }

            val pinShortcutInfo = ShortcutInfo.Builder(context, "project-$projectId")
                .setShortLabel(projectName)
                .setLongLabel(projectName)
                .setIcon(Icon.createWithResource(context, R.drawable.ic_project_folder))
                .setIntent(intent)
                .build()

            // Optional callback when pinned
            val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(pinShortcutInfo)
            val successCallback = PendingIntent.getBroadcast(
                context, 0, pinnedShortcutCallbackIntent, 
                PendingIntent.FLAG_IMMUTABLE
            )

            shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.intentSender)
        }
    }
}
