package ink.trmnl.android.buddy.content.repository

import com.prof18.rssparser.RssParser
import ink.trmnl.android.buddy.content.db.BlogPostDao
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

/**
 * Repository for managing TRMNL blog post content.
 *
 * Implements offline-first pattern: data is fetched from RSS feed,
 * stored in Room database, and emitted via Flow for reactive UI updates.
 */
class BlogPostRepository
    @Inject
    constructor(
        private val blogPostDao: BlogPostDao,
    ) {
        // Create RssParser directly to avoid Metro compiler bug with this class
        private val rssParser = RssParser()

        companion object {
            private const val BLOG_POSTS_FEED_URL = "https://usetrmnl.com/feeds/posts.xml"
        }

        /**
         * Get the latest blog posts from local cache.
         *
         * @param limit Maximum number of blog posts to retrieve (default: 3 for carousel).
         * @return Flow of blog post list that updates when database changes.
         */
        fun getLatestBlogPosts(limit: Int = 3): Flow<List<BlogPostEntity>> = blogPostDao.getLatest(limit)

        /**
         * Get all blog posts from local cache.
         *
         * @return Flow of all blog posts, ordered by published date.
         */
        fun getAllBlogPosts(): Flow<List<BlogPostEntity>> = blogPostDao.getAll()

        /**
         * Get blog posts filtered by category.
         *
         * @param category Category name to filter by.
         * @return Flow of blog posts in the specified category.
         */
        fun getBlogPostsByCategory(category: String): Flow<List<BlogPostEntity>> = blogPostDao.getByCategory(category)

        /**
         * Get only unread blog posts from local cache.
         *
         * @return Flow of unread blog posts, ordered by published date.
         */
        fun getUnreadBlogPosts(): Flow<List<BlogPostEntity>> = blogPostDao.getUnread()

        /**
         * Get favorited/bookmarked blog posts.
         *
         * @return Flow of favorited blog posts, ordered by published date.
         */
        fun getFavoriteBlogPosts(): Flow<List<BlogPostEntity>> = blogPostDao.getFavorites()

        /**
         * Get recently read blog posts.
         *
         * @return Flow of recently read blog posts, ordered by most recently read.
         */
        fun getRecentlyReadBlogPosts(): Flow<List<BlogPostEntity>> = blogPostDao.getRecentlyRead()

        /**
         * Search blog posts by title or summary.
         *
         * @param query Search query string.
         * @return Flow of matching blog posts.
         */
        fun searchBlogPosts(query: String): Flow<List<BlogPostEntity>> = blogPostDao.searchPosts(query)

        /**
         * Get count of unread blog posts.
         *
         * @return Flow of unread count that updates when database changes.
         */
        fun getUnreadCount(): Flow<Int> = blogPostDao.getUnreadCount()

        /**
         * Get count of favorited blog posts.
         *
         * @return Flow of favorites count that updates when database changes.
         */
        fun getFavoritesCount(): Flow<Int> = blogPostDao.getFavoritesCount()

        /**
         * Refresh blog posts from the RSS feed.
         *
         * Fetches the latest blog posts from usetrmnl.com and stores them
         * in the local database. Preserves read status, favorites, and reading
         * progress for existing posts.
         *
         * @return Result indicating success or failure with error message.
         */
        suspend fun refreshBlogPosts(): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val channel = rssParser.getRssChannel(BLOG_POSTS_FEED_URL)
                    val fetchedAt = Instant.now()

                    // Get existing blog posts to preserve user state
                    val existingPosts = blogPostDao.getAll().first()
                    val existingPostsMap =
                        existingPosts.associateBy { it.id }.mapValues { (_, post) ->
                            Triple(post.isRead, post.isFavorite, post.readingProgressPercent)
                        }

                    val blogPosts =
                        channel.items.map { item ->
                            val id = item.link ?: item.guid ?: throw IllegalStateException("Missing ID")
                            val authorName = item.author ?: "TRMNL"
                            val category = item.categories.firstOrNull()
                            val featuredImageUrl = extractFeaturedImage(item.content ?: "")

                            // Preserve user state if post already exists
                            val (isRead, isFavorite, progress) =
                                existingPostsMap[id] ?: Triple(false, false, 0f)

                            BlogPostEntity(
                                id = id,
                                title = item.title ?: "Untitled",
                                summary = item.description ?: "",
                                link = item.link ?: "",
                                authorName = authorName,
                                category = category,
                                publishedDate = item.pubDate?.let { parseDate(it) } ?: Instant.now(),
                                featuredImageUrl = featuredImageUrl,
                                isRead = isRead,
                                readingProgressPercent = progress,
                                lastReadAt = null, // Don't override lastReadAt during refresh
                                fetchedAt = fetchedAt,
                                isFavorite = isFavorite,
                            )
                        }

                    blogPostDao.insertAll(blogPosts)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        /**
         * Mark a blog post as read.
         *
         * @param id The blog post ID.
         */
        suspend fun markAsRead(id: String) {
            withContext(Dispatchers.IO) {
                blogPostDao.markAsRead(id)
            }
        }

        /**
         * Update reading progress for a blog post.
         *
         * @param id The blog post ID.
         * @param progress Reading progress percentage (0.0 to 100.0).
         */
        suspend fun updateReadingProgress(
            id: String,
            progress: Float,
        ) {
            withContext(Dispatchers.IO) {
                blogPostDao.updateReadingProgress(id, progress)
            }
        }

        /**
         * Toggle favorite status for a blog post.
         *
         * @param id The blog post ID.
         */
        suspend fun toggleFavorite(id: String) {
            withContext(Dispatchers.IO) {
                blogPostDao.toggleFavorite(id)
            }
        }

        /**
         * Extract the first image URL from HTML content.
         * Looks for <img> tags in the content and extracts the src attribute.
         *
         * @param htmlContent The HTML content string.
         * @return URL of the first image, or null if no image found.
         */
        private fun extractFeaturedImage(htmlContent: String): String? {
            // Simple regex to extract first img src
            val imgRegex = """<img[^>]+src=["']([^"']+)["']""".toRegex()
            val match = imgRegex.find(htmlContent)
            return match?.groupValues?.getOrNull(1)
        }

        /**
         * Parse date string from RSS feed to Instant.
         * RSS feeds use RFC 822 format (e.g., "Mon, 01 Jan 2024 12:00:00 GMT").
         */
        private fun parseDate(dateString: String): Instant =
            try {
                // RSS feeds may provide dates in several formats:
                // - RFC 822 (e.g., "Mon, 01 Jan 2024 12:00:00 GMT")
                // - ISO 8601 (e.g., "2024-01-01T12:00:00Z", which contains 'T')
                // - Epoch milliseconds (e.g., "1704105600000", all digits)
                // Heuristic:
                //   If the string contains 'T', assume ISO 8601 and parse with Instant.parse.
                //   Otherwise, if all digits, parse as epoch millis.
                //   If neither, fallback to current time.
                if (dateString.contains('T')) {
                    Instant.parse(dateString)
                } else {
                    dateString.toLongOrNull()?.let { Instant.ofEpochMilli(it) } ?: Instant.now()
                }
            } catch (e: Exception) {
                Instant.now()
            }
    }
