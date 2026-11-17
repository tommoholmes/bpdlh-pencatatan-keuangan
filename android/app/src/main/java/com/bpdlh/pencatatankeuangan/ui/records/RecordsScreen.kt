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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bpdlh.pencatatankeuangan.AppContainer
import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.ui.asString
import com.bpdlh.pencatatankeuangan.ui.components.Hairline
import com.bpdlh.pencatatankeuangan.ui.components.LedgerRow
import com.bpdlh.pencatatankeuangan.ui.components.LoadingBlock
import com.bpdlh.pencatatankeuangan.ui.components.MessageBlock
import com.bpdlh.pencatatankeuangan.ui.formatClock
import com.bpdlh.pencatatankeuangan.ui.formatDate
import com.bpdlh.pencatatankeuangan.ui.formatNumber
import com.bpdlh.pencatatankeuangan.ui.theme.LocalStatusColors
import com.bpdlh.pencatatankeuangan.ui.theme.PencatatanTheme

@Composable
fun RecordsRoot(
    container: AppContainer,
    onOpenRecord: (String) -> Unit,
    onAddRecord: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: RecordsViewModel = viewModel { RecordsViewModel(container.records, container.connectivity) },
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
                text = stringResource(R.string.records_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            ConnectionLabel(state.online, Modifier.padding(end = 4.dp))
            IconButton(onClick = onAddClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.records_cd_add),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.records_cd_settings),
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
                text = if (state.records.isEmpty()) {
                    ""
                } else {
                    stringResource(
                        R.string.records_summary,
                        state.records.size,
                        stringResource(R.string.currency_amount, formatNumber(state.total)),
                    )
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SyncLine(state, Modifier.weight(1f))
        }
        if (state.showStaleWarning) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.records_stale_warning),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(8.dp))
        Hairline()

        when {
            state.isLoading -> LoadingBlock(stringResource(R.string.records_loading), Modifier.weight(1f))
            state.error != null -> MessageBlock(
                title = stringResource(R.string.records_error_title),
                body = state.error.asString(),
                actionLabel = stringResource(R.string.action_retry),
                onAction = { onAction(RecordsAction.OnRetryClick) },
                modifier = Modifier.weight(1f),
            )
            state.records.isEmpty() -> MessageBlock(
                title = stringResource(R.string.records_empty_title),
                body = stringResource(R.string.records_empty_body),
                actionLabel = stringResource(R.string.records_empty_action),
                onAction = onAddClick,
                modifier = Modifier.weight(1f),
            )
            else -> LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.records, key = { it.id }) { row ->
                    LedgerRow(
                        title = row.name,
                        meta = formatDate(row.createdAt),
                        amount = stringResource(R.string.currency_amount, formatNumber(row.price)),
                        status = row.statusRes?.let { stringResource(it) },
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
            text = stringResource(R.string.records_syncing)
            color = status.muted
        }
        state.sync.lastError != null -> {
            text = stringResource(R.string.records_sync_failed, state.sync.lastError.uiText.asString())
            color = MaterialTheme.colorScheme.error
        }
        state.pendingCount > 0 -> {
            text = stringResource(R.string.records_pending_count, state.pendingCount)
            color = status.pending
        }
        state.sync.lastSyncAt != null -> {
            text = stringResource(R.string.records_synced_at, formatClock(state.sync.lastSyncAt))
            color = status.muted
        }
        else -> {
            text = stringResource(R.string.records_never_synced)
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
            text = stringResource(if (online) R.string.records_online else R.string.records_offline),
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
                    RecordUi("1", "Kopi susu", 18_000, 1_794_800_000_000, R.string.status_pending_short, false),
                    RecordUi("2", "Bensin motor", 45_000, 1_794_800_000_000, null, false),
                ),
                total = 63_000,
                pendingCount = 1,
            ),
            onAction = {},
            onRecordClick = {},
            onAddClick = {},
            onSettingsClick = {},
        )
    }
}
