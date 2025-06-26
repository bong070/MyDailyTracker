package com.bbks.mydailytracker

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import android.content.Context
import java.time.LocalTime

class HabitResetLogic(
    private val context: Context,
    private val habitRepository: HabitRepository
) {
    suspend fun executeReset() = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("reset_prefs", Context.MODE_PRIVATE)
        val lastResetDate = prefs.getString("last_reset_date", null)
        val today = LocalDate.now().minusDays(1)
        val todayStr = today.toString()
        val dayOfWeek = today.dayOfWeek.value // 1 (Mon) ~ 7 (Sun)
        val tomorrow = today.plusDays(1)
        val tomorrowStr = tomorrow.toString()
        val tomorrowDayOfWeek = tomorrow.dayOfWeek.value

        Log.d("HabitResetLogic", "Reset logic 실행됨 - 오늘: $today")
        val allHabits = habitRepository.getAllHabitsOnce()

        val now = LocalTime.now()
        if (now.hour != 0) {
            Log.d("ResetLogic", "현재 시간은 자정이 아닙니다. 리셋 중단됨.")
            return@withContext
        }

        if (lastResetDate == LocalDate.now().toString()) {
            Log.d("Reset", "오늘 이미 실행됨")
            return@withContext
        }


        for (habit in allHabits) {
            val check = habitRepository.getCheckForHabit(habit.id, todayStr)
            val wasChecked = check != null
            Log.d("HabitResetLogic", "습관: ${habit.name}")
            Log.d("HabitResetLogic", "습관: ${habit.repeatDays.toString()}")

            // ✅ 1. 성공/실패 기록 저장 (예: HabitDailyResult 테이블로)
            habitRepository.saveDailyResult(habit.id, todayStr, wasChecked)

            // ✅ 2. 반복 요일 없는 습관 → 삭제
            if (habit.repeatDays.isEmpty()) {
                habitRepository.delete(habit)
                habitRepository.deleteChecksForHabit(habit.id)
                continue
            }

            // ✅ 3. 반복 요일 있는 습관 → 내일이 지정 요일이면 자동 체크 생성 또는 초기화
            if (habit.repeatDays.contains(tomorrowDayOfWeek)) {
                val checkTomorrow = habitRepository.getCheckForHabit(habit.id, tomorrowStr)
                if (checkTomorrow == null) {
                    val newCheck = HabitCheck(habit.id, tomorrowStr, false)
                    habitRepository.insertHabitCheck(newCheck)
                } else {
                    // 이미 있으면 체크 상태 초기화
                    val resetCheck = checkTomorrow.copy(isCompleted = false)
                    habitRepository.insertHabitCheck(resetCheck)
                }
            }

            // ✅ 4. 오늘이 지정 요일이 아니더라도, 월/수 등의 케이스로 습관을 유지해야 함 → 삭제하지 않음
            // 아무것도 안 하면 자동 유지됨
        }
        prefs.edit().putString("last_reset_date", LocalDate.now().toString()).apply()
    }
}
