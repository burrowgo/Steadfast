package com.example.steadfast.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.steadfast.SteadfastApp
import com.example.steadfast.domain.RankLadder
import com.example.steadfast.domain.StreakCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate

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

        val active = container.streakRepository.getActiveStreak()
        if (active != null) {
            val nowMillis = container.clock.millis()
            val days = if (active.startedAt > 0L) {
                StreakCalculator.streakDays(active.startedAt, nowMillis)
            } else {
                val today = LocalDate.now(container.clock)
                StreakCalculator.streakDays(LocalDate.ofEpochDay(active.startDate), today)
            }
            val rank = RankLadder.getRankForDays(days)
            val rankName = applicationContext.getString(rank.nameRes)

            NotificationHelper.showDailyCheckIn(
                context = applicationContext,
                habitName = active.habitName,
                days = days,
                rankName = rankName
            )
        }

        // Schedule next reminder for tomorrow
        NotificationHelper.scheduleDailyReminder(applicationContext, settings.reminderTime)

        return Result.success()
    }
}
