package com.sonnik.topower.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonnik.topower.R
import com.sonnik.topower.data.EquipmentEntity
import com.sonnik.topower.data.EquipmentType
import com.sonnik.topower.domain.formatNumber

/** Цвета, которыми обычно маркируют резиновые эспандеры. */
val BandColors: List<Long> = listOf(
    0xFFFDD835, // жёлтый
    0xFFFB8C00, // оранжевый
    0xFFE53935, // красный
    0xFFEC407A, // розовый
    0xFF8E24AA, // фиолетовый
    0xFF1E88E5, // синий
    0xFF00ACC1, // бирюзовый
    0xFF43A047, // зелёный
    0xFF9E9E9E, // серый
    0xFF212121, // чёрный
)

@Composable
fun equipmentTypeLabel(type: EquipmentType, plural: Boolean = false): String = stringResource(
    when (type) {
        EquipmentType.DUMBBELL -> if (plural) R.string.eq_dumbbell_plural else R.string.eq_dumbbell
        EquipmentType.KETTLEBELL -> if (plural) R.string.eq_kettlebell_plural else R.string.eq_kettlebell
        EquipmentType.BARBELL -> if (plural) R.string.eq_barbell_plural else R.string.eq_barbell
        EquipmentType.BAND -> if (plural) R.string.eq_band_plural else R.string.eq_band
        EquipmentType.EXPANDER -> if (plural) R.string.eq_expander_plural else R.string.eq_expander
        EquipmentType.WEIGHT_VEST -> if (plural) R.string.eq_weight_vest_plural else R.string.eq_weight_vest
        EquipmentType.OTHER -> if (plural) R.string.eq_other_plural else R.string.eq_other
    }
)

@Composable
fun rangeKg(min: Double, max: Double): String =
    stringResource(R.string.fmt_range_kg, formatNumber(min), formatNumber(max))

/** Нагрузка предмета: «12 кг» или «7–16 кг» для резины. */
@Composable
fun equipmentLoad(item: EquipmentEntity): String {
    val min = item.minResistance
    val max = item.maxResistance
    val weight = item.weight
    return when {
        item.equipmentType.isBand && min != null && max != null -> rangeKg(min, max)
        weight != null -> stringResource(R.string.fmt_kg, formatNumber(weight))
        else -> ""
    }
}

/** Короткое имя предмета для подписей: название или тип с весом. */
@Composable
fun equipmentName(item: EquipmentEntity): String = when {
    item.name.isNotBlank() -> item.name
    item.equipmentType.isBand -> stringResource(R.string.band_default_name)
    else -> equipmentTypeLabel(item.equipmentType)
}

/** Подпись для чипа выбора: у гантелей достаточно веса, у резины — имя и диапазон. */
@Composable
fun equipmentChipLabel(item: EquipmentEntity): String = when {
    item.equipmentType.isBand -> equipmentName(item) + " " + equipmentLoad(item)
    item.name.isNotBlank() -> item.name + " · " + equipmentLoad(item)
    else -> equipmentLoad(item)
}

@Composable
fun ColorDot(color: Long?, size: Dp = 16.dp, modifier: Modifier = Modifier) {
    if (color == null) return
    Box(
        modifier
            .size(size)
            .background(Color(color), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    )
}
