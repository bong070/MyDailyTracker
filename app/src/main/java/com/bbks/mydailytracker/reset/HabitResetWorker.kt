package com.bbks.mydailytracker.reset

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bbks.mydailytracker.data.db.HabitDatabase
import com.bbks.mydailytracker.data.repository.HabitRepository
import com.bbks.mydailytracker.domain.usecase.HabitResetLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HabitResetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = HabitDatabase.Companion.getDatabase(applicationContext)
        val habitRepository =
            HabitRepository(db.habitDao(), db.habitCheckDao(), db.dailyHabitResultDao())

        val resetLogic = HabitResetLogic(applicationContext, habitRepository)
        val habits =
            HabitDatabase.Companion.getDatabase(applicationContext).habitDao().getAllHabitsOnce()
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