package com.bbks.mydailytracker.util

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ResetLogger {
    private const val PREF_NAME = "reset_prefs"
    private const val KEY_LAST_RESET_TIME = "last_reset_time"

    fun logResetTime(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        prefs.edit().putString(KEY_LAST_RESET_TIME, now.format(formatter)).apply()
    }

    fun getLastResetTime(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_RESET_TIME, null)
    }

    fun log(context: Context, message: String) {
        val logFile = File(context.filesDir, "reset_log.txt")
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        logFile.appendText("[$timestamp] $message\n")
    }
}
