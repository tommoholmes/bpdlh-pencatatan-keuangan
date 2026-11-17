package com.bpdlh.pencatatankeuangan.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bpdlh.pencatatankeuangan.AppContainer
import com.bpdlh.pencatatankeuangan.data.Record
import com.bpdlh.pencatatankeuangan.data.SYNC_FAILED
import com.bpdlh.pencatatankeuangan.data.SYNC_PENDING
import com.bpdlh.pencatatankeuangan.data.SyncStatus
import com.bpdlh.pencatatankeuangan.ui.components.Hairline
import com.bpdlh.pencatatankeuangan.ui.components.LedgerRow
import com.bpdlh.pencatatankeuangan.ui.components.LoadingBlock
import com.bpdlh.pencatatankeuangan.ui.components.MessageBlock
import com.bpdlh.pencatatankeuangan.ui.formatClock
import com.bpdlh.pencatatankeuangan.ui.formatDate
import com.bpdlh.pencatatankeuangan.ui.formatRupiah
import com.bpdlh.pencatatankeuangan.ui.pendingStaleWarning
import com.bpdlh.pencatatankeuangan.ui.summaryLine
import com.bpdlh.pencatatankeuangan.ui.theme.LocalStatusColors
import com.bpdlh.pencatatankeuangan.ui.theme.PencatatanTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class RecordUi(
    val id: String,
    val name: String,
    val dateLabel: String,
    val amountLabel: String,
    val statusLabel: String?,
    val failed: Boolean,
)

fun Record.toRecordUi(): RecordUi = RecordUi(
    id = id,
    name = name,
    dateLabel = formatDate(createdAt),
    amountLabel = formatRupiah(price),
    statusLabel = when (syncStatus) {
        SYNC_PENDING -> "menunggu dikirim"
        SYNC_FAILED -> "ditolak server"
        else -> null
    },
    failed = syncStatus == SYNC_FAILED,
)

data class RecordsState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val records: List<RecordUi> = emptyList(),
    val summary: String? = null,
    val pendingCount: Int = 0,
    val sync: SyncStatus = SyncStatus(),
    val online: Boolean = true,
    val pendingWarning: String? = null,
)

sealed interface RecordsAction {
    data object OnRetryClick : RecordsAction
}

class RecordsViewModel(container: AppContainer) : ViewModel() {
    private val retry = MutableStateFlow(0)

    private val records: Flow<Result<List<Record>>> = retry.flatMapLatest {
        container.records.observeActive()
            .map<List<Record>, Result<List<Record>>> { Result.success(it) }
            .catch { error -> emit(Result.failure(error)) }
    }

    private val _state: StateFlow<RecordsState> = combine(
        records,
        container.records.observePendingCount(),
        container.records.syncStatus,
        container.connectivity.online,
    ) { loaded, pending, sync, online ->
        val rows = loaded.getOrDefault(emptyList())
        RecordsState(
            isLoading = false,
            error = loaded.exceptionOrNull()?.let { "Catatan tidak bisa dibaca. ${it.message.orEmpty()}" },
            records = rows.map { it.toRecordUi() },
            summary = rows.takeIf { it.isNotEmpty() }?.let { summaryLine(it.size, it.sumOf { row -> row.price }) },
            pendingCount = pending,
            sync = sync,
            online = online,
            pendingWarning = pendingStaleWarning(
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

@Composable
fun RecordsRoot(
    container: AppContainer,
    onOpenRecord: (String) -> Unit,
    onAddRecord: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: RecordsViewModel = viewModel { RecordsViewModel(container) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecordsScreen(
        state = state,
        onAction = viewModel::onAction,
        onRecordClick = onOpenRecord,
        onAddClick = onAddRecord,
        onSettingsClick = onOpenSettings,
    )
}

@Composable
fun RecordsScreen(
    state: RecordsState,
    onAction: (RecordsAction) -> Unit,
    onRecordClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val status = LocalStatusColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Catatan",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            ConnectionLabel(state.online, Modifier.padding(end = 4.dp))
            IconButton(onClick = onAddClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Catat pengeluaran",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Pengaturan",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.summary.orEmpty(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SyncLine(state, Modifier.weight(1f))
        }
        state.pendingWarning?.let { warning ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = warning,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(8.dp))
        Hairline()

        when {
            state.isLoading -> LoadingBlock("Memuat catatan", Modifier.weight(1f))
            state.error != null -> MessageBlock(
                title = "Catatan tidak bisa dibuka",
                body = state.error,
                actionLabel = "Coba lagi",
                onAction = { onAction(RecordsAction.OnRetryClick) },
                modifier = Modifier.weight(1f),
            )
            state.records.isEmpty() -> MessageBlock(
                title = "Belum ada catatan",
                body = "Catat pengeluaran pertama. Foto, lokasi, dan info perangkat ikut tersimpan kalau tersedia.",
                actionLabel = "Catat pengeluaran",
                onAction = onAddClick,
                modifier = Modifier.weight(1f),
            )
            else -> LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.records, key = { it.id }) { row ->
                    LedgerRow(
                        title = row.name,
                        meta = row.dateLabel,
                        amount = row.amountLabel,
                        status = row.statusLabel,
                        statusColor = if (row.failed) MaterialTheme.colorScheme.error else status.pending,
                        onClick = { onRecordClick(row.id) },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SyncLine(state: RecordsState, modifier: Modifier = Modifier) {
    val status = LocalStatusColors.current
    val text: String
    val color: Color
    when {
        state.sync.running -> {
            text = "Menyinkronkan"
            color = status.muted
        }
        state.sync.lastError != null -> {
            text = "Sinkronisasi gagal: ${state.sync.lastError}"
            color = MaterialTheme.colorScheme.error
        }
        state.pendingCount > 0 -> {
            text = "${state.pendingCount} catatan menunggu dikirim"
            color = status.pending
        }
        state.sync.lastSyncAt != null -> {
            text = "Tersinkron pukul ${formatClock(state.sync.lastSyncAt)}"
            color = status.muted
        }
        else -> {
            text = "Belum pernah tersinkron"
            color = status.muted
        }
    }

    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun ConnectionLabel(online: Boolean, modifier: Modifier = Modifier) {
    val status = LocalStatusColors.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(if (online) status.synced else status.muted, CircleShape),
        )
        Text(
            text = if (online) "Online" else "Offline",
            style = MaterialTheme.typography.labelSmall,
            color = status.muted,
        )
    }
}

@Preview
@Composable
private fun RecordsScreenPreview() {
    PencatatanTheme {
        RecordsScreen(
            state = RecordsState(
                isLoading = false,
                records = listOf(
                    RecordUi("1", "Kopi susu", "17 Nov 2026", "Rp 18.000", "menunggu dikirim", false),
                    RecordUi("2", "Bensin motor", "17 Nov 2026", "Rp 45.000", null, false),
                ),
                summary = "2 catatan · Rp 63.000",
                pendingCount = 1,
            ),
            onAction = {},
            onRecordClick = {},
            onAddClick = {},
            onSettingsClick = {},
        )
    }
}
