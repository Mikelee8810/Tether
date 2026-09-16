package com.relationshipradar.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relationshipradar.app.data.db.Interaction
import com.relationshipradar.app.data.db.ReminderBehavior
import com.relationshipradar.app.engine.PauseOption
import com.relationshipradar.app.engine.RadarStatus
import com.relationshipradar.app.engine.ReminderEngine
import com.relationshipradar.app.engine.SnoozeOption
import com.relationshipradar.app.ui.AvatarPicker
import com.relationshipradar.app.ui.Face
import com.relationshipradar.app.ui.Format
import com.relationshipradar.app.ui.Hairline
import com.relationshipradar.app.ui.Hint
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.SectionHeader
import com.relationshipradar.app.ui.Sheet
import com.relationshipradar.app.ui.StatusChip
import com.relationshipradar.app.ui.ToggleRow
import com.relationshipradar.app.ui.theme.Radar
import com.relationshipradar.app.ui.theme.StatusColors
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PersonScreen(vm: RadarViewModel, personId: Long, onLog: () -> Unit, onBack: () -> Unit) {
    val pwc by vm.person(personId).collectAsStateWithLifecycle(null)
    val interactions by vm.interactions(personId).collectAsStateWithLifecycle(emptyList())
    val identifiers by vm.identifiers(personId).collectAsStateWithLifecycle(emptyList())
    val categories by vm.categories.collectAsStateWithLifecycle()
    val person = pwc?.person ?: return
    val radar = ReminderEngine.evaluate(person, pwc?.category, interactions.firstOrNull { it.countsTowardTimer }?.timestamp)

    var dialog by remember { mutableStateOf<String?>(null) }
    var intervalText by remember(person.reminderIntervalDays) { mutableStateOf(person.reminderIntervalDays?.toString() ?: "") }
    var notes by remember(person.notes) { mutableStateOf(person.notes) }

    Column {
        TopAppBar(
            title = {},
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0), // Scaffold already applied the status bar inset
        )
        LazyColumn(contentPadding = PaddingValues(bottom = Radar.sp5.dp), modifier = Modifier.weight(1f)) {
            // ---- Hero: big shaped avatar wrapped in the interval ring ------------------------
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = Radar.sp4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val frac = if (radar.intervalDays == null || radar.daysSinceEffort == null) 0f else (radar.daysSinceEffort!!.toFloat() / radar.intervalDays!!).coerceIn(0f, 1f)
                    Box(contentAlignment = Alignment.Center) {
                        if (radar.status != RadarStatus.TRACK_ONLY) androidx.compose.material3.CircularProgressIndicator(
                            progress = { frac },
                            modifier = Modifier.size(140.dp),
                            color = StatusColors.accent(radar.status),
                            trackColor = StatusColors.container(radar.status),
                            strokeWidth = 6.dp,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        )
                        Face(
                            person.id, person.displayName, radar.status, person.avatar, person.contactLookupKey,
                            size = 114,
                            modifier = Modifier.clickable { dialog = "avatar" },
                            hiRes = true,
                            showRing = false,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { dialog = "avatar" }) { Text("Change face") }
                    Spacer(Modifier.height(Radar.sp3.dp))
                    Text(person.displayName, style = MaterialTheme.typography.headlineLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        radar.lastEffortAt?.let { "You reached out " + Format.ago(it).lowercase() } ?: "You haven't reached out yet",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(radar.status)
                        radar.dueAt?.let { if (radar.status != RadarStatus.TRACK_ONLY) Hint("· due " + Format.date(it)) }
                    }
                    person.snoozedUntil?.takeIf { it > System.currentTimeMillis() }?.let { Hint("Snoozed " + Format.until(it)) }
                    person.pausedUntil?.takeIf { it > System.currentTimeMillis() }?.let { Hint("Paused " + Format.until(it)) }
                    Spacer(Modifier.height(Radar.sp4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                        Button(onClick = onLog, shape = ButtonGroupDefaults.connectedLeadingButtonShape) { Icon(Icons.Rounded.Edit, null); Spacer(Modifier.size(8.dp)); Text("Log a moment") }
                        if (radar.status != RadarStatus.TRACK_ONLY) {
                            FilledTonalButton(onClick = { dialog = "snooze" }, shape = MaterialTheme.shapes.small) { Icon(Icons.Rounded.Snooze, "Snooze") }
                            FilledTonalButton(onClick = { dialog = "pause" }, shape = ButtonGroupDefaults.connectedTrailingButtonShape) { Icon(Icons.Rounded.Pause, if (radar.status == RadarStatus.PAUSED) "Unpause" else "Pause") }
                        }
                    }
                }
                Spacer(Modifier.height(Radar.sp4.dp))
            }

            item { SectionHeader("Who they are to you") }
            item {
                Column(Modifier.padding(horizontal = Radar.sp3.dp)) {
                    FlowChips(
                        listOf("None") + categories.map { it.name },
                        if (person.categoryId == null) 0 else categories.indexOfFirst { it.id == person.categoryId } + 1,
                    ) { i -> vm.setCategory(personId, if (i == 0) null else categories[i - 1].id) }
                }
            }

            item { SectionHeader("Reminders", Modifier.padding(top = Radar.sp3.dp)) }
            item {
                Sheet(Modifier.padding(horizontal = Radar.sp3.dp)) {
                    ToggleRow("Keep a timeline", "History builds even without reminders", person.trackingEnabled) { vm.updatePerson(person.copy(trackingEnabled = it)) }
                    ToggleRow("Remind me", "When it's been too long", person.remindersEnabled) { vm.updatePerson(person.copy(remindersEnabled = it)) }
                    if (person.remindersEnabled) {
                        Column(Modifier.padding(horizontal = Radar.sp3.dp, vertical = Radar.sp2.dp)) {
                            val def = pwc?.category?.defaultIntervalDays
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    intervalText, { intervalText = it.filter(Char::isDigit).take(4) },
                                    label = { Text("Every N days") }, placeholder = { Text(def?.let { "$it (category default)" } ?: "no default") },
                                    singleLine = true, modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { vm.updatePerson(person.copy(reminderIntervalDays = intervalText.toIntOrNull())) }) { Text("Save") }
                            }
                            Spacer(Modifier.height(Radar.sp2.dp))
                            FlowChips(
                                listOf("Category default", "Own alert", "Daily roundup"),
                                when (person.reminderBehavior) { null -> 0; ReminderBehavior.INDIVIDUAL -> 1; ReminderBehavior.ROUNDUP -> 2 },
                            ) { i -> vm.updatePerson(person.copy(reminderBehavior = when (i) { 1 -> ReminderBehavior.INDIVIDUAL; 2 -> ReminderBehavior.ROUNDUP; else -> null })) }
                            Spacer(Modifier.height(Radar.sp2.dp))
                        }
                    }
                }
            }

            item { SectionHeader("Notes", Modifier.padding(top = Radar.sp3.dp)) }
            item {
                Column(Modifier.padding(horizontal = Radar.sp3.dp)) {
                    OutlinedTextField(notes, { notes = it }, modifier = Modifier.fillMaxWidth(), minLines = 2, placeholder = { Text("Kids' names, what they're going through, gift ideas") })
                    if (notes != person.notes) TextButton(onClick = { vm.updatePerson(person.copy(notes = notes)) }) { Text("Save notes") }
                }
            }

            if (identifiers.isNotEmpty()) {
                item { SectionHeader("Known as", Modifier.padding(top = Radar.sp3.dp)) }
                items(identifiers, key = { it.id }) { Hint("${it.type.name.lowercase().replace('_', ' ')}: ${it.rawValue}", Modifier.padding(horizontal = Radar.sp4.dp, vertical = 2.dp)) }
            }

            item { SectionHeader("Timeline · ${interactions.size}", Modifier.padding(top = Radar.sp3.dp)) }
            if (interactions.isEmpty()) item { Hint("Nothing yet. “Log a moment” adds the first one.", Modifier.padding(horizontal = Radar.sp4.dp)) }
            items(interactions, key = { it.id }) { i -> TimelineRow(i) { vm.deleteInteraction(i) } }

            item {
                Spacer(Modifier.height(Radar.sp4.dp))
                TextButton(onClick = { dialog = "archive" }, Modifier.padding(horizontal = Radar.sp3.dp)) { Text("Archive ${person.displayName}") }
                Hint("Hides them from the circle. Every moment and note stays.", Modifier.padding(horizontal = Radar.sp4.dp))
            }
        }
    }

    when (dialog) {
        "snooze" -> OptionDialog("Snooze reminders", SnoozeOption.entries.map { it.label }, { dialog = null }) { i ->
            val opt = SnoozeOption.entries[i]
            if (opt == SnoozeOption.CUSTOM) dialog = "snoozeDate" else { vm.snooze(personId, opt.resolve()); dialog = null }
        }
        "pause" -> if (radar.status == RadarStatus.PAUSED) { vm.pause(personId, null); dialog = null }
        else OptionDialog("Pause reminders", PauseOption.entries.map { it.label }, { dialog = null }) { i ->
            val opt = PauseOption.entries[i]
            if (opt == PauseOption.UNTIL_DATE) dialog = "pauseDate" else { vm.pause(personId, opt.resolve()); dialog = null }
        }
        "snoozeDate", "pauseDate" -> {
            val state = rememberDatePickerState()
            DatePickerDialog(onDismissRequest = { dialog = null }, confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { utc ->
                        val day = java.time.Instant.ofEpochMilli(utc).atZone(ZoneId.of("UTC")).toLocalDate()
                        val local = day.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        if (dialog == "snoozeDate") vm.snooze(personId, local) else vm.pause(personId, local)
                    }
                    dialog = null
                }) { Text("OK") }
            }) { DatePicker(state) }
        }
        "avatar" -> AvatarPicker(person.avatar, person.contactLookupKey != null, onPick = { vm.updatePerson(person.copy(avatar = it)); dialog = null }, onDismiss = { dialog = null })
        "archive" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Archive ${person.displayName}?") },
            text = { Text("They leave the circle. History stays. Restore from Settings → Archived.") },
            confirmButton = { TextButton(onClick = { vm.archive(personId); dialog = null; onBack() }) { Text("Archive") } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TimelineRow(i: Interaction, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(i.type.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase) + if (i.note.isNotBlank()) " · ${i.note}" else "") },
        supportingContent = { Text((if (i.approximate) "around " else "") + Format.dateTime(i.timestamp) + " · " + i.source.name.lowercase() + if (!i.countsTowardTimer) " · doesn't count" else "") },
        trailingContent = { IconButton(onClick = onDelete) { Icon(Icons.Rounded.Close, "Delete") } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.padding(horizontal = Radar.sp2.dp),
    )
}

@Composable
fun OptionDialog(title: String, options: List<String>, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEachIndexed { i, o ->
                    Text(o, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().clickable { onPick(i) }.padding(vertical = 14.dp))
                    if (i < options.lastIndex) Hairline()
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
