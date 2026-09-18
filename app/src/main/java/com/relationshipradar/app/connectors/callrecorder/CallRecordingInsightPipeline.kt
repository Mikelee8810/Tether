package com.relationshipradar.app.connectors.callrecorder

import android.content.Context
import com.relationshipradar.app.data.db.IdentifierType
import com.relationshipradar.app.data.db.Person
import com.relationshipradar.app.data.repo.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RecordingMatch(val recording: CallRecording, val person: Person?)

data class CallInsightSession(
    val recording: CallRecording,
    val person: Person,
    val transcript: TranscriptResult,
    val candidates: List<InsightCandidate>,
)

class CallRecordingInsightPipeline(
    private val context: Context,
    private val repository: Repository,
    private val bridge: CallRecorderBridge = CallRecorderBridge(context.contentResolver),
) {
    suspend fun recordings(): List<RecordingMatch> = bridge.recordings()
        .sortedByDescending { it.startedAtMillis }
        .map { recording ->
            RecordingMatch(
                recording,
                recording.phoneNumber?.takeIf(String::isNotBlank)?.let {
                    repository.findPersonByIdentifier(IdentifierType.PHONE, it)
                },
            )
        }

    suspend fun analyze(recording: CallRecording, personId: Long): CallInsightSession {
        val person = requireNotNull(repository.getPerson(personId)) { "Choose a person for this call" }
        val model = WhisperModelManager(context).modelFile
        require(model.isFile) { "Offline transcription model is not installed" }
        val pcm = withContext(Dispatchers.IO) { AudioPcmDecoder(context).decodeTo16kMono(recording.contentUri) }
        val transcript = withContext(Dispatchers.Default) { LocalWhisperTranscriber().transcribe(pcm, model) }
        val segments = WhisperTimestampParser.parse(transcript.text).ifEmpty {
            listOf(TranscriptSegment(transcript.text, 0, transcript.audioDurationMillis))
        }
        return CallInsightSession(recording, person, transcript, TranscriptInsightExtractor().extract(segments))
    }
}

object WhisperTimestampParser {
    private val line = Regex("\\[(\\d{2}):(\\d{2}):(\\d{2})[.,](\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2})[.,](\\d{3})]:?\\s*(.+)")

    fun parse(text: String): List<TranscriptSegment> = text.lineSequence().mapNotNull { raw ->
        val match = line.matchEntire(raw.trim()) ?: return@mapNotNull null
        TranscriptSegment(
            text = match.groupValues[9].trim(),
            startMillis = millis(match.groupValues, 1),
            endMillis = millis(match.groupValues, 5),
        )
    }.toList()

    private fun millis(groups: List<String>, offset: Int): Long =
        groups[offset].toLong() * 3_600_000 + groups[offset + 1].toLong() * 60_000 +
            groups[offset + 2].toLong() * 1_000 + groups[offset + 3].toLong()
}
