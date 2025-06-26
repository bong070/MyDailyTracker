package com.bbks.mydailytracker.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bbks.mydailytracker.HabitViewModel
import com.bbks.mydailytracker.model.DayStats
import com.bbks.mydailytracker.ui.common.MyAppTopBar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DayStats(
    val label: String,
    val success: Int,
    val failure: Int,
    val failedHabits: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(navController: NavController, viewModel: HabitViewModel) {
    val today = LocalDate.now()
    var currentStartOfWeek by remember { mutableStateOf(today.with(DayOfWeek.MONDAY)) }

    val endOfWeek = currentStartOfWeek.plusDays(6)
    val formatter = DateTimeFormatter.ofPattern("M/d/yyyy")
    val dateRangeText = "${currentStartOfWeek.format(formatter)} - ${endOfWeek.format(formatter)}"

    var selectedDay by remember { mutableStateOf(0) }
    val stats by viewModel.getWeekStatsForUI().collectAsState()
    val selectedDayIndex = selectedDay.coerceAtMost(stats.lastIndex)
    val failedHabits = stats.getOrNull(selectedDayIndex)?.failedHabits.orEmpty()
    val successColor = MaterialTheme.colorScheme.primary
    val failureColor = MaterialTheme.colorScheme.error

    val isRightArrowEnabled = currentStartOfWeek.plusWeeks(1).isBefore(
        today.with(DayOfWeek.MONDAY).plusDays(7)
    )

    val context = LocalContext.current
    val beigeBackground = Color(0xFFFFF8E1)
    val emphasizedCardBackground = Color(0xFFFFF3C0)

    Scaffold(
        topBar = {
            MyAppTopBar(
                title = "통계",
                onBack = { navController.popBackStack() },
                backgroundColor = beigeBackground
            )
        },
        containerColor = beigeBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(beigeBackground)
                .systemBarsPadding()
                .padding(16.dp)
        ) {
            item {
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
                        stats = stats,
                        selectedDay = selectedDay,
                        onDaySelected = { selectedDay = it },
                        successColor = successColor,
                        failureColor = failureColor
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (stats.isNotEmpty()) {
                    Text(
                        "Failed Habits - ${stats.getOrNull(selectedDay)?.label ?: "-"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (failedHabits.isEmpty()) {
                    Text(
                        "No failed habits 😊",
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }
            }

            items(failedHabits) { habit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = emphasizedCardBackground
                    ),
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
        }
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
    val barWidth = 20.dp
    val maxBarHeight = 160.dp
    val gridLineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
    val emphasizedCardBackground = Color(0xFFFFF3C0)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxBarHeight + 40.dp),
        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp), // 아래만 둥글게
        colors = CardDefaults.cardColors(containerColor = emphasizedCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxBarHeight + 40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(emphasizedCardBackground, shape = RoundedCornerShape(8.dp))
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

                stats.forEachIndexed { index, day ->
                    val total = day.success + day.failure
                    if (total == 0) return@forEachIndexed
                    val successRatio = if (total == 0) 0f else day.success.toFloat() / total
                    val failureRatio = 1f - successRatio

                    val left = columnWidth * index + (columnWidth - barWidth.toPx()) / 2
                    val bottom = size.height - 20.dp.toPx()

                    val failureHeight = barMaxHeightPx * failureRatio
                    val successHeight = barMaxHeightPx * successRatio

                    if (failureHeight > 0f) {
                        drawRoundRect(
                            color = failureColor,
                            topLeft = Offset(left, bottom - failureHeight - successHeight),
                            size = androidx.compose.ui.geometry.Size(
                                barWidth.toPx(),
                                failureHeight
                            ),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                    if (failureHeight > 0f) {
                        drawRoundRect(
                            color = successColor,
                            topLeft = Offset(left, bottom - successHeight),
                            size = androidx.compose.ui.geometry.Size(
                                barWidth.toPx(),
                                successHeight
                            ),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Bottom
            ) {
                stats.forEachIndexed { index, day ->
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black,
                        modifier = Modifier
                            .clickable { onDaySelected(index) }
                            .padding(top = maxBarHeight)
                    )
                }
            }
        }
    }
}
