package ink.trmnl.android.buddy.content.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for TRMNL content (announcements, blog posts).
 *
 * This database stores cached RSS feed content for offline access
 * and provides a single source of truth for content data.
 */
@Database(
    entities = [AnnouncementEntity::class, BlogPostEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ContentDatabase : RoomDatabase() {
    /**
     * Get the DAO for announcement operations.
     */
    abstract fun announcementDao(): AnnouncementDao

    /**
     * Get the DAO for blog post operations.
     */
    abstract fun blogPostDao(): BlogPostDao

    companion object {
        /**
         * Migration from version 1 to version 2.
         * Adds the blog_posts table.
         */
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Create blog_posts table
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `blog_posts` (
                            `id` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `summary` TEXT NOT NULL,
                            `link` TEXT NOT NULL,
                            `authorName` TEXT NOT NULL,
                            `category` TEXT,
                            `publishedDate` INTEGER NOT NULL,
                            `featuredImageUrl` TEXT,
                            `isRead` INTEGER NOT NULL DEFAULT 0,
                            `readingProgressPercent` REAL NOT NULL DEFAULT 0.0,
                            `lastReadAt` INTEGER,
                            `fetchedAt` INTEGER NOT NULL,
                            `isFavorite` INTEGER NOT NULL DEFAULT 0,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )
                }
            }
    }
}
