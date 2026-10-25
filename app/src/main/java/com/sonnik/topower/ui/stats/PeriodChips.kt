package com.sonnik.topower.ui.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sonnik.topower.R
import com.sonnik.topower.domain.StatsPeriod

/** Выбор периода статистики: неделя, месяц, год или всё время. */
@Composable
fun PeriodChips(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatsPeriod.entries.forEach { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onSelect(period) },
                label = { Text(stringResource(periodLabel(period))) },
            )
        }
    }
}

private fun periodLabel(period: StatsPeriod): Int = when (period) {
    StatsPeriod.WEEK -> R.string.period_week
    StatsPeriod.MONTH -> R.string.period_month
    StatsPeriod.YEAR -> R.string.period_year
    StatsPeriod.ALL -> R.string.period_all
}
