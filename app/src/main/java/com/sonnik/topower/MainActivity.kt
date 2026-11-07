package com.sonnik.topower

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.ui.AppRoot
import com.sonnik.topower.ui.LocalAppSettings
import com.sonnik.topower.ui.LocalRestTimer
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.muscles.LocalFemaleBody
import com.sonnik.topower.ui.theme.SonnikTheme
import com.sonnik.topower.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SonnikApp
        setContent {
            val mode by app.settings.themeMode.collectAsStateWithLifecycle()
            val palette by app.settings.themePalette.collectAsStateWithLifecycle()
            val pureBlack by app.settings.pureBlack.collectAsStateWithLifecycle()
            val femaleBody by app.settings.femaleBody.collectAsStateWithLifecycle()
            val dark = isDarkTheme(mode)
            // Значки строки состояния — под выбранную тему, а не под системную.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    // Полупрозрачная подложка под кнопками навигации — как по умолчанию в enableEdgeToEdge.
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose {}
            }
            SonnikTheme(mode, palette, pureBlack) {
                CompositionLocalProvider(
                    LocalRepository provides app.repository,
                    LocalAppSettings provides app.settings,
                    LocalRestTimer provides app.restTimer,
                    LocalFemaleBody provides femaleBody,
                ) {
                    AppRoot()
                }
            }
        }
    }
}

private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
