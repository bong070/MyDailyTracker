package com.bbks.mydailytracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete

@Composable
fun HabitItem(
    habit: Habit,
    isChecked: Boolean,
    onCheckToggle: () -> Unit,
    onClick: () -> Unit,
    onRemove: (Habit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val textColor = if (isChecked) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // 기본 배경색
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = isChecked, onCheckedChange = { onCheckToggle() })
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = habit.name,
                    color = textColor,
                    textDecoration = textDecoration
                )
            }

            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "삭제")
            }
        }
    }
}
