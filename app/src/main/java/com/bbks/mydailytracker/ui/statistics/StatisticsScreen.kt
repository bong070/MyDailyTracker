package com.bbks.mydailytracker.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bbks.mydailytracker.HabitViewModel
import com.bbks.mydailytracker.model.DayStats
import com.bbks.mydailytracker.ui.common.MyAppTopBar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(navController: NavController, viewModel: HabitViewModel) {
    val today = LocalDate.now()
    var currentStartOfWeek by remember { mutableStateOf(today.with(DayOfWeek.MONDAY)) }

    val endOfWeek = currentStartOfWeek.plusDays(6)
    val formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
    val dateRangeText = "${currentStartOfWeek.format(formatter)} - ${endOfWeek.format(formatter)}"

    var selectedDay by remember { mutableStateOf(0) }
    val selectedWeekRange = currentStartOfWeek..endOfWeek
    val stats by viewModel.getWeekStatsForUI().collectAsState()
    val filteredStats = stats.filter { it.date in selectedWeekRange }
    val selectedDayIndex = selectedDay.coerceAtMost(stats.lastIndex)
    val selectedStats = filteredStats.getOrNull(selectedDayIndex)
    val successColor = MaterialTheme.colorScheme.primary
    val failureColor = MaterialTheme.colorScheme.error

    val isRightArrowEnabled = currentStartOfWeek.plusWeeks(1).isBefore(
        today.with(DayOfWeek.MONDAY).plusDays(7)
    )

    val beigeBackground = Color(0xFFFFF8E1)
    val emphasizedCardBackground = Color(0xFFFFF3C0)
    var showSuccessList by remember { mutableStateOf(false) }
    var showFailureList by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MyAppTopBar(
                title = "통계",
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                } },
                backgroundColor = beigeBackground
            )
        },
        containerColor = beigeBackground
    ) { innerPadding ->
        val contentBottomPadding = innerPadding.calculateBottomPadding()
        LazyColumn(
            modifier = Modifier
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                            56.dp,
                    bottom = contentBottomPadding
                )
                .fillMaxSize()
                .background(beigeBackground)
                .navigationBarsPadding()
                .padding(4.dp)
        ) {
            item {
                val totalSuccess = stats
                    .filter { it.date in selectedWeekRange }
                    .sumOf { it.success }

                val totalFailure = stats
                    .filter { it.date in selectedWeekRange }
                    .sumOf { it.failure }


                Text(
                    text = "📊 총 성공: $totalSuccess / 실패: $totalFailure",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium

                )

                Spacer(modifier = Modifier.height(12.dp))

                // 주간 통계 제목
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = emphasizedCardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "주간 통계",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 날짜 범위와 화살표
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "<",
                            modifier = Modifier
                                .clickable {
                                    currentStartOfWeek = currentStartOfWeek.minusWeeks(1)
                                }
                                .padding(horizontal = 12.dp),
                            color = Color.Black,
                        )
                        Text(
                            text = dateRangeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black,
                        )
                        Text(
                            text = ">",
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .let {
                                    if (!isRightArrowEnabled) it else it.clickable {
                                        currentStartOfWeek = currentStartOfWeek.plusWeeks(1)
                                    }
                                },
                            color = if (isRightArrowEnabled)
                                Color.Black
                            else
                                Color.Black.copy(alpha = 0.1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        LegendIndicator(successColor, "Success")
                        LegendIndicator(failureColor, "Failure")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (stats.isNotEmpty()) {
                    BarChart(
                        stats = filteredStats,
                        selectedDay = selectedDay,
                        onDaySelected = { selectedDay = it },
                        successColor = successColor,
                        failureColor = failureColor
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                val selectedDayStats = filteredStats.getOrNull(selectedDayIndex)

                if (selectedDayStats != null) {
                    Text(
                        text = "✅ ${selectedDayStats.label} - 성공: ${selectedDayStats.success}, 실패: ${selectedDayStats.failure}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    onClick = { showSuccessList = !showSuccessList },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFA5D6A7)), // 톤 다운된 민트
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = selectedStats?.label?.takeIf { it.isNotBlank() }?.let {
                                    "성공한 목표 - $it"
                                } ?: "성공한 목표",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (showSuccessList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                        if (showSuccessList) {
                            Divider(color = Color.Gray.copy(alpha = 0.3f))
                            val successHabits = selectedStats?.successHabits.orEmpty()
                            if (successHabits.isEmpty()) {
                                Text(
                                    "성공한 목표가 없습니다.",
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                )
                            } else {
                                successHabits.forEachIndexed { index, habit ->
                                    Column {
                                        Text(
                                            text = habit,
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            color = Color.Black
                                        )
                                        if (index != successHabits.lastIndex)
                                            Divider(color = Color.Gray.copy(alpha = 0.2f))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    onClick = { showFailureList = !showFailureList },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF28B82)), // 톤 다운된 라이트레드
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = selectedStats?.label?.takeIf { it.isNotBlank() }?.let {
                                    "실패한 목표 - $it"
                                } ?: "실패한 목표",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (showFailureList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                        if (showFailureList) {
                            Divider(color = Color.Gray.copy(alpha = 0.3f))
                            val failedHabits = selectedStats?.failedHabits.orEmpty()
                            if (failedHabits.isEmpty()) {
                                Text(
                                    "실패한 목표가 없습니다. 🎉",
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                )
                            } else {
                                failedHabits.forEachIndexed { index, habit ->
                                    Column {
                                        Text(
                                            text = habit,
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            color = Color.Black
                                        )
                                        if (index != failedHabits.lastIndex)
                                            Divider(color = Color.Gray.copy(alpha = 0.2f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitCard(habit: String, background: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = habit,
            modifier = Modifier.padding(12.dp),
            color = Color.Black
        )
    }
}

@Composable
fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = Color.Black
        )
    }
}

@Composable
fun BarChart(
    stats: List<DayStats>,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    successColor: Color,
    failureColor: Color
) {
    val barWidth = 14.dp
    val gap = 4.dp
    val maxBarHeight = 160.dp
    val gridLineColor = Color.Gray.copy(alpha = 0.2f)
    val emphasizedCardBackground = Color(0xFFFFF3C0)

    Card(
        modifier = Modifier.fillMaxWidth().height(maxBarHeight + 40.dp),
        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
        colors = CardDefaults.cardColors(containerColor = emphasizedCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxBarHeight + 40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(emphasizedCardBackground)
                .padding(vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val columnWidth = size.width / stats.size
                val barMaxHeightPx = maxBarHeight.toPx()

                val step = barMaxHeightPx / 4
                repeat(5) {
                    val y = it * step
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val maxTotal = if (stats.isNotEmpty()) {
                    stats.maxOf { it.success + it.failure }.coerceAtLeast(1)
                } else {
                    1 // 기본값
                }

                stats.forEachIndexed { index, day ->
                    val total = day.success + day.failure
                    if (total == 0) return@forEachIndexed

                    val leftSuccess = columnWidth * index + (columnWidth - 2 * barWidth.toPx() - gap.toPx()) / 2
                    val leftFailure = leftSuccess + barWidth.toPx() + gap.toPx()
                    val bottom = size.height - 20.dp.toPx()

                    val successHeight = barMaxHeightPx * (day.success.toFloat() / maxTotal)
                    val failureHeight = barMaxHeightPx * (day.failure.toFloat() / maxTotal)

                    drawRoundRect(
                        color = successColor,
                        topLeft = Offset(leftSuccess, bottom - successHeight),
                        size = Size(barWidth.toPx(), successHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )

                    drawRoundRect(
                        color = failureColor,
                        topLeft = Offset(leftFailure, bottom - failureHeight),
                        size = Size(barWidth.toPx(), failureHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxSize().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Bottom
            ) {
                stats.forEachIndexed { index, day ->
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black,
                        modifier = Modifier.clickable { onDaySelected(index) }.padding(top = maxBarHeight)
                    )
                }
            }
        }
    }
}
