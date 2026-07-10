package com.suvojeet.notenext.data.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityUtils {

    private const val SECURE_PREFS_FILE = "backup_secure_prefs"

    fun getSecurePrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveBackupPassword(context: Context, password: String?) {
        val prefs = getSecurePrefs(context)
        if (password == null) {
            prefs.edit().remove("secure_backup_password").apply()
        } else {
            prefs.edit().putString("secure_backup_password", password).apply()
        }
    }

    fun getBackupPassword(context: Context): String? {
        val prefs = getSecurePrefs(context)
        return prefs.getString("secure_backup_password", null)
    }
}
