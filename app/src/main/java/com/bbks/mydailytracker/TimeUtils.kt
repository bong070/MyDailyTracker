// 📁 파일 위치: com.bbks.mydailytracker.TimeUtils.kt
package com.bbks.mydailytracker

import java.time.Duration
import java.time.LocalDateTime

fun getRemainingTime(): String {
    val now = LocalDateTime.now()
    val endOfDay = now.toLocalDate().atTime(23, 59, 59)
    val duration = Duration.between(now, endOfDay)

    val hours = duration.toHours()
    val minutes = (duration.toMinutes() % 60)
    val seconds = (duration.seconds % 60)

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
