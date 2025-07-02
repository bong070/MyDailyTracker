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
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.bbks.mydailytracker.ui.common.MyAppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: Int,
    viewModel: HabitViewModel,
    onBack: () -> Unit
) {
    val statusBarColor = Color(0xFFFFF8E1)
    val context = LocalContext.current

    var loadedHabit by remember { mutableStateOf<Habit?>(null) }

    LaunchedEffect(habitId) {
        viewModel.getHabitById(habitId).collect { loadedHabit = it }
    }

    val habit = loadedHabit

    if (habit == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text("로딩 중...")
        }
        return
    }

    val timePickerState = remember { mutableStateOf(habit.alarmTime ?: LocalTime.of(8, 0)) }
    val selectedDays = remember { mutableStateListOf<Int>().apply { habit.repeatDays.forEach { add(it) } } }
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

    val selectedColor = Color(0xFF4CAF50) // 선택된 칩 배경색 (연녹색)
    val unselectedColor = Color(0xFFE0E0E0) // 미선택 배경 (연그레이)
    val selectedTextColor = Color.White
    val unselectedTextColor = Color.Black
    val beigeBackground = Color(0xFFFFF8E1)
    var noteText by remember { mutableStateOf(habit.note ?: "") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MyAppTopBar(
                title = habit.name,
                onBack = onBack,
                backgroundColor = MaterialTheme.colorScheme.background
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(WindowInsets.systemBars.asPaddingValues()),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = {
                    val updatedHabit = habit.copy(
                        note = noteText,
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
            Text("🔁 반복 요일", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                daysOfWeek.forEach { day ->
                    val isSelected = selectedDays.contains(day.value)

                    FilterChip(
                        selected = selectedDays.contains(day.value),
                        onClick = {
                            if (isSelected) selectedDays.remove(day.value)
                            else selectedDays.add(day.value)
                        },
                        label = {
                            Text(
                                day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                color = if (isSelected) selectedTextColor else unselectedTextColor
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (isSelected) selectedColor else unselectedColor,
                            labelColor = if (isSelected) selectedTextColor else unselectedTextColor,
                            selectedContainerColor = selectedColor
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("현재 반복 요일 설정:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(currentDaysText.ifEmpty { "선택된 반복 요일 설정 없음" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)

            Spacer(Modifier.height(8.dp))
            Text("저장된 반복 요일 설정:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(savedDaysText.ifEmpty { "저장된 반복 요일 설정 없음" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)

            Spacer(Modifier.height(20.dp))
            Text("⏰ 알람 시간", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currentTimeText, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.width(12.dp))
                Button(onClick = {
                    showLocalTimePickerDialog(context, timePickerState.value) { timePickerState.value = it }
                }) {
                    Text("시간 선택", color = MaterialTheme.colorScheme.onBackground)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("현재 알람 설정:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            if (habit.alarmEnabled) {
                Text(currentDaysText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                currentTimeText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground) }
            } else {
                Text("현재 알람 없음", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
            }

            Spacer(Modifier.height(12.dp))
            Text("저장된 알람 설정:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            if (habit.alarmEnabled) {
                Text(savedDaysText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                savedTimeText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground) }
            } else {
                Text("저장된 알람 없음", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("알람 사용", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.weight(1f))
                Switch(checked = alarmEnabled.value, onCheckedChange = {
                    if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        if (!alarmManager.canScheduleExactAlarms()) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            context.startActivity(intent)
                        }
                    }
                    shouldRequestPermission = it
                    alarmEnabled.value = it
                })
            }

            Spacer(Modifier.height(20.dp))
            Text("📝 메모", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))

            LinedNoteField(
                text = noteText,
                onTextChange = { noteText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
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

@Composable
fun LinedNoteField(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val lineHeight = 24.dp
    val visibleLines = 3
    val noteFont = FontFamily(Font(R.font.nanum_pen_script))

    // 🎨 색상 다크/라이트 대응
    val backgroundColor = MaterialTheme.colorScheme.background
    val lineColor = if (isDark) Color(0xFF555555) else Color(0xFFBDBDBD)
    val textColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF4E4E4E)
    val cursorColor = if (isDark) Color.LightGray else Color.DarkGray


    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(lineHeight * visibleLines)
            .background(backgroundColor)
    ) {
        // 밑줄 배경
        Canvas(modifier = Modifier.matchParentSize()) {
            val paddingTopPx = 8.dp.toPx() // BasicTextField의 top padding
            val totalLines = 4
            repeat(totalLines) { i ->
                val y = paddingTopPx + lineHeight.toPx() * i
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
        }

        BasicTextField(
            value = text,
            onValueChange = {
                val lines = it.lines()
                if (lines.size <= visibleLines) {
                    onTextChange(it)
                } else {
                    // ✅ 5줄까지만 유지
                    val trimmed = lines.take(visibleLines).joinToString("\n")
                    onTextChange(trimmed)
                }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxSize(),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 16.sp,
                fontFamily = noteFont,
                lineHeight = 24.sp,
                color = textColor
            ),
            cursorBrush = SolidColor(cursorColor),
            maxLines = visibleLines
        )
    }
}

