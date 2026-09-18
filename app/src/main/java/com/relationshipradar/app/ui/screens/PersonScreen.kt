package com.relationshipradar.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.relationshipradar.app.data.db.ContactIdentifier
import com.relationshipradar.app.data.db.CallInsight
import com.relationshipradar.app.data.db.IdentifierType
import com.relationshipradar.app.service.CallOverlayService
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource
import com.relationshipradar.app.R
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relationshipradar.app.data.db.Interaction
import com.relationshipradar.app.engine.PauseOption
import com.relationshipradar.app.ui.components.ContactNotesSection
import com.relationshipradar.app.ui.gamification.CelebrationParticleOverlay
import com.relationshipradar.app.ui.theme.Haptics
import com.relationshipradar.app.engine.RadarStatus
import com.relationshipradar.app.engine.ReminderEngine
import com.relationshipradar.app.engine.SnoozeOption
import com.relationshipradar.app.ui.AmbientGlassCanvas
import com.relationshipradar.app.ui.AiSocialWingmanCard
import com.relationshipradar.app.ui.AvatarPicker
import com.relationshipradar.app.ui.Face
import com.relationshipradar.app.ui.Format
import com.relationshipradar.app.ui.Hairline
import com.relationshipradar.app.ui.Hint
import com.relationshipradar.app.ui.OptionDialog
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.SectionHeader
import com.relationshipradar.app.ui.SignalBars
import com.relationshipradar.app.ui.StatusChip
import com.relationshipradar.app.ui.liquidGlass
import com.relationshipradar.app.ui.theme.StatusColors
import com.relationshipradar.app.connectors.callrecorder.formatTranscriptTimestamp
import java.time.ZoneId

enum class PersonTab(val label: String, val icon: String) {
    OVERVIEW("Overview", "🌟"),
    NOTES("Notes & Memory", "📝"),
    TIMELINE("Timeline", "📜")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    vm: RadarViewModel,
    personId: Long,
    onLog: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    val pwc by vm.person(personId).collectAsStateWithLifecycle(null)
    val interactions by vm.interactions(personId).collectAsStateWithLifecycle(emptyList())
    val identifiers by vm.identifiers(personId).collectAsStateWithLifecycle(emptyList())
    val callInsights by vm.callInsights(personId).collectAsStateWithLifecycle(emptyList())
    val categories by vm.categories.collectAsStateWithLifecycle()
    val appSettings by vm.appSettings.collectAsStateWithLifecycle()
    val person = pwc?.person ?: return
    val radar = ReminderEngine.evaluate(
        person,
        pwc?.category,
        interactions.firstOrNull { it.countsTowardTimer }?.timestamp
    )

    var dialog by remember { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(PersonTab.OVERVIEW) }
    var celebrationTrigger by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val bdayInfo = remember(person.birthday) { calculateDaysUntilEvent(person.birthday) }
    val annivInfo = remember(person.anniversary) { calculateDaysUntilEvent(person.anniversary) }

    AmbientGlassCanvas {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .liquidGlass(
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = 3.dp,
                                    surfaceAlphaTop = 0.85f,
                                    surfaceAlphaBottom = 0.55f,
                                )
                                .clickable { onBack() }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .liquidGlass(
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = 3.dp,
                                    surfaceAlphaTop = 0.85f,
                                    surfaceAlphaBottom = 0.55f,
                                )
                                .clickable { onOpenSettings() }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = "Contact Settings",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ---- Hero Liquid Glass Panel ----------------------------------------------------
                    item {
                        val heroShape = RoundedCornerShape(24.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = heroShape,
                                    elevation = 12.dp,
                                    surfaceAlphaTop = 0.85f,
                                    surfaceAlphaBottom = 0.55f,
                                    specularAlphaTop = 0.95f,
                                    specularAlphaBottom = 0.20f,
                                )
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Face(
                                person.id,
                                person.displayName,
                                radar.status,
                                person.avatar,
                                person.contactLookupKey,
                                size = 96,
                                modifier = Modifier.clickable { dialog = "avatar" },
                                hiRes = true,
                                showRing = false,
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { dialog = "avatar" }) {
                                Text(
                                    "Customize Icon / Photo",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatusColors.Cobalt,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                person.displayName,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A),
                                letterSpacing = (-0.5).sp,
                            )
                            Spacer(Modifier.height(6.dp))

                            // 1-Tap Category Dropdown Chip
                            val currentCategory = categories.firstOrNull { it.id == person.categoryId }
                            Box(
                                modifier = Modifier
                                    .shadow(1.dp, RoundedCornerShape(10.dp), spotColor = Color(0x150F172A))
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.White, Color(0xFFF1F5F9))
                                        )
                                    )
                                    .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        Haptics.tick(context)
                                        dialog = "category_picker"
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = currentCategory?.name?.uppercase() ?: "TAP TO CATEGORIZE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StatusColors.Cobalt,
                                        letterSpacing = 0.6.sp,
                                        fontSize = 11.5.sp
                                    )
                                    Icon(
                                        Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "Select Category",
                                        tint = StatusColors.Cobalt,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Text(
                                radar.lastEffortAt?.let { "You reached out " + Format.ago(it).lowercase() }
                                    ?: "You haven't reached out yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B),
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusChip(radar.status)
                                radar.dueAt?.let { if (radar.status != RadarStatus.TRACK_ONLY) Hint("· due " + Format.date(it)) }
                            }

                            person.snoozedUntil?.takeIf { it > System.currentTimeMillis() }?.let {
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .liquidGlass(
                                            shape = RoundedCornerShape(10.dp),
                                            elevation = 2.dp,
                                            surfaceAlphaTop = 0.90f,
                                            surfaceAlphaBottom = 0.60f
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Snoozed " + Format.until(it),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StatusColors.Amber,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            person.pausedUntil?.takeIf { it > System.currentTimeMillis() }?.let {
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .liquidGlass(
                                            shape = RoundedCornerShape(10.dp),
                                            elevation = 2.dp,
                                            surfaceAlphaTop = 0.90f,
                                            surfaceAlphaBottom = 0.60f
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Paused " + Format.until(it),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // ⚡ 1-Tap "Caught Up Today!" Action Button
                            Spacer(Modifier.height(14.dp))
                            val catchUpInteraction = remember { MutableInteractionSource() }
                            val catchUpPressed by catchUpInteraction.collectIsPressedAsState()
                            val catchUpScale by animateFloatAsState(
                                targetValue = if (catchUpPressed) 0.95f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                                label = "catchup_scale"
                            )

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = catchUpScale
                                        scaleY = catchUpScale
                                    }
                                    .shadow(if (catchUpPressed) 2.dp else 5.dp, RoundedCornerShape(16.dp), spotColor = Color(0x35059669))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF10B981), Color(0xFF059669))
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                    .clickable(
                                        interactionSource = catchUpInteraction,
                                        indication = null,
                                        onClick = {
                                            Haptics.celebrate(context)
                                            vm.recordManualCatchUp(person.id)
                                            celebrationTrigger = System.currentTimeMillis()
                                        }
                                    )
                                    .padding(horizontal = 22.dp, vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "⚡ Caught Up Today! (+10 Sparks)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.5.sp
                                    )
                                }
                            }
                        }
                    }

                    // ---- Precision 3-Tab Segmented Selector ----------------------------------
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFCBD5E1).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .padding(3.5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PersonTab.entries.forEach { tab ->
                                val isSelected = selectedTab == tab
                                val tabShape = RoundedCornerShape(11.dp)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(tabShape)
                                        .then(
                                            if (isSelected) {
                                                Modifier
                                                    .shadow(2.dp, tabShape, spotColor = Color(0x180F172A))
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color.White, Color(0xFFF8FAFC))
                                                        )
                                                    )
                                                    .border(1.dp, Color.White.copy(alpha = 0.95f), tabShape)
                                            } else {
                                                Modifier.background(Color.Transparent)
                                            }
                                        )
                                        .clickable {
                                            selectedTab = tab
                                            Haptics.tick(context)
                                        }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(tab.icon, fontSize = 12.sp)
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFF475569),
                                            fontSize = 11.5.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ---- Dynamic Tab Content ------------------------------------------------
                    when (selectedTab) {
                        PersonTab.OVERVIEW -> {
                            // Birthday / Milestone
                            if (bdayInfo != null || annivInfo != null) {
                                item {
                                    BirthdayMilestoneCard(
                                        birthdayDays = bdayInfo?.first,
                                        birthdayDateStr = bdayInfo?.second,
                                        anniversaryDays = annivInfo?.first,
                                        anniversaryDateStr = annivInfo?.second
                                    )
                                }
                            }

                            // Reach Out Dock
                            item {
                                ReachOutDock(
                                    person = person,
                                    identifiers = identifiers,
                                    onOpenSettings = onOpenSettings
                                )
                            }

                            // Catch-Up Cheat Sheet (AI Wingman & Scratchpad)
                            item {
                                SectionHeader("Catch-Up Cheat Sheet")
                                val phone = identifiers.firstOrNull { it.type == IdentifierType.PHONE }?.rawValue
                                val daysSinceContact = radar.lastEffortAt?.let { ((System.currentTimeMillis() - it) / (1000 * 60 * 60 * 24)).toInt() }
                                AiSocialWingmanCard(
                                    name = person.displayName,
                                    category = categories.firstOrNull { it.id == person.categoryId }?.name,
                                    daysSinceContact = daysSinceContact,
                                    talkingPoints = person.talkingPoints,
                                    notes = person.notes,
                                    phone = phone,
                                    appSettings = appSettings
                                )
                                Spacer(Modifier.height(14.dp))
                                TalkingPointsScratchpad(
                                    talkingPoints = person.talkingPoints,
                                    onSavePoints = { updated ->
                                        vm.updatePerson(person.copy(talkingPoints = updated))
                                    }
                                )
                            }
                        }

                        PersonTab.NOTES -> {
                            if (callInsights.isNotEmpty()) {
                                item {
                                    CallMemoriesSection(callInsights)
                                }
                            }
                            // Atomic Individual Notes List with Add & Delete
                            item {
                                ContactNotesSection(
                                    rawNotes = person.notes,
                                    onNotesUpdated = { updatedNotes ->
                                        vm.updatePerson(person.copy(notes = updatedNotes))
                                    },
                                    onAwardSparks = { vm.awardSparks(it) }
                                )
                            }
                        }

                        PersonTab.TIMELINE -> {
                            // Test Call Overlay Preview Option
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .liquidGlass(
                                            shape = RoundedCornerShape(14.dp),
                                            elevation = 3.dp,
                                            surfaceAlphaTop = 0.88f,
                                            surfaceAlphaBottom = 0.58f,
                                        )
                                        .clickable {
                                            CallOverlayService.show(
                                                context = context,
                                                name = person.displayName,
                                                lastEffort = radar.lastEffortAt?.let { "Last connected " + Format.ago(it).lowercase() } ?: "No previous calls",
                                                talkingPoints = person.talkingPoints ?: "• Catch up about upcoming plans",
                                                notes = person.notes.takeIf { it.isNotBlank() }
                                            )
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(StatusColors.Cobalt.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Rounded.PhoneAndroid,
                                                contentDescription = null,
                                                tint = StatusColors.Cobalt,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            "Preview In-Call Cheat Sheet HUD",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                    Text(
                                        "Test Overlay",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusColors.Cobalt,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Known Identifiers
                            if (identifiers.isNotEmpty()) {
                                item {
                                    SectionHeader("Known as")
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .liquidGlass(
                                                shape = RoundedCornerShape(18.dp),
                                                elevation = 6.dp,
                                                surfaceAlphaTop = 0.78f,
                                                surfaceAlphaBottom = 0.48f,
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            identifiers.forEach { id ->
                                                Text(
                                                    "${id.type.name.lowercase().replace('_', ' ')}: ${id.rawValue}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color(0xFF334155),
                                                    fontWeight = FontWeight.Medium,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Interactions Timeline
                            item {
                                SectionHeader("Timeline · ${interactions.size}")
                            }
                            if (interactions.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .liquidGlass(
                                                shape = RoundedCornerShape(18.dp),
                                                elevation = 4.dp,
                                                surfaceAlphaTop = 0.75f,
                                                surfaceAlphaBottom = 0.45f,
                                            )
                                            .padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Nothing yet. Interactions are recorded here.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF64748B),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        )
                                    }
                                }
                            } else {
                                item {
                                    val timelineShape = RoundedCornerShape(20.dp)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .liquidGlass(
                                                shape = timelineShape,
                                                elevation = 8.dp,
                                                surfaceAlphaTop = 0.80f,
                                                surfaceAlphaBottom = 0.50f,
                                            )
                                    ) {
                                        Column {
                                            interactions.forEachIndexed { idx, i ->
                                                TimelineRow(i) { vm.deleteInteraction(i) }
                                                if (idx < interactions.lastIndex) {
                                                    Hairline(Modifier.padding(horizontal = 16.dp))
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

            // Celebratory particle overlay
            CelebrationParticleOverlay(
                triggerKey = celebrationTrigger,
                onFinished = { celebrationTrigger = null }
            )
        }
    }

    when (dialog) {
        "category_picker" -> {
            val options = listOf("None (Uncategorized)") + categories.map { it.name }
            OptionDialog("Who they are to you", options, { dialog = null }) { i ->
                vm.setCategory(personId, if (i == 0) null else categories[i - 1].id)
                dialog = null
            }
        }
        "avatar" -> AvatarPicker(person.avatar, person.contactLookupKey != null, onPick = { vm.updatePerson(person.copy(avatar = it)); dialog = null }, onDismiss = { dialog = null })
    }
}

@Composable
private fun CallMemoriesSection(insights: List<CallInsight>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader("Remembered from calls")
        insights.forEach { insight ->
            val tint = when (insight.type) {
                "FOLLOW_UP" -> Color(0xFFEFF6FF)
                "UPCOMING_DATE" -> Color(0xFFFFF7ED)
                "LITTLE_THING" -> Color(0xFFFDF2F8)
                else -> Color(0xFFF5F3FF)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        elevation = 5.dp,
                        surfaceAlphaTop = .88f,
                        surfaceAlphaBottom = .58f,
                        tintColor = tint,
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        insight.type.lowercase().replace('_', ' ').uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusColors.Cobalt,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .7.sp,
                    )
                    insight.scheduledAt?.let {
                        Text(Format.date(it), style = MaterialTheme.typography.labelSmall, color = StatusColors.Flame, fontWeight = FontWeight.Bold)
                    }
                }
                Text(insight.text, style = MaterialTheme.typography.titleMedium, color = Color(0xFF0F172A), fontWeight = FontWeight.ExtraBold)
                Text(
                    "From the call · ${formatTranscriptTimestamp(insight.sourceStartMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                )
                Text(
                    "“${insight.sourceExcerpt}”",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    lineHeight = 17.sp,
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(i: Interaction, onDelete: () -> Unit) {
    val icon = when (i.type) {
        com.relationshipradar.app.data.db.InteractionType.CALL -> Icons.Rounded.Call
        com.relationshipradar.app.data.db.InteractionType.MESSAGE -> Icons.Rounded.Message
        com.relationshipradar.app.data.db.InteractionType.IN_PERSON -> Icons.Rounded.Groups
        com.relationshipradar.app.data.db.InteractionType.VIDEO_CALL -> Icons.Rounded.Videocam
        com.relationshipradar.app.data.db.InteractionType.EMAIL -> Icons.Rounded.Mail
        else -> Icons.Rounded.Schedule
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = StatusColors.Cobalt,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = when (i.type) {
                        com.relationshipradar.app.data.db.InteractionType.CALL -> "Phone Call"
                        com.relationshipradar.app.data.db.InteractionType.MESSAGE -> "Text Message"
                        com.relationshipradar.app.data.db.InteractionType.IN_PERSON -> "Met in Person"
                        com.relationshipradar.app.data.db.InteractionType.VIDEO_CALL -> "Video Call"
                        com.relationshipradar.app.data.db.InteractionType.EMAIL -> "Email"
                        else -> "Quick Moment"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                )
                if (i.countsTowardTimer) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+10 Sparks ✨",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD97706),
                        )
                    }
                }
            }
            if (i.note.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = i.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF334155),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = Format.ago(i.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Delete",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ---- Milestone & Birthday Helpers -----------------------------------------------------------

fun calculateDaysUntilEvent(dateStr: String?): Pair<Long, String>? {
    if (dateStr.isNullOrBlank()) return null
    return try {
        val today = java.time.LocalDate.now()
        val clean = dateStr.trim().removePrefix("--")
        val parts = clean.split("-")
        val (month, day) = if (parts.size >= 3) {
            Pair(parts[1].toInt(), parts[2].toInt())
        } else if (parts.size == 2) {
            Pair(parts[0].toInt(), parts[1].toInt())
        } else return null

        var nextEvent = java.time.LocalDate.of(today.year, month, day)
        if (nextEvent.isBefore(today)) {
            nextEvent = java.time.LocalDate.of(today.year + 1, month, day)
        }
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, nextEvent)
        val monthName = nextEvent.month.name.lowercase().replaceFirstChar { it.uppercase() }
        Pair(days, "$monthName $day")
    } catch (e: Exception) {
        null
    }
}

@Composable
fun BirthdayMilestoneCard(
    birthdayDays: Long?,
    birthdayDateStr: String?,
    anniversaryDays: Long?,
    anniversaryDateStr: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(18.dp),
                elevation = 4.dp,
                surfaceAlphaTop = 0.88f,
                surfaceAlphaBottom = 0.58f,
            )
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            birthdayDays?.let { days ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StatusColors.Flame.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Cake,
                            contentDescription = null,
                            tint = StatusColors.Flame,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (days == 0L) "Birthday Today!" else "Birthday in $days days ($birthdayDateStr)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = if (days <= 7) StatusColors.Flame else Color(0xFF0F172A)
                    )
                }
            }
            anniversaryDays?.let { days ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEC4899).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFEC4899),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (days == 0L) "Anniversary Today!" else "Anniversary in $days days ($anniversaryDateStr)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        }
    }
}

// ---- 1-Tap Reach-Out & Socials Dock --------------------------------------------------------

private data class ChannelTile(
    val label: String,
    val drawableRes: Int,
    val gradient: List<Color>,
    val action: () -> Unit,
)

@Composable
fun ReachOutDock(
    person: com.relationshipradar.app.data.db.Person,
    identifiers: List<ContactIdentifier>,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val phone = identifiers.firstOrNull { it.type == IdentifierType.PHONE }?.rawValue ?: person.whatsappNumber
    val email = identifiers.firstOrNull { it.type == IdentifierType.EMAIL }?.rawValue

    // Build only channels that are actually configured for this person
    val channels = buildList {
        if (!phone.isNullOrBlank()) {
            add(
                ChannelTile(
                    label = "Call",
                    drawableRes = R.drawable.ic_brand_call,
                    gradient = listOf(Color(0xFF059669), Color(0xFF064E3B)),
                    action = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }
                )
            )
            add(
                ChannelTile(
                    label = "Text",
                    drawableRes = R.drawable.ic_brand_message,
                    gradient = listOf(Color(0xFF2563EB), Color(0xFF1E40AF)),
                    action = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))) }
                )
            )
        }
        person.whatsappNumber?.takeIf { it.isNotBlank() }?.let { num ->
            val clean = num.filter { it.isDigit() }
            add(
                ChannelTile(
                    label = "WhatsApp",
                    drawableRes = R.drawable.ic_brand_whatsapp,
                    gradient = listOf(Color(0xFF16A34A), Color(0xFF14532D)),
                    action = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$clean"))) }
                )
            )
        }
        person.messengerHandle?.takeIf { it.isNotBlank() }?.let { handle ->
            add(
                ChannelTile(
                    label = "Messenger",
                    drawableRes = R.drawable.ic_brand_messenger,
                    gradient = listOf(Color(0xFF0084FF), Color(0xFF4F46E5)),
                    action = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://m.me/$handle"))) }
                )
            )
        }
        person.instagramHandle?.takeIf { it.isNotBlank() }?.let { handle ->
            add(
                ChannelTile(
                    label = "Insta DM",
                    drawableRes = R.drawable.ic_brand_instagram,
                    gradient = listOf(Color(0xFFF59E0B), Color(0xFFE11D48), Color(0xFF9333EA)),
                    action = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ig.me/m/$handle"))) }
                )
            )
        }
        person.snapchatHandle?.takeIf { it.isNotBlank() }?.let { handle ->
            add(
                ChannelTile(
                    label = "Snap",
                    drawableRes = R.drawable.ic_brand_snapchat,
                    gradient = listOf(Color(0xFFEAB308), Color(0xFFD97706)),
                    action = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://snapchat.com/add/$handle"))) }
                )
            )
        }
        person.discordHandle?.takeIf { it.isNotBlank() }?.let { handle ->
            add(
                ChannelTile(
                    label = "Discord",
                    drawableRes = R.drawable.ic_brand_discord,
                    gradient = listOf(Color(0xFF6366F1), Color(0xFF4338CA)),
                    action = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com/users/$handle"))) }
                )
            )
        }
        person.meetLink?.takeIf { it.isNotBlank() }?.let { link ->
            add(
                ChannelTile(
                    label = "Meet",
                    drawableRes = R.drawable.ic_brand_meet,
                    gradient = listOf(Color(0xFF10B981), Color(0xFF0D9488)),
                    action = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                )
            )
        }
        if (!email.isNullOrBlank()) {
            add(
                ChannelTile(
                    label = "Email",
                    drawableRes = R.drawable.ic_brand_mail,
                    gradient = listOf(Color(0xFF64748B), Color(0xFF334155)),
                    action = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))) }
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(20.dp),
                elevation = 6.dp,
                surfaceAlphaTop = 0.85f,
                surfaceAlphaBottom = 0.55f,
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Quick Reach Out",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )
            Text(
                if (channels.isNotEmpty()) "${channels.size} Connected" else "Not configured",
                style = MaterialTheme.typography.labelSmall,
                color = if (channels.isNotEmpty()) StatusColors.Cobalt else Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold
            )
        }

        if (channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenSettings() }
                    .background(Color.White.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        tint = StatusColors.Cobalt,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Add phone or social handles to reach out",
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusColors.Cobalt,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                channels.forEach { ch ->
                    DockPill(
                        drawableRes = ch.drawableRes,
                        label = ch.label,
                        gradient = ch.gradient,
                        onClick = ch.action
                    )
                }
                // Add channel action tile
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 3.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x180F172A))
                            .border(
                                1.2.dp,
                                Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.9f), Color(0x3064748B))
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Add Channel",
                            tint = StatusColors.Cobalt,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Add",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun DockPill(
    label: String,
    gradient: List<Color>,
    drawableRes: Int? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    val primaryColor = gradient.first()
    val deepColor = gradient.last()
    val shape = RoundedCornerShape(16.dp)

    // Tactile Spring Press Dynamics
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "pill_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(
                    elevation = if (isPressed) 2.dp else 5.dp,
                    shape = shape,
                    spotColor = primaryColor.copy(alpha = 0.45f),
                    ambientColor = Color(0x180F172A)
                )
                .border(
                    width = 1.3.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            primaryColor.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.30f),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(100f, 100f)
                    ),
                    shape = shape
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.94f),
                            Color.White.copy(alpha = 0.72f),
                            primaryColor.copy(alpha = 0.16f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Optical glass upper sheen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.65f),
                                Color.Transparent
                            )
                        )
                    )
            )

            if (drawableRes != null) {
                if (drawableRes == R.drawable.ic_brand_snapchat || drawableRes == R.drawable.ic_brand_meet || drawableRes == R.drawable.ic_brand_discord) {
                    Icon(
                        painter = painterResource(drawableRes),
                        contentDescription = label,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(drawableRes),
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer(alpha = 0.99f)
                            .drawWithCache {
                                val brush = Brush.linearGradient(gradient)
                                onDrawWithContent {
                                    drawContent()
                                    drawRect(brush, blendMode = BlendMode.SrcAtop)
                                }
                            }
                    )
                }
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF334155)
        )
    }
}

// ---- Catch-Up Talking Points Scratchpad ----------------------------------------------------

@Composable
fun TalkingPointsScratchpad(
    talkingPoints: String?,
    onSavePoints: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newPointText by remember { mutableStateOf("") }
    val pointsList = remember(talkingPoints) {
        talkingPoints?.lines()?.filter { it.isNotBlank() } ?: emptyList()
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spokenText.isNullOrBlank()) {
            val updated = if (talkingPoints.isNullOrBlank()) "• $spokenText" else "$talkingPoints\n• $spokenText"
            onSavePoints(updated)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(20.dp),
                elevation = 6.dp,
                surfaceAlphaTop = 0.85f,
                surfaceAlphaBottom = 0.55f
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Things To Bring Up",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
                Text(
                    "Talking points & catch-up cheat sheet",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }

            // Voice Dictation Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say talking points to remember...")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            // Speech service not available on device
                        }
                    }
                    .background(StatusColors.Cobalt.copy(alpha = 0.08f))
                    .border(1.dp, StatusColors.Cobalt.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = StatusColors.Cobalt,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    "Dictate",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StatusColors.Cobalt
                )
            }
        }

        if (pointsList.isEmpty()) {
            Text(
                "No talking points yet. Dictate or add what you want to ask next time!",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        } else {
            pointsList.forEach { point ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x0A0F172A), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        point.removePrefix("• ").trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Remove",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                val remaining = pointsList.filter { it != point }.joinToString("\n")
                                onSavePoints(remaining)
                            }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newPointText,
                onValueChange = { newPointText = it },
                placeholder = { Text("Add topic / question...", style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = {
                    if (newPointText.isNotBlank()) {
                        val updated = if (talkingPoints.isNullOrBlank()) "• $newPointText" else "$talkingPoints\n• $newPointText"
                        onSavePoints(updated)
                        newPointText = ""
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Text("Add", style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
        }
    }
}
