package ink.trmnl.android.buddy.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for TRMNL Buddy app.
 *
 * Stores battery history data for device health monitoring and trajectory analysis,
 * and bookmarked recipes for quick access.
 */
@Database(
    entities = [
        BatteryHistoryEntity::class,
        BookmarkedRecipeEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class TrmnlDatabase : RoomDatabase() {
    abstract fun batteryHistoryDao(): BatteryHistoryDao

    abstract fun bookmarkedRecipeDao(): BookmarkedRecipeDao

    companion object {
        private const val DATABASE_NAME = "trmnl_buddy.db"

        /**
         * Migration from version 1 to version 2.
         * Adds bookmarked_recipes table.
         */
        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS bookmarked_recipes (
                            recipe_id INTEGER PRIMARY KEY NOT NULL,
                            recipe_name TEXT NOT NULL,
                            recipe_icon_url TEXT,
                            installs INTEGER NOT NULL,
                            forks INTEGER NOT NULL,
                            bookmarked_at INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                }
            }

        @Volatile
        private var instance: TrmnlDatabase? = null

        /**
         * Get the singleton database instance.
         *
         * @param context Application context
         * @return Database instance
         */
        fun getInstance(context: Context): TrmnlDatabase =
            instance ?: synchronized(this) {
                val newInstance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            TrmnlDatabase::class.java,
                            DATABASE_NAME,
                        ).addMigrations(MIGRATION_1_2)
                        .build()
                instance = newInstance
                newInstance
            }
    }
}
