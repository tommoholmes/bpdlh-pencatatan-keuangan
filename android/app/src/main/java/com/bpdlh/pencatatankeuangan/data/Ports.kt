package com.bpdlh.pencatatankeuangan.data

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthGateway {
    val currentUserEmail: String?
    suspend fun signInWithGoogle(): Result<Unit>
    suspend fun bootstrap(): Result<Unit>
}

interface SessionController {
    suspend fun signOut(): SignOutResult
    suspend fun signOutDiscardingPending()
    suspend fun deleteAccount(): Result<Unit>
}

interface RecordFeed {
    val syncStatus: StateFlow<SyncStatus>
    fun observeActive(): Flow<List<Record>>
    fun observePendingCount(): Flow<Int>
}

interface RecordEditor {
    fun observeRecord(id: String): Flow<Record?>
    suspend fun find(id: String): Record?
    suspend fun save(
        existingId: String?,
        name: String,
        price: Long,
        photoPath: String?,
        place: PlaceSnapshot,
    ): String
    suspend fun delete(id: String)
    suspend fun ensurePhoto(record: Record): String?
}

interface PhotoLibrary {
    suspend fun import(uri: Uri): String
    fun delete(path: String?)
}

interface LocationProvider {
    fun hasPermission(): Boolean
    suspend fun requestOnce(timeoutMs: Long = 10_000): PlaceSnapshot
}

interface SettingsStore {
    var logId: String?
    var crashReportsEnabled: Boolean
    var saveLocationEnabled: Boolean
}

interface ConnectivityState {
    val online: StateFlow<Boolean>
}
