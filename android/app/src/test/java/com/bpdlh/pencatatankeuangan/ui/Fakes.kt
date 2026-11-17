package com.bpdlh.pencatatankeuangan.ui

import android.net.Uri
import com.bpdlh.pencatatankeuangan.data.AuthGateway
import com.bpdlh.pencatatankeuangan.data.ConnectivityState
import com.bpdlh.pencatatankeuangan.data.LocationProvider
import com.bpdlh.pencatatankeuangan.data.PhotoLibrary
import com.bpdlh.pencatatankeuangan.data.PlaceSnapshot
import com.bpdlh.pencatatankeuangan.data.Record
import com.bpdlh.pencatatankeuangan.data.RecordEditor
import com.bpdlh.pencatatankeuangan.data.RecordFeed
import com.bpdlh.pencatatankeuangan.data.SYNC_PENDING
import com.bpdlh.pencatatankeuangan.data.SessionController
import com.bpdlh.pencatatankeuangan.data.SettingsStore
import com.bpdlh.pencatatankeuangan.data.SignOutResult
import com.bpdlh.pencatatankeuangan.data.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

fun testRecord(
    id: String = "rec-1",
    name: String = "Kopi susu",
    price: Long = 18_000,
    syncStatus: String = SYNC_PENDING,
    updatedAt: Long = 1_794_800_000_000,
    photoId: String? = null,
) = Record(
    id = id,
    name = name,
    price = price,
    photoId = photoId,
    latitude = null,
    longitude = null,
    accuracyM = null,
    deviceId = "dev-1",
    deviceModel = "Pixel 8",
    osVersion = "Android 15",
    appVersion = "1.0.0",
    createdAt = 1_794_800_000_000,
    updatedAt = updatedAt,
    deletedAt = null,
    seq = 0,
    syncStatus = syncStatus,
    photoPath = null,
)

class FakeAuth(
    override var currentUserEmail: String? = "budi@example.com",
    var signInResult: Result<Unit> = Result.success(Unit),
    var bootstrapResult: Result<Unit> = Result.success(Unit),
) : AuthGateway {
    var signInCalls = 0
    var bootstrapCalls = 0

    override suspend fun signInWithGoogle(): Result<Unit> {
        signInCalls++
        return signInResult
    }

    override suspend fun bootstrap(): Result<Unit> {
        bootstrapCalls++
        return bootstrapResult
    }
}

class FakeFeed(
    initial: List<Record> = emptyList(),
    pending: Int = 0,
) : RecordFeed {
    val records = MutableStateFlow(initial)
    val pendingCount = MutableStateFlow(pending)
    override val syncStatus: MutableStateFlow<SyncStatus> = MutableStateFlow(SyncStatus())

    override fun observeActive(): Flow<List<Record>> = records
    override fun observePendingCount(): Flow<Int> = pendingCount
}

class FakeEditor(
    record: Record? = null,
    var photoPath: String? = null,
) : RecordEditor {
    val recordFlow = MutableStateFlow(record)

    var record: Record?
        get() = recordFlow.value
        set(value) {
            recordFlow.value = value
        }

    var savedId: String? = null
    var savedName: String? = null
    var savedPrice: Long? = null
    var savedPhotoPath: String? = null
    var deletedId: String? = null
    var saveCalls = 0

    override fun observeRecord(id: String): Flow<Record?> = recordFlow

    override suspend fun find(id: String): Record? = record

    override suspend fun save(
        existingId: String?,
        name: String,
        price: Long,
        photoPath: String?,
        place: PlaceSnapshot,
    ): String {
        saveCalls++
        savedId = existingId
        savedName = name
        savedPrice = price
        savedPhotoPath = photoPath
        return existingId ?: "rec-baru"
    }

    override suspend fun delete(id: String) {
        deletedId = id
    }

    var ensurePhotoCalls = 0

    override suspend fun ensurePhoto(record: Record): String? {
        ensurePhotoCalls++
        return photoPath
    }
}

class FakePhotos : PhotoLibrary {
    var deletedPath: String? = null
    var importedUri: Uri? = null

    override suspend fun import(uri: Uri): String {
        importedUri = uri
        return "/data/photos/baru.jpg"
    }

    override fun delete(path: String?) {
        deletedPath = path
    }
}

class FakeLocation(
    var place: PlaceSnapshot = PlaceSnapshot(-7.7956, 110.3695, 12.5),
    var granted: Boolean = true,
) : LocationProvider {
    var calls = 0

    override fun hasPermission(): Boolean = granted

    override suspend fun requestOnce(timeoutMs: Long): PlaceSnapshot {
        calls++
        return if (granted) place else PlaceSnapshot(null, null, null)
    }
}

class FakeSettings(
    override var logId: String? = "log-1",
    override var crashReportsEnabled: Boolean = true,
    override var saveLocationEnabled: Boolean = true,
) : SettingsStore

class FakeConnectivity(var onlineNow: Boolean = true) : ConnectivityState {
    override val online: StateFlow<Boolean> = MutableStateFlow(onlineNow)
}

class FakeSession(
    var signOutResult: SignOutResult = SignOutResult.Done,
    var deleteResult: Result<Unit> = Result.success(Unit),
) : SessionController {
    var signOutCalls = 0
    var discardCalls = 0
    var deleteCalls = 0

    override suspend fun signOut(): SignOutResult {
        signOutCalls++
        return signOutResult
    }

    override suspend fun signOutDiscardingPending() {
        discardCalls++
    }

    override suspend fun deleteAccount(): Result<Unit> {
        deleteCalls++
        return deleteResult
    }
}
