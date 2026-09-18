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
        require(modelFile.isFile) { "Whisper model is not installed" }
        val floatSamples = FloatArray(pcm.samples.size) { index -> pcm.samples[index] / 32768f }
        val context = WhisperContext.createContextFromFile(modelFile.absolutePath)
        return try {
            TranscriptResult(
                text = context.transcribeData(floatSamples, printTimestamp = true).trim(),
                modelName = modelFile.name,
                audioDurationMillis = pcm.durationMillis,
            )
        } finally {
            context.release()
        }
    }
}
