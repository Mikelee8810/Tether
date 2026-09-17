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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relationshipradar.app.data.db.ReminderBehavior
import com.relationshipradar.app.engine.PauseOption
import com.relationshipradar.app.engine.RadarStatus
import com.relationshipradar.app.engine.ReminderEngine
import com.relationshipradar.app.engine.SnoozeOption
import com.relationshipradar.app.ui.AmbientGlassCanvas
import com.relationshipradar.app.ui.Face
import com.relationshipradar.app.ui.Format
import com.relationshipradar.app.ui.Hairline
import com.relationshipradar.app.ui.Hint
import com.relationshipradar.app.ui.OptionDialog
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.SectionHeader
import com.relationshipradar.app.ui.SignalBars
import com.relationshipradar.app.ui.ToggleRow
import com.relationshipradar.app.ui.liquidGlass
import com.relationshipradar.app.ui.theme.StatusColors
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonSettingsScreen(
    vm: RadarViewModel,
    personId: Long,
    onBack: () -> Unit,
    onArchived: () -> Unit
) {
    val pwc by vm.person(personId).collectAsStateWithLifecycle(null)
    val interactions by vm.interactions(personId).collectAsStateWithLifecycle(emptyList())
    val person = pwc?.person ?: return
    val category = pwc?.category
    val radar = ReminderEngine.evaluate(
        person,
        category,
        interactions.firstOrNull { it.countsTowardTimer }?.timestamp
    )

    var dialog by remember { mutableStateOf<String?>(null) }
    var intervalText by remember(person.reminderIntervalDays) {
        mutableStateOf(person.reminderIntervalDays?.toString() ?: "")
    }
    var birthdayText by remember(person.birthday) { mutableStateOf(person.birthday ?: "") }
    var anniversaryText by remember(person.anniversary) { mutableStateOf(person.anniversary ?: "") }
    var whatsappText by remember(person.whatsappNumber) { mutableStateOf(person.whatsappNumber ?: "") }
    var messengerText by remember(person.messengerHandle) { mutableStateOf(person.messengerHandle ?: "") }
    var instagramText by remember(person.instagramHandle) { mutableStateOf(person.instagramHandle ?: "") }
    var snapchatText by remember(person.snapchatHandle) { mutableStateOf(person.snapchatHandle ?: "") }
    var discordText by remember(person.discordHandle) { mutableStateOf(person.discordHandle ?: "") }
    var meetText by remember(person.meetLink) { mutableStateOf(person.meetLink ?: "") }

    AmbientGlassCanvas {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        TopAppBar(
            title = {
                Text(
                    "Contact Preferences",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.5).sp
                )
            },
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
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ---- Person Mini Hero -----------------------------------------------------------
            item {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Face(
                            person.id,
                            person.displayName,
                            radar.status,
                            person.avatar,
                            person.contactLookupKey,
                            size = 56,
                            hiRes = true,
                            showRing = false,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                person.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                category?.name ?: "Uncategorized",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusColors.Cobalt,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        SignalBars(radar.status, maxHeight = 16.dp, barWidth = 4.dp, spacing = 2.dp)
                    }
                }
            }

            // ---- Reminders & Cadence --------------------------------------------------------
            item {
                SectionHeader("Reminders & Cadence")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(22.dp),
                            elevation = 8.dp,
                            surfaceAlphaTop = 0.85f,
                            surfaceAlphaBottom = 0.55f,
                        )
                ) {
                    Column {
                        ToggleRow(
                            "Keep a timeline",
                            "History builds even without reminders",
                            person.trackingEnabled
                        ) { vm.updatePerson(person.copy(trackingEnabled = it)) }

                        Hairline(Modifier.padding(horizontal = 16.dp))

                        ToggleRow(
                            "Remind me",
                            "Nudge when it's been too long",
                            person.remindersEnabled
                        ) { vm.updatePerson(person.copy(remindersEnabled = it)) }

                        if (person.remindersEnabled) {
                            Hairline(Modifier.padding(horizontal = 16.dp))
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Custom Interval",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Spacer(Modifier.height(6.dp))
                                val defInterval = category?.defaultIntervalDays
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = intervalText,
                                        onValueChange = { intervalText = it.filter(Char::isDigit).take(4) },
                                        label = { Text("Every N days") },
                                        placeholder = { Text(defInterval?.let { "$it (category default)" } ?: "custom") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                                            focusedContainerColor = Color.White.copy(alpha = 0.60f),
                                            unfocusedContainerColor = Color.White.copy(alpha = 0.35f),
                                            focusedIndicatorColor = StatusColors.Cobalt,
                                            unfocusedIndicatorColor = Color(0x250F172A),
                                        ),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .liquidGlass(
                                                shape = RoundedCornerShape(12.dp),
                                                elevation = 4.dp,
                                                surfaceAlphaTop = 0.90f,
                                                surfaceAlphaBottom = 0.70f,
                                            )
                                            .background(StatusColors.Cobalt, RoundedCornerShape(12.dp))
                                            .clickable {
                                                vm.updatePerson(
                                                    person.copy(reminderIntervalDays = intervalText.toIntOrNull())
                                                )
                                            }
                                            .padding(horizontal = 18.dp, vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Save", fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Notification Behavior",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Spacer(Modifier.height(8.dp))
                                val currentIdx = when (person.reminderBehavior) {
                                    null -> 0
                                    ReminderBehavior.INDIVIDUAL -> 1
                                    ReminderBehavior.ROUNDUP -> 2
                                }
                                val behaviorOptions = listOf("Category default", "Own alert", "Daily roundup")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    behaviorOptions.forEachIndexed { i, label ->
                                        com.relationshipradar.app.ui.SquircleChip(
                                            selected = i == currentIdx,
                                            label = label,
                                            onClick = {
                                                vm.updatePerson(
                                                    person.copy(
                                                        reminderBehavior = when (i) {
                                                            1 -> ReminderBehavior.INDIVIDUAL
                                                            2 -> ReminderBehavior.ROUNDUP
                                                            else -> null
                                                        }
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---- Snooze & Pause Status ------------------------------------------------------
            item {
                SectionHeader("Snooze & Pause")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(20.dp),
                            elevation = 6.dp,
                            surfaceAlphaTop = 0.82f,
                            surfaceAlphaBottom = 0.52f,
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val isSnoozed = (person.snoozedUntil ?: 0L) > System.currentTimeMillis()
                        val isPaused = (person.pausedUntil ?: 0L) > System.currentTimeMillis() || radar.status == RadarStatus.PAUSED

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Snooze Status",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (isSnoozed) "Snoozed ${Format.until(person.snoozedUntil!!)}" else "Not snoozed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSnoozed) StatusColors.Amber else Color(0xFF0F172A)
                                )
                            }
                            if (isSnoozed) {
                                TextButton(onClick = { vm.snooze(personId, null) }) {
                                    Text("Clear", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                TextButton(onClick = { dialog = "snooze" }) {
                                    Text("Snooze", color = StatusColors.Cobalt, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Hairline()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Pause Status",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (isPaused) "Paused ${person.pausedUntil?.let { Format.until(it) } ?: "indefinitely"}" else "Active",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPaused) Color(0xFF64748B) else StatusColors.Emerald
                                )
                            }
                            if (isPaused) {
                                TextButton(onClick = { vm.pause(personId, null) }) {
                                    Text("Unpause", color = StatusColors.Emerald, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                TextButton(onClick = { dialog = "pause" }) {
                                    Text("Pause", color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ---- Important Dates & Milestones ----------------------------------------------
            item {
                SectionHeader("Dates & Milestones")
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
                        Text(
                            "Celebrations & Reminders",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        OutlinedTextField(
                            value = birthdayText,
                            onValueChange = { birthdayText = it },
                            label = { Text("Birthday (e.g. 1992-05-14 or 05-14)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = anniversaryText,
                            onValueChange = { anniversaryText = it },
                            label = { Text("Anniversary (e.g. 2018-09-22 or 09-22)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (birthdayText != (person.birthday ?: "") || anniversaryText != (person.anniversary ?: "")) {
                            Button(
                                onClick = {
                                    vm.updatePerson(
                                        person.copy(
                                            birthday = birthdayText.trim().ifEmpty { null },
                                            anniversary = anniversaryText.trim().ifEmpty { null }
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = StatusColors.Cobalt),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Save Dates", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ---- Social Profiles & Handles --------------------------------------------------
            item {
                SectionHeader("1-Tap Social Reach Out")
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
                        Text(
                            "Connected Social Handles",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        OutlinedTextField(
                            value = whatsappText,
                            onValueChange = { whatsappText = it },
                            label = { Text("WhatsApp Phone / Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = messengerText,
                            onValueChange = { messengerText = it },
                            label = { Text("Facebook Messenger Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = instagramText,
                            onValueChange = { instagramText = it },
                            label = { Text("Instagram Handle (@username)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = snapchatText,
                            onValueChange = { snapchatText = it },
                            label = { Text("Snapchat Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = discordText,
                            onValueChange = { discordText = it },
                            label = { Text("Discord Username / User ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = meetText,
                            onValueChange = { meetText = it },
                            label = { Text("Google Meet Personal Link") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        val hasSocialChanges = whatsappText != (person.whatsappNumber ?: "") ||
                                messengerText != (person.messengerHandle ?: "") ||
                                instagramText != (person.instagramHandle ?: "") ||
                                snapchatText != (person.snapchatHandle ?: "") ||
                                discordText != (person.discordHandle ?: "") ||
                                meetText != (person.meetLink ?: "")

                        if (hasSocialChanges) {
                            Button(
                                onClick = {
                                    vm.updatePerson(
                                        person.copy(
                                            whatsappNumber = whatsappText.trim().ifEmpty { null },
                                            messengerHandle = messengerText.trim().ifEmpty { null },
                                            instagramHandle = instagramText.trim().removePrefix("@").ifEmpty { null },
                                            snapchatHandle = snapchatText.trim().ifEmpty { null },
                                            discordHandle = discordText.trim().ifEmpty { null },
                                            meetLink = meetText.trim().ifEmpty { null }
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = StatusColors.Cobalt),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Save Social Handles", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ---- Danger Zone (Archive) ------------------------------------------------------
            item {
                SectionHeader("Contact Management")
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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Archive Contact",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                        Text(
                            "Removes ${person.displayName} from your active circle and radar HUD. All moments, notes, and past history are safely preserved and can be restored at any time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = 2.dp,
                                    surfaceAlphaTop = 0.85f,
                                    surfaceAlphaBottom = 0.60f,
                                )
                                .background(Color(0xFFFEE2E2), RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(14.dp))
                                .clickable { dialog = "archive" }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Archive, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                Text("Archive ${person.displayName}", fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }
            }
        }
    }

    when (dialog) {
        "snooze" -> OptionDialog("Snooze reminders", SnoozeOption.entries.map { it.label }, { dialog = null }) { i ->
            val opt = SnoozeOption.entries[i]
            if (opt == SnoozeOption.CUSTOM) dialog = "snoozeDate" else { vm.snooze(personId, opt.resolve()); dialog = null }
        }
        "pause" -> OptionDialog("Pause reminders", PauseOption.entries.map { it.label }, { dialog = null }) { i ->
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
        "archive" -> AlertDialog(
            onDismissRequest = { dialog = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = {
                Text(
                    "Archive ${person.displayName}?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    "They leave your active circle. History stays. You can restore them from Settings → Archived.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.archive(personId)
                    dialog = null
                    onArchived()
                }) {
                    Text("Archive", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold) } },
        )
    }
}
}
