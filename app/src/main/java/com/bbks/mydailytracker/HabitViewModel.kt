package com.bbks.mydailytracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

class HabitViewModel(
    private val habitDao: HabitDao,
    private val habitCheckDao: HabitCheckDao
) : ViewModel() {

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits

    private val _habitChecks = MutableStateFlow<List<HabitCheck>>(emptyList())
    val habitChecks: StateFlow<List<HabitCheck>> = _habitChecks

    private val _endTime = MutableStateFlow(LocalTime.of(23, 59, 59))
    val endTime: StateFlow<LocalTime> = _endTime

    fun setEndTime(newTime: LocalTime) {
        _endTime.value = newTime
    }

    init {
        loadHabits()
        loadHabitChecks()
    }

    private fun loadHabits() {
        viewModelScope.launch {
            habitDao.getAllHabits().collect { loaded ->
                _habits.value = loaded
            }
        }
    }

    private fun loadHabitChecks() {
        viewModelScope.launch {
            val allHabits = _habits.value
            val allChecks = mutableListOf<HabitCheck>()
            for (habit in allHabits) {
                val today = java.time.LocalDate.now().toString()
                habitCheckDao.getHabitCheck(habit.id, today)?.let {
                    allChecks.add(it)
                }
            }
            _habitChecks.value = allChecks
        }
    }

    fun addHabit(name: String) {
        viewModelScope.launch {
            habitDao.insert(Habit(name = name))
            loadHabits()
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.delete(habit)
            habitCheckDao.deleteChecksForHabit(habit.id)
            loadHabits()
            loadHabitChecks()
        }
    }

    suspend fun toggleHabitCheck(habit: Habit, date: String) {
        val existing = habitCheckDao.getHabitCheck(habit.id, date)
        if (existing == null) {
            habitCheckDao.insertHabitCheck(HabitCheck(habit.id, date, true))
        } else {
            habitCheckDao.deleteChecksForHabit(habit.id)  // 또는 delete(existing)
        }
        loadHabitChecks()
    }
}