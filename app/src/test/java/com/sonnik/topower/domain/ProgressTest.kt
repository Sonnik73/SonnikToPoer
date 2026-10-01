package com.sonnik.topower.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressTest {

    private fun set(workout: String, date: Long, reps: Int? = null, weight: Double? = null, sec: Int? = null, warmup: Boolean = false) =
        SetRecord(workout, date, null, false, reps, weight, sec, null, warmup)

    private val bench = ExerciseConfig(trackReps = true, trackWeight = true, trackTime = false, trackDistance = false, useBodyWeight = false)
    private val plank = ExerciseConfig(trackReps = false, trackWeight = false, trackTime = true, trackDistance = false, useBodyWeight = false)

    @Test
    fun recordIsBestEstimatedMax() {
        val records = listOf(
            set("a", 1, reps = 10, weight = 60.0), // 80
            set("b", 2, reps = 6, weight = 70.0), // 84
            set("c", 3, reps = 12, weight = 100.0, warmup = true),
        )
        val p = computeProgress(records, bench)
        assertEquals(1, p.recordIndex)
        assertEquals(84.0, p.recordScore!!, 1e-9)
    }

    @Test
    fun stalledCountsSessionsWithoutAnyGrowth() {
        val records = listOf(
            set("a", 1, reps = 10, weight = 60.0),
            set("b", 2, reps = 11, weight = 60.0), // рост повторений
            set("c", 3, reps = 10, weight = 60.0),
            set("d", 4, reps = 9, weight = 60.0),
            set("e", 5, reps = 11, weight = 60.0), // повтор максимума — не рост
        )
        val p = computeProgress(records, bench)
        assertEquals(3, p.stalledSessions)
        assertEquals(5, p.sessionCount)
    }

    @Test
    fun heavierWeightResetsStall() {
        val records = listOf(
            set("a", 1, reps = 10, weight = 60.0),
            set("b", 2, reps = 10, weight = 60.0),
            set("c", 3, reps = 5, weight = 62.5), // меньше повторений, но больше вес
        )
        assertEquals(0, computeProgress(records, bench).stalledSessions)
    }

    @Test
    fun timeExercise() {
        val records = listOf(set("a", 1, sec = 60), set("b", 2, sec = 50), set("c", 3, sec = 60))
        val p = computeProgress(records, plank)
        assertEquals(0, p.recordIndex)
        assertEquals(2, p.stalledSessions)
    }

    @Test
    fun emptyHistory() {
        val p = computeProgress(emptyList(), bench)
        assertNull(p.recordIndex)
        assertEquals(0, p.stalledSessions)
    }
}
