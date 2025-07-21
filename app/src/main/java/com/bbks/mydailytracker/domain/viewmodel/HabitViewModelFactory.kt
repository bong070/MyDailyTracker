package com.bbks.mydailytracker.domain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bbks.mydailytracker.data.repository.SettingsRepository
import com.bbks.mydailytracker.data.db.HabitCheckDao
import com.bbks.mydailytracker.data.db.HabitDao
import com.bbks.mydailytracker.data.repository.HabitRepository

class HabitViewModelFactory(private val habitDao: HabitDao, private val habitCheckDao: HabitCheckDao, private val settingsRepository: SettingsRepository, private val habitRepository: HabitRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HabitViewModel(habitDao, habitCheckDao, settingsRepository, habitRepository) as T
    }
}