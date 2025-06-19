package com.bbks.mydailytracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bbks.mydailytracker.data.SettingsRepository

class HabitViewModelFactory(private val habitDao: HabitDao, private val habitCheckDao: HabitCheckDao, private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HabitViewModel(habitDao, habitCheckDao, settingsRepository) as T
    }
}