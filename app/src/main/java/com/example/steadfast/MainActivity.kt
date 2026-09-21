package com.example.steadfast

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.steadfast.data.prefs.ThemeMode
import com.example.steadfast.data.prefs.UserSettings
import com.example.steadfast.ui.nav.MainApp
import com.example.steadfast.ui.theme.SteadfastTheme
import com.example.steadfast.widget.SteadfastWidget

class MainActivity : ComponentActivity() {

    private val pendingHabitId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        extractHabitId(intent)

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
                MainApp(
                    initialHabitId = pendingHabitId.value,
                    onHabitHandled = { pendingHabitId.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractHabitId(intent)
    }

    private fun extractHabitId(intent: Intent?) {
        val habitId = intent?.getLongExtra(SteadfastWidget.EXTRA_HABIT_ID, -1L) ?: -1L
        if (habitId > 0) {
            pendingHabitId.value = habitId
        }
    }
}
