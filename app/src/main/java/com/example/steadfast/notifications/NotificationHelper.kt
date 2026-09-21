package com.example.steadfast.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.steadfast.MainActivity
import com.example.steadfast.R
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object NotificationHelper {
    const val CHANNEL_ID = "steadfast_daily_checkin"
    const val CHANNEL_UPDATES_ID = "steadfast_app_updates"
    const val NOTIFICATION_ID = 1001
    const val NOTIFICATION_UPDATE_ID = 1002
    const val UNIQUE_WORK_REMINDER = "steadfast_daily_reminder"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showDailyCheckIn(
        context: Context,
        habitName: String,
        days: Int,
        rankName: String
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bodyText = context.getString(R.string.notification_body, days, rankName)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_ranks)
            .setContentTitle(habitName.ifBlank { context.getString(R.string.app_name) })
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun createUpdatesNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_updates_name)
            val descriptionText = context.getString(R.string.notification_channel_updates_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_UPDATES_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showUpdateAvailableNotification(
        context: Context,
        newVersion: String,
        downloadUrl: String
    ) {
        createUpdatesNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_UPDATE_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = context.getString(R.string.notification_update_title)
        val body = context.getString(R.string.notification_update_body, newVersion)

        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES_ID)
            .setSmallIcon(R.drawable.ic_nav_ranks)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_UPDATE_ID, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun scheduleDailyReminder(context: Context, timeString: String) {
        val time = try {
            LocalTime.parse(timeString)
        } catch (e: Exception) {
            LocalTime.of(20, 0)
        }

        val now = LocalDateTime.now()
        var targetDateTime = now.toLocalDate().atTime(time)
        if (!targetDateTime.isAfter(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val delayMillis = ChronoUnit.MILLIS.between(now, targetDateTime).coerceAtLeast(1000L)

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_REMINDER,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelDailyReminder(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_REMINDER)
        } catch (e: Exception) {
            // Ignore in test environments where WorkManager is not initialized
        }
    }
}
