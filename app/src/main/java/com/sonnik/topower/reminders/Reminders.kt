package com.sonnik.topower.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sonnik.topower.R
import com.sonnik.topower.SonnikApp
import com.sonnik.topower.data.AppSettings
import com.sonnik.topower.data.MuscleWorkRow
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.domain.MuscleStatus
import com.sonnik.topower.domain.MuscleWork
import com.sonnik.topower.domain.RecoveryNotifyMode
import com.sonnik.topower.domain.ReminderKind
import com.sonnik.topower.domain.computeMuscleStatus
import com.sonnik.topower.domain.inactivityKey
import com.sonnik.topower.domain.plannedReminders
import com.sonnik.topower.domain.readyMuscles
import com.sonnik.topower.domain.recoveryKey
import com.sonnik.topower.timer.RestTimer
import com.sonnik.topower.ui.muscles.muscleNameRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId

fun muscleStatuses(rows: List<MuscleWorkRow>): Map<Muscle, MuscleStatus> =
    computeMuscleStatus(rows.map { MuscleWork(it.workoutId, it.time, Muscle.parse(it.muscles), it.recoveryHours) })

/**
 * Уведомления о восстановлении мышц и о долгом перерыве. Каждый вид — свой будильник
 * (неточный: минута-другая роли не играют). При срабатывании событие перепроверяется по базе.
 */
class Reminders(private val context: Context, private val settings: AppSettings) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    init {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    /** Перепланирует все уведомления по текущим настройкам и данным. */
    fun reschedule(statuses: Map<Muscle, MuscleStatus>, lastWorkout: Long?, now: Long = System.currentTimeMillis()) {
        val planned = plannedReminders(
            settings.reminders.value, statuses, lastWorkout, now, ZoneId.systemDefault(),
            settings.notifiedRecovery, settings.notifiedInactivity,
        ).associateBy { it.kind }
        ReminderKind.entries.forEach { kind ->
            val pending = pendingIntent(kind, planned[kind]?.key)
            alarmManager.cancel(pending)
            planned[kind]?.let { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.at, pending) }
        }
    }

    /** Срабатывание будильника [kind] о событии [key]: показывает уведомление, если оно ещё актуально. */
    fun fire(kind: ReminderKind, key: Long, statuses: Map<Muscle, MuscleStatus>, lastWorkout: Long?, now: Long) {
        val s = settings.reminders.value
        when (kind) {
            ReminderKind.RECOVERY -> {
                if (!s.recoveryOn || s.mode == RecoveryNotifyMode.DAILY) return
                if (recoveryKey(s, statuses) != key || now < key || key <= settings.notifiedRecovery) return
                settings.notifiedRecovery = key
                val title = if (s.mode == RecoveryNotifyMode.GROUPS) {
                    context.getString(R.string.reminder_groups_title, names(s.groups.sortedBy { it.ordinal }))
                } else {
                    context.getString(R.string.reminder_all_title)
                }
                notify(kind, title, context.getString(R.string.reminder_all_text))
            }
            ReminderKind.DAILY -> {
                if (!s.recoveryOn || s.mode != RecoveryNotifyMode.DAILY) return
                val ready = readyMuscles(statuses, now)
                if (ready.isNotEmpty()) {
                    notify(kind, context.getString(R.string.reminder_daily_title), names(ready))
                }
            }
            ReminderKind.INACTIVITY -> {
                if (!s.inactivityOn || inactivityKey(s, lastWorkout) != key || now < key) return
                if (key <= settings.notifiedInactivity) return
                settings.notifiedInactivity = key
                notify(
                    kind,
                    context.getString(R.string.reminder_idle_title),
                    context.resources.getQuantityString(R.plurals.reminder_idle_text, s.inactivityDays, s.inactivityDays),
                )
            }
        }
    }

    private fun names(muscles: List<Muscle>) = muscles.joinToString(", ") { context.getString(muscleNameRes(it)) }

    @SuppressLint("MissingPermission") // разрешение проверяет canNotify
    private fun notify(kind: ReminderKind, title: String, text: String) {
        if (!RestTimer.canNotify(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(RestTimer.openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_BASE + kind.ordinal, notification)
    }

    private fun pendingIntent(kind: ReminderKind, key: Long?): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_BASE + kind.ordinal,
        Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_KIND, kind.name)
            .putExtra(EXTRA_KEY, key ?: 0L),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL = "reminders"
        const val EXTRA_KIND = "kind"
        const val EXTRA_KEY = "key"
        private const val REQUEST_BASE = 100
        private const val NOTIFICATION_BASE = 2000
    }
}

/** Будильник уведомления: проверяет событие по базе, показывает уведомление и планирует следующее. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as SonnikApp
        val kind = ReminderKind.entries.firstOrNull { it.name == intent.getStringExtra(Reminders.EXTRA_KIND) }
        val key = intent.getLongExtra(Reminders.EXTRA_KEY, 0L)
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val statuses = muscleStatuses(app.repository.dao.muscleWork())
                val last = app.repository.dao.lastWorkoutStart()
                val now = System.currentTimeMillis()
                kind?.let { app.reminders.fire(it, key, statuses, last, now) }
                app.reminders.reschedule(statuses, last, now)
            } finally {
                result.finish()
            }
        }
    }
}

/** После перезагрузки телефона будильники пропадают — планируем заново. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as SonnikApp
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.reminders.reschedule(muscleStatuses(app.repository.dao.muscleWork()), app.repository.dao.lastWorkoutStart())
            } finally {
                result.finish()
            }
        }
    }
}
