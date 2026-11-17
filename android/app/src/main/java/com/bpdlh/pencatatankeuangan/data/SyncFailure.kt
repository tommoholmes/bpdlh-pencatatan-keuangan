package com.bpdlh.pencatatankeuangan.data

import androidx.annotation.StringRes
import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.data.remote.ApiException
import com.bpdlh.pencatatankeuangan.ui.UiText
import java.io.IOException
import java.net.UnknownHostException

sealed interface SyncFailure {
    data object Unauthorized : SyncFailure
    data object PhotoTooLarge : SyncFailure
    data class ClientError(val status: Int) : SyncFailure
    data object ServerError : SyncFailure
    data object Offline : SyncFailure
    data object Disconnected : SyncFailure
    data object Unknown : SyncFailure

    @get:StringRes
    val messageRes: Int
        get() = when (this) {
            Unauthorized -> R.string.sync_error_unauthorized
            PhotoTooLarge -> R.string.sync_error_photo_too_large
            is ClientError -> R.string.sync_error_client
            ServerError -> R.string.sync_error_server
            Offline -> R.string.sync_error_offline
            Disconnected -> R.string.sync_error_disconnected
            Unknown -> R.string.sync_error_unknown
        }

    val uiText: UiText
        get() = when (this) {
            is ClientError -> UiText.Res(messageRes, listOf(status))
            else -> UiText.Res(messageRes)
        }
}

fun syncFailure(error: Throwable): SyncFailure = when {
    error is ApiException && error.status == 401 -> SyncFailure.Unauthorized
    error is ApiException && error.status == 413 -> SyncFailure.PhotoTooLarge
    error is ApiException && error.status in 400..499 -> SyncFailure.ClientError(error.status)
    error is ApiException -> SyncFailure.ServerError
    error is UnknownHostException -> SyncFailure.Offline
    error is IOException -> SyncFailure.Disconnected
    else -> SyncFailure.Unknown
}

/** Disimpan di `SharedPreferences`, jadi bentuknya harus stabil antar versi aplikasi. */
fun SyncFailure.encode(): String = when (this) {
    is SyncFailure.ClientError -> "client:$status"
    else -> javaClass.simpleName.lowercase()
}

fun decodeSyncFailure(raw: String?): SyncFailure? = when {
    raw == null -> null
    raw.startsWith("client:") -> raw.removePrefix("client:").toIntOrNull()?.let { SyncFailure.ClientError(it) }
    raw == "unauthorized" -> SyncFailure.Unauthorized
    raw == "phototoolarge" -> SyncFailure.PhotoTooLarge
    raw == "servererror" -> SyncFailure.ServerError
    raw == "offline" -> SyncFailure.Offline
    raw == "disconnected" -> SyncFailure.Disconnected
    else -> SyncFailure.Unknown
}
