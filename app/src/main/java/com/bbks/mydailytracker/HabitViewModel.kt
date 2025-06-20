package com.bbks.mydailytracker

import SortOption
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbks.mydailytracker.data.SettingsRepository
import com.bbks.mydailytracker.data.UserPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class HabitViewModel(
    private val habitDao: HabitDao,
    private val habitCheckDao: HabitCheckDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits

    private val _habitChecks = MutableStateFlow<Map<Int, HabitCheck>>(emptyMap())
    val habitChecks: StateFlow<Map<Int, HabitCheck>> = _habitChecks

    private val _endTime = MutableStateFlow(LocalTime.of(23, 59, 59))
    val endTime: StateFlow<LocalTime> = _endTime

    private val _sortOption = MutableStateFlow(SortOption.ALPHABETICAL)
    val sortOption: StateFlow<SortOption> = _sortOption

    private val _alarmEnabled = MutableStateFlow(false)
    val alarmEnabled: StateFlow<Boolean> = _alarmEnabled

    private val _autoDelete = MutableStateFlow(false)
    val autoDelete: StateFlow<Boolean> = _autoDelete

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
        observePreferences()
    }

    private fun observeHabits() {
        viewModelScope.launch {
            habitDao.getAllHabits().collect { loadedHabits ->
                _habits.value = loadedHabits
                refreshHabitChecks(loadedHabits)
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            settingsRepository.userPreferencesFlow.collect { prefs ->
                _endTime.value = LocalTime.of(prefs.endHour, prefs.endMinute)
                _alarmEnabled.value = prefs.alarmEnabled
                _autoDelete.value = prefs.autoDelete
                _sortOption.value = prefs.sortOption
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
        viewModelScope.launch {
            settingsRepository.updateEndTime(newTime)
        }
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
        viewModelScope.launch {
            settingsRepository.updateSortOption(option)
        }
    }

    fun setAlarmEnabled(value: Boolean) {
        _alarmEnabled.value = value
        viewModelScope.launch {
            settingsRepository.updateAlarmEnabled(value)
        }
    }

    fun setAutoDelete(value: Boolean) {
        _autoDelete.value = value
        viewModelScope.launch {
            settingsRepository.updateAutoDelete(value)
        }
    }

    fun saveSettings(endTime: LocalTime, alarmEnabled: Boolean, autoDelete: Boolean, sortOption: SortOption) {
        viewModelScope.launch {
            val prefs = UserPreferences(
                endHour = endTime.hour,
                endMinute = endTime.minute,
                alarmEnabled = alarmEnabled,
                autoDelete = autoDelete,
                sortOption = sortOption
            )
            settingsRepository.savePreferences(prefs)
        }
    }

    fun updateHabit(updatedHabit: Habit) {
        viewModelScope.launch {
            habitDao.update(updatedHabit)
            _habits.update { habits ->
                habits.map { if (it.id == updatedHabit.id) updatedHabit else it }
            }
        }
    }
}
