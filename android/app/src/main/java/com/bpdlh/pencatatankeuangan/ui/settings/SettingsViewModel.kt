package com.bpdlh.pencatatankeuangan.ui.settings

import androidx.lifecycle.ViewModel
import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.ui.UiText
import androidx.lifecycle.viewModelScope
import com.bpdlh.pencatatankeuangan.data.AuthException
import com.bpdlh.pencatatankeuangan.data.AuthGateway
import com.bpdlh.pencatatankeuangan.data.RecordFeed
import com.bpdlh.pencatatankeuangan.data.SessionController
import com.bpdlh.pencatatankeuangan.data.SettingsStore

import com.bpdlh.pencatatankeuangan.data.SignOutResult
import com.bpdlh.pencatatankeuangan.data.SyncStatus
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
    val message: UiText? = null,
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

class SettingsViewModel(
    private val auth: AuthGateway,
    private val records: RecordFeed,
    private val account: SessionController,
    private val prefs: SettingsStore,
    private val setCrashReporting: (Boolean) -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(
        SettingsState(
            email = auth.currentUserEmail,
            logId = prefs.logId,
            crashReports = prefs.crashReportsEnabled,
            saveLocation = prefs.saveLocationEnabled,
            sync = records.syncStatus.value,
        ),
    )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            records.syncStatus.collect { status ->
                _state.update { it.copy(sync = status, logId = prefs.logId) }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnCrashReportsChange -> {
                setCrashReporting(action.enabled)
                _state.update { it.copy(crashReports = action.enabled) }
            }
            is SettingsAction.OnSaveLocationChange -> {
                prefs.saveLocationEnabled = action.enabled
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
            when (val result = account.signOut()) {
                is SignOutResult.Done -> _state.update { it.copy(isBusy = false, signedOut = true) }
                is SignOutResult.PendingUnsynced -> _state.update {
                    it.copy(
                        isBusy = false,
                        showSignOutDialog = true,
                        message = UiText.Res(R.string.settings_signout_failed, listOf(result.failure.uiText)),
                    )
                }
            }
        }
    }

    private fun signOutDiscardingPending() {
        _state.update { it.copy(showSignOutDialog = false, isBusy = true) }
        viewModelScope.launch {
            account.signOutDiscardingPending()
            _state.update { it.copy(isBusy = false, signedOut = true) }
        }
    }

    private fun deleteAccount() {
        _state.update { it.copy(showDeleteAccountDialog = false, isBusy = true) }
        viewModelScope.launch {
            val result = account.deleteAccount()
            _state.update {
                if (result.isFailure) {
                    it.copy(
                        isBusy = false,
                        message = (result.exceptionOrNull() as? AuthException)?.failure?.uiText
                            ?: UiText.Res(R.string.settings_delete_failed),
                    )
                } else {
                    it.copy(isBusy = false, signedOut = true)
                }
            }
        }
    }
}
