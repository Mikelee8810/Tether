package com.relationshipradar.app.ui.callinsights

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.relationshipradar.app.RadarApp
import com.relationshipradar.app.connectors.callrecorder.CallInsightReviewStore
import com.relationshipradar.app.connectors.callrecorder.InsightCandidate
import kotlinx.coroutines.launch

/** Production route: review gestures are persisted before a confirmation is shown. */
@Composable
fun PersistedInsightReviewSheet(
    personId: Long,
    personName: String,
    sourceRecordingId: String,
    callDurationLabel: String,
    candidates: List<InsightCandidate>,
    onPlayFrom: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val store = remember { CallInsightReviewStore(RadarApp.from(context).repo) }
    val scope = rememberCoroutineScope()

    InsightReviewSheet(
        personName = personName,
        callDurationLabel = callDurationLabel,
        candidates = candidates,
        onPlayFrom = onPlayFrom,
        onAccept = { candidate, editedText ->
            scope.launch {
                val saved = store.accept(personId, sourceRecordingId, candidate, editedText)
                Toast.makeText(context, if (saved) "Saved to $personName" else "Already reviewed", Toast.LENGTH_SHORT).show()
            }
        },
        onDismiss = { candidate ->
            scope.launch { store.dismiss(personId, sourceRecordingId, candidate) }
        },
        modifier = modifier,
    )
}
