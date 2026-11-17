package com.bpdlh.pencatatankeuangan.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {
    @Test
    fun rupiahUsesIndonesianGrouping() {
        assertEquals("Rp 0", formatRupiah(0))
        assertEquals("Rp 18.000", formatRupiah(18_000))
        assertEquals("Rp 1.250.000", formatRupiah(1_250_000))
    }

    @Test
    fun priceInputKeepsOnlyDigitsAndCapsLength() {
        assertEquals("18000", priceDigits("Rp 18.000"))
        assertEquals("18000", priceDigits("18,000"))
        assertEquals("123456789012", priceDigits("1234567890123456"))
    }

    @Test
    fun parsePriceRejectsEmptyAndNonDigits() {
        assertEquals(18_000L, parsePrice("18000"))
        assertNull(parsePrice(""))
        assertNull(parsePrice("abc"))
    }

    @Test
    fun coordinateHasFiveDecimals() {
        assertEquals("-7.79560, 110.36950", formatCoordinate(-7.7956, 110.3695))
    }

    @Test
    fun accuracyIsRoundedMeters() {
        assertEquals("13 m", formatAccuracy(12.5))
        assertEquals("13 m", formatAccuracy(12.6))
    }

    @Test
    fun staleWarningOnlyAfterADay() {
        val now = 10_000_000_000L
        assertNull(pendingStaleWarning(null, now))
        assertNull(pendingStaleWarning(now - 60_000, now))
        assertEquals(
            "Catatan ini belum terkirim lebih dari sehari. Selama belum tersinkron, datanya hanya ada di perangkat ini.",
            pendingStaleWarning(now - 25 * 60 * 60 * 1000, now),
        )
    }

    @Test
    fun summaryShowsCountAndTotal() {
        assertEquals("2 catatan · Rp 21.000", summaryLine(2, 21_000))
    }
}
