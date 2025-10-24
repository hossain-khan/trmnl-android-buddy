package ink.trmnl.android.buddy.content.models

import java.time.Instant

/**
 * Sealed class representing unified content items from different sources.
 *
 * This allows combining announcements and blog posts into a single feed
 * while preserving type-specific information.
 *
 * All content items share common properties for display and sorting:
 * - id: Unique identifier
 * - title: Display title
 * - summary: Short description
 * - link: URL to full content
 * - publishedDate: When the content was published (for sorting)
 * - isRead: Whether user has read this content
 */
sealed class ContentItem {
    abstract val id: String
    abstract val title: String
    abstract val summary: String
    abstract val link: String
    abstract val publishedDate: Instant
    abstract val isRead: Boolean

    /**
     * Announcement content item.
     *
     * Represents TRMNL product announcements and updates.
     */
    data class Announcement(
        override val id: String,
        override val title: String,
        override val summary: String,
        override val link: String,
        override val publishedDate: Instant,
        override val isRead: Boolean,
    ) : ContentItem()

    /**
     * Blog post content item.
     *
     * Represents TRMNL blog posts with richer metadata.
     */
    data class BlogPost(
        override val id: String,
        override val title: String,
        override val summary: String,
        override val link: String,
        override val publishedDate: Instant,
        override val isRead: Boolean,
        val authorName: String,
        val category: String?,
        val featuredImageUrl: String?,
        val isFavorite: Boolean,
    ) : ContentItem()
}
