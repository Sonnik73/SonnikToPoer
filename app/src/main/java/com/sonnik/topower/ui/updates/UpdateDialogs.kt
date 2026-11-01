package com.sonnik.topower.ui.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.domain.ReleaseInfo
import com.sonnik.topower.domain.Updates
import com.sonnik.topower.ui.common.formatDate
import com.sonnik.topower.updates.UpdateCheckState
import com.sonnik.topower.updates.UpdateChecker
import com.sonnik.topower.updates.UpdateFailure
import kotlinx.coroutines.launch

/** Окна обновлений поверх приложения: предложение новой версии и ход ручной проверки. */
@Composable
fun UpdateDialogs(checker: UpdateChecker, openChangelog: () -> Unit) {
    val scope = rememberCoroutineScope()
    val offer by checker.offer.collectAsStateWithLifecycle()
    val check by checker.check.collectAsStateWithLifecycle()
    offer?.let { release ->
        UpdateOfferDialog(
            release = release,
            current = checker.currentVersion,
            onDownload = { checker.download(release) },
            onSkip = { checker.skip(release) },
            onLater = { checker.offer.value = null },
        )
    }
    check?.let { state ->
        UpdateCheckDialog(
            state = state,
            onRetry = { scope.launch { checker.checkNow() } },
            onChangelog = { checker.check.value = null; openChangelog() },
            onDismiss = { checker.check.value = null },
        )
    }
}

/** Карточка «что установлено / что на GitHub». */
@Composable
private fun VersionCard(rows: List<Pair<String, String>>) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { (label, value) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** «Новая версия»: было → стало, дата и размер, что нового, «Скачать». */
@Composable
fun UpdateOfferDialog(release: ReleaseInfo, current: String, onDownload: () -> Unit, onSkip: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.update_new_title)) },
        text = { UpdateOfferContent(release, current) },
        confirmButton = { Button(onClick = onDownload) { Text(stringResource(R.string.update_download)) } },
        dismissButton = {
            Row {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.update_skip)) }
                TextButton(onClick = onLater) { Text(stringResource(R.string.update_later)) }
            }
        },
    )
}

@Composable
fun UpdateOfferContent(release: ReleaseInfo, current: String) {
    Column(
        Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VersionCard(
            listOfNotNull(
                stringResource(R.string.update_installed) to current,
                stringResource(R.string.update_new) to release.label,
                release.publishedAt?.let { stringResource(R.string.update_released) to formatDate(it) },
                release.apkBytes?.let { stringResource(R.string.update_size) to stringResource(R.string.update_mb, Updates.megabytes(it)) },
            ),
        )
        if (release.notes.isNotBlank()) {
            Text(stringResource(R.string.whats_new), style = MaterialTheme.typography.titleSmall)
            Text(release.notes.replace(Regex("(?m)^- "), "• "), style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            stringResource(R.string.update_install_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Ручная проверка: «Проверяем…», «У вас последняя версия», «Не удалось проверить». */
@Composable
fun UpdateCheckDialog(state: UpdateCheckState, onRetry: () -> Unit, onChangelog: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = if (state == UpdateCheckState.Checking) {
            { CircularProgressIndicator() }
        } else {
            null
        },
        title = {
            Text(
                stringResource(
                    when (state) {
                        UpdateCheckState.Checking -> R.string.update_checking
                        is UpdateCheckState.UpToDate -> R.string.update_up_to_date
                        is UpdateCheckState.Failed -> R.string.update_failed
                    }
                )
            )
        },
        text = { UpdateCheckContent(state) },
        confirmButton = {
            when (state) {
                UpdateCheckState.Checking -> TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_hide)) }
                is UpdateCheckState.UpToDate -> Button(onClick = onDismiss) { Text(stringResource(R.string.done)) }
                is UpdateCheckState.Failed -> Button(onClick = onRetry) { Text(stringResource(R.string.update_retry)) }
            }
        },
        dismissButton = {
            when (state) {
                is UpdateCheckState.UpToDate -> TextButton(onClick = onChangelog) { Text(stringResource(R.string.whats_new)) }
                is UpdateCheckState.Failed -> TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                else -> {}
            }
        },
    )
}

@Composable
fun UpdateCheckContent(state: UpdateCheckState) {
    when (state) {
        UpdateCheckState.Checking -> Text(stringResource(R.string.update_checking_text))
        is UpdateCheckState.UpToDate -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.update_up_to_date_text))
            VersionCard(
                listOfNotNull(
                    stringResource(R.string.update_installed) to state.current,
                    stringResource(R.string.update_on_github) to state.latest.label,
                    state.latest.publishedAt?.let { stringResource(R.string.update_released) to formatDate(it) },
                ),
            )
        }
        is UpdateCheckState.Failed -> Text(
            stringResource(
                when (state.reason) {
                    UpdateFailure.NOT_FOUND -> R.string.update_error_not_found
                    UpdateFailure.RATE_LIMITED -> R.string.update_error_rate_limited
                    UpdateFailure.NETWORK -> R.string.update_error_network
                    UpdateFailure.OTHER -> R.string.update_error_other
                }
            ) + if (state.reason == UpdateFailure.OTHER) " (${state.detail})" else ""
        )
    }
}
