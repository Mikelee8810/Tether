package com.relationshipradar.app.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Category::class, Person::class, ContactIdentifier::class, Interaction::class, ReminderState::class, ConnectorCursor::class, PendingIdentity::class, CallInsight::class],
    version = 6,
    exportSchema = true,
    // Expand-only: v2 adds two tables; v3 adds two nullable columns; v4 adds birthday, socials, talking points.
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun personDao(): PersonDao
    abstract fun identifierDao(): IdentifierDao
    abstract fun interactionDao(): InteractionDao
    abstract fun reminderStateDao(): ReminderStateDao
    abstract fun connectorCursorDao(): ConnectorCursorDao
    abstract fun pendingIdentityDao(): PendingIdentityDao
    abstract fun callInsightDao(): CallInsightDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "relationship_radar.db")
                .build()
                .also { instance = it }
        }
    }
}
