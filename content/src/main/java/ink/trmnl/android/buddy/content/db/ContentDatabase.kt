package ink.trmnl.android.buddy.content.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database for TRMNL content (announcements, blog posts).
 *
 * This database stores cached RSS feed content for offline access
 * and provides a single source of truth for content data.
 */
@Database(
    entities = [AnnouncementEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ContentDatabase : RoomDatabase() {
    /**
     * Get the DAO for announcement operations.
     */
    abstract fun announcementDao(): AnnouncementDao
}
