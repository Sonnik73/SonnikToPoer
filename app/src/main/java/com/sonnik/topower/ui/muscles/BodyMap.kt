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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.sonnik.topower.domain.Muscle
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/*
 * Анатомическая схема тела на холсте 100×200. Фигура строится из левой половины, правая — зеркальная.
 * Контуры — замкнутые сплайны через опорные точки; мышца может состоять из нескольких частей
 * (кубики пресса, головки квадрицепса и икр). Объём передают лёгкий градиент и штриховка волокон.
 */

private typealias Pt = Pair<Float, Float>

private fun pt(x: Number, y: Number): Pt = x.toFloat() to y.toFloat()

private fun mirror(points: List<Pt>): List<Pt> = points.map { (x, y) -> (100f - x) to y }

/** Натяжение сплайна Катмулла–Рома: 0 — ломаная, 0.5 — плавно. */
private const val TENSION = 0.5f

/** Замкнутый сплайн Катмулла–Рома через все точки (кубические кривые Безье). */
private fun Path.spline(points: List<Pt>) {
    val n = points.size
    moveTo(points[0].first, points[0].second)
    for (i in 0 until n) {
        val p0 = points[(i - 1 + n) % n]
        val p1 = points[i]
        val p2 = points[(i + 1) % n]
        val p3 = points[(i + 2) % n]
        cubicTo(
            p1.first + (p2.first - p0.first) * TENSION / 3, p1.second + (p2.second - p0.second) * TENSION / 3,
            p2.first - (p3.first - p1.first) * TENSION / 3, p2.second - (p3.second - p1.second) * TENSION / 3,
            p2.first, p2.second,
        )
    }
    close()
}

private fun Path.oval(cx: Float, cy: Float, rx: Float, ry: Float) {
    addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry))
}

/** Часть мышцы: контур и штриховка волокон под углом (рисуется внутри контура). */
private class MuscleShape(val path: Path, val fibers: Path, val bounds: Rect)

/** Шаг штриховки волокон в единицах схемы. */
private const val FIBER_STEP = 2.2f

private fun shape(points: List<Pt>, fiberDeg: Float): MuscleShape {
    val path = Path().apply { spline(points) }
    val bounds = path.getBounds()
    val rad = Math.toRadians(fiberDeg.toDouble())
    val dx = cos(rad).toFloat()
    val dy = sin(rad).toFloat()
    val c = bounds.center
    val r = max(bounds.width, bounds.height)
    val fibers = Path()
    var k = -r
    while (k <= r) {
        val ox = c.x - dy * k
        val oy = c.y + dx * k
        fibers.moveTo(ox - dx * r, oy - dy * r)
        fibers.lineTo(ox + dx * r, oy + dy * r)
        k += FIBER_STEP
    }
    return MuscleShape(path, fibers, bounds)
}

private class MuscleMapBuilder {
    val map = linkedMapOf<Muscle, MutableList<MuscleShape>>()

    /** Левая часть и её зеркальное отражение (волокна тоже отражаются). */
    fun sym(muscle: Muscle, fiberDeg: Float, points: List<Pt>) {
        map.getOrPut(muscle) { mutableListOf() } += listOf(shape(points, fiberDeg), shape(mirror(points), 180f - fiberDeg))
    }

    fun center(muscle: Muscle, fiberDeg: Float, points: List<Pt>) {
        map.getOrPut(muscle) { mutableListOf() } += shape(points, fiberDeg)
    }
}

private fun muscleMap(block: MuscleMapBuilder.() -> Unit): Map<Muscle, List<MuscleShape>> =
    MuscleMapBuilder().apply(block).map

private fun silhouette(): Path = Path().apply {
    oval(50f, 12.5f, 7.5f, 9.5f)
    spline(listOf(pt(45.5, 19), pt(54.5, 19), pt(55.5, 28), pt(44.5, 28)))
    val torsoLeft = listOf(pt(50, 26), pt(44, 26.5), pt(37, 28.5), pt(31, 31), pt(29, 37), pt(31, 46), pt(34, 56), pt(35.5, 68), pt(34, 80), pt(33, 92), pt(35, 104), pt(50, 108))
    spline(torsoLeft + mirror(torsoLeft).reversed().drop(1).dropLast(1))
    listOf(
        listOf(pt(31, 30), pt(26, 32), pt(23, 38), pt(22, 48), pt(21, 60), pt(26, 63), pt(29, 54), pt(31, 44), pt(33, 36)),
        listOf(pt(21, 59), pt(19, 70), pt(17, 82), pt(16, 92), pt(20, 94), pt(23, 84), pt(26, 71), pt(27, 62)),
        listOf(pt(35, 100), pt(33, 116), pt(34.5, 134), pt(37, 148), pt(46, 150), pt(48.5, 132), pt(49.5, 114), pt(50, 106)),
        listOf(pt(36.5, 146), pt(35, 160), pt(36.5, 176), pt(38.5, 190), pt(44.5, 190), pt(45.5, 174), pt(46.5, 160), pt(46, 148)),
    ).forEach { spline(it); spline(mirror(it)) }
    oval(18f, 99f, 3.6f, 5.5f)
    oval(82f, 99f, 3.6f, 5.5f)
    oval(41.5f, 194f, 6f, 3f)
    oval(58.5f, 194f, 6f, 3f)
}

private fun frontMuscles(): Map<Muscle, List<MuscleShape>> = muscleMap {
    sym(Muscle.TRAPS, 160f, listOf(pt(45, 25.5), pt(40, 28.2), pt(34.5, 30.2), pt(40, 30.8), pt(44.5, 29.5)))
    sym(Muscle.SIDE_DELTS, 100f, listOf(pt(30, 30.5), pt(25.8, 32.2), pt(23.4, 37.5), pt(23.8, 44), pt(26.5, 43), pt(27.3, 36.5)))
    sym(Muscle.FRONT_DELTS, 105f, listOf(pt(34, 30), pt(29.5, 31), pt(27.6, 35.5), pt(27.8, 41.5), pt(30, 45), pt(33, 41), pt(34.5, 35.5)))
    sym(Muscle.CHEST, 20f, listOf(pt(49.2, 32), pt(44, 30.8), pt(38.5, 31.2), pt(34.8, 34.5), pt(33.6, 40), pt(35.5, 45.5), pt(40, 49), pt(45, 50), pt(49.2, 49)))
    sym(Muscle.BICEPS, 95f, listOf(pt(30, 45.5), pt(26, 46.5), pt(23.2, 51), pt(22.8, 57), pt(24.5, 62), pt(27.8, 61), pt(29.6, 55), pt(30.6, 49)))
    sym(Muscle.FOREARMS, 100f, listOf(pt(26.5, 63.5), pt(22, 64.8), pt(19.3, 72), pt(17.6, 82), pt(17.2, 90), pt(20, 90.8), pt(22.4, 82), pt(25, 73), pt(27, 67.5)))
    sym(Muscle.OBLIQUES, 55f, listOf(pt(42.8, 52.5), pt(38.5, 51), pt(35.5, 54.5), pt(35.2, 64), pt(36, 76), pt(36.5, 86), pt(39, 92), pt(42.8, 93.5), pt(43.3, 80), pt(43.2, 64)))
    sym(Muscle.ABS, 0f, listOf(pt(49.3, 51.5), pt(45.2, 51.6), pt(44, 54), pt(44, 58), pt(45, 59.6), pt(49.3, 59.6)))
    sym(Muscle.ABS, 0f, listOf(pt(49.3, 61.2), pt(45, 61.2), pt(43.9, 63.5), pt(43.9, 68), pt(45, 69.6), pt(49.3, 69.6)))
    sym(Muscle.ABS, 0f, listOf(pt(49.3, 71.2), pt(45, 71.2), pt(43.9, 73.5), pt(43.9, 78), pt(45, 79.6), pt(49.3, 79.6)))
    sym(Muscle.ABS, 90f, listOf(pt(49.3, 81.2), pt(45, 81.2), pt(44, 84), pt(44.6, 90), pt(46.8, 95), pt(49.3, 97)))
    sym(Muscle.QUADS, 85f, listOf(pt(36.5, 104), pt(34, 114), pt(33.8, 126), pt(35.5, 138), pt(38.5, 146), pt(40.5, 140), pt(40.8, 124), pt(39.8, 110)))
    sym(Muscle.QUADS, 90f, listOf(pt(42.5, 104), pt(40.6, 113), pt(40.8, 127), pt(42, 139), pt(44.5, 143), pt(46.4, 130), pt(46.6, 115), pt(45.5, 107)))
    sym(Muscle.QUADS, 70f, listOf(pt(46.8, 128), pt(44.8, 135), pt(43.8, 142), pt(45, 148.5), pt(48, 146.5), pt(48.8, 137)))
    sym(Muscle.ADDUCTORS, 70f, listOf(pt(49.4, 104.5), pt(47.2, 108), pt(46.8, 116), pt(47, 125), pt(48.6, 123), pt(49.7, 113)))
    sym(Muscle.CALVES, 95f, listOf(pt(37.5, 152), pt(35.6, 160), pt(36.2, 170), pt(38.6, 177), pt(40.6, 168), pt(40.2, 157)))
    sym(Muscle.CALVES, 85f, listOf(pt(45.3, 152), pt(44, 159), pt(44.2, 169), pt(45.4, 174), pt(46.6, 164), pt(46.5, 156)))
}

private fun backMuscles(): Map<Muscle, List<MuscleShape>> = muscleMap {
    center(Muscle.TRAPS, 90f, listOf(pt(50, 21), pt(46.5, 24.5), pt(41, 28), pt(34.5, 30.5), pt(39, 33), pt(44, 38), pt(46.5, 46), pt(50, 57), pt(53.5, 46), pt(56, 38), pt(61, 33), pt(65.5, 30.5), pt(59, 28), pt(53.5, 24.5)))
    sym(Muscle.SIDE_DELTS, 100f, listOf(pt(30, 30.5), pt(25.8, 32.2), pt(23.4, 37.5), pt(23.8, 44), pt(26.5, 43), pt(27.3, 36.5)))
    sym(Muscle.REAR_DELTS, 70f, listOf(pt(34.5, 31), pt(29.8, 32), pt(27.8, 36.5), pt(28.4, 41.8), pt(31.5, 41), pt(34.6, 36.5)))
    sym(Muscle.TRICEPS, 92f, listOf(pt(30.6, 43), pt(26, 44), pt(23, 49.5), pt(22.6, 56), pt(24.4, 62), pt(28, 61), pt(29.6, 55), pt(30.8, 48)))
    sym(Muscle.FOREARMS, 100f, listOf(pt(26.5, 63.5), pt(22, 64.8), pt(19.3, 72), pt(17.6, 82), pt(17.2, 90), pt(20, 90.8), pt(22.4, 82), pt(25, 73), pt(27, 67.5)))
    sym(Muscle.LATS, 60f, listOf(pt(46.6, 41), pt(41, 36.5), pt(35, 35.2), pt(32, 40.5), pt(32.5, 50), pt(34.5, 60), pt(37.5, 70), pt(41.2, 79), pt(44.8, 85), pt(45, 72), pt(45.6, 58), pt(46.4, 48)))
    sym(Muscle.LOWER_BACK, 90f, listOf(pt(49.3, 58), pt(47, 60), pt(46.3, 70), pt(46.1, 82), pt(46.6, 92), pt(49.3, 96)))
    sym(Muscle.GLUTES, 30f, listOf(pt(49.2, 97.5), pt(43.5, 96.5), pt(38, 98.5), pt(34.8, 104), pt(35, 112), pt(38.5, 118), pt(44, 120.5), pt(49.2, 118.5)))
    sym(Muscle.HAMSTRINGS, 95f, listOf(pt(37, 121.5), pt(34.5, 129), pt(34.8, 139), pt(37.5, 147), pt(41, 148.5), pt(42, 136), pt(41.5, 126), pt(40, 121)))
    sym(Muscle.HAMSTRINGS, 85f, listOf(pt(43.4, 121.5), pt(42.6, 129), pt(42.8, 141), pt(44.3, 148.5), pt(47, 146.5), pt(48.3, 134), pt(48.2, 124.5), pt(46.2, 121)))
    sym(Muscle.CALVES, 95f, listOf(pt(39.5, 150.5), pt(36.2, 155), pt(35.6, 164), pt(37.6, 173), pt(40.6, 176), pt(41.6, 164), pt(41.2, 154)))
    sym(Muscle.CALVES, 85f, listOf(pt(43.4, 150.5), pt(42.2, 155), pt(42.4, 166), pt(43.8, 176.5), pt(46.6, 172), pt(47, 161), pt(46.2, 153)))
}

/** Мышцы, видимые на схеме спереди и сзади. */
val FRONT_MUSCLES: Set<Muscle> get() = frontMuscles().keys
val BACK_MUSCLES: Set<Muscle> get() = backMuscles().keys

/** Мышца под точкой ([x], [y]) в координатах схемы 100×200 или null. Верхние слои проверяются первыми. */
fun muscleAt(back: Boolean, x: Float, y: Float): Muscle? {
    // Region работает с целыми координатами — увеличиваем схему в 10 раз для точности.
    val zoom = Matrix().apply { setScale(10f, 10f) }
    val clip = Region(0, 0, 1000, 2000)
    val muscles = if (back) backMuscles() else frontMuscles()
    return muscles.entries.reversed().firstOrNull { (_, shapes) ->
        shapes.any { s ->
            val android = android.graphics.Path(s.path.asAndroidPath()).apply { transform(zoom) }
            Region().apply { setPath(android, clip) }.contains((x * 10).toInt(), (y * 10).toInt())
        }
    }?.key
}

/** Светлее сверху, темнее снизу — лёгкий объём поверх цвета мышцы. */
private val Highlight = Color.White.copy(alpha = 0.16f)
private val Shade = Color.Black.copy(alpha = 0.12f)
private val FiberColor = Color.Black.copy(alpha = 0.09f)
private const val FIBER_WIDTH = 0.28f
private const val GAP_WIDTH = 0.7f
private const val OUTLINE_WIDTH = 1.1f
private const val HIGHLIGHT_WIDTH = 1.2f

/**
 * Тело спереди ([back] = false) или сзади с мышцами, окрашенными через [colorOf].
 * [bodyColor] — силуэт, [gapColor] — тонкие промежутки между мышцами (цвет фона карточки).
 * [selected] обводится [selectedColor]; [onTap] — нажатие на мышцу.
 */
@Composable
fun BodyFigure(
    back: Boolean,
    colorOf: (Muscle) -> Color,
    bodyColor: Color,
    gapColor: Color,
    modifier: Modifier = Modifier,
    selected: Muscle? = null,
    selectedColor: Color = Color.Black,
    onTap: ((Muscle) -> Unit)? = null,
) {
    val body = remember { silhouette() }
    val muscles = remember(back) { if (back) backMuscles() else frontMuscles() }
    val outline = lerp(bodyColor, Color.Black, 0.1f)
    val tap = if (onTap == null) Modifier else Modifier.pointerInput(back, onTap) {
        detectTapGestures { offset ->
            val scale = size.width / 100f
            muscleAt(back, offset.x / scale, offset.y / scale)?.let(onTap)
        }
    }
    Canvas(modifier.aspectRatio(0.5f).then(tap)) {
        val scale = size.width / 100f
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            // Контур: обводка под заливкой видна только снаружи силуэта.
            drawPath(body, outline, style = Stroke(width = OUTLINE_WIDTH))
            drawPath(body, bodyColor)
            muscles.forEach { (muscle, shapes) ->
                val color = colorOf(muscle)
                shapes.forEach { s ->
                    drawPath(s.path, color)
                    clipPath(s.path) {
                        drawRect(
                            Brush.verticalGradient(listOf(Highlight, Color.Transparent, Shade), s.bounds.top, s.bounds.bottom),
                            topLeft = s.bounds.topLeft,
                            size = s.bounds.size,
                        )
                        drawPath(s.fibers, FiberColor, style = Stroke(width = FIBER_WIDTH))
                    }
                    drawPath(s.path, gapColor, style = Stroke(width = GAP_WIDTH))
                }
            }
            selected?.let { m ->
                muscles[m]?.forEach { drawPath(it.path, selectedColor, style = Stroke(width = HIGHLIGHT_WIDTH)) }
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
    val bitmap = android.graphics.Bitmap.createBitmap(((200 + gap) * scale).toInt(), height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    fun stroke(color: Int, width: Float) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = width
        this.color = color
    }
    val gapPaint = stroke(gapColor, GAP_WIDTH)
    val fiberPaint = stroke(FiberColor.toArgb(), FIBER_WIDTH)
    val body = silhouette().asAndroidPath()
    listOf(false, true).forEachIndexed { i, back ->
        canvas.save()
        canvas.translate(i * (100 + gap) * scale, 0f)
        canvas.scale(scale, scale)
        canvas.drawPath(body, stroke(lerp(Color(bodyColor), Color.Black, 0.1f).toArgb(), OUTLINE_WIDTH))
        fill.color = bodyColor
        canvas.drawPath(body, fill)
        (if (back) backMuscles() else frontMuscles()).forEach { (muscle, shapes) ->
            shapes.forEach { s ->
                val p = s.path.asAndroidPath()
                fill.shader = null
                fill.color = colorOf(muscle)
                canvas.drawPath(p, fill)
                canvas.save()
                canvas.clipPath(p)
                fill.shader = android.graphics.LinearGradient(
                    0f, s.bounds.top, 0f, s.bounds.bottom,
                    intArrayOf(Highlight.toArgb(), 0, Shade.toArgb()), null,
                    android.graphics.Shader.TileMode.CLAMP,
                )
                canvas.drawRect(s.bounds.left, s.bounds.top, s.bounds.right, s.bounds.bottom, fill)
                canvas.drawPath(s.fibers.asAndroidPath(), fiberPaint)
                canvas.restore()
                canvas.drawPath(p, gapPaint)
            }
        }
        canvas.restore()
    }
    return bitmap
}
