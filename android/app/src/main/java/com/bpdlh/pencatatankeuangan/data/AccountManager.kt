package com.bpdlh.pencatatankeuangan.data

import com.bpdlh.pencatatankeuangan.auth.AuthRepository

sealed interface SignOutResult {
    data object Done : SignOutResult

    data class PendingUnsynced(val failure: SyncFailure) : SignOutResult
}

class AccountManager(
    private val auth: AuthRepository,
    private val records: RecordsRepository,
    private val sync: SyncCoordinator,
) : SessionController {

    suspend fun syncPending(): SyncOutcome = sync.syncNow()

    override suspend fun signOut(): SignOutResult {
        if (records.pendingCount() > 0) {
            val outcome = sync.syncNow()
            if (outcome is SyncOutcome.Failed && records.pendingCount() > 0) {
                return SignOutResult.PendingUnsynced(outcome.failure)
            }
        }
        wipeAndSignOut()
        return SignOutResult.Done
    }

    override suspend fun signOutDiscardingPending() = wipeAndSignOut()

    override suspend fun deleteAccount(): Result<Unit> {
        val result = auth.deleteAccount()
        if (result.isSuccess) records.wipeLocal()
        return result
    }

    private suspend fun wipeAndSignOut() {
        records.wipeLocal()
        auth.signOut()
    }
}
