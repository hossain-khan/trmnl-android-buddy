package ink.trmnl.android.buddy.content.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of [AnnouncementDao] for testing.
 *
 * Uses an in-memory map to store announcements without requiring a real database.
 */
class FakeAnnouncementDao : AnnouncementDao {
    private val announcements = MutableStateFlow<Map<String, AnnouncementEntity>>(emptyMap())

    override fun getAll(): Flow<List<AnnouncementEntity>> =
        announcements.map { map ->
            map.values.sortedByDescending { it.publishedDate }
        }

    override fun getLatest(limit: Int): Flow<List<AnnouncementEntity>> =
        announcements.map { map ->
            map.values.sortedByDescending { it.publishedDate }.take(limit)
        }

    override fun getUnread(): Flow<List<AnnouncementEntity>> =
        announcements.map { map ->
            map.values.filter { !it.isRead }.sortedByDescending { it.publishedDate }
        }

    override fun getRead(): Flow<List<AnnouncementEntity>> =
        announcements.map { map ->
            map.values.filter { it.isRead }.sortedByDescending { it.publishedDate }
        }

    override suspend fun insertAll(announcements: List<AnnouncementEntity>) {
        val newMap = this.announcements.value.toMutableMap()
        announcements.forEach { announcement ->
            newMap[announcement.id] = announcement
        }
        this.announcements.value = newMap
    }

    override suspend fun markAsRead(id: String) {
        val current = announcements.value[id] ?: return
        announcements.value = announcements.value + (id to current.copy(isRead = true))
    }

    override suspend fun markAsUnread(id: String) {
        val current = announcements.value[id] ?: return
        announcements.value = announcements.value + (id to current.copy(isRead = false))
    }

    override suspend fun markAllAsRead() {
        val updatedMap =
            announcements.value.mapValues { (_, announcement) ->
                announcement.copy(isRead = true)
            }
        announcements.value = updatedMap
    }

    override suspend fun deleteOlderThan(threshold: Long) {
        announcements.value =
            announcements.value.filterValues { announcement ->
                announcement.fetchedAt.epochSecond >= threshold
            }
    }

    override fun getUnreadCount(): Flow<Int> =
        announcements.map { map ->
            map.values.count { !it.isRead }
        }
}
