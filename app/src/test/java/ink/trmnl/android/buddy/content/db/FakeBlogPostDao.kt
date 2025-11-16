package ink.trmnl.android.buddy.content.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Fake implementation of [BlogPostDao] for testing.
 *
 * Uses an in-memory map to store blog posts without requiring a real database.
 */
class FakeBlogPostDao : BlogPostDao {
    private val posts = MutableStateFlow<Map<String, BlogPostEntity>>(emptyMap())

    override fun getAll(): Flow<List<BlogPostEntity>> =
        posts.map { map ->
            map.values.sortedByDescending { it.publishedDate }
        }

    override fun getByCategory(category: String): Flow<List<BlogPostEntity>> =
        posts.map { map ->
            map.values.filter { it.category == category }.sortedByDescending { it.publishedDate }
        }

    override fun getFavorites(): Flow<List<BlogPostEntity>> =
        posts.map { map ->
            map.values.filter { it.isFavorite }.sortedByDescending { it.publishedDate }
        }

    override fun getUnread(): Flow<List<BlogPostEntity>> =
        posts.map { map ->
            map.values.filter { !it.isRead }.sortedByDescending { it.publishedDate }
        }

    override fun getRecentlyRead(): Flow<List<BlogPostEntity>> =
        posts.map { map ->
            map.values
                .filter { it.lastReadAt != null }
                .sortedByDescending { it.lastReadAt }
                .take(10)
        }

    override suspend fun insertAll(posts: List<BlogPostEntity>) {
        val newMap = this.posts.value.toMutableMap()
        posts.forEach { post ->
            // IGNORE strategy: only insert if ID doesn't exist
            if (!newMap.containsKey(post.id)) {
                newMap[post.id] = post
            }
        }
        this.posts.value = newMap
    }

    override suspend fun markAsRead(id: String) {
        val current = posts.value[id] ?: return
        posts.value = posts.value + (id to current.copy(isRead = true))
    }

    override suspend fun markAllAsRead(timestamp: Instant) {
        val updatedMap =
            posts.value.mapValues { (_, post) ->
                if (post.isRead) {
                    post
                } else {
                    post.copy(isRead = true, lastReadAt = timestamp)
                }
            }
        posts.value = updatedMap
    }

    override fun getUnreadCount(): Flow<Int> =
        posts.map { map ->
            map.values.count { !it.isRead }
        }

    override suspend fun updateReadingProgress(
        id: String,
        progress: Float,
        timestamp: Instant,
    ) {
        val current = posts.value[id] ?: return
        posts.value =
            posts.value + (
                id to
                    current.copy(
                        readingProgressPercent = progress,
                        lastReadAt = timestamp,
                    )
            )
    }

    override suspend fun toggleFavorite(id: String) {
        val current = posts.value[id] ?: return
        posts.value = posts.value + (id to current.copy(isFavorite = !current.isFavorite))
    }

    override suspend fun updateSummary(
        id: String,
        summary: String,
    ) {
        val current = posts.value[id] ?: return
        posts.value = posts.value + (id to current.copy(summary = summary))
    }

    override suspend fun deleteOlderThan(threshold: Long) {
        posts.value =
            posts.value.filterValues { post ->
                post.fetchedAt.epochSecond >= threshold
            }
    }

    override fun searchPosts(query: String): Flow<List<BlogPostEntity>> =
        posts.map { map ->
            map.values
                .filter { post ->
                    post.title.contains(query, ignoreCase = true) ||
                        post.summary.contains(query, ignoreCase = true)
                }.sortedByDescending { it.publishedDate }
        }
}
