package com.bbks.mydailytracker

import SortOption
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.util.*

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    viewModel: HabitViewModel
) {
    val context = LocalContext.current

    // ✅ ViewModel 값들과 직접 바인딩 (앱 재시작 후에도 유지됨)
    val endTime by viewModel.endTime.collectAsState()
    val alarmEnabled by viewModel.alarmEnabled.collectAsState()
    val autoDelete by viewModel.autoDelete.collectAsState()
    val selectedSortOption by viewModel.sortOption.collectAsState()

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // 🔁 전체 알람 초기화 확인 다이얼로그
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("전체 알람 초기화") },
            text = { Text("모든 요일의 알람을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    val habitIds = viewModel.habits.value.map { it.id }
                    cancelAllAlarms(context, habitIds)
                    viewModel.disableAllHabitAlarms()
                    Toast.makeText(context, "모든 알람이 초기화되었습니다", Toast.LENGTH_SHORT).show()
                    showResetConfirmDialog = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // ⚙️ 설정 다이얼로그 본체
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") } // 저장 버튼 필요 없음: 즉시 저장되므로
        },
        dismissButton = null,
        title = { Text("설정") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                Text("목표 종료 시간", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text("%02d:%02d".format(endTime.hour, endTime.minute))
                val context = LocalContext.current
                Button(
                    onClick = {
                        showTimePickerDialog(context) { selectedTime ->
                            viewModel.setEndTime(context, LocalTime.of(selectedTime.first, selectedTime.second))
                        }
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("시간 선택")
                }

                Spacer(Modifier.height(16.dp))

                // 2️⃣ 정렬 방식
                Text("정렬 방식", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                DropdownMenuBox(
                    options = SortOption.values().toList(),
                    selected = selectedSortOption,
                    onSelect = { viewModel.setSortOption(it) }
                )

                Spacer(Modifier.height(16.dp))

                // 3️⃣ 알림 사용
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔔 알림 사용")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = alarmEnabled,
                        onCheckedChange = { viewModel.setAlarmEnabled(it) }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 4️⃣ 자동 초기화
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🗑️ 자동 초기화")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = autoDelete,
                        onCheckedChange = { viewModel.setAutoDelete(it) }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // 5️⃣ 전체 알람 초기화 버튼
                Button(
                    onClick = { showResetConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("전체 알람 초기화")
                }
            }
        }
    )
}

fun showTimePickerDialog(context: Context, onTimeSelected: (Pair<Int, Int>) -> Unit) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            onTimeSelected(selectedHour to selectedMinute)
        },
        hour, minute, true
    ).show()
}

@Composable
fun DropdownMenuBox(
    options: List<SortOption>,
    selected: SortOption,
    onSelect: (SortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { expanded = true }) {
        Text("정렬: ${selected.displayName}")
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.displayName) },
                onClick = {
                    onSelect(option)
                    expanded = false
                }
            )
        }
    }
}

