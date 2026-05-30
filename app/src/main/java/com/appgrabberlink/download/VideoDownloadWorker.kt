package com.appgrabberlink.download

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.appgrabberlink.App
import com.appgrabberlink.MainActivity
import com.appgrabberlink.R

class VideoDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val outputPath = inputData.getString(KEY_OUTPUT) ?: return Result.failure()
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val useYtdlp = inputData.getBoolean(KEY_USE_YTDLP, false)

        setForeground(createForegroundInfo())

        val success = if (useYtdlp) {
            val bridge = YtDlpBridge(applicationContext)
            bridge.download(url, outputPath)
        } else {
            val downloader = SegmentedDownloader(applicationContext)
            downloader.download(url, outputPath).isSuccess
        }

        if (success) {
            DownloadManager.onTaskCompleted(taskId)
            sendCompleteNotification(outputPath)
            Result.success()
        } else {
            DownloadManager.onTaskFailed(taskId)
            Result.retry()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = createNotification()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(applicationContext, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            applicationContext, 0, openIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(applicationContext, App.CHANNEL_DOWNLOAD)
            .setContentTitle("Downloading video...")
            .setContentText("AppGrabberLink is downloading your video")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
    }

    private fun sendCompleteNotification(outputPath: String) {
        val openIntent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(
                androidx.core.content.FileProvider.getUriForFile(
                    applicationContext,
                    "${applicationContext.packageName}.fileprovider",
                    java.io.File(outputPath)
                ),
                "video/*"
            )
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, openIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE
            else 0
        )

        val notification = NotificationCompat.Builder(applicationContext, App.CHANNEL_DOWNLOAD)
            .setContentTitle("Download Complete")
            .setContentText("Tap to open video")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val KEY_URL = "url"
        const val KEY_OUTPUT = "output"
        const val KEY_TASK_ID = "task_id"
        const val KEY_USE_YTDLP = "use_ytdlp"
    }
}
