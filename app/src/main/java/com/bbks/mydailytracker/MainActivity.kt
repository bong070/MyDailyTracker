package com.bbks.mydailytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.bbks.mydailytracker.data.SettingsRepository
import com.bbks.mydailytracker.ui.theme.MyDailyTrackerTheme
import com.google.android.gms.ads.MobileAds
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bbks.mydailytracker.ui.statistics.StatisticsScreen
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HabitViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

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
        val dailyHabitResultDao = db.dailyHabitResultDao()
        val settingsRepo = SettingsRepository(applicationContext)
        val habitRepo = HabitRepository(habitDao, habitCheckDao, dailyHabitResultDao)
        val factory = HabitViewModelFactory(habitDao, habitCheckDao, settingsRepo, habitRepo)
        viewModel = ViewModelProvider(this, factory)[HabitViewModel::class.java]

        val resetManager = ResetManager(applicationContext)
        val resetLogic = HabitResetLogic(habitRepo)

        if (resetManager.shouldExecuteReset()) {
            lifecycleScope.launch {
                resetLogic.executeReset()
                resetManager.markResetDone()
            }
        }

        MobileAds.initialize(this) {}

        setContent {
            MyDailyTrackerTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        HabitTrackerScreen(
                            viewModel = viewModel,
                            onNavigateToStats = { navController.navigate("statistics") },
                            onNavigateToDetail = { habitId ->
                                navController.navigate("detail/$habitId")
                            }
                        )
                    }

                    composable("statistics") {
                        StatisticsScreen(navController)
                    }

                    composable("detail/{habitId}") { backStackEntry ->
                        val habitId = backStackEntry.arguments?.getString("habitId")?.toIntOrNull()
                        if (habitId != null) {
                            HabitDetailScreen(
                                habitId = habitId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}