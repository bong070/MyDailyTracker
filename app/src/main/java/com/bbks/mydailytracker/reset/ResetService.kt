package com.bbks.mydailytracker.reset

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.bbks.mydailytracker.data.db.HabitDatabase
import com.bbks.mydailytracker.data.repository.HabitRepository
import com.bbks.mydailytracker.domain.usecase.HabitResetLogic
import com.bbks.mydailytracker.util.ResetLogger
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
            val resetLogic = HabitResetLogic(applicationContext, habitRepo)
            resetLogic.executeReset()
            db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL);")
            ResetLogger.logResetTime(applicationContext)
            ResetLogger.log(applicationContext, "ResetService executeReset() 완료")
            ResetAlarmHelper.scheduleDailyResetAlarm(applicationContext)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
