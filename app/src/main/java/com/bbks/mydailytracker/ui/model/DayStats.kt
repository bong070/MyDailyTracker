package com.bbks.mydailytracker.model

data class DayStats(
    val label: String,
    val success: Int,
    val failure: Int,
    val failedHabits: List<String>
)