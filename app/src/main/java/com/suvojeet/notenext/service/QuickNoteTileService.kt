package com.suvojeet.notenext.service

import android.content.Intent
import android.service.quicksettings.TileService
import com.suvojeet.notenext.MainActivity

class QuickNoteTileService : TileService() {

    override fun onClick() {
        super.onClick()
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = "com.suvojeet.notenext.ACTION_CREATE_NOTE"
            putExtra("START_ADD_NOTE", true)
        }
        startActivityAndCollapse(intent)
    }
}
