package com.bpdlh.pencatatankeuangan.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {
    @Test
    fun numberUsesIndonesianGrouping() {
        assertEquals("0", formatNumber(0))
        assertEquals("18.000", formatNumber(18_000))
        assertEquals("1.250.000", formatNumber(1_250_000))
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
    fun staleWarningOnlyAfterADay() {
        val now = 10_000_000_000L
        assertFalse(isStalePending(null, now))
        assertFalse(isStalePending(now - 60_000, now))
        assertTrue(isStalePending(now - 25 * 60 * 60 * 1000, now))
    }
}
