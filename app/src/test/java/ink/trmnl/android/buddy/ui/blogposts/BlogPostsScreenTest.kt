package ink.trmnl.android.buddy.ui.blogposts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.content.db.BlogPostDao
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import ink.trmnl.android.buddy.content.repository.BlogPostRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Tests for BlogPostsScreen presenter.
 *
 * Verifies:
 * - Initial loading state and data fetch
 * - Empty state handling
 * - Blog post display and sorting (newest first)
 * - Navigation and mark as read
 * - Pull-to-refresh functionality
 * - Toggle favorite functionality
 * - Category filtering
 * - Mark all as read
 * - Error handling and retry
 * - Unread count tracking
 * - Embedded mode behavior
 */
@RunWith(RobolectricTestRunner::class)
class BlogPostsScreenTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `presenter loads blog posts on initial composition`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BlogPostsScreen())
            val dao = FakeBlogPostDao()
            dao.initializePosts(createSampleBlogPosts(3))
            val repository = TestBlogPostRepository(dao)
            val presenter = BlogPostsPresenter(BlogPostsScreen(), navigator, context, repository)

            // When/Then
            presenter.test {
                // Initial state should be loading
                val initialState = awaitItem()
                assertThat(initialState.isLoading).isTrue()
                assertThat(initialState.blogPosts).isEmpty()

                // Wait for loaded state
                val loadedState = awaitItem()
                assertThat(loadedState.isLoading).isFalse()
                assertThat(loadedState.blogPosts).hasSize(3)
                assertThat(loadedState.errorMessage).isNull()

                // Verify posts are sorted by date (newest first)
                assertThat(loadedState.blogPosts[0].id).isEqualTo("post-3")
                assertThat(loadedState.blogPosts[1].id).isEqualTo("post-2")
                assertThat(loadedState.blogPosts[2].id).isEqualTo("post-1")
            }
        }

    @Test
    fun `empty state when no blog posts available`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BlogPostsScreen())
            val dao = FakeBlogPostDao()
            val repository = TestBlogPostRepository(dao)
            val presenter = BlogPostsPresenter(BlogPostsScreen(), navigator, context, repository)

            // When/Then
            presenter.test {
                // Skip initial loading state
                awaitItem()

                // Should show empty state
                val emptyState = awaitItem()
                assertThat(emptyState.isLoading).isFalse()
                assertThat(emptyState.blogPosts).isEmpty()
                assertThat(emptyState.errorMessage).isNull()
            }
        }

    @Test
    fun `refresh triggers blog posts sync`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BlogPostsScreen())
            val dao = FakeBlogPostDao()
            dao.initializePosts(createSampleBlogPosts(2))
            val repository = TestBlogPostRepository(dao)
            val presenter = BlogPostsPresenter(BlogPostsScreen(), navigator, context, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                awaitItem() // loading
                val loadedState = awaitItem() // loaded

                // Trigger refresh
                loadedState.eventSink(BlogPostsScreen.Event.Refresh)

                // Should show refreshing state
                val refreshingState = awaitItem()
                assertThat(refreshingState.isRefreshing).isTrue()

                // Should complete refresh
                val refreshedState = awaitItem()
                assertThat(refreshedState.isRefreshing).isFalse()
                assertThat(repository.refreshCallCount).isEqualTo(1) // Only the manual refresh (not initial since DAO has data)
            }
        }

    @Test
    fun `toggle favorite updates blog post favorite status`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BlogPostsScreen())
            val dao = FakeBlogPostDao()
            dao.initializePosts(createSampleBlogPosts(2))
            val repository = TestBlogPostRepository(dao)
            val presenter = BlogPostsPresenter(BlogPostsScreen(), navigator, context, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                awaitItem() // loading
                val loadedState = awaitItem() // loaded

                val postId = loadedState.blogPosts[0].id
                assertThat(loadedState.blogPosts[0].isFavorite).isFalse()

                // Toggle favorite
                loadedState.eventSink(BlogPostsScreen.Event.ToggleFavorite(postId))

                // Wait for update
                delay(100)

                // Should be favorited
                val updatedState = awaitItem()
                val updatedPost = updatedState.blogPosts.find { it.id == postId }
                assertThat(updatedPost).isNotNull()
                assertThat(updatedPost!!.isFavorite).isTrue()
            }
        }

    @Test
    fun `category filter shows only posts in selected category`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BlogPostsScreen())
            val dao = FakeBlogPostDao()
            val posts =
                listOf(
                    createBlogPost("1", "Post 1", category = "Engineering"),
                    createBlogPost("2", "Post 2", category = "Product Updates"),
                    createBlogPost("3", "Post 3", category = "Engineering"),
                )
            dao.initializePosts(posts)
            val repository = TestBlogPostRepository(dao)
            val presenter = BlogPostsPresenter(BlogPostsScreen(), navigator, context, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                awaitItem() // loading
                val loadedState = awaitItem() // loaded

                assertThat(loadedState.blogPosts).hasSize(3)
                assertThat(loadedState.selectedCategory).isNull()

                // Filter by Engineering category
                loadedState.eventSink(BlogPostsScreen.Event.CategorySelected("Engineering"))

                // Should show filtered posts - await state update first
                val categoryChangeState = awaitItem()
                assertThat(categoryChangeState.selectedCategory).isEqualTo("Engineering")

                // Then await the filtered data
                val filteredState = awaitItem()
                assertThat(filteredState.selectedCategory).isEqualTo("Engineering")
                assertThat(filteredState.blogPosts).hasSize(2)
                assertThat(filteredState.blogPosts.all { it.category == "Engineering" }).isTrue()
            }
        }

    @Test
    fun `selecting null category shows all posts`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BlogPostsScreen())
            val dao = FakeBlogPostDao()
            val posts =
                listOf(
                    createBlogPost("1", "Post 1", category = "Engineering"),
                    createBlogPost("2", "Post 2", category = "Product Updates"),
                )
            dao.initializePosts(posts)
            val repository = TestBlogPostRepository(dao)
            val presenter = BlogPostsPresenter(BlogPostsScreen(), navigator, context, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                awaitItem() // loading
                val loadedState = awaitItem() // loaded

                // Filter by Engineering
                loadedState.eventSink(BlogPostsScreen.Event.CategorySelected("Engineering"))
                val categoryChangeState = awaitItem()
                val filteredState = awaitItem()
                assertThat(filteredState.blogPosts).hasSize(1)

                // Clear filter (select null)
                filteredState.eventSink(BlogPostsScreen.Event.CategorySelected(null))

                // Should show all posts
                val clearCategoryState = awaitItem()
                val allPostsState = awaitItem()
                assertThat(allPostsState.selectedCategory).isNull()
                assertThat(allPostsState.blogPosts).hasSize(2)
            }
        }

    @Test
    fun `mark all as read updates all posts`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BlogPostsScreen())
            val dao = FakeBlogPostDao()
            dao.initializePosts(createSampleBlogPosts(3))
            val repository = TestBlogPostRepository(dao)
            val presenter = BlogPostsPresenter(BlogPostsScreen(), navigator, context, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                awaitItem() // loading
                val loadedState = awaitItem() // loaded

                assertThat(loadedState.blogPosts.all { !it.isRead }).isTrue()
                assertThat(loadedState.unreadCount).isEqualTo(3)

                // Mark all as read
                loadedState.eventSink(BlogPostsScreen.Event.MarkAllAsRead)

                // Wait for update
                delay(100)

                // All posts should be marked as read
                val updatedState = awaitItem()
                assertThat(updatedState.blogPosts.all { it.isRead }).isTrue()
                assertThat(updatedState.unreadCount).isEqualTo(0)
            }
        }

    @Test
    fun `blog post clicked marks post as read`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BlogPostsScreen())
            val dao = FakeBlogPostDao()
            dao.initializePosts(createSampleBlogPosts(2))
            val repository = TestBlogPostRepository(dao)
            val presenter = BlogPostsPresenter(BlogPostsScreen(), navigator, context, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                awaitItem() // loading
                val loadedState = awaitItem() // loaded

                val post = loadedState.blogPosts[0]
                assertThat(post.isRead).isFalse()

                // Click blog post
                loadedState.eventSink(BlogPostsScreen.Event.BlogPostClicked(post))

                // Wait for update
                delay(100)

                // Post should be marked as read
                val updatedState = awaitItem()
                val updatedPost = updatedState.blogPosts.find { it.id == post.id }
                assertThat(updatedPost).isNotNull()
                assertThat(updatedPost!!.isRead).isTrue()
            }
        }

    @Test
    fun `unread count updates correctly`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BlogPostsScreen())
            val dao = FakeBlogPostDao()
            dao.initializePosts(createSampleBlogPosts(5))
            val repository = TestBlogPostRepository(dao)
            val presenter = BlogPostsPresenter(BlogPostsScreen(), navigator, context, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                awaitItem() // loading
                val loadedState = awaitItem() // loaded

                assertThat(loadedState.unreadCount).isEqualTo(5)

                // Mark one as read
                loadedState.eventSink(BlogPostsScreen.Event.BlogPostClicked(loadedState.blogPosts[0]))
                delay(100)

                val afterReadState = awaitItem()
                assertThat(afterReadState.unreadCount).isEqualTo(4)
            }
        }

    @Test
    fun `error on refresh shows error message`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BlogPostsScreen())
            val dao = FakeBlogPostDao()
            dao.initializePosts(createSampleBlogPosts(2))
            val repository = TestBlogPostRepository(dao, shouldFailRefresh = true, errorMessage = "Network error occurred")
            val presenter = BlogPostsPresenter(BlogPostsScreen(), navigator, context, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                awaitItem() // loading
                val loadedState = awaitItem() // loaded

                // Trigger refresh
                loadedState.eventSink(BlogPostsScreen.Event.Refresh)

                // Should show refreshing state
                awaitItem() // refreshing

                // Wait for error
                delay(100)

                // Should show error message
                val errorState = awaitItem()
                assertThat(errorState.isRefreshing).isFalse()
                assertThat(errorState.errorMessage).isNotNull()
                assertThat(errorState.errorMessage).isEqualTo("Failed to refresh: Network error occurred")
            }
        }

    @Test
    fun `error on initial load shows error state`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BlogPostsScreen())
            val dao = FakeBlogPostDao()
            val repository = TestBlogPostRepository(dao, shouldFailRefresh = true, errorMessage = "Failed to fetch posts")
            val presenter = BlogPostsPresenter(BlogPostsScreen(), navigator, context, repository)

            // When/Then
            presenter.test {
                // Initial loading state
                val loadingState = awaitItem()
                assertThat(loadingState.isLoading).isTrue()

                // Wait for error
                delay(100)

                // Should show error state
                val errorState = awaitItem()
                assertThat(errorState.isLoading).isFalse()
                assertThat(errorState.errorMessage).isNotNull()
                assertThat(errorState.errorMessage).isEqualTo("Failed to fetch blog posts: Failed to fetch posts")
                assertThat(errorState.blogPosts).isEmpty()
            }
        }

    @Test
    fun `embedded mode hides top bar`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BlogPostsScreen(isEmbedded = true))
            val dao = FakeBlogPostDao()
            dao.initializePosts(createSampleBlogPosts(2))
            val repository = TestBlogPostRepository(dao)
            val presenter = BlogPostsPresenter(BlogPostsScreen(isEmbedded = true), navigator, context, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                awaitItem() // loading
                val loadedState = awaitItem() // loaded

                // Top bar should be hidden in embedded mode
                assertThat(loadedState.showTopBar).isFalse()
            }
        }

    @Test
    fun `non-embedded mode shows top bar`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BlogPostsScreen(isEmbedded = false))
            val dao = FakeBlogPostDao()
            dao.initializePosts(createSampleBlogPosts(2))
            val repository = TestBlogPostRepository(dao)
            val presenter = BlogPostsPresenter(BlogPostsScreen(isEmbedded = false), navigator, context, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                awaitItem() // loading
                val loadedState = awaitItem() // loaded

                // Top bar should be visible in normal mode
                assertThat(loadedState.showTopBar).isTrue()
            }
        }

    @Test
    fun `available categories extracted from blog posts`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BlogPostsScreen())
            val dao = FakeBlogPostDao()
            val posts =
                listOf(
                    createBlogPost("1", "Post 1", category = "Engineering"),
                    createBlogPost("2", "Post 2", category = "Product Updates"),
                    createBlogPost("3", "Post 3", category = "Engineering"),
                    createBlogPost("4", "Post 4", category = "Community"),
                )
            dao.initializePosts(posts)
            val repository = TestBlogPostRepository(dao)
            val presenter = BlogPostsPresenter(BlogPostsScreen(), navigator, context, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                awaitItem() // loading
                val loadedState = awaitItem() // loaded

                // Should have unique sorted categories
                assertThat(loadedState.availableCategories).hasSize(3)
                assertThat(loadedState.availableCategories).isEqualTo(
                    listOf("Community", "Engineering", "Product Updates"),
                )
            }
        }
}

/**
 * Test wrapper around BlogPostRepository that allows controlling refresh behavior.
 */
private class TestBlogPostRepository(
    dao: BlogPostDao,
    var shouldFailRefresh: Boolean = false,
    var errorMessage: String = "Error fetching blog posts",
) : BlogPostRepository(dao) {
    var refreshCallCount = 0

    override suspend fun refreshBlogPosts(): Result<Unit> {
        refreshCallCount++
        return if (shouldFailRefresh) {
            Result.failure(Exception(errorMessage))
        } else {
            // Call super to test real behavior when not failing
            super.refreshBlogPosts()
        }
    }
}

/**
 * Fake DAO for testing blog posts.
 */
private class FakeBlogPostDao : BlogPostDao {
    private val posts = MutableStateFlow<Map<String, BlogPostEntity>>(emptyMap())

    suspend fun initializePosts(postsList: List<BlogPostEntity>) {
        posts.value = postsList.associateBy { it.id }
    }

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
            if (!newMap.containsKey(post.id)) {
                newMap[post.id] = post
            }
        }
        this.posts.value = newMap
    }

    override suspend fun markAsRead(id: String) {
        val current = posts.value[id] ?: return
        posts.value = posts.value + (id to current.copy(isRead = true, lastReadAt = Instant.now()))
    }

    override suspend fun markAllAsRead(timestamp: Instant) {
        val updatedMap =
            posts.value.mapValues { (_, post) ->
                if (post.isRead) post else post.copy(isRead = true, lastReadAt = timestamp)
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
        posts.value = posts.value + (id to current.copy(readingProgressPercent = progress, lastReadAt = timestamp))
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

/**
 * Create sample blog posts for testing.
 */
private fun createSampleBlogPosts(count: Int): List<BlogPostEntity> =
    (1..count).map { index ->
        createBlogPost(
            id = "post-$index",
            title = "Blog Post $index",
            category = "Test Category",
        )
    }

/**
 * Create a single blog post for testing.
 */
private fun createBlogPost(
    id: String,
    title: String,
    category: String? = "Test Category",
    isRead: Boolean = false,
    isFavorite: Boolean = false,
): BlogPostEntity =
    BlogPostEntity(
        id = id,
        title = title,
        summary = "Summary for $title",
        link = "https://trmnl.com/blog/$id",
        authorName = "Test Author",
        category = category,
        publishedDate = Instant.now().plus(id.filter { it.isDigit() }.toLongOrNull() ?: 0, ChronoUnit.HOURS),
        featuredImageUrl = "https://example.com/image-$id.jpg",
        imageUrls = listOf("https://example.com/image-$id.jpg"),
        isRead = isRead,
        isFavorite = isFavorite,
        fetchedAt = Instant.now(),
    )
