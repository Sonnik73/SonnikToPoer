package com.sonnik.topower.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TargetTest {
    private val weighted = ExerciseConfig(trackReps = true, trackWeight = true, trackTime = false, trackDistance = false, useBodyWeight = false)
    private val bodyweight = ExerciseConfig(trackReps = true, trackWeight = false, trackTime = false, trackDistance = false, useBodyWeight = true)
    private val plank = ExerciseConfig(trackReps = false, trackWeight = false, trackTime = true, trackDistance = false, useBodyWeight = false)
    private val run = ExerciseConfig(trackReps = false, trackWeight = false, trackTime = true, trackDistance = true, useBodyWeight = false)
    private val dumbbells = listOf(10.0, 12.0, 14.0)
    private val next: (Double) -> Double? = { w -> dumbbells.firstOrNull { it > w } }

    @Test fun noHistoryNoTarget() {
        assertNull(nextTarget(emptyList(), weighted, 8..12, next))
    }

    @Test fun addsRepToWeakestSetInTopWeight() {
        val prev = listOf(PlainSet(12, 10.0), PlainSet(10, 12.0), PlainSet(9, 12.0))
        val t = nextTarget(prev, weighted, 8..12, next)!!
        assertEquals(TargetKind.MORE_REPS, t.kind)
        assertEquals(10, t.reps)
        assertEquals(12.0, t.weight!!, 0.0)
    }

    @Test fun topOfRangeMovesToNextWeight() {
        val prev = listOf(PlainSet(12, 10.0), PlainSet(12, 10.0), PlainSet(13, 10.0))
        val t = nextTarget(prev, weighted, 8..12, next)!!
        assertEquals(TargetKind.MORE_LOAD, t.kind)
        assertEquals(8, t.reps)
        assertEquals(12.0, t.weight!!, 0.0)
    }

    @Test fun nothingHeavierMeansMakeHarder() {
        val prev = listOf(PlainSet(12, 14.0))
        val t = nextTarget(prev, weighted, 8..12, next)!!
        assertEquals(TargetKind.MAKE_HARDER, t.kind)
        assertEquals(12, t.reps)
    }

    @Test fun withoutRangeAlwaysOneMoreRep() {
        val t = nextTarget(listOf(PlainSet(40, 10.0)), weighted, null, next)!!
        assertEquals(TargetKind.MORE_REPS, t.kind)
        assertEquals(41, t.reps)
    }

    @Test fun belowRangeJumpsToBottom() {
        val t = nextTarget(listOf(PlainSet(4, 12.0)), weighted, 6..10, next)!!
        assertEquals(6, t.reps)
    }

    @Test fun bodyweightAtTopNeedsHarderVariation() {
        val t = nextTarget(listOf(PlainSet(20), PlainSet(20)), bodyweight, 10..20, next)!!
        assertEquals(TargetKind.MAKE_HARDER, t.kind)
        val more = nextTarget(listOf(PlainSet(15), PlainSet(14)), bodyweight, 10..20, next)!!
        assertEquals(15, more.reps)
    }

    @Test fun assistBandGoesWeaker() {
        // Помощь резины — отрицательный вес: «тяжелее» = слабее резина.
        val bands = listOf(-35.0, -23.0, -16.0)
        val t = nextTarget(listOf(PlainSet(10, -23.0)), weighted, 6..10) { w -> bands.firstOrNull { it > w } }!!
        assertEquals(TargetKind.MORE_LOAD, t.kind)
        assertEquals(-16.0, t.weight!!, 0.0)
    }

    @Test fun timeAndDistanceGrow() {
        assertEquals(50, nextTarget(listOf(PlainSet(durationSec = 45)), plank, null, next)!!.durationSec)
        assertEquals(100, nextTarget(listOf(PlainSet(durationSec = 90)), plank, null, next)!!.durationSec)
        val t = nextTarget(listOf(PlainSet(durationSec = 1800, distanceKm = 5.0)), run.copy(trackTime = false), null, next)!!
        assertEquals(5.3, t.distanceKm!!, 1e-9)
    }

    @Test fun rangeNormalization() {
        assertEquals(8..12, repRange(8, 12))
        assertEquals(12..12, repRange(null, 12))
        assertEquals(10..10, repRange(15, 10))
        assertNull(repRange(8, null))
    }
}
