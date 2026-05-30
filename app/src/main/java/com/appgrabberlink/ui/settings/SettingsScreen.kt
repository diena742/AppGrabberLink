package com.appgrabberlink.ui.settings

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.appgrabberlink.common.model.AppTarget
import com.appgrabberlink.common.model.CaptureMode
import com.appgrabberlink.ui.floating.FloatingViewModel
import com.appgrabberlink.ui.theme.AppGrabberTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val viewModel = FloatingViewModel.instance

    var selectedMode by remember { mutableStateOf(viewModel.captureMode.value ?: CaptureMode.VPN_NON_ROOT) }
    var apps by remember { mutableStateOf(loadInstalledApps(context)) }

    AppGrabberTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
            ) {
                item {
                    Text(
                        text = "Capture Mode",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedMode == CaptureMode.VPN_NON_ROOT,
                            onClick = {
                                selectedMode = CaptureMode.VPN_NON_ROOT
                                viewModel.setCaptureMode(CaptureMode.VPN_NON_ROOT)
                            }
                        )
                        Text("Non-Root (VPN)", modifier = Modifier.padding(start = 4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedMode == CaptureMode.VPN_ROOT,
                            onClick = {
                                selectedMode = CaptureMode.VPN_ROOT
                                viewModel.setCaptureMode(CaptureMode.VPN_ROOT)
                            }
                        )
                        Text("Root (iptables)", modifier = Modifier.padding(start = 4.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                }

                item {
                    Text(
                        text = "Target Applications",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Select which apps to monitor for video links",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(apps) { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = app.enabled,
                            onCheckedChange = {
                                apps = apps.map {
                                    if (it.packageName == app.packageName)
                                        it.copy(enabled = !it.enabled)
                                    else it
                                }
                                viewModel.toggleApp(app)
                            }
                        )
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Download Location",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Default: Downloads/AppGrabberLink/",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun loadInstalledApps(context: android.content.Context): List<AppTarget> {
    val pm = context.packageManager
    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
    }
    val activities = pm.queryIntentActivities(intent, 0)
    return activities.map { resolveInfo ->
        val packageName = resolveInfo.activityInfo.packageName
        val appName = resolveInfo.loadLabel(pm).toString()
        AppTarget(packageName = packageName, appName = appName, enabled = false)
    }.sortedBy { it.appName }
}
