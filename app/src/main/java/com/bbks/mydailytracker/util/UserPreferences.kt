package com.bbks.mydailytracker.util

import com.bbks.mydailytracker.util.SortOption

data class UserPreferences(
    val endHour: Int = 23,
    val endMinute: Int = 59,
    val alarmEnabled: Boolean = false,
    val autoDelete: Boolean = false,
    val sortOption: SortOption = SortOption.ALPHABETICAL,
    val isPremiumUser: Boolean = false
)