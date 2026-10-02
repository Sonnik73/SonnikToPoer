package com.sonnik.topower.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.ui.muscles.muscleAt
import com.sonnik.topower.ui.muscles.renderBodyBitmap
import android.graphics.Bitmap
import java.io.File
import org.junit.Assert.assertTrue
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
        assertEquals(Muscle.ABS, muscleAt(back = false, x = 47f, y = 65f))
        assertEquals(Muscle.QUADS, muscleAt(back = false, x = 37f, y = 125f))
        assertEquals(Muscle.LOWER_BACK, muscleAt(back = true, x = 48f, y = 80f))
        assertEquals(Muscle.HAMSTRINGS, muscleAt(back = true, x = 38f, y = 134f))
        assertEquals(Muscle.CALVES, muscleAt(back = true, x = 39f, y = 164f))
    }

    @Test fun widgetBitmapHasBothFigures() {
        val bitmap = renderBodyBitmap(
            colorOf = { if (it == Muscle.CHEST || it == Muscle.LATS) 0xFFE53935.toInt() else 0xFFC3C9D1.toInt() },
            bodyColor = 0xFFE4E7EB.toInt(),
            gapColor = 0xFFFFFFFF.toInt(),
            height = 360,
        )
        assertEquals(360, bitmap.height)
        assertTrue(bitmap.width > 360)
        // Грудь спереди (x≈43, y≈42) и широчайшие сзади (x≈40, y≈55 правее на ширину фигуры) — красные.
        val s = 360 / 200f
        fun isRed(color: Int) = (color shr 16 and 0xFF) > 170 && (color shr 8 and 0xFF) < 110
        assertTrue(isRed(bitmap.getPixel((43 * s).toInt(), (42 * s).toInt())))
        assertTrue(isRed(bitmap.getPixel(((112 + 40) * s).toInt(), (55 * s).toInt())))
        File("build/screenshots").apply { mkdirs() }.resolve("14-widget-body.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun tapOutsideMuscles() {
        assertNull(muscleAt(back = false, x = 5f, y = 5f))
        assertNull(muscleAt(back = true, x = 50f, y = 13f))
    }
}
