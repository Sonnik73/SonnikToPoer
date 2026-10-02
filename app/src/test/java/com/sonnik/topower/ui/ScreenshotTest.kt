package com.sonnik.topower.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sonnik.topower.MainActivity
import com.sonnik.topower.SonnikApp
import com.sonnik.topower.data.EquipmentEntity
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.data.SetEntity
import com.sonnik.topower.data.WorkoutEntity
import com.sonnik.topower.data.WorkoutExerciseEntity
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.robolectric.Shadows.shadowOf
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.io.FileOutputStream

/**
 * Снимает скриншоты основных экранов с тестовыми данными в app/build/screenshots —
 * чтобы видеть интерфейс без телефона. Не проверяет, а только сохраняет картинки.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w360dp-h780dp-xhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun seed() {
        // Скриншоты нужны только при ручном запуске сборки (SCREENSHOTS=1), релизную сборку не задерживают.
        assumeTrue(System.getenv("SCREENSHOTS") == "1")
        val app = ApplicationProvider.getApplicationContext<SonnikApp>()
        // Приложение при старте само заполняет упражнения в главном потоке. Блокировать его
        // ожиданием нельзя (взаимная блокировка), поэтому готовим данные в фоне, прокручивая очередь.
        inBackground { seedDemoData(app) }
    }

    /** Выполняет [block] в фоне, прокручивая очередь главного потока, пока он не закончится. */
    private fun <T> inBackground(block: suspend () -> T): T {
        val job = CoroutineScope(Dispatchers.IO).async { block() }
        val looper = shadowOf(Looper.getMainLooper())
        while (!job.isCompleted) {
            looper.idle()
            Thread.sleep(5)
        }
        return runBlocking { job.await() }
    }

    @Test fun home() = shoot("01-home") { waitText("Можно тренировать", substring = true) }

    @Test fun workout() = shoot("02-workout") {
        openActiveWorkout()
        waitText("Рекорд", substring = true)
    }

    @Test fun muscleGuide() = shoot("11-muscle-guide") {
        waitText("Можно тренировать", substring = true)
        compose.onAllNodesWithContentDescription("Названия мышц")[0].performClick()
        waitText("Нажмите на мышцу", substring = true)
        // Название может быть и на карточке под диалогом — нажимаем последнее, в диалоге.
        compose.onAllNodesWithText("Широчайшие").onLast().performClick()
    }

    @Test fun muscleVolume() = shoot("12-muscle-volume") {
        waitText("Можно тренировать", substring = true)
        click("Объём за 7 дней")
        waitText("Рабочие подходы за последние", substring = true)
    }

    @Test fun reminders() = shoot("13-reminders") {
        compose.onAllNodesWithContentDescription("Настройки")[0].performClick()
        waitText("Таймер отдыха")
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("О восстановлении мышц"))
        click("О восстановлении мышц")
        waitText("Когда восстановились все мышцы")
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("Тихие часы"))
    }

    @Test fun templates() = shoot("15-templates") {
        waitText("Можно тренировать", substring = true)
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("Верх тела"))
        waitText("Готовность", substring = true)
    }

    @Test fun generatedTemplate() = shoot("16-generated-template") {
        waitText("Можно тренировать", substring = true)
        // Подбор — запросы к базе; в тесте выполняем его в фоне, как подготовку данных.
        val app = ApplicationProvider.getApplicationContext<SonnikApp>()
        inBackground { app.repository.generateTemplate(System.currentTimeMillis()) { "Подбор: " + it.joinToString() } }
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("Подбор:", substring = true))
        compose.onAllNodesWithText("Подбор:", substring = true)[0].performClick()
        waitText("Подходов", substring = true)
    }

    @Test fun history() = shoot("03-history") {
        click("История")
        waitText("Подтягивания", substring = true)
    }

    @Test fun stats() = shoot("04-stats") {
        click("Статистика")
        waitText("Общая статистика")
    }

    @Test fun exerciseStats() = shoot("05-exercise-stats") {
        click("Статистика")
        waitText("Общая статистика")
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("Жим гантелей лёжа"))
        click("Жим гантелей лёжа")
        waitText("Динамика")
    }

    @Test fun changelog() = shoot("10-changelog") {
        compose.onAllNodesWithContentDescription("Настройки")[0].performClick()
        waitText("Таймер отдыха")
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("Что нового"))
        click("Что нового")
        waitText("Версия 1.15")
    }

    @Test fun exercises() = shoot("06-exercises") {
        click("Упражнения")
        waitText("Планка")
    }

    @Test fun inventory() = shoot("07-inventory") {
        click("Инвентарь")
        waitText("Красная")
    }

    @Test fun settings() = shoot("08-settings") {
        compose.onAllNodesWithContentDescription("Настройки")[0].performClick()
        waitText("Таймер отдыха")
    }

    @Test
    @Config(qualifiers = "+night")
    fun workoutDark() = shoot("09-workout-dark") {
        openActiveWorkout()
        waitText("Рекорд", substring = true)
    }

    /** Кнопка «Продолжить» ниже карточки мышц — сначала прокручиваем к ней. */
    private fun openActiveWorkout() {
        waitText("Можно тренировать", substring = true)
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("Продолжить"))
        click("Продолжить")
    }

    private fun click(text: String) {
        compose.waitForIdle()
        compose.onAllNodesWithText(text)[0].performClick()
    }

    private fun waitText(text: String, substring: Boolean = false) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun shoot(name: String, steps: () -> Unit) {
        steps()
        compose.waitForIdle()
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            // Диалог — отдельное окно: дорисовываем его по центру поверх затемнения.
            ShadowDialog.getLatestDialog()?.takeIf { it.isShowing }?.window?.decorView?.let { dialog ->
                canvas.drawColor(0x99000000.toInt())
                canvas.save()
                canvas.translate((view.width - dialog.width) / 2f, (view.height - dialog.height) / 2f)
                dialog.draw(canvas)
                canvas.restore()
            }
            val dir = File("build/screenshots").apply { mkdirs() }
            FileOutputStream(File(dir, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}

/** Демо-данные: инвентарь, упражнения, шаблон, восемь прошлых тренировок и одна текущая. */
suspend fun seedDemoData(app: SonnikApp) {
    val repo = app.repository
    val dao = repo.dao
    // Упражнения по умолчанию заменяем демо-набором.
    repo.seedDefaultsIfNeeded(app.settings, russian = true)
    dao.clearExercises()
    val day = 86_400_000L
    val now = System.currentTimeMillis()

    val d10 = EquipmentEntity(type = "DUMBBELL", weight = 10.0)
    val d12 = EquipmentEntity(type = "DUMBBELL", weight = 12.0)
    val d14 = EquipmentEntity(type = "DUMBBELL", weight = 14.0)
    val red = EquipmentEntity(name = "Красная", type = "BAND", minResistance = 7.0, maxResistance = 16.0, color = 0xFFE53935)
    val blue = EquipmentEntity(name = "Синяя", type = "BAND", minResistance = 11.0, maxResistance = 23.0, color = 0xFF1E88E5)
    dao.insertEquipment(listOf(d10, d12, d14, red, blue))

    val press = ExerciseEntity(name = "Жим гантелей лёжа", trackWeight = true, equipmentType = "DUMBBELL", muscles = "CHEST,FRONT_DELTS,TRICEPS", repMin = 8, repMax = 12)
    val pull = ExerciseEntity(name = "Подтягивания", trackWeight = true, useBodyWeight = true, muscles = "LATS,BICEPS", repMin = 5, repMax = 8)
    val row = ExerciseEntity(name = "Тяга резины к поясу", trackWeight = true, equipmentType = "BAND", muscles = "LATS,REAR_DELTS")
    val plank = ExerciseEntity(name = "Планка", trackReps = false, trackTime = true, muscles = "ABS")
    val squat = ExerciseEntity(name = "Приседания", trackWeight = true, useBodyWeight = true, muscles = "QUADS,GLUTES")
    dao.insertExercises(listOf(press, pull, row, plank, squat))

    val templateId = repo.createTemplate("Верх тела")
    repo.addExercisesToTemplate(templateId, listOf(press.id, pull.id, row.id, plank.id))
    repo.addExercisesToTemplate(repo.createTemplate("Ноги"), listOf(squat.id))
    repo.addExercisesToTemplate(repo.createTemplate("Кор"), listOf(plank.id))

    suspend fun workout(start: Long, finished: Boolean, bodyWeight: Double, sets: List<Pair<ExerciseEntity, List<SetEntity>>>, watch: Boolean) {
        val w = WorkoutEntity(
            startedAt = start,
            finishedAt = if (finished) start + 70 * 60_000 else null,
            bodyWeight = bodyWeight,
            maxHeartRate = if (watch) 158 else null,
            avgHeartRate = if (watch) 121 else null,
            watchDurationSec = if (watch) 4200 else null,
            calories = if (watch) 520 else null,
        )
        dao.upsertWorkout(w)
        sets.forEachIndexed { i, (exercise, list) ->
            val we = WorkoutExerciseEntity(workoutId = w.id, exerciseId = exercise.id, position = i, plannedSets = 3, recoveryHours = 72)
            dao.upsertWorkoutExercise(we)
            list.forEachIndexed { j, s -> dao.upsertSet(s.copy(workoutExerciseId = we.id, position = j)) }
        }
    }

    fun s(reps: Int? = null, weight: Double? = null, sec: Int? = null, pair: Boolean = false, eq: String? = null,
          eq2: String? = null, bandMin: Double? = null, warmup: Boolean = false) =
        SetEntity(workoutExerciseId = "", position = 0, reps = reps, weight = weight, durationSec = sec, pair = pair,
            equipmentId = eq, equipment2Id = eq2, bandMin = bandMin, isWarmup = warmup)

    for (i in 0 until 8) {
        val dumbbell = if (i < 4) d10 else d12
        workout(
            start = now - (9 - i) * 3 * day,
            finished = true,
            bodyWeight = 82.0 - i * 0.4,
            watch = i % 2 == 0,
            sets = listOf(
                press to List(3) { s(reps = 10 + i % 3, weight = dumbbell.weight, pair = true, eq = dumbbell.id) },
                pull to List(3) { s(reps = 8) },
                row to List(3) { s(reps = 15, weight = if (i < 5) 16.0 else 39.0, bandMin = if (i < 5) 7.0 else 18.0, eq = red.id, eq2 = if (i < 5) null else blue.id) },
                plank to listOf(s(sec = 60 + i * 5)),
            ) + if (i == 5) listOf(squat to List(3) { s(reps = 20) }) else emptyList(),
        )
    }
    // Ноги — полтора дня назад: на схеме промежуточный цвет между красным и зелёным.
    workout(
        start = now - 36 * 3_600_000L,
        finished = true,
        bodyWeight = 79.0,
        watch = false,
        sets = listOf(squat to List(3) { s(reps = 25) }),
    )
    workout(
        start = now - 20 * 60_000,
        finished = false,
        bodyWeight = 78.8,
        watch = false,
        sets = listOf(
            press to listOf(s(reps = 12, weight = 10.0, pair = true, eq = d10.id, warmup = true), s(reps = 12, weight = 14.0, pair = true, eq = d14.id)),
            pull to listOf(s(reps = 8)),
            row to emptyList(),
        ),
    )
}
