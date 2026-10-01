package com.sonnik.topower.ui.changelog

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sonnik.topower.R
import com.sonnik.topower.ui.common.ScreenScaffold
import com.sonnik.topower.ui.common.SectionCard
import java.util.Locale

/** Раздел журнала изменений: «## 1.4» и пункты «- …». */
data class ChangelogEntry(val version: String, val items: List<String>)

fun parseChangelog(text: String): List<ChangelogEntry> {
    val entries = mutableListOf<ChangelogEntry>()
    var version: String? = null
    val items = mutableListOf<String>()
    fun flush() {
        version?.let { entries += ChangelogEntry(it, items.toList()) }
        items.clear()
    }
    text.lineSequence().map { it.trim() }.forEach { line ->
        when {
            line.startsWith("## ") -> {
                flush()
                version = line.removePrefix("## ").trim()
            }
            line.startsWith("- ") && version != null -> items += line.removePrefix("- ").trim()
        }
    }
    flush()
    return entries
}

/** Журнал на языке системы (русский или английский) из assets. */
fun loadChangelog(context: Context): List<ChangelogEntry> {
    val file = if (Locale.getDefault().language == "ru") "changelog_ru.md" else "changelog_en.md"
    return runCatching {
        context.assets.open(file).bufferedReader().use { parseChangelog(it.readText()) }
    }.getOrDefault(emptyList())
}

@Composable
private fun Bullets(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Row {
                Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(16.dp))
                Text(item, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val entries = remember { loadChangelog(context) }
    ScreenScaffold(title = stringResource(R.string.whats_new), onBack = onBack) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries, key = { it.version }) { entry ->
                SectionCard {
                    Text(
                        stringResource(R.string.version, entry.version),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Bullets(entry.items)
                }
            }
        }
    }
}

/** Окно с новинками текущей версии, показывается один раз после обновления. */
@Composable
fun WhatsNewDialog(entry: ChangelogEntry, onShowAll: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whats_new_in, entry.version)) },
        text = { Bullets(entry.items) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
        dismissButton = {
            TextButton(onClick = { onDismiss(); onShowAll() }) { Text(stringResource(R.string.whats_new_all)) }
        },
    )
}
