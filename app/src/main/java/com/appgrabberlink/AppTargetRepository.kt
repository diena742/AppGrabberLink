package com.appgrabberlink

import android.content.Context
import android.content.SharedPreferences
import com.appgrabberlink.common.model.AppTarget

class AppTargetRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_targets", Context.MODE_PRIVATE)

    fun saveTargets(targets: List<AppTarget>) {
        val enabled = targets.filter { it.enabled }.map { it.packageName }
        prefs.edit().putStringSet(KEY_ENABLED, enabled.toSet()).apply()
    }

    fun loadTargets(allApps: List<AppTarget>): List<AppTarget> {
        val enabledSet = prefs.getStringSet(KEY_ENABLED, emptySet()) ?: emptySet()
        return allApps.map { app ->
            app.copy(enabled = enabledSet.contains(app.packageName))
        }
    }

    fun isAppEnabled(packageName: String): Boolean {
        val enabledSet = prefs.getStringSet(KEY_ENABLED, emptySet()) ?: emptySet()
        return enabledSet.contains(packageName)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ENABLED = "enabled_packages"
    }
}
