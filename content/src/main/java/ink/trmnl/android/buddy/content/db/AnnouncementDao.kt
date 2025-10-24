package ink.trmnl.android.buddy.content.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for announcements.
 * Provides reactive queries using Flow.
 */
@Dao
interface AnnouncementDao {
    /**
     * Get all announcements ordered by published date (newest first).
     */
    @Query("SELECT * FROM announcements ORDER BY publishedDate DESC")
    fun getAllAnnouncements(): Flow<List<AnnouncementEntity>>

    /**
     * Get the latest N announcements.
     */
    @Query("SELECT * FROM announcements ORDER BY publishedDate DESC LIMIT :limit")
    fun getLatestAnnouncements(limit: Int): Flow<List<AnnouncementEntity>>

    /**
     * Get count of unread announcements.
     */
    @Query("SELECT COUNT(*) FROM announcements WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    /**
     * Get unread announcements only.
     */
    @Query("SELECT * FROM announcements WHERE isRead = 0 ORDER BY publishedDate DESC")
    fun getUnreadAnnouncements(): Flow<List<AnnouncementEntity>>

    /**
     * Get read announcements only.
     */
    @Query("SELECT * FROM announcements WHERE isRead = 1 ORDER BY publishedDate DESC")
    fun getReadAnnouncements(): Flow<List<AnnouncementEntity>>

    /**
     * Get a single announcement by ID.
     */
    @Query("SELECT * FROM announcements WHERE id = :id")
    suspend fun getAnnouncementById(id: String): AnnouncementEntity?

    /**
     * Insert or replace announcements.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<AnnouncementEntity>)

    /**
     * Update an existing announcement.
     */
    @Update
    suspend fun updateAnnouncement(announcement: AnnouncementEntity)

    /**
     * Mark an announcement as read.
     */
    @Query("UPDATE announcements SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    /**
     * Mark an announcement as unread.
     */
    @Query("UPDATE announcements SET isRead = 0 WHERE id = :id")
    suspend fun markAsUnread(id: String)

    /**
     * Mark all announcements as read.
     */
    @Query("UPDATE announcements SET isRead = 1")
    suspend fun markAllAsRead()

    /**
     * Delete all announcements (clear cache).
     */
    @Query("DELETE FROM announcements")
    suspend fun deleteAll()

    /**
     * Delete announcements older than a certain date.
     */
    @Query("DELETE FROM announcements WHERE publishedDate < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: Long)
}
