package com.bbks.mydailytracker.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bbks.mydailytracker.alarm.AlarmStopReceiver
import com.bbks.mydailytracker.R
import com.bbks.mydailytracker.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var originalAlarmVolume: Int? = null
    private var habitTitle: String = ""
    private var notificationId: Int = 1

    companion object {
        var isRunning = false
        @Volatile
        var isRunningExternally: Boolean = false
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) return START_NOT_STICKY
        isRunning = true
        isRunningExternally = true

        habitTitle = intent?.getStringExtra("habitTitle")
            ?: getString(R.string.alarm_on)
        val habitId = intent?.getIntExtra("habitId", -1) ?: -1
        val dayOfWeek = intent?.getIntExtra("dayOfWeek", -1) ?: -1
        notificationId = if (habitId != -1 && dayOfWeek != -1) habitId * 10 + dayOfWeek else 1
        // ✅ 1. 시스템이 강제 종료 못하게 즉시 placeholder 알림 등록
        startForeground(notificationId, createPlaceholderNotification())

        // ✅ 2. 이후 코루틴으로 실제 작업 처리
        serviceScope.launch {
            showAlarmNotification() // 실제 알림 및 소리, 전체화면 알림 띄우기
        }

        return START_NOT_STICKY
    }

    private fun createPlaceholderNotification(): Notification {
        val channelId = "alarm_placeholder"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarm Placeholder",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Starting Alarm...")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
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
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "My Daily Tracker Alarm channel"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
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
        isRunning = false
        isRunningExternally = false
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        originalAlarmVolume?.let {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0)
        }

        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun playAlarmSound() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = SettingsRepository(applicationContext)
                val alarmVolume = repository.userPreferencesFlow.first().alarmVolume

                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                val desiredVolume = (alarmVolume * maxVolume).toInt().coerceIn(0, maxVolume)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, desiredVolume, 0)

                withContext(Dispatchers.Main) {
                    mediaPlayer = MediaPlayer().apply {
                        setAudioStreamType(AudioManager.STREAM_ALARM)
                        val afd = resources.openRawResourceFd(R.raw.alarm_sound)
                        //val afd = resources.openRawResourceFd(R.raw.alarm_sound_debug) //테스트용
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                        prepare()
                        isLooping = true
                        start()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}