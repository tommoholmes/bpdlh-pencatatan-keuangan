package com.bpdlh.pencatatankeuangan.ui.settings

import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.data.AuthException
import com.bpdlh.pencatatankeuangan.data.AuthFailure
import com.bpdlh.pencatatankeuangan.data.SignOutResult
import com.bpdlh.pencatatankeuangan.data.SyncFailure
import com.bpdlh.pencatatankeuangan.ui.FakeAuth
import com.bpdlh.pencatatankeuangan.ui.FakeFeed
import com.bpdlh.pencatatankeuangan.ui.FakeSession
import com.bpdlh.pencatatankeuangan.ui.FakeSettings
import com.bpdlh.pencatatankeuangan.ui.MainDispatcherRule
import com.bpdlh.pencatatankeuangan.ui.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private var crashReporting: Boolean? = null

    private fun viewModel(
        auth: FakeAuth = FakeAuth(),
        session: FakeSession = FakeSession(),
        settings: FakeSettings = FakeSettings(),
        feed: FakeFeed = FakeFeed(),
    ) = SettingsViewModel(
        auth = auth,
        records = feed,
        account = session,
        prefs = settings,
        setCrashReporting = { crashReporting = it },
    )

    @Test
    fun initialStateComesFromDependencies() = runTest(main.dispatcher) {
        val vm = viewModel(
            auth = FakeAuth(currentUserEmail = "budi@example.com"),
            settings = FakeSettings(logId = "log-9", crashReportsEnabled = false, saveLocationEnabled = false),
        )
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("budi@example.com", state.email)
        assertEquals("log-9", state.logId)
        assertFalse(state.crashReports)
        assertFalse(state.saveLocation)
    }

    @Test
    fun crashReportToggleCallsContainerAndUpdatesState() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(SettingsAction.OnCrashReportsChange(false))

        assertEquals(false, crashReporting)
        assertFalse(vm.state.value.crashReports)
    }

    @Test
    fun saveLocationTogglePersistsToPrefs() = runTest(main.dispatcher) {
        val settings = FakeSettings()
        val vm = viewModel(settings = settings)
        advanceUntilIdle()

        vm.onAction(SettingsAction.OnSaveLocationChange(false))

        assertFalse(settings.saveLocationEnabled)
        assertFalse(vm.state.value.saveLocation)
    }

    @Test
    fun signOutDoneMarksSignedOut() = runTest(main.dispatcher) {
        val session = FakeSession(signOutResult = SignOutResult.Done)
        val vm = viewModel(session = session)
        advanceUntilIdle()

        vm.onAction(SettingsAction.OnSignOutClick)
        advanceUntilIdle()

        assertEquals(1, session.signOutCalls)
        assertTrue(vm.state.value.signedOut)
        assertFalse(vm.state.value.isBusy)
        assertNull(vm.state.value.message)
    }

    @Test
    fun pendingUnsyncedOpensDialogWithMessage() = runTest(main.dispatcher) {
        val session = FakeSession(signOutResult = SignOutResult.PendingUnsynced(SyncFailure.Offline))
        val vm = viewModel(session = session)
        advanceUntilIdle()

        vm.onAction(SettingsAction.OnSignOutClick)
        advanceUntilIdle()

        assertTrue(vm.state.value.showSignOutDialog)
        assertEquals(
            UiText.Res(R.string.settings_signout_failed, listOf(UiText.Res(R.string.sync_error_offline))),
            vm.state.value.message,
        )
        assertFalse(vm.state.value.signedOut)
    }

    @Test
    fun discardPendingSignsOutWithoutSync() = runTest(main.dispatcher) {
        val session = FakeSession()
        val vm = viewModel(session = session)
        advanceUntilIdle()

        vm.onAction(SettingsAction.OnSignOutDiscardConfirm)
        advanceUntilIdle()

        assertEquals(1, session.discardCalls)
        assertEquals(0, session.signOutCalls)
        assertTrue(vm.state.value.signedOut)
    }

    @Test
    fun deleteAccountFailureShowsMessage() = runTest(main.dispatcher) {
        val session = FakeSession(deleteResult = Result.failure(AuthException(AuthFailure.DeleteFailed)))
        val vm = viewModel(session = session)
        advanceUntilIdle()

        vm.onAction(SettingsAction.OnDeleteAccountClick)
        assertTrue(vm.state.value.showDeleteAccountDialog)

        vm.onAction(SettingsAction.OnDeleteAccountConfirm)
        advanceUntilIdle()

        assertEquals(1, session.deleteCalls)
        assertEquals(UiText.Res(R.string.settings_delete_failed), vm.state.value.message)
        assertFalse(vm.state.value.signedOut)
    }

    @Test
    fun deleteAccountSuccessSignsOut() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(SettingsAction.OnDeleteAccountClick)
        vm.onAction(SettingsAction.OnDeleteAccountConfirm)
        advanceUntilIdle()

        assertTrue(vm.state.value.signedOut)
        assertFalse(vm.state.value.showDeleteAccountDialog)
    }
}
