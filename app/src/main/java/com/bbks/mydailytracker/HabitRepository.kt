package com.bbks.mydailytracker

import com.bbks.mydailytracker.Habit
import com.bbks.mydailytracker.HabitCheck
import com.bbks.mydailytracker.HabitCheckDao
import com.bbks.mydailytracker.HabitDao
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class HabitRepository(
    private val habitDao: HabitDao,
    private val habitCheckDao: HabitCheckDao,
    private val resultDao: DailyHabitResultDao
) {
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()

    suspend fun insert(habit: Habit) {
        habitDao.insert(habit)
    }

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
        val result = DailyHabitResult(habitId = habitId, date = date, isSuccess = isSuccess, habitName = habitName)
        resultDao.insert(result)
    }

    suspend fun getResultsForHabit(habitId: Int): List<DailyHabitResult> {
        return resultDao.getResultsForHabit(habitId)
    }

    suspend fun getResultsInRange(startDate: String, endDate: String): List<DailyHabitResult> {
        return resultDao.getResultsInRange(startDate, endDate)
    }

    fun getWeeklyStats(): Flow<List<DailyHabitResult>> {
        val startDate = LocalDate.now().minusDays(6).toString() // 최근 7일 포함
        return resultDao.getResultsFrom(startDate)
    }
}
