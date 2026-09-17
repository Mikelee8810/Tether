package com.relationshipradar.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** How reminders for a category (or person) are delivered. */
enum class ReminderBehavior { INDIVIDUAL, ROUNDUP }

enum class IdentifierType { PHONE, EMAIL, HANDLE, CONTACT_LOOKUP }

/** Where an interaction came from. Connectors add their own values here. */
enum class InteractionSource { MANUAL, PHONE, SMS, NOTIFICATION, EMAIL, CALENDAR, OTHER }

enum class InteractionType {
    CALL, MESSAGE, IN_PERSON, VIDEO_CALL, EMAIL, VOICE_NOTE, MEDIA, GROUP_MESSAGE, OTHER
}

enum class Direction { OUTGOING, INCOMING_ANSWERED, INCOMING_IGNORED }

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** null = custom / no default reminder. */
    val defaultIntervalDays: Int?,
    val reminderBehavior: ReminderBehavior = ReminderBehavior.ROUNDUP,
    val builtIn: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "people",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("categoryId"), Index("archived")],
)
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val categoryId: Long? = null,
    /** True for every saved contact by default. */
    val trackingEnabled: Boolean = true,
    /** Off by default; user opts in per person or via category. */
    val remindersEnabled: Boolean = false,
    /** Per-person override of the category default. null = use category. */
    val reminderIntervalDays: Int? = null,
    /** Per-person override of delivery. null = use category. */
    val reminderBehavior: ReminderBehavior? = null,
    val archived: Boolean = false,
    val notes: String = "",
    /** Reminder timing only; does not touch the relationship clock. */
    val snoozedUntil: Long? = null,
    /** Long.MAX_VALUE = indefinite pause. */
    val pausedUntil: Long? = null,
    /** Set once the user answered "What is this person to you?" (or chose Later twice). */
    val categorizationPromptCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** "photo" = contact photo, "avatar:NN" = bundled 3D avatar, null = initials. */
    @androidx.room.ColumnInfo(defaultValue = "NULL") val avatar: String? = null,
    /** Contact lookup key, cached here so the photo can be loaded without a join. */
    @androidx.room.ColumnInfo(defaultValue = "NULL") val contactLookupKey: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val birthday: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val anniversary: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val talkingPoints: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val messengerHandle: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val whatsappNumber: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val instagramHandle: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val snapchatHandle: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val discordHandle: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "NULL") val meetLink: String? = null,
)

@Entity(
    tableName = "identifiers",
    foreignKeys = [ForeignKey(
        entity = Person::class,
        parentColumns = ["id"],
        childColumns = ["personId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("personId"), Index(value = ["type", "normalizedValue"], unique = true)],
)
data class ContactIdentifier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val type: IdentifierType,
    val rawValue: String,
    val normalizedValue: String,
    val source: String = "contacts",
)

@Entity(
    tableName = "interactions",
    foreignKeys = [ForeignKey(
        entity = Person::class,
        parentColumns = ["id"],
        childColumns = ["personId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("personId"), Index("timestamp"), Index(value = ["source", "externalId"], unique = true)],
)
data class Interaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val source: InteractionSource,
    val type: InteractionType,
    val timestamp: Long,
    val direction: Direction,
    val countsTowardTimer: Boolean,
    /** True when the user said "sometime last week". */
    val approximate: Boolean = false,
    val note: String = "",
    /** Connector-side id so re-scans don't duplicate. Manual logs use a random UUID. */
    val externalId: String,
)

/** Push-notification bookkeeping per person. Separate from the relationship clock. */
@Entity(
    tableName = "reminder_state",
    foreignKeys = [ForeignKey(
        entity = Person::class,
        parentColumns = ["id"],
        childColumns = ["personId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class ReminderState(
    @PrimaryKey val personId: Long,
    /** The dueAt this notification cycle belongs to. New cycle when dueAt changes. */
    val cycleDueAt: Long,
    val notificationsSent: Int = 0,
    val lastNotifiedAt: Long? = null,
)

/** Per-connector "scanned up to here" marker so re-scans are cheap and idempotent. */
@Entity(tableName = "connector_cursors")
data class ConnectorCursor(
    @PrimaryKey val connectorId: String,
    val lastScannedAt: Long,
    val lastRunAt: Long,
    val lastRunCount: Int = 0,
    val enabled: Boolean = true,
)

/**
 * An identity a connector saw but could not match with confidence (e.g. a WhatsApp chat
 * called "Jess"). We ask the user once, remember the answer, and never silently merge.
 */
@Entity(tableName = "pending_identities", indices = [Index(value = ["type", "normalizedValue"], unique = true)])
data class PendingIdentity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: IdentifierType,
    val rawValue: String,
    val normalizedValue: String,
    val source: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val seenCount: Int = 1,
    /** Suggested match by name similarity, if any. */
    val suggestedPersonId: Long? = null,
    /** User said "not a person I track" — stop asking. */
    val ignored: Boolean = false,
)
