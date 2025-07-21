package com.bbks.mydailytracker.domain.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbks.mydailytracker.data.repository.SettingsRepository
import com.bbks.mydailytracker.util.SortOption
import com.bbks.mydailytracker.data.db.HabitCheckDao
import com.bbks.mydailytracker.data.db.HabitDao
import com.bbks.mydailytracker.data.model.DailyHabitResult
import com.bbks.mydailytracker.data.model.Habit
import com.bbks.mydailytracker.data.model.HabitCheck
import com.bbks.mydailytracker.data.repository.HabitRepository
import com.bbks.mydailytracker.model.DayStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

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

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    private val today: String = LocalDate.now().toString()

    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser

    val detailEntryCount: StateFlow<Int> = settingsRepository.detailEntryCount
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), 0)

    val isFirstLaunch = settingsRepository.isFirstLaunch
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), true)

    fun setFirstLaunchDone() {
        viewModelScope.launch {
            settingsRepository.setFirstLaunchDone()
        }
    }

    fun onDetailEntrySuccess() {
        viewModelScope.launch {
            if (!isPremiumUser.value) {
                settingsRepository.incrementDetailEntryCount()
            }
        }
    }

    val sortedHabits = combine(habits, sortOption) { habitList, sort ->
        var targetDay = LocalDate.now().dayOfWeek.value
        val filtered = habitList.filter {
            if (it.repeatDays.isEmpty()) {
                LocalDate.parse(it.createdDate) == LocalDate.now()
            } else {
                it.repeatDays.contains(targetDay)
            }
        }

        when (sort) {
            SortOption.ALPHABETICAL -> filtered.sortedBy { it.name }
            SortOption.INCOMPLETED_FIRST -> filtered.sortedByDescending {
                !habitChecks.value.containsKey(it.id)
            }

            SortOption.RECENT -> filtered.sortedByDescending { it.id }
            SortOption.MANUAL -> filtered.sortedBy { it.order }
        }
    }.stateIn(viewModelScope, SharingStarted.Companion.Eagerly, emptyList())

    val weeklyStats: StateFlow<List<DailyHabitResult>> =
        habitRepository.getWeeklyStats()
            .stateIn(
                viewModelScope,
                SharingStarted.Companion.WhileSubscribed(5000),
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
                _isPremiumUser.value = prefs.isPremiumUser
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
            habitDao.insert(
                Habit(
                    name = name,
                    order = lastOrder + 1,
                    createdDate = LocalDate.now().toString()
                )
            ) // order 지정
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

            refreshHabitChecks(_habits.value)
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

    fun setCurrentMonth(month: YearMonth) {
        _currentMonth.value = month
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
        if (fromIndex !in currentList.indices) return

        val safeToIndex = toIndex.coerceIn(0, currentList.size - 1)
        val habit = currentList.removeAt(fromIndex)
        currentList.add(safeToIndex, habit)

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
            Log.d("ViewModel", "refreshHabits called")
        }
    }

    fun getHabitById(habitId: Int): StateFlow<Habit?> =
        habitDao.getHabitByIdFlow(habitId).stateIn(
            viewModelScope,
            SharingStarted.Companion.WhileSubscribed(5000),
            null
        )

    fun getWeekStatsForUI(): StateFlow<List<DayStats>> {
        return weeklyStats
            .map { it.distinctBy { result -> "${result.date}_${result.habitId}" } }
            .combine(habits) { results, habitList ->
                val habitMap = habitList.associateBy { it.id }

                results
                    .groupBy { it.date } // 날짜별로 묶기
                    .toSortedMap()       // 월~일 순 정렬
                    .map { (date, entries) ->
                        val filteredEntries = entries.filter { result ->
                            Log.d("STATS_DEBUG", "📅 $date → ${entries.map { it.habitId to it.habitName }}")
                            val habit = habitMap[result.habitId] ?: return@filter false
                            val statDate = LocalDate.parse(result.date)
                            val created = LocalDate.parse(habit.createdDate)
                            val isRepeatDay = habit.repeatDays.contains(statDate.dayOfWeek.value)
                            (habit.repeatDays.isEmpty() && created == statDate) || isRepeatDay
                        }
                        val successCount = filteredEntries.count { it.isSuccess }
                        val failureCount = filteredEntries.count { !it.isSuccess }

                        val failedHabits = filteredEntries.filter { !it.isSuccess }.map { it.habitName }

                        val successHabits = filteredEntries.filter { it.isSuccess }.map { it.habitName }
                        val label = LocalDate.parse(date).dayOfWeek
                            .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            .take(1) // "M", "T", ...

                        DayStats(
                            date = LocalDate.parse(date),
                            label = label,
                            success = successCount,
                            failure = failureCount,
                            failedHabits = failedHabits,
                            successHabits = successHabits
                        )
                    }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Companion.WhileSubscribed(5000),
                emptyList()
            )
    }


    val monthlyStats: StateFlow<List<DailyHabitResult>> =
        currentMonth
            .flatMapLatest { month ->
                habitRepository.getMonthlyStats(month)
                    .map { it.distinctBy { result -> "${result.date}_${result.habitId}" } }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Companion.WhileSubscribed(5000),
                emptyList()
            )

    fun setPremiumUser(isPremium: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPremiumUser(isPremium)
        }
    }

    fun refreshPreferences() {
        viewModelScope.launch {
            settingsRepository.userPreferencesFlow.first().let { prefs ->
                _isPremiumUser.value = prefs.isPremiumUser
            }
        }
    }

    fun overridePremiumUserForDebug(value: Boolean) {
        _isPremiumUser.value = value // 그냥 메모리에서만 세팅
    }
}