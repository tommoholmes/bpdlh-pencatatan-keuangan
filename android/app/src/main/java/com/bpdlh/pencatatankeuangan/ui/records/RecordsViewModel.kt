package com.bpdlh.pencatatankeuangan.ui.records

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.data.ConnectivityState
import com.bpdlh.pencatatankeuangan.data.Record
import com.bpdlh.pencatatankeuangan.data.RecordFeed
import com.bpdlh.pencatatankeuangan.data.SYNC_FAILED
import com.bpdlh.pencatatankeuangan.data.SYNC_PENDING
import com.bpdlh.pencatatankeuangan.data.SyncStatus
import com.bpdlh.pencatatankeuangan.ui.UiText
import com.bpdlh.pencatatankeuangan.ui.isStalePending
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Satu baris buku kas. Angka dan tanggal diformat di layar, bukan di sini. */
data class RecordUi(
    val id: String,
    val name: String,
    val price: Long,
    val createdAt: Long,
    @StringRes val statusRes: Int?,
    val failed: Boolean,
)

fun Record.toRecordUi(): RecordUi = RecordUi(
    id = id,
    name = name,
    price = price,
    createdAt = createdAt,
    statusRes = when (syncStatus) {
        SYNC_PENDING -> R.string.status_pending_short
        SYNC_FAILED -> R.string.status_failed_short
        else -> null
    },
    failed = syncStatus == SYNC_FAILED,
)

data class RecordsState(
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val records: List<RecordUi> = emptyList(),
    val total: Long = 0,
    val pendingCount: Int = 0,
    val sync: SyncStatus = SyncStatus(),
    val online: Boolean = true,
    val showStaleWarning: Boolean = false,
)

sealed interface RecordsAction {
    data object OnRetryClick : RecordsAction
}

class RecordsViewModel(
    private val records: RecordFeed,
    connectivity: ConnectivityState,
) : ViewModel() {
    private val retry = MutableStateFlow(0)

    private val activeRecords: Flow<Result<List<Record>>> = retry.flatMapLatest {
        records.observeActive()
            .map<List<Record>, Result<List<Record>>> { Result.success(it) }
            .catch { error -> emit(Result.failure(error)) }
    }

    private val _state: StateFlow<RecordsState> = combine(
        activeRecords,
        records.observePendingCount(),
        records.syncStatus,
        connectivity.online,
    ) { loaded, pending, sync, online ->
        val rows = loaded.getOrDefault(emptyList())
        RecordsState(
            isLoading = false,
            error = loaded.exceptionOrNull()?.let {
                UiText.Res(R.string.records_read_error, listOf(it.message.orEmpty()))
            },
            records = rows.map { it.toRecordUi() },
            total = rows.sumOf { it.price },
            pendingCount = pending,
            sync = sync,
            online = online,
            showStaleWarning = isStalePending(
                rows.filter { it.syncStatus == SYNC_PENDING }.minOfOrNull { it.updatedAt },
                System.currentTimeMillis(),
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsState())

    val state: StateFlow<RecordsState> = _state

    fun onAction(action: RecordsAction) {
        when (action) {
            RecordsAction.OnRetryClick -> retry.value += 1
        }
    }
}
