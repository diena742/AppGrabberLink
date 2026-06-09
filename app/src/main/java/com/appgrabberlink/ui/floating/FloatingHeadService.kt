package com.appgrabberlink.ui.floating

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.appgrabberlink.App
import com.appgrabberlink.capture.PcapService
import kotlin.math.abs

class FloatingHeadService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var viewModel: FloatingViewModel

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isExpanded by mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        viewModel = FloatingViewModel.instance
        startForeground(NOTIFICATION_ID, createNotification())
        setupFloatingView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (::composeView.isInitialized) {
            windowManager.removeView(composeView)
        }
        stopCapture()
    }

    private fun createNotification(): Notification {
        val channelId = App.CHANNEL_FLOATING
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("AppGrabberLink")
            .setContentText("Floating panel active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    private fun setupFloatingView() {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            flags,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        composeView = ComposeView(this).apply {
            setContent {
                com.appgrabberlink.ui.theme.AppGrabberTheme {
                    FloatingPanelContent(
                        viewModel = viewModel,
                        isExpanded = isExpanded,
                        onToggleExpand = { toggleExpand() },
                        onToggleCapture = { toggleCapture() },
                        onDownload = { link ->
                            com.appgrabberlink.download.DownloadManager.enqueue(this@FloatingHeadService, link)
                        }
                    )
                }
            }

            setOnTouchListener { _, event ->
                onTouchEvent(event)
            }
        }

        windowManager.addView(composeView, params)
    }

    private fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                params.x = initialX + (event.rawX - initialTouchX).toInt()
                params.y = initialY + (event.rawY - initialTouchY).toInt()
                windowManager.updateViewLayout(composeView, params)
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                if (abs(dx) < 10 && abs(dy) < 10) {
                    toggleExpand()
                }
                return true
            }
        }
        return false
    }

    private fun toggleExpand() {
        isExpanded = !isExpanded
        if (isExpanded) {
            params.width = 500
            params.height = 600
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        windowManager.updateViewLayout(composeView, params)
    }

    private fun toggleCapture() {
        if (viewModel.isCapturing.value == true) {
            stopCapture()
        } else {
            startCapture()
        }
        viewModel.toggleCapture()
    }

    private fun startCapture() {
        val mode = viewModel.captureMode.value?.name ?: "VPN_NON_ROOT"
        val packages = viewModel.targetApps.value
            ?.filter { it.enabled }
            ?.map { it.packageName }
            ?.toTypedArray() ?: emptyArray()

        val intent = Intent(this, PcapService::class.java).apply {
            putExtra(PcapService.EXTRA_MODE, mode)
            putExtra(PcapService.EXTRA_PACKAGES, packages)
        }
        startForegroundServiceCompat(intent)
    }

    private fun stopCapture() {
        stopService(Intent(this, PcapService::class.java))
    }

    private fun startForegroundServiceCompat(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1002
    }
}
