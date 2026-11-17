package com.bpdlh.pencatatankeuangan.data

import android.os.Build
import com.bpdlh.pencatatankeuangan.BuildConfig

/** Info perangkat saat catatan dibuat. Tidak ada IMEI, ANDROID_ID, atau advertising ID. */
data class DeviceSnapshot(val deviceId: String, val model: String, val osVersion: String, val appVersion: String)

fun currentDevice(prefs: Prefs): DeviceSnapshot = DeviceSnapshot(
    deviceId = prefs.deviceId,
    model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".trim(),
    osVersion = "Android ${Build.VERSION.RELEASE}",
    appVersion = BuildConfig.VERSION_NAME,
)
