package com.bpdlh.pencatatankeuangan.ui.settings

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bpdlh.pencatatankeuangan.AppContainer
import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.ui.asString
import com.bpdlh.pencatatankeuangan.ui.components.Hairline
import com.bpdlh.pencatatankeuangan.ui.components.SectionLabel
import com.bpdlh.pencatatankeuangan.ui.components.StatusText
import com.bpdlh.pencatatankeuangan.ui.formatDateTime
import com.bpdlh.pencatatankeuangan.ui.theme.LocalStatusColors

@Composable
fun SettingsRoot(
    container: AppContainer,
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(
            auth = container.auth,
            records = container.records,
            account = container.account,
            prefs = container.prefs,
            setCrashReporting = container::setCrashReporting,
        )
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }

    SettingsScreen(state = state, onAction = viewModel::onAction, onBackClick = onBack)
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val status = LocalStatusColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SectionLabel(stringResource(R.string.settings_section_account), Modifier.padding(top = 16.dp))
            Text(
                text = state.email ?: stringResource(R.string.settings_not_signed_in),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Text(
                text = stringResource(R.string.settings_id_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SelectionContainer {
                Text(
                    text = state.logId ?: stringResource(R.string.settings_log_id_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(Modifier.height(20.dp))
            Hairline()
            SectionLabel(stringResource(R.string.settings_section_sync), Modifier.padding(top = 16.dp))
            val lastSyncAt = state.sync.lastSyncAt
            val syncText = when {
                state.sync.running -> stringResource(R.string.records_syncing)
                state.sync.lastError != null ->
                    stringResource(R.string.settings_sync_failed, state.sync.lastError.uiText.asString())
                lastSyncAt != null -> stringResource(R.string.settings_last_sync, formatDateTime(lastSyncAt))
                else -> stringResource(R.string.records_never_synced)
            }
            StatusText(
                text = syncText,
                color = if (state.sync.lastError != null) MaterialTheme.colorScheme.error else status.muted,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            Button(
                onClick = { onAction(SettingsAction.OnSignOutClick) },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                enabled = !state.isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) { Text(stringResource(R.string.settings_sign_out), style = MaterialTheme.typography.labelLarge) }
        }
    }

    if (state.showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { onAction(SettingsAction.OnSignOutDismiss) },
            title = { Text(stringResource(R.string.settings_signout_dialog_title)) },
            text = {
                Text(stringResource(R.string.settings_signout_dialog_body))
            },
            confirmButton = {
                TextButton(onClick = { onAction(SettingsAction.OnSignOutDiscardConfirm) }) {
                    Text(stringResource(R.string.settings_sign_out_discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(SettingsAction.OnSignOutDismiss) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
