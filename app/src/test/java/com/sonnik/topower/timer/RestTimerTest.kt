package com.sonnik.topower.timer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RestTimerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** Если приложение выгрузили во время отдыха, новый таймер продолжает тот же отсчёт. */
    @Test fun restSurvivesProcessRestart() {
        val timer = RestTimer(context)
        timer.start("press", "Жим", 90)
        val endAt = timer.state.value!!.endAt

        val restored = RestTimer(context).state.value!!
        assertEquals("press", restored.exerciseId)
        assertEquals("Жим", restored.exerciseName)
        assertEquals(endAt, restored.endAt)
        assertEquals(90, restored.totalSec)

        timer.stop()
        assertNull(RestTimer(context).state.value)
    }

    /** Давно закончившийся отдых после перезапуска не показывается. */
    @Test fun staleRestIsNotRestored() {
        RestTimer(context).start("press", "Жим", 1)
        context.getSharedPreferences("rest_timer", Context.MODE_PRIVATE).edit()
            .putLong("end_at", System.currentTimeMillis() - RestTimer.STALE_MS - 1_000).commit()
        assertNull(RestTimer(context).state.value)
    }

    @Test fun bootReschedulesOnlyRunningRest() {
        val timer = RestTimer(context)
        timer.start("press", "Жим", 60)
        timer.rescheduleAfterBoot()
        assertEquals("press", timer.state.value?.exerciseId)
        timer.rescheduleAfterBoot(now = System.currentTimeMillis() + 120_000)
        assertNull(timer.state.value)
    }
}
