package com.appgrabberlink.capture

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.appgrabberlink.App
import com.appgrabberlink.engine.RootHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket

class PcapService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private lateinit var detector: VideoLinkDetector

    override fun onCreate() {
        super.onCreate()
        detector = VideoLinkDetector(this)
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_VPN_NON_ROOT
        @Suppress("UNCHECKED_CAST")
        val packages = intent?.getStringArrayListExtra(EXTRA_PACKAGES) as? List<String> ?: emptyList()
        startCapture(mode, packages)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
        scope.cancel()
    }

    private fun startCapture(mode: String, packages: List<String>) {
        if (isRunning) return
        isRunning = true

        when (mode) {
            MODE_VPN_ROOT -> {
                RootHelper.startIptablesCapture()
                startLocalProxy(8443)
            }
            MODE_VPN_NON_ROOT -> {
                startLocalProxy(8080)
            }
        }

        updateNotification("Monitoring traffic...")
    }

    private fun stopCapture() {
        if (!isRunning) return
        isRunning = false

        RootHelper.stopIptablesCapture()

        try {
            serverSocket?.close()
        } catch (_: Exception) {}

        updateNotification("Capture stopped")
    }

    private fun startLocalProxy(port: Int) {
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    launch {
                        try {
                            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                            val requestBuilder = StringBuilder()
                            var line: String?
                            var contentLength = 0

                            while (reader.readLine().also { line = it } != null) {
                                if (line.isNullOrBlank()) break
                                requestBuilder.append(line).append("\r\n")
                                if (line.startsWith("Content-Length:", ignoreCase = true)) {
                                    contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                                }
                            }

                            if (contentLength > 0) {
                                val body = CharArray(contentLength)
                                reader.read(body)
                                requestBuilder.append(body)
                            }

                            val rawRequest = requestBuilder.toString()
                            val payload = rawRequest.toByteArray()

                            if (payload.isNotEmpty()) {
                                detector.onPacketCaptured(
                                    payload = payload,
                                    sourceIp = client.inetAddress.hostAddress ?: "",
                                    sourcePort = client.port,
                                    destinationIp = "localhost",
                                    destinationPort = port,
                                    requestPath = extractPath(rawRequest),
                                    appPackage = "com.android.chrome"
                                )
                            }

                            client.close()
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun extractPath(request: String): String {
        val firstLine = request.lines().firstOrNull() ?: ""
        val parts = firstLine.split(" ")
        return if (parts.size >= 2) parts[1] else "/"
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, App.CHANNEL_CAPTURE)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Capture Active")
            .setContentText("Monitoring traffic...")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, App.CHANNEL_CAPTURE)
        } else {
            Notification.Builder(this)
        }
        manager.notify(
            NOTIFICATION_ID,
            builder
                .setContentTitle("Capture")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_search)
                .setOngoing(isRunning)
                .build()
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 1003
        const val EXTRA_MODE = "capture_mode"
        const val EXTRA_PACKAGES = "target_packages"
        const val MODE_VPN_NON_ROOT = "vpn_non_root"
        const val MODE_VPN_ROOT = "vpn_root"
    }
}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_VPN_NON_ROOT
        val packages = intent?.getStringArrayListExtra(EXTRA_PACKAGES) ?: arrayListOf()
        startCapture(mode, packages)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    private fun startCapture(mode: String, packages: List<String>) {
        // TODO: Integrate PCAPdroid library
    }

    private fun stopCapture() {
        // TODO: Cleanup
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, App.CHANNEL_CAPTURE)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle(\"Capture Active\")
            .setContentText(\"Monitoring traffic...\")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1003
        const val EXTRA_MODE = \"capture_mode\"
        const val EXTRA_PACKAGES = \"target_packages\"
        const val MODE_VPN_NON_ROOT = \"vpn_non_root\"
        const val MODE_VPN_ROOT = \"vpn_root\"
    }
}
