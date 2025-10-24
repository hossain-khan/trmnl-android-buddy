package ink.trmnl.android.buddy.content.repository

import com.prof18.rssparser.RssParser
import ink.trmnl.android.buddy.content.db.BlogPostDao
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository for managing blog posts from TRMNL RSS feed.
 *
 * Provides offline-first access to blog posts with automatic caching
 * and reactive updates via Flow.
 *
 * Feed URL: https://usetrmnl.com/feeds/posts.xml (Atom format)
 *
 * @property blogPostDao DAO for blog post database operations
 */
class BlogPostRepository(
    private val blogPostDao: BlogPostDao,
) {
    companion object {
        private const val BLOG_POSTS_FEED_URL = "https://usetrmnl.com/feeds/posts.xml"
    }

    /**
     * Get all blog posts from local database, ordered by published date (newest first).
     *
     * @return Flow of blog posts that updates when database changes
     */
    fun getAllBlogPosts(): Flow<List<BlogPostEntity>> = blogPostDao.getAll()

    /**
     * Get blog posts filtered by category.
     *
     * @param category Category to filter by
     * @return Flow of filtered blog posts
     */
    fun getBlogPostsByCategory(category: String): Flow<List<BlogPostEntity>> = blogPostDao.getByCategory(category)

    /**
     * Get favorite/bookmarked blog posts.
     *
     * @return Flow of favorite blog posts
     */
    fun getFavoriteBlogPosts(): Flow<List<BlogPostEntity>> = blogPostDao.getFavorites()

    /**
     * Get unread blog posts.
     *
     * @return Flow of unread blog posts
     */
    fun getUnreadBlogPosts(): Flow<List<BlogPostEntity>> = blogPostDao.getUnread()

    /**
     * Get count of unread blog posts.
     *
     * @return Number of unread posts
     */
    suspend fun getUnreadCount(): Int = blogPostDao.getUnread().first().size

    /**
     * Get recently read blog posts (up to 10).
     *
     * @return Flow of recently read blog posts
     */
    fun getRecentlyReadBlogPosts(): Flow<List<BlogPostEntity>> = blogPostDao.getRecentlyRead()

    /**
     * Search blog posts by query string (searches title and summary).
     *
     * @param query Search query
     * @return Flow of matching blog posts
     */
    fun searchBlogPosts(query: String): Flow<List<BlogPostEntity>> = blogPostDao.searchPosts(query)

    /**
     * Refresh blog posts from RSS feed and update local database.
     *
     * Fetches latest posts from the feed, parses them, and stores in local database.
     * Preserves read status for existing posts.
     *
     * @return Result with success or error
     */
    suspend fun refreshBlogPosts(): Result<Unit> =
        try {
            // Create RSS parser
            val rssParser = RssParser()

            // Fetch and parse feed
            val channel = rssParser.getRssChannel(BLOG_POSTS_FEED_URL)

            // Convert RSS items to BlogPostEntity
            val blogPosts =
                channel.items.mapNotNull { item ->
                    try {
                        // Parse published date
                        val publishedDate =
                            item.pubDate?.let { pubDate ->
                                try {
                                    ZonedDateTime
                                        .parse(pubDate, DateTimeFormatter.ISO_DATE_TIME)
                                        .toInstant()
                                } catch (e: Exception) {
                                    null
                                }
                            } ?: Instant.now()

                        // Extract author from RSS item (if available)
                        val authorName = item.author ?: "TRMNL Team"

                        // Extract category from RSS item (if available)
                        val category = item.categories.firstOrNull()

                        // Extract featured image from content (first img tag)
                        val featuredImageUrl = extractFeaturedImage(item.content)

                        BlogPostEntity(
                            id = item.link ?: item.guid ?: return@mapNotNull null,
                            title = item.title ?: return@mapNotNull null,
                            summary = item.description ?: "",
                            link = item.link ?: return@mapNotNull null,
                            authorName = authorName,
                            category = category,
                            publishedDate = publishedDate,
                            featuredImageUrl = featuredImageUrl,
                            fetchedAt = Instant.now(),
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

            // Insert posts into database (IGNORE strategy preserves existing user state like isFavorite, isRead)
            blogPostDao.insertAll(blogPosts)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    /**
     * Extract featured image URL from HTML content.
     * Looks for the first <img> tag in the content.
     *
     * @param content HTML content string
     * @return Image URL or null if not found
     */
    private fun extractFeaturedImage(content: String?): String? {
        if (content.isNullOrBlank()) return null

        // Simple regex to find first img src
        val imgRegex = """<img[^>]+src\s*=\s*["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
        return imgRegex.find(content)?.groupValues?.getOrNull(1)
    }

    /**
     * Mark a blog post as read.
     *
     * @param id Blog post ID
     */
    suspend fun markAsRead(id: String) {
        blogPostDao.markAsRead(id)
    }

    /**
     * Update reading progress for a blog post.
     *
     * @param id Blog post ID
     * @param progress Reading progress (0-100)
     */
    suspend fun updateReadingProgress(
        id: String,
        progress: Float,
    ) {
        blogPostDao.updateReadingProgress(id, progress, Instant.now())
    }

    /**
     * Toggle favorite status for a blog post.
     *
     * @param id Blog post ID
     */
    suspend fun toggleFavorite(id: String) {
        blogPostDao.toggleFavorite(id)
    }

    /**
     * Clean up old blog posts from cache.
     * Removes posts older than 30 days.
     */
    suspend fun cleanupOldPosts() {
        val thirtyDaysAgo = Instant.now().minusSeconds(30L * 24 * 60 * 60)
        blogPostDao.deleteOlderThan(thirtyDaysAgo.epochSecond)
    }
}
