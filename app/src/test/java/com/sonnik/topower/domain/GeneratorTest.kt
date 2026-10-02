package com.sonnik.topower.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorTest {
    private val h = 3_600_000L
    private val now = 1000 * h

    private fun trained(hoursAgo: Long, recoveryHours: Long = 48) =
        (now - hoursAgo * h).let { it to it + recoveryHours * h }

    private val statuses: Map<Muscle, MuscleStatus> = buildMap {
        // Грудь и трицепс — вчера (восстанавливаются), спина — 3 дня назад, ноги — 6 дней, остальное — не тренировалось.
        listOf(Muscle.CHEST to 20L, Muscle.TRICEPS to 20L, Muscle.FRONT_DELTS to 20L, Muscle.LATS to 72L,
            Muscle.BICEPS to 72L, Muscle.QUADS to 144L, Muscle.GLUTES to 144L, Muscle.ABS to 30L).forEach { (m, ago) ->
            val (last, ready) = trained(ago)
            put(m, MuscleStatus(m, last, ready))
        }
        Muscle.entries.filter { it !in keys }.forEach { put(it, MuscleStatus(it, null, null)) }
    }

    private fun ex(id: String, vararg m: Muscle, done: Int = 0) = ExerciseOption(id, m.toSet(), done)

    private val options = listOf(
        ex("bench", Muscle.CHEST, Muscle.TRICEPS, Muscle.FRONT_DELTS, done = 10),
        ex("row", Muscle.LATS, done = 5),
        ex("chin", Muscle.LATS, Muscle.BICEPS, done = 3),
        ex("squat", Muscle.QUADS, Muscle.GLUTES, done = 2),
        ex("lateral", Muscle.SIDE_DELTS),
        ex("reverse_fly", Muscle.REAR_DELTS),
        ex("shrug", Muscle.TRAPS),
        ex("wrist", Muscle.FOREARMS),
        ex("crunch", Muscle.ABS, done = 4),
        ex("side_bend", Muscle.OBLIQUES),
        ex("calf", Muscle.CALVES),
    )

    @Test fun skipsRecoveringMusclesAndPrefersMostRested() {
        val picked = generateWorkout(options, statuses, now).map { it.id }
        assertFalse("bench" in picked)
        assertFalse("crunch" in picked) // пресс ещё восстанавливается (30 ч из 48)
        assertTrue(picked.size in GENERATED_MIN_EXERCISES..GENERATED_MAX_EXERCISES)
        // Не тренировавшиеся мышцы — самые отдохнувшие.
        assertTrue("lateral" in picked)
        assertTrue("reverse_fly" in picked)
    }

    @Test fun restedOrder() {
        val rested = restedMuscles(statuses, now)
        assertFalse(Muscle.CHEST in rested)
        // Из тренировавшихся первыми идут ноги (6 дней назад), потом спина (3 дня).
        val trainedOrder = rested.filter { statuses[it]?.lastTrained != null }
        assertEquals(setOf(Muscle.QUADS, Muscle.GLUTES), trainedOrder.take(2).toSet())
        assertEquals(setOf(Muscle.LATS, Muscle.BICEPS), trainedOrder.drop(2).toSet())
    }

    @Test fun prefersExerciseCoveringMoreTargetsThenHabit() {
        // Только спина и бицепс готовы: подтягивания покрывают обе — их и берём, тягу уже не нужно.
        val onlyBack = statuses.mapValues { (m, s) ->
            if (m == Muscle.LATS || m == Muscle.BICEPS) s else MuscleStatus(m, now - h, now + 10 * h)
        }
        assertEquals(listOf("chin"), generateWorkout(options, onlyBack, now).map { it.id })
    }

    @Test fun compoundFirst() {
        val picked = generateWorkout(options, statuses, now)
        assertEquals(picked.sortedByDescending { it.muscles.size }, picked)
    }
}
