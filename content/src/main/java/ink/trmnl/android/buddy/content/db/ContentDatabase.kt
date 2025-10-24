package ink.trmnl.android.buddy.content.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database for content management.
 * Stores announcements and other content data.
 */
@Database(
    entities = [AnnouncementEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ContentDatabase : RoomDatabase() {
    abstract fun announcementDao(): AnnouncementDao

    companion object {
        private const val DATABASE_NAME = "content_database"

        @Volatile
        private var instance: ContentDatabase? = null

        fun getInstance(context: Context): ContentDatabase =
            instance ?: synchronized(this) {
                val newInstance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            ContentDatabase::class.java,
                            DATABASE_NAME,
                        ).build()
                instance = newInstance
                newInstance
            }
    }
}
