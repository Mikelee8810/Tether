package com.relationshipradar.app.connectors.callrecorder

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CallRecording(
    val id: String,
    val displayName: String,
    val phoneNumber: String?,
    val direction: String?,
    val startedAtMillis: Long,
    val durationMillis: Long?,
    val mimeType: String,
    val sizeBytes: Long,
    val contentUri: Uri,
)

/** Read-only client for the signature-protected Call Recorder provider. */
class CallRecorderBridge(private val resolver: ContentResolver) {
    suspend fun recordings(): List<CallRecording> = withContext(Dispatchers.IO) {
        resolver.query(RECORDINGS_URI, COLUMNS, null, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        CallRecording(
                            id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            displayName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DISPLAY_NAME)),
                            phoneNumber = cursor.nullableString(COLUMN_PHONE_NUMBER),
                            direction = cursor.nullableString(COLUMN_DIRECTION),
                            startedAtMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_STARTED_AT)),
                            durationMillis = cursor.nullableLong(COLUMN_DURATION),
                            mimeType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MIME_TYPE)),
                            sizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SIZE)),
                            contentUri = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT_URI))),
                        )
                    )
                }
            }
        } ?: emptyList()
    }

    fun open(recording: CallRecording) = resolver.openFileDescriptor(recording.contentUri, "r")

    private fun android.database.Cursor.nullableString(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }

    private fun android.database.Cursor.nullableLong(column: String): Long? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getLong(index)
    }

    private companion object {
        val RECORDINGS_URI: Uri = Uri.parse("content://com.kitsumed.shizucallrecorder.recordings/recordings")
        const val COLUMN_ID = "recording_id"
        const val COLUMN_DISPLAY_NAME = "display_name"
        const val COLUMN_PHONE_NUMBER = "phone_number"
        const val COLUMN_DIRECTION = "direction"
        const val COLUMN_STARTED_AT = "started_at_ms"
        const val COLUMN_DURATION = "duration_ms"
        const val COLUMN_MIME_TYPE = "mime_type"
        const val COLUMN_SIZE = "size_bytes"
        const val COLUMN_CONTENT_URI = "content_uri"
        val COLUMNS = arrayOf(COLUMN_ID, COLUMN_DISPLAY_NAME, COLUMN_PHONE_NUMBER, COLUMN_DIRECTION, COLUMN_STARTED_AT, COLUMN_DURATION, COLUMN_MIME_TYPE, COLUMN_SIZE, COLUMN_CONTENT_URI)
    }
}
