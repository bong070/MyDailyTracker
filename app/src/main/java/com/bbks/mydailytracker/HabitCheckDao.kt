package com.bbks.mydailytracker

import androidx.room.*

@Dao
interface HabitCheckDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitCheck(habitCheck: HabitCheck)

    @Query("SELECT * FROM HabitCheck WHERE habitId = :habitId AND date = :date")
    suspend fun getHabitCheck(habitId: Int, date: String): HabitCheck?

    @Query("DELETE FROM HabitCheck WHERE habitId = :habitId")
    suspend fun deleteChecksForHabit(habitId: Int)
}
