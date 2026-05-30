package com.appgrabberlink

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {

    companion object {
        const val CHANNEL_CAPTURE = \"capture_service\"
        const val CHANNEL_FLOATING = \"floating_service\"
        const val CHANNEL_DOWNLOAD = \"download_service\"
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            listOf(
                NotificationChannel(
                    CHANNEL_CAPTURE,
                    \"Capture Service\",
                    NotificationManager.IMPORTANCE_LOW
                ),
                NotificationChannel(
                    CHANNEL_FLOATING,
                    \"Floating Panel\",
                    NotificationManager.IMPORTANCE_LOW
                ),
                NotificationChannel(
                    CHANNEL_DOWNLOAD,
                    \"Download Service\",
                    NotificationManager.IMPORTANCE_LOW
                )
            ).forEach { channel ->
                channel.setShowBadge(false)
                manager.createNotificationChannel(channel)
            }
        }
    }
}
