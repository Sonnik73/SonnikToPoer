package com.sonnik.topower.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sonnik.topower.R
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.data.SetEntity
import com.sonnik.topower.data.SetWithContext
import com.sonnik.topower.domain.ExerciseConfig
import com.sonnik.topower.domain.Metric
import com.sonnik.topower.domain.SetRecord
import com.sonnik.topower.domain.formatDuration
import com.sonnik.topower.domain.formatNumber
import com.sonnik.topower.ui.common.formatSet

fun SetWithContext.toRecord() = SetRecord(
    workoutId = workoutId,
    date = workoutDate,
    bodyWeight = bodyWeight,
    useBodyWeight = useBodyWeight,
    reps = set.reps,
    weight = set.weight,
    durationSec = set.durationSec,
    distanceKm = set.distanceKm,
    isWarmup = set.isWarmup,
    pair = set.pair,
)

fun ExerciseEntity.config() = ExerciseConfig(
    trackReps = trackReps,
    trackWeight = trackWeight,
    trackTime = trackTime,
    trackDistance = trackDistance,
    useBodyWeight = useBodyWeight,
)

@Composable
fun metricLabel(metric: Metric): String = stringResource(
    when (metric) {
        Metric.WEIGHT -> R.string.metric_weight
        Metric.REPS -> R.string.metric_reps
        Metric.E1RM -> R.string.metric_e1rm
        Metric.VOLUME -> R.string.metric_volume
        Metric.TIME -> R.string.metric_time
        Metric.DISTANCE -> R.string.metric_distance
    }
)

/** Значение метрики с единицами измерения. */
@Composable
fun formatMetric(metric: Metric, value: Double): String = when (metric) {
    Metric.WEIGHT, Metric.E1RM -> stringResource(R.string.fmt_kg, formatNumber(value, 1))
    Metric.VOLUME -> stringResource(R.string.fmt_kg, formatNumber(value, 0))
    Metric.REPS -> formatNumber(value, 0)
    Metric.TIME -> formatDuration(value.toInt())
    Metric.DISTANCE -> stringResource(R.string.fmt_km, formatNumber(value))
}

@Composable
fun formatRecord(record: SetRecord, exercise: ExerciseEntity): String = formatSet(
    SetEntity(
        workoutExerciseId = "",
        position = 0,
        reps = record.reps,
        weight = record.weight,
        durationSec = record.durationSec,
        distanceKm = record.distanceKm,
        isWarmup = record.isWarmup,
        pair = record.pair,
    ),
    exercise,
    record.bodyWeight,
)
