package com.relationshipradar.app.ui

import com.relationshipradar.app.connectors.ContactsImporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs contact import away from the UI thread and converts provider/database failures
 * into a user-facing result instead of letting them crash the app.
 */
internal suspend fun runContactSyncSafely(
    sync: suspend () -> ContactsImporter.Result,
    markSynced: suspend () -> Unit,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): String {
    return try {
        val result = withContext(ioDispatcher) { sync() }
        markSynced()
        "Imported ${result.created} new, updated ${result.updated}, archived ${result.archived}"
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        "Couldn't import contacts. Please try again."
    }
}
