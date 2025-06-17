package com.bbks.mydailytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.bbks.mydailytracker.ui.theme.MyDailyTrackerTheme
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HabitViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            HabitDatabase::class.java,
            "habits.db"
        ).fallbackToDestructiveMigration().build()

        val habitDao = db.habitDao()
        val habitCheckDao = db.habitCheckDao()
        val factory = HabitViewModelFactory(habitDao, habitCheckDao)
        viewModel = ViewModelProvider(this, factory)[HabitViewModel::class.java]

        MobileAds.initialize(this) {}

        setContent {
            MyDailyTrackerTheme {
                HabitTrackerScreen(viewModel)
            }
        }
    }
}