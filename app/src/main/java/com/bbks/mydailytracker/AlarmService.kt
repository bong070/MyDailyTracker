package com.bbks.mydailytracker

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private var habitTitle: String = "알람이 울리고 있어요!"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        habitTitle = intent?.getStringExtra("habitTitle") ?: "알람이 울리고 있어요!"
        showAlarmNotification()
        return START_NOT_STICKY
    }

    private fun showAlarmNotification() {
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound).apply {
            isLooping = true
            start()
        }

        val channelId = "alarm_channel"
        val channelName = "Alarm Notifications"

        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("habitTitle", habitTitle)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val deleteIntent = Intent(this, AlarmStopReceiver::class.java)
        val deletePendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            deleteIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "My Daily Tracker Alarm channel"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("⏰ $habitTitle") // 사용자 정의 제목
            .setContentText("알람이 울리고 있습니다.")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setAutoCancel(true)
            .build()

        startForeground(1, notification)

        // 전체화면 AlarmActivity 실행
        startActivity(fullScreenIntent)
    }

    override fun onDestroy() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
