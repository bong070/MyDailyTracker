package com.bbks.mydailytracker

import android.os.Bundle
import android.util.Log
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
import com.bbks.mydailytracker.reset.ResetAlarmHelper
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HabitViewModel
    private lateinit var billingLauncher: BillingLauncher

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

        MobileAds.initialize(this) {}

        setContent {
            MyDailyTrackerTheme {
                val navController = rememberNavController()
                val isPremiumUser by viewModel.isPremiumUser.collectAsState()

                billingLauncher = BillingLauncher(
                    activity = this,
                    lifecycleScope = lifecycleScope,
                    onPurchaseComplete = {
                        navController.popBackStack()
                    },
                    onPurchaseCancelled = {
                        navController.popBackStack()
                    },
                    setPremiumUser = { isPremium ->
                        viewModel.setPremiumUser(isPremium)
                    },
                    refreshPreferences = {
                        viewModel.refreshPreferences()
                    }
                )

                billingLauncher.setup()
                billingLauncher.restorePurchase()

                LaunchedEffect(Unit) { //테스트용
                    viewModel.overridePremiumUserForDebug(true)
                }

                Column {
                    Box(modifier = Modifier.weight(1f)) {
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
                                if (isPremiumUser) {
                                    StatisticsScreen(
                                        navController = navController,
                                        viewModel = viewModel
                                    )
                                } else {
                                    LaunchedEffect(Unit) {
                                        navController.navigate("locked") {
                                            popUpTo("statistics") { inclusive = true }
                                        }
                                    }
                                }
                            }

                            composable("detail/{habitId}") { backStackEntry ->
                                val habitId = backStackEntry.arguments?.getString("habitId")?.toIntOrNull()
                                if (habitId != null) {
                                    if (isPremiumUser) {
                                        HabitDetailScreen(
                                            habitId = habitId,
                                            viewModel = viewModel,
                                            onBack = {
                                                if (navController.previousBackStackEntry != null) {
                                                    navController.popBackStack()
                                                }
                                            }
                                        )
                                    } else {
                                        LaunchedEffect(Unit) {
                                            navController.navigate("locked") {
                                                popUpTo("detail/{habitId}") { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            }

                            composable("locked") {
                                LockedContentScreen(
                                    onUpgradeClick = {
                                        billingLauncher.launchPurchase("premium_upgrade")
                                    },
                                    onBack = {
                                        if (navController.previousBackStackEntry != null) {
                                            navController.popBackStack()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        ResetAlarmHelper.scheduleDailyResetAlarm(applicationContext)
        lifecycleScope.launch {
            try {
                db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
                Log.d("DB", "✅ WAL 병합 완료")
            } catch (e: Exception) {
                Log.e("DB", "❌ WAL 병합 실패: ${e.localizedMessage}")
            }
        }
    }
}