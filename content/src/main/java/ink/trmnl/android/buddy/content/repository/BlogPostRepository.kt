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
            private const val POSTS_FEED_URL = "https://usetrmnl.com/feeds/posts.xml"

            // Regex to extract first image from HTML content
            private val IMAGE_REGEX = Regex("""<img[^>]+src="([^"]+)"""")
        }

        /**
         * Get the latest blog posts from local cache.
         *
         * @param limit Maximum number of blog posts to retrieve (default: 3 for carousel).
         * @return Flow of blog post list that updates when database changes.
         */
        fun getLatestPosts(limit: Int = 3): Flow<List<BlogPostEntity>> = blogPostDao.getLatest(limit)

        /**
         * Get all blog posts from local cache.
         *
         * @return Flow of all blog posts, ordered by published date.
         */
        fun getAllPosts(): Flow<List<BlogPostEntity>> = blogPostDao.getAll()

        /**
         * Get blog posts filtered by category.
         *
         * @param category The category to filter by.
         * @return Flow of blog posts in the specified category.
         */
        fun getPostsByCategory(category: String): Flow<List<BlogPostEntity>> = blogPostDao.getByCategory(category)

        /**
         * Get only favorited/bookmarked blog posts.
         *
         * @return Flow of favorited blog posts, ordered by published date.
         */
        fun getFavoritePosts(): Flow<List<BlogPostEntity>> = blogPostDao.getFavorites()

        /**
         * Get only unread blog posts.
         *
         * @return Flow of unread blog posts, ordered by published date.
         */
        fun getUnreadPosts(): Flow<List<BlogPostEntity>> = blogPostDao.getUnread()

        /**
         * Get recently read blog posts.
         *
         * @return Flow of recently read blog posts, ordered by last read time.
         */
        fun getRecentlyReadPosts(): Flow<List<BlogPostEntity>> = blogPostDao.getRecentlyRead()

        /**
         * Get count of unread blog posts.
         *
         * @return Flow of unread count that updates when database changes.
         */
        fun getUnreadCount(): Flow<Int> = blogPostDao.getUnreadCount()

        /**
         * Get all unique categories from blog posts.
         *
         * @return Flow of category list that updates when database changes.
         */
        fun getCategories(): Flow<List<String>> = blogPostDao.getCategories()

        /**
         * Search blog posts by query string.
         *
         * @param query The search query.
         * @return Flow of matching blog posts.
         */
        fun searchPosts(query: String): Flow<List<BlogPostEntity>> = blogPostDao.searchPosts(query)

        /**
         * Refresh blog posts from the RSS feed.
         *
         * Fetches the latest blog posts from usetrmnl.com and stores them
         * in the local database. Preserves read status, favorites, and reading
         * progress for existing posts.
         *
         * @return Result indicating success or failure with error message.
         */
        suspend fun refreshPosts(): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val channel = rssParser.getRssChannel(POSTS_FEED_URL)
                    val fetchedAt = Instant.now()

                    // Get existing posts to preserve user preferences
                    val existingPosts = blogPostDao.getAll().first()
                    val existingPostsMap =
                        existingPosts.associateBy { it.id }.mapValues { (_, post) ->
                            Triple(post.isRead, post.isFavorite, post.readingProgressPercent)
                        }

                    val posts =
                        channel.items.map { item ->
                            val id = item.link ?: item.guid ?: throw IllegalStateException("Missing ID")

                            // Extract featured image from HTML content
                            val featuredImageUrl = extractFeaturedImage(item.content ?: "")

                            // Get category from the first category element
                            val category = item.categories.firstOrNull()

                            // Get author name from author field
                            val authorName = item.author ?: "TRMNL"

                            // Create summary from content (first 200 chars without HTML)
                            val summary =
                                item.description?.take(200)
                                    ?: stripHtml(item.content ?: "").take(200)

                            // Preserve existing user preferences if post already exists
                            val (isRead, isFavorite, readingProgress) =
                                existingPostsMap[id] ?: Triple(false, false, 0f)

                            BlogPostEntity(
                                id = id,
                                title = item.title ?: "Untitled",
                                summary = summary,
                                link = item.link ?: "",
                                authorName = authorName,
                                category = category,
                                publishedDate = item.pubDate?.let { parseDate(it) } ?: Instant.now(),
                                featuredImageUrl = featuredImageUrl,
                                isRead = isRead,
                                readingProgressPercent = readingProgress,
                                lastReadAt = null, // Preserve this through a separate query if needed
                                fetchedAt = fetchedAt,
                                isFavorite = isFavorite,
                            )
                        }

                    blogPostDao.insertAll(posts)
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
         *
         * @param htmlContent The HTML content to extract from.
         * @return The first image URL found, or null if none found.
         */
        private fun extractFeaturedImage(htmlContent: String): String? {
            val matchResult = IMAGE_REGEX.find(htmlContent)
            return matchResult?.groupValues?.getOrNull(1)
        }

        /**
         * Strip HTML tags from a string.
         *
         * @param html The HTML string to strip.
         * @return Plain text with HTML tags removed.
         */
        private fun stripHtml(html: String): String = html.replace(Regex("<[^>]*>"), "")

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
