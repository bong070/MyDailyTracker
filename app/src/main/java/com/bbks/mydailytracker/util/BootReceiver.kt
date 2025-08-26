package com.bbks.mydailytracker.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bbks.mydailytracker.alarm.AlarmHelper
import com.bbks.mydailytracker.data.db.HabitDatabase
import com.bbks.mydailytracker.reset.ResetAlarmHelper
import com.bbks.mydailytracker.ui.screen.scheduleWeeklyAlarms
import kotlinx.coroutines.launch
import android.app.AlarmManager
import android.app.PendingIntent
import com.bbks.mydailytracker.alarm.AlarmReceiver
import com.bbks.mydailytracker.alarm.OneShotStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        val triggers = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )

        if (action !in triggers) return

        val pending = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            try {
                AlarmHelper.scheduleDailyAlarms(context)
                ResetAlarmHelper.scheduleDailyResetAlarm(context)

                val db = HabitDatabase.getDatabase(context.applicationContext)
                val habits = db.habitDao().getAllHabitsOnce()

                val nowMillis = System.currentTimeMillis()
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                habits.forEach { h ->
                    val days: List<Int> = h.repeatDays ?: emptyList()

                    if (days.isNotEmpty()) {
                        // 반복 알람 재등록
                        val hour: Int = h.alarmHour ?: run {
                            Log.w("BootReceiver", "skip habit=${h.id} : hour=null"); return@forEach
                        }
                        val minute: Int = h.alarmMinute ?: run {
                            Log.w("BootReceiver", "skip habit=${h.id} : minute=null"); return@forEach
                        }
                        val title: String = h.name ?: ""

                        scheduleWeeklyAlarms(
                            context = context,
                            habitId = h.id,
                            hour = hour,
                            minute = minute,
                            repeatDays = days,
                            habitTitle = title
                        )
                    } else {
                        val at = OneShotStore.get(context.applicationContext, h.id)
                        if (at != null && at > nowMillis) {
                            val pi = PendingIntent.getBroadcast(
                                context,
                                h.id * 10 + 0,  // day=0 (one-shot 식별)
                                Intent(context, AlarmReceiver::class.java).apply {
                                    putExtra("habitId", h.id)
                                    putExtra("habitTitle", h.name ?: "")
                                    putExtra("dayOfWeek", 0)
                                },
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            try {
                                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                                Log.d("BootReceiver", "재등록(단발): habit=${h.id} at=$at")
                            } catch (se: SecurityException) {
                                Log.w("BootReceiver", "단발 재등록 실패(security): ${se.message}")
                            }
                        } else {
                            // 과거거나 저장값 없음 → 재등록 대상 아님. 저장값은 정리.
                            OneShotStore.clear(context.applicationContext, h.id)
                            Log.d("BootReceiver", "스킵/정리(단발): habit=${h.id}")
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}