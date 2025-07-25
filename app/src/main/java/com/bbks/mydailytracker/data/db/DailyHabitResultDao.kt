package com.bbks.mydailytracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bbks.mydailytracker.data.model.DailyHabitResult
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyHabitResultDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(result: DailyHabitResult)

    @Query("SELECT * FROM daily_habit_results WHERE date = :date")
    suspend fun getResultsForDate(date: String): List<DailyHabitResult>

    @Query("SELECT * FROM daily_habit_results WHERE habitId = :habitId ORDER BY date DESC")
    suspend fun getResultsForHabit(habitId: Int): List<DailyHabitResult>

    @Query("SELECT * FROM daily_habit_results WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getResultsInRange(startDate: String, endDate: String): List<DailyHabitResult>

    @Query("DELETE FROM daily_habit_results WHERE habitId = :habitId")
    suspend fun deleteResultsForHabit(habitId: Int)

    @Query("SELECT * FROM daily_habit_results WHERE date >= :startDate ORDER BY date ASC")
    fun getResultsFrom(startDate: String): Flow<List<DailyHabitResult>>

    @Query("SELECT * FROM daily_habit_results WHERE date BETWEEN :start AND :end")
    fun getResultsBetween(start: String, end: String): Flow<List<DailyHabitResult>>
}