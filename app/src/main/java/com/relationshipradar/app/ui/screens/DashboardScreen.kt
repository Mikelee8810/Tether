package com.relationshipradar.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import com.relationshipradar.app.data.repo.AppSettings
import com.relationshipradar.app.ui.QuickReachOutGlassSheet
import com.relationshipradar.app.ui.gamification.CelebrationParticleOverlay
import com.relationshipradar.app.ui.gamification.OrbitStarlightBanner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relationshipradar.app.engine.PersonRadar
import com.relationshipradar.app.engine.RadarStatus
import com.relationshipradar.app.engine.ReminderEngine.needsAttention
import com.relationshipradar.app.ui.AmbientGlassCanvas
import com.relationshipradar.app.ui.Face
import com.relationshipradar.app.ui.Format
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.SignalBars
import com.relationshipradar.app.ui.liquidGlass
import com.relationshipradar.app.ui.theme.Radar
import com.relationshipradar.app.ui.theme.StatusColors

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.relationshipradar.app.ui.theme.EditorialSerif



private enum class Filter(val label: String) {
    ATTENTION("Reconnect"),
    FREQUENT("Frequent"),
    ACTIVE("Active"),
    ALL("All")
}

@Composable
fun DashboardScreen(
    vm: RadarViewModel,
    onOpenPerson: (Long) -> Unit,
    onOpenNewPeople: () -> Unit,
    onOpenWho: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val radar by vm.radar.collectAsStateWithLifecycle()
    val uncategorized by vm.uncategorized.collectAsStateWithLifecycle()
    val pending by vm.pendingIdentities.collectAsStateWithLifecycle()
    val appSettings by vm.appSettings.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(Filter.ATTENTION) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var reachOutTarget by remember { mutableStateOf<PersonRadar?>(null) }
    var celebrationTrigger by remember { mutableStateOf<Long?>(null) }
    var sparkIndex by rememberSaveable { mutableIntStateOf(0) }

    val attention = radar.filter { it.status.needsAttention }
    val sparkCandidates = if (attention.isNotEmpty()) attention else radar
    val activeCandidate = sparkCandidates.getOrNull(if (sparkCandidates.isNotEmpty()) sparkIndex % sparkCandidates.size else 0)

    val baseList = when (filter) {
        Filter.ATTENTION -> attention
        Filter.FREQUENT -> radar.sortedByDescending { it.lastEffortAt ?: 0L }
        Filter.ACTIVE -> radar.filter { it.status != RadarStatus.TRACK_ONLY }
        Filter.ALL -> radar
    }
    val shown = if (searchQuery.isBlank()) {
        baseList
    } else {
        baseList.filter {
            it.person.displayName.contains(searchQuery, ignoreCase = true) ||
            it.category?.name?.contains(searchQuery, ignoreCase = true) == true ||
            it.person.notes.contains(searchQuery, ignoreCase = true) ||
            it.person.talkingPoints?.contains(searchQuery, ignoreCase = true) == true
        }
    }.sortedWith(
        compareByDescending<PersonRadar> { rank(it.status) }
            .thenBy { it.person.displayName.lowercase() }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 160.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        // ---- Screen Title & Top Actions (Interwoven Tether Emblem) ----------------------
        item(key = "screen_title") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TetherBrandMark(
                    attentionCount = attention.size,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sparks Counter Pill (1-tap shortcut to Orbit Constellation)
                    Box(
                        modifier = Modifier
                            .liquidGlass(
                                shape = RoundedCornerShape(14.dp),
                                elevation = 3.dp,
                                surfaceAlphaTop = 0.88f,
                                surfaceAlphaBottom = 0.60f,
                                tintColor = Color(0xFFFFFBEB)
                            )
                            .clickable { onOpenNewPeople() }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("✨", fontSize = 12.sp)
                            Text(
                                text = "${appSettings.orbitSparks}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFD97706),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // + Track Person
                    Box(
                        modifier = Modifier
                            .liquidGlass(
                                shape = RoundedCornerShape(14.dp),
                                elevation = 4.dp,
                                surfaceAlphaTop = 0.85f,
                                surfaceAlphaBottom = 0.55f,
                            )
                            .clickable { showAddPersonDialog = true }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Add Person",
                                tint = StatusColors.Cobalt,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Track",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = StatusColors.Cobalt
                            )
                        }
                    }

                    // Settings
                    Box(
                        modifier = Modifier
                            .liquidGlass(
                                shape = RoundedCornerShape(14.dp),
                                elevation = 4.dp,
                                surfaceAlphaTop = 0.85f,
                                surfaceAlphaBottom = 0.55f,
                            )
                            .clickable { onOpenSettings() }
                            .padding(9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Tune,
                            contentDescription = "Settings",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        // ---- Instant Search Bar --------------------------------------------------------
        item(key = "search_bar") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(16.dp),
                        elevation = 4.dp,
                        surfaceAlphaTop = 0.88f,
                        surfaceAlphaBottom = 0.60f,
                    )
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Medium
                        ),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search people, categories, topics...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }
        }

        // ---- Inner Circle Orbit Tray (Locket / Story Faces Carousel) ------------------
        if (radar.isNotEmpty()) {
            item(key = "orbit_tray") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INNER CIRCLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp,
                        )
                        Text(
                            text = "${radar.size} in orbit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusColors.Cobalt,
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        items(radar, key = { "orbit_${it.person.id}" }) { pr ->
                            OrbitFaceItem(
                                radar = pr,
                                onClick = { onOpenPerson(pr.person.id) }
                            )
                        }
                    }
                }
            }
        }

        // ---- Daily Spark Hero Card (Spacious, Warm, Elevated) -----------------------
        if (activeCandidate != null) {
            item(key = "daily_spark_hero") {
                TodaySparkHeroCard(
                    candidate = activeCandidate,
                    canShuffle = sparkCandidates.size > 1,
                    onShuffle = { sparkIndex++ },
                    onOpenPerson = onOpenPerson,
                    onReachOut = { reachOutTarget = it }
                )
            }
        }

        // ---- Uncategorized / Unmatched Contacts Banner ------------------------------
        if (uncategorized.isNotEmpty() || pending.isNotEmpty()) {
            item(key = "unorganized_banner") {
                UnorganizedContactsBanner(
                    uncategorizedCount = uncategorized.size,
                    pendingCount = pending.size,
                    onOpenNewPeople = onOpenNewPeople,
                    onOpenWho = onOpenWho
                )
            }
        }

        // ---- Orbit Pulse Section Header & Precision Filter Bar ----------------------
        item(key = "orbit_pulse_section") {
            OrbitPulseHeader(
                attentionCount = attention.size,
                totalCount = radar.size,
                currentFilter = filter,
                onFilterSelected = { filter = it }
            )
        }

            // ---- Empty State --------------------------------------------------------------------
            if (shown.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(shape = RoundedCornerShape(20.dp), elevation = 6.dp)
                            .padding(vertical = 40.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (filter == Filter.ATTENTION) "All connection signals are strong." else "No connections found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            } else {
                // ---- Individual Floating Liquid Glass Cards with Breathing Room -------------
                items(shown, key = { it.person.id }) { personRadar ->
                    ConnectionGlassCard(
                        radar = personRadar,
                        onClick = { onOpenPerson(personRadar.person.id) },
                        onReachOut = { reachOutTarget = personRadar }
                    )
                }
            }
    }

    if (showAddPersonDialog) {
        var newName by remember { mutableStateOf("") }
        var newNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            title = {
                Text(
                    "Track New Person",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Add someone to Tether so you never lose touch.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it },
                        label = { Text("Context / Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            vm.createPersonWithNotes(newName.trim(), null, newNotes.trim())
                            showAddPersonDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Text("Add to Tether", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPersonDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    reachOutTarget?.let { target ->
        val identifiers by vm.identifiers(target.person.id).collectAsStateWithLifecycle(emptyList())
        QuickReachOutGlassSheet(
            radar = target,
            categoryName = target.category?.name,
            identifiers = identifiers,
            appSettings = appSettings,
            onDismiss = { reachOutTarget = null },
            onOpenProfile = {
                val targetId = target.person.id
                reachOutTarget = null
                onOpenPerson(targetId)
            },
            onCaughtUpToday = {
                vm.recordManualCatchUp(target.person.id)
                celebrationTrigger = System.currentTimeMillis()
            }
        )
    }

    CelebrationParticleOverlay(
        triggerKey = celebrationTrigger,
        onFinished = { celebrationTrigger = null }
    )
}
}

private fun rank(s: RadarStatus) = when (s) {
    RadarStatus.VERY_OVERDUE -> 6
    RadarStatus.OVERDUE -> 5
    RadarStatus.DUE_SOON -> 4
    RadarStatus.GOOD -> 3
    RadarStatus.SNOOZED -> 2
    RadarStatus.PAUSED -> 1
    RadarStatus.TRACK_ONLY -> 0
}

/** Dedicated, elevated Today's Spark hero card with spacious layout and zero crowding. */
@Composable
private fun TodaySparkHeroCard(
    candidate: PersonRadar,
    canShuffle: Boolean,
    onShuffle: () -> Unit,
    onOpenPerson: (Long) -> Unit,
    onReachOut: (PersonRadar) -> Unit,
    modifier: Modifier = Modifier
) {
    val person = candidate.person
    val firstTopic = person.talkingPoints?.lines()?.firstOrNull { it.isNotBlank() }?.removePrefix("- ")?.removePrefix("• ")?.trim()
    val promptText = when {
        firstTopic != null -> firstTopic
        person.birthday != null -> "Birthday is coming up!"
        else -> "Hasn't heard from you in a while"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                elevation = 6.dp,
                surfaceAlphaTop = 0.88f,
                surfaceAlphaBottom = 0.58f,
                specularAlphaTop = 0.95f,
                tintColor = Color(0xFFFFF7ED)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Top Header: Badge + Shuffle button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEDD5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 12.sp)
                    }
                    Text(
                        text = "TODAY'S SPARK",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEA580C),
                        letterSpacing = 0.8.sp
                    )
                }

                if (canShuffle) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.80f))
                            .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .clickable(onClick = onShuffle)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = "Shuffle",
                                tint = StatusColors.Cobalt,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Shuffle",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusColors.Cobalt
                            )
                        }
                    }
                }
            }

            // Main Content Row: Face, Info, and Say Hey Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Face(
                    id = person.id,
                    name = person.displayName,
                    status = candidate.status,
                    avatar = person.avatar,
                    lookupKey = person.contactLookupKey,
                    size = 48,
                    showRing = true,
                    shape = CircleShape
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenPerson(person.id) }
                ) {
                    Text(
                        text = person.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = promptText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tactile 3D Say Hey Button
                Box(
                    modifier = Modifier
                        .shadow(3.dp, RoundedCornerShape(12.dp), spotColor = Color(0x35E11D48))
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFF5757), StatusColors.Flame)
                            )
                        )
                        .border(0.8.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { onReachOut(candidate) }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            "Say Hey",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/** Clean section header for Orbit Pulse with signal strength and the 4-pill segmented filter bar. */
@Composable
private fun OrbitPulseHeader(
    attentionCount: Int,
    totalCount: Int,
    currentFilter: Filter,
    onFilterSelected: (Filter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ORBIT PULSE",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        totalCount == 0 -> "No Active Connections"
                        attentionCount == 0 -> "All Connections Vibrant ✨"
                        attentionCount == 1 -> "1 Ready to Reconnect"
                        else -> "$attentionCount to Reconnect"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (attentionCount > 0) StatusColors.Flame else Color(0xFF0F172A)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val connectedCount = (totalCount - attentionCount).coerceAtLeast(0)
                Text(
                    text = "$connectedCount of $totalCount in touch",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
                SignalBars(
                    status = if (attentionCount > 0) RadarStatus.OVERDUE else RadarStatus.GOOD,
                    maxHeight = 18.dp,
                    barWidth = 4.dp,
                    spacing = 2.dp
                )
            }
        }

        // Precision Glass Segmented Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(14.dp),
                    elevation = 2.dp,
                    surfaceAlphaTop = 0.85f,
                    surfaceAlphaBottom = 0.55f
                )
                .padding(3.5.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Filter.entries.forEach { f ->
                val isSelected = currentFilter == f
                val itemShape = RoundedCornerShape(11.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(itemShape)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .shadow(2.dp, itemShape, spotColor = Color(0x150F172A))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.White, Color(0xFFF8FAFC))
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.95f), itemShape)
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable { onFilterSelected(f) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (f) {
                            Filter.ATTENTION -> if (attentionCount > 0) "Reconnect ($attentionCount)" else "Reconnect"
                            Filter.FREQUENT -> "Frequent"
                            Filter.ACTIVE -> "Active"
                            Filter.ALL -> "All ($totalCount)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFF475569),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Clean liquid glass banner for uncategorized or unmatched contacts. */
@Composable
private fun UnorganizedContactsBanner(
    uncategorizedCount: Int,
    pendingCount: Int,
    onOpenNewPeople: () -> Unit,
    onOpenWho: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bannerShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = bannerShape, elevation = 4.dp, surfaceAlphaTop = 0.85f, surfaceAlphaBottom = 0.60f)
            .clickable { if (uncategorizedCount > 0) onOpenNewPeople() else onOpenWho() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (uncategorizedCount > 0) "$uncategorizedCount new people to organize" else "$pendingCount unmatched contacts",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = StatusColors.Cobalt,
            )
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = StatusColors.Cobalt,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Precision Apple Liquid Glass connection card with tactile spring press and ambient status luminescence. */
@Composable
private fun ConnectionGlassCard(
    radar: PersonRadar,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onReachOut: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "press_scale"
    )

    val tintColor = when (radar.status) {
        RadarStatus.VERY_OVERDUE -> Color(0xFFFFF1F0) // Subtle warm peach/flame refraction
        RadarStatus.OVERDUE -> Color(0xFFFFF7ED)      // Subtle warm sunrise refraction
        RadarStatus.DUE_SOON -> Color(0xFFFFFBEB)     // Subtle golden amber refraction
        RadarStatus.GOOD -> Color(0xFFF0FDF4)         // Subtle fresh emerald refraction
        else -> Color.White
    }

    val cardShape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .liquidGlass(
                shape = cardShape,
                elevation = if (isPressed) 3.dp else 8.dp,
                surfaceAlphaTop = 0.82f,
                surfaceAlphaBottom = 0.52f,
                specularAlphaTop = 0.95f,
                specularAlphaBottom = 0.20f,
                tintColor = tintColor,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                // Radiant Gemstone Squircle Monogram or Photo
                Face(
                    id = radar.person.id,
                    name = radar.person.displayName,
                    status = radar.status,
                    avatar = radar.person.avatar,
                    lookupKey = radar.person.contactLookupKey,
                    size = 50,
                    showRing = false,
                )

                // Contact Info & Relationship Meta: Name -> Last Contacted -> Category
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.5.dp)
                ) {
                    // 1. Name
                    Text(
                        text = radar.person.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 2. Last Contacted (Between Name and Category)
                    Text(
                        text = radar.lastEffortAt?.let { Format.ago(it) } ?: "Never reached out",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )

                    // 3. Category Badge Pill
                    radar.category?.let { cat ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE2E8F0).copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = cat.name.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF475569),
                                letterSpacing = 0.4.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Sleek Tactile Quick-Connect Action & Signal Strength Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cell Phone Signal Strength Indicator with clean status
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        SignalBars(
                            status = radar.status,
                            maxHeight = 13.dp,
                            barWidth = 3.dp,
                            spacing = 2.dp,
                        )
                        Text(
                            text = StatusColors.label(radar.status),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = StatusColors.accent(radar.status),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                        )
                    }

                    // Sleek Tactile 3D Glass Connect Tile (Zero generic bulk)
                    val btnInteractionSource = remember { MutableInteractionSource() }
                    val btnPressed by btnInteractionSource.collectIsPressedAsState()
                    val btnScale by animateFloatAsState(
                        targetValue = if (btnPressed) 0.90f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                        label = "reach_btn_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .graphicsLayer {
                                scaleX = btnScale
                                scaleY = btnScale
                            }
                            .shadow(if (btnPressed) 1.dp else 3.dp, RoundedCornerShape(12.dp), spotColor = if (radar.status.needsAttention) Color(0x35E11D48) else Color(0x180F172A))
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (radar.status.needsAttention)
                                    Brush.verticalGradient(listOf(Color(0xFFFFF1EE), Color(0xFFFFD9D2)))
                                else
                                    Brush.verticalGradient(listOf(Color.White, Color(0xFFF1F5F9)))
                            )
                            .border(
                                1.dp,
                                if (radar.status.needsAttention) StatusColors.Flame.copy(alpha = 0.55f)
                                else Color.White.copy(alpha = 0.95f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(
                                interactionSource = btnInteractionSource,
                                indication = null,
                                onClick = onReachOut
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = "Reach Out",
                            tint = if (radar.status.needsAttention) StatusColors.Flame else StatusColors.Cobalt,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Contextual Memory Chip (talking point, topic, notes) - full width with zero cutoff!
            val firstTopic = radar.person.talkingPoints?.lines()?.firstOrNull { it.isNotBlank() }?.removePrefix("- ")?.removePrefix("• ")?.trim()
            if (!firstTopic.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 63.dp)
                        .background(Color(0xFFF1F5F9).copy(alpha = 0.85f), RoundedCornerShape(7.dp))
                        .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(7.dp))
                        .padding(horizontal = 9.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = "💭 $firstTopic",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Interwoven Tether Brand Mark:
 * An intertwined liquid glass cord / knot logo where two luminous loops link through each other,
 * paired with bespoke editorial serif typography and a living ambient status pulse.
 */
@Composable
fun TetherBrandMark(
    attentionCount: Int,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tether_brand_pulse")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_phase"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Interwoven Liquid Glass Tether Knot Emblem
        Box(
            modifier = Modifier
                .size(42.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(13.dp),
                    elevation = 6.dp,
                    surfaceAlphaTop = 0.88f,
                    surfaceAlphaBottom = 0.60f,
                    specularAlphaTop = 0.98f,
                    specularAlphaBottom = 0.25f,
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(28.dp)) {
                val w = size.width
                val h = size.height
                val strokeW = 4.2.dp.toPx()

                // Left loop (Indigo to Cobalt)
                val leftPath = Path().apply {
                    moveTo(w * 0.45f, h * 0.28f)
                    cubicTo(w * 0.22f, h * 0.28f, w * 0.08f, h * 0.40f, w * 0.08f, h * 0.54f)
                    cubicTo(w * 0.08f, h * 0.68f, w * 0.22f, h * 0.80f, w * 0.45f, h * 0.80f)
                    cubicTo(w * 0.60f, h * 0.80f, w * 0.68f, h * 0.70f, w * 0.72f, h * 0.58f)
                }

                // Right loop (Coral to Sunset Flame)
                val rightPath = Path().apply {
                    moveTo(w * 0.55f, h * 0.72f)
                    cubicTo(w * 0.78f, h * 0.72f, w * 0.92f, h * 0.60f, w * 0.92f, h * 0.46f)
                    cubicTo(w * 0.92f, h * 0.32f, w * 0.78f, h * 0.20f, w * 0.55f, h * 0.20f)
                    cubicTo(w * 0.40f, h * 0.20f, w * 0.32f, h * 0.30f, w * 0.28f, h * 0.42f)
                }

                // Draw background shadows / depth
                drawPath(
                    path = leftPath,
                    color = Color(0x304338CA),
                    style = Stroke(width = strokeW + 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = rightPath,
                    color = Color(0x30EA580C),
                    style = Stroke(width = strokeW + 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw Left Loop with Electric Gradient
                val leftBrush = Brush.linearGradient(
                    colors = listOf(Color(0xFF4F46E5), Color(0xFF38BDF8)),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.75f, h)
                )
                drawPath(
                    path = leftPath,
                    brush = leftBrush,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw Right Loop with Warm Sunset Gradient
                val rightBrush = Brush.linearGradient(
                    colors = listOf(Color(0xFFEA580C), Color(0xFFFBBF24)),
                    start = Offset(w * 0.25f, 0f),
                    end = Offset(w, h)
                )
                drawPath(
                    path = rightPath,
                    brush = rightBrush,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Interwoven crossover overpass (Left loop passes OVER top right loop)
                val overpassPath = Path().apply {
                    moveTo(w * 0.40f, h * 0.28f)
                    cubicTo(w * 0.48f, h * 0.28f, w * 0.56f, h * 0.34f, w * 0.62f, h * 0.44f)
                }
                drawPath(
                    path = overpassPath,
                    brush = leftBrush,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Specular Glass Gleam on Crest
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = 1.8.dp.toPx(),
                    center = Offset(w * 0.24f, h * 0.35f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = 1.8.dp.toPx(),
                    center = Offset(w * 0.76f, h * 0.65f)
                )
            }
        }

        // Brand Typography & Living Status Capsule
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Tether",
                    fontFamily = EditorialSerif,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.5).sp,
                )
                // Pulse dot
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(if (attentionCount > 0) StatusColors.Flame else StatusColors.Emerald)
                )
            }
            Text(
                text = if (attentionCount > 0) "$attentionCount to reconnect" else "all close in orbit",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (attentionCount > 0) StatusColors.Flame else Color(0xFF64748B),
                letterSpacing = 0.4.sp,
            )
        }
    }
}

/**
 * Inner Circle Story Face Item:
 * Gen-Z / Locket-style interactive circular orbit face item with rotating halo story ring.
 * Features 100% concentric circular geometry with uniform gap spacing (Zero oblong distortion).
 */
@Composable
fun OrbitFaceItem(
    radar: PersonRadar,
    onClick: () -> Unit,
) {
    val rawName = radar.person.displayName.trim()
    val firstName = if (rawName.length <= 11) rawName else (rawName.split("\\s+".toRegex()).firstOrNull() ?: rawName)
    val needsLove = radar.status.needsAttention

    val infiniteTransition = rememberInfiniteTransition(label = "orbit_story_${radar.person.id}")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )
    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (needsLove) 1.05f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(62.dp)
        ) {
            // 1. Concentric Animated Story Ring (Full 360-degree circular symmetry, zero distortion)
            Canvas(
                modifier = Modifier
                    .size(62.dp)
                    .graphicsLayer {
                        rotationZ = ringRotation
                        scaleX = ringPulse
                        scaleY = ringPulse
                    }
            ) {
                val strokeWidth = 2.5.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val ringBrush = if (needsLove) {
                    Brush.sweepGradient(
                        listOf(
                            StatusColors.Flame,
                            Color(0xFFF59E0B),
                            Color(0xFFEF4444),
                            StatusColors.Flame,
                        )
                    )
                } else {
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFF10B981),
                            Color(0xFF06B6D4),
                            Color(0xFF3B82F6),
                            Color(0xFF10B981),
                        )
                    )
                }
                drawCircle(
                    brush = ringBrush,
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
            }

            // 2. Crisp Circular Separation Gap & Face Avatar
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Face(
                    id = radar.person.id,
                    name = radar.person.displayName,
                    status = radar.status,
                    avatar = radar.person.avatar,
                    lookupKey = radar.person.contactLookupKey,
                    size = 49,
                    showRing = false,
                    shape = CircleShape
                )
            }
        }

        Text(
            text = firstName,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

