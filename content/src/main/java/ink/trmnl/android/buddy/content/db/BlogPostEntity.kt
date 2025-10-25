package ink.trmnl.android.buddy.content.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity representing a blog post from TRMNL RSS feed.
 *
 * Blog posts are richer than announcements, containing author information,
 * categories, featured images, and reading progress tracking.
 *
 * @property id Unique identifier (post URL)
 * @property title Blog post title
 * @property summary Short description or excerpt
 * @property link URL to the full blog post
 * @property authorName Author of the post (e.g., "Ryan Kulp", "Mario Lurig")
 * @property category Post category (e.g., "TRMNL", "DevOps", nullable)
 * @property publishedDate Publication timestamp (used for sorting)
 * @property featuredImageUrl URL to the main/hero image (nullable)
 * @property imageUrls List of all image URLs found in the post content
 * @property isRead Whether the user has read this post
 * @property readingProgressPercent How much of the post the user has read (0-100)
 * @property lastReadAt Last time the user opened this post
 * @property fetchedAt When this post was cached from the RSS feed
 * @property isFavorite Whether the user has bookmarked this post
 */
@Entity(tableName = "blog_posts")
data class BlogPostEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val link: String,
    val authorName: String,
    val category: String?,
    val publishedDate: Instant,
    val featuredImageUrl: String?,
    val imageUrls: List<String>? = null,
    val isRead: Boolean = false,
    val readingProgressPercent: Float = 0f,
    val lastReadAt: Instant? = null,
    val fetchedAt: Instant = Instant.now(),
    val isFavorite: Boolean = false,
)
