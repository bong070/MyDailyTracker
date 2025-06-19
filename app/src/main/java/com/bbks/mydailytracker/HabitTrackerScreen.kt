package com.bbks.mydailytracker

import SortOption
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val sortedHabits by viewModel.sortedHabits.collectAsState()
    val habitChecks by viewModel.habitChecks.collectAsState(initial = emptyMap())
    val endTime by viewModel.endTime.collectAsState(initial = LocalTime.of(23, 59, 59))
    val todayString = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val completedCount = habitChecks.values.count { it.date == todayString }
    var newHabitName by remember { mutableStateOf("") }
    val selectedHabitState = remember { mutableStateOf<Habit?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val sortOption by viewModel.sortOption.collectAsState()

    val scope = rememberCoroutineScope()

    if (selectedHabitState.value != null) {
        CalendarScreen(
            viewModel = viewModel,
            habit = selectedHabitState.value!!,
            onBack = { selectedHabitState.value = null }
        )
    } else {
        Scaffold(
            modifier = Modifier.padding(WindowInsets.systemBars.asPaddingValues()),
            topBar = {
                TopBarWithCountdownAndSettings(
                    endTime = endTime,
                    onSettingsClick = { showSettings = true }
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

                    val totalCount = sortedHabits.size

                    if (totalCount > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🏆 오늘 완료: $completedCount / $totalCount",
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 1.0f),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = completedCount / totalCount.toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (sortedHabits.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("매일의 작은 루틴이 큰 변화를 만듭니다!")
                        }
                    } else {
                        LazyColumn {
                            items(sortedHabits) { habit ->
                                val isChecked =
                                    habitChecks[habit.id]?.isCompleted == true && habitChecks[habit.id]?.date == todayString

                                HabitItem(
                                    habit = habit,
                                    isChecked = isChecked,
                                    onCheckToggle = {
                                        scope.launch {
                                            viewModel.toggleHabitCheck(habit)
                                        }
                                    },
                                    onClick = { selectedHabitState.value = habit },
                                    onRemove = { viewModel.deleteHabit(it) }
                                )
                            }
                        }
                    }
                }
            }
        )

        val sortOption by viewModel.sortOption.collectAsState()

        if (showSettings) {
            SettingsDialog(
                onDismiss = { showSettings = false },
                initialSortOption = sortOption,
                onSave = { time, alarm, autoDelete, selectedSort ->
                    val (hour, minute) = time
                    viewModel.setEndTime(LocalTime.of(hour, minute))
                    viewModel.setSortOption(selectedSort)
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
        println("LaunchedEffect triggered with new endTime: $endTime")
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    var duration = Duration.between(now, endTime)

    if (duration.isNegative) {
        duration = duration.plusHours(24)
    }

    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
fun SortMenu(current: SortOption, onSelect: (SortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text("정렬: ${current.name}")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOption.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}