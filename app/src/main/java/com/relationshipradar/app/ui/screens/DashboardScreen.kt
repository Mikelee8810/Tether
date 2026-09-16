package com.relationshipradar.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relationshipradar.app.engine.PersonRadar
import com.relationshipradar.app.engine.RadarStatus
import com.relationshipradar.app.engine.ReminderEngine.needsAttention
import com.relationshipradar.app.ui.Face
import com.relationshipradar.app.ui.Format
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.Sheet
import com.relationshipradar.app.ui.theme.Radar
import com.relationshipradar.app.ui.theme.StatusColors

private enum class Filter(val label: String) { ATTENTION("Needs you"), REMINDERS("Reminders"), ALL("Everyone") }

/** Big faces = closest people (by category). Overdue faces desaturate. Tap a face → person. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DashboardScreen(vm: RadarViewModel, onOpenPerson: (Long) -> Unit, onOpenNewPeople: () -> Unit, onOpenWho: () -> Unit) {
    val radar by vm.radar.collectAsStateWithLifecycle()
    val uncategorized by vm.uncategorized.collectAsStateWithLifecycle()
    val pending by vm.pendingIdentities.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(Filter.ATTENTION) }

    val attention = radar.filter { it.status.needsAttention }
    val shown = when (filter) {
        Filter.ATTENTION -> attention
        Filter.REMINDERS -> radar.filter { it.status != RadarStatus.TRACK_ONLY }
        Filter.ALL -> radar
    }.sortedWith(compareByDescending<PersonRadar> { tileSpan(it) }.thenByDescending { rank(it.status) }.thenBy { it.person.displayName.lowercase() })

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(start = Radar.sp3.dp, end = Radar.sp3.dp, bottom = Radar.fabClearance.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(span = { GridItemSpan(4) }) {
            Column(Modifier.padding(horizontal = Radar.sp2.dp)) {
                Spacer(Modifier.height(Radar.sp2.dp))
                // Encouraging, non-guilt header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            when {
                                radar.isEmpty() -> "Welcome"
                                attention.isEmpty() -> "All caught up ✨"
                                attention.size == 1 -> "1 person to reach out to"
                                else -> "${attention.size} people to reach out to"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        )
                        Text(
                            if (attention.isEmpty()) "Your connections are glowing and healthy." else "Gentle nudges to keep your bonds strong.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(Radar.sp3.dp))
                if (uncategorized.isNotEmpty() || pending.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Radar.sp2.dp)) {
                        if (uncategorized.isNotEmpty()) Nudge("${uncategorized.size} new ${if (uncategorized.size == 1) "person" else "people"}", "Say who they are to you", onOpenNewPeople)
                        if (pending.isNotEmpty()) Nudge("${pending.size} unmatched ${if (pending.size == 1) "contact" else "contacts"}", "A number or chat that couldn't be placed", onOpenWho)
                    }
                    Spacer(Modifier.height(Radar.sp3.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    Filter.entries.forEachIndexed { i, f ->
                        ToggleButton(
                            checked = filter == f, onCheckedChange = { filter = f },
                            shapes = when (i) { 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes(); Filter.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes(); else -> ButtonGroupDefaults.connectedMiddleButtonShapes() },
                        ) { Text(f.label) }
                    }
                }
                Spacer(Modifier.height(Radar.sp3.dp))
            }
        }

        if (radar.isEmpty()) {
            item(span = { GridItemSpan(4) }) { Text("Nobody here yet.", Modifier.padding(Radar.sp3.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else if (shown.isEmpty()) {
            item(span = { GridItemSpan(4) }) { Text(if (filter == Filter.ATTENTION) "You're caught up." else "Turn on reminders for someone to see them here.", Modifier.padding(Radar.sp3.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(shown, key = { it.person.id }, span = { GridItemSpan(tileSpan(it)) }) { r -> FaceTile(r, Modifier.animateItem()) { onOpenPerson(r.person.id) } }
        }
    }
}

/** Partner / best friend / immediate family get the big tiles. */
private fun tileSpan(r: PersonRadar): Int = when (r.category?.sortOrder) { 0, 1, 3 -> 2; else -> 1 }
private fun rank(s: RadarStatus) = when (s) { RadarStatus.VERY_OVERDUE -> 6; RadarStatus.OVERDUE -> 5; RadarStatus.DUE_SOON -> 4; RadarStatus.GOOD -> 3; RadarStatus.SNOOZED -> 2; RadarStatus.PAUSED -> 1; RadarStatus.TRACK_ONLY -> 0 }

@Composable
private fun FaceTile(r: PersonRadar, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val big = tileSpan(r) == 2
    BoxWithConstraints(modifier.clickable(onClick = onClick)) {
        val side = maxWidth
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Face(
                r.person.id,
                r.person.displayName,
                r.status,
                r.person.avatar,
                r.person.contactLookupKey,
                size = if (big) (side.value * 0.85f).toInt() else side.value.toInt(),
                hiRes = big,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                r.person.displayName.substringBefore(' '),
                style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            if (r.status != RadarStatus.TRACK_ONLY) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(Modifier.size(6.dp).background(StatusColors.accent(r.status), CircleShape))
                    Text(
                        Format.ago(r.lastEffortAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            } else {
                Text(
                    Format.ago(r.lastEffortAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(Radar.sp2.dp))
        }
    }
}

@Composable
private fun Nudge(title: String, sub: String, onClick: () -> Unit) {
    Sheet(Modifier.clickable(onClick = onClick)) {
        ListItem(
            headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
            supportingContent = { Text(sub) },
            trailingContent = { Text("Sort", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

/** Still used by the quick-log search list. */
@Composable
fun PersonRow(r: PersonRadar, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = { Face(r.person.id, r.person.displayName, r.status, r.person.avatar, r.person.contactLookupKey, 48) },
        headlineContent = { Text(r.person.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(StatusColors.label(r.status) + (r.category?.let { " · ${it.name}" } ?: "")) },
        trailingContent = { Text(Format.ago(r.lastEffortAt), style = MaterialTheme.typography.labelLarge) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
