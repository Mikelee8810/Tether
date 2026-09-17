package com.relationshipradar.app.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relationshipradar.app.data.db.IdentifierType
import com.relationshipradar.app.data.db.PendingIdentity
import com.relationshipradar.app.ui.Format
import com.relationshipradar.app.ui.Hairline
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.liquidGlass
import com.relationshipradar.app.ui.theme.StatusColors

/**
 * Uncertain identity matches.
 * The rule: ask once, remember the answer, never silently merge.
 * Designed with Apple Liquid Glass aesthetic and zero harsh boundaries.
 */
@Composable
fun WhoIsThisScreen(vm: RadarViewModel) {
    val pending by vm.pendingIdentities.collectAsStateWithLifecycle()
    val radar by vm.radar.collectAsStateWithLifecycle()
    var linking by remember { mutableStateOf<PendingIdentity?>(null) }

    if (pending.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(28.dp),
                        elevation = 8.dp,
                        surfaceAlphaTop = 0.88f,
                        surfaceAlphaBottom = 0.58f,
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        StatusColors.Emerald.copy(alpha = 0.20f),
                                        StatusColors.Emerald.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.PersonSearch,
                            contentDescription = null,
                            tint = StatusColors.Emerald,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "Identities Resolved",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.5).sp
                    )

                    Text(
                        text = "When an unfamiliar phone number, chat handle, or email reaches out, Relationship Radar prompts you here to link them safely.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(pending, key = { it.id }) { p ->
            val suggested = radar.firstOrNull { it.person.id == p.suggestedPersonId }
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
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A).copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.HelpOutline,
                                contentDescription = null,
                                tint = StatusColors.Cobalt,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = display(p),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A),
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${p.source} · seen ${p.seenCount}× · last ${Format.ago(p.lastSeenAt).lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (suggested != null) {
                        Button(
                            onClick = { vm.resolvePending(p, suggested.person.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusColors.Cobalt),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("This is ${suggested.person.displayName}", fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { vm.ignorePending(p) }) {
                            Text("Ignore", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.size(4.dp))
                        TextButton(onClick = { vm.resolvePendingAsNew(p, defaultName(p)) }) {
                            Text("New person", color = StatusColors.Cobalt, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.size(4.dp))
                        Button(
                            onClick = { linking = p },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F172A).copy(alpha = 0.08f),
                                contentColor = Color(0xFF0F172A)
                            )
                        ) {
                            Text("Link person", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }

    linking?.let { p ->
        var query by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { linking = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Who is ${display(p)}?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search circle") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val matches = radar.filter { query.isBlank() || it.person.displayName.contains(query, true) }.take(6)
                    LazyColumn(
                        modifier = Modifier.height((matches.size * 56).coerceAtMost(280).dp)
                    ) {
                        items(matches, key = { it.person.id }) { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.resolvePending(p, r.person.id)
                                        linking = null
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = r.person.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "Link →",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusColors.Cobalt
                                )
                            }
                            Hairline()
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { linking = null }) {
                    Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

private fun display(p: PendingIdentity) = when (p.type) {
    IdentifierType.HANDLE -> p.rawValue.substringAfter(':')
    else -> p.rawValue
}

private fun defaultName(p: PendingIdentity) = if (p.type == IdentifierType.HANDLE) display(p) else "Unknown (${p.rawValue})"

