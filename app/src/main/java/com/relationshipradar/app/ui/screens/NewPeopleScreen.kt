package com.relationshipradar.app.ui.screens

import android.animation.ValueAnimator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relationshipradar.app.engine.RadarStatus
import com.relationshipradar.app.ui.Face
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.SignalBars
import com.relationshipradar.app.ui.gamification.GamificationSystem
import com.relationshipradar.app.ui.gamification.Milestone
import com.relationshipradar.app.ui.gamification.OrbitMilestones
import com.relationshipradar.app.ui.liquidGlass
import com.relationshipradar.app.ui.theme.StatusColors

/**
 * Orbit Screen: The central relationship health hub and directory.
 * Combines gamified Sparks progress, Constellation milestones,
 * incoming contact sorting, and an ADHD-friendly full circle directory.
 */
@Composable
fun NewPeopleScreen(
    vm: RadarViewModel,
    onOpenPerson: (Long) -> Unit = {}
) {
    val uncategorized by vm.uncategorized.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val radarList by vm.radar.collectAsStateWithLifecycle()
    val settings by vm.appSettings.collectAsStateWithLifecycle()

    val sparks = settings.orbitSparks
    val weeklyCount = settings.weeklyConnectionsCount
    val orbitProgress = GamificationSystem.progressFor(sparks)
    val level = orbitProgress.currentLevel
    val nextLevel = orbitProgress.nextLevel
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val progressInLevel by animateFloatAsState(
        targetValue = orbitProgress.fraction,
        animationSpec = if (motionEnabled) tween(780, delayMillis = 180, easing = FastOutSlowInEasing) else tween(0),
        label = "orbit level progress",
    )

    val contactsWithCustomAvatar = radarList.count { it.person.avatar != null }
    val innerCircleTotal = radarList.count { it.category?.name?.contains("inner", ignoreCase = true) == true }
    val innerCircleHealthy = radarList.count {
        it.category?.name?.contains("inner", ignoreCase = true) == true &&
        it.status != RadarStatus.OVERDUE && it.status != RadarStatus.VERY_OVERDUE
    }

    val milestones = remember(sparks, weeklyCount, radarList.size, contactsWithCustomAvatar, innerCircleHealthy) {
        OrbitMilestones.computeMilestones(
            sparks = sparks,
            weeklyCount = weeklyCount,
            totalContacts = radarList.size,
            contactsWithCustomAvatar = contactsWithCustomAvatar,
            innerCircleHealthyCount = innerCircleHealthy,
            innerCircleTotalCount = innerCircleTotal
        )
    }

    var selectedCategoryFilter by remember { mutableStateOf<Long?>(null) }
    val filteredContacts = remember(radarList, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) radarList
        else radarList.filter { it.category?.id == selectedCategoryFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- Screen Header ------------------------------------------------------------------
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Orbit",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-1).sp,
                    )
                    Text(
                        text = "Your relationship constellation & progress",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // ---- Hero Card: Orbit Rank & Sparks Progression -------------------------------------
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(24.dp),
                        elevation = 6.dp,
                        surfaceAlphaTop = 0.88f,
                        surfaceAlphaBottom = 0.62f,
                        tintColor = Color(0xFFFFFBEB)
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
                                        )
                                    )
                                    .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(level.badgeIcon, fontSize = 22.sp)
                            }
                            Column {
                                Text(
                                    text = level.title.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFEA580C),
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "$sparks Sparks ✨",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        // Weekly Streak Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFECFDF5))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                    text = if (weeklyCount == 1) "🔥 1 connection" else "🔥 $weeklyCount connections",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF059669)
                            )
                        }
                    }

                    // Progress bar to next rank
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = nextLevel?.let { "Next: ${it.title}" } ?: "Highest orbit reached",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold
                            )
                            if (nextLevel != null) {
                                Text(
                                    text = "${sparks} / ${nextLevel.minSparks}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressInLevel)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        // ---- Constellation Milestones Carousel ----------------------------------------------
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CONSTELLATION MILESTONES",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(milestones, key = { it.id }) { m ->
                        MilestoneBadgeCard(m)
                    }
                }
            }
        }

        // ---- Unsorted Contacts Banner (Conditional Triage) ----------------------------------
        if (uncategorized.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(20.dp),
                            elevation = 5.dp,
                            surfaceAlphaTop = 0.90f,
                            surfaceAlphaBottom = 0.65f,
                            tintColor = Color(0xFFEFF6FF)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StatusColors.Cobalt.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚡", fontSize = 14.sp)
                            }
                            Text(
                                text = "${uncategorized.size} Unsorted Contacts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusColors.Cobalt
                            )
                        }

                        uncategorized.take(3).forEach { pwc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.7f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = pwc.person.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    categories.take(3).forEach { cat ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFE2E8F0))
                                                .clickable { vm.setCategory(pwc.person.id, cat.id) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF334155)
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

        // ---- Circle Directory Section -------------------------------------------------------
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CIRCLE DIRECTORY",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "${filteredContacts.size} people",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusColors.Cobalt
                    )
                }

                // Category Filter Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "All" Pill
                    val isAllSelected = selectedCategoryFilter == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isAllSelected) StatusColors.Cobalt else Color(0xFF0F172A).copy(alpha = 0.05f)
                            )
                            .clickable { selectedCategoryFilter = null }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "All (${radarList.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isAllSelected) Color.White else Color(0xFF475569)
                        )
                    }

                    // Category Pills
                    categories.forEach { cat ->
                        val count = radarList.count { it.category?.id == cat.id }
                        val isSelected = selectedCategoryFilter == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) StatusColors.Cobalt else Color(0xFF0F172A).copy(alpha = 0.05f)
                                )
                                .clickable { selectedCategoryFilter = cat.id }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "${cat.name} ($count)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }

        // ---- Contact Directory Cards --------------------------------------------------------
        items(filteredContacts, key = { "dir_${it.person.id}" }) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(20.dp),
                        elevation = 4.dp,
                        surfaceAlphaTop = 0.88f,
                        surfaceAlphaBottom = 0.58f
                    )
                    .clickable { onOpenPerson(item.person.id) }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Face(
                        id = item.person.id,
                        name = item.person.displayName,
                        status = item.status,
                        avatar = item.person.avatar,
                        lookupKey = item.person.contactLookupKey,
                        size = 46
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.person.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.lastEffortAt?.let { com.relationshipradar.app.ui.Format.ago(it) } ?: "Never reached out",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        item.category?.let { cat ->
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE2E8F0).copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = cat.name.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF475569),
                                    letterSpacing = 0.4.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Signal bars & Status Indicator
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SignalBars(
                            status = item.status,
                            maxHeight = 14.dp,
                            barWidth = 3.dp,
                            spacing = 2.dp
                        )
                        Text(
                            text = StatusColors.label(item.status),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.5.sp,
                            color = StatusColors.accent(item.status),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneBadgeCard(milestone: Milestone) {
    val isUnlocked = milestone.isUnlocked
    Box(
        modifier = Modifier
            .size(width = 150.dp, height = 125.dp)
            .liquidGlass(
                shape = RoundedCornerShape(18.dp),
                elevation = if (isUnlocked) 4.dp else 2.dp,
                surfaceAlphaTop = if (isUnlocked) 0.95f else 0.75f,
                surfaceAlphaBottom = if (isUnlocked) 0.65f else 0.45f,
                tintColor = if (isUnlocked) Color(0xFFFEF9C3) else Color.White
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(milestone.icon, fontSize = 24.sp)
                if (isUnlocked) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DONE",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF15803D)
                        )
                    }
                } else {
                    Text(
                        text = "${milestone.currentProgress}/${milestone.targetProgress}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = milestone.description,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    lineHeight = 13.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
