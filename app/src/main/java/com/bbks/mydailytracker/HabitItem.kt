package com.bbks.mydailytracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@Composable
fun HabitItem(
    habit: Habit,
    isChecked: Boolean,
    onCheckToggle: () -> Unit,
    onClick: () -> Unit,
    onRemove: (Habit) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("삭제 확인") },
            text = { Text("정말 \"${habit.name}\"을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(habit)
                    showDialog = false
                }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isChecked, onCheckedChange = { onCheckToggle() })
            Spacer(modifier = Modifier.width(8.dp))
            Text(habit.name)
        }

        IconButton(onClick = { showDialog = true }) {
            Icon(Icons.Default.Delete, contentDescription = "삭제")
        }
    }
}