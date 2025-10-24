package ink.trmnl.android.buddy.content.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity representing a TRMNL blog post from the RSS feed.
 *
 * Blog posts are rich content articles from TRMNL that provide tutorials,
 * updates, and platform insights. Unlike announcements, they include
 * featured images, author information, categories, and reading progress tracking.
 */
@Entity(tableName = "blog_posts")
data class BlogPostEntity(
    /**
     * Unique identifier from the RSS feed (typically the post URL).
     */
    @PrimaryKey
    val id: String,
    /**
     * Title of the blog post.
     */
    val title: String,
    /**
     * Brief summary or description of the blog post.
     */
    val summary: String,
    /**
     * URL to the full blog post on usetrmnl.com.
     */
    val link: String,
    /**
     * Author display name (e.g., "Ryan Kulp", "Mario Lurig").
     */
    val authorName: String,
    /**
     * Post category (nullable, e.g., "trmnl", "features", "tutorials").
     */
    val category: String?,
    /**
     * When the blog post was published by TRMNL.
     */
    val publishedDate: Instant,
    /**
     * URL to the featured image (nullable).
     * Extracted from the HTML content of the blog post.
     */
    val featuredImageUrl: String?,
    /**
     * Whether the user has read this blog post.
     */
    val isRead: Boolean = false,
    /**
     * Reading progress percentage (0.0 to 100.0).
     * Used for tracking how much of the post the user has read.
     */
    val readingProgressPercent: Float = 0f,
    /**
     * Last time the user read this post (nullable).
     */
    val lastReadAt: Instant? = null,
    /**
     * When this blog post was fetched from the RSS feed.
     * Used for cache invalidation and showing "new" badges.
     */
    val fetchedAt: Instant = Instant.now(),
    /**
     * Whether the user has bookmarked/favorited this blog post.
     */
    val isFavorite: Boolean = false,
)
