package com.sonnik.topower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.sonnik.topower.ui.AppRoot
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.theme.SonnikTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as SonnikApp).repository
        setContent {
            SonnikTheme {
                CompositionLocalProvider(LocalRepository provides repository) {
                    AppRoot()
                }
            }
        }
    }
}
