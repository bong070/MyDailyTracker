package com.bbks.mydailytracker.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bbks.mydailytracker.alarm.AlarmService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitTitle = intent.getStringExtra("habitTitle") ?: "알람이 울리고 있어요!"
        val habitId = intent.getIntExtra("habitId", -1)
        val dayOfWeek = intent.getIntExtra("dayOfWeek", -1)
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("habitTitle", habitTitle)
            putExtra("habitId", habitId)
            putExtra("dayOfWeek", dayOfWeek)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}