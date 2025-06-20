package com.bbks.mydailytracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import kotlin.jvm.java

object AlarmHelper {
    fun scheduleRepeatingAlarm(context: Context, habit: Habit) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        habit.repeatDays.forEach { dayInt ->
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("habit_name", habit.name)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                habit.id * 10 + dayInt, // 고유하게 만들기 위해
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, if (dayInt == 7) 1 else dayInt + 1) // DayOfWeek 기준으로 조정
                set(Calendar.HOUR_OF_DAY, habit.alarmTime?.hour ?: 8)
                set(Calendar.MINUTE, habit.alarmTime?.minute ?: 0)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.WEEK_OF_YEAR, 1)
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
            )
        }
    }

    fun cancelAlarms(context: Context, habit: Habit) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        habit.repeatDays.forEach { dayInt ->
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                habit.id * 10 + dayInt,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
