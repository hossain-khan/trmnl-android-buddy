package ink.trmnl.android.buddy.content.repository

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import ink.trmnl.android.buddy.content.db.AnnouncementDao
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.content.db.BlogPostDao
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import ink.trmnl.android.buddy.content.models.ContentItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [ContentFeedRepository].
 */
class ContentFeedRepositoryTest {
    private lateinit var fakeAnnouncementDao: FakeAnnouncementDao
    private lateinit var fakeBlogPostDao: FakeBlogPostDao
    private lateinit var repository: ContentFeedRepository

    @Before
    fun setup() {
        fakeAnnouncementDao = FakeAnnouncementDao()
        fakeBlogPostDao = FakeBlogPostDao()
        repository = ContentFeedRepository(fakeAnnouncementDao, fakeBlogPostDao)
    }

    @Test
    fun `getLatestContent combines and sorts announcements and blog posts`() =
        runTest {
            // Setup test data with specific dates
            val now = Instant.now()
            val announcement1 =
                createAnnouncement(
                    "a1",
                    "Announcement 1",
                    now.minusSeconds(100),
                )
            val blogPost1 = createBlogPost("b1", "Blog 1", now.minusSeconds(50))
            val announcement2 =
                createAnnouncement(
                    "a2",
                    "Announcement 2",
                    now.minusSeconds(200),
                )

            fakeAnnouncementDao.insertAll(listOf(announcement1, announcement2))
            fakeBlogPostDao.insertAll(listOf(blogPost1))

            val result = repository.getLatestContent(3).first()

            // Should be sorted by date (newest first)
            assertThat(result).hasSize(3)
            assertThat(result[0].id).isEqualTo("b1") // Most recent
            assertThat(result[1].id).isEqualTo("a1")
            assertThat(result[2].id).isEqualTo("a2") // Oldest
        }

    @Test
    fun `getLatestContent respects limit parameter`() =
        runTest {
            // Setup more data than limit
            val now = Instant.now()
            fakeAnnouncementDao.insertAll(
                listOf(
                    createAnnouncement("a1", "Announcement 1", now),
                    createAnnouncement("a2", "Announcement 2", now.minusSeconds(100)),
                ),
            )
            fakeBlogPostDao.insertAll(
                listOf(
                    createBlogPost("b1", "Blog 1", now.minusSeconds(50)),
                    createBlogPost("b2", "Blog 2", now.minusSeconds(150)),
                ),
            )

            val result = repository.getLatestContent(2).first()

            assertThat(result).hasSize(2)
        }

    @Test
    fun `getLatestContent converts announcements to ContentItem Announcement`() =
        runTest {
            val announcement = createAnnouncement("a1", "Test Announcement", Instant.now())
            fakeAnnouncementDao.insertAll(listOf(announcement))

            val result = repository.getLatestContent(10).first()

            assertThat(result).hasSize(1)
            assertThat(result[0]).isInstanceOf(ContentItem.Announcement::class)
            val item = result[0] as ContentItem.Announcement
            assertThat(item.id).isEqualTo("a1")
            assertThat(item.title).isEqualTo("Test Announcement")
        }

    @Test
    fun `getLatestContent converts blog posts to ContentItem BlogPost`() =
        runTest {
            val blogPost = createBlogPost("b1", "Test Blog Post", Instant.now())
            fakeBlogPostDao.insertAll(listOf(blogPost))

            val result = repository.getLatestContent(10).first()

            assertThat(result).hasSize(1)
            assertThat(result[0]).isInstanceOf(ContentItem.BlogPost::class)
            val item = result[0] as ContentItem.BlogPost
            assertThat(item.id).isEqualTo("b1")
            assertThat(item.title).isEqualTo("Test Blog Post")
            assertThat(item.authorName).isEqualTo("Test Author")
        }

    @Test
    fun `getLatestUnreadContent filters only unread items`() =
        runTest {
            val now = Instant.now()
            fakeAnnouncementDao.insertAll(
                listOf(
                    createAnnouncement("a1", "Unread Announcement", now, isRead = false),
                    createAnnouncement("a2", "Read Announcement", now.minusSeconds(50), isRead = true),
                ),
            )
            fakeBlogPostDao.insertAll(
                listOf(
                    createBlogPost("b1", "Unread Blog", now.minusSeconds(25), isRead = false),
                    createBlogPost("b2", "Read Blog", now.minusSeconds(75), isRead = true),
                ),
            )

            val result = repository.getLatestUnreadContent(10).first()

            assertThat(result).hasSize(2)
            // All items should be unread
            result.forEach { item ->
                assertThat(item.isRead).isEqualTo(false)
            }
        }

    @Test
    fun `getLatestUnreadContent sorts unread items by date`() =
        runTest {
            val now = Instant.now()
            fakeAnnouncementDao.insertAll(
                listOf(
                    createAnnouncement("a1", "Older Unread", now.minusSeconds(100), isRead = false),
                ),
            )
            fakeBlogPostDao.insertAll(
                listOf(
                    createBlogPost("b1", "Newer Unread", now, isRead = false),
                ),
            )

            val result = repository.getLatestUnreadContent(10).first()

            assertThat(result).hasSize(2)
            assertThat(result[0].id).isEqualTo("b1") // Newer first
            assertThat(result[1].id).isEqualTo("a1")
        }

    @Test
    fun `getUnreadCount combines counts from both sources`() =
        runTest {
            fakeAnnouncementDao.insertAll(
                listOf(
                    createAnnouncement("a1", "Unread 1", Instant.now(), isRead = false),
                    createAnnouncement("a2", "Read", Instant.now(), isRead = true),
                ),
            )
            fakeBlogPostDao.insertAll(
                listOf(
                    createBlogPost("b1", "Unread 1", Instant.now(), isRead = false),
                    createBlogPost("b2", "Unread 2", Instant.now(), isRead = false),
                    createBlogPost("b3", "Read", Instant.now(), isRead = true),
                ),
            )

            val count = repository.getUnreadCount().first()

            // 1 unread announcement + 2 unread blog posts = 3
            assertThat(count).isEqualTo(3)
        }

    @Test
    fun `getLatestContent returns empty list when no content`() =
        runTest {
            val result = repository.getLatestContent(10).first()

            assertThat(result).hasSize(0)
        }

    private fun createAnnouncement(
        id: String,
        title: String,
        publishedDate: Instant,
        isRead: Boolean = false,
    ) = AnnouncementEntity(
        id = id,
        title = title,
        summary = "Summary for $title",
        link = "https://example.com/$id",
        publishedDate = publishedDate,
        isRead = isRead,
        fetchedAt = Instant.now(),
    )

    private fun createBlogPost(
        id: String,
        title: String,
        publishedDate: Instant,
        isRead: Boolean = false,
    ) = BlogPostEntity(
        id = id,
        title = title,
        summary = "Summary for $title",
        link = "https://example.com/$id",
        authorName = "Test Author",
        category = "Test Category",
        publishedDate = publishedDate,
        featuredImageUrl = null,
        isRead = isRead,
        isFavorite = false,
        fetchedAt = Instant.now(),
    )

    /**
     * Fake implementation of AnnouncementDao for testing.
     */
    private class FakeAnnouncementDao : AnnouncementDao {
        private val announcements = mutableListOf<AnnouncementEntity>()

        override fun getAll(): Flow<List<AnnouncementEntity>> = flowOf(announcements.toList())

        override fun getLatest(limit: Int): Flow<List<AnnouncementEntity>> = flowOf(announcements.take(limit))

        override fun getUnread(): Flow<List<AnnouncementEntity>> = flowOf(announcements.filter { !it.isRead })

        override fun getRead(): Flow<List<AnnouncementEntity>> = flowOf(announcements.filter { it.isRead })

        override suspend fun insertAll(announcements: List<AnnouncementEntity>) {
            this.announcements.addAll(announcements)
        }

        override suspend fun markAsRead(id: String) {
            val index = announcements.indexOfFirst { it.id == id }
            if (index >= 0) {
                announcements[index] = announcements[index].copy(isRead = true)
            }
        }

        override suspend fun markAsUnread(id: String) {
            val index = announcements.indexOfFirst { it.id == id }
            if (index >= 0) {
                announcements[index] = announcements[index].copy(isRead = false)
            }
        }

        override suspend fun markAllAsRead() {
            announcements.forEachIndexed { index, announcement ->
                announcements[index] = announcement.copy(isRead = true)
            }
        }

        override suspend fun deleteOlderThan(threshold: Long) {
            announcements.removeIf { it.fetchedAt.epochSecond < threshold }
        }

        override fun getUnreadCount(): Flow<Int> = flowOf(announcements.count { !it.isRead })
    }

    /**
     * Fake implementation of BlogPostDao for testing.
     */
    private class FakeBlogPostDao : BlogPostDao {
        private val posts = mutableListOf<BlogPostEntity>()

        override suspend fun insertAll(posts: List<BlogPostEntity>) {
            this.posts.addAll(posts)
        }

        override suspend fun updateSummary(
            id: String,
            summary: String,
        ) {
            posts.find { it.id == id }?.let {
                val index = posts.indexOf(it)
                posts[index] = it.copy(summary = summary)
            }
        }

        override fun getAll() = flowOf(posts.toList())

        override fun getByCategory(category: String) = flowOf(posts.filter { it.category == category })

        override fun getFavorites() = flowOf(posts.filter { it.isFavorite })

        override fun getUnread() = flowOf(posts.filter { !it.isRead })

        override fun getRecentlyRead() =
            flowOf(
                posts
                    .filter { it.isRead }
                    .sortedByDescending { it.lastReadAt }
                    .take(10),
            )

        override fun searchPosts(query: String) =
            flowOf(
                posts.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.summary.contains(query, ignoreCase = true)
                },
            )

        override suspend fun markAsRead(id: String) {
            posts.find { it.id == id }?.let {
                val index = posts.indexOf(it)
                posts[index] = it.copy(isRead = true, lastReadAt = Instant.now())
            }
        }

        override suspend fun markAllAsRead(timestamp: Instant) {
            posts.forEachIndexed { index, post ->
                if (!post.isRead) {
                    posts[index] = post.copy(isRead = true, lastReadAt = timestamp)
                }
            }
        }

        override fun getUnreadCount() = flowOf(posts.count { !it.isRead })

        override suspend fun updateReadingProgress(
            id: String,
            progress: Float,
            timestamp: Instant,
        ) {
            posts.find { it.id == id }?.let {
                val index = posts.indexOf(it)
                posts[index] = it.copy(lastReadAt = timestamp)
            }
        }

        override suspend fun toggleFavorite(id: String) {
            posts.find { it.id == id }?.let {
                val index = posts.indexOf(it)
                posts[index] = it.copy(isFavorite = !it.isFavorite)
            }
        }

        override suspend fun deleteOlderThan(threshold: Long) {
            posts.removeAll { it.fetchedAt.epochSecond < threshold }
        }
    }
}
