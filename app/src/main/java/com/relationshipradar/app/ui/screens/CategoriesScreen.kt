package com.relationshipradar.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relationshipradar.app.data.db.Category
import com.relationshipradar.app.data.db.ReminderBehavior
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.liquidGlass
import com.relationshipradar.app.ui.theme.StatusColors

@Composable
fun CategoriesScreen(vm: RadarViewModel, showAdd: Boolean, onAddConsumed: () -> Unit) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Category?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Add Category Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${categories.size} Relationship Tiers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )

            Box(
                modifier = Modifier
                    .liquidGlass(
                        shape = RoundedCornerShape(12.dp),
                        elevation = 2.dp,
                        surfaceAlphaTop = 0.90f,
                        surfaceAlphaBottom = 0.60f
                    )
                    .clickable { editing = Category(name = "", defaultIntervalDays = 30) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add Tier",
                        tint = StatusColors.Cobalt,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "New Tier",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusColors.Cobalt
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categories, key = { it.id }) { c ->
                val cardShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = cardShape,
                            elevation = 4.dp,
                            surfaceAlphaTop = 0.85f,
                            surfaceAlphaBottom = 0.55f,
                        )
                        .clickable { editing = c }
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = c.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A),
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val intervalText = c.defaultIntervalDays?.let { "Every $it days" } ?: "Custom interval"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StatusColors.Cobalt.copy(alpha = 0.10f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = intervalText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusColors.Cobalt
                                    )
                                }

                                val behaviorText = if (c.reminderBehavior == ReminderBehavior.INDIVIDUAL) "Own alert" else "Daily roundup"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StatusColors.Emerald.copy(alpha = 0.10f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = behaviorText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusColors.Emerald
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A).copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Edit",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) editing = Category(name = "", defaultIntervalDays = 30).also { onAddConsumed() }

    editing?.let { c ->
        var name by remember(c) { mutableStateOf(c.name) }
        var days by remember(c) { mutableStateOf(c.defaultIntervalDays?.toString() ?: "") }
        var behavior by remember(c) { mutableStateOf(c.reminderBehavior) }

        AlertDialog(
            onDismissRequest = { editing = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = if (c.id == 0L) "New Relationship Tier" else "Edit Tier",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tier Name") },
                        singleLine = true,
                        enabled = !c.builtIn,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = days,
                        onValueChange = { days = it.filter(Char::isDigit).take(4) },
                        label = { Text("Default cadence: Every N days") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = behavior == ReminderBehavior.INDIVIDUAL,
                            onClick = { behavior = ReminderBehavior.INDIVIDUAL },
                            label = { Text("Own alert", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusColors.Cobalt.copy(alpha = 0.15f),
                                selectedLabelColor = StatusColors.Cobalt
                            )
                        )
                        FilterChip(
                            selected = behavior == ReminderBehavior.ROUNDUP,
                            onClick = { behavior = ReminderBehavior.ROUNDUP },
                            label = { Text("Roundup", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusColors.Cobalt.copy(alpha = 0.15f),
                                selectedLabelColor = StatusColors.Cobalt
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        val updated = c.copy(
                            name = name.trim(),
                            defaultIntervalDays = days.toIntOrNull(),
                            reminderBehavior = behavior,
                            sortOrder = if (c.id == 0L) 100 else c.sortOrder
                        )
                        if (c.id == 0L) vm.addCategory(updated) else vm.updateCategory(updated)
                        editing = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusColors.Cobalt)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    if (!c.builtIn && c.id != 0L) {
                        TextButton(onClick = {
                            vm.deleteCategory(c)
                            editing = null
                        }) {
                            Text("Delete", color = StatusColors.Flame, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { editing = null }) {
                        Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    }
                }
            },
        )
    }
}

