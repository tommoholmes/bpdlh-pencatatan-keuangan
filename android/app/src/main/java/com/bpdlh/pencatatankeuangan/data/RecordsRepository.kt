package com.bpdlh.pencatatankeuangan.data

import com.bpdlh.pencatatankeuangan.data.remote.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class PlaceSnapshot(val latitude: Double?, val longitude: Double?, val accuracyM: Double?)

class RecordsRepository(
    private val dao: RecordDao,
    private val syncStateDao: SyncStateDao,
    private val photos: PhotoStore,
    private val prefs: Prefs,
    private val api: SyncApi,
    private val scheduleSync: () -> Unit,
    syncCoordinator: SyncCoordinator,
) {
    val syncStatus: StateFlow<SyncStatus> = syncCoordinator.status

    fun observeActive(): Flow<List<Record>> = dao.observeActive()

    fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    suspend fun pendingCount(): Int = dao.countPending()

    fun observeSyncState(): Flow<SyncState?> = syncStateDao.observe()

    suspend fun find(id: String): Record? = dao.findById(id)

    suspend fun syncState(): SyncState {
        syncStateDao.get()?.let { return it }
        val fresh = SyncState(lastPullSeq = 0, deviceId = prefs.deviceId)
        syncStateDao.save(fresh)
        return fresh
    }

    /**
     * Menulis catatan dan menandainya `pending` dalam satu transaksi lokal. UI langsung berubah
     * tanpa menunggu server. Lokasi dan info perangkat hanya diisi saat catatan dibuat.
     */
    suspend fun save(
        existingId: String?,
        name: String,
        price: Long,
        photoPath: String?,
        place: PlaceSnapshot,
    ): String {
        val now = System.currentTimeMillis()
        val device = currentDevice(prefs)
        val existing = existingId?.let { dao.findById(it) }

        val record = if (existing == null) {
            Record(
                id = UUID.randomUUID().toString(),
                name = name,
                price = price,
                photoId = photoPath?.let { UUID.randomUUID().toString() },
                latitude = place.latitude,
                longitude = place.longitude,
                accuracyM = place.accuracyM,
                deviceId = device.deviceId,
                deviceModel = device.model,
                osVersion = device.osVersion,
                appVersion = device.appVersion,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                seq = 0,
                syncStatus = SYNC_PENDING,
                photoPath = photoPath,
            )
        } else {
            val keptPhotoId = when {
                photoPath == null -> null
                existing.photoPath == photoPath -> existing.photoId
                else -> UUID.randomUUID().toString()
            }
            if (existing.photoPath != photoPath) photos.delete(existing.photoPath)
            existing.copy(
                name = name,
                price = price,
                photoId = keptPhotoId,
                photoPath = photoPath,
                updatedAt = now,
                syncStatus = SYNC_PENDING,
            )
        }

        dao.upsert(record)
        scheduleSync()
        return record.id
    }

    suspend fun delete(id: String) {
        val existing = dao.findById(id) ?: return
        photos.delete(existing.photoPath)
        val now = System.currentTimeMillis()
        dao.upsert(
            existing.copy(
                deletedAt = now,
                updatedAt = now,
                syncStatus = SYNC_PENDING,
                photoId = null,
                photoPath = null,
            ),
        )
        scheduleSync()
    }

    /** Dipakai saat logout dan hapus akun: database lokal dan foto ikut dihapus. */
    suspend fun wipeLocal() {
        dao.clearRecords()
        syncStateDao.save(SyncState(lastPullSeq = 0, deviceId = prefs.deviceId))
        photos.clearAll()
        prefs.clearSession()
    }

    /** Foto dari perangkat lain diunduh saat pertama ditampilkan, lalu disimpan lokal. */
    suspend fun ensurePhoto(record: Record): String? {
        record.photoPath?.let { if (java.io.File(it).exists()) return it }
        val photoId = record.photoId ?: return null
        val bytes = api.getPhoto(photoId)
        return photos.save(bytes, photoId).also { dao.setPhotoPath(record.id, it) }
    }


    suspend fun storePhoto(bytes: ByteArray, photoId: String, recordId: String) {
        val path = photos.save(bytes, photoId)
        dao.setPhotoPath(recordId, path)
    }
}
