package com.relationshipradar.app.data

import android.content.Context
import com.relationshipradar.app.data.db.AppDatabase
import com.relationshipradar.app.data.db.Category
import com.relationshipradar.app.data.db.ContactIdentifier
import com.relationshipradar.app.data.db.IdentifierType
import com.relationshipradar.app.data.db.Interaction
import com.relationshipradar.app.data.db.InteractionSource
import com.relationshipradar.app.data.db.InteractionType
import com.relationshipradar.app.data.db.Person
import com.relationshipradar.app.data.db.ReminderBehavior
import com.relationshipradar.app.data.db.Direction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Local JSON Backup & Restore for Tether.
 * Completely offline, privacy-first data portability.
 */
object BackupManager {

    suspend fun exportToJson(db: AppDatabase): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appName", "Tether")

        // 1. Categories
        val categoriesArray = JSONArray()
        val categories = db.categoryDao().getAll()
        for (c in categories) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("defaultIntervalDays", c.defaultIntervalDays ?: JSONObject.NULL)
                put("reminderBehavior", c.reminderBehavior.name)
                put("builtIn", c.builtIn)
                put("sortOrder", c.sortOrder)
            }
            categoriesArray.put(obj)
        }
        root.put("categories", categoriesArray)

        // 2. People
        val peopleArray = JSONArray()
        val people = db.personDao().getAll()
        for (p in people) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("displayName", p.displayName)
                put("categoryId", p.categoryId ?: JSONObject.NULL)
                put("trackingEnabled", p.trackingEnabled)
                put("remindersEnabled", p.remindersEnabled)
                put("reminderIntervalDays", p.reminderIntervalDays ?: JSONObject.NULL)
                put("reminderBehavior", p.reminderBehavior?.name ?: JSONObject.NULL)
                put("archived", p.archived)
                put("notes", p.notes)
                put("snoozedUntil", p.snoozedUntil ?: JSONObject.NULL)
                put("pausedUntil", p.pausedUntil ?: JSONObject.NULL)
                put("avatar", p.avatar ?: JSONObject.NULL)
                put("contactLookupKey", p.contactLookupKey ?: JSONObject.NULL)
                put("birthday", p.birthday ?: JSONObject.NULL)
                put("anniversary", p.anniversary ?: JSONObject.NULL)
                put("talkingPoints", p.talkingPoints ?: JSONObject.NULL)
                put("messengerHandle", p.messengerHandle ?: JSONObject.NULL)
                put("whatsappNumber", p.whatsappNumber ?: JSONObject.NULL)
                put("instagramHandle", p.instagramHandle ?: JSONObject.NULL)
                put("snapchatHandle", p.snapchatHandle ?: JSONObject.NULL)
                put("discordHandle", p.discordHandle ?: JSONObject.NULL)
                put("meetLink", p.meetLink ?: JSONObject.NULL)
            }
            peopleArray.put(obj)
        }
        root.put("people", peopleArray)

        // 3. Identifiers
        val idArray = JSONArray()
        val identifiers = db.identifierDao().getAll()
        for (i in identifiers) {
            val obj = JSONObject().apply {
                put("id", i.id)
                put("personId", i.personId)
                put("type", i.type.name)
                put("rawValue", i.rawValue)
                put("normalizedValue", i.normalizedValue)
                put("source", i.source)
            }
            idArray.put(obj)
        }
        root.put("identifiers", idArray)

        // 4. Interactions
        val interactionsArray = JSONArray()
        val interactions = db.interactionDao().getAll()
        for (ix in interactions) {
            val obj = JSONObject().apply {
                put("id", ix.id)
                put("personId", ix.personId)
                put("source", ix.source.name)
                put("type", ix.type.name)
                put("direction", ix.direction.name)
                put("timestamp", ix.timestamp)
                put("countsTowardTimer", ix.countsTowardTimer)
                put("approximate", ix.approximate)
                put("note", ix.note)
                put("externalId", ix.externalId)
            }
            interactionsArray.put(obj)
        }
        root.put("interactions", interactionsArray)

        root.toString(2)
    }

    suspend fun saveBackupToFile(context: Context, db: AppDatabase): File = withContext(Dispatchers.IO) {
        val json = exportToJson(db)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "Tether_Backup_$timeStamp.json"
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(json)
        file
    }

    suspend fun restoreFromJson(db: AppDatabase, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            
            // Import Categories
            if (root.has("categories")) {
                val array = root.getJSONArray("categories")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val cat = Category(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        defaultIntervalDays = if (obj.isNull("defaultIntervalDays")) null else obj.getInt("defaultIntervalDays"),
                        reminderBehavior = ReminderBehavior.valueOf(obj.optString("reminderBehavior", "ROUNDUP")),
                        builtIn = obj.optBoolean("builtIn", false),
                        sortOrder = obj.optInt("sortOrder", 0)
                    )
                    db.categoryDao().insert(cat)
                }
            }

            // Import People
            if (root.has("people")) {
                val array = root.getJSONArray("people")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val person = Person(
                        id = obj.getLong("id"),
                        displayName = obj.getString("displayName"),
                        categoryId = if (obj.isNull("categoryId")) null else obj.getLong("categoryId"),
                        trackingEnabled = obj.optBoolean("trackingEnabled", true),
                        remindersEnabled = obj.optBoolean("remindersEnabled", false),
                        reminderIntervalDays = if (obj.isNull("reminderIntervalDays")) null else obj.getInt("reminderIntervalDays"),
                        reminderBehavior = if (obj.isNull("reminderBehavior")) null else ReminderBehavior.valueOf(obj.getString("reminderBehavior")),
                        archived = obj.optBoolean("archived", false),
                        notes = obj.optString("notes", ""),
                        snoozedUntil = if (obj.isNull("snoozedUntil")) null else obj.getLong("snoozedUntil"),
                        pausedUntil = if (obj.isNull("pausedUntil")) null else obj.getLong("pausedUntil"),
                        avatar = if (obj.isNull("avatar")) null else obj.getString("avatar"),
                        contactLookupKey = if (obj.isNull("contactLookupKey")) null else obj.getString("contactLookupKey"),
                        birthday = if (obj.isNull("birthday")) null else obj.getString("birthday"),
                        anniversary = if (obj.isNull("anniversary")) null else obj.getString("anniversary"),
                        talkingPoints = if (obj.isNull("talkingPoints")) null else obj.getString("talkingPoints"),
                        messengerHandle = if (obj.isNull("messengerHandle")) null else obj.getString("messengerHandle"),
                        whatsappNumber = if (obj.isNull("whatsappNumber")) null else obj.getString("whatsappNumber"),
                        instagramHandle = if (obj.isNull("instagramHandle")) null else obj.getString("instagramHandle"),
                        snapchatHandle = if (obj.isNull("snapchatHandle")) null else obj.getString("snapchatHandle"),
                        discordHandle = if (obj.isNull("discordHandle")) null else obj.getString("discordHandle"),
                        meetLink = if (obj.isNull("meetLink")) null else obj.getString("meetLink")
                    )
                    db.personDao().insert(person)
                }
            }

            // Import Identifiers
            if (root.has("identifiers")) {
                val array = root.getJSONArray("identifiers")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val ident = ContactIdentifier(
                        id = obj.getLong("id"),
                        personId = obj.getLong("personId"),
                        type = IdentifierType.valueOf(obj.getString("type")),
                        rawValue = obj.getString("rawValue"),
                        normalizedValue = obj.getString("normalizedValue"),
                        source = obj.optString("source", "manual")
                    )
                    db.identifierDao().insert(ident)
                }
            }

            // Import Interactions
            if (root.has("interactions")) {
                val array = root.getJSONArray("interactions")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val ix = Interaction(
                        id = obj.getLong("id"),
                        personId = obj.getLong("personId"),
                        source = InteractionSource.valueOf(obj.getString("source")),
                        type = InteractionType.valueOf(obj.getString("type")),
                        timestamp = obj.getLong("timestamp"),
                        direction = Direction.valueOf(obj.getString("direction")),
                        countsTowardTimer = obj.optBoolean("countsTowardTimer", true),
                        approximate = obj.optBoolean("approximate", false),
                        note = obj.optString("note", ""),
                        externalId = obj.optString("externalId", UUID.randomUUID().toString())
                    )
                    db.interactionDao().insert(ix)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
