package ink.trmnl.android.buddy.content.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for announcement-related database operations.
 */
@Dao
interface AnnouncementDao {
    /**
     * Get all announcements, ordered by published date (newest first).
     */
    @Query("SELECT * FROM announcements ORDER BY publishedDate DESC")
    fun getAll(): Flow<List<AnnouncementEntity>>

    /**
     * Get the latest N announcements for display in the carousel.
     *
     * @param limit Maximum number of announcements to retrieve.
     */
    @Query("SELECT * FROM announcements ORDER BY publishedDate DESC LIMIT :limit")
    fun getLatest(limit: Int): Flow<List<AnnouncementEntity>>

    /**
     * Get only unread announcements, ordered by published date.
     */
    @Query("SELECT * FROM announcements WHERE isRead = 0 ORDER BY publishedDate DESC")
    fun getUnread(): Flow<List<AnnouncementEntity>>

    /**
     * Get only read announcements, ordered by published date.
     */
    @Query("SELECT * FROM announcements WHERE isRead = 1 ORDER BY publishedDate DESC")
    fun getRead(): Flow<List<AnnouncementEntity>>

    /**
     * Insert announcements, replacing existing ones with the same ID.
     *
     * @param announcements List of announcements to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(announcements: List<AnnouncementEntity>)

    /**
     * Mark an announcement as read.
     *
     * @param id The announcement ID.
     */
    @Query("UPDATE announcements SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    /**
     * Mark all announcements as read.
     */
    @Query("UPDATE announcements SET isRead = 1")
    suspend fun markAllAsRead()

    /**
     * Delete announcements older than the specified timestamp.
     * Used for cache cleanup.
     *
     * @param threshold Timestamp threshold (Unix epoch seconds).
     */
    @Query("DELETE FROM announcements WHERE fetchedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    /**
     * Get the count of unread announcements.
     */
    @Query("SELECT COUNT(*) FROM announcements WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>
}
