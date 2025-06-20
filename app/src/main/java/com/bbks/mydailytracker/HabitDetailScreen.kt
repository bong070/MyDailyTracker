package com.bbks.mydailytracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.EmptyCoroutineContext.get
import androidx.compose.foundation.lazy.items
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.derivedStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habit: Habit,
    viewModel: HabitViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val timePickerState = remember { mutableStateOf(habit.alarmTime ?: LocalTime.of(8, 0)) }
    val selectedDays = remember {
        mutableStateListOf<Int>().apply {
            habit.repeatDays.forEach { add(it) }
        }
    }
    var alarmEnabled = remember { mutableStateOf(habit.alarmEnabled) }

    val daysOfWeek = DayOfWeek.values() // MONDAY(1) ~ SUNDAY(7)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(habit.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = {
                    val updatedHabit = habit.copy(
                        alarmHour = timePickerState.value.hour,
                        alarmMinute = timePickerState.value.minute,
                        alarmEnabled = alarmEnabled.value,
                        repeatDays = selectedDays.toList()
                    )
                    if (alarmEnabled.value) {
                        if (!canScheduleExactAlarms(context)) {
                            Toast.makeText(context, "정확한 알람 권한이 필요합니다. 설정에서 허용해주세요.", Toast.LENGTH_LONG).show()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val alarmManager =
                                    context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                if (!alarmManager.canScheduleExactAlarms()) {
                                    val intent =
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    context.startActivity(intent)
                                    return@Button
                                }
                            }
                        }
                        scheduleWeeklyAlarms(
                            context,
                            timePickerState.value.hour,
                            timePickerState.value.minute,
                            selectedDays.toList()
                        )
                        Toast.makeText(context, "알람이 설정되었습니다", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        cancelWeeklyAlarms(context, habit.repeatDays)
                        Toast.makeText(context, "알람이 취소되었습니다", Toast.LENGTH_SHORT).show()
                    }
                    viewModel.updateHabit(updatedHabit)
                    onBack()
                }) {
                    Text("저장")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("🔁 반복 요일", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(daysOfWeek) { day ->
                    FilterChip(
                        selected = selectedDays.contains(day.value),
                        onClick = {
                            if (selectedDays.contains(day.value)) selectedDays.remove(day.value)
                            else selectedDays.add(day.value)
                        },
                        label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⏰ 알람 시간", modifier = Modifier.weight(1f))
                Text("%02d:%02d".format(timePickerState.value.hour, timePickerState.value.minute))
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    showLocalTimePickerDialog(context) { selectedTime ->
                        timePickerState.value = selectedTime
                    }
                }) {
                    Text("시간 선택")
                }
            }

            Spacer(Modifier.height(16.dp))

            val summaryText = if (alarmEnabled.value) {
                formatAlarmSummary(
                    selectedDays,
                    timePickerState.value.hour,
                    timePickerState.value.minute
                )
            } else {
                "알람 사용 안함"
            }

            Text(
                text = "🔔 현재 알람 설정: $summaryText",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            val savedSummaryText = if (habit.alarmEnabled) {
                formatAlarmSummary(
                    habit.repeatDays,
                    habit.alarmHour ?: 8,
                    habit.alarmMinute ?: 0
                )
            } else {
                "알람 사용 안함"
            }

            Text(
                text = "🔔 저장된 알람: $savedSummaryText",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔔 알람 사용")
                Spacer(Modifier.weight(1f))
                Switch(checked = alarmEnabled.value, onCheckedChange = { alarmEnabled.value = it })
            }

            Spacer(Modifier.height(16.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms(context)) {
                Text(
                    "📌 정확한 알람 권한이 꺼져 있습니다. 설정 > 알림 > 정확한 알람에서 허용해주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun showLocalTimePickerDialog(context: Context, onTimeSelected: (LocalTime) -> Unit) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            onTimeSelected(LocalTime.of(selectedHour, selectedMinute))
        },
        hour, minute, true
    ).show()
}

fun scheduleWeeklyAlarms(context: Context, hour: Int, minute: Int, repeatDays: List<Int>) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val now = Calendar.getInstance()

    for (day in repeatDays) {
        // 기존 알람 취소
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("dayOfWeek", day)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            day, // 요일을 requestCode로 사용
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE // 이미 있는 알람만 가져옴
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d("AlarmSchedule", "🔄 기존 알람 취소 - 요일: $day")
        }

        val calendarDay = (day % 7) + 1
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, calendarDay)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        Log.d("AlarmSchedule", "알람 설정 - 요일: $day, 시간: ${calendar.time}")

        val newIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("dayOfWeek", day)
        }

        val newPendingIntent = PendingIntent.getBroadcast(
            context,
            day,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}

fun cancelWeeklyAlarms(context: Context, repeatDays: List<Int>) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    for (day in repeatDays) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            day,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmCancel", "알람 취소됨 - 요일: $day")
    }
}

fun formatAlarmSummary(repeatDays: List<Int>, hour: Int, minute: Int): String {
    if (repeatDays.isEmpty()) return "설정 안됨"

    val dayNames = repeatDays
        .sorted()
        .map { DayOfWeek.of(if (it == 7) 7 else it) }  // 1=MON, ..., 7=SUN
        .joinToString(", ") {
            it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }

    val timeStr = "%02d:%02d".format(hour, minute)
    return "$dayNames · $timeStr"
}


fun canScheduleExactAlarms(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else {
        true
    }
}



