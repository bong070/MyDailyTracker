package com.bbks.mydailytracker

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
    initialDayEndTime: Pair<Int, Int> = 23 to 59, // (hour, minute)
    initialAlarmEnabled: Boolean = false,
    initialAutoDelete: Boolean = false,
    onSave: (dayEndTime: Pair<Int, Int>, alarmEnabled: Boolean, autoDelete: Boolean) -> Unit
) {
    val context = LocalContext.current

    var dayEndTime by remember { mutableStateOf(initialDayEndTime) }
    var alarmEnabled by remember { mutableStateOf(initialAlarmEnabled) }
    var autoDelete by remember { mutableStateOf(initialAutoDelete) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(dayEndTime, alarmEnabled, autoDelete)
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
            Column {
                val (hour, minute) = dayEndTime
                Text("My Day End Time: %02d:%02d".format(hour, minute))
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    showTimePickerDialog(context) { selectedTime ->
                        dayEndTime = selectedTime
                    }
                }) {
                    Text("시간 선택")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("알람 사용")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(checked = alarmEnabled, onCheckedChange = { alarmEnabled = it })
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("완료 항목 자동 삭제")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(checked = autoDelete, onCheckedChange = { autoDelete = it })
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
