package com.bbks.mydailytracker.domain.usecase

import android.content.Context
import com.bbks.mydailytracker.calendar.CalendarReader
import com.bbks.mydailytracker.data.db.HabitDao
import com.bbks.mydailytracker.data.db.HabitCheckDao
import com.bbks.mydailytracker.data.model.Habit
import com.bbks.mydailytracker.data.model.HabitCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * 매일 00:00 리셋 직후 호출:
 * - 오늘의 사용자 캘린더 이벤트를 읽어
 * - 각 이벤트마다 "동일 이름 허용"으로 새 습관 생성
 * - 오늘 날짜 HabitCheck를 존재 보장(false)
 * - order는 현재 최대 + 1씩 증가
 */
class CalendarSyncUseCase(
    private val context: Context,
    private val habitDao: HabitDao,
    private val habitCheckDao: HabitCheckDao
) {
    suspend fun syncToday(): Int = withContext(Dispatchers.IO) {
        val events = CalendarReader.getUserEventsForToday(context)
        if (events.isEmpty()) return@withContext 0

        val today = LocalDate.now().toString()
        var created = 0
        var orderBase = habitDao.getMaxOrderIndex()

        for (evt in events) {
            val name = evt.title
            orderBase += 1

            // 항상 새 습관 생성 (중복 허용)
            val newId = habitDao.insertAndReturnId(
                Habit(
                    name = name,
                    alarmEnabled = false,
                    alarmHour = null,
                    alarmMinute = null,
                    repeatDays = emptyList(),
                    order = orderBase,
                    note = null,
                    createdDate = today
                )
            ).toInt()
            created++

            // 오늘 체크 존재 보장(false)
            val existingCheck = habitCheckDao.getHabitCheck(newId, today)
            if (existingCheck == null) {
                habitCheckDao.insertHabitCheck(HabitCheck(newId, today, false))
            }
        }
        created
    }
}
