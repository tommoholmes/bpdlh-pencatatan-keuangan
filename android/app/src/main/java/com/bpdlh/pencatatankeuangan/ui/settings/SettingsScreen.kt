package com.bpdlh.pencatatankeuangan.ui.settings

import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bpdlh.pencatatankeuangan.AppContainer
import com.bpdlh.pencatatankeuangan.data.SignOutResult
import com.bpdlh.pencatatankeuangan.data.SyncStatus
import com.bpdlh.pencatatankeuangan.ui.components.Hairline
import com.bpdlh.pencatatankeuangan.ui.components.SectionLabel
import com.bpdlh.pencatatankeuangan.ui.components.StatusText
import com.bpdlh.pencatatankeuangan.ui.formatDateTime
import com.bpdlh.pencatatankeuangan.ui.theme.LocalStatusColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val email: String? = null,
    val logId: String? = null,
    val crashReports: Boolean = true,
    val saveLocation: Boolean = true,
    val sync: SyncStatus = SyncStatus(),
    val isBusy: Boolean = false,
    val message: String? = null,
    val showSignOutDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val signedOut: Boolean = false,
)

sealed interface SettingsAction {
    data class OnCrashReportsChange(val enabled: Boolean) : SettingsAction
    data class OnSaveLocationChange(val enabled: Boolean) : SettingsAction
    data object OnSignOutClick : SettingsAction
    data object OnSignOutDiscardConfirm : SettingsAction
    data object OnSignOutDismiss : SettingsAction
    data object OnDeleteAccountClick : SettingsAction
    data object OnDeleteAccountConfirm : SettingsAction
    data object OnDeleteAccountDismiss : SettingsAction
}

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(
        SettingsState(
            email = container.auth.currentUser?.email,
            logId = container.prefs.logId,
            crashReports = container.prefs.crashReportsEnabled,
            saveLocation = container.prefs.saveLocationEnabled,
            sync = container.records.syncStatus.value,
        ),
    )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.records.syncStatus.collect { status ->
                _state.update { it.copy(sync = status, logId = container.prefs.logId) }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnCrashReportsChange -> {
                container.setCrashReporting(action.enabled)
                _state.update { it.copy(crashReports = action.enabled) }
            }
            is SettingsAction.OnSaveLocationChange -> {
                container.prefs.saveLocationEnabled = action.enabled
                _state.update { it.copy(saveLocation = action.enabled) }
            }
            SettingsAction.OnSignOutClick -> signOut()
            SettingsAction.OnSignOutDiscardConfirm -> signOutDiscardingPending()
            SettingsAction.OnSignOutDismiss -> _state.update { it.copy(showSignOutDialog = false) }
            SettingsAction.OnDeleteAccountClick ->
                _state.update { it.copy(showDeleteAccountDialog = true, message = null) }
            SettingsAction.OnDeleteAccountConfirm -> deleteAccount()
            SettingsAction.OnDeleteAccountDismiss -> _state.update { it.copy(showDeleteAccountDialog = false) }
        }
    }

    private fun signOut() {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            when (val result = container.account.signOut()) {
                is SignOutResult.Done -> _state.update { it.copy(isBusy = false, signedOut = true) }
                is SignOutResult.PendingUnsynced -> _state.update {
                    it.copy(
                        isBusy = false,
                        showSignOutDialog = true,
                        message = "Sinkronisasi gagal: ${result.message}",
                    )
                }
            }
        }
    }

    private fun signOutDiscardingPending() {
        _state.update { it.copy(showSignOutDialog = false, isBusy = true) }
        viewModelScope.launch {
            container.account.signOutDiscardingPending()
            _state.update { it.copy(isBusy = false, signedOut = true) }
        }
    }

    private fun deleteAccount() {
        _state.update { it.copy(showDeleteAccountDialog = false, isBusy = true) }
        viewModelScope.launch {
            val result = container.account.deleteAccount()
            _state.update {
                if (result.isFailure) {
                    it.copy(isBusy = false, message = result.exceptionOrNull()?.message ?: "Hapus akun gagal.")
                } else {
                    it.copy(isBusy = false, signedOut = true)
                }
            }
        }
    }
}

@Composable
fun SettingsRoot(
    container: AppContainer,
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = viewModel { SettingsViewModel(container) },
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
                    contentDescription = "Kembali",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Pengaturan",
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
            SectionLabel("Akun", Modifier.padding(top = 16.dp))
            Text(
                text = state.email ?: "Belum masuk",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Text(
                text = "ID",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SelectionContainer {
                Text(
                    text = state.logId ?: "Belum dibuat. Terbentuk setelah sinkronisasi pertama.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(Modifier.height(20.dp))
            Hairline()
            SectionLabel("Sinkronisasi", Modifier.padding(top = 16.dp))
            val lastSyncAt = state.sync.lastSyncAt
            val syncText = when {
                state.sync.running -> "Menyinkronkan"
                state.sync.lastError != null -> "Gagal: ${state.sync.lastError}"
                lastSyncAt != null -> "Terakhir ${formatDateTime(lastSyncAt)}"
                else -> "Belum pernah tersinkron"
            }
            StatusText(
                text = syncText,
                color = if (state.sync.lastError != null) MaterialTheme.colorScheme.error else status.muted,
                modifier = Modifier.padding(vertical = 6.dp),
            )

            TextButton(
                onClick = { onAction(SettingsAction.OnSignOutClick) },

                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White,
                    containerColor = Color.Red,
                ),
                shape = RoundedCornerShape(5.dp),
                enabled = !state.isBusy,
            ) { Text("Keluar") }
        }
    }

    if (state.showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { onAction(SettingsAction.OnSignOutDismiss) },
            title = { Text("Catatan belum terkirim") },
            text = {
                Text("Masih ada catatan yang gagal dikirim. Keluar sekarang akan menghapus catatan itu dari perangkat ini.")
            },
            confirmButton = {
                TextButton(onClick = { onAction(SettingsAction.OnSignOutDiscardConfirm) }) {
                    Text("Keluar dan hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(SettingsAction.OnSignOutDismiss) }) { Text("Batal") }
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
