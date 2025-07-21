package com.bbks.mydailytracker.data.db

import androidx.room.TypeConverter
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.toString()

    @TypeConverter
    fun toLocalTime(timeString: String?): LocalTime? =
        timeString?.let { LocalTime.parse(it) }

    @TypeConverter
    fun fromIntList(list: List<Int>): String = list.joinToString(",")

    @TypeConverter
    fun toIntList(data: String): List<Int> =
        if (data.isBlank()) emptyList()
        else data.split(",").map { it.toInt() }
}