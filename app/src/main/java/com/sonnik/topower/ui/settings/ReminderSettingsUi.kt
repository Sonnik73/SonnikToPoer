package com.sonnik.topower.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.data.AppSettings
import com.sonnik.topower.domain.RecoveryNotifyMode
import com.sonnik.topower.domain.ReminderSettings
import com.sonnik.topower.timer.RestTimer
import com.sonnik.topower.ui.LocalAppSettings
import com.sonnik.topower.ui.common.SectionCard
import com.sonnik.topower.ui.muscles.MusclePicker

private fun formatMinutes(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

/** Уведомления о восстановлении мышц, тихие часы и напоминание о перерыве. */
@Composable
fun ReminderSettingsCard() {
    val settings = LocalAppSettings.current
    val r by settings.reminders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun update(value: ReminderSettings) {
        val turnedOn = (value.recoveryOn && !r.recoveryOn) || (value.inactivityOn && !r.inactivityOn)
        if (turnedOn && !RestTimer.canNotify(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        settings.setReminders(value)
    }

    fun pickTime(minutes: Int, onPick: (Int) -> Unit) {
        TimePickerDialog(context, { _, h, m -> onPick(h * 60 + m) }, minutes / 60, minutes % 60, true).show()
    }

    SectionCard {
        Text(stringResource(R.string.reminders_section), style = MaterialTheme.typography.titleMedium)

        SwitchRow(stringResource(R.string.reminder_recovery_on), r.recoveryOn) { update(r.copy(recoveryOn = it)) }
        if (r.recoveryOn) {
            listOf(
                RecoveryNotifyMode.ALL to R.string.reminder_mode_all,
                RecoveryNotifyMode.GROUPS to R.string.reminder_mode_groups,
                RecoveryNotifyMode.DAILY to R.string.reminder_mode_daily,
            ).forEach { (mode, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = r.mode == mode, onClick = { update(r.copy(mode = mode)) }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = r.mode == mode, onClick = null, modifier = Modifier.padding(12.dp))
                    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
                }
            }
            when (r.mode) {
                RecoveryNotifyMode.GROUPS -> MusclePicker(r.groups) { update(r.copy(groups = it)) }
                RecoveryNotifyMode.DAILY -> OutlinedButton(onClick = { pickTime(r.dailyMinutes) { update(r.copy(dailyMinutes = it)) } }) {
                    Text(stringResource(R.string.reminder_daily_time, formatMinutes(r.dailyMinutes)))
                }
                RecoveryNotifyMode.ALL -> Unit
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SwitchRow(stringResource(R.string.reminder_idle_on), r.inactivityOn) { update(r.copy(inactivityOn = it)) }
        if (r.inactivityOn) {
            Text(
                pluralStringResource(R.plurals.reminder_idle_days, r.inactivityDays, r.inactivityDays),
                style = MaterialTheme.typography.bodyMedium,
            )
            Stepper(
                value = r.inactivityDays.toString(),
                onMinus = { update(r.copy(inactivityDays = r.inactivityDays - 1)) },
                onPlus = { update(r.copy(inactivityDays = r.inactivityDays + 1)) },
                minusEnabled = r.inactivityDays > AppSettings.MIN_IDLE_DAYS,
                plusEnabled = r.inactivityDays < AppSettings.MAX_IDLE_DAYS,
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SwitchRow(stringResource(R.string.reminder_quiet_on), r.quietOn) { update(r.copy(quietOn = it)) }
        if (r.quietOn) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.reminder_quiet_from))
                OutlinedButton(onClick = { pickTime(r.quietStart) { update(r.copy(quietStart = it)) } }) {
                    Text(formatMinutes(r.quietStart))
                }
                Text(stringResource(R.string.reminder_quiet_to))
                OutlinedButton(onClick = { pickTime(r.quietEnd) { update(r.copy(quietEnd = it)) } }) {
                    Text(formatMinutes(r.quietEnd))
                }
            }
        }
        Text(
            stringResource(R.string.reminders_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
