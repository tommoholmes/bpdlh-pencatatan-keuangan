package com.bpdlh.pencatatankeuangan.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.bpdlh.pencatatankeuangan.data.PlaceSnapshot
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class LocationClient(private val context: Context) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun requestOnce(timeoutMs: Long = 10_000): PlaceSnapshot {
        val empty = PlaceSnapshot(null, null, null)
        if (!hasPermission()) return empty

        val result = withTimeoutOrNull(timeoutMs) {
            runCatching {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
            }.getOrNull()
        } ?: return empty

        return PlaceSnapshot(
            latitude = result.latitude,
            longitude = result.longitude,
            accuracyM = if (result.hasAccuracy()) result.accuracy.toDouble() else null,
        )
    }
}
