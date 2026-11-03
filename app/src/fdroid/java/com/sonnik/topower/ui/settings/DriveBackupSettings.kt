package com.sonnik.topower.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sonnik.topower.R
import com.sonnik.topower.ui.common.SectionCard

/** Google Диск в сборке для F-Droid: только копия файлом через окно «Сохранить в…», без входа в Google. */
@Composable
fun DriveBackupSettings() {
    SectionCard {
        Text(stringResource(R.string.drive_title), style = MaterialTheme.typography.titleMedium)
        FileBackupSection()
    }
}
