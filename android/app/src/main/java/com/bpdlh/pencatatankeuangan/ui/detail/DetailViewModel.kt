package com.bpdlh.pencatatankeuangan.ui.detail

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.data.Record
import com.bpdlh.pencatatankeuangan.data.RecordEditor
import com.bpdlh.pencatatankeuangan.data.SYNC_FAILED
import com.bpdlh.pencatatankeuangan.data.SYNC_PENDING
import com.bpdlh.pencatatankeuangan.ui.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecordDetailUi(
    val name: String,
    val price: Long,
    val createdAt: Long,
    val updatedAt: Long,
    @StringRes val statusRes: Int,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyM: Double?,
    val deviceModel: String?,
    val osVersion: String?,
    val appVersion: String?,
)

fun Record.toRecordDetailUi(): RecordDetailUi = RecordDetailUi(
    name = name,
    price = price,
    createdAt = createdAt,
    updatedAt = updatedAt,
    statusRes = when (syncStatus) {
        SYNC_PENDING -> R.string.status_pending
        SYNC_FAILED -> R.string.status_failed
        else -> R.string.status_synced
    },
    latitude = latitude,
    longitude = longitude,
    accuracyM = accuracyM,
    deviceModel = deviceModel,
    osVersion = osVersion,
    appVersion = appVersion,
)

data class DetailState(
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val detail: RecordDetailUi? = null,
    val photoPath: String? = null,
    val photoFailed: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val deleted: Boolean = false,
)

sealed interface DetailAction {
    data object OnRetryClick : DetailAction
    data object OnDeleteClick : DetailAction
    data object OnDeleteConfirm : DetailAction
    data object OnDeleteDismiss : DetailAction
}

class DetailViewModel(
    private val records: RecordEditor,
    private val recordId: String,
) : ViewModel() {
    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    private var photoJob: Job? = null
    private var attemptedPhotoId: String? = null

    init {
        viewModelScope.launch {
            records.observeRecord(recordId).collect(::apply)
        }
    }

    fun onAction(action: DetailAction) {
        when (action) {
            DetailAction.OnRetryClick -> retryPhoto()
            DetailAction.OnDeleteClick -> _state.value = _state.value.copy(showDeleteDialog = true)
            DetailAction.OnDeleteDismiss -> _state.value = _state.value.copy(showDeleteDialog = false)
            DetailAction.OnDeleteConfirm -> delete()
        }
    }

    private fun apply(record: Record?) {
        if (record == null) {
            _state.value = DetailState(
                isLoading = false,
                error = UiText.Res(R.string.detail_not_found),
            )
            return
        }
        _state.value = _state.value.copy(
            isLoading = false,
            error = null,
            detail = record.toRecordDetailUi(),
            photoPath = record.photoPath,
            deleted = _state.value.deleted || record.deletedAt != null,
        )
        ensurePhoto(record)
    }

    private fun ensurePhoto(record: Record) {
        val photoId = record.photoId
        if (photoId == null) {
            _state.value = _state.value.copy(photoFailed = false)
            return
        }
        val localPath = record.photoPath
        if (localPath != null && java.io.File(localPath).exists()) {
            _state.value = _state.value.copy(photoPath = localPath, photoFailed = false)
            return
        }
        if (photoJob?.isActive == true || attemptedPhotoId == photoId) return
        attemptedPhotoId = photoId
        photoJob = viewModelScope.launch {
            val path = runCatching { records.ensurePhoto(record) }.getOrNull()
            _state.value = _state.value.copy(photoPath = path, photoFailed = path == null)
        }
    }

    private fun retryPhoto() {
        attemptedPhotoId = null
        photoJob?.cancel()
        photoJob = null
        _state.value = _state.value.copy(photoFailed = false)
        viewModelScope.launch {
            val record = records.find(recordId) ?: return@launch
            attemptedPhotoId = null
            ensurePhoto(record)
        }
    }

    private fun delete() {
        _state.value = _state.value.copy(showDeleteDialog = false)
        viewModelScope.launch {
            records.delete(recordId)
            _state.value = _state.value.copy(deleted = true)
        }
    }
}
