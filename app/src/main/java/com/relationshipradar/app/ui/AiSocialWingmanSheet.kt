package com.relationshipradar.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.ui.text.style.TextOverflow
import com.relationshipradar.app.ui.theme.Haptics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.relationshipradar.app.ai.AiWingmanService
import com.relationshipradar.app.ai.WingmanVibe
import com.relationshipradar.app.data.db.ContactIdentifier
import com.relationshipradar.app.data.db.IdentifierType
import com.relationshipradar.app.data.repo.AppSettings
import com.relationshipradar.app.engine.PersonRadar
import com.relationshipradar.app.ui.theme.StatusColors
import kotlinx.coroutines.launch

/**
 * Reusable AI Social Wingman component.
 * Generates 3 tailored, low-pressure conversation starters for introverts.
 */
@Composable
fun AiSocialWingmanCard(
    name: String,
    category: String?,
    daysSinceContact: Int?,
    talkingPoints: String?,
    notes: String?,
    phone: String?,
    appSettings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedVibe by remember { mutableStateOf(WingmanVibe.LOW_PRESSURE) }
    var customThought by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var providerUsed by remember { mutableStateOf("") }
    var isAiPowered by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var copiedIndex by remember { mutableStateOf<Int?>(null) }

    fun loadStarters() {
        isLoading = true
        scope.launch {
            val key = when (appSettings.aiProvider.lowercase()) {
                "groq" -> appSettings.groqApiKey
                "openrouter" -> appSettings.openRouterApiKey
                "grok" -> appSettings.grokApiKey
                "openai" -> appSettings.openAiApiKey
                "claude" -> appSettings.anthropicApiKey
                "custom" -> appSettings.customApiKey
                else -> appSettings.geminiApiKey
            }
            val res = AiWingmanService.generateStarters(
                name = name,
                category = category,
                daysSinceContact = daysSinceContact,
                talkingPoints = talkingPoints,
                notes = notes,
                vibe = selectedVibe,
                provider = appSettings.aiProvider,
                apiKey = key,
                customBaseUrl = appSettings.customApiBaseUrl,
                customModel = appSettings.customModelName,
                customPrompt = customThought.ifBlank { null }
            )
            suggestions = res.suggestions
            providerUsed = res.providerUsed
            isAiPowered = res.isAiPowered
            isLoading = false
        }
    }

    // Auto-generate on first appearance or vibe switch
    LaunchedEffect(selectedVibe) {
        loadStarters()
    }

    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = cardShape,
                elevation = 8.dp,
                surfaceAlphaTop = 0.88f,
                surfaceAlphaBottom = 0.58f,
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row with Dimensional 3D Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Multi-layer Glowing Icon Tile
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(6.dp, RoundedCornerShape(11.dp), spotColor = Color(0x356366F1))
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            "AI Social Wingman",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            if (isAiPowered) "Powered by $providerUsed" else "Smart Offline Starters",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAiPowered) Color(0xFF6366F1) else Color(0xFF64748B)
                        )
                    }
                }

                // Refresh / Regenerate Tactile Pill
                Box(
                    modifier = Modifier
                        .shadow(2.dp, RoundedCornerShape(10.dp), spotColor = Color(0x180F172A))
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .clickable { loadStarters() }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Regenerate",
                            tint = StatusColors.Cobalt,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            "Shuffle",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusColors.Cobalt,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            // Custom Thought Input (Turn your rough thought into tailored messages)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(14.dp), spotColor = Color(0x100F172A))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.95f))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.EditNote,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = customThought,
                    onValueChange = { customThought = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF0F172A),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (customThought.isEmpty()) {
                            Text(
                                "Type a thought (e.g. coffee Saturday, sorry for lagging)...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                )
                if (customThought.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable {
                                customThought = ""
                                loadStarters()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(8.dp), spotColor = Color(0x256366F1))
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))))
                            .clickable { loadStarters() }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                "Spark",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }

            // Vibe Selector (7 Versatile Categories)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 1.dp)
            ) {
                items(WingmanVibe.entries) { vibe ->
                    val isSelected = selectedVibe == vibe
                    val pillShape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .clip(pillShape)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .shadow(2.dp, pillShape, spotColor = Color(0x256366F1))
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f), pillShape)
                                } else {
                                    Modifier
                                        .background(Color(0xFFE2E8F0).copy(alpha = 0.55f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.7f), pillShape)
                                }
                            )
                            .clickable { selectedVibe = vibe }
                            .padding(horizontal = 11.dp, vertical = 6.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(text = vibe.emoji, fontSize = 12.sp)
                            Text(
                                text = vibe.shortTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFF475569),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF6366F1),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Crafting low-pressure starters...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // List of 3 Generated Low-Pressure Drafts
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEachIndexed { index, draftText ->
                        val isCopied = copiedIndex == index

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = Color(0x100F172A))
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.90f))
                                .border(0.8.dp, Color.White, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = draftText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF1E293B),
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Normal
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1-Tap Copy Button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isCopied) StatusColors.Emerald.copy(alpha = 0.12f)
                                                else Color(0xFFF1F5F9)
                                            )
                                            .clickable {
                                                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Tether Icebreaker", draftText))
                                                copiedIndex = index
                                                Toast.makeText(ctx, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 9.dp, vertical = 5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                if (isCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = if (isCopied) StatusColors.Emerald else Color(0xFF475569),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                if (isCopied) "Copied" else "Copy",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCopied) StatusColors.Emerald else Color(0xFF475569),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // 1-Tap Send via SMS Button
                                    Box(
                                        modifier = Modifier
                                            .shadow(2.dp, RoundedCornerShape(8.dp), spotColor = Color(0x256366F1))
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                                                )
                                            )
                                            .clickable {
                                                val smsUri = if (!phone.isNullOrBlank()) Uri.parse("smsto:$phone") else Uri.parse("sms:")
                                                val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                                                    putExtra("sms_body", draftText)
                                                }
                                                try {
                                                    ctx.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(ctx, "No SMS app found", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Rounded.Send,
                                                contentDescription = "Send",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                "Send in SMS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Multi-Channel Reach-Out Glass Modal Sheet.
 * Ensures the user is NEVER forced onto a phone call and can pick their preferred communication mode.
 */
@Composable
fun QuickReachOutGlassSheet(
    radar: PersonRadar,
    categoryName: String?,
    identifiers: List<ContactIdentifier>,
    appSettings: AppSettings,
    onDismiss: () -> Unit,
    onOpenProfile: () -> Unit,
    onCaughtUpToday: () -> Unit = {},
) {
    val ctx = LocalContext.current
    var showAiWingman by remember { mutableStateOf(false) }

    val phone = identifiers.firstOrNull { it.type == IdentifierType.PHONE }?.rawValue

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(26.dp),
                    elevation = 16.dp,
                    surfaceAlphaTop = 0.94f,
                    surfaceAlphaBottom = 0.72f,
                    specularAlphaTop = 0.95f,
                    specularAlphaBottom = 0.35f,
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tactile Glass Pull-Down Drag Bar
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF94A3B8).copy(alpha = 0.45f))
                        .clickable {
                            Haptics.tick(ctx)
                            onDismiss()
                        }
                )

                // Contact Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Face(
                            id = radar.person.id,
                            name = radar.person.displayName,
                            status = radar.status,
                            avatar = radar.person.avatar,
                            lookupKey = radar.person.contactLookupKey,
                            size = 48,
                            showRing = true
                        )
                        Column {
                            Text(
                                text = radar.person.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = categoryName ?: "In your circle",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Tactile 3D Card Down / Dismiss Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .shadow(2.dp, CircleShape, spotColor = Color(0x200F172A))
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.White, Color(0xFFF1F5F9))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.95f), CircleShape)
                            .clickable {
                                Haptics.tick(ctx)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Dismiss Card",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Text(
                    text = "How would you like to connect with ${radar.person.displayName.split(" ").first()}?",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium
                )

                // 5 Dimensional Reach-Out Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Option 0: ⚡ 1-Tap "Caught Up Today!" (Dopamine hit + Gamification + Reset)
                    TactileChannelRow(
                        title = "⚡ Caught Up Today!",
                        subtitle = "Talked in person or direct chat (+10 Sparks)",
                        icon = Icons.Rounded.CheckCircle,
                        gradientColors = listOf(Color(0xFF059669), Color(0xFF10B981)),
                        onClick = {
                            Haptics.celebrate(ctx)
                            onCaughtUpToday()
                            onDismiss()
                        }
                    )

                    // Option 1: Text Message (SMS)
                    TactileChannelRow(
                        title = "Text Message",
                        subtitle = phone ?: "Send an SMS / iMessage",
                        icon = Icons.AutoMirrored.Rounded.Chat,
                        gradientColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
                        onClick = {
                            val smsUri = if (!phone.isNullOrBlank()) Uri.parse("smsto:$phone") else Uri.parse("sms:")
                            ctx.startActivity(Intent(Intent.ACTION_SENDTO, smsUri))
                            onDismiss()
                        }
                    )

                    // Option 2: WhatsApp
                    TactileChannelRow(
                        title = "WhatsApp",
                        subtitle = "Send quick text or voice message",
                        icon = Icons.Rounded.Forum,
                        gradientColors = listOf(Color(0xFF16A34A), Color(0xFF15803D)),
                        onClick = {
                            val cleanNumber = phone?.filter { it.isDigit() }
                            val intent = if (!cleanNumber.isNullOrBlank()) {
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanNumber"))
                            } else {
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"))
                            }
                            try {
                                ctx.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        }
                    )

                    // Option 3: Phone Call (Opens Dialer with number filled, NEVER dials by surprise!)
                    TactileChannelRow(
                        title = "Phone Call",
                        subtitle = if (phone != null) "Open dialer ($phone)" else "Open phone dialer",
                        icon = Icons.Rounded.Call,
                        gradientColors = listOf(Color(0xFF334155), Color(0xFF1E293B)),
                        onClick = {
                            val callUri = if (!phone.isNullOrBlank()) Uri.parse("tel:$phone") else Uri.parse("tel:")
                            ctx.startActivity(Intent(Intent.ACTION_DIAL, callUri))
                            onDismiss()
                        }
                    )

                    // Option 4: AI Social Wingman (Toggles inline starter drafts)
                    TactileChannelRow(
                        title = "✨ AI Social Wingman",
                        subtitle = if (showAiWingman) "Hide drafts" else "Don't know what to say? Draft 3 texts",
                        icon = Icons.Rounded.AutoAwesome,
                        gradientColors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
                        onClick = { showAiWingman = !showAiWingman }
                    )
                }

                // Inline AI Wingman Panel
                AnimatedVisibility(visible = showAiWingman) {
                    AiSocialWingmanCard(
                        name = radar.person.displayName,
                        category = categoryName,
                        daysSinceContact = radar.lastEffortAt?.let { ((System.currentTimeMillis() - it) / (1000 * 60 * 60 * 24)).toInt() },
                        talkingPoints = radar.person.talkingPoints,
                        notes = radar.person.notes,
                        phone = phone,
                        appSettings = appSettings
                    )
                }

                // 3D Tactile Open Full Profile & Notes Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = Color(0x120F172A))
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White, Color(0xFFF8FAFC))
                            )
                        )
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .clickable {
                            Haptics.tick(ctx)
                            onDismiss()
                            onOpenProfile()
                        }
                        .padding(vertical = 11.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Open Full Profile & Notes",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusColors.Cobalt,
                            fontSize = 12.5.sp
                        )
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = StatusColors.Cobalt,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TactileChannelRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape, spotColor = Color(0x150F172A))
            .clip(shape)
            .background(Color.White)
            .border(0.8.dp, Color(0xFFF1F5F9), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Dimensional 3D Icon Tile
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = gradientColors.first().copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(gradientColors))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
