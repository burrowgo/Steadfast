package com.example.steadfast.data.updater

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.steadfast.SteadfastApp
import com.example.steadfast.data.prefs.AutoUpdateFrequency
import com.example.steadfast.notifications.NotificationHelper
import kotlinx.coroutines.flow.first

class AutoUpdateCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SteadfastApp ?: return Result.success()
        val settingsRepo = app.container.settingsRepository
        val userSettings = settingsRepo.settingsFlow.first()

        if (userSettings.autoUpdateFrequency == AutoUpdateFrequency.MANUAL) {
            return Result.success()
        }

        val currentVersion = try {
            applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0).versionName ?: "0.7.4"
        } catch (e: Exception) {
            "0.7.4"
        }

        val checker = app.container.updateChecker
        val result = checker.checkForUpdate(currentVersion)
        settingsRepo.setLastUpdateCheckTime(System.currentTimeMillis())

        if (result is UpdateCheckResult.UpdateAvailable) {
            settingsRepo.setPendingUpdate(result)
            NotificationHelper.showUpdateAvailableNotification(
                context = applicationContext,
                newVersion = result.version,
                downloadUrl = result.downloadUrl
            )
        }

        return Result.success()
    }
}
