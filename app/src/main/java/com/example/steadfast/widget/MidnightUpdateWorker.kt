package com.example.steadfast.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MidnightUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        WidgetUpdater.updateAll(applicationContext)
        // Re-enqueue for the following midnight
        WidgetUpdater.scheduleMidnightWorker(applicationContext)
        return Result.success()
    }
}
