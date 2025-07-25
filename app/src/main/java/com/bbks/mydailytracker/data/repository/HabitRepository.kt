package com.bbks.mydailytracker.data.repository

import com.bbks.mydailytracker.data.db.DailyHabitResultDao
import com.bbks.mydailytracker.data.db.HabitCheckDao
import com.bbks.mydailytracker.data.db.HabitDao
import com.bbks.mydailytracker.data.model.DailyHabitResult
import com.bbks.mydailytracker.data.model.Habit
import com.bbks.mydailytracker.data.model.HabitCheck
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

class HabitRepository(
    private val habitDao: HabitDao,
    private val habitCheckDao: HabitCheckDao,
    private val resultDao: DailyHabitResultDao
) {
    suspend fun update(habit: Habit) {
        habitDao.update(habit)
    }

    suspend fun delete(habit: Habit) {
        habitDao.delete(habit)
    }

    suspend fun getAllHabitsOnce(): List<Habit> {
        return habitDao.getAllHabitsOnce()
    }

    suspend fun getCheckForHabit(habitId: Int, date: String): HabitCheck? {
        return habitCheckDao.getHabitCheck(habitId, date)
    }

    suspend fun insertHabitCheck(check: HabitCheck) {
        habitCheckDao.insertHabitCheck(check)
    }

    suspend fun deleteChecksForHabit(habitId: Int) {
        habitCheckDao.deleteChecksForHabit(habitId)
    }

    suspend fun saveDailyResult(habitId: Int, date: String, isSuccess: Boolean, habitName: String) {
        val result = DailyHabitResult(
            habitId = habitId,
            date = date,
            isSuccess = isSuccess,
            habitName = habitName
        )
        resultDao.insert(result)
    }

    fun getWeeklyStats(): Flow<List<DailyHabitResult>> {
        val startDate = LocalDate.now().minusYears(1).toString() // 최근 7일 포함
        return resultDao.getResultsFrom(startDate)
    }

    fun getMonthlyStats(yearMonth: YearMonth): Flow<List<DailyHabitResult>> {
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        return resultDao.getResultsBetween(startDate.toString(), endDate.toString())
    }
}