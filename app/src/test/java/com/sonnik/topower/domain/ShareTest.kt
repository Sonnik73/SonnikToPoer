package com.sonnik.topower.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ShareTest {
    private val zone = ZoneOffset.UTC
    private fun day(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli() + 10 * 3_600_000L
    private val today = LocalDate.of(2026, 10, 8) // четверг

    @Test fun streakCountsConsecutiveWeeks() {
        val dates = listOf(today, today.minusWeeks(1), today.minusWeeks(2), today.minusWeeks(4)).map(::day)
        assertEquals(3, weekStreak(dates, today, zone))
    }

    @Test fun streakNotBrokenByEmptyCurrentWeek() {
        val dates = listOf(today.minusWeeks(1), today.minusWeeks(2)).map(::day)
        assertEquals(2, weekStreak(dates, today, zone))
        assertEquals(0, weekStreak(listOf(day(today.minusWeeks(3))), today, zone))
    }

    private val cfg = ExerciseConfig(trackReps = true, trackWeight = true, trackTime = false, trackDistance = false, useBodyWeight = false)
    private data class Row(val ex: String, val r: SetRecord)
    private fun row(ex: String, date: LocalDate, reps: Int, w: Double, warm: Boolean = false) =
        Row(ex, SetRecord(ex + date, day(date), null, false, reps, w, null, null, warm))

    @Test fun recentRecordsNeedHistoryAndBeatIt() {
        val since = day(today.minusDays(6))
        val rows = listOf(
            row("press", today.minusDays(20), 10, 10.0),
            row("press", today.minusDays(2), 10, 12.0), // рекорд
            row("row", today.minusDays(20), 12, 20.0),
            row("row", today.minusDays(1), 10, 20.0), // хуже — не рекорд
            row("new", today.minusDays(1), 10, 50.0), // нет истории — не рекорд
            row("curl", today.minusDays(20), 10, 8.0),
            row("curl", today.minusDays(1), 12, 20.0, warm = true), // разминка не считается
        )
        val records = recentRecords(rows, { it.ex }, { it.r }, { cfg }, since)
        assertEquals(listOf("press"), records.map { it.exerciseId })
        assertEquals(20.0, records.single().gainPercent, 1e-6)
    }

    @Test fun periodSummaryCountsOnlyWindow() {
        val now = day(today)
        val sets = listOf(row("a", today.minusDays(1), 10, 10.0).r, row("a", today.minusDays(10), 10, 10.0).r, row("a", today, 5, 10.0, warm = true).r)
        val s = periodSummary(listOf(day(today.minusDays(1)), day(today.minusDays(10))), sets, now, 7)
        assertEquals(1, s.workouts)
        assertEquals(1, s.sets)
        assertEquals(100.0, s.tonnage, 1e-9)
    }
}
