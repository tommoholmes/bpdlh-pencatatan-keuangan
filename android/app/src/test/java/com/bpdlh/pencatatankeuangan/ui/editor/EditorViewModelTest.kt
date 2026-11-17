package com.bpdlh.pencatatankeuangan.ui.editor

import androidx.lifecycle.SavedStateHandle
import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.ui.FakeEditor
import com.bpdlh.pencatatankeuangan.ui.FakeLocation
import com.bpdlh.pencatatankeuangan.ui.FakePhotos
import com.bpdlh.pencatatankeuangan.ui.FakeSettings
import com.bpdlh.pencatatankeuangan.ui.MainDispatcherRule
import com.bpdlh.pencatatankeuangan.ui.UiText
import com.bpdlh.pencatatankeuangan.ui.testRecord
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
class EditorViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private fun viewModel(
        editor: FakeEditor = FakeEditor(),
        location: FakeLocation = FakeLocation(),
        settings: FakeSettings = FakeSettings(),
        recordId: String? = null,
    ) = EditorViewModel(
        records = editor,
        photos = FakePhotos(),
        location = location,
        prefs = settings,
        recordId = recordId,
        savedStateHandle = SavedStateHandle(),
    )

    @Test
    fun newRecordFillsLocationWhenPermissionGranted() = runTest(main.dispatcher) {
        val location = FakeLocation()
        val vm = viewModel(location = location)
        advanceUntilIdle()

        assertEquals(LocationState.Filled, vm.state.value.locationState)
        assertEquals(-7.7956, vm.state.value.place.latitude!!, 0.0001)
        assertEquals(1, location.calls)
    }

    @Test
    fun locationStaysIdleWithoutPermission() = runTest(main.dispatcher) {
        val vm = viewModel(location = FakeLocation(granted = false))
        advanceUntilIdle()

        assertEquals(LocationState.Idle, vm.state.value.locationState)
    }

    @Test
    fun locationDisabledBySetting() = runTest(main.dispatcher) {
        val vm = viewModel(settings = FakeSettings(saveLocationEnabled = false))
        advanceUntilIdle()

        assertEquals(LocationState.Disabled, vm.state.value.locationState)
    }

    @Test
    fun existingRecordIsLoadedAndLocationIsNotRequested() = runTest(main.dispatcher) {
        val editor = FakeEditor(record = testRecord())
        val location = FakeLocation()
        val vm = viewModel(editor = editor, location = location, recordId = "rec-1")
        advanceUntilIdle()

        assertTrue(vm.state.value.existing)
        assertEquals("Kopi susu", vm.state.value.name)
        assertEquals("18000", vm.state.value.priceDigits)
        assertEquals(0, location.calls)
    }

    @Test
    fun emptyNameBlocksSave() = runTest(main.dispatcher) {
        val editor = FakeEditor()
        val vm = viewModel(editor = editor)
        advanceUntilIdle()

        vm.onAction(EditorAction.OnNameChange("   "))
        vm.onAction(EditorAction.OnPriceChange("18000"))
        vm.onAction(EditorAction.OnSaveClick)
        advanceUntilIdle()

        assertEquals(UiText.Res(R.string.editor_name_required), vm.state.value.nameError)
        assertEquals(0, editor.saveCalls)
        assertFalse(vm.state.value.saved)
    }

    @Test
    fun missingPriceBlocksSave() = runTest(main.dispatcher) {
        val editor = FakeEditor()
        val vm = viewModel(editor = editor)
        advanceUntilIdle()

        vm.onAction(EditorAction.OnNameChange("Kopi"))
        vm.onAction(EditorAction.OnSaveClick)
        advanceUntilIdle()

        assertEquals(UiText.Res(R.string.editor_price_required), vm.state.value.priceError)
        assertEquals(0, editor.saveCalls)
    }

    @Test
    fun validFormSavesTrimmedNameAndParsedPrice() = runTest(main.dispatcher) {
        val editor = FakeEditor()
        val vm = viewModel(editor = editor)
        advanceUntilIdle()

        vm.onAction(EditorAction.OnNameChange("  Kopi susu  "))
        vm.onAction(EditorAction.OnPriceChange("Rp 18.000"))
        vm.onAction(EditorAction.OnSaveClick)
        advanceUntilIdle()

        assertEquals(1, editor.saveCalls)
        assertEquals("Kopi susu", editor.savedName)
        assertEquals(18_000L, editor.savedPrice)
        assertNull(editor.savedId)
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun priceInputKeepsOnlyDigits() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAction(EditorAction.OnPriceChange("Rp 18.000,00"))

        assertEquals("1800000", vm.state.value.priceDigits)
    }

    @Test
    fun locationDeniedIsReflectedInState() = runTest(main.dispatcher) {
        val vm = viewModel(location = FakeLocation(granted = false))
        advanceUntilIdle()

        vm.onAction(EditorAction.OnLocationDenied)

        assertEquals(LocationState.Denied, vm.state.value.locationState)
    }
}
