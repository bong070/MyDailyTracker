package com.bbks.mydailytracker.data

import SortOption

data class UserPreferences(
    val endHour: Int = 23,
    val endMinute: Int = 59,
    val alarmEnabled: Boolean = false,
    val autoDelete: Boolean = false,
    val sortOption: SortOption = SortOption.ALPHABETICAL,
)
