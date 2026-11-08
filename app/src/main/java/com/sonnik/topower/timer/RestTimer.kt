package com.sonnik.topower.timer

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sonnik.topower.MainActivity
import com.sonnik.topower.R
import com.sonnik.topower.data.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Идущий отдых: до [endAt] (System.currentTimeMillis), всего [totalSec]. */
data class RestState(
    val exerciseId: String,
    val exerciseName: String,
    val endAt: Long,
    val totalSec: Int,
)

/**
 * Таймер отдыха между подходами. В приложении отсчёт показывает панель на экране тренировки,
 * при заблокированном экране — уведомление с обратным отсчётом; в конце будильник
 * присылает уведомление со звуком и вибрацией.
 */
class RestTimer(private val context: Context) {
    // Отдых запоминается: если система выгрузит приложение или телефон перезагрузится, отсчёт продолжится.
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<RestState?> = _state.asStateFlow()

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    init {
        createChannels(context)
    }

    fun start(exerciseId: String, exerciseName: String, seconds: Int) {
        val state = RestState(exerciseId, exerciseName, System.currentTimeMillis() + seconds * 1000L, seconds)
        set(state)
        schedule(state)
    }

    /** После перезагрузки будильники пропадают: ставим заново, если отдых ещё идёт. */
    fun rescheduleAfterBoot(now: Long = System.currentTimeMillis()) {
        val current = _state.value ?: return
        if (current.endAt > now) schedule(current) else stop()
    }

    private fun set(state: RestState?) {
        _state.value = state
        prefs.edit().apply {
            if (state == null) {
                clear()
            } else {
                putString(KEY_ID, state.exerciseId)
                putString(KEY_NAME, state.exerciseName)
                putLong(KEY_END, state.endAt)
                putInt(KEY_TOTAL, state.totalSec)
            }
        }.apply()
    }

    /** Сохранённый отдых, если он закончился не больше [STALE_MS] назад; более старый не показываем. */
    private fun load(now: Long = System.currentTimeMillis()): RestState? {
        val id = prefs.getString(KEY_ID, null) ?: return null
        val endAt = prefs.getLong(KEY_END, 0L)
        if (endAt + STALE_MS < now) return null
        return RestState(id, prefs.getString(KEY_NAME, null).orEmpty(), endAt, prefs.getInt(KEY_TOTAL, 0))
    }

    /** Меняет длительность текущего отдыха на [deltaSec]; возвращает новую полную длительность. */
    fun adjust(deltaSec: Int): Int? {
        val current = _state.value ?: return null
        val total = (current.totalSec + deltaSec).coerceIn(AppSettings.MIN_REST_SEC, AppSettings.MAX_REST_SEC)
        val endAt = current.endAt + (total - current.totalSec) * 1000L
        val updated = current.copy(endAt = endAt, totalSec = total)
        set(updated)
        if (endAt > System.currentTimeMillis()) schedule(updated) else stop()
        return total
    }

    /** Останавливает отсчёт и убирает уведомление. */
    fun stop() {
        set(null)
        alarmManager.cancel(alarmIntent())
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun schedule(state: RestState) {
        val pending = alarmIntent()
        alarmManager.cancel(pending)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.endAt, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.endAt, pending)
        }
        showCountdown(state)
    }

    @SuppressLint("MissingPermission") // разрешение проверяет canNotify
    private fun showCountdown(state: RestState) {
        if (!canNotify(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.rest_title))
            .setContentText(state.exerciseName)
            .setWhen(state.endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, RestTimerReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_RUNNING = "rest_running"
        const val CHANNEL_DONE = "rest_done"
        const val NOTIFICATION_ID = 1001
        private const val PREFS = "rest_timer"
        private const val KEY_ID = "exercise_id"
        private const val KEY_NAME = "exercise_name"
        private const val KEY_END = "end_at"
        private const val KEY_TOTAL = "total_sec"
        /** Столько панель «Отдых окончен» видна после конца отдыха (как в RestTimerBar). */
        const val STALE_MS = 60_000L

        fun canNotify(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun createChannels(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_RUNNING,
                    context.getString(R.string.rest_channel_running),
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_DONE,
                    context.getString(R.string.rest_channel_done),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                }
            )
        }
    }
}

/** Срабатывает по окончании отдыха: уведомление со звуком и вибрацией. */
class RestTimerReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission") // разрешение проверяет canNotify
    override fun onReceive(context: Context, intent: Intent) {
        if (!RestTimer.canNotify(context)) return
        val notification = NotificationCompat.Builder(context, RestTimer.CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.rest_done_title))
            .setContentText(context.getString(R.string.rest_done_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(RestTimer.openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(RestTimer.NOTIFICATION_ID, notification)
    }
}
