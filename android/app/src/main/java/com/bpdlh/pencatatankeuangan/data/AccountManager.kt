package com.bpdlh.pencatatankeuangan.data

import com.bpdlh.pencatatankeuangan.auth.AuthRepository

sealed interface SignOutResult {
    data object Done : SignOutResult

    data class PendingUnsynced(val message: String) : SignOutResult
}

class AccountManager(
    private val auth: AuthRepository,
    private val records: RecordsRepository,
    private val sync: SyncCoordinator,
) {

    suspend fun syncPending(): SyncOutcome = sync.syncNow()

    suspend fun signOut(): SignOutResult {
        if (records.pendingCount() > 0) {
            val outcome = sync.syncNow()
            if (outcome is SyncOutcome.Failed && records.pendingCount() > 0) {
                return SignOutResult.PendingUnsynced(outcome.message)
            }
        }
        wipeAndSignOut()
        return SignOutResult.Done
    }

    suspend fun signOutDiscardingPending() = wipeAndSignOut()

    suspend fun deleteAccount(): Result<Unit> {
        val result = auth.deleteAccount()
        if (result.isSuccess) records.wipeLocal()
        return result
    }

    private suspend fun wipeAndSignOut() {
        records.wipeLocal()
        auth.signOut()
    }
}
