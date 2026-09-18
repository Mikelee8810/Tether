package com.relationshipradar.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class PersonWithCategory(
    @Embedded val person: Person,
    @Relation(parentColumn = "categoryId", entityColumn = "id") val category: Category?,
)

data class LastEffort(val personId: Long, val lastEffortAt: Long?)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    suspend fun getAll(): List<Category>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<Category>)

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}

@Dao
interface PersonDao {
    @Transaction
    @Query("SELECT * FROM people WHERE archived = 0 ORDER BY displayName COLLATE NOCASE")
    fun observeActive(): Flow<List<PersonWithCategory>>

    @Transaction
    @Query("SELECT * FROM people WHERE archived = 1 ORDER BY displayName COLLATE NOCASE")
    fun observeArchived(): Flow<List<PersonWithCategory>>

    @Transaction
    @Query("SELECT * FROM people WHERE archived = 0")
    suspend fun getActive(): List<PersonWithCategory>

    @Transaction
    @Query("SELECT * FROM people WHERE id = :id")
    fun observe(id: Long): Flow<PersonWithCategory?>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun get(id: Long): Person?

    @Query("SELECT * FROM people")
    suspend fun getAll(): List<Person>

    @Query("SELECT * FROM people WHERE displayName = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): Person?

    @Transaction
    @Query("SELECT * FROM people WHERE archived = 0 AND categoryId IS NULL AND categorizationPromptCount < 2 ORDER BY createdAt DESC")
    fun observeUncategorized(): Flow<List<PersonWithCategory>>

    @Insert
    suspend fun insert(person: Person): Long

    @Update
    suspend fun update(person: Person)

    @Query("UPDATE people SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("UPDATE people SET categoryId = :categoryId WHERE id = :personId")
    suspend fun setCategory(personId: Long, categoryId: Long?)

    @Query("UPDATE people SET categorizationPromptCount = categorizationPromptCount + 1 WHERE id = :personId")
    suspend fun bumpPromptCount(personId: Long)

    @Query("UPDATE people SET snoozedUntil = :until WHERE id = :id")
    suspend fun setSnooze(id: Long, until: Long?)

    @Query("UPDATE people SET pausedUntil = :until WHERE id = :id")
    suspend fun setPause(id: Long, until: Long?)
}

@Dao
interface IdentifierDao {
    @Query("SELECT * FROM identifiers WHERE personId = :personId")
    fun observeForPerson(personId: Long): Flow<List<ContactIdentifier>>

    @Query("SELECT * FROM identifiers WHERE type = :type AND normalizedValue = :value LIMIT 1")
    suspend fun find(type: IdentifierType, value: String): ContactIdentifier?

    @Query("SELECT * FROM identifiers WHERE type = :type")
    suspend fun allOfType(type: IdentifierType): List<ContactIdentifier>

    @Query("SELECT * FROM identifiers")
    suspend fun getAll(): List<ContactIdentifier>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(identifier: ContactIdentifier): Long

    @Query("UPDATE identifiers SET personId = :toPerson WHERE personId = :fromPerson")
    suspend fun reassign(fromPerson: Long, toPerson: Long)
}

@Dao
interface InteractionDao {
    @Query("SELECT * FROM interactions WHERE personId = :personId ORDER BY timestamp DESC")
    fun observeForPerson(personId: Long): Flow<List<Interaction>>

    @Query("SELECT * FROM interactions WHERE personId = :personId ORDER BY timestamp DESC")
    suspend fun forPerson(personId: Long): List<Interaction>

    @Query("SELECT * FROM interactions")
    suspend fun getAll(): List<Interaction>

    @Query("SELECT * FROM interactions ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<Interaction>>

    @Query("SELECT personId, MAX(timestamp) AS lastEffortAt FROM interactions WHERE countsTowardTimer = 1 GROUP BY personId")
    suspend fun lastEfforts(): List<LastEffort>

    @Query("SELECT personId, MAX(timestamp) AS lastEffortAt FROM interactions WHERE countsTowardTimer = 1 GROUP BY personId")
    fun observeLastEfforts(): Flow<List<LastEffort>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(interaction: Interaction): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(interactions: List<Interaction>)

    @Delete
    suspend fun delete(interaction: Interaction)

    @Query("UPDATE interactions SET personId = :toPerson WHERE personId = :fromPerson")
    suspend fun reassign(fromPerson: Long, toPerson: Long)
}

@Dao
interface ReminderStateDao {
    @Query("SELECT * FROM reminder_state WHERE personId = :personId")
    suspend fun get(personId: Long): ReminderState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ReminderState)
}

@Dao
interface ConnectorCursorDao {
    @Query("SELECT * FROM connector_cursors WHERE connectorId = :id")
    suspend fun get(id: String): ConnectorCursor?

    @Query("SELECT * FROM connector_cursors")
    fun observeAll(): Flow<List<ConnectorCursor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cursor: ConnectorCursor)

    @Query("UPDATE connector_cursors SET enabled = :enabled WHERE connectorId = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)
}

@Dao
interface PendingIdentityDao {
    @Query("SELECT * FROM pending_identities WHERE ignored = 0 ORDER BY seenCount DESC, lastSeenAt DESC")
    fun observeOpen(): Flow<List<PendingIdentity>>

    @Query("SELECT * FROM pending_identities WHERE type = :type AND normalizedValue = :value")
    suspend fun find(type: IdentifierType, value: String): PendingIdentity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(p: PendingIdentity): Long

    @Update
    suspend fun update(p: PendingIdentity)

    @Delete
    suspend fun delete(p: PendingIdentity)
}

@Dao
interface CallInsightDao {
    @Query("SELECT * FROM call_insights WHERE personId = :personId AND decision = 'ACCEPTED' ORDER BY COALESCE(scheduledAt, createdAt) DESC")
    fun observeAcceptedForPerson(personId: Long): Flow<List<CallInsight>>

    @Query("SELECT * FROM call_insights WHERE personId = :personId ORDER BY createdAt DESC")
    suspend fun forPerson(personId: Long): List<CallInsight>

    @Query("SELECT * FROM call_insights WHERE decision = 'ACCEPTED' AND scheduledAt IS NOT NULL AND scheduledAt >= :from ORDER BY scheduledAt")
    fun observeUpcoming(from: Long): Flow<List<CallInsight>>

    @Query("SELECT * FROM call_insights WHERE decision = 'ACCEPTED' AND type = 'FOLLOW_UP' AND scheduledAt IS NOT NULL AND scheduledAt <= :now AND notifiedAt IS NULL ORDER BY scheduledAt")
    suspend fun dueFollowUps(now: Long): List<CallInsight>

    @Query("UPDATE call_insights SET notifiedAt = :notifiedAt WHERE id = :id AND notifiedAt IS NULL")
    suspend fun markNotified(id: Long, notifiedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(insight: CallInsight): Long
}
