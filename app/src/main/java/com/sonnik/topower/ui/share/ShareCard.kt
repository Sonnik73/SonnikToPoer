package com.sonnik.topower.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.domain.VolumeLevel
import com.sonnik.topower.domain.volumeLevel
import com.sonnik.topower.ui.muscles.renderBodyBitmap
import com.sonnik.topower.ui.muscles.volumeColor
import java.io.File

data class ShareTile(val value: String, val label: String)

data class ShareRecordLine(val exercise: String, val set: String, val gain: String)

/** Всё, что попадает на карточку, уже в виде строк — рендер не зависит от Compose. */
data class ShareCardData(
    val title: String,
    val period: String,
    val tiles: List<ShareTile>,
    val musclesTitle: String,
    val volume: Map<Muscle, Int>,
    val muscleNames: Map<Muscle, String>,
    /** «%d подх.» */
    val setsFormat: String,
    val volumeLegend: List<Pair<VolumeLevel, String>>,
    val recordsTitle: String,
    val records: List<ShareRecordLine>,
    val noRecords: String,
    val footer: String,
)

const val SHARE_WIDTH = 1080
const val SHARE_HEIGHT = 1350

private val CardColor = 0xFFFFFFFF.toInt()
private val TileColor = 0xFFF2F4F7.toInt()
private val TextColor = 0xFF1B1F24.toInt()
private val MutedColor = 0xFF5F6670.toInt()
private val AccentColor = 0xFF2E5C8A.toInt()
private val GainColor = 0xFF2E7D32.toInt()
private val IdleMuscle = 0xFFC9CED6.toInt()
private val BodyColor = 0xFFE6E9ED.toInt()

private fun text(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = size
    this.color = color
    typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
}

/** Текст, укороченный с «…» до [maxWidth] (свой расчёт — не зависит от TextUtils). */
private fun ellipsize(text: String, paint: TextPaint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    var end = text.length
    while (end > 0 && paint.measureText(text, 0, end) + paint.measureText("…") > maxWidth) end--
    return text.substring(0, end).trimEnd() + "…"
}

private fun Canvas.line(text: String, x: Float, y: Float, paint: TextPaint, maxWidth: Float) {
    drawText(ellipsize(text, paint, maxWidth), x, y, paint)
}

/** Строка в [maxWidth]: сначала уменьшаем шрифт (до [minSize]), потом укорачиваем. */
private fun Canvas.fitLine(text: String, x: Float, y: Float, paint: TextPaint, maxWidth: Float, minSize: Float) {
    val p = TextPaint(paint)
    while (p.measureText(text) > maxWidth && p.textSize > minSize) p.textSize -= 1f
    line(text, x, y, p, maxWidth)
}

/** Карточка 1080×1350 с итогами недели: плитки, схема нагруженных мышц и рекорды. */
fun renderShareCard(data: ShareCardData): Bitmap {
    val bitmap = Bitmap.createBitmap(SHARE_WIDTH, SHARE_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawColor(CardColor)
    // Акцентная полоса сверху.
    fill.color = AccentColor
    canvas.drawRect(0f, 0f, SHARE_WIDTH.toFloat(), 16f, fill)

    val left = 96f
    val width = SHARE_WIDTH - 2 * left
    canvas.line(data.title, left, 160f, text(62f, AccentColor, bold = true), width)
    canvas.line(data.period, left, 214f, text(34f, MutedColor), width)

    // Плитки 3×2.
    val gap = 24f
    val tileW = (width - 2 * gap) / 3
    val tileH = 150f
    val value = text(58f, TextColor, bold = true)
    val label = text(27f, MutedColor)
    data.tiles.take(6).forEachIndexed { i, tile ->
        val x = left + (i % 3) * (tileW + gap)
        val y = 262f + (i / 3) * (tileH + gap)
        fill.color = TileColor
        canvas.drawRoundRect(RectF(x, y, x + tileW, y + tileH), 28f, 28f, fill)
        canvas.line(tile.value, x + 26f, y + 76f, value, tileW - 40f)
        canvas.fitLine(tile.label, x + 26f, y + 120f, label, tileW - 44f, minSize = 21f)
    }

    // Мышцы за неделю.
    val section = text(36f, TextColor, bold = true)
    canvas.line(data.musclesTitle, left, 660f, section, width)
    val body = renderBodyBitmap(
        colorOf = { m -> volumeColor(volumeLevel(data.volume[m] ?: 0), Color(IdleMuscle)).toArgb() },
        bodyColor = BodyColor,
        gapColor = CardColor,
        height = 400,
    )
    canvas.drawBitmap(body, left - 10f, 688f, null)

    // Справа — самые нагруженные мышцы недели и легенда.
    val colX = left + body.width + 30f
    val colW = left + width - colX
    val muscleName = text(30f, TextColor)
    val muscleSets = text(30f, MutedColor, bold = true)
    data.volume.filterValues { it > 0 }.entries.sortedByDescending { it.value }.take(6).forEachIndexed { i, (m, sets) ->
        val y = 736f + i * 50f
        fill.color = volumeColor(volumeLevel(sets), Color(IdleMuscle)).toArgb()
        canvas.drawCircle(colX + 10f, y - 10f, 10f, fill)
        val count = data.setsFormat.format(sets)
        val countW = muscleSets.measureText(count)
        canvas.drawText(count, colX + colW - countW, y, muscleSets)
        canvas.line(data.muscleNames[m].orEmpty(), colX + 32f, y, muscleName, colW - countW - 48f)
    }
    val legend = text(26f, MutedColor)
    data.volumeLegend.forEachIndexed { i, (level, caption) ->
        val x = colX + (i % 2) * (colW / 2)
        val y = 1050f + (i / 2) * 40f
        fill.color = volumeColor(level, Color(IdleMuscle)).toArgb()
        canvas.drawCircle(x + 10f, y - 9f, 10f, fill)
        canvas.line(caption, x + 28f, y, legend, colW / 2 - 32f)
    }

    // Рекорды недели.
    canvas.line(data.recordsTitle, left, 1160f, section, width)
    val name = text(30f, TextColor, bold = true)
    val detail = text(30f, MutedColor)
    val gain = text(30f, GainColor, bold = true)
    if (data.records.isEmpty()) {
        canvas.line(data.noRecords, left, 1206f, detail, width)
    }
    data.records.take(3).forEachIndexed { i, r ->
        val y = 1206f + i * 42f
        val gainWidth = gain.measureText(r.gain)
        canvas.drawText(r.gain, left + width - gainWidth, y, gain)
        val nameText = ellipsize(r.exercise, name, width * 0.5f)
        canvas.drawText(nameText, left, y, name)
        val nameWidth = name.measureText(nameText) + 16f
        canvas.line(r.set, left + nameWidth, y, detail, width - nameWidth - gainWidth - 16f)
    }

    canvas.line(data.footer, left, SHARE_HEIGHT - 20f, text(24f, MutedColor), width)
    return bitmap
}

/** Сохраняет картинку во временную папку и открывает системное меню «Поделиться». */
fun shareImage(context: Context, bitmap: Bitmap, text: String, chooserTitle: String) {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "sonniktopower.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, text)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
