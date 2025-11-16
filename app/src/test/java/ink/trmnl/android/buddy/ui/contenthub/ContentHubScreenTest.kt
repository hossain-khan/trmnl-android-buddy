package ink.trmnl.android.buddy.ui.contenthub

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.content.db.AnnouncementDao
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.content.db.BlogPostDao
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import ink.trmnl.android.buddy.content.repository.AnnouncementRepository
import ink.trmnl.android.buddy.content.repository.BlogPostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

/**
 * Tests for ContentHubScreen presenter.
 *
 * Verifies:
 * - Initial state with default tab selection
 * - Tab switching between announcements and blog posts
 * - Unread count tracking for both content types
 * - Navigation (back button)
 * - Edge cases with no content
 */
class ContentHubScreenTest {
    @Test
    fun `presenter loads with default announcements tab selected`() =
        runTest {
            // Given
            val navigator = FakeNavigator(ContentHubScreen)
            val announcementDao = FakeAnnouncementDao()
            val blogPostDao = FakeBlogPostDao()
            val announcementRepository = AnnouncementRepository(announcementDao)
            val blogPostRepository = BlogPostRepository(blogPostDao)
            val presenter = ContentHubPresenter(navigator, announcementRepository, blogPostRepository)

            // When/Then
            presenter.test {
                val state = awaitItem()

                assertThat(state.selectedTab).isEqualTo(ContentHubScreen.Tab.ANNOUNCEMENTS)
                assertThat(state.announcementsUnreadCount).isEqualTo(0)
                assertThat(state.blogPostsUnreadCount).isEqualTo(0)
            }
        }

    @Test
    fun `tab selection switches between announcements and blog posts`() =
        runTest {
            // Given
            val navigator = FakeNavigator(ContentHubScreen)
            val announcementDao = FakeAnnouncementDao()
            val blogPostDao = FakeBlogPostDao()
            val announcementRepository = AnnouncementRepository(announcementDao)
            val blogPostRepository = BlogPostRepository(blogPostDao)
            val presenter = ContentHubPresenter(navigator, announcementRepository, blogPostRepository)

            // When/Then
            presenter.test {
                val initialState = awaitItem()
                assertThat(initialState.selectedTab).isEqualTo(ContentHubScreen.Tab.ANNOUNCEMENTS)

                // Switch to blog posts tab
                initialState.eventSink(ContentHubScreen.Event.TabSelected(ContentHubScreen.Tab.BLOG_POSTS))

                val blogPostsState = awaitItem()
                assertThat(blogPostsState.selectedTab).isEqualTo(ContentHubScreen.Tab.BLOG_POSTS)

                // Switch back to announcements
                blogPostsState.eventSink(ContentHubScreen.Event.TabSelected(ContentHubScreen.Tab.ANNOUNCEMENTS))

                val announcementsState = awaitItem()
                assertThat(announcementsState.selectedTab).isEqualTo(ContentHubScreen.Tab.ANNOUNCEMENTS)
            }
        }

    @Test
    fun `unread count updates for announcements`() =
        runTest {
            // Given
            val navigator = FakeNavigator(ContentHubScreen)
            val announcementDao = FakeAnnouncementDao()
            val blogPostDao = FakeBlogPostDao()

            val announcementRepository = AnnouncementRepository(announcementDao)
            val blogPostRepository = BlogPostRepository(blogPostDao)
            val presenter = ContentHubPresenter(navigator, announcementRepository, blogPostRepository)

            // When/Then
            presenter.test {
                // First emission should have initial values (0)
                val initialState = awaitItem()
                assertThat(initialState.announcementsUnreadCount).isEqualTo(0)

                // Add unread announcements after presenter is collecting
                announcementDao.addUnreadAnnouncements(5)

                // Should see updated unread count
                val updatedState = awaitItem()
                assertThat(updatedState.announcementsUnreadCount).isEqualTo(5)
            }
        }

    @Test
    fun `unread count updates for blog posts`() =
        runTest {
            // Given
            val navigator = FakeNavigator(ContentHubScreen)
            val announcementDao = FakeAnnouncementDao()
            val blogPostDao = FakeBlogPostDao()

            val announcementRepository = AnnouncementRepository(announcementDao)
            val blogPostRepository = BlogPostRepository(blogPostDao)
            val presenter = ContentHubPresenter(navigator, announcementRepository, blogPostRepository)

            // When/Then
            presenter.test {
                // First emission should have initial values (0)
                val initialState = awaitItem()
                assertThat(initialState.blogPostsUnreadCount).isEqualTo(0)

                // Add unread blog posts after presenter is collecting
                blogPostDao.addUnreadBlogPosts(3)

                // Should see updated unread count
                val updatedState = awaitItem()
                assertThat(updatedState.blogPostsUnreadCount).isEqualTo(3)
            }
        }

    @Test
    fun `unread counts update simultaneously for both content types`() =
        runTest {
            // Given
            val navigator = FakeNavigator(ContentHubScreen)
            val announcementDao = FakeAnnouncementDao()
            val blogPostDao = FakeBlogPostDao()

            val announcementRepository = AnnouncementRepository(announcementDao)
            val blogPostRepository = BlogPostRepository(blogPostDao)
            val presenter = ContentHubPresenter(navigator, announcementRepository, blogPostRepository)

            // When/Then
            presenter.test {
                // First emission should have initial values (0)
                val initialState = awaitItem()
                assertThat(initialState.announcementsUnreadCount).isEqualTo(0)
                assertThat(initialState.blogPostsUnreadCount).isEqualTo(0)

                // Add unread to both after presenter is collecting
                announcementDao.addUnreadAnnouncements(7)
                blogPostDao.addUnreadBlogPosts(4)

                // We may receive one or two state updates depending on timing
                var latestState = initialState
                repeat(2) {
                    latestState = awaitItem()
                }

                assertThat(latestState.announcementsUnreadCount).isEqualTo(7)
                assertThat(latestState.blogPostsUnreadCount).isEqualTo(4)
            }
        }

    @Test
    fun `back clicked navigates back`() =
        runTest {
            // Given
            val navigator = FakeNavigator(ContentHubScreen)
            val announcementDao = FakeAnnouncementDao()
            val blogPostDao = FakeBlogPostDao()
            val announcementRepository = AnnouncementRepository(announcementDao)
            val blogPostRepository = BlogPostRepository(blogPostDao)
            val presenter = ContentHubPresenter(navigator, announcementRepository, blogPostRepository)

            // When/Then
            presenter.test {
                val state = awaitItem()

                state.eventSink(ContentHubScreen.Event.BackClicked)

                // Verify navigation occurred
                assertThat(navigator.awaitPop()).isNotNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `tab selection persists with predefined unread counts`() =
        runTest {
            // Given
            val navigator = FakeNavigator(ContentHubScreen)
            val announcementDao = FakeAnnouncementDao()
            val blogPostDao = FakeBlogPostDao()

            val announcementRepository = AnnouncementRepository(announcementDao)
            val blogPostRepository = BlogPostRepository(blogPostDao)
            val presenter = ContentHubPresenter(navigator, announcementRepository, blogPostRepository)

            // When/Then
            presenter.test {
                val initialState = awaitItem()

                // Set unread counts after presenter starts
                announcementDao.addUnreadAnnouncements(2)
                blogPostDao.addUnreadBlogPosts(8)

                // Wait for updates (may receive 1 or 2 state updates)
                var latestState = initialState
                repeat(2) {
                    latestState = awaitItem()
                }

                assertThat(latestState.announcementsUnreadCount).isEqualTo(2)
                assertThat(latestState.blogPostsUnreadCount).isEqualTo(8)

                // Switch to blog posts tab
                latestState.eventSink(ContentHubScreen.Event.TabSelected(ContentHubScreen.Tab.BLOG_POSTS))
                val blogPostsState = awaitItem()

                // Tab selection should be blog posts with counts preserved
                assertThat(blogPostsState.selectedTab).isEqualTo(ContentHubScreen.Tab.BLOG_POSTS)
                assertThat(blogPostsState.announcementsUnreadCount).isEqualTo(2)
                assertThat(blogPostsState.blogPostsUnreadCount).isEqualTo(8)
            }
        }

    @Test
    fun `handles zero unread counts gracefully`() =
        runTest {
            // Given
            val navigator = FakeNavigator(ContentHubScreen)
            val announcementDao = FakeAnnouncementDao()
            val blogPostDao = FakeBlogPostDao()
            val announcementRepository = AnnouncementRepository(announcementDao)
            val blogPostRepository = BlogPostRepository(blogPostDao)
            val presenter = ContentHubPresenter(navigator, announcementRepository, blogPostRepository)

            // When/Then
            presenter.test {
                val state = awaitItem()

                // Both should start at 0
                assertThat(state.announcementsUnreadCount).isEqualTo(0)
                assertThat(state.blogPostsUnreadCount).isEqualTo(0)

                // Tab switching should work fine with 0 counts
                state.eventSink(ContentHubScreen.Event.TabSelected(ContentHubScreen.Tab.BLOG_POSTS))
                val blogPostsState = awaitItem()
                assertThat(blogPostsState.selectedTab).isEqualTo(ContentHubScreen.Tab.BLOG_POSTS)
            }
        }

    @Test
    fun `handles large unread counts`() =
        runTest {
            // Given
            val navigator = FakeNavigator(ContentHubScreen)
            val announcementDao = FakeAnnouncementDao()
            val blogPostDao = FakeBlogPostDao()

            val announcementRepository = AnnouncementRepository(announcementDao)
            val blogPostRepository = BlogPostRepository(blogPostDao)
            val presenter = ContentHubPresenter(navigator, announcementRepository, blogPostRepository)

            // When/Then
            presenter.test {
                val initialState = awaitItem()

                // Set large unread counts after presenter starts
                announcementDao.addUnreadAnnouncements(999)
                blogPostDao.addUnreadBlogPosts(1234)

                // Wait for updates (may receive 1 or 2 state updates)
                var latestState = initialState
                repeat(2) {
                    latestState = awaitItem()
                }

                assertThat(latestState.announcementsUnreadCount).isEqualTo(999)
                assertThat(latestState.blogPostsUnreadCount).isEqualTo(1234)
            }
        }

    @Test
    fun `tab switches immediately without waiting for unread counts`() =
        runTest {
            // Given
            val navigator = FakeNavigator(ContentHubScreen)
            val announcementDao = FakeAnnouncementDao()
            val blogPostDao = FakeBlogPostDao()
            val announcementRepository = AnnouncementRepository(announcementDao)
            val blogPostRepository = BlogPostRepository(blogPostDao)
            val presenter = ContentHubPresenter(navigator, announcementRepository, blogPostRepository)

            // When/Then
            presenter.test {
                val initialState = awaitItem()
                assertThat(initialState.selectedTab).isEqualTo(ContentHubScreen.Tab.ANNOUNCEMENTS)

                // Tab switch should be immediate
                initialState.eventSink(ContentHubScreen.Event.TabSelected(ContentHubScreen.Tab.BLOG_POSTS))

                val nextState = awaitItem()
                assertThat(nextState.selectedTab).isEqualTo(ContentHubScreen.Tab.BLOG_POSTS)
            }
        }
}

/**
 * Fake AnnouncementDao for testing ContentHubPresenter.
 * Only implements getUnreadCount() which is the only method used by the presenter.
 */
private class FakeAnnouncementDao : AnnouncementDao {
    private val announcements = MutableStateFlow<Map<String, AnnouncementEntity>>(emptyMap())

    override fun getAll(): Flow<List<AnnouncementEntity>> =
        announcements.map { map ->
            map.values.sortedByDescending { it.publishedDate }
        }

    override fun getLatest(limit: Int): Flow<List<AnnouncementEntity>> = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override fun getUnread(): Flow<List<AnnouncementEntity>> = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override fun getRead(): Flow<List<AnnouncementEntity>> = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override suspend fun insertAll(announcements: List<AnnouncementEntity>) {
        val newMap = this.announcements.value.toMutableMap()
        announcements.forEach { announcement ->
            newMap[announcement.id] = announcement
        }
        this.announcements.value = newMap
    }

    override suspend fun markAsRead(id: String): Unit = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override suspend fun markAsUnread(id: String): Unit = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override suspend fun markAllAsRead(): Unit = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override suspend fun deleteOlderThan(threshold: Long): Unit = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override fun getUnreadCount(): Flow<Int> =
        announcements.map { map ->
            map.values.count { !it.isRead }
        }

    /**
     * Test helper to add unread announcements.
     */
    suspend fun addUnreadAnnouncements(count: Int) {
        val newAnnouncements =
            (1..count).map { index ->
                AnnouncementEntity(
                    id = "announcement-$index",
                    title = "Announcement $index",
                    summary = "Summary $index",
                    link = "https://example.com/announcement-$index",
                    publishedDate = Instant.now(),
                    isRead = false,
                    fetchedAt = Instant.now(),
                )
            }
        insertAll(newAnnouncements)
    }
}

/**
 * Fake BlogPostDao for testing ContentHubPresenter.
 * Only implements getUnreadCount() which is the only method used by the presenter.
 */
private class FakeBlogPostDao : BlogPostDao {
    private val posts = MutableStateFlow<Map<String, BlogPostEntity>>(emptyMap())

    override fun getAll(): Flow<List<BlogPostEntity>> =
        posts.map { map ->
            map.values.sortedByDescending { it.publishedDate }
        }

    override fun getByCategory(category: String): Flow<List<BlogPostEntity>> =
        throw NotImplementedError("Not needed for ContentHubScreen tests")

    override fun getFavorites(): Flow<List<BlogPostEntity>> = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override fun getUnread(): Flow<List<BlogPostEntity>> = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override fun getRecentlyRead(): Flow<List<BlogPostEntity>> = throw NotImplementedError("Not needed for ContentHubScreen tests")

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

    override suspend fun markAsRead(id: String): Unit = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override suspend fun markAllAsRead(timestamp: Instant): Unit = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override fun getUnreadCount(): Flow<Int> =
        posts.map { map ->
            map.values.count { !it.isRead }
        }

    override suspend fun updateReadingProgress(
        id: String,
        progress: Float,
        timestamp: Instant,
    ): Unit = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override suspend fun toggleFavorite(id: String): Unit = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override suspend fun updateSummary(
        id: String,
        summary: String,
    ): Unit = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override suspend fun deleteOlderThan(threshold: Long): Unit = throw NotImplementedError("Not needed for ContentHubScreen tests")

    override fun searchPosts(query: String): Flow<List<BlogPostEntity>> = throw NotImplementedError("Not needed for ContentHubScreen tests")

    /**
     * Test helper to add unread blog posts.
     */
    suspend fun addUnreadBlogPosts(count: Int) {
        val newBlogPosts =
            (1..count).map { index ->
                BlogPostEntity(
                    id = "blog-post-$index",
                    title = "Blog Post $index",
                    summary = "Summary $index",
                    link = "https://example.com/blog-$index",
                    authorName = "Author $index",
                    category = "Category",
                    publishedDate = Instant.now(),
                    featuredImageUrl = null,
                    imageUrls = null,
                    isRead = false,
                    fetchedAt = Instant.now(),
                )
            }
        insertAll(newBlogPosts)
    }
}
