package com.bpdlh.pencatatankeuangan.data

import com.bpdlh.pencatatankeuangan.data.remote.ApiException
import java.io.IOException
import java.net.UnknownHostException

fun syncErrorMessage(error: Throwable): String = when {
    error is ApiException && error.status == 401 -> "Sesi berakhir. Masuk ulang dengan Google."
    error is ApiException && error.status == 413 -> "Foto lebih dari 5 MB. Ganti dengan foto yang lebih kecil."
    error is ApiException && error.status in 400..499 -> "Server menolak data (kode ${error.status})."
    error is ApiException -> "Server sedang bermasalah. Coba lagi nanti."
    error is UnknownHostException -> "Tidak ada koneksi internet."
    error is IOException -> "Koneksi terputus. Coba lagi nanti."
    else -> "Sinkronisasi gagal."
}
