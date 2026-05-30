package com.appgrabberlink.capture

import android.content.Context
import android.content.pm.PackageManager
import com.appgrabberlink.common.model.VideoLink
import com.appgrabberlink.ui.floating.FloatingViewModel

class VideoLinkDetector(private val context: Context) {

    fun onPacketCaptured(
        payload: ByteArray,
        sourceIp: String,
        sourcePort: Int,
        destinationIp: String,
        destinationPort: Int,
        requestPath: String,
        appPackage: String
    ) {
        val links = TrafficParser.parseHttpResponse(
            payload = payload,
            sourceIp = sourceIp,
            sourcePort = sourcePort,
            destinationIp = destinationIp,
            destinationPort = destinationPort,
            requestPath = requestPath,
            appPackage = appPackage
        )

        links.forEach { link ->
            val enriched = link.copy(
                sourceApp = resolveAppName(appPackage)
            )
            FloatingViewModel.instance.addDetectedLink(enriched)
        }
    }

    private fun resolveAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
