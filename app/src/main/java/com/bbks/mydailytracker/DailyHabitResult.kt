package com.bbks.mydailytracker

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_habit_results",
    indices = [Index(value = ["date"]), Index(value = ["habitId"])]
)
data class DailyHabitResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val date: String, // "yyyy-MM-dd"
    val isSuccess: Boolean,
    val habitName: String
)