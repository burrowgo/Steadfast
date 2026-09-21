package com.example.steadfast.data.updater

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.steadfast.data.prefs.AutoUpdateFrequency
import java.util.concurrent.TimeUnit

object AutoUpdateScheduler {
    const val UNIQUE_WORK_AUTO_UPDATE = "steadfast_periodic_update_check"

    fun schedule(context: Context, frequency: AutoUpdateFrequency) {
        val workManager = WorkManager.getInstance(context)

        if (frequency == AutoUpdateFrequency.MANUAL) {
            workManager.cancelUniqueWork(UNIQUE_WORK_AUTO_UPDATE)
            return
        }

        val repeatIntervalDays = when (frequency) {
            AutoUpdateFrequency.DAILY -> 1L
            AutoUpdateFrequency.WEEKLY -> 7L
            AutoUpdateFrequency.MANUAL -> return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AutoUpdateCheckWorker>(
            repeatIntervalDays,
            TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_AUTO_UPDATE,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
