package com.sonnik.topower.ui.measurements

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.Crossfade
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
import com.sonnik.topower.domain.isValidNumber
import com.sonnik.topower.domain.parseDecimal
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.ChartPoint
import com.sonnik.topower.ui.common.LineChart
import com.sonnik.topower.ui.common.NumberField
import com.sonnik.topower.ui.common.SectionCard
import com.sonnik.topower.ui.common.formatDate
import com.sonnik.topower.ui.common.formatDateTime
import com.sonnik.topower.ui.muscles.BodyShape
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

/** Первый замер каждого места — точка отсчёта для «было». [items] — в любом порядке. */
fun firstMeasurements(items: List<MeasurementEntity>): Map<MeasurementPart, MeasurementEntity> =
    MeasurementPart.entries.mapNotNull { part ->
        items.filter { it.part == part.name }.minByOrNull { it.takenAt }?.let { part to it }
    }.toMap()

/** Во сколько раз изменение обхвата усиливается на схеме: +3 % в сантиметрах — +18 % в ширине. */
private const val SHAPE_GAIN = 6f
private const val SHAPE_MIN = 0.65f
private const val SHAPE_MAX = 1.5f

/**
 * Обхваты для схемы «стало»: отношение последнего замера к первому, усиленное в [SHAPE_GAIN] раз,
 * чтобы разница в сантиметр-другой была заметна. Опечатки не раздувают фигуру: коэффициент ограничен.
 */
fun bodyShape(first: Map<MeasurementPart, Double>, latest: Map<MeasurementPart, Double>): BodyShape {
    fun k(part: MeasurementPart): Float {
        val a = first[part]?.takeIf { it > 0 } ?: return 1f
        val b = latest[part] ?: return 1f
        val raw = 1f + ((b / a).toFloat() - 1f) * SHAPE_GAIN
        return (kotlin.math.round(raw.coerceIn(SHAPE_MIN, SHAPE_MAX) * 100f) / 100f)
    }
    return BodyShape(
        biceps = k(MeasurementPart.BICEPS),
        forearm = k(MeasurementPart.FOREARM),
        chest = k(MeasurementPart.CHEST),
        waist = k(MeasurementPart.WAIST),
        hips = k(MeasurementPart.HIPS),
        thigh = k(MeasurementPart.THIGH),
        calf = k(MeasurementPart.CALF),
    )
}

fun partNameRes(part: MeasurementPart): Int = when (part) {
    MeasurementPart.BICEPS -> R.string.measure_biceps
    MeasurementPart.FOREARM -> R.string.measure_forearm
    MeasurementPart.CHEST -> R.string.measure_chest
    MeasurementPart.WAIST -> R.string.measure_waist
    MeasurementPart.HIPS -> R.string.measure_hips
    MeasurementPart.THIGH -> R.string.measure_thigh
    MeasurementPart.CALF -> R.string.measure_calf
}

/** Подписи слева — руки, справа — корпус. */
private val LEFT_PARTS = listOf(MeasurementPart.BICEPS, MeasurementPart.FOREARM, MeasurementPart.THIGH, MeasurementPart.CALF)

/**
 * Карточка «Замеры тела»: фигура спереди с пунктирами мест замеров и подписями сбоку
 * (последнее значение и изменение), кнопки «Провести замеры» и «История».
 */
@Composable
fun MeasurementsCard(modifier: Modifier = Modifier) {
    val repo = LocalRepository.current
    val items by remember { repo.dao.observeMeasurements() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val latest = remember(items) { latestMeasurements(items) }
    val first = remember(items) { firstMeasurements(items) }
    var measuring by rememberSaveable { mutableStateOf(false) }
    var history by rememberSaveable { mutableStateOf(false) }
    // «Было / стало» — когда замеров хотя бы два по дате.
    val canCompare = items.map { it.takenAt }.distinct().size >= 2
    var before by rememberSaveable { mutableStateOf(false) }
    val showBefore = before && canCompare

    SectionCard(modifier) {
        Text(stringResource(R.string.measure_title), style = MaterialTheme.typography.titleMedium)
        // Дата на переключателе — только если все значения этого вида сняты в один день:
        // иначе, например, талию впервые померили позже бицепса, и общая дата была бы неверной.
        val beforeDate = first.values.map { it.takenAt }.distinct().singleOrNull()
        val afterDate = latest.values.map { it.takenAt }.distinct().singleOrNull()
        if (canCompare) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = showBefore,
                    onClick = { before = true },
                    label = { Text(beforeDate?.let { stringResource(R.string.measure_before, formatDate(it)) } ?: stringResource(R.string.measure_before_plain)) },
                )
                FilterChip(
                    selected = !showBefore,
                    onClick = { before = false },
                    label = { Text(afterDate?.let { stringResource(R.string.measure_after, formatDate(it)) } ?: stringResource(R.string.measure_after_plain)) },
                )
            }
        }
        val firstValues = first.mapValues { it.value.valueCm }
        val latestValues = latest.mapValues { it.value.valueCm }
        // «Стало»: изменение к первому замеру этого места, если с тех пор мерили ещё раз.
        val sinceFirst = latest.mapNotNull { (part, l) ->
            first[part]?.takeIf { it.takenAt != l.takenAt }?.let { part to l.valueCm - it.valueCm }
        }.toMap()
        val shape = remember(items) { bodyShape(firstValues, latestValues) }
        Crossfade(targetState = showBefore, label = "measure-before-after") { b ->
            if (b) MeasurementFigure(firstValues, emptyMap(), shape = null)
            else MeasurementFigure(latestValues, sinceFirst, shape = shape.takeIf { canCompare })
        }
        if (canCompare && !showBefore) {
            Text(
                stringResource(R.string.measure_compare_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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

/**
 * Фигура с пунктирами замеров и выносными подписями: рука и нога — слева, грудь, талия и ягодицы — справа.
 * [values] — значения в подписях, [deltas] — изменения под ними, [shape] — обхваты фигуры («стало»).
 */
@Composable
private fun MeasurementFigure(
    values: Map<MeasurementPart, Double>,
    deltas: Map<MeasurementPart, Double>,
    shape: BodyShape?,
) {
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
    val goodColor = Color(0xFF2E7D32)
    fun valueText(part: MeasurementPart): String = values[part]?.let { cmFormat.format(formatNumber(it, 1)) } ?: noValue
    fun deltaText(part: MeasurementPart): String? = deltas[part]?.takeIf { kotlin.math.abs(it) >= 0.05 }
        ?.let { (if (it > 0) "+" else "−") + formatNumber(kotlin.math.abs(it), 1) }
    /** Рост обхватов — хорошо, кроме талии: там наоборот. */
    fun deltaGood(part: MeasurementPart): Boolean {
        val d = deltas[part] ?: return false
        return if (part == MeasurementPart.WAIST) d < 0 else d > 0
    }

    Canvas(Modifier.fillMaxWidth().height(300.dp)) {
        val figH = size.height
        val figW = figH / 2f
        val left = (size.width - figW) / 2f
        val scale = figW / 100f
        drawIntoCanvas { c ->
            val canvas = c.nativeCanvas
            canvas.save()
            canvas.translate(left, 0f)
            canvas.scale(scale, scale)
            canvas.drawBodyFront({ idle.toArgb() }, bodyColor.toArgb(), gapColor.toArgb(), female, shape)
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
            val side = left - pad * 2.5f // место под подписи с каждой стороны
            val deltaPaint = Paint(nameText)
            // Подписи разносим по высоте, чтобы не наезжали: не ближе [gap] друг к другу.
            val gap = 50.dp.toPx()
            fun place(parts: List<MeasurementPart>): Map<MeasurementPart, Float> {
                val ys = mutableMapOf<MeasurementPart, Float>()
                var prev = Float.NEGATIVE_INFINITY
                parts.sortedBy { measurementLine(it, female, shape).first.y }.forEach { p ->
                    val lineY = measurementLine(p, female, shape).first.y * scale
                    val y = maxOf(lineY - 8.dp.toPx(), prev + gap).coerceIn(14.dp.toPx(), size.height - 30.dp.toPx())
                    ys[p] = y
                    prev = y
                }
                return ys
            }
            val labelY = place(LEFT_PARTS) + place(MeasurementPart.entries - LEFT_PARTS.toSet())
            fun Paint.fit(text: String, base: Float) {
                textSize = base
                val w = measureText(text)
                if (w > side) textSize = base * side / w
            }
            MeasurementPart.entries.forEach { part ->
                val (a, b) = measurementLine(part, female, shape)
                val y = a.y * scale
                canvas.drawLine(left + a.x * scale, y, left + b.x * scale, y, dash)
                val onLeft = part in LEFT_PARTS
                val ly = labelY.getValue(part)
                // Выносная линия от края пунктира к подписи у края карточки.
                val from = if (onLeft) left + a.x * scale else left + b.x * scale
                val to = if (onLeft) left - pad else left + figW + pad
                canvas.drawLine(from, y, to, ly, leader)
                val align = if (onLeft) Paint.Align.RIGHT else Paint.Align.LEFT
                val textX = if (onLeft) to - pad / 2 else to + pad / 2
                nameText.textAlign = align
                valueTextPaint.textAlign = align
                deltaPaint.textAlign = align
                nameText.fit(names.getValue(part), 12.sp.toPx())
                canvas.drawText(names.getValue(part), textX, ly - 2.dp.toPx(), nameText)
                valueTextPaint.fit(valueText(part), 14.sp.toPx())
                canvas.drawText(valueText(part), textX, ly + 15.dp.toPx(), valueTextPaint)
                deltaText(part)?.let { d ->
                    deltaPaint.color = (if (deltaGood(part)) goodColor else mutedColor).toArgb()
                    deltaPaint.fit(d, 12.sp.toPx())
                    canvas.drawText(d, textX, ly + 30.dp.toPx(), deltaPaint)
                }
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
    val allValid = MeasurementPart.entries.all { isValidNumber(texts[it.name].orEmpty()) }
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
            TextButton(onClick = { onSave(values); onDismiss() }, enabled = values.isNotEmpty() && allValid) { Text(stringResource(R.string.save)) }
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
