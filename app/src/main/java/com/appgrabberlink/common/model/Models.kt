package com.appgrabberlink.common.model

import java.util.UUID

data class VideoLink(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val sourceApp: String,
    val title: String = url.substringAfterLast(\"/\").substringBefore(\"?\"),
    val fileSize: Long = 0L,
    val mimeType: String = \"\",
    val format: VideoFormat = VideoFormat.UNKNOWN,
    val detectedAt: Long = System.currentTimeMillis(),
    val status: LinkStatus = LinkStatus.NEW
)

enum class VideoFormat {
    MP4, M3U8, MPD, MKV, WEBM, TS, FLV, AVI, UNKNOWN
}

enum class LinkStatus {
    NEW, QUEUED, DOWNLOADING, COMPLETED, FAILED
}

enum class CaptureMode {
    VPN_NON_ROOT,
    VPN_ROOT
}

data class AppTarget(
    val packageName: String,
    val appName: String,
    val enabled: Boolean = true
)

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val videoLink: VideoLink,
    val outputPath: String,
    val progress: Float = 0f,
    val segments: Int = 4,
    val status: DownloadStatus = DownloadStatus.PENDING
)

enum class DownloadStatus {
    PENDING, ACTIVE, PAUSED, COMPLETED, FAILED
}

data class ProgressInfo(
    val taskId: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val segmentsCompleted: Int,
    val totalSegments: Int
) {
    val progressFraction: Float
        get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
}
