package com.suvojeet.notenext.util

import android.content.Context
import android.content.Intent
import org.acra.data.CrashReportData
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory
import org.acra.config.CoreConfiguration

class CrashReportSender(private val context: Context) : ReportSender {
    override fun send(context: Context, errorContent: CrashReportData) {
        val reportText = errorContent.toJSON()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "NoteNext Crash Report")
            putExtra(Intent.EXTRA_TEXT, reportText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooserIntent = Intent.createChooser(shareIntent, "Share Crash Report via")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
}

class CrashReportSenderFactory : ReportSenderFactory {
    override fun create(context: Context, config: CoreConfiguration): ReportSender {
        return CrashReportSender(context)
    }

    override fun enabled(config: CoreConfiguration): Boolean = true
}
