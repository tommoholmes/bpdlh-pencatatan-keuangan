package com.bpdlh.pencatatankeuangan.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.bpdlh.pencatatankeuangan.AppContainer
import com.bpdlh.pencatatankeuangan.data.Record
import com.bpdlh.pencatatankeuangan.data.SYNC_FAILED
import com.bpdlh.pencatatankeuangan.data.SYNC_PENDING
import com.bpdlh.pencatatankeuangan.ui.components.Hairline
import com.bpdlh.pencatatankeuangan.ui.components.KeyValueLine
import com.bpdlh.pencatatankeuangan.ui.components.LoadingBlock
import com.bpdlh.pencatatankeuangan.ui.components.MessageBlock
import com.bpdlh.pencatatankeuangan.ui.components.SecondaryAction
import com.bpdlh.pencatatankeuangan.ui.formatAccuracy
import com.bpdlh.pencatatankeuangan.ui.formatCoordinate
import com.bpdlh.pencatatankeuangan.ui.formatDateTime
import com.bpdlh.pencatatankeuangan.ui.formatRupiah
import com.bpdlh.pencatatankeuangan.ui.theme.AmountTextStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class RecordDetailUi(
    val name: String,
    val amountLabel: String,
    val createdLabel: String,
    val updatedLabel: String,
    val statusLabel: String,
    val latitude: Double?,
    val longitude: Double?,
    val coordinateLabel: String?,
    val accuracyLabel: String?,
    val deviceModel: String,
    val osVersion: String,
    val appVersion: String,
)

fun Record.toRecordDetailUi(): RecordDetailUi {
    val hasPlace = latitude != null && longitude != null
    return RecordDetailUi(
        name = name,
        amountLabel = formatRupiah(price),
        createdLabel = formatDateTime(createdAt),
        updatedLabel = formatDateTime(updatedAt),
        statusLabel = when (syncStatus) {
            SYNC_PENDING -> "Menunggu dikirim"
            SYNC_FAILED -> "Ditolak server"
            else -> "Tersinkron"
        },
        latitude = latitude,
        longitude = longitude,
        coordinateLabel = if (hasPlace) formatCoordinate(latitude, longitude) else null,
        accuracyLabel = accuracyM?.let { "±${formatAccuracy(it)}" },
        deviceModel = deviceModel ?: "Tidak tercatat",
        osVersion = osVersion ?: "Tidak tercatat",
        appVersion = appVersion ?: "Tidak tercatat",
    )
}

data class DetailState(
    val isLoading: Boolean = true,
    val error: String? = null,
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

class DetailViewModel(private val container: AppContainer, private val recordId: String) : ViewModel() {
    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun onAction(action: DetailAction) {
        when (action) {
            DetailAction.OnRetryClick -> load()
            DetailAction.OnDeleteClick -> _state.value = _state.value.copy(showDeleteDialog = true)
            DetailAction.OnDeleteDismiss -> _state.value = _state.value.copy(showDeleteDialog = false)
            DetailAction.OnDeleteConfirm -> delete()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val record = container.records.find(recordId)
            if (record == null) {
                _state.value = DetailState(
                    isLoading = false,
                    error = "Catatan tidak ditemukan. Mungkin sudah dihapus di perangkat lain.",
                )
                return@launch
            }
            _state.value = DetailState(
                isLoading = false,
                detail = record.toRecordDetailUi(),
                photoPath = record.photoPath,
            )
            if (record.photoId != null) {
                val path = runCatching { container.records.ensurePhoto(record) }.getOrNull()
                _state.value = _state.value.copy(photoPath = path, photoFailed = path == null)
            }
        }
    }

    private fun delete() {
        _state.value = _state.value.copy(showDeleteDialog = false)
        viewModelScope.launch {
            container.records.delete(recordId)
            _state.value = _state.value.copy(deleted = true)
        }
    }
}

@Composable
fun DetailRoot(
    container: AppContainer,
    recordId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: DetailViewModel = viewModel(key = recordId) { DetailViewModel(container, recordId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    DetailScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBack,
        onEditClick = onEdit,
    )
}

@Composable
fun DetailScreen(
    state: DetailState,
    onAction: (DetailAction) -> Unit,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    val context = LocalContext.current
    var mapError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Kembali",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            if (state.detail != null) {
                TextButton(onClick = onEditClick, modifier = Modifier.height(48.dp)) { Text("Ubah") }
                TextButton(onClick = { onAction(DetailAction.OnDeleteClick) }, modifier = Modifier.height(48.dp)) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        when {
            state.isLoading -> LoadingBlock("Membuka catatan")
            state.detail == null -> MessageBlock(
                title = "Catatan tidak bisa dibuka",
                body = state.error.orEmpty(),
                actionLabel = "Kembali",
                onAction = onBackClick,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            else -> {
                val detail = state.detail
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = detail.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = detail.amountLabel,
                        style = AmountTextStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    val photoPath = state.photoPath
                    if (photoPath != null) {
                        Spacer(Modifier.height(16.dp))
                        AsyncImage(
                            model = File(photoPath),
                            contentDescription = "Foto catatan ${detail.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                    } else if (state.photoFailed) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Foto belum bisa diunduh. Coba lagi saat ada internet.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SecondaryAction("Muat ulang foto", onClick = { onAction(DetailAction.OnRetryClick) })
                    }

                    Spacer(Modifier.height(16.dp))
                    Hairline()

                    KeyValueLine("Dibuat", detail.createdLabel)
                    KeyValueLine("Diubah", detail.updatedLabel)
                    KeyValueLine("Status", detail.statusLabel)

                    if (detail.coordinateLabel != null && detail.latitude != null && detail.longitude != null) {
                        KeyValueLine("Lokasi", detail.coordinateLabel)
                        detail.accuracyLabel?.let { KeyValueLine("Akurasi", it) }
                        val latitude = detail.latitude
                        val longitude = detail.longitude
                        TextButton(
                            onClick = {
                                val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
                                mapError = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                    .exceptionOrNull()
                                    ?.let { "Tidak ada aplikasi peta di perangkat ini." }
                            },
                        ) { Text("Buka di aplikasi peta") }
                        mapError?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        KeyValueLine("Lokasi", "Tidak dicatat")
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = listOf(detail.deviceModel, detail.osVersion, "app ${detail.appVersion}")
                            .joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { onAction(DetailAction.OnDeleteDismiss) },
            title = { Text("Hapus catatan?") },
            text = { Text("Catatan dihapus dari perangkat ini dan dari perangkat lain saat tersinkron.") },
            confirmButton = {
                TextButton(onClick = { onAction(DetailAction.OnDeleteConfirm) }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(DetailAction.OnDeleteDismiss) }) { Text("Batal") }
            },
        )
    }
}
