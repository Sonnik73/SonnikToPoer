package com.sonnik.topower.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.SonnikApp
import com.sonnik.topower.backup.DriveAuth
import com.sonnik.topower.backup.RemoteBackup
import com.sonnik.topower.ui.common.ConfirmDialog
import com.sonnik.topower.ui.common.SectionCard
import com.sonnik.topower.ui.common.formatDateTime
import kotlinx.coroutines.launch

/** Резервные копии на Google Диске — как в Pin: вход в Google, автокопии, «Сохранить сейчас», восстановление. */
@Composable
fun DriveBackupSettings() {
    val context = LocalContext.current
    val backup = (context.applicationContext as SonnikApp).driveBackup
    val state by backup.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var backups by remember { mutableStateOf<List<RemoteBackup>?>(null) }
    var loadingList by remember { mutableStateOf(false) }
    var toRestore by remember { mutableStateOf<RemoteBackup?>(null) }
    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    val restoredText = stringResource(R.string.drive_restored)
    val savedText = stringResource(R.string.drive_saved)
    val noAccessText = stringResource(R.string.drive_no_access)
    val signInFailed = stringResource(R.string.drive_sign_in_failed, "%s")

    val connect: () -> Unit = { scope.launch { if (backup.connect()) toast(savedText) } }
    val signIn = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        runCatching { DriveAuth.resultFromIntent(context, result.data) }
            .onSuccess { if (it.accessToken != null) connect() else toast(noAccessText) }
            .onFailure { toast(signInFailed.format(DriveAuth.describe(context, it))) }
    }
    val startSignIn: () -> Unit = {
        connectDrive(context, onConnected = connect, onMessage = ::toast) { request -> signIn.launch(request) }
    }

    SectionCard {
        Text(stringResource(R.string.drive_title), style = MaterialTheme.typography.titleMedium)
        FileBackupSection()
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(stringResource(R.string.drive_account_title), style = MaterialTheme.typography.titleSmall)
        if (!state.connected) {
            Text(stringResource(R.string.drive_description), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = startSignIn, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.drive_connect)) }
            return@SectionCard
        }
        Text(
            when {
                state.running -> stringResource(R.string.drive_running)
                state.lastError != null -> stringResource(R.string.drive_error, state.lastError!!)
                state.lastBackupAt == 0L -> stringResource(R.string.drive_never)
                else -> stringResource(R.string.drive_last, formatDateTime(state.lastBackupAt))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.drive_auto_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = { scope.launch { if (backup.backupNow()) toast(savedText) } },
            enabled = !state.running,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.drive_save_now)) }
        OutlinedButton(
            onClick = {
                loadingList = true
                scope.launch {
                    runCatching { backup.list() }
                        .onSuccess { backups = it }
                        .onFailure { toast(it.message ?: it.javaClass.simpleName) }
                    loadingList = false
                }
            },
            enabled = !state.running && !loadingList,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.drive_restore)) }
        if (state.lastError != null) {
            OutlinedButton(onClick = startSignIn, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.drive_sign_in_again_button))
            }
        }
        TextButton(onClick = { backup.disconnect() }) { Text(stringResource(R.string.drive_disconnect)) }
    }

    if (loadingList) {
        AlertDialog(
            onDismissRequest = {},
            icon = { CircularProgressIndicator() },
            title = { Text(stringResource(R.string.drive_loading)) },
            confirmButton = {},
        )
    }
    backups?.let { list ->
        BackupListDialog(list, onPick = { backups = null; toRestore = it }, onDismiss = { backups = null })
    }
    toRestore?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.import_confirm_title),
            text = stringResource(R.string.drive_restore_confirm, formatDateTime(item.modified)),
            confirmText = stringResource(R.string.drive_restore),
            onConfirm = {
                scope.launch {
                    runCatching { backup.restore(item) }
                        .onSuccess { toast(restoredText) }
                        .onFailure { toast(it.message ?: it.javaClass.simpleName) }
                }
            },
            onDismiss = { toRestore = null },
        )
    }
}

@Composable
private fun BackupListDialog(list: List<RemoteBackup>, onPick: (RemoteBackup) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.drive_pick_backup)) },
        text = {
            if (list.isEmpty()) {
                Text(stringResource(R.string.drive_no_backups))
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(list, key = { it.id }) { item ->
                        ListItem(
                            modifier = Modifier.clickable { onPick(item) },
                            headlineContent = { Text(formatDateTime(item.modified)) },
                            supportingContent = item.bytes?.let { { Text(stringResource(R.string.drive_size_kb, (it + 1023) / 1024)) } },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun connectDrive(
    context: Context,
    onConnected: () -> Unit,
    onMessage: (String) -> Unit,
    launch: (IntentSenderRequest) -> Unit,
) {
    val activity = context.findActivity() ?: return
    if (!DriveAuth.playServicesAvailable(context)) {
        onMessage(context.getString(R.string.drive_no_play_services))
        return
    }
    DriveAuth.authorize(activity)
        .addOnSuccessListener { result ->
            val pending = result.pendingIntent
            if (result.hasResolution() && pending != null) {
                launch(IntentSenderRequest.Builder(pending.intentSender).build())
            } else {
                onConnected()
            }
        }
        .addOnFailureListener { onMessage(context.getString(R.string.drive_sign_in_failed, DriveAuth.describe(context, it))) }
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
