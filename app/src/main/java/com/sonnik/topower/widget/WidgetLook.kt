package com.sonnik.topower.widget

import android.content.Context

/** Вид виджета на этом устройстве: непрозрачность фона в процентах (100 — сплошной, 0 — без фона). */
object WidgetLook {
    private const val PREFS = "widgets"
    private const val KEY_OPACITY = "opacity"
    const val DEFAULT_OPACITY = 95
    const val STEP = 5

    fun opacity(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_OPACITY, DEFAULT_OPACITY).coerceIn(0, 100)

    fun setOpacity(context: Context, percent: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_OPACITY, percent.coerceIn(0, 100)).apply()
    }

    /** 0–255 для ImageView.setImageAlpha. */
    fun alpha(context: Context): Int = opacity(context) * 255 / 100
}
