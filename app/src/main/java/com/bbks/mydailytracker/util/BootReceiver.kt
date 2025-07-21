package com.bbks.mydailytracker.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bbks.mydailytracker.alarm.AlarmHelper
import com.bbks.mydailytracker.reset.ResetAlarmHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmHelper.scheduleDailyAlarms(context)
            ResetAlarmHelper.scheduleDailyResetAlarm(context)
        }
    }
}