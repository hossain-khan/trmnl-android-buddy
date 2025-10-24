package ink.trmnl.android.buddy.content.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Data Access Object for blog post operations.
 *
 * Provides queries for retrieving, filtering, and managing blog posts
 * from the local Room database.
 */
@Dao
interface BlogPostDao {
    /**
     * Get all blog posts ordered by published date (newest first).
     */
    @Query("SELECT * FROM blog_posts ORDER BY publishedDate DESC")
    fun getAll(): Flow<List<BlogPostEntity>>

    /**
     * Get blog posts filtered by category.
     *
     * @param category Category to filter by
     */
    @Query("SELECT * FROM blog_posts WHERE category = :category ORDER BY publishedDate DESC")
    fun getByCategory(category: String): Flow<List<BlogPostEntity>>

    /**
     * Get all favorite/bookmarked blog posts.
     */
    @Query("SELECT * FROM blog_posts WHERE isFavorite = 1 ORDER BY publishedDate DESC")
    fun getFavorites(): Flow<List<BlogPostEntity>>

    /**
     * Get all unread blog posts.
     */
    @Query("SELECT * FROM blog_posts WHERE isRead = 0 ORDER BY publishedDate DESC")
    fun getUnread(): Flow<List<BlogPostEntity>>

    /**
     * Get recently read blog posts (up to 10 most recent).
     */
    @Query("SELECT * FROM blog_posts WHERE lastReadAt IS NOT NULL ORDER BY lastReadAt DESC LIMIT 10")
    fun getRecentlyRead(): Flow<List<BlogPostEntity>>

    /**
     * Insert or replace blog posts in the database.
     *
     * @param posts List of blog posts to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<BlogPostEntity>)

    /**
     * Mark a blog post as read.
     *
     * @param id Blog post ID
     */
    @Query("UPDATE blog_posts SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    /**
     * Update reading progress for a blog post.
     *
     * @param id Blog post ID
     * @param progress Reading progress (0-100)
     * @param timestamp When the progress was recorded
     */
    @Query("UPDATE blog_posts SET readingProgressPercent = :progress, lastReadAt = :timestamp WHERE id = :id")
    suspend fun updateReadingProgress(
        id: String,
        progress: Float,
        timestamp: Instant,
    )

    /**
     * Toggle favorite status for a blog post.
     *
     * @param id Blog post ID
     */
    @Query("UPDATE blog_posts SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    /**
     * Delete blog posts older than a threshold timestamp.
     * Used for cache cleanup.
     *
     * @param threshold Epoch seconds threshold
     */
    @Query("DELETE FROM blog_posts WHERE fetchedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    /**
     * Search blog posts by title or summary.
     *
     * @param query Search query string
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
}
