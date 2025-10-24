package ink.trmnl.android.buddy.content.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity representing a TRMNL announcement from the RSS feed.
 *
 * Announcements are short updates from TRMNL that appear in a carousel
 * on the home screen. They typically contain important news, feature
 * announcements, or system updates.
 */
@Entity(tableName = "announcements")
data class AnnouncementEntity(
    /**
     * Unique identifier from the RSS feed (typically the post URL).
     */
    @PrimaryKey
    val id: String,
    /**
     * Title of the announcement.
     */
    val title: String,
    /**
     * Brief summary or description of the announcement.
     */
    val summary: String,
    /**
     * URL to the full announcement on usetrmnl.com.
     */
    val link: String,
    /**
     * When the announcement was published by TRMNL.
     */
    val publishedDate: Instant,
    /**
     * Whether the user has read/viewed this announcement.
     */
    val isRead: Boolean = false,
    /**
     * When this announcement was fetched from the RSS feed.
     * Used for cache invalidation and showing "new" badges.
     */
    val fetchedAt: Instant = Instant.now(),
)
