package com.example.steadfast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.steadfast.data.prefs.ThemeMode
import com.example.steadfast.data.prefs.UserSettings
import com.example.steadfast.ui.nav.MainApp
import com.example.steadfast.ui.theme.SteadfastTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as SteadfastApp
        val settingsRepo = app.container.settingsRepository

        setContent {
            val settings by settingsRepo.settingsFlow.collectAsStateWithLifecycle(
                initialValue = UserSettings()
            )

            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SteadfastTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.useDynamicColor
            ) {
                MainApp()
            }
        }
    }
}
