package com.sonnik.topower.domain

/** За сколько дней считается недельный объём. */
const val VOLUME_DAYS = 7

private const val VOLUME_DAY_MS = 86_400_000L

/** Рабочие подходы упражнения в тренировке: когда, на какие мышцы и сколько. */
data class MuscleSets(val time: Long, val muscles: Set<Muscle>, val sets: Int)

/** Рабочие подходы на каждую мышцу за последние [days] дней (подход упражнения засчитывается всем его основным мышцам). */
fun weeklySets(rows: List<MuscleSets>, now: Long, days: Int = VOLUME_DAYS): Map<Muscle, Int> {
    val since = now - days * VOLUME_DAY_MS
    val recent = rows.filter { it.time in (since + 1)..now }
    return Muscle.entries.associateWith { m -> recent.filter { m in it.muscles }.sumOf { it.sets } }
}

/** Оценка недельного объёма: ориентир для роста мышц — 10–20 рабочих подходов в неделю. */
enum class VolumeLevel { NONE, LOW, MODERATE, OPTIMAL, HIGH }

fun volumeLevel(sets: Int): VolumeLevel = when {
    sets <= 0 -> VolumeLevel.NONE
    sets < 6 -> VolumeLevel.LOW
    sets < 10 -> VolumeLevel.MODERATE
    sets <= 20 -> VolumeLevel.OPTIMAL
    else -> VolumeLevel.HIGH
}

/** Мышцы шаблона, которые ещё восстанавливаются, — от самой долгой. */
fun blockingMuscles(templateMuscles: Set<Muscle>, statuses: Map<Muscle, MuscleStatus>, now: Long): List<MuscleStatus> =
    templateMuscles.mapNotNull { statuses[it] }
        .filter { it.state(now) == MuscleState.RECOVERING }
        .sortedByDescending { it.readyAt }

/**
 * Готовность шаблона от 0 (все мышцы только что нагружены) до 1 (все восстановились): средняя доля
 * восстановления его мышц. Мышцы, которые давно или ещё не тренировались, считаются готовыми. null — у шаблона нет мышц.
 */
fun templateReadiness(templateMuscles: Set<Muscle>, statuses: Map<Muscle, MuscleStatus>, now: Long): Float? {
    if (templateMuscles.isEmpty()) return null
    return templateMuscles.map { m -> statuses[m]?.recoveredFraction(now) ?: 1f }.average().toFloat()
}
