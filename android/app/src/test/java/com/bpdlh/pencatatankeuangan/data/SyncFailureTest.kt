package com.bpdlh.pencatatankeuangan.data

import com.bpdlh.pencatatankeuangan.data.remote.ApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class SyncFailureTest {
    @Test
    fun mapsHttpStatuses() {
        assertEquals(SyncFailure.Unauthorized, syncFailure(ApiException(401, "")))
        assertEquals(SyncFailure.PhotoTooLarge, syncFailure(ApiException(413, "")))
        assertEquals(SyncFailure.ClientError(422), syncFailure(ApiException(422, "")))
        assertEquals(SyncFailure.ServerError, syncFailure(ApiException(500, "")))
    }

    @Test
    fun mapsNetworkErrors() {
        assertEquals(SyncFailure.Offline, syncFailure(UnknownHostException("no dns")))
        assertEquals(SyncFailure.Disconnected, syncFailure(IOException("reset")))
        assertEquals(SyncFailure.Unknown, syncFailure(IllegalStateException("boom")))
    }

    @Test
    fun encodeDecodeRoundTrips() {
        val all = listOf(
            SyncFailure.Unauthorized,
            SyncFailure.PhotoTooLarge,
            SyncFailure.ClientError(400),
            SyncFailure.ServerError,
            SyncFailure.Offline,
            SyncFailure.Disconnected,
            SyncFailure.Unknown,
        )
        for (failure in all) {
            assertEquals(failure, decodeSyncFailure(failure.encode()))
        }
        assertNull(decodeSyncFailure(null))
    }
}
