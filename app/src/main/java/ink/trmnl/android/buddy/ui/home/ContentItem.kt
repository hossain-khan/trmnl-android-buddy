package ink.trmnl.android.buddy.ui.home

import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import java.time.Instant

/**
 * Sealed class representing a content item that can be either an announcement or a blog post.
 *
 * Used for displaying combined content feed in the carousel and content hub.
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
     */
    data class Announcement(
        val entity: AnnouncementEntity,
    ) : ContentItem() {
        override val id: String get() = entity.id
        override val title: String get() = entity.title
        override val summary: String get() = entity.summary
        override val link: String get() = entity.link
        override val publishedDate: Instant get() = entity.publishedDate
        override val isRead: Boolean get() = entity.isRead
    }

    /**
     * Blog post content item.
     */
    data class BlogPost(
        val entity: BlogPostEntity,
    ) : ContentItem() {
        override val id: String get() = entity.id
        override val title: String get() = entity.title
        override val summary: String get() = entity.summary
        override val link: String get() = entity.link
        override val publishedDate: Instant get() = entity.publishedDate
        override val isRead: Boolean get() = entity.isRead

        val authorName: String get() = entity.authorName
        val category: String? get() = entity.category
        val featuredImageUrl: String? get() = entity.featuredImageUrl
    }

    /**
     * Get the type label for display (e.g., "Announcement" or "Blog").
     */
    fun getTypeLabel(): String =
        when (this) {
            is Announcement -> "Announcement"
            is BlogPost -> "Blog"
        }
}
