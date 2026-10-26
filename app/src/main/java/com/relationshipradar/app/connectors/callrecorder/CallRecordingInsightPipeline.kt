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
        val modelManager = WhisperModelManager(context)
        // Gate on the verified status (expected size + hash), not mere file existence, so a
        // truncated or corrupted download is reported before transcription starts.
        val status = modelManager.status()
        require(status.valid) { "Offline transcription model is not installed or failed verification" }
        val model = modelManager.modelFile
        val decoder = AudioPcmDecoder(context)
        val transcriber = LocalWhisperTranscriber()
        // Decode and transcribe in bounded chunks, stitching the text, so a long call never
        // sits in memory all at once. The Whisper context is opened once for the whole call.
        val text = StringBuilder()
        var durationMillis = 0L
        withContext(Dispatchers.Default) {
            val session = transcriber.openSession(model)
            try {
                decoder.decodeTo16kMonoChunkFlow(recording.contentUri).collect { chunk ->
                    durationMillis = chunk.startOffsetMillis + chunk.durationMillis
                    val chunkText = session.transcribe(chunk)
                    if (chunkText.isNotBlank()) {
                        if (text.isNotEmpty()) text.append('\n')
                        text.append(chunkText)
                    }
                }
            } finally {
                session.close()
            }
        }
        val transcript = TranscriptResult(
            text = text.toString(),
            modelName = model.name,
            audioDurationMillis = durationMillis,
        )
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
