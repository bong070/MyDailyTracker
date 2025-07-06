package com.bbks.mydailytracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bbks.mydailytracker.R
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_habit_channel"
        val db = HabitDatabase.getDatabase(context)
        val resultDao = db.dailyHabitResultDao()

        val today = LocalDate.now().toString()

        val (completedCount, totalCount) = runBlocking {
            val results = resultDao.getResultsForDate(today)
            val completed = results.count { it.isSuccess }
            Pair(completed, results.size)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Daily Habit Reminder", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val contentText = context.getString(R.string.today_completed, completedCount, totalCount)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("My Daily Tracker")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
