package com.relationshipradar.app.ui.screens

import android.Manifest
import android.content.Intent
import android.provider.Settings as SysSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relationshipradar.app.connectors.RadarNotificationListener
import com.relationshipradar.app.data.db.ConnectorCursor
import com.relationshipradar.app.ui.Format
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.SectionHeader
import com.relationshipradar.app.ui.liquidGlass
import com.relationshipradar.app.ui.theme.StatusColors

/**
 * Sources Screen: Real-time sensor and interaction connector management.
 * Designed with Apple Liquid Glass aesthetic, ambient status pills, and zero harsh boundaries.
 */
@Composable
fun ConnectorsScreen(vm: RadarViewModel, onOpenCallInsights: () -> Unit = {}) {
    val ctx = LocalContext.current
    val cursors by vm.cursors.collectAsStateWithLifecycle()
    var callsGranted by remember { mutableStateOf(vm.hasPermission(Manifest.permission.READ_CALL_LOG)) }
    var smsGranted by remember { mutableStateOf(vm.hasPermission(Manifest.permission.READ_SMS)) }
    var calGranted by remember { mutableStateOf(vm.hasPermission(Manifest.permission.READ_CALENDAR)) }
    var listenerOn by remember { mutableStateOf(RadarNotificationListener.isEnabled(ctx)) }
    var msg by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }

    val callsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        callsGranted = ok
        if (ok) vm.scanNow { msg = it }
    }
    val calLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        calGranted = ok
        if (ok) vm.scanNow { msg = it }
    }
    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        smsGranted = ok
        if (ok) vm.scanNow { msg = it }
    }

    fun cursor(id: String) = cursors.firstOrNull { it.connectorId == id }
    fun enabled(id: String) = cursor(id)?.enabled ?: true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // ---- Top Header ---------------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sources",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-1).sp,
                )
                Text(
                    text = "Ambient signal detection & device sync",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 180.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ---- Privacy Guarantee Pill -----------------------------------------------------
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(18.dp),
                            elevation = 3.dp,
                            surfaceAlphaTop = 0.85f,
                            surfaceAlphaBottom = 0.55f,
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StatusColors.Emerald.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Security,
                                contentDescription = null,
                                tint = StatusColors.Emerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Zero message content is ever read or stored. Only interaction timestamps and contact handles remain on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ---- Phone Calls ----------------------------------------------------------------
            item {
                SectionHeader("Device Telephony")
                LuxurySourceCard(
                    icon = Icons.Rounded.Call,
                    title = "Phone Calls",
                    description = "Reads call logs: duration, timestamp, and direction. Conversation audio is never recorded or accessed.",
                    granted = callsGranted,
                    enabled = enabled("calls"),
                    last = cursor("calls"),
                    onToggle = { vm.setConnectorEnabled("calls", it) },
                    onGrant = { callsLauncher.launch(Manifest.permission.READ_CALL_LOG) },
                )
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(22.dp), elevation = 6.dp)
                        .clickable(onClick = onOpenCallInsights).padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(StatusColors.Cobalt.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Call, contentDescription = null, tint = StatusColors.Cobalt)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Call Recorder insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text("Transcribe a recording privately and review what’s worth remembering.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                        Text("Open", color = StatusColors.Cobalt, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ---- SMS Messages ---------------------------------------------------------------
            item {
                SectionHeader("Messaging")
                LuxurySourceCard(
                    icon = Icons.Rounded.Message,
                    title = "Text Messages (SMS)",
                    description = "Analyzes SMS contact exchanges and timestamps. Text body and attachments are strictly ignored.",
                    granted = smsGranted,
                    enabled = enabled("sms"),
                    last = cursor("sms"),
                    onToggle = { vm.setConnectorEnabled("sms", it) },
                    onGrant = { smsLauncher.launch(Manifest.permission.READ_SMS) },
                )
            }

            // ---- Calendar -------------------------------------------------------------------
            item {
                SectionHeader("Schedule")
                LuxurySourceCard(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "Calendar Events",
                    description = "Counts past shared events with known contacts as seeing them. Notes, titles, and details are never read.",
                    granted = calGranted,
                    enabled = enabled("calendar"),
                    last = cursor("calendar"),
                    onToggle = { vm.setConnectorEnabled("calendar", it) },
                    onGrant = { calLauncher.launch(Manifest.permission.READ_CALENDAR) },
                )
            }

            // ---- Chat Apps ------------------------------------------------------------------
            item {
                SectionHeader("Chat Apps (WhatsApp, Telegram, Signal, Slack)")
                val chatShape = RoundedCornerShape(22.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = chatShape,
                            elevation = 6.dp,
                            surfaceAlphaTop = 0.85f,
                            surfaceAlphaBottom = 0.55f,
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(StatusColors.Cobalt.copy(alpha = 0.15f), StatusColors.Cobalt.copy(alpha = 0.05f))
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Forum,
                                    contentDescription = null,
                                    tint = StatusColors.Cobalt,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Chat App Listener",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A),
                                )
                                Text(
                                    text = if (listenerOn) "Active and watching replies" else "Permission required",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (listenerOn) StatusColors.Emerald else StatusColors.Amber,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (listenerOn) {
                                Switch(
                                    checked = enabled(RadarNotificationListener.CONNECTOR_ID),
                                    onCheckedChange = { vm.setConnectorEnabled(RadarNotificationListener.CONNECTOR_ID, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = StatusColors.Cobalt,
                                    )
                                )
                            }
                        }

                        Text(
                            text = "Monitors incoming notification headers for messages and direct replies from WhatsApp, Messenger, Instagram, Telegram, Signal, Discord, Slack, and Teams.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp,
                        )

                        if (!listenerOn) {
                            Button(
                                onClick = { ctx.startActivity(Intent(SysSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusColors.Cobalt),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Notification Access", fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = { listenerOn = RadarNotificationListener.isEnabled(ctx) },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("I turned it on — re-check", color = StatusColors.Cobalt, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            cursor(RadarNotificationListener.CONNECTOR_ID)?.let {
                                Text(
                                    text = "Credited ${it.lastRunCount} messages so far",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            // ---- Background Pulse Engine ----------------------------------------------------
            item {
                SectionHeader("Radar Pulse Engine")
                val pulseShape = RoundedCornerShape(22.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = pulseShape,
                            elevation = 6.dp,
                            surfaceAlphaTop = 0.85f,
                            surfaceAlphaBottom = 0.55f,
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(StatusColors.Emerald.copy(alpha = 0.15f), StatusColors.Emerald.copy(alpha = 0.05f))
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Sync,
                                    contentDescription = null,
                                    tint = StatusColors.Emerald,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Background Scan",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A),
                                )
                                Text(
                                    text = "Runs every 6 hours automatically",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        Text(
                            text = "Calls, texts, and calendar are evaluated every 6 hours in the background to calculate connection fade. First run looks back one year.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp,
                        )

                        Button(
                            onClick = {
                                isScanning = true
                                vm.scanNow {
                                    msg = it
                                    isScanning = false
                                }
                            },
                            enabled = !isScanning,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusColors.Cobalt),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isScanning) "Scanning device…" else "Scan now", fontWeight = FontWeight.Bold)
                        }

                        msg?.let {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LuxurySourceCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    enabled: Boolean,
    last: ConnectorCursor?,
    onToggle: (Boolean) -> Unit,
    onGrant: () -> Unit,
) {
    val cardShape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = cardShape,
                elevation = 6.dp,
                surfaceAlphaTop = 0.85f,
                surfaceAlphaBottom = 0.55f,
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (granted) StatusColors.Cobalt.copy(alpha = 0.15f) else Color(0x150F172A),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (granted) StatusColors.Cobalt else Color(0xFF64748B),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                    )
                    Text(
                        text = if (!granted) "Access required" else if (enabled) "Syncing" else "Paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!granted) StatusColors.Amber else if (enabled) StatusColors.Emerald else Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (granted) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StatusColors.Cobalt,
                        )
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569),
                lineHeight = 18.sp,
            )

            if (!granted) {
                Button(
                    onClick = onGrant,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusColors.Cobalt),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Allow access", fontWeight = FontWeight.Bold)
                }
            } else {
                val statsText = last?.takeIf { it.lastRunAt > 0 }?.let {
                    "Last scan ${Format.dateTime(it.lastRunAt)} · ${it.lastRunCount} new"
                } ?: "Ready to scan"

                Text(
                    text = statsText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
