package com.example.steadfast.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.steadfast.R
import com.example.steadfast.SteadfastApp
import com.example.steadfast.data.prefs.ThemeMode
import com.example.steadfast.data.prefs.UserSettings
import com.example.steadfast.domain.model.HabitWithStreak
import com.example.steadfast.ui.components.HabitCard
import com.example.steadfast.ui.theme.SteadfastTheme
import kotlinx.coroutines.launch

class WidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set result to CANCELED so that backing out aborts widget placement
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val app = application as SteadfastApp
        val container = app.container

        setContent {
            val settings by container.settingsRepository.settingsFlow.collectAsStateWithLifecycle(
                initialValue = UserSettings()
            )

            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val habitsWithStreaks by container.habitRepository.activeHabitsWithStreaks.collectAsStateWithLifecycle(
                initialValue = emptyList()
            )

            val scope = rememberCoroutineScope()

            SteadfastTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.useDynamicColor
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.widget_configure_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { innerPadding ->
                    WidgetConfigureContent(
                        habits = habitsWithStreaks,
                        onHabitSelected = { item ->
                            container.widgetConfigurationRepository.setHabitIdForWidget(appWidgetId, item.habit.id)
                            lifecycleScope.launch {
                                try {
                                    val manager = GlanceAppWidgetManager(this@WidgetConfigureActivity)
                                    val glanceId = manager.getGlanceIdBy(appWidgetId)
                                    updateAppWidgetState(this@WidgetConfigureActivity, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                                        prefs.toMutablePreferences().apply {
                                            this[SteadfastWidget.KEY_HABIT_ID] = item.habit.id
                                        }
                                    }
                                    SteadfastWidget().update(this@WidgetConfigureActivity, glanceId)
                                    SteadfastCircleWidget().update(this@WidgetConfigureActivity, glanceId)
                                } catch (e: Exception) {
                                    // Widget ID might not be mapped yet by launcher
                                }

                                WidgetUpdater.updateAll(this@WidgetConfigureActivity)
                                val resultValue = Intent().apply {
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                }
                                setResult(Activity.RESULT_OK, resultValue)
                                finish()
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetConfigureContent(
    habits: List<HabitWithStreak>,
    onHabitSelected: (HabitWithStreak) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.widget_configure_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (habits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.widget_configure_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(habits, key = { it.habit.id }) { item ->
                        HabitCard(
                            item = item,
                            onClick = { onHabitSelected(item) }
                        )
                    }
                }
            }
        }
    }
}
