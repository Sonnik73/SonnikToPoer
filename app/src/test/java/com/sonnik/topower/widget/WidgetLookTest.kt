package com.sonnik.topower.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WidgetLookTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun defaultIsAlmostSolid() {
        assertEquals(WidgetLook.DEFAULT_OPACITY, WidgetLook.opacity(context))
        assertEquals(95 * 255 / 100, WidgetLook.alpha(context))
    }

    @Test fun opacityIsClampedToPercent() {
        WidgetLook.setOpacity(context, 140)
        assertEquals(100, WidgetLook.opacity(context))
        assertEquals(255, WidgetLook.alpha(context))
        WidgetLook.setOpacity(context, -10)
        assertEquals(0, WidgetLook.opacity(context))
        assertEquals(0, WidgetLook.alpha(context))
        WidgetLook.setOpacity(context, 40)
        assertEquals(102, WidgetLook.alpha(context))
    }
}
