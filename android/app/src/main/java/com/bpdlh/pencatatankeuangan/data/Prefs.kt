package com.bpdlh.pencatatankeuangan.data

import android.content.Context
import java.util.UUID

/** Setelan pengguna dan identitas perangkat. */
class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null)
            ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_DEVICE_ID, it).apply() }

    var logId: String?
        get() = prefs.getString(KEY_LOG_ID, null)
        set(value) = prefs.edit().putString(KEY_LOG_ID, value).apply()

    /** Laporan masalah aktif sejak aplikasi pertama dipakai; penguji bisa mematikannya. */
    var crashReportsEnabled: Boolean
        get() = prefs.getBoolean(KEY_CRASH, true)
        set(value) = prefs.edit().putBoolean(KEY_CRASH, value).apply()

    var saveLocationEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCATION, true)
        set(value) = prefs.edit().putBoolean(KEY_LOCATION, value).apply()

    var lastSyncAt: Long?
        get() = prefs.getLong(KEY_LAST_SYNC, 0).takeIf { it > 0 }
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value ?: 0).apply()

    var lastSyncError: String?
        get() = prefs.getString(KEY_LAST_ERROR, null)
        set(value) = prefs.edit().putString(KEY_LAST_ERROR, value).apply()

    fun clearSession() {
        prefs.edit().remove(KEY_LOG_ID).remove(KEY_LAST_SYNC).remove(KEY_LAST_ERROR).apply()
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_LOG_ID = "log_id"
        const val KEY_CRASH = "crash_reports"
        const val KEY_LOCATION = "save_location"
        const val KEY_LAST_SYNC = "last_sync_at"
        const val KEY_LAST_ERROR = "last_sync_error"
    }
}
