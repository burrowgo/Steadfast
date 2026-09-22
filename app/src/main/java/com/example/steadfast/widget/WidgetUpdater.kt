package com.example.steadfast.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.steadfast.data.db.AppDatabase
import com.example.steadfast.data.prefs.SettingsRepository
import com.example.steadfast.data.prefs.WidgetConfigurationRepository
import com.example.steadfast.data.prefs.WidgetShape
import com.example.steadfast.data.prefs.dataStore
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object WidgetUpdater {
    private const val UNIQUE_WORK_MIDNIGHT = "steadfast_midnight_widget_update"

    /**
     * Update ALL widgets by writing fresh per-widget state from the DB,
     * then triggering Glance recomposition.
     *
     * This is the correct pattern: Glance's provideGlance() is NOT re-executed
     * on updateAll(), so provideContent must read from reactive per-widget state.
     * We write fresh data here so that recomposition picks up the latest values.
     */
    suspend fun updateAll(context: Context) {
        val database = AppDatabase.getInstance(context)
        val widgetConfigRepo = WidgetConfigurationRepository(context)
        val settings = SettingsRepository(context.dataStore).settingsFlow.first()

        // Update all standard widgets
        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(SteadfastWidget::class.java)
            for (glanceId in glanceIds) {
                val appWidgetId = SteadfastWidget.extractAppWidgetId(context, glanceId)
                val habitId = if (appWidgetId > 0) widgetConfigRepo.getHabitIdForWidget(appWidgetId) else null
                val (habit, streak) = SteadfastWidget.resolveForHabitId(database, habitId)
                SteadfastWidget.writeWidgetState(
                    context, glanceId, habit, streak,
                    isCircle = settings.widgetShape == WidgetShape.CIRCLE,
                    opacity = settings.widgetBackgroundOpacity,
                    fontColor = settings.widgetFontColor,
                    bgTheme = settings.widgetBgTheme,
                    showHabitName = settings.widgetShowHabitName
                )
            }
            SteadfastWidget().updateAll(context)
        } catch (_: Exception) {
            // In case standard widget is not yet placed
        }

        // Update all circle widgets
        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(SteadfastCircleWidget::class.java)
            for (glanceId in glanceIds) {
                val appWidgetId = SteadfastWidget.extractAppWidgetId(context, glanceId)
                val habitId = if (appWidgetId > 0) widgetConfigRepo.getHabitIdForWidget(appWidgetId) else null
                val (habit, streak) = SteadfastWidget.resolveForHabitId(database, habitId)
                SteadfastWidget.writeWidgetState(
                    context, glanceId, habit, streak,
                    isCircle = true,
                    opacity = settings.widgetBackgroundOpacity,
                    fontColor = settings.widgetFontColor,
                    bgTheme = settings.widgetBgTheme,
                    showHabitName = settings.widgetShowHabitName
                )
            }
            SteadfastCircleWidget().updateAll(context)
        } catch (_: Exception) {
            // In case circle widget is not yet placed
        }
    }

    fun scheduleMidnightWorker(context: Context) {
        val now = LocalDateTime.now()
        // Midnight of next day plus 1 minute grace buffer
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay().plusMinutes(1)
        val delayMillis = ChronoUnit.MILLIS.between(now, nextMidnight).coerceAtLeast(1000L)

        val workRequest = OneTimeWorkRequestBuilder<MidnightUpdateWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_MIDNIGHT,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
