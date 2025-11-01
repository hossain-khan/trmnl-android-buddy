package ink.trmnl.android.buddy.content.repository

import ink.trmnl.android.buddy.content.db.AnnouncementDao
import ink.trmnl.android.buddy.content.db.BlogPostDao
import ink.trmnl.android.buddy.content.models.ContentItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Repository for managing combined content feed (announcements + blog posts).
 *
 * Provides unified access to both content types sorted by published date,
 * enabling a single feed UI for all TRMNL content.
 *
 * @property announcementDao DAO for announcements
 * @property blogPostDao DAO for blog posts
 */
class ContentFeedRepository(
    private val announcementDao: AnnouncementDao,
    private val blogPostDao: BlogPostDao,
) {
    companion object {
        private const val DEFAULT_LIMIT = 3
    }

    /**
     * Get latest content from both announcements and blog posts,
     * sorted by published date (descending).
     *
     * This combines both feeds into a single stream of content items,
     * perfect for carousel displays or unified content views.
     *
     * @param limit Maximum number of items to return (default: 3 for carousel)
     * @return Flow of combined content items sorted by date
     */
    fun getLatestContent(limit: Int = DEFAULT_LIMIT): Flow<List<ContentItem>> =
        combine(
            announcementDao.getAll(),
            blogPostDao.getAll(),
        ) { announcements, blogPosts ->
            val items = mutableListOf<ContentItem>()

            // Convert announcements to ContentItem
            announcements.forEach { announcement ->
                items.add(
                    ContentItem.Announcement(
                        id = announcement.id,
                        title = announcement.title,
                        summary = announcement.summary,
                        link = announcement.link,
                        publishedDate = announcement.publishedDate,
                        isRead = announcement.isRead,
                    ),
                )
            }

            // Convert blog posts to ContentItem
            blogPosts.forEach { post ->
                items.add(
                    ContentItem.BlogPost(
                        id = post.id,
                        title = post.title,
                        summary = post.summary,
                        link = post.link,
                        publishedDate = post.publishedDate,
                        authorName = post.authorName,
                        category = post.category,
                        featuredImageUrl = post.featuredImageUrl,
                        isRead = post.isRead,
                        isFavorite = post.isFavorite,
                    ),
                )
            }

            // Sort by published date (newest first) and take top N
            items.sortedByDescending { it.publishedDate }.take(limit)
        }

    /**
     * Get latest unread content from both announcements and blog posts,
     * sorted by published date (descending).
     *
     * This is optimized for carousel displays that only show unread items.
     * Filters out all read content before sorting and limiting.
     *
     * @param limit Maximum number of unread items to return (default: 3 for carousel)
     * @return Flow of unread content items sorted by date
     */
    fun getLatestUnreadContent(limit: Int = DEFAULT_LIMIT): Flow<List<ContentItem>> =
        combine(
            announcementDao.getUnread(),
            blogPostDao.getUnread(),
        ) { announcements, blogPosts ->
            val items = mutableListOf<ContentItem>()

            // Convert announcements to ContentItem
            announcements.forEach { announcement ->
                items.add(
                    ContentItem.Announcement(
                        id = announcement.id,
                        title = announcement.title,
                        summary = announcement.summary,
                        link = announcement.link,
                        publishedDate = announcement.publishedDate,
                        isRead = announcement.isRead,
                    ),
                )
            }

            // Convert blog posts to ContentItem
            blogPosts.forEach { post ->
                items.add(
                    ContentItem.BlogPost(
                        id = post.id,
                        title = post.title,
                        summary = post.summary,
                        link = post.link,
                        publishedDate = post.publishedDate,
                        authorName = post.authorName,
                        category = post.category,
                        featuredImageUrl = post.featuredImageUrl,
                        isRead = post.isRead,
                        isFavorite = post.isFavorite,
                    ),
                )
            }

            // Sort by published date (newest first) and take top N
            items.sortedByDescending { it.publishedDate }.take(limit)
        }

    /**
     * Get unread content count across both announcements and blog posts.
     *
     * Useful for badge indicators showing total unread items.
     *
     * @return Flow of unread count
     */
    fun getUnreadCount(): Flow<Int> =
        combine(
            announcementDao.getUnreadCount(),
            blogPostDao.getUnread(),
        ) { announcementCount, unreadBlogPosts ->
            announcementCount + unreadBlogPosts.size
        }
}
