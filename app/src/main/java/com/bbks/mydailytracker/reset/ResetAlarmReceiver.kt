package com.bbks.mydailytracker.reset

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ResetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ResetAlarmReceiver", "자정 리셋 알람 트리거됨")
        val serviceIntent = Intent(context, ResetService::class.java)
        context.startService(serviceIntent)
    }
}