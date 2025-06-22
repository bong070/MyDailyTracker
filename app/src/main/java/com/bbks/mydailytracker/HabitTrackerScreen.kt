package com.bbks.mydailytracker

import SortOption
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.Duration
import android.Manifest
import android.content.Context
import android.content.IntentFilter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import org.burnoutcrew.reorderable.*
import androidx.compose.material.icons.filled.InsertChart

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HabitTrackerScreen(viewModel: HabitViewModel, onNavigateToStats: () -> Unit) {
    val sortedHabits by viewModel.sortedHabits.collectAsState()
    val habitChecks by viewModel.habitChecks.collectAsState(initial = emptyMap())
    val endTime by viewModel.endTime.collectAsState(initial = LocalTime.of(23, 59, 59))
    val todayString = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val completedCount = habitChecks.values.count { it.date == todayString }

    var newHabitName by remember { mutableStateOf("") }
    val selectedHabitState = remember { mutableStateOf<Habit?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    val sortOption by viewModel.sortOption.collectAsState()
    val alarmEnabled by viewModel.alarmEnabled.collectAsState()
    val autoDelete by viewModel.autoDelete.collectAsState()

    val scope = rememberCoroutineScope()

    RequestNotificationPermission()

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val receiver = HabitRefreshReceiver {
            scope.launch {
                viewModel.refreshHabits()
            }
        }
        val intentFilter = IntentFilter("com.bbks.mydailytracker.HABITS_REFRESH")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, intentFilter)
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    if (selectedHabitState.value != null) {
        HabitDetailScreen(
            habit = selectedHabitState.value!!,
            viewModel = viewModel,
            onBack = { selectedHabitState.value = null }
        )
    } else {
        Scaffold(
            modifier = Modifier.padding(WindowInsets.systemBars.asPaddingValues()),
            topBar = {
                TopBarWithCountdownAndSettings(
                    endTime = endTime,
                    alarmEnabled = alarmEnabled,
                    context = context,
                    onSettingsClick = { showSettings = true },
                    onStatsClick = onNavigateToStats
                )
            },
            bottomBar = { AdMobBanner() }
        ) { padding ->
            val listState = rememberLazyListState()
            var hasShownManualToast by remember { mutableStateOf(false) }

            val reorderState = rememberReorderableLazyListState(
                listState = listState,
                onMove = { from, to ->
                if (sortOption != SortOption.MANUAL) {
                    viewModel.setSortOption(SortOption.MANUAL)
                    if (!hasShownManualToast) {
                        Toast.makeText(context, "사용자 지정 정렬로 전환되었습니다", Toast.LENGTH_SHORT).show()
                        hasShownManualToast = true
                    }
                }
                viewModel.reorderHabits(from.index, to.index)
            })

            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = newHabitName,
                        onValueChange = { newHabitName = it },
                        label = { Text("새로운 목표 아이템") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (newHabitName.isNotBlank()) {
                            viewModel.addHabit(newHabitName)
                            newHabitName = ""
                        }
                    }) {
                        Text("추가")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (sortedHabits.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🏆 오늘 완료: $completedCount / ${sortedHabits.size}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = completedCount / sortedHabits.size.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (sortedHabits.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("매일의 작은 루틴이 큰 변화를 만듭니다!")
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .reorderable(reorderState)
                            .detectReorderAfterLongPress(reorderState)
                    ) {
                        items(sortedHabits, key = { it.id }) { habit ->
                            ReorderableItem(state = reorderState, key = habit.id) { isDragging ->
                                val isChecked = habitChecks[habit.id]?.let {
                                    it.isCompleted && it.date == todayString
                                } ?: false

                                val itemModifier = Modifier
                                    .background(if (isDragging) Color.LightGray else Color.Transparent)
                                    .animateItemPlacement()

                                HabitItem(
                                    habit = habit,
                                    isChecked = isChecked,
                                    onCheckToggle = {
                                        scope.launch { viewModel.toggleHabitCheck(habit) }
                                    },
                                    onClick = { selectedHabitState.value = habit },
                                    onRemove = { viewModel.deleteHabit(it) },
                                    modifier = itemModifier
                                )
                            }
                        }
                    }
                }
            }

            if (showSettings) {
                SettingsDialog(
                    viewModel = viewModel,
                    onDismiss = { showSettings = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithCountdownAndSettings(
    endTime: LocalTime,
    alarmEnabled: Boolean,
    context: Context,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    var remainingTime by remember { mutableStateOf(calculateRemainingTime(endTime)) }

    LaunchedEffect(endTime) {
        println("LaunchedEffect triggered with new endTime: $endTime")
        while (true) {
            remainingTime = calculateRemainingTime(endTime)
            delay(1000L)
        }
    }
    // 알람 상태가 바뀔 때마다 실행
    LaunchedEffect(alarmEnabled) {
        if (alarmEnabled) {
            AlarmHelper.scheduleDailyAlarms(context)
        } else {
            AlarmHelper.cancelAllAlarms(context)
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
            IconButton(onClick = onStatsClick) {
                Icon(Icons.Default.InsertChart, contentDescription = "통계")
            }
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
fun RequestNotificationPermission() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "알림 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}