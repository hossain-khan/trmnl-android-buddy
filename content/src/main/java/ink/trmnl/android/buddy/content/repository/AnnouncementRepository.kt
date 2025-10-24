package ink.trmnl.android.buddy.content.repository

import com.prof18.rssparser.RssParser
import ink.trmnl.android.buddy.content.db.AnnouncementDao
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
         * Get count of unread announcements.
         *
         * @return Flow of unread count that updates when database changes.
         */
        fun getUnreadCount(): Flow<Int> = announcementDao.getUnreadCount()

        /**
         * Refresh announcements from the RSS feed.
         *
         * Fetches the latest announcements from usetrmnl.com and stores them
         * in the local database. Existing announcements are updated.
         *
         * @return Result indicating success or failure with error message.
         */
        suspend fun refreshAnnouncements(): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val channel = rssParser.getRssChannel(ANNOUNCEMENTS_FEED_URL)
                    val fetchedAt = Instant.now()

                    val announcements =
                        channel.items.map { item ->
                            AnnouncementEntity(
                                id = item.link ?: item.guid ?: throw IllegalStateException("Missing ID"),
                                title = item.title ?: "Untitled",
                                summary = item.description ?: "",
                                link = item.link ?: "",
                                publishedDate = item.pubDate?.let { parseDate(it) } ?: Instant.now(),
                                isRead = false,
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
                // RssParser already provides parsed date as String
                // We'll use a simple heuristic: if it contains T, it's ISO format
                if (dateString.contains('T')) {
                    Instant.parse(dateString)
                } else {
                    // Try parsing as epoch millis if it's all digits
                    dateString.toLongOrNull()?.let { Instant.ofEpochMilli(it) } ?: Instant.now()
                }
            } catch (e: Exception) {
                Instant.now()
            }
    }
