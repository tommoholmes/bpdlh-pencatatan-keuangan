package com.bpdlh.pencatatankeuangan.data

import com.bpdlh.pencatatankeuangan.data.remote.PushRecord
import com.bpdlh.pencatatankeuangan.data.remote.SyncApi
import kotlinx.coroutines.CancellationException

sealed interface SyncOutcome {
    data object NotSignedIn : SyncOutcome
    data class Done(val pushed: Int, val pulled: Int) : SyncOutcome
    data class Failed(val failure: SyncFailure) : SyncOutcome
}

class SyncEngine(
    private val dao: RecordDao,
    private val syncStateDao: SyncStateDao,
    private val api: SyncApi,
    private val photos: PhotoStore,
    private val prefs: Prefs,
) {
    suspend fun run(): SyncOutcome = try {
        val syncId = java.util.UUID.randomUUID().toString()
        val state = syncStateDao.get() ?: SyncState(lastPullSeq = 0, deviceId = prefs.deviceId)
        if (prefs.logId == null) prefs.logId = api.bootstrap(syncId)

        val queue = dao.pending(BATCH)
        for (record in queue) {
            val path = record.photoPath
            val photoId = record.photoId
            if (path != null && photoId != null) api.putPhoto(photoId, photos.read(path), syncId)
        }

        var pushed = 0
        if (queue.isNotEmpty()) {
            val response = api.push(state.deviceId, queue.map { it.toPushRecord() }, syncId)
            for (result in response.results) {
                val sent = queue.firstOrNull { it.id == result.id } ?: continue
                if (result.status == "applied") {
                    dao.markStatusIfUnchanged(sent.id, sent.updatedAt, SYNC_SYNCED)
                    pushed++
                } else {
                    dao.markStatusIfUnchanged(sent.id, sent.updatedAt, SYNC_FAILED)
                    if (io.sentry.Sentry.isEnabled()) {
                        io.sentry.Sentry.captureMessage(
                            "catatan ditolak: ${result.reason ?: "tanpa alasan"}",
                            io.sentry.SentryLevel.WARNING,
                        )
                    }
                }
            }
        }

        val pulled = pullAll(state, syncId)
        prefs.lastSyncAt = System.currentTimeMillis()
        prefs.lastSyncError = null
        SyncOutcome.Done(pushed, pulled)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (io.sentry.Sentry.isEnabled()) io.sentry.Sentry.captureException(e)
        val failure = syncFailure(e)
        prefs.lastSyncError = failure
        SyncOutcome.Failed(failure)
    }

    private suspend fun pullAll(state: SyncState, syncId: String): Int {
        var since = state.lastPullSeq
        var total = 0
        var pages = 0
        while (pages < MAX_PAGES) {
            val page = api.pull(since, syncId = syncId)
            dao.applyRemote(page.records.map { it.toRecord() })
            total += page.records.size
            since = page.nextSeq
            syncStateDao.save(state.copy(lastPullSeq = since))
            pages++
            if (!page.hasMore) break
        }
        return total
    }

    private companion object {
        const val BATCH = 20
        const val MAX_PAGES = 20
    }
}

private fun Record.toPushRecord() = PushRecord(
    id = id,
    name = name,
    price = price,
    photoId = photoId,
    latitude = latitude,
    longitude = longitude,
    accuracyM = accuracyM,
    deviceModel = deviceModel,
    osVersion = osVersion,
    appVersion = appVersion,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun com.bpdlh.pencatatankeuangan.data.remote.PulledRecord.toRecord() = Record(
    id = id,
    name = name,
    price = price,
    photoId = photoId,
    latitude = latitude,
    longitude = longitude,
    accuracyM = accuracyM,
    deviceId = deviceId,
    deviceModel = deviceModel,
    osVersion = osVersion,
    appVersion = appVersion,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    seq = seq,
    syncStatus = SYNC_SYNCED,
    photoPath = null,
)
