package com.bbks.mydailytracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class HabitResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("HabitResetReceiver", "리셋 알람 수신됨")
        // 자정 로직 처리 시작 (다음 단계에서 구현 예정)
        HabitResetWorker.enqueue(context)
    }
}