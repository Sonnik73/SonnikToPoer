package com.sonnik.topower.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class StatsTest {

    private fun set(
        workout: String,
        date: Long,
        reps: Int? = null,
        weight: Double? = null,
        body: Double? = null,
        useBody: Boolean = false,
        sec: Int? = null,
        km: Double? = null,
        warmup: Boolean = false,
    ) = SetRecord(workout, date, body, useBody, reps, weight, sec, km, warmup)

    private val bench = ExerciseConfig(trackReps = true, trackWeight = true, trackTime = false, trackDistance = false, useBodyWeight = false)
    private val pullUps = ExerciseConfig(trackReps = true, trackWeight = true, trackTime = false, trackDistance = false, useBodyWeight = true)

    @Test
    fun effectiveWeightAddsBodyWeight() {
        assertEquals(90.0, effectiveWeight(set("w", 0, reps = 5, weight = 10.0, body = 80.0, useBody = true))!!, 1e-9)
        assertEquals(80.0, effectiveWeight(set("w", 0, reps = 5, body = 80.0, useBody = true))!!, 1e-9)
        assertEquals(60.0, effectiveWeight(set("w", 0, reps = 5, weight = 60.0, body = 80.0))!!, 1e-9)
        assertNull(effectiveWeight(set("w", 0, reps = 5, useBody = true)))
    }

    @Test
    fun benchStatsIgnoreWarmupAndTrackStartAndMax() {
        val records = listOf(
            set("a", 1, reps = 10, weight = 20.0, warmup = true),
            set("a", 1, reps = 10, weight = 50.0),
            set("a", 1, reps = 8, weight = 55.0),
            set("b", 2, reps = 6, weight = 70.0),
            set("c", 3, reps = 10, weight = 60.0),
        )
        val stats = computeExerciseStats(records, bench)!!
        assertEquals(3, stats.workoutCount)
        assertEquals(4, stats.setCount)
        val weight = stats.metrics.first { it.metric == Metric.WEIGHT }
        assertEquals(55.0, weight.first.value, 1e-9)
        assertEquals(70.0, weight.best.value, 1e-9)
        assertEquals(60.0, weight.last.value, 1e-9)
        assertEquals(15.0, weight.gain, 1e-9)
        val volume = stats.metrics.first { it.metric == Metric.VOLUME }
        assertEquals(50.0 * 10 + 55.0 * 8, volume.first.value, 1e-9)
        // Лучший подход — по расчётному максимуму: 70×6 → 84 больше, чем 60×10 → 80
        assertEquals(70.0, stats.bestSet!!.weight!!, 1e-9)
    }

    @Test
    fun pullUpsUseBodyWeight() {
        val records = listOf(
            set("a", 1, reps = 8, body = 80.0, useBody = true),
            set("b", 2, reps = 5, weight = 10.0, body = 81.0, useBody = true),
        )
        val stats = computeExerciseStats(records, pullUps)!!
        val weight = stats.metrics.first { it.metric == Metric.WEIGHT }
        assertEquals(80.0, weight.first.value, 1e-9)
        assertEquals(91.0, weight.best.value, 1e-9)
    }

    @Test
    fun onlyWarmupsGiveNoStats() {
        assertNull(computeExerciseStats(listOf(set("a", 1, reps = 5, warmup = true)), bench))
    }

    @Test
    fun overallStatsCountsWeeks() {
        val zone = ZoneOffset.UTC
        val today = LocalDate.of(2026, 10, 1) // четверг
        fun ms(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()
        val dates = listOf(ms(LocalDate.of(2026, 9, 28)), ms(LocalDate.of(2026, 9, 30)), ms(LocalDate.of(2026, 9, 25)))
        val sets = listOf(set("a", dates[0], reps = 10, weight = 50.0), set("b", dates[1], km = 5.0))
        val stats = computeOverallStats(dates, sets, today, zone, weeks = 4)
        assertEquals(3, stats.totalWorkouts)
        assertEquals(2, stats.workoutsThisWeek)
        assertEquals(0, stats.workoutsThisMonth) // все тренировки в сентябре, а сегодня 1 октября
        assertEquals(500.0, stats.totalTonnage, 1e-9)
        assertEquals(5.0, stats.totalDistanceKm, 1e-9)
        assertEquals(4, stats.weekly.size)
        assertEquals(2, stats.weekly.last().count)
        assertEquals(1, stats.weekly[2].count)
    }

    @Test
    fun numbersFormatAndParse() {
        assertEquals("60", formatNumber(60.0))
        assertEquals("62.5", formatNumber(62.5))
        assertEquals("1.33", formatNumber(1.3333))
        assertEquals(62.5, parseDecimal(" 62,5 ")!!, 1e-9)
        assertNull(parseDecimal(""))
        assertEquals("1:05", formatDuration(65))
        assertEquals("1:00:05", formatDuration(3605))
    }
}
