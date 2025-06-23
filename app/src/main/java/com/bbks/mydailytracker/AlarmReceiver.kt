package com.bbks.mydailytracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitTitle = intent.getStringExtra("habitTitle") ?: "알람이 울리고 있어요!"
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("habitTitle", habitTitle)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}