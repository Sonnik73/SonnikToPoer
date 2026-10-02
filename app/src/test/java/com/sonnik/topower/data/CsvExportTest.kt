package com.sonnik.topower.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class CsvExportTest {
    @Test fun oneRowPerSetWithWarmupMarkAndTonnage() {
        val press = ExerciseEntity(id = "e", name = "Жим; гантелей", trackWeight = true, muscles = "CHEST")
        val w = WorkoutEntity(id = "w", startedAt = 1_759_400_000_000, bodyWeight = 80.5, note = "ok \"1\"")
        val item = WorkoutExerciseEntity(id = "i", workoutId = "w", exerciseId = "e", position = 0)
        val d12 = EquipmentEntity(id = "d", type = "DUMBBELL", weight = 12.0)
        val sets = listOf(
            SetEntity(workoutExerciseId = "i", position = 1, reps = 10, weight = 12.0, pair = true, equipmentId = "d"),
            SetEntity(workoutExerciseId = "i", position = 0, reps = 12, weight = 8.0, isWarmup = true),
        )
        val csv = CsvExport.build(
            header = (1..CsvExport.COLUMNS).map { "c$it" },
            exercises = listOf(press), workouts = listOf(w), workoutExercises = listOf(item), sets = sets,
            equipment = listOf(d12), zone = ZoneOffset.UTC, decimalComma = true, warmupMark = "Р", yesMark = "да",
            muscleNames = { "Грудь" },
        )
        assertTrue(csv.startsWith("﻿"))
        val lines = csv.removePrefix("﻿").trimEnd().split("\r\n")
        assertEquals(3, lines.size)
        assertEquals(
            "2025-10-02;10:13;\"Жим; гантелей\";Р;12;8;;;80,5;;;;Грудь;\"ok \"\"1\"\"\"",
            lines[1],
        )
        assertEquals(
            "2025-10-02;10:13;\"Жим; гантелей\";1;10;12;да;12;80,5;;;240;Грудь;\"ok \"\"1\"\"\"",
            lines[2],
        )
    }
}
