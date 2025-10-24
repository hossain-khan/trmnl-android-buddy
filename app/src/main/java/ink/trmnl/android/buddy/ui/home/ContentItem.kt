package ink.trmnl.android.buddy.ui.home

import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import java.time.Instant

/**
 * Sealed class representing content items that can appear in the content carousel.
 * Combines announcements and blog posts into a single type for unified display.
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
        override val id: String = entity.id
        override val title: String = entity.title
        override val summary: String = entity.summary
        override val link: String = entity.link
        override val publishedDate: Instant = entity.publishedDate
        override val isRead: Boolean = entity.isRead
    }

    /**
     * Blog post content item.
     */
    data class BlogPost(
        val entity: BlogPostEntity,
    ) : ContentItem() {
        override val id: String = entity.id
        override val title: String = entity.title
        override val summary: String = entity.summary
        override val link: String = entity.link
        override val publishedDate: Instant = entity.publishedDate
        override val isRead: Boolean = entity.isRead

        val authorName: String = entity.authorName
        val category: String? = entity.category
        val featuredImageUrl: String? = entity.featuredImageUrl
    }
}
