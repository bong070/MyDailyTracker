package com.bbks.mydailytracker.data.model

import androidx.room.Entity

@Entity(primaryKeys = ["habitId", "date"])
data class HabitCheck(
    val habitId: Int,
    val date: String,
    val isCompleted: Boolean
)