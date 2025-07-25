package com.bbks.mydailytracker.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "habits")
// 예시
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val alarmEnabled: Boolean = false,
    val alarmHour: Int? = null,
    val alarmMinute: Int? = null,
    val repeatDays: List<Int> = emptyList(), // 1(월) ~ 7(일)

    @ColumnInfo(name = "order_index")
    val order: Int = 0,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "created_date")
    val createdDate: String = LocalDate.now().toString()
)

val Habit.alarmTime: LocalTime?
    get() = if (alarmHour != null && alarmMinute != null)
        LocalTime.of(alarmHour, alarmMinute)
    else null