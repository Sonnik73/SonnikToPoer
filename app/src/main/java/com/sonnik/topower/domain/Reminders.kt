package com.sonnik.topower.domain

import java.time.Instant
import java.time.ZoneId

/** Когда сообщать о восстановлении. */
enum class RecoveryNotifyMode {
    /** Когда восстановились все мышцы последних тренировок. */
    ALL,

    /** Когда восстановились выбранные мышцы. */
    GROUPS,

    /** Раз в день в заданное время — список готовых мышц. */
    DAILY,
}

/** Настройки уведомлений. Время — минуты от полуночи. */
data class ReminderSettings(
    val recoveryOn: Boolean = false,
    val mode: RecoveryNotifyMode = RecoveryNotifyMode.ALL,
    val groups: Set<Muscle> = emptySet(),
    val dailyMinutes: Int = 9 * 60,
    val quietOn: Boolean = true,
    val quietStart: Int = 22 * 60,
    val quietEnd: Int = 8 * 60,
    val inactivityOn: Boolean = false,
    val inactivityDays: Int = 4,
)

enum class ReminderKind { RECOVERY, DAILY, INACTIVITY }

/** Запланированное уведомление: [key] — событие, о котором оно (момент восстановления, срок без тренировок). */
data class Reminder(val kind: ReminderKind, val at: Long, val key: Long)

private const val MINUTE_MS = 60_000L
private const val DAY_MS = 24 * 60 * MINUTE_MS

/** Пропущенное (телефон был выключен) уведомление о восстановлении ещё уместно столько времени. */
const val RECOVERY_GRACE_MS = 12 * 60 * MINUTE_MS

/** Если [time] попадает в тихие часы — конец тихих часов, иначе сам [time]. */
fun outOfQuietHours(time: Long, s: ReminderSettings, zone: ZoneId): Long {
    if (!s.quietOn || s.quietStart == s.quietEnd) return time
    val dt = Instant.ofEpochMilli(time).atZone(zone)
    val m = dt.hour * 60 + dt.minute
    val quiet = if (s.quietStart < s.quietEnd) m >= s.quietStart && m < s.quietEnd else m >= s.quietStart || m < s.quietEnd
    if (!quiet) return time
    var end = dt.toLocalDate().atTime(s.quietEnd / 60, s.quietEnd % 60).atZone(zone)
    if (!end.isAfter(dt)) end = end.plusDays(1)
    return end.toInstant().toEpochMilli()
}

/** Ближайшие [minutes] от полуночи после [now]. */
fun nextDailyTime(now: Long, minutes: Int, zone: ZoneId): Long {
    val dt = Instant.ofEpochMilli(now).atZone(zone)
    var at = dt.toLocalDate().atTime(minutes / 60, minutes % 60).atZone(zone)
    if (!at.isAfter(dt)) at = at.plusDays(1)
    return at.toInstant().toEpochMilli()
}

/** Момент восстановления, о котором сообщать: последнее восстановление нужных мышц или null. */
fun recoveryKey(s: ReminderSettings, statuses: Map<Muscle, MuscleStatus>): Long? {
    val muscles = if (s.mode == RecoveryNotifyMode.GROUPS) s.groups else Muscle.entries.toSet()
    return muscles.mapNotNull { statuses[it]?.readyAt }.maxOrNull()
}

/** Срок без тренировок, после которого напомнить, или null. */
fun inactivityKey(s: ReminderSettings, lastWorkout: Long?): Long? =
    lastWorkout?.let { it + s.inactivityDays * DAY_MS }

/**
 * Уведомления, которые нужно запланировать сейчас. [notifiedRecovery] и [notifiedInactivity] — ключи
 * уже показанных уведомлений, чтобы не повторяться.
 */
fun plannedReminders(
    s: ReminderSettings,
    statuses: Map<Muscle, MuscleStatus>,
    lastWorkout: Long?,
    now: Long,
    zone: ZoneId,
    notifiedRecovery: Long,
    notifiedInactivity: Long,
): List<Reminder> = buildList {
    if (s.recoveryOn) {
        if (s.mode == RecoveryNotifyMode.DAILY) {
            val at = nextDailyTime(now, s.dailyMinutes, zone)
            add(Reminder(ReminderKind.DAILY, at, at))
        } else {
            val key = recoveryKey(s, statuses)
            if (key != null && key > notifiedRecovery && now - key < RECOVERY_GRACE_MS) {
                add(Reminder(ReminderKind.RECOVERY, outOfQuietHours(maxOf(key, now), s, zone), key))
            }
        }
    }
    if (s.inactivityOn) {
        val key = inactivityKey(s, lastWorkout)
        if (key != null && key > notifiedInactivity) {
            add(Reminder(ReminderKind.INACTIVITY, outOfQuietHours(maxOf(key, now), s, zone), key))
        }
    }
}

/** Мышцы, готовые к тренировке: тренировались недавно и уже восстановились. */
fun readyMuscles(statuses: Map<Muscle, MuscleStatus>, now: Long): List<Muscle> =
    statuses.values.filter { it.state(now) == MuscleState.READY }.sortedBy { it.lastTrained }.map { it.muscle }
