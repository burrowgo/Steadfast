package com.example.steadfast.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.steadfast.SteadfastApp
import kotlinx.coroutines.flow.first

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SteadfastApp ?: return Result.failure()
        val container = app.container

        val settings = container.settingsRepository.settingsFlow.first()
        if (!settings.reminderEnabled) {
            return Result.success()
        }

        val activeHabits = container.habitRepository.activeHabitsWithStreaks.first()
        if (activeHabits.isNotEmpty()) {
            val primary = activeHabits.first()
            val rankName = applicationContext.getString(primary.currentRank.nameRes)

            NotificationHelper.showDailyCheckIn(
                context = applicationContext,
                habitName = primary.habit.name,
                days = primary.currentStreakDays,
                rankName = rankName
            )
        }

        // Schedule next reminder for tomorrow
        NotificationHelper.scheduleDailyReminder(applicationContext, settings.reminderTime)

        return Result.success()
    }
}
