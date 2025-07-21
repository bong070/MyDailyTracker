package com.bbks.mydailytracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bbks.mydailytracker.data.model.HabitCheck

@Dao
interface HabitCheckDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertHabitCheck(habitCheck: HabitCheck)

    @Query("SELECT * FROM HabitCheck WHERE habitId = :habitId AND date = :date")
    suspend fun getHabitCheck(habitId: Int, date: String): HabitCheck?

    @Query("DELETE FROM HabitCheck WHERE habitId = :habitId")
    suspend fun deleteChecksForHabit(habitId: Int)

    @Update
    suspend fun updateHabitCheck(check: HabitCheck)

    @Query("SELECT * FROM HabitCheck WHERE date = :date")
    suspend fun getChecksForDate(date: String): List<HabitCheck>
}