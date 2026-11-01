package com.sonnik.topower.ui.settings

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.SonnikApp
import com.sonnik.topower.backup.FileBackupWorker
import com.sonnik.topower.data.BackupCodec
import com.sonnik.topower.ui.common.formatDateTime
import kotlinx.coroutines.launch

private const val FILE_NAME = "SonnikToPower.json"
private val JSON_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")

/** В файле, который подключают, уже есть копия: восстановить из неё или перезаписать. */
private data class FoundBackup(val uri: Uri, val text: String, val exportedAt: Long, val workouts: Int)

/**
 * Копия одним файлом, как в Pin: файл создаётся через окно «Сохранить в…» (выбрать Google Диск),
 * дальше приложение само перезаписывает его после изменений. Без входа в Google и Google Cloud.
 */
@Composable
fun ColumnScope.FileBackupSection() {
    val context = LocalContext.current
    val fileBackup = (context.applicationContext as SonnikApp).fileBackup
    val state by fileBackup.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var found by remember { mutableStateOf<FoundBackup?>(null) }
    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    val createdText = stringResource(R.string.file_backup_created)
    val attachedText = stringResource(R.string.file_backup_attached)
    val savedText = stringResource(R.string.file_backup_saved)
    val restoredText = stringResource(R.string.drive_restored)

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            FileBackupWorker.attach(context, uri)
            toast(createdText)
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = fileBackup.readBackup(uri)
            if (text == null) {
                // Пустой или чужой файл — просто пишем в него.
                FileBackupWorker.attach(context, uri)
                toast(attachedText)
            } else {
                val backup = BackupCodec.decode(text)
                found = FoundBackup(uri, text, backup.exportedAt, backup.workouts.size)
            }
        }
    }

    Text(stringResource(R.string.file_backup_title), style = MaterialTheme.typography.titleSmall)
    if (!state.enabled) {
        Text(stringResource(R.string.file_backup_description), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { create.launch(FILE_NAME) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.file_backup_create))
        }
        OutlinedButton(onClick = { open.launch(JSON_TYPES) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.file_backup_open))
        }
        Text(
            stringResource(R.string.file_backup_open_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        val name = remember(state.uri) { displayName(context, state.uri) ?: FILE_NAME }
        Text(
            when {
                state.error != null -> stringResource(R.string.drive_error, state.error!!)
                state.lastAt == 0L -> stringResource(R.string.file_backup_connected, name)
                else -> stringResource(R.string.file_backup_last, name, formatDateTime(state.lastAt))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.file_backup_auto_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = { scope.launch { if (fileBackup.write(force = true)) toast(savedText) } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.file_backup_save_now)) }
        if (state.error != null) {
            OutlinedButton(onClick = { open.launch(JSON_TYPES) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.file_backup_pick_again))
            }
        }
        TextButton(onClick = { FileBackupWorker.detach(context) }) { Text(stringResource(R.string.file_backup_detach)) }
    }

    found?.let { f ->
        AlertDialog(
            onDismissRequest = { found = null },
            title = { Text(stringResource(R.string.file_backup_found_title)) },
            text = { Text(stringResource(R.string.file_backup_found_text, formatDateTime(f.exportedAt), f.workouts)) },
            confirmButton = {
                TextButton(onClick = {
                    found = null
                    scope.launch {
                        runCatching { fileBackup.restore(f.text) }
                            .onSuccess {
                                FileBackupWorker.attach(context, f.uri, writeNow = false)
                                toast(restoredText)
                            }
                            .onFailure { toast(it.message ?: it.javaClass.simpleName) }
                    }
                }) { Text(stringResource(R.string.file_backup_restore)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    found = null
                    FileBackupWorker.attach(context, f.uri)
                    toast(attachedText)
                }) { Text(stringResource(R.string.file_backup_overwrite)) }
            },
        )
    }
}

private fun displayName(context: android.content.Context, uri: String?): String? = uri?.let {
    runCatching {
        context.contentResolver.query(Uri.parse(it), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()
}
