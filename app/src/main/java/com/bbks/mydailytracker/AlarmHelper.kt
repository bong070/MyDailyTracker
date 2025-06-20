package com.bbks.mydailytracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import java.util.Calendar
import kotlin.jvm.java
import com.bbks.mydailytracker.NotificationReceiver

object AlarmHelper {
    fun scheduleDailyAlarms(context: Context) {
        val times = listOf(12 to 0, 18 to 0) // 오후 12시, 6시

        times.forEachIndexed { index, (hour, minute) ->
            val alarmTime = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
            }

            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                index, // requestCode 고유화
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime.timeInMillis,
                            pendingIntent
                        )
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                        // 사용자에게 알람 권한이 필요하다는 안내를 할 수도 있음
                    }
                } else {
                    // 사용자 설정에서 정확한 알람 권한을 수동으로 허용해야 함
                    Toast.makeText(context, "정확한 알람 권한이 필요합니다", Toast.LENGTH_SHORT).show()
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelAllAlarms(context: Context) {
        (0..1).forEach { index ->
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }
}
