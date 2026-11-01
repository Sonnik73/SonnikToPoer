package com.sonnik.topower.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.data.AppSettings
import com.sonnik.topower.domain.formatDuration
import com.sonnik.topower.ui.LocalAppSettings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sonnik.topower.R
import com.sonnik.topower.SonnikApp
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.ConfirmDialog
import com.sonnik.topower.ui.common.ScreenScaffold
import com.sonnik.topower.ui.common.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sonnik.topower.ui.share.fileTimestamp
import java.util.Locale
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.ui.muscles.muscleNameRes

@Composable
fun SettingsScreen(onBack: () -> Unit, openChangelog: () -> Unit) {
    val repo = LocalRepository.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val exportDone = stringResource(R.string.export_done)
    val importDone = stringResource(R.string.import_done)
    val errorTemplate = stringResource(R.string.backup_error, "%s")
    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = repo.exportBackup()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
                        ?: error("Cannot open file")
                }
                toast(exportDone)
            } catch (e: Exception) {
                toast(errorTemplate.format(e.message ?: e.javaClass.simpleName))
            }
        }
    }

    val csvHeader = stringResource(R.string.csv_header).split('|')
    val warmupMark = stringResource(R.string.warmup_short)
    val yesMark = stringResource(R.string.csv_yes)
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val csv = repo.exportCsv(
                    header = csvHeader,
                    decimalComma = Locale.getDefault().language == "ru",
                    warmupMark = warmupMark,
                    yesMark = yesMark,
                    muscleNames = { text -> Muscle.parse(text).sortedBy { it.ordinal }.joinToString(", ") { context.getString(muscleNameRes(it)) } },
                )
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(csv.toByteArray()) }
                        ?: error("Cannot open file")
                }
                toast(exportDone)
            } catch (e: Exception) {
                toast(errorTemplate.format(e.message ?: e.javaClass.simpleName))
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }

    ScreenScaffold(title = stringResource(R.string.nav_settings), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppearanceSettings()
            TrainingSettings()
            ReminderSettingsCard()
            WidgetSettings()
            SectionCard {
                Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.backup_description), style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { exportLauncher.launch("SonnikToPower-backup-${fileTimestamp()}.json") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.export_backup)) }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.import_backup)) }
                OutlinedButton(
                    onClick = { csvLauncher.launch("SonnikToPower-${fileTimestamp()}.csv") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.export_csv)) }
                Text(
                    stringResource(R.string.export_csv_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DriveBackupSettings()
            SectionCard {
                Text(stringResource(R.string.about_title), style = MaterialTheme.typography.titleMedium)
                val version = remember {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
                }
                Text(stringResource(R.string.app_name) + " · " + stringResource(R.string.version, version))
                OutlinedButton(onClick = openChangelog, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.whats_new))
                }
                UpdateSettings()
                Text(
                    stringResource(R.string.language_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.import_confirm_title),
            text = stringResource(R.string.import_confirm_text),
            confirmText = stringResource(R.string.import_backup),
            onConfirm = {
                scope.launch {
                    try {
                        val json = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                                ?: error("Cannot open file")
                        }
                        repo.importBackup(json)
                        toast(importDone)
                    } catch (e: Exception) {
                        toast(errorTemplate.format(e.message ?: e.javaClass.simpleName))
                    }
                }
            },
            onDismiss = { pendingImport = null },
        )
    }
}

/** Таймер отдыха и подсказка о застое. */
@Composable
private fun TrainingSettings() {
    val settings = LocalAppSettings.current
    val rest by settings.defaultRestSec.collectAsStateWithLifecycle()
    val plateauOn by settings.plateauEnabled.collectAsStateWithLifecycle()
    val plateauN by settings.plateauSessions.collectAsStateWithLifecycle()

    SectionCard {
        Text(stringResource(R.string.rest_section), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.rest_default), style = MaterialTheme.typography.bodyMedium)
        Stepper(
            value = formatDuration(rest),
            onMinus = { settings.setDefaultRestSec(rest - AppSettings.REST_STEP_SEC) },
            onPlus = { settings.setDefaultRestSec(rest + AppSettings.REST_STEP_SEC) },
            minusEnabled = rest > AppSettings.MIN_REST_SEC,
            plusEnabled = rest < AppSettings.MAX_REST_SEC,
        )
        Text(
            stringResource(R.string.rest_default_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    SectionCard {
        Text(stringResource(R.string.plateau_section), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.plateau_enabled), modifier = Modifier.weight(1f))
            Switch(checked = plateauOn, onCheckedChange = { settings.setPlateauEnabled(it) })
        }
        if (plateauOn) {
            Text(stringResource(R.string.plateau_sessions, plateauN), style = MaterialTheme.typography.bodyMedium)
            Stepper(
                value = plateauN.toString(),
                onMinus = { settings.setPlateauSessions(plateauN - 1) },
                onPlus = { settings.setPlateauSessions(plateauN + 1) },
                minusEnabled = plateauN > AppSettings.MIN_PLATEAU_SESSIONS,
                plusEnabled = plateauN < AppSettings.MAX_PLATEAU_SESSIONS,
            )
        }
        Text(
            stringResource(R.string.plateau_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean,
    plusEnabled: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FilledTonalIconButton(onClick = onMinus, enabled = minusEnabled) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(value, style = MaterialTheme.typography.titleLarge)
        FilledTonalIconButton(onClick = onPlus, enabled = plusEnabled) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Обновления: автопроверка при входе и кнопка «Проверить обновления» (окна — в AppRoot). */
@Composable
private fun UpdateSettings() {
    val updates = (LocalContext.current.applicationContext as SonnikApp).updates
    val enabled by updates.enabled.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    OutlinedButton(onClick = { scope.launch { updates.checkNow() } }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.update_check_now))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.update_auto), modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = { updates.setEnabled(it) })
    }
}
