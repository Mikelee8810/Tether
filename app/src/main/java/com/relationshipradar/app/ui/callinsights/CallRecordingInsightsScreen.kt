package com.relationshipradar.app.ui.callinsights

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relationshipradar.app.RadarApp
import com.relationshipradar.app.connectors.callrecorder.CallInsightSession
import com.relationshipradar.app.connectors.callrecorder.CallRecordingInsightPipeline
import com.relationshipradar.app.connectors.callrecorder.RecordingMatch
import com.relationshipradar.app.connectors.callrecorder.WhisperModelManager
import com.relationshipradar.app.connectors.callrecorder.WhisperModelStatus
import com.relationshipradar.app.ui.Format
import com.relationshipradar.app.ui.RadarViewModel
import com.relationshipradar.app.ui.liquidGlass
import com.relationshipradar.app.ui.theme.EditorialSerif
import com.relationshipradar.app.ui.theme.StatusColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun CallRecordingInsightsScreen(vm: RadarViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pipeline = remember { CallRecordingInsightPipeline(context, RadarApp.from(context).repo) }
    val people by vm.radar.collectAsStateWithLifecycle()
    var recordings by remember { mutableStateOf<List<RecordingMatch>?>(null) }
    var choosing by remember { mutableStateOf<RecordingMatch?>(null) }
    var working by remember { mutableStateOf<RecordingMatch?>(null) }
    var session by remember { mutableStateOf<CallInsightSession?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() { scope.launch { recordings = runCatching { pipeline.recordings() }.onFailure { error = it.message }.getOrDefault(emptyList()) } }
    fun analyze(match: RecordingMatch, personId: Long) {
        working = match; error = null
        scope.launch {
            runCatching { pipeline.analyze(match.recording, personId) }
                .onSuccess { session = it }
                .onFailure { error = it.message ?: "Could not analyze this recording" }
            working = null
        }
    }
    LaunchedEffect(Unit) { refresh() }

    session?.let { result ->
        PersistedInsightReviewSheet(
            personId = result.person.id,
            personName = result.person.displayName,
            sourceRecordingId = result.recording.id,
            callDurationLabel = result.recording.durationMillis?.let(::durationLabel) ?: "recorded call",
            candidates = result.candidates,
            onPlayFrom = {},
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 28.dp, 18.dp, 160.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Calls worth keeping", fontFamily = EditorialSerif, fontSize = 36.sp, color = Color(0xFF0F172A))
                Text("Choose a recording. Audio stays on this phone and suggestions require your approval.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
                error?.let { Text(it, color = StatusColors.Flame, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
            }
        }
        item { WhisperModelInstallCard() }
        when (val rows = recordings) {
            null -> item { Text("Checking Call Recorder…", color = Color(0xFF64748B)) }
            else -> if (rows.isEmpty()) item {
                EmptyRecordingsCard(onRefresh = ::refresh)
            } else items(rows, key = { it.recording.id }) { match ->
                RecordingCard(match, working?.recording?.id == match.recording.id) {
                    match.person?.let { analyze(match, it.id) } ?: run { choosing = match }
                }
            }
        }
    }

    choosing?.let { match ->
        AlertDialog(
            onDismissRequest = { choosing = null },
            title = { Text("Who was this call with?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tether couldn’t confidently match ${match.recording.phoneNumber ?: "this recording"}. Choose explicitly.", style = MaterialTheme.typography.bodySmall)
                    people.forEach { radar ->
                        Text(
                            radar.person.displayName,
                            modifier = Modifier.fillMaxWidth().clickable { choosing = null; analyze(match, radar.person.id) }.padding(vertical = 10.dp),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choosing = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun WhisperModelInstallCard() {
    val context = LocalContext.current
    val manager = remember { WhisperModelManager(context) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<WhisperModelStatus?>(null) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() { status = manager.status() }
    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(20.dp), elevation = 6.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Offline transcription", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(
                    when {
                        status == null -> "Checking installed model…"
                        progress != null -> "Downloading securely… ${progress}%"
                        status?.valid == true -> "Ready · verified tiny.en model"
                        status?.installed == true -> "Installed file failed verification"
                        else -> "Optional 74 MB download · audio never leaves your phone"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status?.valid == true) StatusColors.Emerald else Color(0xFF64748B),
                )
            }
            Text(if (status?.valid == true) "✓" else "↓", fontSize = 24.sp, color = StatusColors.Cobalt, fontWeight = FontWeight.Black)
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = StatusColors.Flame, fontWeight = FontWeight.Bold) }
        if (status?.valid == true) {
            TextButton(onClick = { manager.remove(); scope.launch { refresh() } }) { Text("Remove model") }
        } else {
            Button(
                onClick = {
                    message = null
                    val request = manager.install()
                    progress = 0
                    scope.launch {
                        while (true) {
                            val info = androidx.work.WorkManager.getInstance(context).getWorkInfoById(request.id).get()
                            if (info == null) {
                                delay(350)
                                continue
                            }
                            progress = info.progress.getInt("percent", progress ?: 0)
                            if (info.state.isFinished) {
                                progress = null
                                refresh()
                                if (!info.state.name.equals("SUCCEEDED")) message = info.outputData.getString("error") ?: "Download failed. Try again."
                                break
                            }
                            delay(350)
                        }
                    }
                },
                enabled = progress == null && status != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (status == null) "Checking…" else if (progress == null) "Install offline transcription" else "Installing…") }
        }
    }
}

@Composable
private fun RecordingCard(match: RecordingMatch, working: Boolean, onAnalyze: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(20.dp), elevation = 6.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(match.person?.displayName ?: match.recording.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(Format.date(match.recording.startedAtMillis), style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
            }
            Text(match.recording.durationMillis?.let(::durationLabel) ?: "Call", color = StatusColors.Cobalt, fontWeight = FontWeight.Bold)
        }
        Text(
            if (match.person != null) "Matched by phone number" else "Needs your confirmation",
            style = MaterialTheme.typography.labelSmall,
            color = if (match.person != null) StatusColors.Emerald else StatusColors.Amber,
            fontWeight = FontWeight.Bold,
        )
        Button(onClick = onAnalyze, enabled = !working, modifier = Modifier.fillMaxWidth()) {
            Text(if (working) "Transcribing on-device…" else if (match.person == null) "Choose person & transcribe" else "Transcribe & find insights")
        }
    }
}

@Composable
private fun EmptyRecordingsCard(onRefresh: () -> Unit) {
    Box(Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(20.dp), elevation = 6.dp).padding(22.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("No finished recordings yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Recordings will appear here automatically after Call Recorder finishes a call.", color = Color(0xFF64748B))
            TextButton(onClick = onRefresh) { Text("Check again") }
        }
    }
}

private fun durationLabel(millis: Long): String = "${millis / 60_000}:${((millis / 1_000) % 60).toString().padStart(2, '0')}"
