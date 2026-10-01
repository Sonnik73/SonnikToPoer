package com.sonnik.topower.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sonnik.topower.R

data class ChartPoint(val x: Long, val y: Double)

/**
 * Простой линейный график: точки по порядку, ось Y от минимума до максимума.
 * Подписи — максимальное и минимальное значения слева, первая и последняя даты снизу.
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
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    val range = (maxY - minY).takeIf { it > 0.0 } ?: 1.0

    Column(modifier.fillMaxWidth()) {
        Text(formatY(maxY), style = MaterialTheme.typography.labelSmall)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val pad = 6.dp.toPx()
            val w = size.width - pad * 2
            val h = size.height - pad * 2
            drawLine(gridColor, Offset(0f, pad), Offset(size.width, pad))
            drawLine(gridColor, Offset(0f, pad + h), Offset(size.width, pad + h))
            val offsets = points.mapIndexed { i, p ->
                val x = pad + w * i / (points.size - 1)
                val y = pad + h * (1f - ((p.y - minY) / range).toFloat())
                Offset(x, y)
            }
            val path = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            offsets.forEach { drawCircle(lineColor, radius = 4.dp.toPx(), center = it) }
        }
        Text(formatY(minY), style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatShortDate(points.first().x), style = MaterialTheme.typography.labelSmall)
            Text(formatShortDate(points.last().x), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Столбчатая диаграмма целых значений с подписями под первым и последним столбцом. */
@Composable
fun BarChart(
    values: List<Int>,
    firstLabel: String,
    lastLabel: String,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) return
    val barColor = MaterialTheme.colorScheme.tertiary
    val maxValue = values.max().coerceAtLeast(1)
    Column(modifier.fillMaxWidth()) {
        Text(maxValue.toString(), style = MaterialTheme.typography.labelSmall)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            val slot = size.width / values.size
            val barWidth = slot * 0.6f
            values.forEachIndexed { i, v ->
                val barHeight = size.height * v / maxValue
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(slot * i + (slot - barWidth) / 2, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(firstLabel, style = MaterialTheme.typography.labelSmall)
            Text(lastLabel, style = MaterialTheme.typography.labelSmall)
        }
    }
}
