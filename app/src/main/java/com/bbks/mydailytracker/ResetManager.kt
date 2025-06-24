package com.bbks.mydailytracker

import android.content.Context
import java.time.LocalDate

class ResetManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("reset_prefs", Context.MODE_PRIVATE)

    fun shouldExecuteReset(): Boolean {
        val today = LocalDate.now().toString()
        val lastReset = prefs.getString("last_reset_date", null)
        return today != lastReset
    }

    fun markResetDone() {
        val today = LocalDate.now().toString()
        prefs.edit().putString("last_reset_date", today).apply()
    }
}
