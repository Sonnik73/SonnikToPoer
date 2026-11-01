package com.sonnik.topower.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatesTest {
    @Test fun parsesVersions() {
        assertEquals(listOf(1, 27), Updates.parts("v1.27"))
        assertEquals(listOf(1, 2, 3), Updates.parts("1.2.3-beta"))
        assertEquals(emptyList<Int>(), Updates.parts("latest"))
    }

    @Test fun comparesNumbersNotStrings() {
        assertTrue(Updates.isNewer("v1.10", "1.9"))
        assertTrue(Updates.isNewer("v1.28", "1.27"))
        assertFalse(Updates.isNewer("v1.27", "1.27"))
        assertFalse(Updates.isNewer("v1.26", "1.27"))
        assertTrue(Updates.isNewer("2.0", "1.99"))
        assertFalse(Updates.isNewer("v1.27", "1.27.0"))
        assertFalse(Updates.isNewer("nightly", "1.27"))
    }

    @Test fun offersOnlyNewerAndNotSkipped() {
        val r = ReleaseInfo("v1.28", "page", "apk", "")
        assertTrue(Updates.shouldOffer(r, "1.27", null))
        assertFalse(Updates.shouldOffer(r, "1.27", "1.28"))
        assertTrue(Updates.shouldOffer(r, "1.27", "v1.27"))
        assertFalse(Updates.shouldOffer(r, "1.28", null))
        assertFalse(Updates.shouldOffer(null, "1.27", null))
    }

    @Test fun checksAtMostEverySixHours() {
        val h = 3_600_000L
        assertTrue(Updates.isDue(0, 10 * h))
        assertFalse(Updates.isDue(10 * h, 15 * h))
        assertTrue(Updates.isDue(10 * h, 16 * h))
        assertTrue(Updates.isDue(10 * h, 5 * h)) // часы перевели назад
    }

    @Test fun notesWithoutVersionHeading() {
        assertEquals("- Один\n- Два", Updates.cleanNotes("## 1.28\n- Один\n- Два\n"))
        assertEquals("7.2", Updates.megabytes(7_586_445))
    }
}
