package com.bbks.mydailytracker

import SortOption
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import org.burnoutcrew.reorderable.*
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HabitTrackerScreen(
    viewModel: HabitViewModel,
    onNavigateToStats: () -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {

    LaunchedEffect(Unit) {
        viewModel.refreshHabits()
    }

    val sortedHabits by viewModel.sortedHabits.collectAsState()
    val habitChecks by viewModel.habitChecks.collectAsState(initial = emptyMap())
    val endTime by viewModel.endTime.collectAsState(initial = LocalTime.of(0, 0, 0))
    val todayString = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val completedCount = habitChecks.values.count { it.date == todayString && it.isCompleted}

    var newHabitName by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val sortOption by viewModel.sortOption.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        listState = listState,
        onMove = { from, to ->
            viewModel.setSortOption(SortOption.MANUAL)
            viewModel.reorderHabits(from.index, to.index)
        }
    )
    val draggingIndex = reorderState.draggingItemIndex
    val keyboardController = LocalSoftwareKeyboardController.current
    var showDeleteDialog by remember { mutableStateOf<Habit?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopBarWithCountdownAndSettings(
                endTime = endTime,
                alarmEnabled = viewModel.alarmEnabled.collectAsState().value,
                context = LocalContext.current,
                onSettingsClick = { showSettings = true },
                onStatsClick = onNavigateToStats
            )
        },
        bottomBar = { AdMobBanner() }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp))
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            // 입력창
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newHabitName,
                    onValueChange = { newHabitName = it },
                    placeholder = {
                        Text(
                            "새로운 목표 아이템",
                            modifier = Modifier.padding(start = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedIndicatorColor = Color(0xFFBDBDBD),
                        unfocusedIndicatorColor = Color(0xFFBDBDBD),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newHabitName.isNotBlank()) {
                            viewModel.addHabit(newHabitName)
                            newHabitName = ""
                            keyboardController?.hide()
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("추가")
                }
            }

            // 완료 수 & 진행 바
            Text(
                text = "🏆 오늘 완료: $completedCount / ${sortedHabits.size}",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            LinearProgressIndicator(
                progress = if (sortedHabits.isNotEmpty()) completedCount / sortedHabits.size.toFloat() else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            // 습관 리스트
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .reorderable(reorderState)
                    .navigationBarsPadding()
            ) {
                itemsIndexed(sortedHabits) { index, habit ->
                    ReorderableItem(reorderState, key = habit.id) {
                        val isChecked = habitChecks[habit.id]?.let {
                            it.isCompleted && it.date == todayString
                        } ?: false
                        val isDragging = index == draggingIndex
                        val scale by animateFloatAsState(if (isDragging) 1.05f else 1f)
                        val elevation = if (isDragging) 8.dp else 0.dp
                        val bgColor = when {
                            isDragging -> Color(0xFFFFF3E0)
                            isChecked -> Color(0xFFE6F4EA).copy(alpha = 0.6f)
                            else -> Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .detectReorderAfterLongPress(reorderState)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .shadow(elevation, RoundedCornerShape(12.dp))
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(bgColor)
                                .animateContentSize()
                                .clickable {
                                    onNavigateToDetail(habit.id)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconToggleButton(
                                checked = isChecked,
                                onCheckedChange = {
                                    scope.launch { viewModel.toggleHabitCheck(habit) }
                                }
                            ) {
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .background(
                                                Color(0xFF8BC34A),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(4.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(
                                                2.dp,
                                                Color(0xFF795548),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = habit.name,
                                modifier = Modifier.weight(1f).alpha(if (isChecked) 0.5f else 1f),
                                textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Medium
                            )

                            IconButton(onClick = { showDeleteDialog = habit }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "삭제",
                                    tint = Color(0xFF757575), // 회색
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // 삭제 확인 다이얼로그
                        if (showDeleteDialog != null) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = null },
                                title = { Text("삭제 확인") },
                                text = {
                                    // 흔들림 방지를 위해 Box로 높이 고정
                                    Box(Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                    ) {
                                        Text(
                                            buildAnnotatedString {
                                                append("‘")
                                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                    append(showDeleteDialog?.name ?: "")
                                                }
                                                append("’을(를) 삭제하시겠습니까?")
                                            },
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.deleteHabit(showDeleteDialog!!)
                                        showDeleteDialog = null
                                    }) {
                                        Text("삭제", color = Color.Red)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = null }) {
                                        Text("취소")
                                    }
                                }
                            )
                        }

                        if (index != 0) {
                            Divider(color = Color(0xFFEADBB6), thickness = 1.dp)
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(48.dp)) // ✅ 마지막에 여백 추가
                }
            }

            if (showSettings) {
                SettingsDialog(viewModel = viewModel, onDismiss = { showSettings = false })
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
            .statusBarsPadding()
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