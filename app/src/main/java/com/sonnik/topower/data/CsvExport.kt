package com.sonnik.topower.data

import com.sonnik.topower.domain.Metric
import com.sonnik.topower.domain.SetRecord
import com.sonnik.topower.domain.setValue
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Выгрузка всех подходов в CSV для Excel и Google Таблиц: строка на подход.
 * Разделитель «;», в начале BOM — так Excel сразу открывает файл по колонкам и с кириллицей.
 */
object CsvExport {
    /** Заголовки колонок по порядку (локализуются снаружи). */
    const val COLUMNS = 14

    fun build(
        header: List<String>,
        exercises: List<ExerciseEntity>,
        workouts: List<WorkoutEntity>,
        workoutExercises: List<WorkoutExerciseEntity>,
        sets: List<SetEntity>,
        equipment: List<EquipmentEntity>,
        zone: ZoneId,
        decimalComma: Boolean,
        warmupMark: String,
        yesMark: String,
        muscleNames: (String) -> String = { it },
    ): String {
        require(header.size == COLUMNS)
        val exerciseById = exercises.associateBy { it.id }
        val equipmentById = equipment.associateBy { it.id }
        val itemsByWorkout = workoutExercises.groupBy { it.workoutId }
        val setsByItem = sets.groupBy { it.workoutExerciseId }
        val date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val time = DateTimeFormatter.ofPattern("HH:mm")

        fun num(value: Double?): String {
            value ?: return ""
            val text = BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            return if (decimalComma) text.replace('.', ',') else text
        }

        val out = StringBuilder("﻿")
        out.appendRow(header)
        workouts.sortedBy { it.startedAt }.forEach { w ->
            val started = Instant.ofEpochMilli(w.startedAt).atZone(zone)
            itemsByWorkout[w.id].orEmpty().sortedBy { it.position }.forEach { item ->
                val exercise = exerciseById[item.exerciseId] ?: return@forEach
                var working = 0
                setsByItem[item.id].orEmpty().sortedBy { it.position }.forEach { s ->
                    if (!s.isWarmup) working++
                    val tonnage = setValue(
                        SetRecord(w.id, w.startedAt, w.bodyWeight, exercise.useBodyWeight, s.reps, s.weight,
                            s.durationSec, s.distanceKm, s.isWarmup, s.pair),
                        Metric.VOLUME,
                    )
                    val gear = listOfNotNull(s.equipmentId, s.equipment2Id).mapNotNull { equipmentById[it] }
                        .joinToString(" + ") { it.name.ifBlank { num(it.weight ?: it.maxResistance) } }
                    out.appendRow(
                        listOf(
                            started.format(date),
                            started.format(time),
                            exercise.name,
                            if (s.isWarmup) warmupMark else working.toString(),
                            s.reps?.toString().orEmpty(),
                            num(s.weight),
                            if (s.pair) yesMark else "",
                            gear,
                            num(w.bodyWeight),
                            s.durationSec?.toString().orEmpty(),
                            num(s.distanceKm),
                            if (s.isWarmup) "" else num(tonnage),
                            muscleNames(exercise.muscles),
                            w.note,
                        )
                    )
                }
            }
        }
        return out.toString()
    }

    private fun StringBuilder.appendRow(cells: List<String>) {
        cells.joinTo(this, ";") { cell ->
            if (cell.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + cell.replace("\"", "\"\"") + "\"" else cell
        }
        append("\r\n")
    }
}
