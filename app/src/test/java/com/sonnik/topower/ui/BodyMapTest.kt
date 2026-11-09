package com.sonnik.topower.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.data.MeasurementPart
import com.sonnik.topower.ui.muscles.BodyShape
import com.sonnik.topower.ui.muscles.measurementLine
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
        // Силуэт залит и сзади: голень без мышц — цвет тела (с лёгким градиентом света), а не пустота.
        fun isBody(color: Int) = color ushr 24 == 0xFF &&
            listOf(16, 8, 0).all { shift -> kotlin.math.abs((color shr shift and 0xFF) - (0xFFE4E7EB.toInt() shr shift and 0xFF)) <= 24 }
        assertTrue(isBody(bitmap.getPixel(((112 + 40) * s).toInt(), (185 * s).toInt())))
        assertTrue(isBody(bitmap.getPixel((40 * s).toInt(), (185 * s).toInt())))
        File("build/screenshots").apply { mkdirs() }.resolve("14-widget-body.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun femaleFigureHasSameMuscles() {
        assertEquals(Muscle.CHEST, muscleAt(back = false, x = 43f, y = 44f, female = true))
        assertEquals(Muscle.ABS, muscleAt(back = false, x = 47.5f, y = 65f, female = true))
        assertEquals(Muscle.GLUTES, muscleAt(back = true, x = 40f, y = 102f, female = true))
        val bitmap = renderBodyBitmap(
            colorOf = { if (it == Muscle.GLUTES) 0xFFE53935.toInt() else 0xFFC3C9D1.toInt() },
            bodyColor = 0xFFE4E7EB.toInt(),
            gapColor = 0xFFFFFFFF.toInt(),
            height = 360,
            female = true,
        )
        File("build/screenshots").apply { mkdirs() }.resolve("35-widget-body-female.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    /** Обхваты меняют ширину своего места: рука толще, талия уже, середина тела на месте. */
    @Test fun shapeChangesGirths() {
        val shape = BodyShape(biceps = 1.2f, waist = 0.9f, thigh = 1.1f)
        fun width(part: MeasurementPart, s: BodyShape?, female: Boolean = false) =
            measurementLine(part, female, s).let { (a, b) -> b.x - a.x }
        assertTrue(width(MeasurementPart.BICEPS, shape) > width(MeasurementPart.BICEPS, null) * 1.15f)
        assertTrue(width(MeasurementPart.WAIST, shape) < width(MeasurementPart.WAIST, null) * 0.95f)
        assertTrue(width(MeasurementPart.THIGH, shape) > width(MeasurementPart.THIGH, null) * 1.05f)
        assertEquals(width(MeasurementPart.CHEST, null), width(MeasurementPart.CHEST, shape), 1e-3f)
        assertTrue(width(MeasurementPart.BICEPS, shape, female = true) > width(MeasurementPart.BICEPS, null, female = true) * 1.15f)
        // Талия сужается симметрично относительно середины тела.
        val (a, c) = measurementLine(MeasurementPart.WAIST, false, shape)
        assertEquals(50f, (a.x + c.x) / 2, 1e-3f)
    }

    /** Бедро выросло без ягодиц, бицепс — без груди: нога не заходит за середину тела, рука — на корпус. */
    @Test fun thickLimbsDoNotOverlapBody() {
        val shape = BodyShape(biceps = 1.5f, thigh = 1.5f)
        for (female in listOf(false, true)) {
            val (_, thighInner) = measurementLine(MeasurementPart.THIGH, female, shape)
            assertTrue("thigh $female: ${thighInner.x}", thighInner.x <= 50f)
            val (_, armInner) = measurementLine(MeasurementPart.BICEPS, female, shape)
            val (_, armInnerBefore) = measurementLine(MeasurementPart.BICEPS, female, null)
            assertTrue("arm $female: ${armInner.x}", armInner.x <= armInnerBefore.x + 0.01f)
        }
    }

    @Test fun tapOutsideMuscles() {
        assertNull(muscleAt(back = false, x = 5f, y = 5f))
        assertNull(muscleAt(back = true, x = 50f, y = 13f))
    }
}
