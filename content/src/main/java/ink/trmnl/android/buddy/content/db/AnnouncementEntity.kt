package ink.trmnl.android.buddy.content.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Room entity for storing announcement data.
 * Represents a single announcement from the TRMNL RSS feed.
 */
@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val summary: String,
    val link: String,
    val publishedDate: Instant,
    val isRead: Boolean = false,
    val fetchedAt: Instant,
)
