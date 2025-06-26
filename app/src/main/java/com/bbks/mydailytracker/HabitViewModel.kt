package com.bbks.mydailytracker

import SortOption
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbks.mydailytracker.data.SettingsRepository
import com.bbks.mydailytracker.data.UserPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.bbks.mydailytracker.model.DayStats

class HabitViewModel(
    private val habitDao: HabitDao,
    private val habitCheckDao: HabitCheckDao,
    private val settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits

    private val _habitChecks = MutableStateFlow<Map<Int, HabitCheck>>(emptyMap())
    val habitChecks: StateFlow<Map<Int, HabitCheck>> = _habitChecks

    private val _endTime = MutableStateFlow(LocalTime.of(0, 0, 0))
    val endTime: StateFlow<LocalTime> = _endTime

    private val _sortOption = MutableStateFlow(SortOption.ALPHABETICAL)
    val sortOption: StateFlow<SortOption> = _sortOption

    private val _alarmEnabled = MutableStateFlow(false)
    val alarmEnabled: StateFlow<Boolean> = _alarmEnabled

    private val _autoDelete = MutableStateFlow(false)
    val autoDelete: StateFlow<Boolean> = _autoDelete

    private val today: String = LocalDate.now().toString()

    val sortedHabits = combine(habits, sortOption) { habitList, sort ->
        var targetDay = LocalDate.now().dayOfWeek.value
        val filtered = habitList.filter {
            it.repeatDays.isEmpty() || it.repeatDays.contains(targetDay)
        }

        when (sort) {
            SortOption.ALPHABETICAL -> filtered.sortedBy { it.name }
            SortOption.COMPLETED_FIRST -> filtered.sortedByDescending {
                habitChecks.value.containsKey(it.id)
            }
            SortOption.RECENT -> filtered.sortedByDescending { it.id }
            SortOption.MANUAL -> filtered.sortedBy { it.order }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val weeklyStats: StateFlow<List<DailyHabitResult>> =
        habitRepository.getWeeklyStats()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

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
            val lastOrder = habits.value.maxOfOrNull { it.order ?: 0 } ?: 0
            habitDao.insert(Habit(name = name, order = lastOrder + 1)) // order 지정
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

    fun setEndTime(context: Context, newTime: LocalTime) {
        _endTime.value = newTime
        viewModelScope.launch {
            settingsRepository.updateEndTime(newTime)
            HabitResetScheduler.scheduleDailyReset(context, newTime)
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

    fun saveSettings(
        endTime: LocalTime,
        alarmEnabled: Boolean,
        autoDelete: Boolean,
        sortOption: SortOption
    ) {
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

    fun disableAllHabitAlarms() {
        viewModelScope.launch {
            val habits = habitRepository.getAllHabitsOnce()// 모든 습관 불러오기
            for (habit in habits) {
                val updated = habit.copy(
                    alarmEnabled = false,
                    repeatDays = emptyList()
                )
                habitRepository.update(updated)
            }
        }
    }

    fun reorderHabits(fromIndex: Int, toIndex: Int) {
        val currentList = sortedHabits.value.toMutableList()
        val habit = currentList.removeAt(fromIndex)
        currentList.add(toIndex, habit)

        viewModelScope.launch {
            currentList.forEachIndexed { index, updatedHabit ->
                val reordered = updatedHabit.copy(order = index)
                habitRepository.update(reordered)
            }
        }
    }

    fun refreshHabits() {
        viewModelScope.launch {
            val loadedHabits = habitDao.getAllHabitsOnce()
            _habits.value = loadedHabits
            refreshHabitChecks(loadedHabits)
        }
    }

    fun getHabitById(habitId: Int): StateFlow<Habit?> =
        habitDao.getHabitByIdFlow(habitId).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    fun getWeekStatsForUI(): StateFlow<List<DayStats>> {
        return weeklyStats
            .combine(habits) { results, habitList ->
                val habitMap = habitList.associateBy { it.id }

                results
                    .groupBy { it.date } // 날짜별로 묶기
                    .toSortedMap()       // 월~일 순 정렬
                    .map { (date, entries) ->
                        val successCount = entries.count { it.isSuccess }
                        val failureCount = entries.count { !it.isSuccess }

                        val failedHabits = entries
                            .filter { !it.isSuccess }
                            .mapNotNull { habitMap[it.habitId]?.name }

                        val label = LocalDate.parse(date).dayOfWeek
                            .getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
                            .take(1) // "M", "T", ...

                        DayStats(
                            label = label,
                            success = successCount,
                            failure = failureCount,
                            failedHabits = failedHabits
                        )
                    }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
    }
}