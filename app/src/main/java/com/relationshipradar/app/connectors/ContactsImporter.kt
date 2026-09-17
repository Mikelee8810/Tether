package com.relationshipradar.app.connectors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.relationshipradar.app.data.db.IdentifierType
import com.relationshipradar.app.data.repo.Repository

/**
 * Imports every saved contact as a Person (tracking ON, reminders OFF).
 * Re-running is safe: contacts are matched by lookup key, then phone/email.
 * Contacts that vanished from the phone are archived, never deleted.
 */
class ContactsImporter(private val context: Context, private val repo: Repository) {

    fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    data class Result(val created: Int, val updated: Int, val archived: Int)

    suspend fun sync(): Result {
        if (!hasPermission()) return Result(0, 0, 0)
        var created = 0
        var updated = 0
        val seenLookupKeys = mutableSetOf<String>()

        val cr = context.contentResolver
        val contacts = mutableMapOf<String, String>() // lookupKey -> name
        cr.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val key = c.getString(0) ?: continue
                val name = c.getString(1)?.trim().orEmpty()
                if (name.isNotEmpty()) contacts[key] = name
            }
        }

        val phones = mutableMapOf<String, MutableList<String>>()
        cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val key = c.getString(0) ?: continue
                c.getString(1)?.let { phones.getOrPut(key) { mutableListOf() }.add(it) }
            }
        }

        val emails = mutableMapOf<String, MutableList<String>>()
        cr.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.LOOKUP_KEY, ContactsContract.CommonDataKinds.Email.ADDRESS),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val key = c.getString(0) ?: continue
                c.getString(1)?.let { emails.getOrPut(key) { mutableListOf() }.add(it) }
            }
        }

        val birthdays = mutableMapOf<String, String>()
        val anniversaries = mutableMapOf<String, String>()
        cr.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Event.START_DATE,
                ContactsContract.CommonDataKinds.Event.TYPE
            ),
            "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE),
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val key = c.getString(0) ?: continue
                val date = c.getString(1) ?: continue
                val type = c.getInt(2)
                if (type == ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY) {
                    birthdays[key] = date
                } else if (type == ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY) {
                    anniversaries[key] = date
                }
            }
        }

        for ((key, name) in contacts) {
            seenLookupKeys += key
            var person = repo.findPersonByIdentifier(IdentifierType.CONTACT_LOOKUP, key)
            if (person == null) {
                // High-confidence identity match: same phone number or email already known.
                person = phones[key]?.firstNotNullOfOrNull { repo.findPersonByIdentifier(IdentifierType.PHONE, it) }
                    ?: emails[key]?.firstNotNullOfOrNull { repo.findPersonByIdentifier(IdentifierType.EMAIL, it) }
            }
            val bday = birthdays[key]
            val anniv = anniversaries[key]
            val personId = if (person == null) {
                created++
                val id = repo.createPerson(name)
                repo.getPerson(id)?.let {
                    repo.updatePerson(
                        it.copy(
                            contactLookupKey = key,
                            avatar = "photo",
                            birthday = bday,
                            anniversary = anniv
                        )
                    )
                }
                id
            } else {
                updated++
                if (person.archived) repo.restore(person.id) // returning contact: restore + merge
                repo.updatePerson(
                    person.copy(
                        displayName = name,
                        contactLookupKey = key,
                        avatar = person.avatar ?: "photo",
                        birthday = bday ?: person.birthday,
                        anniversary = anniv ?: person.anniversary
                    )
                )
                person.id
            }
            repo.addIdentifier(personId, IdentifierType.CONTACT_LOOKUP, key)
            phones[key]?.forEach { repo.addIdentifier(personId, IdentifierType.PHONE, it) }
            emails[key]?.forEach { repo.addIdentifier(personId, IdentifierType.EMAIL, it) }
        }

        // Archive people whose contact card disappeared.
        var archived = 0
        val known = repo.identifiersOfType(IdentifierType.CONTACT_LOOKUP)
        for (id in known) {
            if (id.normalizedValue !in seenLookupKeys) {
                val p = repo.getPerson(id.personId) ?: continue
                if (!p.archived) { repo.archive(p.id); archived++ }
            }
        }
        return Result(created, updated, archived)
    }
}
