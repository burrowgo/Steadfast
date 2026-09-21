package com.example.steadfast.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object WidgetUpdater {
    private const val UNIQUE_WORK_MIDNIGHT = "steadfast_midnight_widget_update"

    suspend fun updateAll(context: Context) {
        try {
            SteadfastWidget().updateAll(context)
        } catch (e: Exception) {
            // In case standard widget is not yet placed
        }
        try {
            SteadfastCircleWidget().updateAll(context)
        } catch (e: Exception) {
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
