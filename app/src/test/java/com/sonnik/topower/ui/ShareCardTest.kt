package com.sonnik.topower.ui

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.domain.VolumeLevel
import com.sonnik.topower.ui.share.SHARE_HEIGHT
import com.sonnik.topower.ui.share.SHARE_WIDTH
import com.sonnik.topower.ui.share.ShareCardData
import com.sonnik.topower.ui.share.ShareRecordLine
import com.sonnik.topower.ui.share.ShareTile
import com.sonnik.topower.ui.share.renderShareCard
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ShareCardTest {
    @Test fun rendersWeekCard() {
        val bitmap = renderShareCard(
            ShareCardData(
                title = "SonnikToPower · моя неделя",
                period = "2 окт. — 8 окт.",
                tiles = listOf(
                    ShareTile("3", "тренировок за неделю"), ShareTile("11", "за 30 дней"),
                    ShareTile("42", "всего тренировок"), ShareTile("6", "недель подряд"),
                    ShareTile("8 450", "тоннаж за неделю, кг"), ShareTile("1 560", "ккал за неделю"),
                ),
                musclesTitle = "Мышцы за неделю",
                volume = mapOf(Muscle.CHEST to 12, Muscle.TRICEPS to 7, Muscle.LATS to 10, Muscle.BICEPS to 4, Muscle.QUADS to 22, Muscle.GLUTES to 9),
                muscleNames = mapOf(Muscle.CHEST to "Грудь", Muscle.TRICEPS to "Трицепс", Muscle.LATS to "Широчайшие", Muscle.BICEPS to "Бицепс", Muscle.QUADS to "Квадрицепсы", Muscle.GLUTES to "Ягодицы"),
                setsFormat = "%d подх.",
                volumeLegend = listOf(VolumeLevel.LOW to "1–5", VolumeLevel.MODERATE to "6–9", VolumeLevel.OPTIMAL to "10–20 — норма", VolumeLevel.HIGH to "21+"),
                recordsTitle = "Рекорды недели",
                records = listOf(
                    ShareRecordLine("Жим гантелей лёжа", "2 × 14 кг · 10 повт.", "+12%"),
                    ShareRecordLine("Подтягивания прямым широким хватом", "СВ+5 кг × 8", "+6%"),
                    ShareRecordLine("Планка", "2:15", "+8%"),
                ),
                noRecords = "",
                footer = "Записано в SonnikToPower",
            )
        )
        assertEquals(SHARE_WIDTH, bitmap.width)
        assertEquals(SHARE_HEIGHT, bitmap.height)
        File("build/screenshots").apply { mkdirs() }.resolve("17-share-card.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
