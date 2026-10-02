package com.sonnik.topower.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class RemindersTest {
    private val zone = ZoneOffset.UTC
    private fun at(day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 10, day, hour, minute).toInstant(zone).toEpochMilli()
    private val h = 3_600_000L

    @Test fun quietHoursAcrossMidnight() {
        val s = ReminderSettings(quietStart = 22 * 60, quietEnd = 8 * 60)
        assertEquals(at(3, 8), outOfQuietHours(at(2, 23), s, zone))
        assertEquals(at(3, 8), outOfQuietHours(at(3, 3), s, zone))
        assertEquals(at(3, 12), outOfQuietHours(at(3, 12), s, zone))
        assertEquals(at(3, 3), outOfQuietHours(at(3, 3), s.copy(quietOn = false), zone))
    }

    @Test fun quietHoursSameDay() {
        val s = ReminderSettings(quietStart = 13 * 60, quietEnd = 15 * 60)
        assertEquals(at(3, 15), outOfQuietHours(at(3, 14), s, zone))
        assertEquals(at(3, 16), outOfQuietHours(at(3, 16), s, zone))
    }

    @Test fun dailyTime() {
        assertEquals(at(3, 9), nextDailyTime(at(3, 8), 9 * 60, zone))
        assertEquals(at(4, 9), nextDailyTime(at(3, 9), 9 * 60, zone))
    }

    private val statuses = mapOf(
        Muscle.CHEST to MuscleStatus(Muscle.CHEST, at(1, 18), at(4, 2)),
        Muscle.QUADS to MuscleStatus(Muscle.QUADS, at(1, 18), at(3, 18)),
    )

    @Test fun recoveryWaitsForLastMuscleAndQuietHours() {
        val s = ReminderSettings(recoveryOn = true)
        val r = plannedReminders(s, statuses, at(1, 18), at(2, 10), zone, 0, 0).single()
        assertEquals(ReminderKind.RECOVERY, r.kind)
        assertEquals(at(4, 2), r.key)
        assertEquals(at(4, 8), r.at) // 02:00 — тихие часы, переносим на 08:00
    }

    @Test fun recoveryOfSelectedGroups() {
        val s = ReminderSettings(recoveryOn = true, mode = RecoveryNotifyMode.GROUPS, groups = setOf(Muscle.QUADS))
        val r = plannedReminders(s, statuses, null, at(2, 10), zone, 0, 0).single()
        assertEquals(at(3, 18), r.at)
    }

    @Test fun recoveryNotRepeatedAndNotStale() {
        val s = ReminderSettings(recoveryOn = true)
        assertTrue(plannedReminders(s, statuses, null, at(2, 10), zone, at(4, 2), 0).isEmpty())
        // Восстановились давно — не напоминаем задним числом.
        assertTrue(plannedReminders(s, statuses, null, at(4, 2) + 13 * h, zone, 0, 0).isEmpty())
        // Пропущено недавно (в тихие часы) — сразу после них.
        val late = plannedReminders(s, statuses, null, at(4, 3), zone, 0, 0).single()
        assertEquals(at(4, 8), late.at)
    }

    @Test fun inactivityReminder() {
        val s = ReminderSettings(inactivityOn = true, inactivityDays = 3)
        val r = plannedReminders(s, emptyMap(), at(1, 12), at(2, 10), zone, 0, 0).single()
        assertEquals(ReminderKind.INACTIVITY, r.kind)
        assertEquals(at(4, 12), r.at)
        assertTrue(plannedReminders(s, emptyMap(), at(1, 12), at(2, 10), zone, 0, r.key).isEmpty())
    }

    @Test fun dailyMode() {
        val s = ReminderSettings(recoveryOn = true, mode = RecoveryNotifyMode.DAILY, dailyMinutes = 9 * 60)
        val r = plannedReminders(s, statuses, null, at(2, 10), zone, 0, 0).single()
        assertEquals(ReminderKind.DAILY, r.kind)
        assertEquals(at(3, 9), r.at)
    }

    @Test fun volumeCountsLastWeek() {
        val now = at(10, 12)
        val rows = listOf(
            MuscleSets(at(9, 12), setOf(Muscle.CHEST, Muscle.TRICEPS), 4),
            MuscleSets(at(5, 12), setOf(Muscle.CHEST), 3),
            MuscleSets(at(2, 12), setOf(Muscle.CHEST), 10), // старше недели
        )
        val v = weeklySets(rows, now)
        assertEquals(7, v[Muscle.CHEST])
        assertEquals(4, v[Muscle.TRICEPS])
        assertEquals(0, v[Muscle.LATS])
        assertEquals(VolumeLevel.MODERATE, volumeLevel(7))
        assertEquals(VolumeLevel.OPTIMAL, volumeLevel(12))
        assertEquals(VolumeLevel.HIGH, volumeLevel(21))
    }

    @Test fun templateBlockingMuscles() {
        val b = blockingMuscles(setOf(Muscle.CHEST, Muscle.QUADS, Muscle.LATS), statuses, at(3, 20))
        assertEquals(listOf(Muscle.CHEST), b.map { it.muscle })
    }

    @Test fun templateReadinessAveragesRecovery() {
        // Грудь: 1-е 18:00 → 4-е 02:00 (56 ч); квадрицепсы: → 3-е 18:00 (48 ч). Сейчас 3-е 06:00 — 36 ч прошло.
        val now = at(3, 6)
        assertEquals(1f, templateReadiness(setOf(Muscle.LATS), statuses, now)!!, 1e-6f)
        val chest = 36f / 56f
        val quads = 36f / 48f
        assertEquals((chest + quads + 1f) / 3, templateReadiness(setOf(Muscle.CHEST, Muscle.QUADS, Muscle.LATS), statuses, now)!!, 1e-4f)
        assertEquals(null, templateReadiness(emptySet(), statuses, now))
    }
}
