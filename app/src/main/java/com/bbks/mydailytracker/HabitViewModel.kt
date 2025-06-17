package com.bbks.mydailytracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class HabitViewModel(private val habitDao: HabitDao, private val habitCheckDao: HabitCheckDao) : ViewModel() {
    val habits: Flow<List<Habit>> = habitDao.getAllHabits()

    fun addHabit(name: String) {
        viewModelScope.launch {
            habitDao.insert(Habit(name = name))
        }
    }

    suspend fun toggleHabitCheck(habit: Habit, date: String) {
        val existing = habitCheckDao.getHabitCheck(habit.id, date)
        val newCheck = if (existing?.isCompleted == true) {
            HabitCheck(habit.id, date, false)
        } else {
            HabitCheck(habit.id, date, true)
        }
        habitCheckDao.insertHabitCheck(newCheck)
    }
}