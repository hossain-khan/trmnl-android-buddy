package ink.trmnl.android.buddy.content.db

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for BlogPostDao.
 *
 * Tests basic CRUD operations and queries without requiring Android components.
 */
class BlogPostDaoTest {
    private lateinit var dao: FakeBlogPostDao

    @Before
    fun setUp() {
        dao = FakeBlogPostDao()
    }

    @Test
    fun `insertAll should store blog posts`() =
        runTest {
            val posts =
                listOf(
                    createTestPost(id = "1", title = "Post 1"),
                    createTestPost(id = "2", title = "Post 2"),
                )

            dao.insertAll(posts)

            val result = dao.getAll().first()
            assertThat(result).hasSize(2)
        }

    @Test
    fun `getLatest should return limited number of posts`() =
        runTest {
            val posts =
                listOf(
                    createTestPost(id = "1", publishedDate = Instant.now().minusSeconds(300)),
                    createTestPost(id = "2", publishedDate = Instant.now().minusSeconds(200)),
                    createTestPost(id = "3", publishedDate = Instant.now().minusSeconds(100)),
                    createTestPost(id = "4", publishedDate = Instant.now()),
                )

            dao.insertAll(posts)

            val result = dao.getLatest(2).first()
            assertThat(result).hasSize(2)
            assertThat(result[0].id).isEqualTo("4") // Newest first
            assertThat(result[1].id).isEqualTo("3")
        }

    @Test
    fun `getByCategory should filter posts by category`() =
        runTest {
            val posts =
                listOf(
                    createTestPost(id = "1", category = "Tutorial"),
                    createTestPost(id = "2", category = "Feature"),
                    createTestPost(id = "3", category = "Tutorial"),
                )

            dao.insertAll(posts)

            val result = dao.getByCategory("Tutorial").first()
            assertThat(result).hasSize(2)
        }

    @Test
    fun `getFavorites should return only favorited posts`() =
        runTest {
            val posts =
                listOf(
                    createTestPost(id = "1", isFavorite = true),
                    createTestPost(id = "2", isFavorite = false),
                    createTestPost(id = "3", isFavorite = true),
                )

            dao.insertAll(posts)

            val result = dao.getFavorites().first()
            assertThat(result).hasSize(2)
        }

    @Test
    fun `getUnread should return only unread posts`() =
        runTest {
            val posts =
                listOf(
                    createTestPost(id = "1", isRead = false),
                    createTestPost(id = "2", isRead = true),
                    createTestPost(id = "3", isRead = false),
                )

            dao.insertAll(posts)

            val result = dao.getUnread().first()
            assertThat(result).hasSize(2)
        }

    @Test
    fun `markAsRead should update read status`() =
        runTest {
            val post = createTestPost(id = "1", isRead = false)
            dao.insertAll(listOf(post))

            dao.markAsRead("1")

            val result = dao.getAll().first()
            assertThat(result[0].isRead).isTrue()
            assertThat(result[0].lastReadAt).isNotNull()
        }

    @Test
    fun `updateReadingProgress should update progress`() =
        runTest {
            val post = createTestPost(id = "1", readingProgressPercent = 0f)
            dao.insertAll(listOf(post))

            dao.updateReadingProgress("1", 50f)

            val result = dao.getAll().first()
            assertThat(result[0].readingProgressPercent).isEqualTo(50f)
        }

    @Test
    fun `toggleFavorite should toggle favorite status`() =
        runTest {
            val post = createTestPost(id = "1", isFavorite = false)
            dao.insertAll(listOf(post))

            dao.toggleFavorite("1")
            var result = dao.getAll().first()
            assertThat(result[0].isFavorite).isTrue()

            dao.toggleFavorite("1")
            result = dao.getAll().first()
            assertThat(result[0].isFavorite).isFalse()
        }

    @Test
    fun `searchPosts should find matching posts`() =
        runTest {
            val posts =
                listOf(
                    createTestPost(id = "1", title = "Kotlin Tutorial"),
                    createTestPost(id = "2", title = "Java Guide"),
                    createTestPost(id = "3", summary = "Learn Kotlin basics"),
                )

            dao.insertAll(posts)

            val result = dao.searchPosts("Kotlin").first()
            assertThat(result).hasSize(2)
        }

    @Test
    fun `getCategories should return unique categories`() =
        runTest {
            val posts =
                listOf(
                    createTestPost(id = "1", category = "Tutorial"),
                    createTestPost(id = "2", category = "Feature"),
                    createTestPost(id = "3", category = "Tutorial"),
                    createTestPost(id = "4", category = null),
                )

            dao.insertAll(posts)

            val result = dao.getCategories().first()
            assertThat(result).hasSize(2)
        }

    @Test
    fun `deleteOlderThan should remove old posts`() =
        runTest {
            val oldTimestamp = Instant.now().minusSeconds(7200).epochSecond
            val recentTimestamp = Instant.now().epochSecond

            val posts =
                listOf(
                    createTestPost(id = "1", fetchedAt = Instant.ofEpochSecond(oldTimestamp - 1000)),
                    createTestPost(id = "2", fetchedAt = Instant.ofEpochSecond(recentTimestamp)),
                )

            dao.insertAll(posts)

            dao.deleteOlderThan(oldTimestamp)

            val result = dao.getAll().first()
            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo("2")
        }

    @Test
    fun `replace strategy should update existing posts`() =
        runTest {
            val post1 = createTestPost(id = "1", title = "Original Title")
            dao.insertAll(listOf(post1))

            val post2 = createTestPost(id = "1", title = "Updated Title")
            dao.insertAll(listOf(post2))

            val result = dao.getAll().first()
            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo("Updated Title")
        }

    private fun createTestPost(
        id: String,
        title: String = "Test Post",
        summary: String = "Test summary",
        link: String = "https://example.com/post",
        authorName: String = "Test Author",
        category: String? = "Test",
        publishedDate: Instant = Instant.now(),
        featuredImageUrl: String? = null,
        isRead: Boolean = false,
        readingProgressPercent: Float = 0f,
        lastReadAt: Instant? = null,
        fetchedAt: Instant = Instant.now(),
        isFavorite: Boolean = false,
    ): BlogPostEntity =
        BlogPostEntity(
            id = id,
            title = title,
            summary = summary,
            link = link,
            authorName = authorName,
            category = category,
            publishedDate = publishedDate,
            featuredImageUrl = featuredImageUrl,
            isRead = isRead,
            readingProgressPercent = readingProgressPercent,
            lastReadAt = lastReadAt,
            fetchedAt = fetchedAt,
            isFavorite = isFavorite,
        )
}

/**
 * Fake implementation of BlogPostDao for testing.
 * Provides in-memory storage for testing without requiring Room database.
 */
class FakeBlogPostDao : BlogPostDao {
    private val posts = mutableListOf<BlogPostEntity>()

    override fun getAll() = kotlinx.coroutines.flow.flowOf(posts.sortedByDescending { it.publishedDate })

    override fun getLatest(limit: Int) = kotlinx.coroutines.flow.flowOf(posts.sortedByDescending { it.publishedDate }.take(limit))

    override fun getByCategory(category: String) =
        kotlinx.coroutines.flow.flowOf(
            posts
                .filter { it.category == category }
                .sortedByDescending { it.publishedDate },
        )

    override fun getFavorites() = kotlinx.coroutines.flow.flowOf(posts.filter { it.isFavorite }.sortedByDescending { it.publishedDate })

    override fun getUnread() = kotlinx.coroutines.flow.flowOf(posts.filter { !it.isRead }.sortedByDescending { it.publishedDate })

    override fun getRecentlyRead() =
        kotlinx.coroutines.flow.flowOf(
            posts
                .filter { it.lastReadAt != null }
                .sortedByDescending { it.lastReadAt },
        )

    override suspend fun insertAll(posts: List<BlogPostEntity>) {
        posts.forEach { newPost ->
            val index = this.posts.indexOfFirst { it.id == newPost.id }
            if (index >= 0) {
                this.posts[index] = newPost
            } else {
                this.posts.add(newPost)
            }
        }
    }

    override suspend fun markAsRead(
        id: String,
        timestamp: Long,
    ) {
        val index = posts.indexOfFirst { it.id == id }
        if (index >= 0) {
            posts[index] =
                posts[index].copy(
                    isRead = true,
                    lastReadAt = Instant.ofEpochSecond(timestamp),
                )
        }
    }

    override suspend fun updateReadingProgress(
        id: String,
        progress: Float,
    ) {
        val index = posts.indexOfFirst { it.id == id }
        if (index >= 0) {
            posts[index] = posts[index].copy(readingProgressPercent = progress)
        }
    }

    override suspend fun toggleFavorite(id: String) {
        val index = posts.indexOfFirst { it.id == id }
        if (index >= 0) {
            posts[index] = posts[index].copy(isFavorite = !posts[index].isFavorite)
        }
    }

    override suspend fun deleteOlderThan(threshold: Long) {
        posts.removeIf { it.fetchedAt.epochSecond < threshold }
    }

    override fun searchPosts(query: String) =
        kotlinx.coroutines.flow.flowOf(
            posts
                .filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.summary.contains(query, ignoreCase = true) ||
                        it.authorName.contains(query, ignoreCase = true)
                }.sortedByDescending { it.publishedDate },
        )

    override fun getUnreadCount() = kotlinx.coroutines.flow.flowOf(posts.count { !it.isRead })

    override fun getCategories() =
        kotlinx.coroutines.flow.flowOf(
            posts
                .mapNotNull { it.category }
                .distinct()
                .sorted(),
        )
}
