package com.relationshipradar.app.ui.callinsights

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relationshipradar.app.connectors.callrecorder.InsightCandidate
import com.relationshipradar.app.connectors.callrecorder.InsightType
import com.relationshipradar.app.connectors.callrecorder.formatTranscriptTimestamp
import com.relationshipradar.app.ui.liquidGlass
import com.relationshipradar.app.ui.theme.EditorialSerif
import com.relationshipradar.app.ui.theme.StatusColors

@Composable
fun InsightReviewSheet(
    personName: String,
    callDurationLabel: String,
    candidates: List<InsightCandidate>,
    onPlayFrom: (Long) -> Unit,
    onAccept: (InsightCandidate, String) -> Unit,
    onDismiss: (InsightCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolved = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 72.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Worth remembering?", fontFamily = EditorialSerif, fontSize = 38.sp, color = Color(0xFF0F172A))
                Text(
                    "$personName · $callDurationLabel · ${candidates.size} suggestions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Tether found possibilities, not facts. You decide what stays.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                )
            }
        }

        items(candidates, key = { it.id }) { candidate ->
            AnimatedVisibility(visible = resolved[candidate.id] != true) {
                InsightCandidateCard(
                    candidate = candidate,
                    onPlayFrom = onPlayFrom,
                    onAccept = { edited ->
                        onAccept(candidate, edited)
                        resolved[candidate.id] = true
                    },
                    onDismiss = {
                        onDismiss(candidate)
                        resolved[candidate.id] = true
                    },
                )
            }
        }
    }
}

@Composable
private fun InsightCandidateCard(
    candidate: InsightCandidate,
    onPlayFrom: (Long) -> Unit,
    onAccept: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var editedText by remember(candidate.id) { mutableStateOf(candidate.text) }
    val tint = when (candidate.type) {
        InsightType.FOLLOW_UP -> Color(0xFFEFF6FF)
        InsightType.UPCOMING_DATE -> Color(0xFFFFF7ED)
        InsightType.MEMORY -> Color(0xFFF5F3FF)
        InsightType.LITTLE_THING -> Color(0xFFFDF2F8)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(24.dp),
                elevation = 5.dp,
                surfaceAlphaTop = 0.90f,
                surfaceAlphaBottom = 0.62f,
                tintColor = tint,
            )
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(candidate.type.icon, fontWeight = FontWeight.Black, color = StatusColors.Cobalt) }
                    Text(candidate.type.label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color(0xFF475569), letterSpacing = 0.7.sp)
                }
                candidate.suggestedDate?.let {
                    Text(it.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = StatusColors.Flame)
                }
            }

            if (editing) {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                )
            } else {
                Text(editedText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
            }

            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.58f)).padding(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("FROM THE CALL", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF94A3B8), letterSpacing = 0.7.sp)
                    Text("“${candidate.sourceExcerpt}”", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569), lineHeight = 17.sp)
                    Text(
                        "▶ Play from ${formatTranscriptTimestamp(candidate.sourceStartMillis)}",
                        modifier = Modifier.clickable { onPlayFrom(candidate.sourceStartMillis) }.padding(vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusColors.Cobalt,
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewAction(Icons.Rounded.Close, "Dismiss", Color(0xFF64748B), Modifier.weight(1f), onDismiss)
                ReviewAction(
                    Icons.Rounded.Edit,
                    if (editing) "Cancel" else "Edit",
                    StatusColors.Cobalt,
                    Modifier.weight(1f),
                    onClick = { editing = !editing },
                )
                ReviewAction(Icons.Rounded.Check, "Accept", Color.White, Modifier.weight(1f), { onAccept(editedText.trim()) }, filled = true)
            }
        }
    }
}

@Composable
private fun ReviewAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    filled: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (filled) Modifier.background(StatusColors.Cobalt) else Modifier.background(Color.White.copy(alpha = 0.62f)).border(0.7.dp, Color.White, RoundedCornerShape(12.dp)))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.size(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
    }
}
