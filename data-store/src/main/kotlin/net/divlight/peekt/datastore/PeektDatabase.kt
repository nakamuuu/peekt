package net.divlight.peekt.datastore

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for Peekt HTTP transaction storage.
 */
@Database(
    entities = [HttpTransactionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PeektDatabase : RoomDatabase() {
    abstract fun httpTransactionDao(): HttpTransactionDao

    companion object {
        private const val NAME = "peekt.db"

        /**
         * Opens or creates the Peekt database file on device storage.
         *
         * @param context Any [Context]; [Context.getApplicationContext] is used internally.
         */
        fun create(context: Context): PeektDatabase {
            return Room.databaseBuilder(context.applicationContext, PeektDatabase::class.java, NAME)
                .fallbackToDestructiveMigration(false)
                .build()
        }

        /**
         * Creates an in-memory database instance. Data is discarded when the process ends.
         *
         * @param context Any [Context]; [Context.getApplicationContext] is used internally.
         */
        fun createInMemory(context: Context): PeektDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                PeektDatabase::class.java
            )
                .fallbackToDestructiveMigration(false)
                .build()
        }
    }
}
