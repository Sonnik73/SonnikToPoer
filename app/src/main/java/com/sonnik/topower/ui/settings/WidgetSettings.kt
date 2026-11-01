package com.sonnik.topower.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sonnik.topower.R
import com.sonnik.topower.ui.common.SectionCard
import com.sonnik.topower.widget.MuscleWidget
import com.sonnik.topower.widget.WidgetLook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Виджет на рабочем столе: прозрачность фона — как в Pin. */
@Composable
fun WidgetSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var opacity by remember { mutableIntStateOf(WidgetLook.opacity(context)) }

    SectionCard {
        Text(stringResource(R.string.widget_section), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.widget_opacity), style = MaterialTheme.typography.bodyLarge)
                Text(
                    when {
                        opacity >= 100 -> stringResource(R.string.widget_opacity_solid)
                        opacity <= 0 -> stringResource(R.string.widget_opacity_none)
                        else -> stringResource(R.string.widget_opacity_value, opacity)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OpacitySample(opacity)
        }
        Slider(
            value = opacity.toFloat(),
            onValueChange = {
                opacity = (it / WidgetLook.STEP).toInt() * WidgetLook.STEP
                WidgetLook.setOpacity(context, opacity)
            },
            onValueChangeFinished = { scope.launch(Dispatchers.IO) { MuscleWidget.refresh(context) } },
            valueRange = 0f..100f,
        )
    }
}

/** Образец: фон виджета на «обоях» с переливом и полоски цветов восстановления. */
@Composable
private fun OpacitySample(opacity: Int) {
    Box(
        Modifier
            .size(56.dp)
            .background(Brush.linearGradient(listOf(Color(0xFF3A5A8C), Color(0xFFE08A3C))), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .size(42.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = opacity / 100f), RoundedCornerShape(10.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            listOf(Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF43A047)).forEach { c ->
                Box(Modifier.width(26.dp).height(5.dp).background(c, RoundedCornerShape(3.dp)))
            }
        }
    }
}
