package com.suvojeet.notenext.util

import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

object LogcatManager {
    private var logcatProcess: java.lang.Process? = null
    private var logFile: File? = null

    fun startLogging(context: Context) {
        if (isLogging()) return
        
        try {
            val logsDir = File(context.cacheDir, "logs")
            if (!logsDir.exists()) logsDir.mkdirs()
            
            logFile = File(logsDir, "notenext_debug_logs_${System.currentTimeMillis()}.log")
            
            // Get current process ID to filter logs only for this app
            val pid = Process.myPid()
            
            // Execute logcat filtering by PID
            // -f: write to file
            // --pid: filter by process ID
            val logPath = logFile?.absolutePath ?: return
            logcatProcess = Runtime.getRuntime().exec("logcat -f $logPath --pid=$pid *:V")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stopLogging(): File? {
        logcatProcess?.destroy()
        logcatProcess = null
        val capturedFile = logFile
        // Note: logFile is not cleared here so it can be shared immediately after stopping
        return capturedFile
    }

    fun isLogging(): Boolean = logcatProcess != null

    fun shareLogFile(context: Context, file: File) {
        if (!file.exists()) return
        
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(intent, "Share NoteNext Logs").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
