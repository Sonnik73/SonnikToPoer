package com.sonnik.topower.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.sonnik.topower.R
import kotlin.math.abs

data class ChartPoint(val x: Long, val y: Double)

/** Зазор между подписями значений, чтобы они не слипались. */
private val LabelGap = 6.dp

/**
 * Подписи значений над точками без наложения: сначала [priority] (выбранная, последняя, максимальная),
 * затем остальные слева направо — каждая, если не задевает уже поставленные.
 * [anchors] — точки, над которыми стоит подпись; возвращает индексы и прямоугольники подписей.
 */
private fun DrawScope.placeLabels(
    layouts: List<TextLayoutResult>,
    anchors: List<Offset>,
    priority: List<Int>,
    top: Float,
): List<Pair<Int, Rect>> {
    val gap = LabelGap.toPx()
    val placed = mutableListOf<Pair<Int, Rect>>()
    (priority + anchors.indices).distinct().forEach { i ->
        val labelSize = layouts[i].size
        val left = (anchors[i].x - labelSize.width / 2f).coerceIn(0f, (size.width - labelSize.width).coerceAtLeast(0f))
        var y = anchors[i].y - gap - labelSize.height
        if (y < top) y = anchors[i].y + gap // нет места сверху — под точкой
        val rect = Rect(left, y, left + labelSize.width, y + labelSize.height)
        val inflated = rect.inflate(gap / 2)
        if (placed.none { it.second.overlaps(inflated) }) placed += i to rect
    }
    return placed
}

private fun DrawScope.drawValueLabels(
    measurer: TextMeasurer,
    labels: List<String>,
    anchors: List<Offset>,
    priority: List<Int>,
    selected: Int?,
    style: TextStyle,
    color: Color,
    selectedColor: Color,
) {
    val layouts = labels.mapIndexed { i, text ->
        val isSelected = i == selected
        measurer.measure(
            text,
            style.copy(color = if (isSelected) selectedColor else color, fontWeight = if (isSelected) FontWeight.Bold else null),
        )
    }
    placeLabels(layouts, anchors, priority, top = 0f).forEach { (i, rect) -> drawText(layouts[i], topLeft = rect.topLeft) }
}

private fun nearestIndex(x: Float, xs: List<Float>): Int = xs.indices.minBy { abs(xs[it] - x) }

/** Строка над графиком: значение и дата выбранной точки или подсказка «нажмите на точку». */
@Composable
private fun SelectionLine(text: String?) {
    Text(
        text ?: stringResource(R.string.chart_tap_hint),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (text != null) FontWeight.SemiBold else FontWeight.Normal,
        color = if (text != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Линейный график: точки по порядку, ось Y от минимума до максимума. Над точками — значения
 * (сколько помещается без наложения), нажатие или проведение пальцем выбирает точку.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    formatY: @Composable (Double) -> String,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) {
        Text(
            stringResource(R.string.not_enough_data),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labels = points.map { formatY(it.y) }
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    val range = (maxY - minY).takeIf { it > 0.0 } ?: 1.0
    val maxIndex = points.indices.maxBy { points[it].y }

    Column(modifier.fillMaxWidth()) {
        SelectionLine(selected?.let { labels[it] + " · " + formatDate(points[it].x) })
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .pointerInput(points) {
                    detectTapGestures { o -> selected = nearestIndex(o.x, pointXs(points.size, size.width.toFloat(), 8.dp.toPx())) }
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures { change, _ ->
                        selected = nearestIndex(change.position.x, pointXs(points.size, size.width.toFloat(), 8.dp.toPx()))
                    }
                }
        ) {
            val pad = 8.dp.toPx()
            val top = 22.dp.toPx() // место под подписи над верхней точкой
            val h = size.height - top - pad
            drawLine(gridColor, Offset(0f, top), Offset(size.width, top))
            drawLine(gridColor, Offset(0f, top + h), Offset(size.width, top + h))
            val xs = pointXs(points.size, size.width, pad)
            val offsets = points.mapIndexed { i, p -> Offset(xs[i], top + h * (1f - ((p.y - minY) / range).toFloat())) }
            selected?.let { s ->
                drawLine(gridColor, Offset(offsets[s].x, top), Offset(offsets[s].x, top + h), strokeWidth = 2.dp.toPx())
            }
            val path = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            offsets.forEachIndexed { i, o ->
                drawCircle(lineColor, radius = (if (i == selected) 6 else 4).dp.toPx(), center = o)
            }
            drawValueLabels(
                measurer, labels, offsets, listOfNotNull(selected, points.lastIndex, maxIndex),
                selected, labelStyle, labelColor, lineColor,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatShortDate(points.first().x), style = MaterialTheme.typography.labelSmall)
            Text(formatShortDate(points.last().x), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Координаты X точек: равномерно по ширине с отступом [pad] по краям. */
private fun pointXs(count: Int, width: Float, pad: Float): List<Float> =
    List(count) { pad + (width - 2 * pad) * it / (count - 1) }

/**
 * Столбчатая диаграмма целых значений: над столбцами — числа (кроме нулей), под первым и последним —
 * подписи. Нажатие на столбец показывает его значение и подпись из [barLabels].
 */
@Composable
fun BarChart(
    values: List<Int>,
    barLabels: List<String>,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) return
    val barColor = MaterialTheme.colorScheme.tertiary
    val selectedColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val maxValue = values.max().coerceAtLeast(1)
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    Column(modifier.fillMaxWidth()) {
        SelectionLine(selected?.let { values[it].toString() + " · " + barLabels[it] })
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(130.dp)
                .pointerInput(values) {
                    detectTapGestures { o ->
                        selected = (o.x / (size.width.toFloat() / values.size)).toInt().coerceIn(0, values.lastIndex)
                    }
                }
        ) {
            val top = 24.dp.toPx() // место под подпись над самым высоким столбцом
            val slot = size.width / values.size
            val barWidth = slot * 0.6f
            val anchors = values.mapIndexed { i, v ->
                val barHeight = (size.height - top) * v / maxValue
                drawRoundRect(
                    color = if (i == selected) selectedColor else barColor,
                    topLeft = Offset(slot * i + (slot - barWidth) / 2, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
                Offset(slot * i + slot / 2, size.height - barHeight)
            }
            // Нули не подписываем: на пустых неделях подпись только мешает.
            val shown = values.indices.filter { values[it] > 0 || it == selected }
            drawValueLabels(
                measurer,
                labels = shown.map { values[it].toString() },
                anchors = shown.map { anchors[it] },
                priority = listOfNotNull(selected?.let { shown.indexOf(it) }),
                selected = selected?.let { shown.indexOf(it) },
                style = labelStyle,
                color = labelColor,
                selectedColor = selectedColor,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(barLabels.first(), style = MaterialTheme.typography.labelSmall)
            Text(barLabels.last(), style = MaterialTheme.typography.labelSmall)
        }
    }
}
