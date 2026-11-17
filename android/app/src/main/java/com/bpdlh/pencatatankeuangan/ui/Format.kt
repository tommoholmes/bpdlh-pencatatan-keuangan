package com.bpdlh.pencatatankeuangan.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val indonesia = Locale("in", "ID")
private val rupiah = NumberFormat.getIntegerInstance(indonesia)
private val dateTimeFormat = SimpleDateFormat("d MMM yyyy, HH:mm", indonesia)
private val dateFormat = SimpleDateFormat("d MMM yyyy", indonesia)
private val clockFormat = SimpleDateFormat("HH:mm", indonesia)

fun formatRupiah(price: Long): String = "Rp ${rupiah.format(price)}"

fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

fun formatClock(millis: Long): String = clockFormat.format(Date(millis))

// Titik desimal wajib di sini: intent geo: hanya menerima format itu.
fun formatCoordinate(latitude: Double, longitude: Double): String =
    String.format(Locale.ROOT, "%.5f, %.5f", latitude, longitude)

fun formatAccuracy(meters: Double): String = "${Math.round(meters)} m"

/** Hanya digit, maksimal 12 angka supaya harga tetap masuk akal untuk rupiah. */
fun priceDigits(raw: String): String = raw.filter { it.isDigit() }.take(12)

fun parsePrice(digits: String): Long? = digits.toLongOrNull()

fun summaryLine(count: Int, total: Long): String = "$count catatan · ${formatRupiah(total)}"

private const val STALE_AFTER_MS = 24L * 60 * 60 * 1000

/** Peringatan hanya untuk catatan yang sudah lebih dari sehari belum terkirim. */
fun pendingStaleWarning(oldestPendingAt: Long?, now: Long): String? =
    if (oldestPendingAt != null && oldestPendingAt < now - STALE_AFTER_MS) {
        "Catatan ini belum terkirim lebih dari sehari. Selama belum tersinkron, datanya hanya ada di perangkat ini."
    } else {
        null
    }
