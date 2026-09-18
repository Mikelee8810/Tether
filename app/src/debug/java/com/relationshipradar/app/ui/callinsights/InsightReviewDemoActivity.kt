package com.relationshipradar.app.ui.callinsights

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.relationshipradar.app.RadarApp
import com.relationshipradar.app.connectors.callrecorder.TranscriptInsightExtractor
import com.relationshipradar.app.connectors.callrecorder.TranscriptSegment
import com.relationshipradar.app.ui.AmbientGlassCanvas
import com.relationshipradar.app.ui.theme.RadarTheme
import kotlinx.coroutines.launch

class InsightReviewDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val candidates = TranscriptInsightExtractor().extract(
            listOf(
                TranscriptSegment(
                    "Marcus has an interview on Friday. Remember to ask how it went next week.",
                    742_000,
                    755_000,
                    0.91f,
                )
            )
        )
        lifecycleScope.launch {
            val repo = RadarApp.from(this@InsightReviewDemoActivity).repo
            val personId = repo.suggestPersonByName("Marcus")?.id ?: repo.createPerson("Marcus")
            setContent {
                RadarTheme {
                    AmbientGlassCanvas {
                        PersistedInsightReviewSheet(
                            personId = personId,
                            personName = "Marcus",
                            sourceRecordingId = "demo-call-002",
                            callDurationLabel = "24 min call",
                            candidates = candidates,
                            onPlayFrom = {},
                        )
                    }
                }
            }
        }
    }
}
