package ink.trmnl.android.buddy.content.repository

import com.prof18.rssparser.RssParser
import ink.trmnl.android.buddy.content.db.AnnouncementDao
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

/**
 * Repository for managing TRMNL announcement content.
 *
 * Implements offline-first pattern: data is fetched from RSS feed,
 * stored in Room database, and emitted via Flow for reactive UI updates.
 */
class AnnouncementRepository
    @Inject
    constructor(
        private val announcementDao: AnnouncementDao,
    ) {
        // Create RssParser directly to avoid Metro compiler bug with this class
        private val rssParser = RssParser()

        companion object {
            private const val ANNOUNCEMENTS_FEED_URL = "https://usetrmnl.com/feeds/announcements.xml"
        }

        /**
         * Get the latest announcements from local cache.
         *
         * @param limit Maximum number of announcements to retrieve (default: 3 for carousel).
         * @return Flow of announcement list that updates when database changes.
         */
        fun getLatestAnnouncements(limit: Int = 3): Flow<List<AnnouncementEntity>> = announcementDao.getLatest(limit)

        /**
         * Get all announcements from local cache.
         *
         * @return Flow of all announcements, ordered by published date.
         */
        fun getAllAnnouncements(): Flow<List<AnnouncementEntity>> = announcementDao.getAll()

        /**
         * Get only unread announcements from local cache.
         *
         * @return Flow of unread announcements, ordered by published date.
         */
        fun getUnreadAnnouncements(): Flow<List<AnnouncementEntity>> = announcementDao.getUnread()

        /**
         * Get only read announcements from local cache.
         *
         * @return Flow of read announcements, ordered by published date.
         */
        fun getReadAnnouncements(): Flow<List<AnnouncementEntity>> = announcementDao.getRead()

        /**
         * Get count of unread announcements.
         *
         * @return Flow of unread count that updates when database changes.
         */
        fun getUnreadCount(): Flow<Int> = announcementDao.getUnreadCount()

        /**
         * Refresh announcements from the RSS feed.
         *
         * Fetches the latest announcements from usetrmnl.com and stores them
         * in the local database. Preserves read status for existing announcements.
         *
         * @return Result indicating success or failure with error message.
         */
        suspend fun refreshAnnouncements(): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val channel = rssParser.getRssChannel(ANNOUNCEMENTS_FEED_URL)
                    val fetchedAt = Instant.now()

                    // Get existing announcements to preserve read status
                    val existingAnnouncements = announcementDao.getAll().first()
                    val existingReadIds = existingAnnouncements.filter { it.isRead }.map { it.id }.toSet()

                    val announcements =
                        channel.items.map { item ->
                            val id = item.link ?: item.guid ?: throw IllegalStateException("Missing ID")
                            AnnouncementEntity(
                                id = id,
                                title = item.title ?: "Untitled",
                                summary = item.description ?: "",
                                link = item.link ?: "",
                                publishedDate = item.pubDate?.let { parseDate(it) } ?: Instant.now(),
                                isRead = existingReadIds.contains(id), // Preserve read status
                                fetchedAt = fetchedAt,
                            )
                        }

                    announcementDao.insertAll(announcements)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        /**
         * Mark an announcement as read.
         *
         * @param id The announcement ID.
         */
        suspend fun markAsRead(id: String) {
            withContext(Dispatchers.IO) {
                announcementDao.markAsRead(id)
            }
        }

        /**
         * Mark an announcement as unread.
         *
         * @param id The announcement ID.
         */
        suspend fun markAsUnread(id: String) {
            withContext(Dispatchers.IO) {
                announcementDao.markAsUnread(id)
            }
        }

        /**
         * Mark all announcements as read.
         */
        suspend fun markAllAsRead() {
            withContext(Dispatchers.IO) {
                announcementDao.markAllAsRead()
            }
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
