package com.bbks.mydailytracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class HabitRefreshReceiver(private val onRefresh: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.bbks.mydailytracker.HABITS_REFRESH") {
            onRefresh()
        }
    }
}
