package com.bbks.mydailytracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitTrackerScreen(viewModel: HabitViewModel) {
    val habits by viewModel.habits.collectAsState(initial = emptyList())
    var newHabitName by remember { mutableStateOf("") }
    var selectedHabit by remember { mutableStateOf<Habit?>(null) }

    if (selectedHabit != null) {
        CalendarScreen(viewModel, selectedHabit!!)
    } else {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("My Daily Tracker") })
            },
            content = { padding ->
                Column(modifier = Modifier.padding(padding).padding(16.dp)) {

                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = newHabitName,
                            onValueChange = { newHabitName = it },
                            label = { Text("Add New Tracking Item") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (newHabitName.isNotBlank()) {
                                viewModel.addHabit(newHabitName)
                                newHabitName = ""
                            }
                        }) {
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn {
                        items(habits) { habit ->
                            Button(
                                onClick = { selectedHabit = habit },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text(habit.name)
                            }
                        }
                    }
                }
            }
        )
    }
}