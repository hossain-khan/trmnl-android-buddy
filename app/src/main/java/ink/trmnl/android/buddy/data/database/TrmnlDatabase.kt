package ink.trmnl.android.buddy.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for TRMNL Buddy app.
 *
 * Stores battery history data for device health monitoring and trajectory analysis.
 */
@Database(
    entities = [BatteryHistoryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TrmnlDatabase : RoomDatabase() {
    abstract fun batteryHistoryDao(): BatteryHistoryDao

    companion object {
        private const val DATABASE_NAME = "trmnl_buddy.db"

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
                        ).build()
                instance = newInstance
                newInstance
            }
    }
}
