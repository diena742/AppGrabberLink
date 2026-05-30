package com.appgrabberlink.download

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.appgrabberlink.common.model.DownloadStatus
import com.appgrabberlink.common.model.DownloadTask
import com.appgrabberlink.common.model.VideoFormat
import com.appgrabberlink.common.model.VideoLink
import com.appgrabberlink.ui.floating.FloatingViewModel
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

object DownloadManager {

    private val queue = ConcurrentLinkedQueue<DownloadTask>()
    private val activeTasks = mutableMapOf<String, DownloadTask>()

    fun enqueue(context: Context, link: VideoLink) {
        val task = DownloadTask(
            videoLink = link,
            outputPath = generateOutputPath(context, link)
        )
        queue.add(task)
        FloatingViewModel.instance.incrementQueue()
        processNext(context)
    }

    private fun processNext(context: Context) {
        val task = queue.poll() ?: return
        activeTasks[task.id] = task.copy(status = DownloadStatus.ACTIVE)

        val useYtdlp = task.videoLink.format == VideoFormat.UNKNOWN ||
                task.videoLink.format == VideoFormat.M3U8

        val inputData = Data.Builder()
            .putString(VideoDownloadWorker.KEY_URL, task.videoLink.url)
            .putString(VideoDownloadWorker.KEY_OUTPUT, task.outputPath)
            .putString(VideoDownloadWorker.KEY_TASK_ID, task.id)
            .putBoolean(VideoDownloadWorker.KEY_USE_YTDLP, useYtdlp)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
            .setInputData(inputData)
            .addTag("download_${task.id}")
            .build()

        WorkManager.getInstance(context)
            .enqueue(workRequest)
    }

    private fun generateOutputPath(context: Context, link: VideoLink): String {
        val dir = java.io.File(
            android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            ),
            "AppGrabberLink"
        )
        dir.mkdirs()
        val extension = when (link.format) {
            VideoFormat.MP4 -> "mp4"
            VideoFormat.M3U8 -> "ts"
            VideoFormat.MKV -> "mkv"
            VideoFormat.WEBM -> "webm"
            VideoFormat.TS -> "ts"
            else -> "mp4"
        }
        val filename = link.title
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(100) + "_${System.currentTimeMillis()}.$extension"
        return java.io.File(dir, filename).absolutePath
    }

    fun onTaskCompleted(taskId: String) {
        activeTasks.remove(taskId)
        FloatingViewModel.instance.decrementQueue()
    }

    fun onTaskFailed(taskId: String) {
        activeTasks.remove(taskId)
        FloatingViewModel.instance.decrementQueue()
    }

    fun removeFromQueue(url: String) {
        queue.removeAll { it.videoLink.url == url }
    }
}
