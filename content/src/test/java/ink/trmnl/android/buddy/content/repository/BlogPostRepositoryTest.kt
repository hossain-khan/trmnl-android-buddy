package ink.trmnl.android.buddy.content.repository

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import ink.trmnl.android.buddy.content.db.FakeBlogPostDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for BlogPostRepository.
 *
 * Tests repository logic using a fake DAO to avoid Room dependencies.
 */
class BlogPostRepositoryTest {
    private lateinit var dao: FakeBlogPostDao
    private lateinit var repository: BlogPostRepository

    @Before
    fun setUp() {
        dao = FakeBlogPostDao()
        repository = BlogPostRepository(dao)
    }

    @Test
    fun `getLatestPosts should return limited posts from dao`() =
        runTest {
            // Populate with test data
            val posts =
                listOf(
                    createTestPost(id = "1", publishedDate = Instant.now().minusSeconds(300)),
                    createTestPost(id = "2", publishedDate = Instant.now().minusSeconds(200)),
                    createTestPost(id = "3", publishedDate = Instant.now().minusSeconds(100)),
                )
            dao.insertAll(posts)

            val result = repository.getLatestPosts(2).first()

            assertThat(result).hasSize(2)
        }

    @Test
    fun `getAllPosts should return all posts from dao`() =
        runTest {
            val posts =
                listOf(
                    createTestPost(id = "1"),
                    createTestPost(id = "2"),
                    createTestPost(id = "3"),
                )
            dao.insertAll(posts)

            val result = repository.getAllPosts().first()

            assertThat(result).hasSize(3)
        }

    @Test
    fun `getPostsByCategory should filter by category`() =
        runTest {
            val posts =
                listOf(
                    createTestPost(id = "1", category = "Tutorial"),
                    createTestPost(id = "2", category = "Feature"),
                    createTestPost(id = "3", category = "Tutorial"),
                )
            dao.insertAll(posts)

            val result = repository.getPostsByCategory("Tutorial").first()

            assertThat(result).hasSize(2)
        }

    @Test
    fun `getFavoritePosts should return only favorites`() =
        runTest {
            val posts =
                listOf(
                    createTestPost(id = "1", isFavorite = true),
                    createTestPost(id = "2", isFavorite = false),
                )
            dao.insertAll(posts)

            val result = repository.getFavoritePosts().first()

            assertThat(result).hasSize(1)
        }

    @Test
    fun `getUnreadPosts should return only unread`() =
        runTest {
            val posts =
                listOf(
                    createTestPost(id = "1", isRead = false),
                    createTestPost(id = "2", isRead = true),
                )
            dao.insertAll(posts)

            val result = repository.getUnreadPosts().first()

            assertThat(result).hasSize(1)
        }

    @Test
    fun `markAsRead should delegate to dao`() =
        runTest {
            val post = createTestPost(id = "1", isRead = false)
            dao.insertAll(listOf(post))

            repository.markAsRead("1")

            val result = dao.getAll().first()
            assertThat(result[0].isRead).isEqualTo(true)
            assertThat(result[0].lastReadAt).isNotNull()
        }

    @Test
    fun `updateReadingProgress should delegate to dao`() =
        runTest {
            val post = createTestPost(id = "1", readingProgressPercent = 0f)
            dao.insertAll(listOf(post))

            repository.updateReadingProgress("1", 75f)

            val result = dao.getAll().first()
            assertThat(result[0].readingProgressPercent).isEqualTo(75f)
        }

    @Test
    fun `toggleFavorite should delegate to dao`() =
        runTest {
            val post = createTestPost(id = "1", isFavorite = false)
            dao.insertAll(listOf(post))

            repository.toggleFavorite("1")

            val result = dao.getAll().first()
            assertThat(result[0].isFavorite).isEqualTo(true)
        }

    @Test
    fun `searchPosts should delegate to dao`() =
        runTest {
            val posts =
                listOf(
                    createTestPost(id = "1", title = "Kotlin Guide"),
                    createTestPost(id = "2", title = "Java Tutorial"),
                )
            dao.insertAll(posts)

            val result = repository.searchPosts("Kotlin").first()

            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo("Kotlin Guide")
        }

    @Test
    fun `getUnreadCount should delegate to dao`() =
        runTest {
            val posts =
                listOf(
                    createTestPost(id = "1", isRead = false),
                    createTestPost(id = "2", isRead = false),
                    createTestPost(id = "3", isRead = true),
                )
            dao.insertAll(posts)

            val result = repository.getUnreadCount().first()

            assertThat(result).isEqualTo(2)
        }

    @Test
    fun `getCategories should delegate to dao`() =
        runTest {
            val posts =
                listOf(
                    createTestPost(id = "1", category = "Tutorial"),
                    createTestPost(id = "2", category = "Feature"),
                    createTestPost(id = "3", category = "Tutorial"),
                )
            dao.insertAll(posts)

            val result = repository.getCategories().first()

            assertThat(result).hasSize(2)
        }

    // Note: refreshPosts() requires network access and RSS parsing, which is difficult to test
    // in unit tests. This would typically be tested with integration tests or by mocking
    // the RssParser dependency. For now, we focus on testing the DAO delegation logic.

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
