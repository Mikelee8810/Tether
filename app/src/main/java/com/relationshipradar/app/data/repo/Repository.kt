package com.relationshipradar.app.data.repo

import com.relationshipradar.app.data.db.AppDatabase
import com.relationshipradar.app.data.db.BuiltInCategories
import com.relationshipradar.app.data.db.Category
import com.relationshipradar.app.data.db.CallInsight
import com.relationshipradar.app.data.db.CallInsightDecision
import com.relationshipradar.app.data.db.ConnectorCursor
import com.relationshipradar.app.data.db.PendingIdentity
import com.relationshipradar.app.data.db.ContactIdentifier
import com.relationshipradar.app.data.db.Direction
import com.relationshipradar.app.data.db.IdentifierType
import com.relationshipradar.app.data.db.Interaction
import com.relationshipradar.app.data.db.InteractionSource
import com.relationshipradar.app.data.db.InteractionType
import com.relationshipradar.app.data.db.Person
import com.relationshipradar.app.engine.PersonRadar
import com.relationshipradar.app.engine.ReminderEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import java.time.LocalDate
import java.time.ZoneId

/** Single entry point for the UI, workers, and connectors. */
class Repository(private val db: AppDatabase) {
    private val people = db.personDao()
    private val categories = db.categoryDao()
    private val identifiers = db.identifierDao()
    private val interactions = db.interactionDao()

    suspend fun seedCategoriesIfEmpty() {
        if (categories.count() == 0) categories.insertAll(BuiltInCategories.all)
    }

    // ---- Radar ----------------------------------------------------------------------------

    fun observeRadar(): Flow<List<PersonRadar>> =
        combine(people.observeActive(), interactions.observeLastEfforts()) { list, efforts ->
            val byId = efforts.associate { it.personId to it.lastEffortAt }
            list.map { ReminderEngine.evaluate(it.person, it.category, byId[it.person.id]) }
        }

    suspend fun radarSnapshot(): List<PersonRadar> {
        val efforts = interactions.lastEfforts().associate { it.personId to it.lastEffortAt }
        return people.getActive().map { ReminderEngine.evaluate(it.person, it.category, efforts[it.person.id]) }
    }

    fun observePerson(id: Long) = people.observe(id)
    fun observeIdentifiers(id: Long) = identifiers.observeForPerson(id)
    fun observeInteractions(id: Long) = interactions.observeForPerson(id)
    fun observeArchived() = people.observeArchived()
    fun observeUncategorized() = people.observeUncategorized()
    fun observeCategories() = categories.observeAll()

    // ---- People ---------------------------------------------------------------------------

    suspend fun createPerson(name: String, categoryId: Long? = null): Long =
        people.insert(Person(displayName = name.trim(), categoryId = categoryId))

    suspend fun updatePerson(person: Person) = people.update(person)
    suspend fun setCategory(personId: Long, categoryId: Long?) = people.setCategory(personId, categoryId)
    suspend fun bumpPromptCount(personId: Long) = people.bumpPromptCount(personId)
    suspend fun snooze(personId: Long, until: Long?) = people.setSnooze(personId, until)
    suspend fun pause(personId: Long, until: Long?) = people.setPause(personId, until)

    /** Archive keeps timeline, notes, and identity links. */
    suspend fun archive(personId: Long) = people.setArchived(personId, true)
    suspend fun restore(personId: Long) = people.setArchived(personId, false)

    /** Merge [from] into [into]: moves identifiers + interactions, then archives the empty shell. */
    suspend fun merge(from: Long, into: Long) {
        if (from == into) return
        identifiers.reassign(from, into)
        interactions.reassign(from, into)
        people.setArchived(from, true)
    }

    // ---- Identity -------------------------------------------------------------------------

    suspend fun addIdentifier(personId: Long, type: IdentifierType, raw: String, source: String = "manual") =
        identifiers.insert(ContactIdentifier(personId = personId, type = type, rawValue = raw, normalizedValue = normalize(type, raw), source = source))

    suspend fun findPersonByIdentifier(type: IdentifierType, raw: String): Person? =
        identifiers.find(type, normalize(type, raw))?.let { people.get(it.personId) }

    suspend fun identifiersOfType(type: IdentifierType) = identifiers.allOfType(type)
    suspend fun getPerson(id: Long) = people.get(id)

    fun normalize(type: IdentifierType, raw: String): String = when (type) {
        IdentifierType.PHONE -> raw.filter { it.isDigit() || it == '+' }.let { if (it.length > 10 && !it.startsWith("+")) it.takeLast(10) else it.removePrefix("+1") }
        IdentifierType.EMAIL -> raw.trim().lowercase()
        else -> raw.trim().lowercase()
    }

    // ---- Interactions ---------------------------------------------------------------------

    /** Manual "I saw someone" log. */
    suspend fun logManual(personId: Long, type: InteractionType, timestamp: Long, approximate: Boolean, note: String = "") =
        interactions.insert(
            Interaction(
                personId = personId,
                source = InteractionSource.MANUAL,
                type = type,
                timestamp = timestamp,
                direction = Direction.OUTGOING,
                countsTowardTimer = true,
                approximate = approximate,
                note = note,
                externalId = "manual:" + UUID.randomUUID(),
            ),
        )

    /** Connectors call this. Idempotent on (source, externalId). */
    suspend fun record(interaction: Interaction) = interactions.insert(interaction)
    suspend fun recordAll(list: List<Interaction>) = interactions.insertAll(list)
    suspend fun deleteInteraction(i: Interaction) = interactions.delete(i)

    // ---- Categories -----------------------------------------------------------------------

    suspend fun addCategory(c: Category) = categories.insert(c)
    suspend fun updateCategory(c: Category) = categories.update(c)
    suspend fun deleteCategory(c: Category) { if (!c.builtIn) categories.delete(c) }

    fun reminderStateDao() = db.reminderStateDao()

    // ---- Connectors -----------------------------------------------------------------------

    private val cursors = db.connectorCursorDao()
    private val pending = db.pendingIdentityDao()

    fun observeCursors() = cursors.observeAll()
    suspend fun cursor(id: String) = cursors.get(id)
    suspend fun saveCursor(c: ConnectorCursor) = cursors.upsert(c)
    suspend fun setConnectorEnabled(id: String, enabled: Boolean) {
        val c = cursors.get(id) ?: ConnectorCursor(id, 0L, 0L)
        cursors.upsert(c.copy(enabled = enabled))
    }

    fun observePendingIdentities() = pending.observeOpen()

    /** Called by connectors for an identity they couldn't match. Bumps seen count if already there. */
    suspend fun notePendingIdentity(type: IdentifierType, raw: String, source: String, suggested: Long?, seenAt: Long) {
        val norm = normalize(type, raw)
        val existing = pending.find(type, norm)
        if (existing == null) {
            pending.insert(PendingIdentity(type = type, rawValue = raw, normalizedValue = norm, source = source, firstSeenAt = seenAt, lastSeenAt = seenAt, suggestedPersonId = suggested))
        } else if (!existing.ignored) {
            pending.update(existing.copy(lastSeenAt = maxOf(existing.lastSeenAt, seenAt), seenCount = existing.seenCount + 1, suggestedPersonId = existing.suggestedPersonId ?: suggested))
        }
    }

    /** User answered: this identity belongs to [personId]. Remembered forever via the identifier table. */
    suspend fun resolvePendingIdentity(p: PendingIdentity, personId: Long) {
        addIdentifier(personId, p.type, p.rawValue, p.source)
        pending.delete(p)
    }

    /** User answered: create a new person for this identity. */
    suspend fun resolvePendingAsNewPerson(p: PendingIdentity, name: String): Long {
        val id = createPerson(name)
        resolvePendingIdentity(p, id)
        return id
    }

    suspend fun ignorePendingIdentity(p: PendingIdentity) = pending.update(p.copy(ignored = true))

    /** Loose name match used only to *suggest*, never to auto-merge. */
    suspend fun suggestPersonByName(name: String): Person? = people.findByName(name.trim())

    // ---- Reviewed call insights -----------------------------------------------------------

    fun observeAcceptedCallInsights(personId: Long) = db.callInsightDao().observeAcceptedForPerson(personId)
    fun observeUpcomingCallInsights(from: Long = System.currentTimeMillis()) = db.callInsightDao().observeUpcoming(from)
    suspend fun callInsightsForPerson(personId: Long) = db.callInsightDao().forPerson(personId)
    suspend fun dueCallFollowUps(now: Long = System.currentTimeMillis()) = db.callInsightDao().dueFollowUps(now)
    suspend fun markCallFollowUpNotified(id: Long, at: Long = System.currentTimeMillis()) = db.callInsightDao().markNotified(id, at)

    /** Saves one explicit review decision. The recording/candidate key makes repeated imports safe. */
    suspend fun acceptCallInsight(
        personId: Long,
        sourceRecordingId: String,
        candidateId: String,
        type: String,
        text: String,
        sourceExcerpt: String,
        sourceStartMillis: Long,
        suggestedDate: LocalDate?,
    ): Long = db.callInsightDao().insert(
        CallInsight(
            personId = personId,
            sourceRecordingId = sourceRecordingId,
            candidateId = candidateId,
            type = type,
            decision = CallInsightDecision.ACCEPTED,
            text = text.trim(),
            sourceExcerpt = sourceExcerpt,
            sourceStartMillis = sourceStartMillis,
            scheduledAt = suggestedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        ),
    )

    suspend fun dismissCallInsight(
        personId: Long,
        sourceRecordingId: String,
        candidateId: String,
        type: String,
        text: String,
        sourceExcerpt: String,
        sourceStartMillis: Long,
    ): Long = db.callInsightDao().insert(
        CallInsight(
            personId = personId,
            sourceRecordingId = sourceRecordingId,
            candidateId = candidateId,
            type = type,
            decision = CallInsightDecision.DISMISSED,
            text = text,
            sourceExcerpt = sourceExcerpt,
            sourceStartMillis = sourceStartMillis,
        ),
    )
}
