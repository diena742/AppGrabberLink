package com.appgrabberlink.common.extensions

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Environment
import java.io.File

fun Context.getInstalledApps(): List<AppInfo> {
    val pm = packageManager
    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
    }
    val activities = pm.queryIntentActivities(intent, 0)
    return activities.map { resolveInfo ->
        val packageName = resolveInfo.activityInfo.packageName
        val appName = resolveInfo.loadLabel(pm).toString()
        val icon = resolveInfo.loadIcon(pm)
        AppInfo(packageName = packageName, appName = appName, icon = icon)
    }.sortedBy { it.appName }
}

fun Context.getAppIcon(packageName: String): Drawable? {
    return try {
        packageManager.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}

fun Context.getDownloadPath(): String {
    val dir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "AppGrabberLink"
    )
    dir.mkdirs()
    return dir.absolutePath
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)
