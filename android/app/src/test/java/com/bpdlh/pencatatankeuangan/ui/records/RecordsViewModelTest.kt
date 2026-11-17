package com.bpdlh.pencatatankeuangan.ui.records

import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.data.SYNC_FAILED
import com.bpdlh.pencatatankeuangan.data.SYNC_SYNCED
import com.bpdlh.pencatatankeuangan.ui.FakeConnectivity
import com.bpdlh.pencatatankeuangan.ui.FakeFeed
import com.bpdlh.pencatatankeuangan.ui.MainDispatcherRule
import com.bpdlh.pencatatankeuangan.ui.testRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun mapsRecordsAndTotal() = runTest(main.dispatcher) {
        val feed = FakeFeed(listOf(testRecord(), testRecord(id = "rec-2", name = "Bensin", price = 45_000)))
        val viewModel = RecordsViewModel(feed, FakeConnectivity())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.records.size)
        assertEquals("Kopi susu", state.records.first().name)
        assertEquals(18_000L, state.records.first().price)
        assertEquals(63_000L, state.total)
        assertFalse(state.isLoading)
        job.cancel()
    }

    @Test
    fun pendingRowCarriesStatusResource() = runTest(main.dispatcher) {
        val feed = FakeFeed(listOf(testRecord(syncStatus = SYNC_FAILED)))
        val viewModel = RecordsViewModel(feed, FakeConnectivity())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        assertEquals(R.string.status_failed_short, viewModel.state.value.records.single().statusRes)
        assertTrue(viewModel.state.value.records.single().failed)
        job.cancel()
    }

    @Test
    fun syncedRowHasNoStatusResource() = runTest(main.dispatcher) {
        val feed = FakeFeed(listOf(testRecord(syncStatus = SYNC_SYNCED)))
        val viewModel = RecordsViewModel(feed, FakeConnectivity())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        assertNull(viewModel.state.value.records.single().statusRes)
        job.cancel()
    }

    @Test
    fun oldPendingRowRaisesStaleWarning() = runTest(main.dispatcher) {
        val dayAgo = System.currentTimeMillis() - 25 * 60 * 60 * 1000
        val feed = FakeFeed(listOf(testRecord(updatedAt = dayAgo)))
        val viewModel = RecordsViewModel(feed, FakeConnectivity())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.state.value.showStaleWarning)
        job.cancel()
    }

    @Test
    fun pendingCountAndOnlineAreForwarded() = runTest(main.dispatcher) {
        val feed = FakeFeed(listOf(testRecord()), pending = 3)
        val viewModel = RecordsViewModel(feed, FakeConnectivity(onlineNow = false))
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        assertEquals(3, viewModel.state.value.pendingCount)
        assertFalse(viewModel.state.value.online)
        job.cancel()
    }

    @Test
    fun emptyListHasNoTotal() = runTest(main.dispatcher) {
        val viewModel = RecordsViewModel(FakeFeed(), FakeConnectivity())
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        assertEquals(0L, viewModel.state.value.total)
        assertTrue(viewModel.state.value.records.isEmpty())
        job.cancel()
    }
}
