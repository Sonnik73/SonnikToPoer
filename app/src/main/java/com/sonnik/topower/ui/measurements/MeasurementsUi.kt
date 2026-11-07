package com.sonnik.topower.ui.measurements

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.data.MeasurementEntity
import com.sonnik.topower.data.MeasurementPart
import com.sonnik.topower.domain.formatNumber
import com.sonnik.topower.domain.parseDecimal
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.ChartPoint
import com.sonnik.topower.ui.common.LineChart
import com.sonnik.topower.ui.common.NumberField
import com.sonnik.topower.ui.common.SectionCard
import com.sonnik.topower.ui.common.formatDate
import com.sonnik.topower.ui.common.formatDateTime
import com.sonnik.topower.ui.muscles.LocalFemaleBody
import com.sonnik.topower.ui.muscles.drawBodyFront
import com.sonnik.topower.ui.muscles.measurementLine
import com.sonnik.topower.ui.theme.isAppInDarkTheme

/** Последнее значение замера и изменение к предыдущему (null — замер один). */
data class LatestMeasurement(val valueCm: Double, val deltaCm: Double?, val takenAt: Long)

/** По каждому месту — последний замер и изменение к прошлому. [items] — замеры по возрастанию даты. */
fun latestMeasurements(items: List<MeasurementEntity>): Map<MeasurementPart, LatestMeasurement> =
    MeasurementPart.entries.mapNotNull { part ->
        val list = items.filter { it.part == part.name }.sortedBy { it.takenAt }
        val last = list.lastOrNull() ?: return@mapNotNull null
        val prev = list.getOrNull(list.size - 2)
        part to LatestMeasurement(last.valueCm, prev?.let { last.valueCm - it.valueCm }, last.takenAt)
    }.toMap()

fun partNameRes(part: MeasurementPart): Int = when (part) {
    MeasurementPart.BICEPS -> R.string.measure_biceps
    MeasurementPart.FOREARM -> R.string.measure_forearm
    MeasurementPart.CHEST -> R.string.measure_chest
    MeasurementPart.WAIST -> R.string.measure_waist
}

/** Подписи слева — руки, справа — корпус. */
private val LEFT_PARTS = listOf(MeasurementPart.BICEPS, MeasurementPart.FOREARM)

/**
 * Карточка «Замеры тела»: фигура спереди с пунктирами мест замеров и подписями сбоку
 * (последнее значение и изменение), кнопки «Провести замеры» и «История».
 */
@Composable
fun MeasurementsCard(modifier: Modifier = Modifier) {
    val repo = LocalRepository.current
    val items by remember { repo.dao.observeMeasurements() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val latest = remember(items) { latestMeasurements(items) }
    var measuring by rememberSaveable { mutableStateOf(false) }
    var history by rememberSaveable { mutableStateOf(false) }

    SectionCard(modifier) {
        Text(stringResource(R.string.measure_title), style = MaterialTheme.typography.titleMedium)
        MeasurementFigure(latest)
        Text(
            latest.values.maxOfOrNull { it.takenAt }?.let { stringResource(R.string.measure_last, formatDate(it)) }
                ?: stringResource(R.string.measure_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { measuring = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.measure_now)) }
            OutlinedButton(onClick = { history = true }, enabled = items.isNotEmpty()) { Text(stringResource(R.string.measure_history)) }
        }
    }

    if (measuring) {
        MeasureDialog(
            latest = latest,
            onSave = { values -> repo.launch { saveMeasurements(values) } },
            onDismiss = { measuring = false },
        )
    }
    if (history) {
        MeasurementHistoryDialog(
            items = items,
            onDelete = { takenAt -> repo.launch { deleteMeasurementsAt(takenAt) } },
            onDismiss = { history = false },
        )
    }
}

/** Фигура с пунктирами замеров и выносными подписями: руки — слева, грудь и талия — справа. */
@Composable
private fun MeasurementFigure(latest: Map<MeasurementPart, LatestMeasurement>) {
    val female = LocalFemaleBody.current
    val idle = if (isAppInDarkTheme) Color(0xFF4A515B) else Color(0xFFC3C9D1)
    val bodyColor = MaterialTheme.colorScheme.surfaceVariant
    val gapColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val lineColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val names = MeasurementPart.entries.associateWith { stringResource(partNameRes(it)) }
    val noValue = stringResource(R.string.measure_no_value)
    val cmFormat = stringResource(R.string.measure_cm, "%s")
    fun valueText(part: MeasurementPart): String {
        val m = latest[part] ?: return noValue
        val delta = m.deltaCm?.takeIf { kotlin.math.abs(it) >= 0.05 }
            ?.let { " (" + (if (it > 0) "+" else "−") + formatNumber(kotlin.math.abs(it), 1) + ")" }.orEmpty()
        return cmFormat.format(formatNumber(m.valueCm, 1)) + delta
    }

    Canvas(Modifier.fillMaxWidth().height(260.dp)) {
        val figH = size.height
        val figW = figH / 2f
        val left = (size.width - figW) / 2f
        val scale = figW / 100f
        drawIntoCanvas { c ->
            val canvas = c.nativeCanvas
            canvas.save()
            canvas.translate(left, 0f)
            canvas.scale(scale, scale)
            canvas.drawBodyFront({ idle.toArgb() }, bodyColor.toArgb(), gapColor.toArgb(), female)
            canvas.restore()

            val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.dp.toPx()
                color = lineColor.toArgb()
                strokeCap = Paint.Cap.ROUND
                pathEffect = DashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()), 0f)
            }
            val leader = Paint(dash).apply { strokeWidth = 1.dp.toPx(); pathEffect = null; alpha = 140 }
            val nameText = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12.sp.toPx(); color = mutedColor.toArgb() }
            val valueTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 13.sp.toPx(); color = textColor.toArgb(); typeface = Typeface.DEFAULT_BOLD
            }
            val pad = 6.dp.toPx()
            MeasurementPart.entries.forEach { part ->
                val (a, b) = measurementLine(part, female)
                val y = a.y * scale
                canvas.drawLine(left + a.x * scale, y, left + b.x * scale, y, dash)
                val onLeft = part in LEFT_PARTS
                // Выносная линия от края пунктира к подписи у края карточки.
                val from = if (onLeft) left + a.x * scale else left + b.x * scale
                val to = if (onLeft) left - pad else left + figW + pad
                canvas.drawLine(from, y, to, y, leader)
                val align = if (onLeft) Paint.Align.RIGHT else Paint.Align.LEFT
                nameText.textAlign = align
                valueTextPaint.textAlign = align
                val textX = if (onLeft) to - pad / 2 else to + pad / 2
                canvas.drawText(names.getValue(part), textX, y - 4.dp.toPx(), nameText)
                canvas.drawText(valueText(part), textX, y + 14.dp.toPx(), valueTextPaint)
            }
        }
    }
}

/** Окно «Провести замеры»: обхваты в сантиметрах; пустые поля не сохраняются. */
@Composable
fun MeasureDialog(
    latest: Map<MeasurementPart, LatestMeasurement>,
    onSave: (Map<MeasurementPart, Double>) -> Unit,
    onDismiss: () -> Unit,
) {
    var texts by rememberSaveable { mutableStateOf(MeasurementPart.entries.associate { it.name to "" }) }
    val values = MeasurementPart.entries.mapNotNull { p -> parseDecimal(texts[p.name].orEmpty())?.takeIf { it > 0 }?.let { p to it } }.toMap()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.measure_now)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.measure_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MeasurementPart.entries.forEach { part ->
                    NumberField(
                        value = texts[part.name].orEmpty(),
                        onValueChange = { v -> texts = texts + (part.name to v) },
                        label = stringResource(R.string.measure_field, stringResource(partNameRes(part))),
                        supportingText = latest[part]?.let { stringResource(R.string.measure_previous, formatNumber(it.valueCm, 1)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(values); onDismiss() }, enabled = values.isNotEmpty()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** История: график по выбранному месту и все замеры по датам с удалением. */
@Composable
private fun MeasurementHistoryDialog(items: List<MeasurementEntity>, onDelete: (Long) -> Unit, onDismiss: () -> Unit) {
    var part by rememberSaveable { mutableStateOf(MeasurementPart.BICEPS) }
    val byDate = items.groupBy { it.takenAt }.toSortedMap(compareByDescending { it })
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.measure_history)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MeasurementPart.entries.forEach { p ->
                        FilterChip(selected = p == part, onClick = { part = p }, label = { Text(stringResource(partNameRes(p))) })
                    }
                }
                val unit = stringResource(R.string.measure_cm, "%s")
                LineChart(
                    points = items.filter { it.part == part.name }.sortedBy { it.takenAt }.map { ChartPoint(it.takenAt, it.valueCm) },
                    formatY = { unit.format(formatNumber(it, 1)) },
                )
                byDate.forEach { (takenAt, list) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(formatDateTime(takenAt), style = MaterialTheme.typography.titleSmall)
                            Text(
                                MeasurementPart.entries.mapNotNull { p ->
                                    list.firstOrNull { it.part == p.name }?.let { stringResource(partNameRes(p)) + " " + formatNumber(it.valueCm, 1) }
                                }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDelete(takenAt) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
