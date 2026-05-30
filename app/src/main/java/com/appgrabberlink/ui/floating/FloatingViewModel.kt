package com.appgrabberlink.ui.floating

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.appgrabberlink.common.model.AppTarget
import com.appgrabberlink.common.model.CaptureMode
import com.appgrabberlink.common.model.VideoLink

class FloatingViewModel : ViewModel() {

    companion object {
        lateinit var instance: FloatingViewModel
    }

    private val _detectedLinks = MutableLiveData<List<VideoLink>>(emptyList())
    val detectedLinks: LiveData<List<VideoLink>> = _detectedLinks

    private val _isCapturing = MutableLiveData(false)
    val isCapturing: LiveData<Boolean> = _isCapturing

    private val _captureMode = MutableLiveData(CaptureMode.VPN_NON_ROOT)
    val captureMode: LiveData<CaptureMode> = _captureMode

    private val _targetApps = MutableLiveData<List<AppTarget>>(emptyList())
    val targetApps: LiveData<List<AppTarget>> = _targetApps

    private val _queueSize = MutableLiveData(0)
    val queueSize: LiveData<Int> = _queueSize

    init {
        instance = this
    }

    fun toggleCapture() {
        _isCapturing.value = !(_isCapturing.value ?: false)
    }

    fun setCaptureMode(mode: CaptureMode) {
        _captureMode.value = mode
    }

    fun addDetectedLink(link: VideoLink) {
        val current = _detectedLinks.value.orEmpty().toMutableList()
        current.add(0, link)
        _detectedLinks.value = current
    }

    fun setTargetApps(apps: List<AppTarget>) {
        _targetApps.value = apps
    }

    fun toggleApp(app: AppTarget) {
        val current = _targetApps.value.orEmpty().toMutableList()
        val index = current.indexOfFirst { it.packageName == app.packageName }
        if (index >= 0) {
            current[index] = current[index].copy(enabled = !current[index].enabled)
            _targetApps.value = current
        }
    }

    fun incrementQueue() {
        _queueSize.value = (_queueSize.value ?: 0) + 1
    }

    fun decrementQueue() {
        val current = _queueSize.value ?: 0
        _queueSize.value = if (current > 0) current - 1 else 0
    }

    fun clearDetectedLinks() {
        _detectedLinks.value = emptyList()
    }
}
