package com.aicallshield

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * AICallShield Application class.
 * Handles app-wide initialization including notification channels.
 */
class AICallShieldApp : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val screeningChannel = NotificationChannel(
                CHANNEL_SCREENING,
                getString(R.string.channel_screening),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_description)
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(screeningChannel)
        }
    }

    companion object {
        const val CHANNEL_SCREENING = "call_screening_channel"
        lateinit var appContext: Context
            private set
    }
}
