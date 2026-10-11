package com.sonnik.topower.ui

import com.sonnik.topower.data.EquipmentType
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.ui.exercises.BodyArea
import com.sonnik.topower.ui.exercises.ToolFilter
import com.sonnik.topower.ui.exercises.filterExercises
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseFilterTest {
    private val names = mapOf(Muscle.BICEPS to "Бицепс", Muscle.LATS to "Широчайшие", Muscle.CHEST to "Грудь", Muscle.QUADS to "Квадрицепсы")
    private fun name(m: Muscle) = names[m] ?: m.name

    private val curl = ExerciseEntity(name = "Сгибания рук с гантелями", muscles = "BICEPS", equipmentType = "DUMBBELL")
    private val chin = ExerciseEntity(name = "Подтягивания", muscles = "LATS,BICEPS", useBodyWeight = true)
    private val bandCurl = ExerciseEntity(name = "Сгибания с резиной", muscles = "BICEPS", equipmentType = "BAND")
    private val press = ExerciseEntity(name = "Жим гантелей лёжа", muscles = "CHEST", equipmentType = "DUMBBELL")
    private val squat = ExerciseEntity(name = "Приседания в жилете", muscles = "QUADS", useBodyWeight = true, equipmentType = "WEIGHT_VEST")
    private val all = listOf(curl, chin, bandCurl, press, squat)

    private fun ids(list: List<ExerciseEntity>) = list.map { it.name }

    @Test fun emptyFilterKeepsAll() {
        assertEquals(ids(all), ids(filterExercises(all, "  ", null, null, ::name)))
    }

    @Test fun queryMatchesNameAndMuscles() {
        assertEquals(ids(listOf(curl, bandCurl)), ids(filterExercises(all, "сгиб", null, null, ::name)))
        // «бицепс» нет в названиях, но это основная мышца трёх упражнений.
        assertEquals(ids(listOf(curl, chin, bandCurl)), ids(filterExercises(all, "БИЦЕПС", null, null, ::name)))
    }

    @Test fun areaAndToolCombine() {
        assertEquals(ids(listOf(curl, chin, bandCurl)), ids(filterExercises(all, "", BodyArea.ARMS, null, ::name)))
        assertEquals(ids(listOf(curl)), ids(filterExercises(all, "", BodyArea.ARMS, ToolFilter(EquipmentType.DUMBBELL), ::name)))
        assertEquals(ids(listOf(chin, squat)), ids(filterExercises(all, "", null, ToolFilter.BODYWEIGHT, ::name)))
        assertEquals(ids(listOf(squat)), ids(filterExercises(all, "", BodyArea.LEGS, ToolFilter.BODYWEIGHT, ::name)))
    }

    @Test fun availableToolsComeFromExercises() {
        assertEquals(
            listOf(ToolFilter.BODYWEIGHT, ToolFilter(EquipmentType.DUMBBELL), ToolFilter(EquipmentType.BAND), ToolFilter(EquipmentType.WEIGHT_VEST)),
            ToolFilter.available(all),
        )
    }

    @Test fun everyMuscleBelongsToOneArea() {
        Muscle.entries.forEach { m -> assertEquals(m.name, 1, BodyArea.entries.count { m in it.muscles }) }
    }
}
