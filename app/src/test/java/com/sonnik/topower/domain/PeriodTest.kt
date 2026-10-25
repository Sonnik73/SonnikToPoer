package com.sonnik.topower.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class PeriodTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 22) // четверг
    private fun at(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli() + 18 * 3_600_000L

    @Test fun periodStart() {
        assertEquals(at(today.minusDays(6)) - 18 * 3_600_000L, StatsPeriod.WEEK.start(today, zone))
        assertNull(StatsPeriod.ALL.start(today, zone))
    }

    @Test fun weekIsDaily() {
        val (unit, b) = activityBuckets(listOf(at(today), at(today), at(today.minusDays(3)), at(today.minusDays(20))), StatsPeriod.WEEK, today, zone)
        assertEquals(BucketUnit.DAY, unit)
        assertEquals(7, b.size)
        assertEquals(today, b.last().start)
        assertEquals(listOf(0, 0, 0, 1, 0, 0, 2), b.map { it.count })
    }

    @Test fun monthIsWeeklyYearIsMonthly() {
        val (mu, m) = activityBuckets(listOf(at(today)), StatsPeriod.MONTH, today, zone)
        assertEquals(BucketUnit.WEEK, mu)
        assertEquals(weekStart(today), m.last().start)
        assertEquals(1, m.last().count)
        val (yu, y) = activityBuckets(listOf(at(today), at(today.minusMonths(5))), StatsPeriod.YEAR, today, zone)
        assertEquals(BucketUnit.MONTH, yu)
        assertEquals(13, y.size) // от октября прошлого года до текущего
        assertEquals(2, y.sumOf { it.count })
    }

    @Test fun allSwitchesToMonthsForLongHistory() {
        assertEquals(BucketUnit.WEEK, activityBuckets(listOf(at(today.minusWeeks(10))), StatsPeriod.ALL, today, zone).first)
        val (unit, b) = activityBuckets(listOf(at(today.minusMonths(14)), at(today)), StatsPeriod.ALL, today, zone)
        assertEquals(BucketUnit.MONTH, unit)
        assertEquals(15, b.size)
        assertEquals(1, b.first().count)
    }
}
