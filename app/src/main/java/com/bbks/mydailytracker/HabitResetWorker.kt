package com.bbks.mydailytracker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HabitResetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = HabitDatabase.getDatabase(applicationContext)
        val habitRepository = HabitRepository(db.habitDao(), db.habitCheckDao(), db.dailyHabitResultDao())

        val resetLogic = HabitResetLogic(habitRepository)
        val habits = HabitDatabase.getDatabase(applicationContext).habitDao().getAllHabitsOnce()
        Log.d("ResetWorker", "DB에 있는 습관 개수: ${habits.size}")
        resetLogic.executeReset()
        val intent = Intent("com.bbks.mydailytracker.HABITS_REFRESH").apply {
            setPackage(applicationContext.packageName) // 외부 노출 방지
        }
        applicationContext.sendBroadcast(intent)

        Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val work = OneTimeWorkRequestBuilder<HabitResetWorker>().build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }
}
