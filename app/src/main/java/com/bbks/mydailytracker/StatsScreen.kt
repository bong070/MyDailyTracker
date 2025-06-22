package com.bbks.mydailytracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: HabitViewModel, onBack: () -> Unit) {
    val weeklyStats by viewModel.weeklyStats.collectAsState()

    val grouped = weeklyStats.groupBy { it.date }
    val dates = generatePast7Days() // 최근 7일 날짜 리스트

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 주간 통계") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(16.dp)) {

            dates.forEach { date ->
                val results = grouped[date].orEmpty()
                val successCount = results.count { it.isSuccess }
                val failCount = results.count { !it.isSuccess }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(date, style = MaterialTheme.typography.bodyMedium)
                    Text("✅ $successCount · ❌ $failCount", style = MaterialTheme.typography.bodyMedium)
                }

                LinearProgressIndicator(
                    progress = if (results.isNotEmpty()) successCount / results.size.toFloat() else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }

            if (weeklyStats.isEmpty()) {
                Spacer(Modifier.height(48.dp))
                Text(
                    "최근 7일 동안의 기록이 없어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun generatePast7Days(): List<String> {
    val today = LocalDate.now()
    return (0..6).map { today.minusDays(it.toLong()).toString() }.reversed()
}
