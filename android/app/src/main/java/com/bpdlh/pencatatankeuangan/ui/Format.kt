package com.bpdlh.pencatatankeuangan.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val indonesia = Locale("in", "ID")
private val grouping = NumberFormat.getIntegerInstance(indonesia)
private val dateTimeFormat = SimpleDateFormat("d MMM yyyy, HH:mm", indonesia)
private val dateFormat = SimpleDateFormat("d MMM yyyy", indonesia)
private val clockFormat = SimpleDateFormat("HH:mm", indonesia)

/** Angka saja; simbol mata uang berasal dari resource `currency_amount`. */
fun formatNumber(value: Long): String = grouping.format(value)

fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

fun formatClock(millis: Long): String = clockFormat.format(Date(millis))

fun formatCoordinate(latitude: Double, longitude: Double): String =
    String.format(Locale.ROOT, "%.5f, %.5f", latitude, longitude)

fun priceDigits(raw: String): String = raw.filter { it.isDigit() }.take(12)

fun parsePrice(digits: String): Long? = digits.toLongOrNull()

private const val STALE_AFTER_MS = 24L * 60 * 60 * 1000

fun isStalePending(oldestPendingAt: Long?, now: Long): Boolean =
    oldestPendingAt != null && oldestPendingAt < now - STALE_AFTER_MS
