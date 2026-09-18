package com.relationshipradar.app.connectors.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.runBlocking

/** Debug-only runtime proof for the cross-app provider contract. */
class CallRecorderBridgeProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        Thread {
            try {
                val recordings = runBlocking { CallRecorderBridge(context.contentResolver).recordings() }
                val first = recordings.firstOrNull()
                val streamReadable = first?.let {
                    CallRecorderBridge(context.contentResolver).open(it)?.use { descriptor -> descriptor.statSize >= 0L }
                } ?: true
                Log.i(TAG, "success count=${recordings.size} firstMime=${first?.mimeType ?: "none"} streamReadable=$streamReadable")
            } catch (error: Throwable) {
                Log.e(TAG, "failure ${error::class.java.simpleName}: ${error.message}", error)
            } finally {
                result.finish()
            }
        }.start()
    }

    private companion object {
        const val TAG = "CallRecorderBridgeProbe"
    }
}
