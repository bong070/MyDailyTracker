package com.bbks.mydailytracker

import SortOption
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class HabitViewModel(
    private val habitDao: HabitDao,
    private val habitCheckDao: HabitCheckDao
) : ViewModel() {

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits

    private val _habitChecks = MutableStateFlow<Map<Int, HabitCheck>>(emptyMap())
    val habitChecks: StateFlow<Map<Int, HabitCheck>> = _habitChecks

    private val _endTime = MutableStateFlow(LocalTime.of(23, 59, 59))
    val endTime: StateFlow<LocalTime> = _endTime

    private val _sortOption = MutableStateFlow(SortOption.ALPHABETICAL)
    val sortOption: StateFlow<SortOption> = _sortOption

    private val today: String = LocalDate.now().toString()

    val sortedHabits = combine(habits, sortOption) { habitList, sort ->
        when (sort) {
            SortOption.ALPHABETICAL -> habitList.sortedBy { it.name }
            SortOption.COMPLETED_FIRST -> habitList.sortedByDescending { habitChecks.value.containsKey(it.id) }
            SortOption.RECENT -> habitList.sortedByDescending { it.id }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        observeHabits()
    }

    private fun observeHabits() {
        viewModelScope.launch {
            habitDao.getAllHabits().collect { loadedHabits ->
                _habits.value = loadedHabits
                refreshHabitChecks(loadedHabits)
            }
        }
    }

    private fun refreshHabitChecks(habits: List<Habit>) {
        viewModelScope.launch {
            val checks = habits.mapNotNull { habit ->
                habitCheckDao.getHabitCheck(habit.id, today)?.let { check ->
                    habit.id to check
                }
            }.toMap()
            _habitChecks.value = checks
        }
    }

    fun addHabit(name: String) {
        viewModelScope.launch {
            habitDao.insert(Habit(name = name))
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.delete(habit)
            habitCheckDao.deleteChecksForHabit(habit.id)
            _habits.update { it.filterNot { it.id == habit.id } }
            _habitChecks.update { it - habit.id }
        }
    }

    fun toggleHabitCheck(habit: Habit) {
        viewModelScope.launch {
            val existing = habitCheckDao.getHabitCheck(habit.id, today)
            if (existing == null) {
                val newCheck = HabitCheck(habit.id, today, true)
                habitCheckDao.insertHabitCheck(newCheck)
                _habitChecks.update { it + (habit.id to newCheck) }
            } else {
                habitCheckDao.deleteChecksForHabit(habit.id)
                _habitChecks.update { it - habit.id }
            }
        }
    }

    fun isHabitChecked(habitId: Int): Boolean =
        _habitChecks.value.containsKey(habitId)

    fun setEndTime(newTime: LocalTime) {
        _endTime.value = newTime
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }
}
