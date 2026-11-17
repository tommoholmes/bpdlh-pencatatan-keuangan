package com.bpdlh.pencatatankeuangan.data

import com.bpdlh.pencatatankeuangan.data.remote.ApiException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.UnknownHostException

class SyncErrorTest {
    @Test
    fun unauthorizedAsksToSignInAgain() {
        assertEquals("Sesi berakhir. Masuk ulang dengan Google.", syncErrorMessage(ApiException(401, "HTTP 401")))
    }

    @Test
    fun tooLargePhotoNamesTheLimit() {
        assertEquals("Foto lebih dari 5 MB. Ganti dengan foto yang lebih kecil.", syncErrorMessage(ApiException(413, "")))
    }

    @Test
    fun otherClientErrorsShowOnlyTheCode() {
        assertEquals("Server menolak data (kode 422).", syncErrorMessage(ApiException(422, "HTTP 422: bad")))
    }

    @Test
    fun serverErrorDoesNotLeakTheBody() {
        assertEquals("Server sedang bermasalah. Coba lagi nanti.", syncErrorMessage(ApiException(500, "stack trace")))
    }

    @Test
    fun offlineAndUnknownAreDistinct() {
        assertEquals("Tidak ada koneksi internet.", syncErrorMessage(UnknownHostException("no dns")))
        assertEquals("Sinkronisasi gagal.", syncErrorMessage(IllegalStateException("boom")))
    }
}
