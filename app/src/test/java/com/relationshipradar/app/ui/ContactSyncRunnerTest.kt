package com.relationshipradar.app.ui

import com.relationshipradar.app.connectors.ContactsImporter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSyncRunnerTest {

    @Test
    fun `sync failure is converted to a safe message instead of escaping`() = runBlocking {
        var markedSynced = false

        val message = runContactSyncSafely(
            sync = { throw SecurityException("contacts provider changed") },
            markSynced = { markedSynced = true },
        )

        assertEquals("Couldn't import contacts. Please try again.", message)
        assertFalse(markedSynced)
    }

    @Test
    fun `successful sync reports counts and records last sync`() = runBlocking {
        var markedSynced = false

        val message = runContactSyncSafely(
            sync = { ContactsImporter.Result(created = 2, updated = 3, archived = 1) },
            markSynced = { markedSynced = true },
        )

        assertEquals("Imported 2 new, updated 3, archived 1", message)
        assertTrue(markedSynced)
    }
}
