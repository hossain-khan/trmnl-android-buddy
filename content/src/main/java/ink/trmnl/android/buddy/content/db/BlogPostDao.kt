package ink.trmnl.android.buddy.content.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for blog post-related database operations.
 */
@Dao
interface BlogPostDao {
    /**
     * Get all blog posts, ordered by published date (newest first).
     */
    @Query("SELECT * FROM blog_posts ORDER BY publishedDate DESC")
    fun getAll(): Flow<List<BlogPostEntity>>

    /**
     * Get the latest N blog posts for display in the carousel.
     *
     * @param limit Maximum number of blog posts to retrieve.
     */
    @Query("SELECT * FROM blog_posts ORDER BY publishedDate DESC LIMIT :limit")
    fun getLatest(limit: Int): Flow<List<BlogPostEntity>>

    /**
     * Get blog posts filtered by category.
     *
     * @param category Category name to filter by.
     */
    @Query("SELECT * FROM blog_posts WHERE category = :category ORDER BY publishedDate DESC")
    fun getByCategory(category: String): Flow<List<BlogPostEntity>>

    /**
     * Get all favorited/bookmarked blog posts.
     */
    @Query("SELECT * FROM blog_posts WHERE isFavorite = 1 ORDER BY publishedDate DESC")
    fun getFavorites(): Flow<List<BlogPostEntity>>

    /**
     * Get only unread blog posts, ordered by published date.
     */
    @Query("SELECT * FROM blog_posts WHERE isRead = 0 ORDER BY publishedDate DESC")
    fun getUnread(): Flow<List<BlogPostEntity>>

    /**
     * Get recently read blog posts (posts with lastReadAt not null),
     * ordered by most recently read first.
     */
    @Query("SELECT * FROM blog_posts WHERE lastReadAt IS NOT NULL ORDER BY lastReadAt DESC")
    fun getRecentlyRead(): Flow<List<BlogPostEntity>>

    /**
     * Insert blog posts, replacing existing ones with the same ID.
     *
     * @param posts List of blog posts to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<BlogPostEntity>)

    /**
     * Mark a blog post as read and update lastReadAt timestamp.
     *
     * @param id The blog post ID.
     */
    @Query("UPDATE blog_posts SET isRead = 1, lastReadAt = :timestamp WHERE id = :id")
    suspend fun markAsRead(
        id: String,
        timestamp: Long =
            java.time.Instant
                .now()
                .epochSecond,
    )

    /**
     * Update reading progress for a blog post.
     *
     * @param id The blog post ID.
     * @param progress Reading progress percentage (0.0 to 100.0).
     */
    @Query("UPDATE blog_posts SET readingProgressPercent = :progress WHERE id = :id")
    suspend fun updateReadingProgress(
        id: String,
        progress: Float,
    )

    /**
     * Toggle favorite status for a blog post.
     *
     * @param id The blog post ID.
     */
    @Query("UPDATE blog_posts SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    /**
     * Delete blog posts older than the specified timestamp.
     * Used for cache cleanup.
     *
     * @param threshold Timestamp threshold (Unix epoch seconds).
     */
    @Query("DELETE FROM blog_posts WHERE fetchedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    /**
     * Search blog posts by title or summary (case-insensitive).
     *
     * @param query Search query string.
     */
    @Query(
        """
        SELECT * FROM blog_posts 
        WHERE title LIKE '%' || :query || '%' 
        OR summary LIKE '%' || :query || '%'
        ORDER BY publishedDate DESC
        """,
    )
    fun searchPosts(query: String): Flow<List<BlogPostEntity>>

    /**
     * Get the count of unread blog posts.
     */
    @Query("SELECT COUNT(*) FROM blog_posts WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    /**
     * Get the count of favorited blog posts.
     */
    @Query("SELECT COUNT(*) FROM blog_posts WHERE isFavorite = 1")
    fun getFavoritesCount(): Flow<Int>
}
