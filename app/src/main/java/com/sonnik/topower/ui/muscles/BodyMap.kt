package com.sonnik.topower.ui.muscles

import android.graphics.Matrix
import android.graphics.Region
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import com.sonnik.topower.domain.Muscle

/*
 * Схематичное тело на холсте 100×200. Фигура строится из левой половины, правая — зеркальная.
 * Мышцы — сглаженные многоугольники и овалы поверх серого силуэта.
 */

private typealias Pt = Pair<Float, Float>

private fun pt(x: Number, y: Number): Pt = x.toFloat() to y.toFloat()

private fun mirror(points: List<Pt>): List<Pt> = points.map { (x, y) -> (100f - x) to y }

/** Замкнутая сглаженная фигура через точки: квадратичные кривые между серединами сторон. */
private fun Path.smooth(points: List<Pt>) {
    fun mid(a: Pt, b: Pt) = Offset((a.first + b.first) / 2, (a.second + b.second) / 2)
    val n = points.size
    val start = mid(points[n - 1], points[0])
    moveTo(start.x, start.y)
    for (i in 0 until n) {
        val p = points[i]
        val m = mid(p, points[(i + 1) % n])
        quadraticTo(p.first, p.second, m.x, m.y)
    }
    close()
}

private fun Path.oval(cx: Float, cy: Float, rx: Float, ry: Float) {
    addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry))
}

/** Фигура с левой и зеркальной правой частью. */
private fun symmetric(points: List<Pt>) = Path().apply {
    smooth(points)
    smooth(mirror(points))
}

private fun symmetricOval(cx: Float, cy: Float, rx: Float, ry: Float) = Path().apply {
    oval(cx, cy, rx, ry)
    oval(100f - cx, cy, rx, ry)
}

private fun center(points: List<Pt>) = Path().apply { smooth(points) }

private fun silhouette(): Path = Path().apply {
    oval(50f, 13f, 8f, 10f)
    smooth(listOf(pt(45, 20), pt(55, 20), pt(56, 30), pt(44, 30)))
    val torsoLeft = listOf(pt(50, 27), pt(40, 28), pt(32, 31), pt(30, 40), pt(34, 52), pt(35, 70), pt(33, 90), pt(35, 104), pt(50, 108))
    smooth(torsoLeft + mirror(torsoLeft).reversed().drop(1).dropLast(1))
    listOf(
        listOf(pt(31, 31), pt(25, 34), pt(22, 48), pt(21, 62), pt(26, 64), pt(30, 52), pt(33, 40)),
        listOf(pt(21, 60), pt(18, 76), pt(16, 92), pt(20, 94), pt(24, 80), pt(27, 63)),
        listOf(pt(35, 100), pt(33, 120), pt(36, 148), pt(46, 150), pt(49, 120), pt(50, 106)),
        listOf(pt(36, 146), pt(35, 165), pt(38, 190), pt(45, 190), pt(46, 165), pt(46, 148)),
    ).forEach { smooth(it); smooth(mirror(it)) }
    oval(18f, 99f, 3.5f, 5f)
    oval(82f, 99f, 3.5f, 5f)
    oval(41f, 194f, 6f, 3f)
    oval(59f, 194f, 6f, 3f)
}

private val forearms = listOf(pt(25, 64), pt(21, 66), pt(18, 80), pt(17, 90), pt(20, 91), pt(23, 80), pt(26, 68))

private fun frontMuscles(): Map<Muscle, Path> = linkedMapOf(
    Muscle.TRAPS to symmetric(listOf(pt(45, 26), pt(38, 30), pt(44, 31))),
    Muscle.SIDE_DELTS to symmetricOval(26.5f, 37f, 3.5f, 6f),
    Muscle.FRONT_DELTS to symmetricOval(31f, 36f, 4.5f, 5.5f),
    Muscle.CHEST to symmetric(listOf(pt(49, 33), pt(39, 32), pt(34, 38), pt(35, 47), pt(42, 51), pt(49, 50))),
    Muscle.BICEPS to symmetric(listOf(pt(29, 42), pt(25, 44), pt(23, 55), pt(25, 61), pt(28, 58), pt(30, 48))),
    Muscle.FOREARMS to symmetric(forearms),
    Muscle.OBLIQUES to symmetric(listOf(pt(44, 55), pt(37, 53), pt(36, 70), pt(37, 86), pt(44, 92))),
    Muscle.ABS to center(listOf(pt(46, 53), pt(54, 53), pt(55, 92), pt(45, 92))),
    Muscle.QUADS to symmetric(listOf(pt(37, 108), pt(35, 122), pt(37, 142), pt(43, 146), pt(47, 132), pt(46, 112))),
    Muscle.ADDUCTORS to symmetric(listOf(pt(48.5, 110), pt(46.5, 114), pt(46.5, 130), pt(49, 124))),
    Muscle.CALVES to symmetric(listOf(pt(37, 154), pt(36, 166), pt(39, 180), pt(42, 168), pt(41, 155))),
)

private fun backMuscles(): Map<Muscle, Path> = linkedMapOf(
    Muscle.LATS to symmetric(listOf(pt(44, 44), pt(35, 42), pt(34, 52), pt(37, 70), pt(44, 80), pt(47, 62))),
    Muscle.TRAPS to center(listOf(pt(50, 24), pt(40, 30), pt(46, 42), pt(50, 54), pt(54, 42), pt(60, 30))),
    Muscle.SIDE_DELTS to symmetricOval(26.5f, 37f, 3.5f, 6f),
    Muscle.REAR_DELTS to symmetricOval(31f, 37f, 4.5f, 5f),
    Muscle.LOWER_BACK to center(listOf(pt(46, 66), pt(54, 66), pt(55, 94), pt(45, 94))),
    Muscle.TRICEPS to symmetric(listOf(pt(29, 41), pt(24, 43), pt(22, 54), pt(24, 61), pt(28, 58), pt(30, 48))),
    Muscle.FOREARMS to symmetric(forearms),
    Muscle.GLUTES to symmetric(listOf(pt(49, 98), pt(40, 97), pt(35, 104), pt(37, 116), pt(46, 118), pt(49, 112))),
    Muscle.HAMSTRINGS to symmetric(listOf(pt(37, 120), pt(36, 134), pt(38, 146), pt(44, 148), pt(47, 134), pt(47, 120))),
    Muscle.CALVES to symmetric(listOf(pt(37, 152), pt(35, 164), pt(38, 178), pt(43, 178), pt(46, 164), pt(45, 152))),
)

/** Мышцы, видимые на схеме спереди и сзади. */
val FRONT_MUSCLES: Set<Muscle> get() = frontMuscles().keys
val BACK_MUSCLES: Set<Muscle> get() = backMuscles().keys

/** Мышца под точкой ([x], [y]) в координатах схемы 100×200 или null. Верхние слои проверяются первыми. */
fun muscleAt(back: Boolean, x: Float, y: Float): Muscle? {
    // Region работает с целыми координатами — увеличиваем схему в 10 раз для точности.
    val zoom = Matrix().apply { setScale(10f, 10f) }
    val clip = Region(0, 0, 1000, 2000)
    val muscles = if (back) backMuscles() else frontMuscles()
    return muscles.entries.reversed().firstOrNull { (_, path) ->
        val android = android.graphics.Path(path.asAndroidPath()).apply { transform(zoom) }
        Region().apply { setPath(android, clip) }.contains((x * 10).toInt(), (y * 10).toInt())
    }?.key
}

/**
 * Тело спереди ([back] = false) или сзади с мышцами, окрашенными через [colorOf].
 * [bodyColor] — силуэт, [gapColor] — тонкие промежутки между мышцами (цвет фона карточки).
 * [onTap] — нажатие на мышцу.
 */
@Composable
fun BodyFigure(
    back: Boolean,
    colorOf: (Muscle) -> Color,
    bodyColor: Color,
    gapColor: Color,
    modifier: Modifier = Modifier,
    onTap: ((Muscle) -> Unit)? = null,
) {
    val body = remember { silhouette() }
    val muscles = remember(back) { if (back) backMuscles() else frontMuscles() }
    val tap = if (onTap == null) Modifier else Modifier.pointerInput(back, onTap) {
        detectTapGestures { offset ->
            val scale = size.width / 100f
            muscleAt(back, offset.x / scale, offset.y / scale)?.let(onTap)
        }
    }
    Canvas(modifier.aspectRatio(0.5f).then(tap)) {
        val scale = size.width / 100f
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            drawPath(body, bodyColor)
            muscles.forEach { (muscle, path) ->
                drawPath(path, colorOf(muscle))
                drawPath(path, gapColor, style = Stroke(width = 0.8f))
            }
        }
    }
}

/**
 * Тело спереди и сзади рядом одной картинкой — для виджета (там нет Compose).
 * [colorOf] — цвет мышцы (ARGB), [height] — высота картинки в пикселях.
 */
fun renderBodyBitmap(colorOf: (Muscle) -> Int, bodyColor: Int, gapColor: Int, height: Int): android.graphics.Bitmap {
    val scale = height / 200f
    val gap = 12f
    val bitmap = android.graphics.Bitmap.createBitmap((200 * scale + gap * scale).toInt(), height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val stroke = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 0.8f
        color = gapColor
    }
    val body = silhouette().asAndroidPath()
    listOf(false, true).forEachIndexed { i, back ->
        canvas.save()
        canvas.translate(i * (100 + gap) * scale, 0f)
        canvas.scale(scale, scale)
        fill.color = bodyColor
        canvas.drawPath(body, fill)
        (if (back) backMuscles() else frontMuscles()).forEach { (muscle, path) ->
            val p = path.asAndroidPath()
            fill.color = colorOf(muscle)
            canvas.drawPath(p, fill)
            canvas.drawPath(p, stroke)
        }
        canvas.restore()
    }
    return bitmap
}
