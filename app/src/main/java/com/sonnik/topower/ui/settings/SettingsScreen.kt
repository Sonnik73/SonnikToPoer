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
import androidx.compose.material3.FilledTonalButton
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
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.ConfirmDialog
import com.sonnik.topower.ui.common.ScreenScaffold
import com.sonnik.topower.ui.common.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun SettingsScreen(onBack: () -> Unit) {
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
            TrainingSettings()
            SectionCard {
                Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.backup_description), style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { exportLauncher.launch("sonniktopower-backup-${LocalDate.now()}.json") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.export_backup)) }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.import_backup)) }
            }
            SectionCard {
                Text(stringResource(R.string.about_title), style = MaterialTheme.typography.titleMedium)
                val version = remember {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
                }
                Text(stringResource(R.string.app_name) + " · " + stringResource(R.string.version, version))
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
private fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean,
    plusEnabled: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FilledTonalButton(onClick = onMinus, enabled = minusEnabled) { Text("−") }
        Text(value, style = MaterialTheme.typography.titleLarge)
        FilledTonalButton(onClick = onPlus, enabled = plusEnabled) { Text("+") }
    }
}
