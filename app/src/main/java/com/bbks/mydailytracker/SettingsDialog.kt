package com.bbks.mydailytracker

import SortOption
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.*

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    viewModel: HabitViewModel,
    initialDayEndTime: Pair<Int, Int> = 23 to 59,
    initialAlarmEnabled: Boolean = false,
    initialAutoDelete: Boolean = false,
    initialSortOption: SortOption = SortOption.ALPHABETICAL,
    onSave: (dayEndTime: Pair<Int, Int>, alarmEnabled: Boolean, autoDelete: Boolean, SortOption) -> Unit
) {
    val context = LocalContext.current

    var dayEndTime by remember { mutableStateOf(initialDayEndTime) }
    var alarmEnabled by remember { mutableStateOf(initialAlarmEnabled) }
    var autoDelete by remember { mutableStateOf(initialAutoDelete) }
    var selectedSortOption by remember { mutableStateOf(initialSortOption) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // 🔁 전체 알람 초기화 확인 다이얼로그
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("전체 알람 초기화") },
            text = { Text("모든 요일의 알람을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    cancelAllAlarms(context)
                    viewModel.disableAllHabitAlarms()
                    Toast.makeText(context, "모든 알람이 초기화되었습니다", Toast.LENGTH_SHORT).show()
                    showResetConfirmDialog = false
                }) {
                    Text("확인")
                }
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
            TextButton(onClick = {
                onSave(dayEndTime, alarmEnabled, autoDelete, selectedSortOption)
                onDismiss()
            }) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        title = { Text("설정") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                // 1️⃣ My Day End Time
                Text("My Day End Time", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text("%02d:%02d".format(dayEndTime.first, dayEndTime.second))
                Button(
                    onClick = {
                        showTimePickerDialog(context) { selectedTime ->
                            dayEndTime = selectedTime
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
                    onSelect = { selectedSortOption = it }
                )

                Spacer(Modifier.height(16.dp))

                // 3️⃣ 알림 사용
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔔 알림 사용")
                    Spacer(Modifier.weight(1f))
                    Switch(checked = alarmEnabled, onCheckedChange = { alarmEnabled = it })
                }

                Spacer(Modifier.height(24.dp))

                // 4️⃣ 전체 알람 초기화
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
        Text("정렬: ${selected.name}")
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.name) },
                onClick = {
                    onSelect(option)
                    expanded = false
                }
            )
        }
    }
}
