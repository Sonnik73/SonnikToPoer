@file:OptIn(ExperimentalLayoutApi::class)

package com.sonnik.topower.ui.inventory

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.data.EquipmentEntity
import com.sonnik.topower.data.EquipmentType
import com.sonnik.topower.domain.formatNumber
import com.sonnik.topower.domain.parseDecimal
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.ConfirmDialog
import com.sonnik.topower.ui.common.EmptyState
import com.sonnik.topower.ui.common.NumberField
import com.sonnik.topower.ui.common.ScreenScaffold
import com.sonnik.topower.ui.common.SectionTitle
import com.sonnik.topower.ui.exercises.CheckRow

@Composable
fun InventoryScreen() {
    val repo = LocalRepository.current
    val context = LocalContext.current
    val items by remember { repo.dao.observeEquipment(includeArchived = false) }
        .collectAsStateWithLifecycle(initialValue = null)
    var creating by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EquipmentEntity?>(null) }
    var deleting by remember { mutableStateOf<EquipmentEntity?>(null) }
    val archivedMsg = stringResource(R.string.equipment_archived_msg)

    ScreenScaffold(
        title = stringResource(R.string.nav_inventory),
        showSettings = true,
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_equipment))
            }
        },
    ) { padding ->
        val list = items
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyState(stringResource(R.string.no_equipment), Modifier.padding(padding))
            else -> LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
            ) {
                list.groupBy { it.equipmentType }.toSortedMap().forEach { (type, group) ->
                    item(key = "header-$type") { SectionTitle(equipmentTypeLabel(type, plural = true)) }
                    items(group, key = { it.id }) { item ->
                        ListItem(
                            modifier = Modifier.clickable { editing = item },
                            leadingContent = if (item.color != null) {
                                { ColorDot(item.color, size = 24.dp) }
                            } else {
                                null
                            },
                            // У безымянных гантелей и гирь заголовком служит вес — тип уже в заголовке группы.
                            headlineContent = {
                                Text(if (item.name.isBlank() && !item.equipmentType.isBand) equipmentLoad(item) else equipmentName(item))
                            },
                            supportingContent = if (item.name.isBlank() && !item.equipmentType.isBand) {
                                null
                            } else {
                                { Text(equipmentLoad(item), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (creating) EquipmentEditDialog(initial = null, onDismiss = { creating = false })
    editing?.let { item ->
        EquipmentEditDialog(initial = item, onDismiss = { editing = null }, onDelete = { deleting = item })
    }
    deleting?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.delete_equipment_title, equipmentName(item) + " " + equipmentLoad(item)),
            text = stringResource(R.string.delete_equipment_confirm),
            onConfirm = {
                repo.launch {
                    if (!deleteOrArchiveEquipment(item)) Toast.makeText(context, archivedMsg, Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
fun EquipmentEditDialog(initial: EquipmentEntity?, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null) {
    val repo = LocalRepository.current
    var type by rememberSaveable { mutableStateOf(initial?.equipmentType ?: EquipmentType.DUMBBELL) }
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var weight by rememberSaveable { mutableStateOf(initial?.weight?.let(::formatNumber) ?: "") }
    var min by rememberSaveable { mutableStateOf(initial?.minResistance?.let(::formatNumber) ?: "") }
    var max by rememberSaveable { mutableStateOf(initial?.maxResistance?.let(::formatNumber) ?: "") }
    var color by rememberSaveable { mutableStateOf(initial?.color) }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    val minValue = parseDecimal(min)
    val maxValue = parseDecimal(max)
    val weightValue = parseDecimal(weight)
    val error: Int? = when {
        type.isBand && (minValue == null || maxValue == null || minValue < 0 || minValue > maxValue) ->
            R.string.band_range_required
        !type.isBand && (weightValue == null || weightValue < 0) -> R.string.weight_required
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.new_equipment else R.string.edit_equipment)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.equipment_type), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EquipmentType.entries.forEach { t ->
                        FilterChip(
                            selected = t == type,
                            onClick = { type = t },
                            label = { Text(equipmentTypeLabel(t)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(if (type.isBand) R.string.equipment_name_band else R.string.equipment_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type.isBand) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(min, { min = it }, stringResource(R.string.band_min), Modifier.weight(1f))
                        NumberField(max, { max = it }, stringResource(R.string.band_max), Modifier.weight(1f))
                    }
                    Text(
                        stringResource(R.string.band_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(stringResource(R.string.band_color), style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BandColors.forEach { c ->
                            val selected = c == color
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .then(
                                        if (selected) {
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { color = if (selected) null else c },
                                contentAlignment = Alignment.Center,
                            ) { ColorDot(c, size = 30.dp) }
                        }
                    }
                } else {
                    NumberField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = stringResource(
                            if (type == EquipmentType.EXPANDER) R.string.equipment_force else R.string.equipment_weight
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (showErrors && error != null) {
                    Text(stringResource(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (onDelete != null) {
                    TextButton(onClick = { onDismiss(); onDelete() }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (error != null) {
                    showErrors = true
                    return@TextButton
                }
                val base = initial ?: EquipmentEntity(type = type.name)
                val item = base.copy(
                    name = name.trim(),
                    type = type.name,
                    weight = if (type.isBand) null else weightValue,
                    minResistance = if (type.isBand) minValue else null,
                    maxResistance = if (type.isBand) maxValue else null,
                    color = if (type.isBand) color else null,
                )
                repo.launch { saveEquipment(item) }
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
