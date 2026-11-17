package com.bpdlh.pencatatankeuangan.ui.detail

import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.ui.FakeEditor
import com.bpdlh.pencatatankeuangan.ui.MainDispatcherRule
import com.bpdlh.pencatatankeuangan.ui.testRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun missingRecordShowsError() = runTest(main.dispatcher) {
        val vm = DetailViewModel(FakeEditor(record = null), "rec-1")
        advanceUntilIdle()

        assertNull(vm.state.value.detail)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun loadsRecordFields() = runTest(main.dispatcher) {
        val vm = DetailViewModel(FakeEditor(record = testRecord()), "rec-1")
        advanceUntilIdle()

        val detail = vm.state.value.detail!!
        assertEquals("Kopi susu", detail.name)
        assertEquals(18_000L, detail.price)
        assertEquals(R.string.status_pending, detail.statusRes)
        assertEquals("Pixel 8", detail.deviceModel)
        assertNull(detail.latitude)
    }

    @Test
    fun photoDownloadFailureIsReported() = runTest(main.dispatcher) {
        val vm = DetailViewModel(FakeEditor(record = testRecord(photoId = "ph-1"), photoPath = null), "rec-1")
        advanceUntilIdle()

        assertTrue(vm.state.value.photoFailed)
        assertNull(vm.state.value.photoPath)
    }

    @Test
    fun photoDownloadSuccessSetsPath() = runTest(main.dispatcher) {
        val vm = DetailViewModel(FakeEditor(record = testRecord(photoId = "ph-1"), photoPath = "/p.jpg"), "rec-1")
        advanceUntilIdle()

        assertFalse(vm.state.value.photoFailed)
        assertEquals("/p.jpg", vm.state.value.photoPath)
    }

    @Test
    fun deleteConfirmMarksDeletedAndClosesDialog() = runTest(main.dispatcher) {
        val editor = FakeEditor(record = testRecord())
        val vm = DetailViewModel(editor, "rec-1")
        advanceUntilIdle()

        vm.onAction(DetailAction.OnDeleteClick)
        assertTrue(vm.state.value.showDeleteDialog)

        vm.onAction(DetailAction.OnDeleteConfirm)
        advanceUntilIdle()

        assertEquals("rec-1", editor.deletedId)
        assertTrue(vm.state.value.deleted)
        assertFalse(vm.state.value.showDeleteDialog)
    }

    @Test
    fun stateFollowsDatabaseUpdatesAfterEdit() = runTest(main.dispatcher) {
        val editor = FakeEditor(record = testRecord(name = "Kopi susu", price = 18_000))
        val vm = DetailViewModel(editor, "rec-1")
        advanceUntilIdle()

        assertEquals("Kopi susu", vm.state.value.detail?.name)
        assertEquals(18_000L, vm.state.value.detail?.price)

        editor.record = testRecord(name = "Kopi susu besar", price = 25_000, updatedAt = 1_794_900_000_000)
        advanceUntilIdle()

        assertEquals("Kopi susu besar", vm.state.value.detail?.name)
        assertEquals(25_000L, vm.state.value.detail?.price)
    }

    @Test
    fun tombstoneFromOtherDeviceMarksDeleted() = runTest(main.dispatcher) {
        val editor = FakeEditor(record = testRecord())
        val vm = DetailViewModel(editor, "rec-1")
        advanceUntilIdle()
        assertFalse(vm.state.value.deleted)

        editor.record = testRecord().copy(deletedAt = 1_794_900_000_000)
        advanceUntilIdle()

        assertTrue(vm.state.value.deleted)
    }

    @Test
    fun deleteDismissKeepsRecord() = runTest(main.dispatcher) {
        val editor = FakeEditor(record = testRecord())
        val vm = DetailViewModel(editor, "rec-1")
        advanceUntilIdle()

        vm.onAction(DetailAction.OnDeleteClick)
        vm.onAction(DetailAction.OnDeleteDismiss)

        assertFalse(vm.state.value.showDeleteDialog)
        assertNull(editor.deletedId)
        assertFalse(vm.state.value.deleted)
    }
}
