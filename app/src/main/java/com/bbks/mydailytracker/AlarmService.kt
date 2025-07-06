package com.bbks.mydailytracker

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private var habitTitle: String = ""
    private var notificationId: Int = 1

    companion object {
        var isRunning = false
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) return START_NOT_STICKY
        isRunning = true

        habitTitle = intent?.getStringExtra("habitTitle")
            ?: getString(R.string.alarm_on)
        val habitId = intent?.getIntExtra("habitId", -1) ?: -1
        val dayOfWeek = intent?.getIntExtra("dayOfWeek", -1) ?: -1
        notificationId = if (habitId != -1 && dayOfWeek != -1) habitId * 10 + dayOfWeek else 1
        showAlarmNotification()
        return START_NOT_STICKY
    }

    private fun showAlarmNotification() {
        playAlarmSound()

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
            notificationId,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val deleteIntent = Intent(this, AlarmStopReceiver::class.java)
        val deletePendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
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
            .setContentText(getString(R.string.alarm_on))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setAutoCancel(true)
            .build()

        startForeground(notificationId, notification)
    }

    override fun onDestroy() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun playAlarmSound() {
        mediaPlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_ALARM)
            val afd = resources.openRawResourceFd(R.raw.alarm_sound)
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            prepare()
            isLooping = true
            start()
        }
    }
}
