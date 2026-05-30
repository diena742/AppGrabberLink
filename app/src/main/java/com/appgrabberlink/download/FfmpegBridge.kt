package com.appgrabberlink.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FfmpegBridge {

    suspend fun concatSegments(
        segmentFiles: List<File>,
        outputPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val output = File(outputPath)
            FileOutputStream(output).use { out ->
                segmentFiles.forEach { seg ->
                    FileInputStream(seg).use { input ->
                        input.copyTo(out, bufferSize = 65536)
                    }
                    seg.delete()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun convertFormat(
        inputPath: String,
        outputPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Simple pass-through copy (format conversion requires FFmpeg binary)
            val input = File(inputPath)
            val output = File(outputPath)
            FileInputStream(input).use { input ->
                FileOutputStream(output).use { out ->
                    input.copyTo(out, bufferSize = 65536)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getMediaInfo(filePath: String): Map<String, String> = withContext(Dispatchers.IO) {
        val info = mutableMapOf<String, String>()
        val file = File(filePath)
        if (file.exists()) {
            info["size"] = file.length().toString()
        }
        info
    }
}
