package com.suvojeet.notenext

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Build

class ShareReceiverActivity : Activity() {

    companion object {
        // Cap shared text size. Anything beyond this is almost certainly a malformed or
        // hostile share — we silently truncate so a misbehaving sender can't blow up the
        // editor or trigger OOM by pushing megabytes through PendingIntent extras.
        private const val MAX_SHARED_TEXT_LENGTH = 100_000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawText = when {
            intent.action == Intent.ACTION_SEND && "text/plain" == intent.type -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && intent.action == Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            }
            else -> null
        }

        val sharedText = rawText?.take(MAX_SHARED_TEXT_LENGTH)

        if (!sharedText.isNullOrEmpty()) {
            val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, sharedText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(mainActivityIntent)
        }
        finish()
    }
}
