package ink.trmnl.android.buddy.content.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity representing a TRMNL blog post from the RSS feed.
 *
 * Blog posts are detailed articles from TRMNL that provide tutorials,
 * feature deep-dives, and updates about the platform. They typically
 * include featured images, author information, and categories.
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
     * Author's display name.
     */
    val authorName: String,
    /**
     * Post category (e.g., "Tutorial", "Feature", "Update").
     * Nullable as not all posts may have a category.
     */
    val category: String?,
    /**
     * When the blog post was published by TRMNL.
     */
    val publishedDate: Instant,
    /**
     * URL to the featured/header image for the blog post.
     * Nullable as not all posts may have a featured image.
     */
    val featuredImageUrl: String?,
    /**
     * Whether the user has read/viewed this blog post.
     */
    val isRead: Boolean = false,
    /**
     * Reading progress percentage (0.0 to 100.0).
     * Used for tracking how much of the post the user has read.
     */
    val readingProgressPercent: Float = 0f,
    /**
     * Last time the user read/viewed this blog post.
     * Nullable as the post may never have been read.
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
