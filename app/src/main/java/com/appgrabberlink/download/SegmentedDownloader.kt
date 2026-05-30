package com.appgrabberlink.download

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class SegmentedDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun download(
        url: String,
        outputPath: String,
        segmentsCount: Int = 4,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val headRequest = Request.Builder().url(url).head().build()
            val headResponse = client.newCall(headRequest).execute()
            val totalSize = headResponse.body?.contentLength() ?: -1L
            val acceptRanges = headResponse.header("Accept-Ranges", "")

            if (totalSize <= 0 || acceptRanges != "bytes") {
                return@runCatching singleStreamDownload(url, outputPath, onProgress)
            }

            val segmentSize = totalSize / segmentsCount
            val tempDir = File(context.cacheDir, "segments_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val jobs = (0 until segmentsCount).map { i ->
                async(Dispatchers.IO) {
                    val start = i * segmentSize
                    val end = if (i == segmentsCount - 1) totalSize - 1 else (i + 1) * segmentSize - 1
                    val segFile = File(tempDir, "seg_$i")
                    downloadSegment(url, start, end, segFile)
                    segFile
                }
            }

            val segmentFiles = jobs.awaitAll()

            val output = File(outputPath)
            FileOutputStream(output).use { out ->
                segmentFiles.forEach { seg ->
                    seg.inputStream().use { it.copyTo(out, bufferSize = 8192) }
                    seg.delete()
                }
            }

            tempDir.delete()

            val downloaded = output.length()
            onProgress(if (downloaded > 0) 1f else 0f)

            output
        }
    }

    private fun downloadSegment(url: String, start: Long, end: Long, dest: File) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Range", "bytes=$start-$end")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Segment download failed: ${response.code}")
            }
            val body = response.body ?: throw java.io.IOException("Empty body")
            FileOutputStream(dest).use { out ->
                body.byteStream().use { input ->
                    input.copyTo(out, bufferSize = 8192)
                }
            }
        }
    }

    private fun singleStreamDownload(
        url: String,
        outputPath: String,
        onProgress: (Float) -> Unit
    ): File {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val total = response.body?.contentLength() ?: -1L
        val output = File(outputPath)
        var downloaded = 0L

        FileOutputStream(output).use { out ->
            response.body?.byteStream()?.use { input ->
                val buf = ByteArray(8192)
                var read: Int
                while (input.read(buf).also { read = it } != -1) {
                    out.write(buf, 0, read)
                    downloaded += read
                    if (total > 0) {
                        onProgress(downloaded.toFloat() / total)
                    }
                }
            }
        }
        return output
    }
}
