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
import android.text.TextUtils
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

private fun Canvas.line(text: String, x: Float, y: Float, paint: TextPaint, maxWidth: Float) {
    drawText(TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.END).toString(), x, y, paint)
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
        canvas.line(tile.label, x + 26f, y + 120f, label, tileW - 40f)
    }

    // Мышцы за неделю.
    val section = text(36f, TextColor, bold = true)
    canvas.line(data.musclesTitle, left, 660f, section, width)
    val body = renderBodyBitmap(
        colorOf = { m -> volumeColor(volumeLevel(data.volume[m] ?: 0), Color(IdleMuscle)).toArgb() },
        bodyColor = BodyColor,
        gapColor = CardColor,
        height = 360,
    )
    canvas.drawBitmap(body, (SHARE_WIDTH - body.width) / 2f, 690f, null)
    val legend = text(26f, MutedColor)
    var x = left
    data.volumeLegend.forEach { (level, caption) ->
        fill.color = volumeColor(level, Color(IdleMuscle)).toArgb()
        canvas.drawCircle(x + 10f, 1076f, 10f, fill)
        canvas.drawText(caption, x + 28f, 1085f, legend)
        x += 28f + legend.measureText(caption) + 32f
    }

    // Рекорды недели.
    canvas.line(data.recordsTitle, left, 1140f, section, width)
    val name = text(30f, TextColor, bold = true)
    val detail = text(30f, MutedColor)
    val gain = text(30f, GainColor, bold = true)
    if (data.records.isEmpty()) {
        canvas.line(data.noRecords, left, 1186f, detail, width)
    }
    data.records.take(3).forEachIndexed { i, r ->
        val y = 1186f + i * 44f
        val gainWidth = gain.measureText(r.gain)
        canvas.drawText(r.gain, left + width - gainWidth, y, gain)
        val nameText = TextUtils.ellipsize(r.exercise, name, width * 0.45f, TextUtils.TruncateAt.END).toString()
        canvas.drawText(nameText, left, y, name)
        val nameWidth = name.measureText(nameText) + 16f
        canvas.line(r.set, left + nameWidth, y, detail, width - nameWidth - gainWidth - 16f)
    }

    canvas.line(data.footer, left, SHARE_HEIGHT - 26f, text(24f, MutedColor), width)
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
