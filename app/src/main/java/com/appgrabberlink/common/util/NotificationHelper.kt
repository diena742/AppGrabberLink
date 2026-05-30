package com.appgrabberlink.common.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.appgrabberlink.App
import com.appgrabberlink.MainActivity
import com.appgrabberlink.R

object NotificationHelper {

    fun showDownloadProgress(
        context: Context,
        taskId: String,
        progress: Int,
        max: Int
    ) {
        val notification = NotificationCompat.Builder(context, App.CHANNEL_DOWNLOAD)
            .setContentTitle("Downloading...")
            .setContentText("$progress% completed")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(max, progress, false)
            .build()

        notify(context, taskId.hashCode(), notification)
    }

    fun showDownloadComplete(
        context: Context,
        title: String,
        outputPath: String
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, App.CHANNEL_DOWNLOAD)
            .setContentTitle("Download Complete")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notify(context, System.currentTimeMillis().toInt(), notification)
    }

    fun showDownloadError(context: Context, title: String, error: String) {
        val notification = NotificationCompat.Builder(context, App.CHANNEL_DOWNLOAD)
            .setContentTitle("Download Failed")
            .setContentText("$title: $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()

        notify(context, System.currentTimeMillis().toInt(), notification)
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }
}
