package com.example.smsmvp

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*

class PollService : Service() {
    private val TAG = "PollService"
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "sms_mvp_channel"
    private var pollingJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startPolling()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS MVP Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps SMS MVP running in background"
            }
            
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS MVP")
            .setContentText("Monitoring SMS and polling server")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .build()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    Net.pollForCommands()
                    delay(30000) // Poll every 30 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Polling error", e)
                    delay(60000) // Wait longer on error
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
    }
}
