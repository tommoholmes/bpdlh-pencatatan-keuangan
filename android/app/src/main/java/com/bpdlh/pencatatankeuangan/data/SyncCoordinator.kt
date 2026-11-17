package com.bpdlh.pencatatankeuangan.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SyncStatus(
    val running: Boolean = false,
    val lastSyncAt: Long? = null,
    val lastError: String? = null,
)

/** Satu siklus sinkronisasi dalam satu waktu, dipanggil dari UI maupun WorkManager. */
class SyncCoordinator(private val engine: SyncEngine, private val prefs: Prefs) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow(SyncStatus(lastSyncAt = prefs.lastSyncAt, lastError = prefs.lastSyncError))
    val status: StateFlow<SyncStatus> = _status

    suspend fun syncNow(): SyncOutcome = mutex.withLock {
        _status.value = _status.value.copy(running = true)
        val outcome = engine.run()
        _status.value = SyncStatus(
            running = false,
            lastSyncAt = prefs.lastSyncAt,
            lastError = (outcome as? SyncOutcome.Failed)?.message,
        )
        outcome
    }
}
