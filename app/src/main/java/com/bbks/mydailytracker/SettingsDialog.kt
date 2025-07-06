package com.bbks.mydailytracker

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.clip

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    viewModel: HabitViewModel
) {
    val context = LocalContext.current
    val alarmEnabled by viewModel.alarmEnabled.collectAsState()
    val selectedSortOption by viewModel.sortOption.collectAsState()
    val habitIds = viewModel.habits.collectAsState().value.map { it.id }

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(stringResource(R.string.reset_all_alarms), color = MaterialTheme.colorScheme.onBackground) },
            text = { Text(stringResource(R.string.confirm_reset_all_alarms), color = MaterialTheme.colorScheme.onBackground) },
            confirmButton = {
                TextButton(onClick = {
                    cancelAllAlarms(context, habitIds)
                    viewModel.disableAllHabitAlarms()
                    Toast.makeText(context, context.getString(R.string.toast_reset_done), Toast.LENGTH_SHORT).show()
                    showResetConfirmDialog = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(min = 320.dp, max = 380.dp)
                .wrapContentHeight()
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.sort_mode), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
                    CustomSortDropdown(
                        selected = selectedSortOption,
                        onSelect = { viewModel.setSortOption(it) }
                    )
                }

                val dialogBackground = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                val borderColor = MaterialTheme.colorScheme.outline

                /*OutlinedButton(
                    onClick = {}, // 눌림 없음
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = dialogBackground,
                        disabledContainerColor = dialogBackground,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, borderColor),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.use_notifications),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Switch(
                            checked = alarmEnabled,
                            onCheckedChange = { viewModel.setAlarmEnabled(it) }
                        )
                    }
                }*/

                Button(
                    onClick = { showResetConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) {
                    Text(stringResource(R.string.reset_all_alarms), color = MaterialTheme.colorScheme.onBackground)
                }

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:bbksapps@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "My Daily Tracker Feedback")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(stringResource(R.string.send_feedback), color = MaterialTheme.colorScheme.onBackground)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.close), color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}

@Composable
fun CustomSortDropdown(
    selected: SortOption,
    onSelect: (SortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val borderColor = MaterialTheme.colorScheme.outline
    val dialogBackground = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(dialogBackground)
            .clickable { expanded = true }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = selected.labelResId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(dialogBackground)
                .widthIn(min = 200.dp)
        ) {
            SortOption.values().forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(option.labelResId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

