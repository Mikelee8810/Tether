package com.relationshipradar.app.connectors.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.relationshipradar.app.RadarApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallInsightPersistenceProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = RadarApp.from(context).repo
                val person = repo.suggestPersonByName("Marcus")
                val personId = person?.id ?: repo.createPerson("Marcus")
                val candidate = TranscriptInsightExtractor().extract(
                    listOf(TranscriptSegment("Marcus has an interview on Friday. Remember to ask how it went next week.", 742_000, 755_000, .91f)),
                ).first { it.type == InsightType.FOLLOW_UP }
                val store = CallInsightReviewStore(repo)
                val first = store.accept(personId, "probe-recording-001", candidate, "Ask how the interview went")
                val duplicate = store.accept(personId, "probe-recording-001", candidate, "Duplicate must not save")
                val rows = repo.callInsightsForPerson(personId)
                Log.i("InsightStoreProbe", "success first=$first duplicate=$duplicate rows=${rows.size} text=${rows.firstOrNull()?.text} scheduled=${rows.firstOrNull()?.scheduledAt != null}")
            } catch (error: Throwable) {
                Log.e("InsightStoreProbe", "failure", error)
            } finally {
                pending.finish()
            }
        }
    }
}
