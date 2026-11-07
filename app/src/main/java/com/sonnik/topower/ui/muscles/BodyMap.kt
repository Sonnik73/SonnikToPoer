package com.sonnik.topower.ui.muscles

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Region
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.sonnik.topower.data.MeasurementPart
import com.sonnik.topower.domain.Muscle
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import android.graphics.Path as APath

/*
 * Анатомическая схема тела на холсте 100×200. Фигура строится из левой половины, правая — зеркальная.
 * Контуры — замкнутые сплайны Катмулла–Рома через опорные точки; мышца может состоять из нескольких
 * частей (кубики пресса, головки квадрицепса и икр). Поверх цвета — лёгкий объём (градиент), волокна
 * по форме мышцы (веером к прикреплению или дугами вдоль оси), сухожилия и вены.
 * Один рисовальщик на android.graphics.Canvas — и для экрана, и для виджета.
 */

private typealias Pt = PointF

/*
 * Женская фигура строится из тех же точек, пересчитанных по пропорциям: уже плечи и талия, шире бёдра.
 * Пересчёт зависит только от высоты и расстояния до оси, поэтому зеркальные части остаются зеркальными.
 */
private var buildingFemale = false

/** Все фигуры строятся под одной блокировкой: флаг [buildingFemale] общий. */
private val FIGURE_LOCK = Any()

/** Ширина женской фигуры относительно мужской на высоте y (опорные точки, между ними — линейно). */
private val FEMALE_WIDTH = listOf(
    0f to 0.93f, 18f to 0.93f, 22f to 0.86f, 28f to 0.86f, 44f to 0.88f, 56f to 0.86f, 66f to 0.8f, 76f to 0.8f,
    88f to 0.96f, 100f to 1.12f, 108f to 1.12f, 120f to 1.07f, 140f to 1.01f, 160f to 0.95f, 200f to 0.92f,
)

private fun femaleWidth(x: Float, y: Float): Float {
    // Руки от плеча до кисти сужаются вместе с плечами и не меняют форму.
    if (kotlin.math.abs(x - 50f) > 19.5f && y in 28f..102f) return 0.86f
    val i = FEMALE_WIDTH.indexOfFirst { it.first >= y }.coerceAtLeast(1)
    val (y0, k0) = FEMALE_WIDTH[i - 1]
    val (y1, k1) = FEMALE_WIDTH[i]
    return k0 + (k1 - k0) * ((y - y0) / (y1 - y0)).coerceIn(0f, 1f)
}

private fun pt(x: Number, y: Number): PointF {
    val px = x.toFloat()
    val py = y.toFloat()
    return if (buildingFemale) PointF(50f + (px - 50f) * femaleWidth(px, py), py) else PointF(px, py)
}

private fun <T> female(block: () -> T): T {
    buildingFemale = true
    try {
        return block()
    } finally {
        buildingFemale = false
    }
}

private fun mirror(points: List<Pt>): List<Pt> = points.map { PointF(100f - it.x, it.y) }

/** Натяжение сплайна: 0 — ломаная, 0.5 — плавно. */
private const val TENSION = 0.5f
private const val SAMPLES_PER_SEGMENT = 8

/** Контрольные точки кубического участка p1→p2 сплайна Катмулла–Рома. */
private fun controls(p0: Pt, p1: Pt, p2: Pt, p3: Pt) = Pair(
    PointF(p1.x + (p2.x - p0.x) * TENSION / 3, p1.y + (p2.y - p0.y) * TENSION / 3),
    PointF(p2.x - (p3.x - p1.x) * TENSION / 3, p2.y - (p3.y - p1.y) * TENSION / 3),
)

/** Замкнутый сплайн через все точки. */
private fun APath.spline(points: List<Pt>) {
    val n = points.size
    moveTo(points[0].x, points[0].y)
    for (i in 0 until n) {
        val p1 = points[i]
        val p2 = points[(i + 1) % n]
        val (c1, c2) = controls(points[(i - 1 + n) % n], p1, p2, points[(i + 2) % n])
        cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    close()
}

/** Открытый сплайн (вены, сухожильные линии): крайние точки повторяются. */
private fun APath.openSpline(points: List<Pt>) {
    val p = listOf(points.first()) + points + listOf(points.last())
    moveTo(points[0].x, points[0].y)
    for (i in 1 until p.size - 2) {
        val (c1, c2) = controls(p[i - 1], p[i], p[i + 1], p[i + 2])
        cubicTo(c1.x, c1.y, c2.x, c2.y, p[i + 1].x, p[i + 1].y)
    }
}

/** Точки замкнутого сплайна (те же кривые, что рисует [spline]) — для построения волокон. */
private fun splineSamples(points: List<Pt>): List<Pt> {
    val n = points.size
    val out = ArrayList<Pt>(n * SAMPLES_PER_SEGMENT)
    for (i in 0 until n) {
        val p1 = points[i]
        val p2 = points[(i + 1) % n]
        val (c1, c2) = controls(points[(i - 1 + n) % n], p1, p2, points[(i + 2) % n])
        for (s in 0 until SAMPLES_PER_SEGMENT) {
            val t = s.toFloat() / SAMPLES_PER_SEGMENT
            val u = 1 - t
            out += PointF(
                u * u * u * p1.x + 3 * u * u * t * c1.x + 3 * u * t * t * c2.x + t * t * t * p2.x,
                u * u * u * p1.y + 3 * u * u * t * c1.y + 3 * u * t * t * c2.y + t * t * t * p2.y,
            )
        }
    }
    return out
}

private fun APath.oval(cx: Float, cy: Float, rx: Float, ry: Float) {
    val c = pt(cx, cy)
    val sx = if (buildingFemale) femaleWidth(cx, cy) else 1f
    addOval(RectF(c.x - rx * sx, c.y - ry, c.x + rx * sx, c.y + ry), APath.Direction.CW)
}

/** Ломаная из [n] точек, равномерно распределённых по длине [chain]. */
private fun resample(chain: List<Pt>, n: Int): List<Pt> {
    if (chain.size < 2) return List(n) { chain.first() }
    val dist = FloatArray(chain.size)
    for (i in 1 until chain.size) dist[i] = dist[i - 1] + hypot(chain[i].x - chain[i - 1].x, chain[i].y - chain[i - 1].y)
    val total = dist.last()
    var j = 0
    return List(n) { i ->
        val d = total * i / (n - 1)
        while (j < chain.size - 2 && dist[j + 1] < d) j++
        val seg = (dist[j + 1] - dist[j]).coerceAtLeast(1e-6f)
        val k = ((d - dist[j]) / seg).coerceIn(0f, 1f)
        PointF(chain[j].x + (chain[j + 1].x - chain[j].x) * k, chain[j].y + (chain[j + 1].y - chain[j].y) * k)
    }
}

/** Шаг между волокнами в единицах схемы. */
private const val FIBER_STEP = 1.7f
private const val FIBER_POINTS = 24
private const val FAN_START = 0.22f

/**
 * Волокна вдоль оси под углом [deg]: контур делится на две стороны между крайними по оси точками,
 * волокна — промежуточные линии между сторонами, поэтому повторяют изгиб мышцы.
 */
private fun longFibers(outline: List<Pt>, deg: Float): APath {
    val rad = Math.toRadians(deg.toDouble())
    val ax = cos(rad).toFloat()
    val ay = sin(rad).toFloat()
    val proj = outline.map { it.x * ax + it.y * ay }
    val i0 = proj.indices.minBy { proj[it] }
    val i1 = proj.indices.maxBy { proj[it] }
    val n = outline.size
    val sideA = resample(List((i1 - i0 + n) % n + 1) { outline[(i0 + it) % n] }, FIBER_POINTS)
    val sideB = resample(List((i0 - i1 + n) % n + 1) { outline[(i0 - it + n) % n] }, FIBER_POINTS)
    val width = sideA.indices.maxOf { hypot(sideA[it].x - sideB[it].x, sideA[it].y - sideB[it].y) }
    val count = max(2, (width / FIBER_STEP).toInt())
    return APath().apply {
        for (f in 1 until count) {
            val t = f.toFloat() / count
            sideA.indices.forEach { k ->
                val x = sideA[k].x + (sideB[k].x - sideA[k].x) * t
                val y = sideA[k].y + (sideB[k].y - sideA[k].y) * t
                if (k == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
    }
}

/** Волокна веером из точки прикрепления [focus] к краю мышцы (грудь, дельты, широчайшие, ягодицы). */
private fun fanFibers(outline: List<Pt>, focus: Pt): APath {
    val length = outline.indices.sumOf { i ->
        val a = outline[i]
        val b = outline[(i + 1) % outline.size]
        hypot(a.x - b.x, a.y - b.y).toDouble()
    }.toFloat()
    val edge = resample(outline + outline.first(), max(8, (length / FIBER_STEP).toInt()))
    return APath().apply {
        // Линии начинаются чуть дальше точки схождения — иначе там сливаются в тёмное пятно.
        edge.forEach { p ->
            moveTo(focus.x + (p.x - focus.x) * FAN_START, focus.y + (p.y - focus.y) * FAN_START)
            lineTo(p.x, p.y)
        }
    }
}

/**
 * Часть мышцы: контур, волокна и объём. Свет падает сверху слева: блик смещён к нему,
 * к дальнему краю мышца темнеет, а по краю внутри идёт мягкая тень — мышца выглядит выпуклой.
 */
private class MuscleShape(val path: APath, val fibers: APath, val bounds: RectF) {
    val shade: Shader = RadialGradient(
        bounds.left + bounds.width() * 0.38f,
        bounds.top + bounds.height() * 0.3f,
        max(bounds.width(), bounds.height()) * 0.85f,
        intArrayOf(
            Color.White.copy(alpha = 0.26f).toArgb(),
            Color.White.copy(alpha = 0.04f).toArgb(),
            Color.Black.copy(alpha = 0.18f).toArgb(),
        ),
        floatArrayOf(0f, 0.45f, 1f),
        Shader.TileMode.CLAMP,
    )
}

private fun shape(points: List<Pt>, fiberDeg: Float, fan: Pt?): MuscleShape {
    val path = APath().apply { spline(points) }
    val bounds = RectF().also { path.computeBounds(it, true) }
    val outline = splineSamples(points)
    return MuscleShape(path, if (fan != null) fanFibers(outline, fan) else longFibers(outline, fiberDeg), bounds)
}

/** Фигура спереди или сзади: мышцы, светлые сухожилия и вены поверх. */
private class Figure(
    val muscles: Map<Muscle, List<MuscleShape>>,
    val tendons: List<APath>,
    val tendonLines: List<APath>,
    val veins: List<APath>,
    /** Тонкие тёмные линии поверх мышц: белая линия живота, ключицы, складки. */
    val creases: List<APath>,
)

private class FigureBuilder {
    val muscles = linkedMapOf<Muscle, MutableList<MuscleShape>>()
    val tendons = mutableListOf<APath>()
    val tendonLines = mutableListOf<APath>()
    val veins = mutableListOf<APath>()
    val creases = mutableListOf<APath>()

    /** Левая часть и её зеркальное отражение (волокна и точка прикрепления тоже отражаются). */
    fun sym(muscle: Muscle, fiberDeg: Float, points: List<Pt>, fan: Pt? = null) {
        muscles.getOrPut(muscle) { mutableListOf() } += listOf(
            shape(points, fiberDeg, fan),
            shape(mirror(points), 180f - fiberDeg, fan?.let { PointF(100f - it.x, it.y) }),
        )
    }

    fun center(muscle: Muscle, fiberDeg: Float, points: List<Pt>) {
        muscles.getOrPut(muscle) { mutableListOf() } += shape(points, fiberDeg, null)
    }

    fun tendon(points: List<Pt>) {
        tendons += APath().apply { spline(points) }
        tendons += APath().apply { spline(mirror(points)) }
    }

    fun tendonLine(points: List<Pt>) {
        tendonLines += APath().apply { openSpline(points) }
        tendonLines += APath().apply { openSpline(mirror(points)) }
    }

    fun vein(points: List<Pt>) {
        veins += APath().apply { openSpline(points) }
        veins += APath().apply { openSpline(mirror(points)) }
    }

    fun crease(points: List<Pt>, symmetric: Boolean = true) {
        creases += APath().apply { openSpline(points) }
        if (symmetric) creases += APath().apply { openSpline(mirror(points)) }
    }
}

private fun figure(block: FigureBuilder.() -> Unit): Figure =
    FigureBuilder().apply(block).let { Figure(it.muscles, it.tendons, it.tendonLines, it.veins, it.creases) }

/**
 * Части силуэта по отдельности: зеркальные части обходятся в обратную сторону, и в одном пути
 * их пересечения при заливке вычитались бы друг из друга.
 */
private fun silhouette(): List<APath> = buildList {
    fun part(block: APath.() -> Unit) = add(APath().apply(block))
    part { oval(50f, 12.5f, 7.5f, 9.5f) }
    part { spline(listOf(pt(45.5, 19), pt(54.5, 19), pt(55.5, 28), pt(44.5, 28))) }
    val torsoLeft = listOf(pt(50, 26), pt(44, 26.5), pt(37, 28.5), pt(31, 31), pt(29, 37), pt(31, 46), pt(34, 56), pt(35.5, 68), pt(34, 80), pt(33, 92), pt(35, 104), pt(50, 108))
    part { spline(torsoLeft + mirror(torsoLeft).reversed().drop(1).dropLast(1)) }
    listOf(
        listOf(pt(31, 30), pt(26, 32), pt(23, 38), pt(22, 48), pt(21, 60), pt(26, 63), pt(29, 54), pt(31, 44), pt(33, 36)),
        listOf(pt(21, 59), pt(19, 70), pt(17, 82), pt(16, 92), pt(20, 94), pt(23, 84), pt(26, 71), pt(27, 62)),
        listOf(pt(35, 100), pt(33, 116), pt(34.5, 134), pt(37, 148), pt(46, 150), pt(48.5, 132), pt(49.5, 114), pt(50, 106)),
        listOf(pt(36.5, 146), pt(35, 160), pt(36.5, 176), pt(38.5, 190), pt(44.5, 190), pt(45.5, 174), pt(46.5, 160), pt(46, 148)),
    ).forEach { part { spline(it) }; part { spline(mirror(it)) } }
    part { oval(18f, 99f, 3.6f, 5.5f) }
    part { oval(82f, 99f, 3.6f, 5.5f) }
    part { oval(41.5f, 194f, 6f, 3f) }
    part { oval(58.5f, 194f, 6f, 3f) }
}

private fun frontFigure() = figure {
    sym(Muscle.TRAPS, 160f, listOf(pt(45, 25.5), pt(40, 28.2), pt(34.5, 30.2), pt(40, 30.8), pt(44.5, 29.5)))
    sym(Muscle.SIDE_DELTS, 100f, listOf(pt(30, 30.5), pt(25.8, 32.2), pt(23.4, 37.5), pt(23.8, 44), pt(26.5, 43), pt(27.3, 36.5)), fan = pt(24.6, 43.6))
    sym(Muscle.FRONT_DELTS, 105f, listOf(pt(34, 30), pt(29.5, 31), pt(27.6, 35.5), pt(27.8, 41.5), pt(30, 45), pt(33, 41), pt(34.5, 35.5)), fan = pt(29.6, 44.5))
    if (buildingFemale) {
        // Женская грудь — круглее и ниже.
        sym(Muscle.CHEST, 20f, listOf(pt(49.4, 34), pt(44.5, 32.6), pt(39, 33.4), pt(35.2, 37.6), pt(34.2, 43.6), pt(35.8, 49.6), pt(39.8, 53.6), pt(44.8, 54.4), pt(48.4, 52.6), pt(49.4, 49)), fan = pt(34.6, 40))
    } else {
        sym(Muscle.CHEST, 20f, listOf(pt(49.2, 32), pt(44, 30.8), pt(38.5, 31.2), pt(34.8, 34.5), pt(33.6, 40), pt(35.5, 45.5), pt(40, 49), pt(45, 50), pt(49.2, 49)), fan = pt(33.8, 38.5))
    }
    sym(Muscle.BICEPS, 95f, listOf(pt(30, 45.5), pt(26.6, 46.4), pt(24.8, 50.5), pt(24.8, 56), pt(25.9, 60.8), pt(28.2, 60.4), pt(29.8, 55), pt(30.6, 49)))
    sym(Muscle.BRACHIALIS, 98f, listOf(pt(24.2, 49), pt(22.8, 52.5), pt(22.5, 57.5), pt(23.5, 62.2), pt(25.2, 61.8), pt(24.2, 57), pt(24.1, 52.5)))
    sym(Muscle.FOREARMS, 100f, listOf(pt(26.5, 63.5), pt(22, 64.8), pt(19.3, 72), pt(17.6, 82), pt(17.2, 90), pt(20, 90.8), pt(22.4, 82), pt(25, 73), pt(27, 67.5)))
    sym(Muscle.BRACHIORADIALIS, 105f, listOf(pt(24.2, 62.8), pt(21.4, 64.4), pt(19.4, 69.5), pt(18.6, 76.5), pt(19.8, 78.6), pt(21.8, 72.8), pt(24, 67.2), pt(25.4, 64.6)))
    sym(Muscle.OBLIQUES, 55f, listOf(pt(42.8, 52.5), pt(38.5, 51), pt(35.5, 54.5), pt(35.2, 64), pt(36, 76), pt(36.5, 86), pt(39, 92), pt(42.8, 93.5), pt(43.3, 80), pt(43.2, 64)))
    // У женской фигуры зубчатая ниже — под грудью.
    val serratusShift = if (buildingFemale) 3.2 else 0.0
    sym(Muscle.SERRATUS, 160f, listOf(pt(37.4, 45.2 + serratusShift), pt(34.8, 45.8 + serratusShift), pt(33.5, 47.6 + serratusShift), pt(34.6, 48.6 + serratusShift), pt(37.6, 47.8 + serratusShift)))
    sym(Muscle.SERRATUS, 160f, listOf(pt(37.8, 48.8 + serratusShift), pt(34.8, 49.4 + serratusShift), pt(33.7, 51.4 + serratusShift), pt(35, 52.2 + serratusShift), pt(38.1, 51.3 + serratusShift)))
    sym(Muscle.SERRATUS, 160f, listOf(pt(38.2, 52.4 + serratusShift), pt(35.2, 53 + serratusShift), pt(34.4, 55 + serratusShift), pt(35.8, 55.7 + serratusShift), pt(38.5, 54.7 + serratusShift)))
    sym(Muscle.ABS, 0f, listOf(pt(49.3, 51.5), pt(45.2, 51.6), pt(44, 54), pt(44, 58), pt(45, 59.6), pt(49.3, 59.6)))
    sym(Muscle.ABS, 0f, listOf(pt(49.3, 61.2), pt(45, 61.2), pt(43.9, 63.5), pt(43.9, 68), pt(45, 69.6), pt(49.3, 69.6)))
    sym(Muscle.ABS, 0f, listOf(pt(49.3, 71.2), pt(45, 71.2), pt(43.9, 73.5), pt(43.9, 78), pt(45, 79.6), pt(49.3, 79.6)))
    sym(Muscle.ABS, 90f, listOf(pt(49.3, 81.2), pt(45, 81.2), pt(44, 84), pt(44.6, 90), pt(46.8, 95), pt(49.3, 97)))
    sym(Muscle.QUADS, 85f, listOf(pt(36.5, 104), pt(34, 114), pt(33.8, 126), pt(35.5, 138), pt(38.5, 146), pt(40.5, 140), pt(40.8, 124), pt(39.8, 110)))
    sym(Muscle.QUADS, 90f, listOf(pt(42.5, 104), pt(40.6, 113), pt(40.8, 127), pt(42, 139), pt(44.5, 143), pt(46.4, 130), pt(46.6, 115), pt(45.5, 107)))
    sym(Muscle.QUADS, 70f, listOf(pt(46.8, 128), pt(44.8, 135), pt(43.8, 142), pt(45, 148.5), pt(48, 146.5), pt(48.8, 137)))
    sym(Muscle.ADDUCTORS, 70f, listOf(pt(49.4, 104.5), pt(47.2, 108), pt(46.8, 116), pt(47, 125), pt(48.6, 123), pt(49.7, 113)))
    sym(Muscle.CALVES, 95f, listOf(pt(37.4, 152.5), pt(35.8, 160), pt(36.3, 169.5), pt(38, 175), pt(39.3, 167), pt(39, 157)))
    sym(Muscle.TIBIALIS, 92f, listOf(pt(41, 151.8), pt(39.6, 156.5), pt(39.7, 166), pt(40.7, 177), pt(42, 181), pt(42.6, 169), pt(42.5, 158), pt(42.2, 153)))
    sym(Muscle.CALVES, 85f, listOf(pt(45.3, 152), pt(44, 159), pt(44.2, 169), pt(45.4, 174), pt(46.6, 164), pt(46.5, 156)))
    tendon(listOf(pt(42, 148.6), pt(45.2, 148.6), pt(45.6, 151.6), pt(43.6, 153.6), pt(41.6, 151.6)))
    tendon(listOf(pt(25, 61.2), pt(26.6, 61.6), pt(25.6, 64.4), pt(24.4, 63.8)))
    vein(listOf(pt(32.6, 35.5), pt(31.6, 40), pt(30.3, 44.5)))
    vein(listOf(pt(25.2, 47.5), pt(24.2, 52), pt(24, 57), pt(24.4, 61.5), pt(23.6, 65)))
    vein(listOf(pt(23.6, 65), pt(21.8, 71), pt(20.2, 78), pt(19, 85), pt(18.4, 89.5)))
    vein(listOf(pt(21.8, 71), pt(23.6, 76), pt(23, 82), pt(21.6, 88)))
    vein(listOf(pt(43.2, 84), pt(43.6, 89), pt(44.8, 94), pt(46.6, 100)))
    // Ключицы, грудина, пупок и складка над коленом — мелкие детали поверх.
    crease(listOf(pt(46.6, 27.6), pt(42, 28.4), pt(37.6, 29.8)))
    // Складка под грудью.
    if (buildingFemale) crease(listOf(pt(36.2, 50.6), pt(39.8, 54.6), pt(44.8, 55.4), pt(48.6, 53.6)))
    crease(listOf(pt(50, 33), pt(50, 47.5)), symmetric = false)
    crease(listOf(pt(49.6, 87.2), pt(50, 88.2), pt(50.4, 87.2)), symmetric = false)
    crease(listOf(pt(41.5, 145.6), pt(43.8, 146.6), pt(46, 145.8)))
}

private fun backFigure() = figure {
    center(Muscle.TRAPS, 90f, listOf(pt(50, 21), pt(46.5, 24.5), pt(41, 28), pt(34.5, 30.5), pt(39, 33), pt(44, 38), pt(46.5, 46), pt(50, 57), pt(53.5, 46), pt(56, 38), pt(61, 33), pt(65.5, 30.5), pt(59, 28), pt(53.5, 24.5)))
    sym(Muscle.SIDE_DELTS, 100f, listOf(pt(30, 30.5), pt(25.8, 32.2), pt(23.4, 37.5), pt(23.8, 44), pt(26.5, 43), pt(27.3, 36.5)), fan = pt(24.6, 43.6))
    sym(Muscle.REAR_DELTS, 70f, listOf(pt(34.5, 31), pt(29.8, 32), pt(27.8, 36.5), pt(28.4, 41.8), pt(31.5, 41), pt(34.6, 36.5)), fan = pt(28.8, 41.5))
    sym(Muscle.TRICEPS, 92f, listOf(pt(30.6, 43), pt(26, 44), pt(23, 49.5), pt(22.6, 56), pt(24.4, 62), pt(28, 61), pt(29.6, 55), pt(30.8, 48)))
    sym(Muscle.FOREARMS, 100f, listOf(pt(26.5, 63.5), pt(22, 64.8), pt(19.3, 72), pt(17.6, 82), pt(17.2, 90), pt(20, 90.8), pt(22.4, 82), pt(25, 73), pt(27, 67.5)))
    sym(Muscle.INFRASPINATUS, 20f, listOf(pt(39.4, 33.8), pt(36, 33.6), pt(34, 35.6), pt(34.6, 38.8), pt(37.6, 40.6), pt(41.8, 41.4), pt(45.2, 41.8), pt(43.2, 38.2)), fan = pt(34.6, 35.4))
    sym(Muscle.TERES_MAJOR, 160f, listOf(pt(34.4, 39.8), pt(31.8, 41.2), pt(31.2, 44), pt(32.8, 45.4), pt(35.8, 43.8), pt(37, 41.6)))
    sym(Muscle.LATS, 60f, listOf(pt(46.6, 43), pt(43.6, 42.2), pt(39.4, 42.6), pt(36.8, 44.2), pt(33.6, 46.6), pt(32.8, 51), pt(34.5, 60), pt(37.5, 70), pt(41.2, 79), pt(44.8, 85), pt(45, 72), pt(45.6, 58), pt(46.4, 49)), fan = pt(33.8, 46))
    // Ягодицы начинаются от гребня таза (чуть ниже поясницы), под ними — ягодичная складка.
    sym(Muscle.LOWER_BACK, 90f, listOf(pt(49.3, 58), pt(47, 60), pt(46.3, 70), pt(46.1, 79), pt(46.8, 86), pt(49.3, 89.5)))
    sym(Muscle.GLUTES, 30f, listOf(pt(49.2, 91), pt(43.8, 89.6), pt(38.2, 91.4), pt(34.6, 97), pt(34.6, 105), pt(37.8, 111.6), pt(43.6, 114.4), pt(49.2, 112.6)), fan = pt(35, 102))
    sym(Muscle.HAMSTRINGS, 95f, listOf(pt(37, 115.4), pt(34.4, 124), pt(34.6, 137), pt(37.5, 147), pt(41, 148.5), pt(42, 135), pt(41.6, 123), pt(40.2, 116.2)))
    sym(Muscle.HAMSTRINGS, 85f, listOf(pt(43.4, 116.4), pt(42.6, 125), pt(42.8, 140), pt(44.3, 148.5), pt(47, 146.5), pt(48.3, 133), pt(48.2, 121), pt(46.4, 115.6)))
    sym(Muscle.CALVES, 95f, listOf(pt(39.5, 150.5), pt(36.2, 155), pt(35.6, 164), pt(37.6, 173), pt(40.6, 176), pt(41.6, 164), pt(41.2, 154)))
    sym(Muscle.CALVES, 85f, listOf(pt(43.4, 150.5), pt(42.2, 155), pt(42.4, 166), pt(43.8, 176.5), pt(46.6, 172), pt(47, 161), pt(46.2, 153)))
    tendon(listOf(pt(40.4, 175), pt(42.6, 175), pt(42.4, 186), pt(41.6, 189), pt(40.6, 186)))
    tendon(listOf(pt(24.4, 60.4), pt(28, 60.6), pt(27, 63.4), pt(25, 63.4)))
    tendonLine(listOf(pt(37.6, 147.5), pt(37.2, 151.5)))
    tendonLine(listOf(pt(46.4, 147.5), pt(46.8, 151.5)))
    vein(listOf(pt(39, 156), pt(38.3, 162), pt(38.6, 168), pt(39.6, 173)))
    vein(listOf(pt(44.6, 157), pt(45, 163), pt(44.6, 169)))
    vein(listOf(pt(20.4, 74), pt(19.4, 81), pt(18.6, 88)))
    // Позвоночник и подколенная складка.
    crease(listOf(pt(50, 58), pt(50, 89)), symmetric = false)
    crease(listOf(pt(37.4, 113.4), pt(42.6, 115.6), pt(47.6, 114.6)))
    crease(listOf(pt(38.4, 149), pt(42, 149.8), pt(45.6, 149)))
}

private val SILHOUETTE by lazy(FIGURE_LOCK) { silhouette() }
private val FRONT by lazy(FIGURE_LOCK) { frontFigure() }
private val BACK by lazy(FIGURE_LOCK) { backFigure() }
private val SILHOUETTE_F by lazy(FIGURE_LOCK) { female { silhouette() } }
private val FRONT_F by lazy(FIGURE_LOCK) { female { frontFigure() } }
private val BACK_F by lazy(FIGURE_LOCK) { female { backFigure() } }

/** Волосы: спереди — за головой (видны по бокам и у шеи), сзади — поверх затылка. */
private val HAIR_FRONT by lazy(FIGURE_LOCK) {
    female {
        APath().apply {
            // Длинные волосы до плеч: за головой и шеей видны по бокам лица и над плечами.
            spline(listOf(pt(50, 2), pt(43.5, 3.8), pt(40.4, 9.5), pt(39.6, 17), pt(38.6, 24), pt(38.4, 29.5), pt(41.4, 31.2), pt(44, 28.6), pt(46, 24.6), pt(50, 23.6), pt(54, 24.6), pt(56, 28.6), pt(58.6, 31.2), pt(61.6, 29.5), pt(61.4, 24), pt(60.4, 17), pt(59.6, 9.5), pt(56.5, 3.8)))
            oval(50f, 3.6f, 3.4f, 2.8f) // пучок
        }
    }
}
private val HAIR_BACK by lazy(FIGURE_LOCK) {
    female {
        APath().apply {
            spline(listOf(pt(50, 2), pt(43.6, 3.8), pt(40.8, 10), pt(40.6, 17), pt(41.4, 22.4), pt(44, 25), pt(47.4, 24.6), pt(50, 23.2), pt(52.6, 24.6), pt(56, 25), pt(58.6, 22.4), pt(59.4, 17), pt(59.2, 10), pt(56.4, 3.8)))
            oval(50f, 3.2f, 3.6f, 3f)
        }
    }
}

private fun figureOf(back: Boolean, female: Boolean = false) = when {
    female && back -> BACK_F
    female -> FRONT_F
    back -> BACK
    else -> FRONT
}

/** Мышцы, видимые на схеме спереди и сзади. */
val FRONT_MUSCLES: Set<Muscle> get() = FRONT.muscles.keys
val BACK_MUSCLES: Set<Muscle> get() = BACK.muscles.keys

/** Мышца под точкой ([x], [y]) в координатах схемы 100×200 или null. Верхние слои проверяются первыми. */
fun muscleAt(back: Boolean, x: Float, y: Float, female: Boolean = false): Muscle? {
    // Region работает с целыми координатами — увеличиваем схему в 10 раз для точности.
    val zoom = Matrix().apply { setScale(10f, 10f) }
    val clip = Region(0, 0, 1000, 2000)
    return figureOf(back, female).muscles.entries.reversed().firstOrNull { (_, shapes) ->
        shapes.any { s ->
            val scaled = APath(s.path).apply { transform(zoom) }
            Region().apply { setPath(scaled, clip) }.contains((x * 10).toInt(), (y * 10).toInt())
        }
    }?.key
}

private const val FIBER_WIDTH = 0.26f
private const val GAP_WIDTH = 0.7f
private const val OUTLINE_WIDTH = 1.1f
private const val SELECTED_WIDTH = 1.2f
private const val TENDON_LINE_WIDTH = 0.9f
private const val VEIN_WIDTH = 0.55f
private const val VEIN_HIGHLIGHT_WIDTH = 0.2f
private const val RIM_WIDTH = 1.5f
private const val CREASE_WIDTH = 0.35f
private val FiberColor = Color.Black.copy(alpha = 0.1f).toArgb()
private val HairColor = Color(0xFF6D4C41)
private val RimColor = Color.Black.copy(alpha = 0.13f).toArgb()
private val CreaseColor = Color.Black.copy(alpha = 0.16f).toArgb()
private val VeinColor = Color(0xFF3C2F5A).copy(alpha = 0.42f).toArgb()
private val VeinHighlight = Color.White.copy(alpha = 0.35f).toArgb()

private fun strokePaint(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = width
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
    this.color = color
}

/** Рисует фигуру в координатах 100×200 (масштаб задаёт вызывающий). */
private fun Canvas.drawFigure(
    back: Boolean,
    colorOf: (Muscle) -> Int,
    bodyColor: Int,
    gapColor: Int,
    selected: Muscle?,
    selectedColor: Int,
    female: Boolean = false,
) {
    val figure = figureOf(back, female)
    val silhouette = if (female) SILHOUETTE_F else SILHOUETTE
    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    // Волосы — тёплого каштанового оттенка поверх цвета тела, чтобы подходили и к светлой, и к тёмной теме.
    val hair = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lerp(Color(bodyColor), HairColor, 0.55f).toArgb() }
    if (female && !back) drawPath(HAIR_FRONT, hair)
    // Контур: обводка под заливкой видна только снаружи силуэта.
    val outline = strokePaint(lerp(Color(bodyColor), Color.Black, 0.1f).toArgb(), OUTLINE_WIDTH)
    silhouette.forEach { drawPath(it, outline) }
    // Тело чуть светлее сверху, будто освещено сверху.
    fill.shader = LinearGradient(
        0f, 0f, 0f, 200f,
        lerp(Color(bodyColor), Color.White, 0.12f).toArgb(),
        lerp(Color(bodyColor), Color.Black, 0.06f).toArgb(),
        Shader.TileMode.CLAMP,
    )
    silhouette.forEach { drawPath(it, fill) }
    fill.shader = null
    if (female && back) drawPath(HAIR_BACK, hair)

    // Сухожилия — светлее тела; мышцы ложатся поверх.
    val tendonColor = lerp(Color(bodyColor), Color.White, 0.6f).toArgb()
    fill.color = tendonColor
    figure.tendons.forEach { drawPath(it, fill) }
    val tendonLine = strokePaint(tendonColor, TENDON_LINE_WIDTH)
    figure.tendonLines.forEach { drawPath(it, tendonLine) }

    val fiber = strokePaint(FiberColor, FIBER_WIDTH)
    val gap = strokePaint(gapColor, GAP_WIDTH)
    val rim = strokePaint(RimColor, RIM_WIDTH)
    figure.muscles.forEach { (muscle, shapes) ->
        val color = colorOf(muscle)
        shapes.forEach { s ->
            fill.shader = null
            fill.color = color
            drawPath(s.path, fill)
            save()
            clipPath(s.path)
            fill.shader = s.shade
            drawRect(s.bounds, fill)
            drawPath(s.fibers, fiber)
            // Тень по краю внутри мышцы: обводка, обрезанная контуром, видна только изнутри.
            drawPath(s.path, rim)
            restore()
            drawPath(s.path, gap)
        }
    }
    fill.shader = null

    val vein = strokePaint(VeinColor, VEIN_WIDTH)
    val veinLight = strokePaint(VeinHighlight, VEIN_HIGHLIGHT_WIDTH)
    figure.veins.forEach { v ->
        drawPath(v, vein)
        save()
        translate(-0.15f, -0.15f)
        drawPath(v, veinLight)
        restore()
    }

    val crease = strokePaint(CreaseColor, CREASE_WIDTH)
    figure.creases.forEach { drawPath(it, crease) }

    selected?.let { m ->
        val stroke = strokePaint(selectedColor, SELECTED_WIDTH)
        figure.muscles[m]?.forEach { drawPath(it.path, stroke) }
    }
}

/**
 * Линия замера на фигуре спереди в координатах схемы 100×200: плечо и предплечье — на левой
 * (по экрану) руке, грудь и талия — поперёк корпуса. Для женской фигуры — в её пропорциях.
 */
fun measurementLine(part: MeasurementPart, female: Boolean): Pair<PointF, PointF> = synchronized(FIGURE_LOCK) {
    fun line() = when (part) {
        MeasurementPart.BICEPS -> pt(21.6, 53) to pt(31, 53)
        MeasurementPart.FOREARM -> pt(18.8, 70) to pt(26.8, 70)
        MeasurementPart.CHEST -> pt(31.2, 41) to pt(68.8, 41)
        MeasurementPart.WAIST -> pt(35, 74) to pt(65, 74)
    }
    if (female) female { line() } else line()
}

/** Фигура спереди на холсте 100×200 (масштаб задаёт вызывающий) — для схемы замеров. */
fun Canvas.drawBodyFront(colorOf: (Muscle) -> Int, bodyColor: Int, gapColor: Int, female: Boolean) =
    drawFigure(back = false, colorOf = colorOf, bodyColor = bodyColor, gapColor = gapColor, selected = null, selectedColor = 0, female = female)

/** Женский силуэт на схемах — по настройке «Схема мышц». */
val LocalFemaleBody = staticCompositionLocalOf { false }

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
    female: Boolean = LocalFemaleBody.current,
) {
    val tap = if (onTap == null) Modifier else Modifier.pointerInput(back, onTap, female) {
        detectTapGestures { offset ->
            val scale = size.width / 100f
            muscleAt(back, offset.x / scale, offset.y / scale, female)?.let(onTap)
        }
    }
    Canvas(modifier.aspectRatio(0.5f).then(tap)) {
        val scale = size.width / 100f
        drawIntoCanvas { c ->
            val canvas = c.nativeCanvas
            canvas.save()
            canvas.scale(scale, scale)
            canvas.drawFigure(
                back = back,
                colorOf = { colorOf(it).toArgb() },
                bodyColor = bodyColor.toArgb(),
                gapColor = gapColor.toArgb(),
                selected = selected,
                selectedColor = selectedColor.toArgb(),
                female = female,
            )
            canvas.restore()
        }
    }
}

/**
 * Тело спереди и сзади рядом одной картинкой — для виджета (там нет Compose).
 * [colorOf] — цвет мышцы (ARGB), [height] — высота картинки в пикселях.
 */
fun renderBodyBitmap(
    colorOf: (Muscle) -> Int,
    bodyColor: Int,
    gapColor: Int,
    height: Int,
    female: Boolean = false,
): android.graphics.Bitmap {
    val scale = height / 200f
    val gap = 12f
    val bitmap = android.graphics.Bitmap.createBitmap(((200 + gap) * scale).toInt(), height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    listOf(false, true).forEachIndexed { i, back ->
        canvas.save()
        canvas.translate(i * (100 + gap) * scale, 0f)
        canvas.scale(scale, scale)
        canvas.drawFigure(back, colorOf, bodyColor, gapColor, selected = null, selectedColor = 0, female = female)
        canvas.restore()
    }
    return bitmap
}
