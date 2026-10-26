package com.relationshipradar.app.connectors.callrecorder

import com.whispercpp.whisper.WhisperContext
import java.io.File

data class TranscriptResult(
    val text: String,
    val modelName: String,
    val audioDurationMillis: Long,
)

/** Thin lifecycle-safe wrapper around the official whisper.cpp Android binding. */
class LocalWhisperTranscriber {
    suspend fun transcribe(pcm: DecodedPcm, modelFile: File): TranscriptResult {
        val session = openSession(modelFile)
        return try {
            TranscriptResult(
                text = session.transcribe(pcm),
                modelName = modelFile.name,
                audioDurationMillis = pcm.durationMillis,
            )
        } finally {
            session.close()
        }
    }

    /** Opens one Whisper context that can transcribe many bounded chunks in sequence. */
    fun openSession(modelFile: File): Session {
        require(modelFile.isFile) { "Whisper model is not installed" }
        return Session(WhisperContext.createContextFromFile(modelFile.absolutePath))
    }

    /**
     * A single loaded Whisper context. Each [transcribe] call converts only that chunk's
     * samples to floats, so transcribing a long call in decoder chunks keeps peak memory
     * bounded by the chunk size instead of the whole call. Timestamped output is shifted by
     * the chunk's offset so stitched text keeps recording-relative timestamps.
     */
    class Session internal constructor(private val context: WhisperContext) : AutoCloseable {
        suspend fun transcribe(pcm: DecodedPcm): String {
            val floatSamples = FloatArray(pcm.samples.size) { index -> pcm.samples[index] / 32768f }
            val raw = context.transcribeData(floatSamples, printTimestamp = true).trim()
            return shiftTimestamps(raw, pcm.startOffsetMillis)
        }

        override fun close() = context.release()

        private fun shiftTimestamps(text: String, offsetMillis: Long): String {
            if (offsetMillis == 0L) return text
            val segments = WhisperTimestampParser.parse(text)
            if (segments.isEmpty()) return text
            return segments.joinToString("\n") { segment ->
                "[${formatTimestamp(segment.startMillis + offsetMillis)} --> " +
                    "${formatTimestamp(segment.endMillis + offsetMillis)}] ${segment.text}"
            }
        }

        private fun formatTimestamp(millis: Long): String {
            val hours = millis / 3_600_000
            val minutes = millis % 3_600_000 / 60_000
            val seconds = millis % 60_000 / 1_000
            val ms = millis % 1_000
            return "%02d:%02d:%02d.%03d".format(hours, minutes, seconds, ms)
        }
    }
}
