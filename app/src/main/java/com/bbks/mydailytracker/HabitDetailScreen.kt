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
import androidx.compose.material.icons.Icons
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
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.NotificationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bbks.mydailytracker.ui.common.MyAppTopBar
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.NotificationManagerCompat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitDetailScreen(
    habitId: Int,
    viewModel: HabitViewModel,
    onBack: () -> Unit
) {
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
            Text(stringResource(R.string.loading))
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

    var noteText by remember { mutableStateOf(habit.note ?: "") }

    val isModified = habit.note != noteText ||
            habit.repeatDays.toSet() != selectedDays.toSet() ||
            habit.alarmEnabled != alarmEnabled.value ||
            habit.alarmHour != timePickerState.value.hour ||
            habit.alarmMinute != timePickerState.value.minute

    val dayLabels = listOf(
        R.string.day_mon_short,
        R.string.day_tue_short,
        R.string.day_wed_short,
        R.string.day_thu_short,
        R.string.day_fri_short,
        R.string.day_sat_short,
        R.string.day_sun_short,
    )

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
                            Toast.makeText(context, context.getString(R.string.need_exact_alarm_permission), Toast.LENGTH_LONG).show()
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
                        Toast.makeText(context, context.getString(R.string.alarm_set), Toast.LENGTH_SHORT).show()
                    } else {
                        cancelWeeklyAlarms(context, habit.id, habit.repeatDays)
                        Toast.makeText(context, context.getString(R.string.alarm_cancel), Toast.LENGTH_SHORT).show()
                    }

                    viewModel.updateHabit(updatedHabit)
                    onBack()
                },
                    enabled = isModified
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text("🔁 반복 요일", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dayLabels.forEachIndexed { index, labelRes ->
                            val day = daysOfWeek[index]
                            val isSelected = selectedDays.contains(day.value)

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedDays.remove(day.value)
                                    else selectedDays.add(day.value)
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = labelRes),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = if (isSelected) Color.White else Color.Black
                                    )
                                },
                                leadingIcon = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = if (isSelected) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                                    selectedContainerColor = Color(0xFF4CAF50)
                                )
                            )
                        }
                    }/*{
                        daysOfWeek.forEach { day ->
                            val isSelected = selectedDays.contains(day.value)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedDays.remove(day.value)
                                    else selectedDays.add(day.value)
                                },
                                label = {
                                    Text(
                                        day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = if (isSelected) Color.White else Color.Black
                                    )
                                },
                                leadingIcon = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = if (isSelected) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                                    selectedContainerColor = Color(0xFF4CAF50)
                                )
                            )
                        }
                    }*/

                    Spacer(Modifier.height(12.dp))
                    Divider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.alarm_setting),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = alarmEnabled.value,
                            onCheckedChange = {
                                if (it && areNotificationsBlocked(context)) {
                                    val notificationManager = NotificationManagerCompat.from(context)
                                    if (!notificationManager.areNotificationsEnabled()) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.notification_blocked_warning),
                                            Toast.LENGTH_LONG
                                        ).show()

                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)

                                        return@Switch
                                    }
                                }

                                if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                    if (!alarmManager.canScheduleExactAlarms()) {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        context.startActivity(intent)
                                    }
                                }
                                shouldRequestPermission = it
                                alarmEnabled.value = it
                            }
                        )
                    }

                    if (alarmEnabled.value) {
                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                showLocalTimePickerDialog(context, timePickerState.value) {
                                    timePickerState.value = it
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.change_time, currentTimeText),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Divider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(Modifier.height(12.dp))

                    Text(stringResource(R.string.current_settings), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        text = stringResource(
                            R.string.repetition_days,
                            if (currentDaysText.isEmpty()) stringResource(R.string.not_set) else currentDaysText,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(
                            R.string.alarm_time,
                            if (alarmEnabled.value) {
                                currentTimeText.ifEmpty { stringResource(R.string.not_set) }
                            } else {
                                stringResource(R.string.not_set)
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        stringResource(R.string.saved_settings),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Color.Gray)
                    )
                    Text(
                        text = stringResource(
                            R.string.repetition_days,
                            savedDaysText.ifEmpty { stringResource(R.string.not_set) }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = stringResource(
                            R.string.alarm_time,
                            savedTimeText ?: stringResource(R.string.not_set)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 제목
                    Text(
                        stringResource(R.string.notes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = stringResource(R.string.notes_ins),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Spacer(Modifier.height(12.dp))

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
            if (it == Calendar.SUNDAY) 7 else it - 1
        })
    } else {
        repeatDays
    }

    for (day in actualRepeatDays) {
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
            }
        }
    }
}

fun formatDaysText(repeatDays: List<Int>): String {
    return repeatDays.sorted()
        .mapNotNull { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
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
            Toast.makeText(context, context.getString(R.string.notification_permission_denied), Toast.LENGTH_SHORT).show()
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
                    setAskedNotificationPermission(context)
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
    val visibleLines = 5
    val noteFont = FontFamily(Font(R.font.nanum_pen_script))

    val backgroundColor = MaterialTheme.colorScheme.surface
    val lineColor = if (isDark) Color(0xFF555555) else Color(0xFFBDBDBD)
    val textColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF4E4E4E)
    val cursorColor = if (isDark) Color.LightGray else Color.DarkGray


    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(lineHeight * visibleLines)
            .background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val paddingTopPx = 8.dp.toPx()
            val totalLines = 6
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

fun areNotificationsBlocked(context: Context): Boolean {
    val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
    return !(notificationManager?.areNotificationsEnabled() ?: true)
}

