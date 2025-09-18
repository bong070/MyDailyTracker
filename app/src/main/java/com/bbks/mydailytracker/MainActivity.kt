package com.bbks.mydailytracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.bbks.mydailytracker.ui.theme.MyDailyTrackerTheme
import com.google.android.gms.ads.MobileAds
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bbks.mydailytracker.ui.screen.StatisticsScreen
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bbks.mydailytracker.reset.ResetAlarmHelper
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.sqlite.db.SimpleSQLiteQuery
import com.bbks.mydailytracker.alarm.AlarmService
import com.bbks.mydailytracker.billing.BillingLauncher
import com.bbks.mydailytracker.billing.PremiumSync
import com.bbks.mydailytracker.data.db.HabitDatabase
import com.bbks.mydailytracker.data.repository.HabitRepository
import com.bbks.mydailytracker.data.repository.SettingsRepository
import com.bbks.mydailytracker.domain.viewmodel.HabitViewModel
import com.bbks.mydailytracker.domain.viewmodel.HabitViewModelFactory
import com.bbks.mydailytracker.ui.screen.HabitDetailScreen
import com.bbks.mydailytracker.ui.screen.HabitTrackerScreen
import com.bbks.mydailytracker.ui.screen.LockedContentScreen
import com.bbks.mydailytracker.util.RewardedAdController
import kotlin.jvm.java
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HabitViewModel
    private lateinit var billingLauncher: BillingLauncher
    private lateinit var rewardedAdController: RewardedAdController
    private lateinit var premiumSync: PremiumSync
    private val reqCalendarPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            if (!shouldShowRequestPermissionRationale(android.Manifest.permission.READ_CALENDAR)) {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            reqCalendarPermission.launch(android.Manifest.permission.READ_CALENDAR)
        }

        val db = HabitDatabase.getDatabase(applicationContext)

        val habitDao = db.habitDao()
        val habitCheckDao = db.habitCheckDao()
        val dailyHabitResultDao = db.dailyHabitResultDao()
        val settingsRepo = SettingsRepository(applicationContext)
        val habitRepo = HabitRepository(habitDao, habitCheckDao, dailyHabitResultDao)
        val factory = HabitViewModelFactory(habitDao, habitCheckDao, settingsRepo, habitRepo)
        viewModel = ViewModelProvider(this, factory)[HabitViewModel::class.java]

        MobileAds.initialize(this) {}
        //rewardedAdController = RewardedAdController(this, "ca-app-pub-2864557421723275/4196668759") //테스트용
        rewardedAdController = RewardedAdController(this, "ca-app-pub-7350776421233026/7085582206")
        rewardedAdController.loadAd()

        premiumSync = PremiumSync(
            app = application,
            premiumProductId = "premium_upgrade",
            setPremium = { active -> viewModel.setPremiumUser(active) },
            refreshPreferences = { viewModel.refreshPreferences() }
        )
        premiumSync.init()   // 앱 시작 + 포그라운드 복귀 때 자동 동기화

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

                //LaunchedEffect(Unit) { //테스트용
                //    viewModel.overridePremiumUserForDebug(true)
                //}

                Column {
                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(navController = navController, startDestination = "main") {
                            composable("main") {
                                HabitTrackerScreen(
                                    viewModel = viewModel,
                                    rewardedAdController = rewardedAdController,
                                    onNavigateToStats = { navController.navigate("statistics") },
                                    onNavigateToDetail = { habitId ->
                                        navController.navigate("detail/$habitId")
                                    },
                                    onUpgradeClick = {
                                        navController.navigate("locked")
                                    }
                                )
                            }

                            composable("statistics") {
                                StatisticsScreen(
                                    navController = navController,
                                    viewModel = viewModel
                                )
                            }

                            composable("detail/{habitId}") { backStackEntry ->
                                val habitId = backStackEntry.arguments?.getString("habitId")?.toIntOrNull()
                                if (habitId != null) {
                                    HabitDetailScreen(
                                        habitId = habitId,
                                        viewModel = viewModel,
                                        onBack = {
                                            if (navController.previousBackStackEntry != null) {
                                                navController.popBackStack()
                                            }
                                        }
                                    )
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
                val query = SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")
                db.openHelper.writableDatabase.query(query)
                Log.d("DB", "✅ WAL 병합 시도 완료")
            } catch (e: Exception) {
                Log.e("DB", "❌ WAL 병합 실패: ${e.localizedMessage}")
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    override fun onResume() {
        super.onResume()

        if (AlarmService.isRunningExternally) {
            Log.d("Alarm", "앱 열림 → 알람 강제 종료 시도")
            val intent = Intent(this, AlarmService::class.java)
            stopService(intent)
        }
    }
}