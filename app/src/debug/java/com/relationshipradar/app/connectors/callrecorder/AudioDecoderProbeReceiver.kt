package com.relationshipradar.app.connectors.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Debug-only on-device proof that both Call Recorder output formats reach the same PCM contract. */
class AudioDecoderProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        Thread {
            try {
                val decoder = AudioPcmDecoder(context)
                FIXTURES.forEach { fixture ->
                    val pcm = context.assets.openFd(fixture).use { asset ->
                        decoder.decode(asset.fileDescriptor, asset.startOffset, asset.length)
                    }
                    check(pcm.samples.isNotEmpty()) { "$fixture decoded to no samples" }
                    check(pcm.sampleRate == 16_000 && pcm.channelCount == 1)
                    Log.i(TAG, "success file=$fixture samples=${pcm.samples.size} rate=${pcm.sampleRate} channels=${pcm.channelCount} durationMs=${pcm.durationMillis}")
                }
            } catch (error: Throwable) {
                Log.e(TAG, "failure ${error::class.java.simpleName}: ${error.message}", error)
            } finally {
                result.finish()
            }
        }.start()
    }

    private companion object {
        const val TAG = "AudioDecoderProbe"
        val FIXTURES = listOf(
            "call_recordings/tether-sample-opus.ogg",
            "call_recordings/tether-sample-aac.m4a",
        )
    }
}
