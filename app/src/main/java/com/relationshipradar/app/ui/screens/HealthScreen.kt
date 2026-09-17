package com.relationshipradar.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings as SysSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.relationshipradar.app.shizuku.ShellCommands
import com.relationshipradar.app.shizuku.ShizukuBridge
import com.relationshipradar.app.ui.HealthDot
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.SectionHeader
import com.relationshipradar.app.ui.liquidGlass
import com.relationshipradar.app.ui.theme.StatusColors
import com.relationshipradar.app.work.Health

/**
 * Health Screen: Background service integrity and Shizuku diagnostics.
 * Designed with Apple Liquid Glass aesthetic and zero harsh boundaries.
 */
@Composable
fun HealthScreen(vm: RadarViewModel) {
    val ctx = LocalContext.current
    var report by remember { mutableStateOf(vm.health()) }
    var shizuku by remember { mutableStateOf(ShizukuBridge.status(ctx)) }
    var log by remember { mutableStateOf<List<String>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }

    // Re-measure whenever we come back from a Settings screen.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                report = vm.health()
                shizuku = ShizukuBridge.status(ctx)
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    fun refresh() {
        report = vm.health()
        shizuku = ShizukuBridge.status(ctx)
    }

    fun run(cmds: List<ShellCommands.Command>) {
        busy = true
        vm.runShizuku(cmds) {
            log = it
            busy = false
            refresh()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // ---- System Status Hero -------------------------------------------------------------
        item {
            val worst = report.worst
            val heroShape = RoundedCornerShape(22.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = heroShape,
                        elevation = 8.dp,
                        surfaceAlphaTop = 0.85f,
                        surfaceAlphaBottom = 0.55f,
                    )
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (worst) {
                                    Health.Level.OK -> StatusColors.Emerald.copy(alpha = 0.15f)
                                    Health.Level.ATTENTION -> StatusColors.Amber.copy(alpha = 0.15f)
                                    Health.Level.OFF -> StatusColors.Flame.copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        HealthDot(worst)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (worst) {
                                Health.Level.OK -> "System Operational"
                                Health.Level.ATTENTION -> "Attention Required"
                                Health.Level.OFF -> "Background Offline"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = when (worst) {
                                Health.Level.OK -> "All sensors, listeners, and cadence jobs are running normally."
                                Health.Level.ATTENTION -> "A few background permissions or battery optimizations need attention."
                                Health.Level.OFF -> "Critical sync or listener services are stopped by Android."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // ---- Shizuku Boost Section ----------------------------------------------------------
        item {
            SectionHeader("Shizuku System Hardening")
            val boostShape = RoundedCornerShape(22.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = boostShape,
                        elevation = 6.dp,
                        surfaceAlphaTop = 0.85f,
                        surfaceAlphaBottom = 0.55f,
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(StatusColors.Cobalt.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = StatusColors.Cobalt,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "One-Tap Android Bypass",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = when (shizuku) {
                                    ShizukuBridge.Status.READY -> "Connected & Ready"
                                    ShizukuBridge.Status.PERMISSION_NEEDED -> "Permission Needed"
                                    ShizukuBridge.Status.NOT_RUNNING -> "Service Not Started"
                                    ShizukuBridge.Status.NOT_INSTALLED -> "Not Installed"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when (shizuku) {
                                    ShizukuBridge.Status.READY -> StatusColors.Emerald
                                    ShizukuBridge.Status.PERMISSION_NEEDED -> StatusColors.Amber
                                    else -> Color(0xFF64748B)
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = when (shizuku) {
                            ShizukuBridge.Status.READY -> "Connected. One tap below instructs Android to stop throttling this app and disable battery restrictions."
                            ShizukuBridge.Status.PERMISSION_NEEDED -> "Shizuku is running on this device. Grant access to unlock one-tap system hardening."
                            ShizukuBridge.Status.NOT_RUNNING -> "Shizuku is installed but not active. Open Shizuku and start it via Wireless Debugging."
                            ShizukuBridge.Status.NOT_INSTALLED -> "Shizuku isn't installed. Everything works without it; the checks below can be resolved individually via Android Settings."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569),
                        lineHeight = 18.sp
                    )

                    when (shizuku) {
                        ShizukuBridge.Status.PERMISSION_NEEDED -> {
                            Button(
                                onClick = { ShizukuBridge.requestPermission() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusColors.Cobalt),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Shizuku Access", fontWeight = FontWeight.Bold)
                            }
                        }
                        ShizukuBridge.Status.READY -> {
                            Button(
                                onClick = { run(ShellCommands.hardening) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusColors.Cobalt)
                            ) {
                                Text(if (busy) "Working…" else "Harden Background (Battery, Standby)", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { run(listOf(ShellCommands.Command.ALLOW_NOTIFICATION_LISTENER)) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0F172A).copy(alpha = 0.08f),
                                    contentColor = Color(0xFF0F172A)
                                )
                            ) {
                                Text("Enable Chat-App Listener", fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            TextButton(
                                onClick = ::refresh,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Re-check Shizuku Status", color = StatusColors.Cobalt, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (log.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                log.forEach {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- Checks List --------------------------------------------------------------------
        item {
            SectionHeader("Diagnostics & Integrity")
        }

        items(report.checks) { c ->
            val checkShape = RoundedCornerShape(18.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = checkShape,
                        elevation = 4.dp,
                        surfaceAlphaTop = 0.85f,
                        surfaceAlphaBottom = 0.55f,
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HealthDot(c.level)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = c.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = c.detail + (c.fixHint?.takeIf { c.level != Health.Level.OK }?.let { "\n$it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            lineHeight = 16.sp
                        )
                    }

                    if (c.level != Health.Level.OK) {
                        when (c.title) {
                            "Battery optimisation" -> {
                                Button(
                                    onClick = {
                                        ctx.startActivity(Intent(SysSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusColors.Cobalt),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Fix", fontWeight = FontWeight.Bold)
                                }
                            }
                            "Chat app listener" -> {
                                Button(
                                    onClick = { ctx.startActivity(Intent(SysSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusColors.Cobalt),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Fix", fontWeight = FontWeight.Bold)
                                }
                            }
                            "Notifications", "Call log access", "SMS access" -> {
                                Button(
                                    onClick = {
                                        ctx.startActivity(Intent(SysSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusColors.Cobalt),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Fix", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = ::refresh) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = StatusColors.Cobalt, modifier = Modifier.size(16.dp))
                        Text("Re-measure All Diagnostics", color = StatusColors.Cobalt, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}


