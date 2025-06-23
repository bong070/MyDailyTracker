package com.bbks.mydailytracker

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bbks.mydailytracker.ui.theme.MyDailyTrackerTheme

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val title = intent?.getStringExtra("habitTitle") ?: "알람이 울리고 있어요!"
        super.onCreate(savedInstanceState)

        // 전체 화면 및 화면 켜기
        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContent {
            MyDailyTrackerTheme {
                AlarmUIScreen(
                    title = title,
                    onStopAlarm = {
                        stopService(Intent(this, AlarmService::class.java))
                        finishAffinity() // 액티비티 및 백스택 완전 종료
                    }
                )
            }
        }
    }

    // 뒤로가기 버튼도 알람 종료로 처리
    override fun onBackPressed() {
        stopService(Intent(this, AlarmService::class.java))
        finishAffinity()
    }
}

@Composable
fun AlarmUIScreen(title: String, onStopAlarm: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onStopAlarm) {
                Text("알람 멈추기")
            }
        }
    }
}
