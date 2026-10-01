package com.sonnik.topower.ui.muscles

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleColorTest {

    @Test
    fun gradientEndsAndIsContinuous() {
        assertEquals(Color(0xFFE53935), recoveryColor(0f))
        assertEquals(Color(0xFF43A047), recoveryColor(1f))
        // Соседние доли дают близкие цвета — без скачков на границах отрезков.
        var prev = recoveryColor(0f)
        for (i in 1..100) {
            val c = recoveryColor(i / 100f)
            val diff = maxOf(kotlin.math.abs(c.red - prev.red), kotlin.math.abs(c.green - prev.green), kotlin.math.abs(c.blue - prev.blue))
            assertTrue("jump at ${i / 100f}: $diff", diff < 0.05f)
            prev = c
        }
    }

    @Test
    fun countdownFormat() {
        assertEquals("0:00:00", formatCountdown(0))
        assertEquals("0:00:01", formatCountdown(1))
        assertEquals("71:59:12", formatCountdown((71 * 3600 + 59 * 60 + 12) * 1000L))
    }
}
