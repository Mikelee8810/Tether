package com.relationshipradar.app.connectors.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.runBlocking
import java.io.File

/** Debug-only end-to-end proof: AAC fixture -> PCM -> whisper.cpp -> timestamped text. */
class WhisperTranscriptionProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                val pcm = context.assets.openFd("call_recordings/tether-sample-aac.m4a").use { asset ->
                    AudioPcmDecoder(context).decode(asset.fileDescriptor, asset.startOffset, asset.length)
                }
                val model = File(context.filesDir, "models/ggml-tiny.en.bin")
                val transcript = runBlocking { LocalWhisperTranscriber().transcribe(pcm, model) }
                Log.i(TAG, "success model=${transcript.modelName} durationMs=${transcript.audioDurationMillis} text=${transcript.text}")
            } catch (error: Throwable) {
                Log.e(TAG, "failure ${error::class.java.simpleName}: ${error.message}", error)
            } finally {
                pending.finish()
            }
        }.start()
    }

    private companion object {
        const val TAG = "WhisperProbe"
    }
}
