package com.bbks.mydailytracker.calendar

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

object CalendarReader {

    data class Event(
        val title: String,
        val calendarId: Long,
        val allDay: Boolean
    )

    fun getUserEventsForToday(context: Context): List<Event> {
        val granted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return emptyList()

        val tz = ZoneId.systemDefault()
        val day = LocalDate.now()
        val start = ZonedDateTime.of(day.atStartOfDay(), tz).toInstant().toEpochMilli()
        val end = ZonedDateTime.of(day.plusDays(1).atStartOfDay(), tz).toInstant().toEpochMilli() - 1

        // 사용자 편집 가능한 캘린더만 허용 (시스템/공휴일 제외 목적)
        val allowedCalendarIds = mutableSetOf<Long>()
        val calProjection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE
        )
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, calProjection, null, null, null
        )?.use { c ->
            val idxId = c.getColumnIndex(CalendarContract.Calendars._ID)
            val idxAccess = c.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
            val idxVisible = c.getColumnIndex(CalendarContract.Calendars.VISIBLE)
            while (c.moveToNext()) {
                val id = c.getLong(idxId)
                val access = c.getInt(idxAccess)
                val visible = c.getInt(idxVisible) == 1
                val isEditable = access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
                if (visible && isEditable) allowedCalendarIds += id
            }
        }
        if (allowedCalendarIds.isEmpty()) return emptyList()

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, start)
        ContentUris.appendId(builder, end)

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.ALL_DAY
        )

        val events = mutableListOf<Event>()
        context.contentResolver.query(builder.build(), projection, null, null, null)?.use { c ->
            val idxTitle = c.getColumnIndex(CalendarContract.Instances.TITLE)
            val idxCalId = c.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
            val idxAllDay = c.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            while (c.moveToNext()) {
                val title = c.getString(idxTitle)?.trim().orEmpty()
                val calId = c.getLong(idxCalId)
                val allDay = c.getInt(idxAllDay) == 1
                if (title.isNotEmpty() && allowedCalendarIds.contains(calId)) {
                    events += Event(title, calId, allDay)
                }
            }
        }
        return events
    }
}
