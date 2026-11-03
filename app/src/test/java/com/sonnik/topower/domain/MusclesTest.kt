package com.sonnik.topower.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MusclesTest {
    private val h = 3_600_000L
    private val day = 24 * h

    @Test
    fun parseAndFormat() {
        assertEquals(setOf(Muscle.CHEST, Muscle.TRICEPS), Muscle.parse("TRICEPS, CHEST,unknown"))
        assertEquals("CHEST,TRICEPS", Muscle.format(setOf(Muscle.TRICEPS, Muscle.CHEST)))
        assertEquals(emptySet<Muscle>(), Muscle.parse(""))
    }

    @Test
    fun recoveryUsesChosenHoursAndExtraExercises() {
        val work = listOf(
            MuscleWork("w1", 0, setOf(Muscle.CHEST, Muscle.TRICEPS), 72),
            MuscleWork("w1", 0, setOf(Muscle.TRICEPS), 24),
            MuscleWork("w1", 0, setOf(Muscle.TRICEPS), null),
            MuscleWork("w1", 0, setOf(Muscle.BICEPS), null),
        )
        val status = computeMuscleStatus(work)
        assertEquals(72 * h, status.getValue(Muscle.CHEST).readyAt)
        // трицепс: максимум 72 ч + 2 доп. упражнения × 12 ч
        assertEquals(96 * h, status.getValue(Muscle.TRICEPS).readyAt)
        assertEquals(48 * h, status.getValue(Muscle.BICEPS).readyAt)
        assertEquals(MuscleState.NEVER, status.getValue(Muscle.CALVES).state(0))
    }

    @Test
    fun statesOverTime() {
        val s = computeMuscleStatus(listOf(MuscleWork("w", 0, setOf(Muscle.LATS), 48))).getValue(Muscle.LATS)
        assertEquals(MuscleState.RECOVERING, s.state(24 * h))
        assertEquals(0.5f, s.recoveredFraction(24 * h), 1e-6f)
        assertEquals(24, s.hoursLeft(24 * h))
        assertEquals(MuscleState.READY, s.state(3 * day))
        assertEquals(MuscleState.STALE, s.state(11 * day))
    }

    @Test
    fun latestWorkoutWins() {
        val s = computeMuscleStatus(
            listOf(MuscleWork("old", 0, setOf(Muscle.QUADS), 96), MuscleWork("new", 5 * day, setOf(Muscle.QUADS), 24))
        ).getValue(Muscle.QUADS)
        assertEquals(5 * day, s.lastTrained)
        assertEquals(5 * day + 24 * h, s.readyAt)
    }

    @Test
    fun suggestions() {
        assertEquals(setOf(Muscle.LATS, Muscle.BICEPS, Muscle.BRACHIALIS), suggestMuscles("Подтягивания узким обратным хватом"))
        assertEquals(setOf(Muscle.LATS), suggestMuscles("Подтягивания прямым широким хватом"))
        assertEquals(setOf(Muscle.BICEPS, Muscle.BRACHIALIS, Muscle.BRACHIORADIALIS), suggestMuscles("Молотки с гантелями"))
        assertEquals(setOf(Muscle.BRACHIORADIALIS, Muscle.BRACHIALIS, Muscle.FOREARMS), suggestMuscles("Сгибания рук обратным хватом"))
        assertEquals(setOf(Muscle.TIBIALIS), suggestMuscles("Подъёмы носков стоя у стены"))
        assertEquals(setOf(Muscle.CALVES), suggestMuscles("Подъёмы на носки с гантелями"))
        assertEquals(setOf(Muscle.CHEST), suggestMuscles("Разводка гантелей на скамье"))
        assertEquals(setOf(Muscle.CALVES), suggestMuscles("Прыжки на скакалке"))
    }
}
