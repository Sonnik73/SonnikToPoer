package com.sonnik.topower.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.ui.muscles.muscleAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class BodyMapTest {
    @Test fun tapFindsMuscle() {
        assertEquals(Muscle.CHEST, muscleAt(back = false, x = 43f, y = 42f))
        assertEquals(Muscle.CHEST, muscleAt(back = false, x = 57f, y = 42f))
        assertEquals(Muscle.ABS, muscleAt(back = false, x = 50f, y = 70f))
        assertEquals(Muscle.LOWER_BACK, muscleAt(back = true, x = 50f, y = 80f))
        assertEquals(Muscle.HAMSTRINGS, muscleAt(back = true, x = 42f, y = 134f))
    }

    @Test fun tapOutsideMuscles() {
        assertNull(muscleAt(back = false, x = 5f, y = 5f))
        assertNull(muscleAt(back = true, x = 50f, y = 13f))
    }
}
