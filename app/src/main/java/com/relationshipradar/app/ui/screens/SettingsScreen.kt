package com.relationshipradar.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings as SysSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import com.relationshipradar.app.RadarApp
import com.relationshipradar.app.data.BackupManager
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.border
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relationshipradar.app.ui.AmbientGlassCanvas
import com.relationshipradar.app.ui.Format
import com.relationshipradar.app.ui.Hairline
import com.relationshipradar.app.ui.Hint
import com.relationshipradar.app.ui.LinkRow
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.SectionHeader
import com.relationshipradar.app.ui.ToggleRow
import com.relationshipradar.app.ui.liquidGlass
import com.relationshipradar.app.ui.theme.StatusColors
import com.relationshipradar.app.work.Notifications

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: RadarViewModel,
    onBack: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenConnectors: () -> Unit,
    onOpenWho: () -> Unit,
    onOpenHealth: () -> Unit
) {
    val pendingCount by vm.pendingIdentities.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val s by vm.appSettings.collectAsStateWithLifecycle()
    val archived by vm.archived.collectAsStateWithLifecycle()
    var syncMsg by remember { mutableStateOf<String?>(null) }
    var contactsGranted by remember { mutableStateOf(vm.contacts.hasPermission()) }
    var notifGranted by remember { mutableStateOf(Notifications.canPost(ctx)) }
    var overlayGranted by remember { mutableStateOf(SysSettings.canDrawOverlays(ctx)) }
    val scope = rememberCoroutineScope()

    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        contactsGranted = ok
        if (ok) vm.syncContacts { syncMsg = it }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifGranted = it }

    fun openAppSettings() = ctx.startActivity(Intent(SysSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // ---- Contacts Section -------------------------------------------------------
        item {
            SectionHeader("Contacts")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(20.dp),
                        elevation = 6.dp,
                        surfaceAlphaTop = 0.85f,
                        surfaceAlphaBottom = 0.55f,
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Hint("Every saved contact joins the circle. Reminders stay off until you turn them on.")
                    if (contactsGranted) {
                        Box(
                            modifier = Modifier
                                .liquidGlass(
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = 3.dp,
                                    surfaceAlphaTop = 0.90f,
                                    surfaceAlphaBottom = 0.70f,
                                )
                                .background(StatusColors.Cobalt, RoundedCornerShape(12.dp))
                                .clickable { vm.syncContacts { syncMsg = it } }
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Import / refresh contacts", fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                        if (s.lastContactsSyncAt > 0) {
                            Text(
                                "Last import: " + Format.dateTime(s.lastContactsSyncAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .liquidGlass(
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = 3.dp,
                                    surfaceAlphaTop = 0.90f,
                                    surfaceAlphaBottom = 0.70f,
                                )
                                .background(StatusColors.Cobalt, RoundedCornerShape(12.dp))
                                .clickable { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) }
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Contacts, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Allow contacts access", fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                        TextButton(onClick = ::openAppSettings) {
                            Text("Denied before? Open app settings", color = StatusColors.Cobalt, fontWeight = FontWeight.Bold)
                        }
                    }
                    syncMsg?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = StatusColors.Cobalt)
                    }
                }
            }
        }

                // ---- Sources Section --------------------------------------------------------
                item {
                    SectionHeader("Sources")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                elevation = 6.dp,
                                surfaceAlphaTop = 0.85f,
                                surfaceAlphaBottom = 0.55f,
                            )
                    ) {
                        Column {
                            LinkRow("Calls, texts, calendar, chat apps", "What the radar watches and why", onOpenConnectors)
                            Hairline(Modifier.padding(horizontal = 16.dp))
                            LinkRow(
                                "Who is this?",
                                if (pendingCount.isEmpty()) "Nothing to sort out" else "${pendingCount.size} unmatched numbers or chats",
                                onOpenWho
                            )
                        }
                    }
                }

                // ---- Notifications Section --------------------------------------------------
                item {
                    SectionHeader("Notifications")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                elevation = 6.dp,
                                surfaceAlphaTop = 0.85f,
                                surfaceAlphaBottom = 0.55f,
                            )
                    ) {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            if (!notifGranted) {
                                Column(Modifier.padding(16.dp)) {
                                    Hint("Reminders can't reach you until notifications are allowed.")
                                    Spacer(Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .liquidGlass(
                                                shape = RoundedCornerShape(12.dp),
                                                elevation = 3.dp,
                                                surfaceAlphaTop = 0.90f,
                                                surfaceAlphaBottom = 0.70f,
                                            )
                                            .background(StatusColors.Cobalt, RoundedCornerShape(12.dp))
                                            .clickable { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                                            .padding(horizontal = 18.dp, vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Text("Allow notifications", fontWeight = FontWeight.Black, color = Color.White)
                                        }
                                    }
                                }
                                Hairline(Modifier.padding(horizontal = 16.dp))
                            }
                            ToggleRow("Daily roundup", "One quiet summary of everyone who is due", s.roundupEnabled, vm::setRoundupEnabled)
                            Hairline(Modifier.padding(horizontal = 16.dp))
                            ToggleRow("Individual alerts", "Separate alert for high-priority people", s.individualAlertsEnabled, vm::setIndividualAlerts)
                            Hairline(Modifier.padding(horizontal = 16.dp))
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Roundup time: ${s.roundupHour}:00",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(Modifier.height(4.dp))
                                Slider(
                                    value = s.roundupHour.toFloat(),
                                    onValueChange = { vm.setRoundupHour(it.toInt()) },
                                    valueRange = 6f..22f,
                                    steps = 15
                                )
                                Spacer(Modifier.height(4.dp))
                                Hint("Backup nudges: day 0, 2, 4, 6 after due — then it goes quiet and just shows the colour.")
                            }
                        }
                    }
                }

                // ---- Categories Section -----------------------------------------------------
                item {
                    SectionHeader("Categories")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                elevation = 6.dp,
                                surfaceAlphaTop = 0.85f,
                                surfaceAlphaBottom = 0.55f,
                            )
                    ) {
                        LinkRow("Categories and default timers", "Best friend every 7 days, friend every 30…", onOpenCategories)
                    }
                }

                // ---- Background Health Section ----------------------------------------------
                item {
                    SectionHeader("Background health & Shizuku")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                elevation = 6.dp,
                                surfaceAlphaTop = 0.85f,
                                surfaceAlphaBottom = 0.55f,
                            )
                    ) {
                        LinkRow("Is the radar actually running?", "Battery, standby, permissions, Shizuku one-tap hardening", onOpenHealth)
                    }
                }

                // ---- AI Social Wingman (Conversational Spark for Introverts) -----------------
                item {
                    SectionHeader("AI Social Wingman")
                    var selectedProvider by remember(s.aiProvider) { mutableStateOf(s.aiProvider) }
                    var geminiKeyInput by remember(s.geminiApiKey) { mutableStateOf(s.geminiApiKey) }
                    var groqKeyInput by remember(s.groqApiKey) { mutableStateOf(s.groqApiKey) }
                    var openRouterKeyInput by remember(s.openRouterApiKey) { mutableStateOf(s.openRouterApiKey) }
                    var grokKeyInput by remember(s.grokApiKey) { mutableStateOf(s.grokApiKey) }
                    var openAiKeyInput by remember(s.openAiApiKey) { mutableStateOf(s.openAiApiKey) }
                    var claudeKeyInput by remember(s.anthropicApiKey) { mutableStateOf(s.anthropicApiKey) }
                    var customBaseUrlInput by remember(s.customApiBaseUrl) { mutableStateOf(s.customApiBaseUrl) }
                    var customKeyInput by remember(s.customApiKey) { mutableStateOf(s.customApiKey) }
                    var customModelInput by remember(s.customModelName) { mutableStateOf(s.customModelName) }
                    var keyVisible by remember { mutableStateOf(false) }
                    var testStatus by remember { mutableStateOf<String?>(null) }

                    val activeKey = when (selectedProvider) {
                        "groq" -> groqKeyInput
                        "openrouter" -> openRouterKeyInput
                        "grok" -> grokKeyInput
                        "openai" -> openAiKeyInput
                        "claude" -> claudeKeyInput
                        "custom" -> customKeyInput
                        else -> geminiKeyInput
                    }
                    val currentMeta = com.relationshipradar.app.ai.AiProviders.find(selectedProvider)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(22.dp),
                                elevation = 8.dp,
                                surfaceAlphaTop = 0.88f,
                                surfaceAlphaBottom = 0.58f,
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Section Header with Dimensional Icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = Color(0x306366F1))
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            androidx.compose.ui.graphics.Brush.linearGradient(
                                                listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        "Conversational Spark",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        "AI Wingman for introverts & hesitation",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Text(
                                "Never stare at a blank text wondering what to say. The AI Wingman drafts 3 low-pressure messages tailored to your notes and talking points.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569),
                                lineHeight = 18.sp
                            )

                            // Provider Selector (Scrollable Tactile 3D Glass Pills)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Choose AI Provider (Free & Paid Supported)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )

                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(com.relationshipradar.app.ai.AiProviders.ALL) { meta ->
                                        val isSelected = selectedProvider == meta.id
                                        val pillShape = RoundedCornerShape(10.dp)

                                        Box(
                                            modifier = Modifier
                                                .shadow(if (isSelected) 3.dp else 0.dp, pillShape, spotColor = Color(0x200F172A))
                                                .clip(pillShape)
                                                .background(if (isSelected) Color.White else Color(0xFFCBD5E1).copy(alpha = 0.35f))
                                                .border(
                                                    0.8.dp,
                                                    if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                                    pillShape
                                                )
                                                .clickable {
                                                    selectedProvider = meta.id
                                                    vm.setAiProvider(meta.id)
                                                    testStatus = null
                                                }
                                                .padding(horizontal = 12.dp, vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                if (meta.isFreeTierAvailable) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(StatusColors.Emerald)
                                                    )
                                                }
                                                Text(
                                                    text = meta.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFF475569),
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 1-Tap Direct Link to Get API Key from the Selected Provider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF6366F1).copy(alpha = 0.08f))
                                    .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentMeta.keyUrl))
                                            ctx.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(ctx, "Could not open browser", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Key,
                                            contentDescription = null,
                                            tint = Color(0xFF6366F1),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Get ${currentMeta.name} Key ↗",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6366F1)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (currentMeta.isFreeTierAvailable) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    ) {
                                        Text(
                                            currentMeta.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (currentMeta.isFreeTierAvailable) Color(0xFF15803D) else Color(0xFF475569)
                                        )
                                    }
                                }
                            }

                            // Custom Endpoint Configuration Fields
                            if (selectedProvider == "custom") {
                                OutlinedTextField(
                                    value = customBaseUrlInput,
                                    onValueChange = { customBaseUrlInput = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("http://localhost:11434/v1", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall) },
                                    label = { Text("Base URL", style = MaterialTheme.typography.labelSmall) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = customModelInput,
                                    onValueChange = { customModelInput = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("llama3 or mistral", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall) },
                                    label = { Text("Model Name", style = MaterialTheme.typography.labelSmall) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // API Key Masked Input Field
                            OutlinedTextField(
                                value = when (selectedProvider) {
                                    "groq" -> groqKeyInput
                                    "openrouter" -> openRouterKeyInput
                                    "grok" -> grokKeyInput
                                    "openai" -> openAiKeyInput
                                    "claude" -> claudeKeyInput
                                    "custom" -> customKeyInput
                                    else -> geminiKeyInput
                                },
                                onValueChange = { newVal ->
                                    when (selectedProvider) {
                                        "groq" -> groqKeyInput = newVal
                                        "openrouter" -> openRouterKeyInput = newVal
                                        "grok" -> grokKeyInput = newVal
                                        "openai" -> openAiKeyInput = newVal
                                        "claude" -> claudeKeyInput = newVal
                                        "custom" -> customKeyInput = newVal
                                        else -> geminiKeyInput = newVal
                                    }
                                    testStatus = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        when (selectedProvider) {
                                            "groq" -> "gsk_..."
                                            "openrouter" -> "sk-or-..."
                                            "grok" -> "xai-..."
                                            "openai" -> "sk-..."
                                            "claude" -> "sk-ant-..."
                                            "custom" -> "API Key (optional for Ollama)"
                                            else -> "AIzaSy... (Gemini Free Key)"
                                        },
                                        color = Color(0xFF94A3B8),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                singleLine = true,
                                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Key,
                                        contentDescription = null,
                                        tint = StatusColors.Cobalt,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { keyVisible = !keyVisible }) {
                                        Icon(
                                            if (keyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = "Toggle Key Visibility",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Save & Test Action Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Status Indicator
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (activeKey.isNotBlank() || selectedProvider == "custom") StatusColors.Emerald else StatusColors.Amber)
                                    )
                                    Text(
                                        if (activeKey.isNotBlank()) "${currentMeta.name} Configured" else "Smart Offline Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeKey.isNotBlank()) StatusColors.Emerald else StatusColors.Amber
                                    )
                                }

                                // Tactile 3D Save Button
                                Box(
                                    modifier = Modifier
                                        .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color(0x300F172A))
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            when (selectedProvider) {
                                                "groq" -> vm.setGroqApiKey(groqKeyInput)
                                                "openrouter" -> vm.setOpenRouterApiKey(openRouterKeyInput)
                                                "grok" -> vm.setGrokApiKey(grokKeyInput)
                                                "openai" -> vm.setOpenAiApiKey(openAiKeyInput)
                                                "claude" -> vm.setAnthropicApiKey(claudeKeyInput)
                                                "custom" -> vm.setCustomApi(customBaseUrlInput, customKeyInput, customModelInput)
                                                else -> vm.setGeminiApiKey(geminiKeyInput)
                                            }
                                            testStatus = "Saved! ${currentMeta.name} is ready."
                                            Toast.makeText(ctx, "AI Wingman key saved", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            "Save Key",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            testStatus?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusColors.Emerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Privacy Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x080F172A), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "🔒 Privacy First: Keys are stored encrypted strictly on your device. Requests go directly to the provider endpoint with zero middleman servers.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // ---- In-Call Floating HUD Overlay -------------------------------------------
                item {
                    SectionHeader("In-Call Floating Cheat Sheet")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                elevation = 6.dp,
                                surfaceAlphaTop = 0.85f,
                                surfaceAlphaBottom = 0.55f,
                            )
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Phone Call HUD Overlay",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Pops a floating memory jogger with notes and talking points right over your phone screen when a tracked contact calls.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                            Spacer(Modifier.height(4.dp))
                            if (overlayGranted) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(StatusColors.Emerald, CircleShape)
                                    )
                                    Text(
                                        "Active · Display over other apps granted",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusColors.Emerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val intent = Intent(
                                            SysSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${ctx.packageName}")
                                        )
                                        ctx.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusColors.Cobalt)
                                ) {
                                    Text("Grant Overlay Permission", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ---- Data Portability & Local JSON Backup -----------------------------------
                item {
                    SectionHeader("Data Portability & Backup")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                elevation = 6.dp,
                                surfaceAlphaTop = 0.85f,
                                surfaceAlphaBottom = 0.55f,
                            )
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Local JSON Archive",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Export all connections, categories, interaction timelines, and talking points to an offline JSON backup file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val app = ctx.applicationContext as RadarApp
                                        val file = BackupManager.saveBackupToFile(ctx, app.db)
                                        Toast.makeText(ctx, "Saved to ${file.name}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                            ) {
                                Text("Export Tether Backup (JSON)", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ---- Archived Section -------------------------------------------------------
                item {
                    SectionHeader("Archived · ${archived.size}")
                    if (archived.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = 4.dp,
                                    surfaceAlphaTop = 0.75f,
                                    surfaceAlphaBottom = 0.45f,
                                )
                                .padding(16.dp)
                        ) {
                            Text("Nobody archived.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = 6.dp,
                                    surfaceAlphaTop = 0.85f,
                                    surfaceAlphaBottom = 0.55f,
                                )
                        ) {
                            Column {
                                archived.forEachIndexed { idx, pwc ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF1E293B)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val initials = pwc.person.displayName.split(' ', '-').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }
                                                Text(initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                            }
                                            Text(
                                                pwc.person.displayName,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(StatusColors.Cobalt.copy(alpha = 0.12f))
                                                .clickable { vm.restore(pwc.person.id) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Restore", color = StatusColors.Cobalt, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                        }
                                    }
                                    if (idx < archived.lastIndex) Hairline(Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
            }
}
