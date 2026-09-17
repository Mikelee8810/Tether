package com.relationshipradar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relationshipradar.app.data.db.InteractionType
import com.relationshipradar.app.ui.Format
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.liquidGlass
import com.relationshipradar.app.ui.theme.StatusColors
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Rough dates: the whole point is that "sometime last week" is good enough. */
private enum class When(val label: String, val daysAgo: Long?, val approx: Boolean) {
    NOW("Just now", 0, false),
    YESTERDAY("Yesterday", 1, false),
    FEW_DAYS("A few days ago", 3, true),
    LAST_WEEK("Last week", 7, true),
    TWO_WEEKS("Two weeks ago", 14, true),
    LAST_MONTH("Last month", 30, true),
    PICK("Pick a date", null, false),
}

private val quickTypes = listOf(
    InteractionType.IN_PERSON to "Saw them",
    InteractionType.CALL to "Called",
    InteractionType.MESSAGE to "Messaged",
    InteractionType.VIDEO_CALL to "Video call",
    InteractionType.EMAIL to "Emailed",
    InteractionType.OTHER to "Other",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogScreen(vm: RadarViewModel, preselectedPersonId: Long?, onDone: () -> Unit) {
    val radar by vm.radar.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var personId by rememberSaveable { mutableStateOf(preselectedPersonId) }
    var type by rememberSaveable { mutableStateOf(InteractionType.IN_PERSON) }
    var whenChoice by rememberSaveable { mutableStateOf(When.NOW) }
    var customDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var celebrating by remember { mutableStateOf(false) }

    val selected = radar.firstOrNull { it.person.id == personId }

    Column(Modifier.padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Who?", style = MaterialTheme.typography.titleLarge)
        if (selected == null) {
            OutlinedTextField(query, { query = it }, label = { Text("Search people") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            val matches = radar.filter { query.isBlank() || it.person.displayName.contains(query, ignoreCase = true) }.take(12)
            if (matches.isEmpty()) {
                Text("No one matches. Add them from the + button first.", style = MaterialTheme.typography.bodySmall)
            }
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = 4) {
                matches.forEach { r ->
                    Column(Modifier.width(72.dp).clickable { personId = r.person.id }, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        com.relationshipradar.app.ui.Face(r.person.id, r.person.displayName, r.status, r.person.avatar, r.person.contactLookupKey, 64)
                        Text(r.person.displayName.substringBefore(' '), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        } else {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.relationshipradar.app.ui.Face(selected.person.id, selected.person.displayName, selected.status, selected.person.avatar, selected.person.contactLookupKey, 56)
                Text(selected.person.displayName, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                if (preselectedPersonId == null) TextButton(onClick = { personId = null }) { Text("Change") }
            }
        }

        Text("What?", style = MaterialTheme.typography.titleLarge)
        FlowChips(quickTypes.map { it.second }, quickTypes.indexOfFirst { it.first == type }) { type = quickTypes[it].first }

        Text("When?", style = MaterialTheme.typography.titleLarge)
        FlowChips(When.entries.map { it.label }, whenChoice.ordinal) {
            whenChoice = When.entries[it]
            if (whenChoice == When.PICK) showPicker = true
        }
        if (whenChoice == When.PICK && customDate != null) Text(Format.date(customDate!!), style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(8.dp))
        val canSave = selected != null && (whenChoice != When.PICK || customDate != null)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(16.dp),
                    elevation = if (canSave) 6.dp else 0.dp,
                    surfaceAlphaTop = if (canSave) 0.95f else 0.40f,
                    surfaceAlphaBottom = if (canSave) 0.70f else 0.20f,
                )
                .background(
                    if (canSave) StatusColors.Cobalt else Color(0xFFCBD5E1),
                    RoundedCornerShape(16.dp)
                )
                .clickable(enabled = canSave) {
                    val ts = when (whenChoice) {
                        When.PICK -> customDate!!
                        else -> System.currentTimeMillis() - TimeUnit.DAYS.toMillis(whenChoice.daysAgo!!)
                    }
                    vm.logManual(selected!!.person.id, type, ts, whenChoice.approx, note.trim())
                    celebrating = true
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Save Moment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }

    if (celebrating) com.relationshipradar.app.ui.Celebrate(onDone)

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = customDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { utc ->
                        val day = java.time.Instant.ofEpochMilli(utc).atZone(ZoneId.of("UTC")).toLocalDate()
                        customDate = day.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    showPicker = false
                }) { Text("OK") }
            },
        ) { DatePicker(state) }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FlowChips(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { i, l ->
            com.relationshipradar.app.ui.SquircleChip(
                selected = i == selectedIndex,
                label = l,
                onClick = { onSelect(i) }
            )
        }
    }
}

