package com.bbks.mydailytracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bbks.mydailytracker.reset.ResetAlarmHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmHelper.scheduleDailyAlarms(context)
            ResetAlarmHelper.scheduleDailyResetAlarm(context)
        }
    }
}