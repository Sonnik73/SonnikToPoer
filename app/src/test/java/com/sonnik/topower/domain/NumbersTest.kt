package com.sonnik.topower.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumbersTest {
    @Test fun validNumbers() {
        assertTrue(isValidNumber(""))
        assertTrue(isValidNumber("12,5"))
        assertTrue(isValidNumber("12.5"))
        assertTrue(isValidNumber("0"))
        assertTrue(isValidNumber("-5", allowNegative = true))
        assertTrue(isValidNumber("12", decimal = false))
    }

    @Test fun invalidNumbersAreNotSilentlyDropped() {
        assertFalse(isValidNumber("1.2.3"))
        assertFalse(isValidNumber("-"))
        assertFalse(isValidNumber("-5"))
        assertFalse(isValidNumber("12.5", decimal = false))
        assertFalse(isValidNumber("99999999999", decimal = false)) // больше Int
    }

    @Test fun bigNumbersAreGroupedByThousands() {
        assertEquals("41\u202F780", formatBig(41780.0))
        assertEquals("1\u202F048\u202F576", formatBig(1048576.0))
        assertEquals("60", formatBig(60.0))
        assertEquals("12.5", formatBig(12.5, 1))
        assertEquals("—", formatBig(Double.NaN))
    }
}
