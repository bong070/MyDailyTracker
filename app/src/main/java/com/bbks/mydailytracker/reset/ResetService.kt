package com.bbks.mydailytracker.reset

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.bbks.mydailytracker.HabitDatabase
import com.bbks.mydailytracker.HabitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResetService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CoroutineScope(Dispatchers.IO).launch {
            val db = HabitDatabase.getDatabase(applicationContext)
            val habitRepo = HabitRepository(
                db.habitDao(), db.habitCheckDao(), db.dailyHabitResultDao()
            )
            val resetLogic = com.bbks.mydailytracker.HabitResetLogic(habitRepo)
            resetLogic.executeReset()
            Log.d("ResetService", "executeReset() 완료")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
