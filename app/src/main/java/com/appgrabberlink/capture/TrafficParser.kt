package com.appgrabberlink.capture

import com.appgrabberlink.common.model.VideoFormat
import com.appgrabberlink.common.model.VideoLink
import java.io.ByteArrayInputStream
import java.util.UUID

object TrafficParser {

    fun parseHttpResponse(
        payload: ByteArray,
        sourceIp: String,
        sourcePort: Int,
        destinationIp: String,
        destinationPort: Int,
        requestPath: String,
        appPackage: String
    ): List<VideoLink> {
        val headers = extractHeaders(payload)
        val contentType = headers["Content-Type"] ?: headers["content-type"] ?: ""
        val contentLength = headers["Content-Length"]?.toLongOrNull() ?: headers["content-length"]?.toLongOrNull() ?: 0L
        val contentDisposition = headers["Content-Disposition"] ?: headers["content-disposition"] ?: ""
        val url = "$destinationIp:$destinationPort$requestPath"

        val links = mutableListOf<VideoLink>()

        if (contentType.startsWith("video/")) {
            links.add(buildVideoLink(
                url = url,
                contentType = contentType,
                contentLength = contentLength,
                contentDisposition = contentDisposition,
                sourceApp = appPackage
            ))
        }

        val bodyStart = findBodyStart(payload)
        if (bodyStart >= 0 && bodyStart < payload.size) {
            val body = payload.copyOfRange(bodyStart, payload.size)
            val bodyText = try { body.decodeToString() } catch (e: Exception) { "" }

            if (bodyText.startsWith("#EXTM3U")) {
                links.addAll(parseHlsPlaylist(url, bodyText, appPackage))
            }

            if (bodyText.startsWith("<?xml") && contentType.contains("dash+xml")) {
                links.addAll(parseDashManifest(url, bodyText, appPackage))
            }

            if (body.size > 8) {
                val ftypMagic = byteArrayOf(0x66, 0x74, 0x79, 0x70) // "ftyp"
                if (body[4] == ftypMagic[0] && body[5] == ftypMagic[1] &&
                    body[6] == ftypMagic[2] && body[7] == ftypMagic[3]) {
                    links.add(buildVideoLink(
                        url = url,
                        contentType = "video/mp4",
                        contentLength = contentLength,
                        contentDisposition = contentDisposition,
                        sourceApp = appPackage
                    ))
                }
            }
        }

        return links
    }

    private fun extractHeaders(payload: ByteArray): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val text = try { payload.decodeToString() } catch (e: Exception) { return headers }
        val lines = text.lines()
        for (line in lines) {
            if (line.isBlank()) break
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim()
                val value = line.substring(colonIndex + 1).trim()
                headers[key] = value
            }
        }
        return headers
    }

    private fun findBodyStart(payload: ByteArray): Int {
        val text = try { payload.decodeToString() } catch (e: Exception) { return -1 }
        val doubleCrlf = text.indexOf("\r\n\r\n")
        if (doubleCrlf >= 0) return doubleCrlf + 4
        val lf = text.indexOf("\n\n")
        if (lf >= 0) return lf + 2
        return -1
    }

    private fun parseHlsPlaylist(baseUrl: String, body: String, sourceApp: String): List<VideoLink> {
        val base = baseUrl.substringBeforeLast("/")
        val lines = body.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        return lines.mapIndexed { index, line ->
            val segmentUrl = if (line.startsWith("http")) line else "$base/$line"
            buildVideoLink(
                url = segmentUrl,
                contentType = "video/MP2T",
                contentLength = 0L,
                contentDisposition = "",
                sourceApp = sourceApp,
                format = VideoFormat.TS,
                title = "segment_${index + 1}.ts"
            )
        }
    }

    private fun parseDashManifest(baseUrl: String, body: String, sourceApp: String): List<VideoLink> {
        val base = baseUrl.substringBeforeLast("/")
        val urls = Regex("baseURL>(.*?)<")
            .findAll(body)
            .map { it.groupValues[1] }
            .toList()
        return urls.mapIndexed { index, relativeUrl ->
            val segmentUrl = if (relativeUrl.startsWith("http")) relativeUrl else "$base/$relativeUrl"
            buildVideoLink(
                url = segmentUrl,
                contentType = "video/mp4",
                contentLength = 0L,
                contentDisposition = "",
                sourceApp = sourceApp,
                format = VideoFormat.MP4,
                title = "dash_segment_${index + 1}.mp4"
            )
        }
    }

    private fun buildVideoLink(
        url: String,
        contentType: String,
        contentLength: Long,
        contentDisposition: String,
        sourceApp: String,
        format: VideoFormat = VideoFormat.UNKNOWN,
        title: String? = null
    ): VideoLink {
        val detectedFormat = if (format != VideoFormat.UNKNOWN) format else detectFormat(contentType, url)
        val detectedTitle = title
            ?: extractFilename(contentDisposition)
            ?: url.substringAfterLast("/").substringBefore("?").ifBlank { "video_${System.currentTimeMillis()}" }
        return VideoLink(
            id = UUID.randomUUID().toString(),
            url = sanitizeUrl(url),
            sourceApp = sourceApp,
            title = detectedTitle,
            fileSize = contentLength,
            mimeType = contentType,
            format = detectedFormat
        )
    }

    private fun detectFormat(contentType: String, url: String): VideoFormat {
        return when {
            contentType.contains("mp4") || url.contains(".mp4") -> VideoFormat.MP4
            contentType.contains("m3u8") || url.contains(".m3u8") -> VideoFormat.M3U8
            contentType.contains("mpd") || url.contains(".mpd") -> VideoFormat.MPD
            contentType.contains("mkv") || url.contains(".mkv") -> VideoFormat.MKV
            contentType.contains("webm") || url.contains(".webm") -> VideoFormat.WEBM
            contentType.contains("mp2t") || url.contains(".ts") -> VideoFormat.TS
            else -> VideoFormat.UNKNOWN
        }
    }

    private fun extractFilename(contentDisposition: String): String? {
        val filenameMatch = Regex("filename=\"?(.*?)\"?$").find(contentDisposition)
        return filenameMatch?.groupValues?.get(1)?.trim()
    }

    private fun sanitizeUrl(url: String): String {
        return url.replace(":443", "").replace(":80", "")
    }
}
