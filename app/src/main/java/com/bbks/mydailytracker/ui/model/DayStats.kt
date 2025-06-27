package com.bbks.mydailytracker.model

import java.time.LocalDate

data class DayStats(
    val date: LocalDate,
    val label: String,
    val success: Int,
    val failure: Int,
    val failedHabits: List<String>,
    val successHabits: List<String>
)