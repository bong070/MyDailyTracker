package com.bbks.mydailytracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.lazy.items
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


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
        mutableStateListOf<Int>().apply { habit.repeatDays.forEach { add(it) } }
    }
    val alarmEnabled = remember { mutableStateOf(habit.alarmEnabled) }

    val daysOfWeek = DayOfWeek.values()

    val currentDaysText = formatDaysText(selectedDays)
    val currentTimeText = formatTimeText(timePickerState.value.hour, timePickerState.value.minute)

    val savedDaysText = formatDaysText(habit.repeatDays)
    val savedTimeText = habit.alarmHour?.let { hour ->
        habit.alarmMinute?.let { minute -> formatTimeText(hour, minute) }
    }
    var shouldRequestPermission by remember { mutableStateOf(false) }

    if (shouldRequestPermission) {
        RequestNotificationPermissionOnce()
        shouldRequestPermission = false
    }

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
                    .padding(16.dp).padding(WindowInsets.systemBars.asPaddingValues()),
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
                            Toast.makeText(context, "정확한 알람 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                context.startActivity(intent)
                                return@Button
                            }
                        }
                        cancelWeeklyAlarms(context, habit.id, selectedDays)
                        scheduleWeeklyAlarms(
                            context,
                            habit.id,
                            timePickerState.value.hour,
                            timePickerState.value.minute,
                            selectedDays,
                            habitTitle = habit.name
                        )
                        Toast.makeText(context, "알람이 설정되었습니다", Toast.LENGTH_SHORT).show()
                    } else {
                        cancelWeeklyAlarms(context, habit.id, habit.repeatDays)
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 반복 요일 설정
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

            Spacer(Modifier.height(12.dp))
            Text("현재 반복 요일 설정:", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            if (!currentDaysText.isNullOrEmpty()) {
                Text(currentDaysText, style = MaterialTheme.typography.bodySmall)
            } else {
                Text("선택된 반복 요일 설정 없음", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))
            Text("저장된 반복 요일 설정:", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            if (!savedDaysText.isNullOrEmpty()) {
                Text(savedDaysText, style = MaterialTheme.typography.bodySmall)
            } else {
                Text("저장된 반복 요일 설정 없음", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(20.dp))
            // 알람 시간
            Text("⏰ 알람 시간", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("%02d:%02d".format(timePickerState.value.hour, timePickerState.value.minute))
                Spacer(Modifier.width(12.dp))
                Button(onClick = {
                    showLocalTimePickerDialog(context, timePickerState.value) { timePickerState.value = it }
                }) {
                    Text("시간 선택")
                }
            }

            Spacer(Modifier.height(12.dp))
            // 현재 알람 설정
            Text("현재 알람 설정:", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            if (alarmEnabled.value) {
                Text(currentDaysText, style = MaterialTheme.typography.bodySmall)
                Text(currentTimeText, style = MaterialTheme.typography.bodySmall)
            } else {
                Text("선택된 알람 없음", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(12.dp))
            // 저장된 알람 설정
            Text("저장된 알람 설정:", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            if (habit.alarmEnabled) {
                Text(savedDaysText, style = MaterialTheme.typography.bodySmall)
                savedTimeText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text("저장된 알람 없음", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(20.dp))
            // 알람 사용 스위치
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("알람 사용", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Switch(checked = alarmEnabled.value, onCheckedChange = {
                    if (it) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            if (!alarmManager.canScheduleExactAlarms()) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                context.startActivity(intent)
                            }
                        }
                        shouldRequestPermission = true
                    }
                    alarmEnabled.value = it
                })
            }

            Spacer(Modifier.height(8.dp))

            // 권한 안내
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms(context)) {
                Text(
                    "📌 정확한 알람 권한이 꺼져 있습니다. 설정 > 어플리케이션 > My Daily Tracker에서 알람에서 허용해주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun showLocalTimePickerDialog(context: Context, initialTime: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            onTimeSelected(LocalTime.of(selectedHour, selectedMinute))
        },
        initialTime.hour,
        initialTime.minute,
        true
    ).show()
}

fun scheduleWeeklyAlarms(context: Context, habitId: Int, hour: Int, minute: Int, repeatDays: List<Int>, habitTitle: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val now = Calendar.getInstance()

    val actualRepeatDays = if (repeatDays.isEmpty()) {
        listOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let {
            // Calendar의 요일 값(1~7)을 DayOfWeek(1~7)와 일치시킴
            if (it == Calendar.SUNDAY) 7 else it - 1
        })
    } else {
        repeatDays
    }

    for (day in actualRepeatDays) {
        // 기존 알람 취소
        val requestCode = habitId * 10 + day
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("dayOfWeek", day)
            putExtra("habitTitle", habitTitle)
        }
        val cancelIntent = PendingIntent.getBroadcast(
            context,
            requestCode, // 요일을 requestCode로 사용
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE // 이미 있는 알람만 가져옴
        )
        if (cancelIntent != null) {
            alarmManager.cancel(cancelIntent)
            Log.d("AlarmSchedule", "🔄 기존 알람 취소 - 요일: $day")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

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

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}

fun cancelWeeklyAlarms(context: Context, habitId: Int, repeatDays: List<Int>) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    for (day in repeatDays) {
        val requestCode = habitId * 10 + day
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d("AlarmCancel", "습관ID=$habitId, 요일=$day 알람 취소됨")
        }
    }
}

fun canScheduleExactAlarms(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else {
        true
    }
}

fun cancelAllAlarms(context: Context, habitIds: List<Int>) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    for (habitId in habitIds) {
        for (day in 1..7) {
            val requestCode = habitId * 10 + day
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                Log.d("AlarmCancel", "전체 초기화 - 습관ID=$habitId, 요일=$day 알람 취소됨")
            }
        }
    }
}

fun formatDaysText(repeatDays: List<Int>): String {
    return repeatDays
        .map { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
        .joinToString(", ")
}

fun formatTimeText(hour: Int, minute: Int): String {
    return "%02d:%02d".format(hour, minute)
}

@Composable
fun RequestNotificationPermissionOnce() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        setAskedNotificationPermission(context)
        if (!isGranted) {
            Toast.makeText(context, "알림 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasAskedNotificationPermission(context)) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    setAskedNotificationPermission(context) // 이미 허용된 경우도 체크
                }
            }
        }
    }
}

fun hasAskedNotificationPermission(context: Context): Boolean {
    val prefs = context.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
    return prefs.getBoolean("notification_permission_asked", false)
}

fun setAskedNotificationPermission(context: Context) {
    val prefs = context.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("notification_permission_asked", true).apply()
}



