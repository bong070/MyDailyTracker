package com.bbks.mydailytracker.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.shadow
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.collections.forEachIndexed
import kotlin.collections.lastIndex
import kotlin.collections.orEmpty
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.res.stringResource
import com.bbks.mydailytracker.R
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(navController: NavController, viewModel: HabitViewModel) {
    val isDark = isSystemInDarkTheme()

    val toggleBackground = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFF3C0)
    val selectedTabColor = if (isDark) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFFE082)
    val dimTextColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.DarkGray

    val tabTitles = listOf(stringResource(R.string.weekly_statistics),
        stringResource(R.string.monthly_statistics))
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            MyAppTopBar(
                title = stringResource(R.string.statistics),
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(toggleBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(
                                elevation = if (isSelected) 2.dp else 0.dp,
                                shape = RoundedCornerShape(20.dp),
                                clip = false
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) selectedTabColor else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else dimTextColor,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> WeeklyStatsScreen(viewModel = viewModel)
                1 -> MonthlyStatsScreen(viewModel = viewModel)
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
            color = MaterialTheme.colorScheme.onBackground
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
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val emphasizedCardBackground = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxBarHeight + 40.dp),
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

                val maxTotal = stats.maxOfOrNull { it.success + it.failure }?.coerceAtLeast(1) ?: 1

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
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .clickable { onDaySelected(index) }
                            .padding(top = maxBarHeight)
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyStatsScreen(viewModel: HabitViewModel) {
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

    val beigeBackground = MaterialTheme.colorScheme.background
    val emphasizedCardBackground = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }
    var showSuccessList by remember { mutableStateOf(false) }
    var showFailureList by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(beigeBackground)
            .navigationBarsPadding()
            .padding(4.dp)
    ) {
        item {
            val totalSuccess = filteredStats.sumOf { it.success }
            val totalFailure = filteredStats.sumOf { it.failure }
            val totalCount = totalSuccess + totalFailure
            val successRate = if (totalCount > 0) (totalSuccess * 100 / totalCount) else 0

            Text(
                text = stringResource(R.string.total_success_failure, totalSuccess, totalFailure, successRate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 주간 통계 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                colors = CardDefaults.cardColors(containerColor = emphasizedCardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.weekly_statistics),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "<",
                            modifier = Modifier
                                .clickable { currentStartOfWeek = currentStartOfWeek.minusWeeks(1) }
                                .padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = dateRangeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
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
                                MaterialTheme.colorScheme.onBackground
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendIndicator(successColor, stringResource(R.string.success))
                        LegendIndicator(failureColor, stringResource(R.string.failure))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (filteredStats.isNotEmpty()) {
                BarChart(
                    stats = filteredStats,
                    selectedDay = selectedDay,
                    onDaySelected = { selectedDay = it },
                    successColor = successColor,
                    failureColor = failureColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            selectedStats?.let {
                val total = it.success + it.failure
                val rate = if (total > 0) (it.success * 100 / total) else 0
                Text(
                    text = stringResource(
                        R.string.success_failure_format,
                        it.label,
                        it.success,
                        it.failure,
                        rate
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            val successCardColor = if (isSystemInDarkTheme()) Color(0xFF66BB6A) else Color(0xFFA5D6A7)
            val failureCardColor = if (isSystemInDarkTheme()) Color(0xFFE57373) else Color(0xFFF28B82)

            // ✅ 성공 카드
            Card(
                onClick = { showSuccessList = !showSuccessList },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = successCardColor),
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
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = selectedStats?.label?.let {
                                stringResource(R.string.success_label_with_day, it)
                            } ?: stringResource(R.string.success_label_default),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showSuccessList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (showSuccessList) {
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        val successHabits = selectedStats?.successHabits.orEmpty()
                        if (successHabits.isEmpty()) {
                            Text(
                                stringResource(R.string.no_success),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                        } else {
                            successHabits.forEachIndexed { index, habit ->
                                Column {
                                    Text(
                                        text = habit,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (index != successHabits.lastIndex)
                                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ✅ 실패 카드
            Card(
                onClick = { showFailureList = !showFailureList },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = failureCardColor),
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
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = selectedStats?.label?.let {
                                stringResource(R.string.failure_label_with_day, it)
                            } ?: stringResource(R.string.failure_label_default),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showFailureList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (showFailureList) {
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        val failedHabits = selectedStats?.failedHabits.orEmpty()
                        if (failedHabits.isEmpty()) {
                            Text(
                                stringResource(R.string.no_failure),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                        } else {
                            failedHabits.forEachIndexed { index, habit ->
                                Column {
                                    Text(
                                        text = habit,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (index != failedHabits.lastIndex)
                                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
fun MonthlyStatsScreen(viewModel: HabitViewModel, modifier: Modifier = Modifier) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val habitMap = habits.associateBy { it.id }

    val isDark = isSystemInDarkTheme()
    val beigeBackground = MaterialTheme.colorScheme.background
    val textColor = if (isDark) MaterialTheme.colorScheme.onBackground else Color.Black

    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 일요일 = 0

    val daysInMonth = lastDayOfMonth.dayOfMonth
    val totalGridCount = startDayOfWeek + daysInMonth
    val weeks = (totalGridCount + 6) / 7

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }


    val statsByDate = remember(monthlyStats, habits) {
        monthlyStats
            .groupBy { LocalDate.parse(it.date) }
            .mapValues { (parsedDate, entries) ->
                val filtered = entries.filter { result ->
                    val habit = habitMap[result.habitId] ?: return@filter false
                    val created = LocalDate.parse(habit.createdDate)
                    val isRepeatDay = habit.repeatDays.contains(parsedDate.dayOfWeek.value)
                    val isSameDay = created == parsedDate
                    habit.repeatDays.isEmpty() && isSameDay || isRepeatDay
                }
                val success = filtered.filter { it.isSuccess }.mapNotNull { it.habitName }
                val failure = filtered.filter { !it.isSuccess }.mapNotNull { it.habitName }
                success to failure
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(beigeBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val (totalSuccess, totalFailure) = statsByDate.values.fold(0 to 0) { acc, (success, failure) ->
            acc.first + success.size to acc.second + failure.size
        }
        val monthlyRate = if (totalSuccess + totalFailure > 0) {
            (totalSuccess * 100) / (totalSuccess + totalFailure)
        } else null

        // 상단 년/월 + 화살표
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.setCurrentMonth(currentMonth.minusMonths(1)) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.previous_month), tint = textColor)
            }
            Text(
                text = if (monthlyRate != null) {
                    stringResource(
                        R.string.year_month_with_rate_format,
                        currentMonth.year,
                        currentMonth.monthValue,
                        monthlyRate
                    )
                } else {
                    stringResource(
                        R.string.year_month_format,
                        currentMonth.year,
                        currentMonth.monthValue
                    )
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textColor
            )
            IconButton(
                onClick = {
                    if (currentMonth.isBefore(YearMonth.now())) {
                        viewModel.setCurrentMonth(currentMonth.plusMonths(1))
                    }
                }
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = stringResource(R.string.next_month),
                    tint = if (currentMonth.isBefore(YearMonth.now())) textColor else Color.Gray.copy(alpha = 0.2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 요일 헤더
        val dayLabels = listOf(
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        ).map {
            it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dayLabels.forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 날짜 그리드
        Column {
            repeat(weeks) { weekIndex ->
                Row(Modifier.fillMaxWidth()) {
                    for (dayIndex in 0..6) {
                        val cellIndex = weekIndex * 7 + dayIndex
                        val dayNumber = cellIndex - startDayOfWeek + 1

                        val date = if (dayNumber in 1..daysInMonth) currentMonth.atDay(dayNumber) else null
                        val (successList, failureList) = statsByDate[date] ?: Pair(emptyList(), emptyList())
                        val totalCount = successList.size + failureList.size
                        val rate = if (totalCount > 0) (successList.size * 100) / totalCount else null

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        date == null -> Color.Transparent
                                        //selectedDate == date -> Color(0xFF4CAF50)
                                        else -> getColorForSuccessRate(rate)
                                    }
                                )
                                .clickable(enabled = date != null) {
                                    selectedDate = date
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNumber in 1..daysInMonth) {
                                Text(
                                    text = "$dayNumber",
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedDate == date) Color.White else textColor
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        selectedDate?.let { date ->
            val (successHabits, failureHabits) = statsByDate[date] ?: Pair(emptyList(), emptyList())
            val totalCount = successHabits.size + failureHabits.size
            val successRate = if (totalCount > 0) (successHabits.size * 100) / totalCount else null
            val successCardColor = if (isDark) Color(0xFF66BB6A) else Color(0xFFA5D6A7)
            val failureCardColor = if (isDark) Color(0xFFE57373) else Color(0xFFF28B82)

            // 성공 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = successCardColor),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (successRate != null) {
                            stringResource(
                                R.string.success_goals_with_date_and_rate,
                                date.monthValue,
                                date.dayOfMonth,
                                successRate
                            )
                        } else {
                            stringResource(
                                R.string.success_goals_with_date,
                                date.monthValue,
                                date.dayOfMonth
                            )
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (successHabits.isEmpty()) {
                        Text(text = stringResource(R.string.no_success), color = textColor)
                    } else {
                        successHabits.forEach { habit ->
                            Text("• $habit", color = textColor)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 실패 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = failureCardColor),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(
                            R.string.failed_goals_with_date,
                            date.monthValue,
                            date.dayOfMonth
                        ),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (failureHabits.isEmpty()) {
                        Text(text = stringResource(R.string.no_failure), color = textColor)
                    } else {
                        failureHabits.forEach { habit ->
                            Text("• $habit", color = textColor)
                        }
                    }
                }
            }
        }
    }
}

fun getColorForSuccessRate(rate: Int?): Color {
    return when {
        rate == null -> Color(0xFF757575)
        rate == 0 -> Color(0xFF757575)
        rate in 1..25 -> Color(0xFFE57373)
        rate in 26..50 -> Color(0xFFFFAB91)
        rate in 51..75 -> Color(0xFFFBC02D)
        else -> Color(0xFF81C784)
    }
}