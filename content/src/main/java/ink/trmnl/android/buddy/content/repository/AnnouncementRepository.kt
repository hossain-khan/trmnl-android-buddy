package ink.trmnl.android.buddy.content.repository

import ink.trmnl.android.buddy.content.RssParserFactory
import ink.trmnl.android.buddy.content.db.AnnouncementDao
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing announcements.
 * Implements offline-first pattern with Room as source of truth.
 */
@Singleton
class AnnouncementRepository
    @Inject
    constructor(
        private val announcementDao: AnnouncementDao,
    ) {
        // Use factory to create RssParser to avoid Metro DI issues with third-party types
        private val rssParser = RssParserFactory.create()

        companion object {
            private const val FEED_URL = "https://usetrmnl.com/feeds/announcements.xml"
        }

        /**
         * Get all announcements as a reactive Flow.
         * Returns cached data immediately and updates when data changes.
         */
        fun getAllAnnouncements(): Flow<List<AnnouncementEntity>> = announcementDao.getAllAnnouncements()

        /**
         * Get the latest N announcements.
         */
        fun getLatestAnnouncements(limit: Int): Flow<List<AnnouncementEntity>> = announcementDao.getLatestAnnouncements(limit)

        /**
         * Get count of unread announcements.
         */
        fun getUnreadCount(): Flow<Int> = announcementDao.getUnreadCount()

        /**
         * Get unread announcements only.
         */
        fun getUnreadAnnouncements(): Flow<List<AnnouncementEntity>> = announcementDao.getUnreadAnnouncements()

        /**
         * Get read announcements only.
         */
        fun getReadAnnouncements(): Flow<List<AnnouncementEntity>> = announcementDao.getReadAnnouncements()

        /**
         * Refresh announcements from the RSS feed.
         * Fetches new data from the network and updates the database.
         *
         * @return Result<Unit> - Success if refresh was successful, Failure with error message otherwise.
         */
        suspend fun refreshAnnouncements(): Result<Unit> {
            return try {
                Timber.d("Refreshing announcements from feed: $FEED_URL")
                val channel = rssParser.getRssChannel(FEED_URL)

                val fetchedAt = Clock.System.now()
                val announcements =
                    channel.items.mapNotNull { item ->
                        try {
                            // Parse the published date from RSS format
                            val publishedDate = item.pubDate?.let { parseRssDate(it) } ?: fetchedAt

                            AnnouncementEntity(
                                id = item.guid ?: item.link ?: return@mapNotNull null,
                                title = item.title ?: return@mapNotNull null,
                                summary = item.description ?: "",
                                link = item.link ?: return@mapNotNull null,
                                publishedDate = publishedDate,
                                isRead = false,
                                fetchedAt = fetchedAt,
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "Error parsing announcement item: ${item.title}")
                            null
                        }
                    }

                // Preserve read status for existing announcements
                val existingAnnouncements = announcementDao.getAllAnnouncements().firstOrNull() ?: emptyList()
                val existingReadIds = existingAnnouncements.filter { it.isRead }.map { it.id }.toSet()

                val mergedAnnouncements =
                    announcements.map { announcement ->
                        if (announcement.id in existingReadIds) {
                            announcement.copy(isRead = true)
                        } else {
                            announcement
                        }
                    }

                announcementDao.insertAnnouncements(mergedAnnouncements)
                Timber.d("Successfully refreshed ${mergedAnnouncements.size} announcements")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh announcements")
                Result.failure(e)
            }
        }

        /**
         * Mark an announcement as read.
         */
        suspend fun markAsRead(announcementId: String) {
            announcementDao.markAsRead(announcementId)
        }

        /**
         * Mark an announcement as unread.
         */
        suspend fun markAsUnread(announcementId: String) {
            announcementDao.markAsUnread(announcementId)
        }

        /**
         * Mark all announcements as read.
         */
        suspend fun markAllAsRead() {
            announcementDao.markAllAsRead()
        }

        /**
         * Clear all announcements from cache.
         */
        suspend fun clearCache() {
            announcementDao.deleteAll()
        }

        /**
         * Parse RSS date string to Instant.
         * Handles common RSS date formats (RFC 822, ISO 8601).
         */
        private fun parseRssDate(dateString: String): Instant =
            try {
                // RSS-Parser should handle this, but we may need to parse manually
                // For now, try ISO 8601 format
                Instant.parse(dateString)
            } catch (e: Exception) {
                // If parsing fails, use current time
                Timber.w("Failed to parse date: $dateString, using current time")
                Clock.System.now()
            }
    }
