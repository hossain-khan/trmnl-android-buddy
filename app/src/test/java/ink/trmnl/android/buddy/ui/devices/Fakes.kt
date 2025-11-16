package ink.trmnl.android.buddy.ui.devices

import ink.trmnl.android.buddy.content.db.AnnouncementDao
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.content.db.BlogPostDao
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Fake DAOs
class FakeAnnouncementDao : AnnouncementDao {
    private val announcements = mutableMapOf<String, AnnouncementEntity>()
    val markedAsReadIds = mutableListOf<String>()

    override fun getAll(): Flow<List<AnnouncementEntity>> =
        flowOf(announcements.values.toList())

    override fun getLatest(limit: Int): Flow<List<AnnouncementEntity>> =
        flowOf(announcements.values.take(limit))

    override fun getUnread(): Flow<List<AnnouncementEntity>> =
        flowOf(announcements.values.filter { !it.isRead })

    override fun getRead(): Flow<List<AnnouncementEntity>> =
        flowOf(announcements.values.filter { it.isRead })

    override suspend fun insertAll(announcements: List<AnnouncementEntity>) {
        announcements.forEach { this.announcements[it.id] = it }
    }

    override suspend fun markAsRead(id: String) {
        markedAsReadIds.add(id)
        announcements[id]?.let {
            announcements[id] = it.copy(isRead = true)
        }
    }

    override suspend fun markAsUnread(id: String) {
        announcements[id]?.let {
            announcements[id] = it.copy(isRead = false)
        }
    }

    override suspend fun markAllAsRead() {
        announcements.keys.forEach { key ->
            announcements[key]?.let {
                announcements[key] = it.copy(isRead = true)
            }
        }
    }

    override suspend fun deleteOlderThan(threshold: Long) {
        announcements.entries.removeIf { it.value.fetchedAt < threshold }
    }

    override fun getUnreadCount(): Flow<Int> =
        flowOf(announcements.values.count { !it.isRead })
}

class FakeBlogPostDao : BlogPostDao {
    private val blogPosts = mutableMapOf<String, BlogPostEntity>()
    val markedAsReadIds = mutableListOf<String>()

    override fun getAll(): Flow<List<BlogPostEntity>> =
        flowOf(blogPosts.values.toList())

    override fun getLatest(limit: Int): Flow<List<BlogPostEntity>> =
        flowOf(blogPosts.values.take(limit))

    override fun getUnread(): Flow<List<BlogPostEntity>> =
        flowOf(blogPosts.values.filter { !it.isRead })

    override fun getRead(): Flow<List<BlogPostEntity>> =
        flowOf(blogPosts.values.filter { it.isRead })

    override fun getFavorites(): Flow<List<BlogPostEntity>> =
        flowOf(blogPosts.values.filter { it.isFavorite })

    override suspend fun insertAll(blogPosts: List<BlogPostEntity>) {
        blogPosts.forEach { this.blogPosts[it.id] = it }
    }

    override suspend fun markAsRead(id: String) {
        markedAsReadIds.add(id)
        blogPosts[id]?.let {
            blogPosts[id] = it.copy(isRead = true)
        }
    }

    override suspend fun markAsUnread(id: String) {
        blogPosts[id]?.let {
            blogPosts[id] = it.copy(isRead = false)
        }
    }

    override suspend fun markAllAsRead() {
        blogPosts.keys.forEach { key ->
            blogPosts[key]?.let {
                blogPosts[key] = it.copy(isRead = true)
            }
        }
    }

    override suspend fun toggleFavorite(id: String) {
        blogPosts[id]?.let {
            blogPosts[id] = it.copy(isFavorite = !it.isFavorite)
        }
    }

    override suspend fun deleteOlderThan(threshold: Long) {
        blogPosts.entries.removeIf { it.value.fetchedAt < threshold }
    }

    override fun getUnreadCount(): Flow<Int> =
        flowOf(blogPosts.values.count { !it.isRead })
}
