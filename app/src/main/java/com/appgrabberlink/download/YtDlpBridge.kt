package com.appgrabberlink.download

import android.content.Context
import com.appgrabberlink.engine.RootHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class FormatInfo(
    val formatId: String,
    val extension: String,
    val resolution: String?,
    val filesize: Long?,
    val note: String?
)

class YtDlpBridge(private val context: Context) {

    private val binaryPath: String by lazy {
        val dest = File(context.filesDir, "bin/yt-dlp")
        if (!dest.exists()) {
            dest.parentFile?.mkdirs()
            context.assets.open("yt-dlp_arm64").use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }
            dest.setExecutable(true)
        }
        dest.absolutePath
    }

    suspend fun getFormats(url: String): List<FormatInfo> = withContext(Dispatchers.IO) {
        val cmd = if (RootHelper.isRootAvailable) {
            arrayOf("su", "-c", binaryPath, "--dump-json", "--no-download", url)
        } else {
            arrayOf(binaryPath, "--dump-json", "--no-download", url)
        }

        val process = ProcessBuilder(*cmd)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0 || output.isBlank()) {
            emptyList()
        } else {
            parseFormats(output)
        }
    }

    suspend fun download(
        url: String,
        outputPath: String,
        formatId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val args = mutableListOf<String>()

        if (RootHelper.isRootAvailable) {
            args.add("su")
            args.add("-c")
        }

        args.add(binaryPath)
        args.add("-f")
        args.add(formatId ?: "bestvideo+bestaudio/best")
        args.add("-o")
        args.add(outputPath)
        args.add("--no-playlist")
        args.add("--no-part")
        args.add("--no-mtime")
        args.add(url)

        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()

        process.inputStream.bufferedReader().use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
            }
        }

        process.waitFor() == 0
    }

    private fun parseFormats(jsonOutput: String): List<FormatInfo> {
        try {
            val json = org.json.JSONObject(jsonOutput)
            val formats = json.optJSONArray("formats") ?: return emptyList()
            val result = mutableListOf<FormatInfo>()
            for (i in 0 until formats.length()) {
                val fmt = formats.getJSONObject(i)
                result.add(
                    FormatInfo(
                        formatId = fmt.optString("format_id", ""),
                        extension = fmt.optString("ext", ""),
                        resolution = fmt.optString("resolution", null),
                        filesize = if (fmt.has("filesize")) fmt.optLong("filesize") else null,
                        note = fmt.optString("format_note", null)
                    )
                )
            }
            return result
        } catch (e: Exception) {
            return emptyList()
        }
    }
}
