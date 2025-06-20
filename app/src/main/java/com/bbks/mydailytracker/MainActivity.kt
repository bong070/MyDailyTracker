package com.bbks.mydailytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.bbks.mydailytracker.data.SettingsRepository
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

        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.VIBRATE),
            0
        )

        val habitDao = db.habitDao()
        val habitCheckDao = db.habitCheckDao()
        val settingsRepo = SettingsRepository(applicationContext)
        val factory = HabitViewModelFactory(habitDao, habitCheckDao, settingsRepo)
        viewModel = ViewModelProvider(this, factory)[HabitViewModel::class.java]

        MobileAds.initialize(this) {}

        setContent {
            MyDailyTrackerTheme {
                HabitTrackerScreen(viewModel)
            }
        }
    }
}