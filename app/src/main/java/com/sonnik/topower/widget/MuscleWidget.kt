package com.sonnik.topower.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.sonnik.topower.R
import com.sonnik.topower.SonnikApp
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.domain.MuscleState
import com.sonnik.topower.domain.MuscleStatus
import com.sonnik.topower.domain.state
import com.sonnik.topower.reminders.muscleStatuses
import com.sonnik.topower.timer.RestTimer
import com.sonnik.topower.ui.muscles.muscleColor
import com.sonnik.topower.ui.muscles.renderBodyBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Виджет «Состояние мышц»: схема тела в цветах восстановления и время до полного восстановления.
 * Обновляется при изменении тренировок и раз в полчаса (часов точности достаточно).
 */
class MuscleWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as SonnikApp
                update(context, muscleStatuses(app.repository.dao.muscleWork()))
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        private const val IMAGE_HEIGHT_PX = 360

        /** Перерисовывает все виджеты, если они есть на рабочем столе. */
        fun update(context: Context, statuses: Map<Muscle, MuscleStatus>, now: Long = System.currentTimeMillis()) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, MuscleWidget::class.java))
            if (ids.isEmpty()) return

            val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
            val idle = if (night) 0xFF4A515B.toInt() else 0xFFC3C9D1.toInt()
            val bodyColor = if (night) 0xFF2E333A.toInt() else 0xFFE4E7EB.toInt()
            val gapColor = context.getColor(R.color.widget_bg) or 0xFF000000.toInt()
            val bitmap = renderBodyBitmap(
                colorOf = { m -> statuses[m]?.let { muscleColor(it, now, androidx.compose.ui.graphics.Color(idle)).toArgb() } ?: idle },
                bodyColor = bodyColor,
                gapColor = gapColor,
                height = IMAGE_HEIGHT_PX,
            )

            val views = RemoteViews(context.packageName, R.layout.widget_muscles).apply {
                setImageViewBitmap(R.id.widget_body, bitmap)
                setTextViewText(R.id.widget_status, statusText(context, statuses, now))
                setOnClickPendingIntent(R.id.widget_root, RestTimer.openAppIntent(context))
            }
            manager.updateAppWidget(ids, views)
        }

        private fun statusText(context: Context, statuses: Map<Muscle, MuscleStatus>, now: Long): String {
            val recovering = statuses.values.filter { it.state(now) == MuscleState.RECOVERING }
            if (recovering.isNotEmpty()) {
                val left = recovering.maxOf { it.readyAt ?: now } - now
                val hours = ((left + 3_599_999) / 3_600_000).toInt()
                val time = if (left < 3_600_000) {
                    context.getString(R.string.widget_minutes, ((left + 59_999) / 60_000).toInt())
                } else {
                    context.getString(R.string.widget_hours, hours)
                }
                return context.getString(R.string.widget_full_in, time)
            }
            return if (statuses.values.any { it.lastTrained != null }) {
                context.getString(R.string.widget_all_ready)
            } else {
                context.getString(R.string.widget_no_data)
            }
        }
    }
}
