package com.sonnik.topower.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Период статистики; [days] == null — за всё время. */
enum class StatsPeriod(val days: Long?) {
    WEEK(7), MONTH(30), YEAR(365), ALL(null);

    /** Начало периода (полночь первого дня) или null для «всё время». */
    fun start(today: LocalDate, zone: ZoneId): Long? =
        days?.let { today.minusDays(it - 1).atStartOfDay(zone).toInstant().toEpochMilli() }
}

/** Шаг диаграммы тренировок: по дням, неделям (с понедельника) или месяцам. */
enum class BucketUnit { DAY, WEEK, MONTH }

data class CountBucket(val start: LocalDate, val count: Int)

/** Через сколько недель истории «всё время» переходит с недельных столбцов на месячные. */
private const val WEEKLY_LIMIT = 26

/**
 * Число тренировок по шагам периода: неделя — по дням, месяц — по неделям, год — по месяцам,
 * всё время — по неделям, а при длинной истории (больше [WEEKLY_LIMIT] недель) — по месяцам.
 */
fun activityBuckets(
    workoutDates: List<Long>,
    period: StatsPeriod,
    today: LocalDate,
    zone: ZoneId,
): Pair<BucketUnit, List<CountBucket>> {
    val days = workoutDates.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    val first = days.minOrNull() ?: today
    val unit = when (period) {
        StatsPeriod.WEEK -> BucketUnit.DAY
        StatsPeriod.MONTH -> BucketUnit.WEEK
        StatsPeriod.YEAR -> BucketUnit.MONTH
        StatsPeriod.ALL ->
            if (ChronoUnit.WEEKS.between(weekStart(first), weekStart(today)) < WEEKLY_LIMIT) BucketUnit.WEEK else BucketUnit.MONTH
    }
    fun bucketOf(d: LocalDate): LocalDate = when (unit) {
        BucketUnit.DAY -> d
        BucketUnit.WEEK -> weekStart(d)
        BucketUnit.MONTH -> d.withDayOfMonth(1)
    }
    fun next(d: LocalDate): LocalDate = when (unit) {
        BucketUnit.DAY -> d.plusDays(1)
        BucketUnit.WEEK -> d.plusWeeks(1)
        BucketUnit.MONTH -> d.plusMonths(1)
    }
    val from = bucketOf(
        when (period) {
            StatsPeriod.ALL -> first
            else -> today.minusDays(period.days!! - 1)
        }
    )
    val counts = days.groupingBy { bucketOf(it) }.eachCount()
    val buckets = mutableListOf<CountBucket>()
    var d = from
    val last = bucketOf(today)
    while (!d.isAfter(last)) {
        buckets += CountBucket(d, counts[d] ?: 0)
        d = next(d)
    }
    return unit to buckets
}
