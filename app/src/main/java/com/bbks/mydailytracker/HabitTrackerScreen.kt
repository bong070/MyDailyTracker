package com.bbks.mydailytracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitTrackerScreen(viewModel: HabitViewModel) {
    val habits by viewModel.habits.collectAsState(initial = emptyList())
    val habitChecks by viewModel.habitChecks.collectAsState(initial = emptyList())
    val endTime by viewModel.endTime.collectAsState()

    var newHabitName by remember { mutableStateOf("") }
    val selectedHabitState = remember { mutableStateOf<Habit?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    val todayString = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val scope = rememberCoroutineScope()

    if (selectedHabitState.value != null) {
        CalendarScreen(
            viewModel = viewModel,
            habit = selectedHabitState.value!!,
            onBack = { selectedHabitState.value = null }
        )
    } else {
        Scaffold(
            modifier = Modifier
                .padding(WindowInsets.systemBars.asPaddingValues()),
            topBar = {
                TopBarWithCountdownAndSettings(
                    endTime = endTime,
                    onSettingsClick = {
                        showSettings = true
                    }
                )
            },
            content = { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = newHabitName,
                            onValueChange = { newHabitName = it },
                            label = { Text("Add New Tracking Item") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (newHabitName.isNotBlank()) {
                                viewModel.addHabit(newHabitName)
                                newHabitName = ""
                            }
                        }) {
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn {
                        items(habits) { habit ->
                            val isChecked = habitChecks.any {
                                it.habitId == habit.id && it.date == todayString
                            }

                            HabitItem(
                                habit = habit,
                                isChecked = isChecked,
                                onCheckToggle = {
                                    scope.launch {
                                        viewModel.toggleHabitCheck(habit, todayString)
                                    }
                                },
                                onClick = { selectedHabitState.value = habit },
                                onRemove = { viewModel.deleteHabit(it) }
                            )
                        }
                    }
                }
            }
        )

        if (showSettings) {
            SettingsDialog(
                onDismiss = { showSettings = false },
                onSave = { time, alarm, autoDelete ->
                    val (hour, minute) = time
                    val newEndTime = LocalTime.of(hour, minute)
                    viewModel.setEndTime(newEndTime)
                    showSettings = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithCountdownAndSettings(
    endTime: LocalTime,
    onSettingsClick: () -> Unit
) {
    var remainingTime by remember { mutableStateOf(calculateRemainingTime(endTime)) }

    LaunchedEffect(endTime) {
        while (true) {
            remainingTime = calculateRemainingTime(endTime)
            delay(1000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "My Daily Tracker",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "설정")
            }
        }

        Text(
            text = "남은 시간: $remainingTime",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

fun calculateRemainingTime(endTime: LocalTime): String {
    val now = LocalTime.now()
    val duration = Duration.between(now, endTime)
    return if (!duration.isNegative) {
        String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutes() % 60, duration.seconds % 60)
    } else {
        "00:00:00"
    }
}

