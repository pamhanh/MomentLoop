package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Project

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Retrieve settings to check if reminder is enabled
        val sharedPrefs = applicationContext.getSharedPreferences("journey_lens_prefs", Context.MODE_PRIVATE)
        val remindersEnabled = sharedPrefs.getBoolean("reminders_enabled", true)
        if (!remindersEnabled) {
            return Result.success()
        }

        val db = AppDatabase.getDatabase(applicationContext)
        val projects = db.projectDao().getAllProjects()

        if (projects.isEmpty()) {
            return Result.success()
        }

        // Find the project with the lowest progress or oldest update
        val targetProject = projects.minByOrNull { it.progressPercent } ?: projects.first()

        sendNotification(targetProject)
        return Result.success()
    }

    private fun sendNotification(project: Project) {
        val channelId = "daily_reminders_channel"
        val notificationId = 101

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to capture moments of your journey daily"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "JourneyLens"
        val body = "Don't forget your ${project.title} journey — snap a moment today! 📸"

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_camera) // standard fallback
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
