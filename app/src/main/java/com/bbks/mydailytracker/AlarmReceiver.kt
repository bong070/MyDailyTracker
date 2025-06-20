package com.bbks.mydailytracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.media.Ringtone
import android.net.Uri
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.util.Log
import android.widget.Toast
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)
        Log.d("AlarmReceiver", "알람 수신됨 ✅ dayOfWeek=${intent.getIntExtra("dayOfWeek", -1)}")
        // 1. 소리 재생
        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone: Ringtone = RingtoneManager.getRingtone(context, alarmUri)
        ringtone.play()

        // 2. 진동 (선택)
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.VIBRATE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(1000)
            }
        }

        // 3. 알림 표시 (선택)
        val notification = NotificationCompat.Builder(context, "alarm_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 알림 아이콘
            .setContentTitle("습관 알람")
            .setContentText("설정한 습관 알람 시간이 되었습니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1001, notification)

        // (선택) 사용자에게 알림
        Toast.makeText(context, "습관 알람이 울립니다!", Toast.LENGTH_SHORT).show()
    }
}

private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "습관 알람"
        val descriptionText = "습관 알람 알림 채널"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("alarm_channel", name, importance).apply {
            description = descriptionText
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
