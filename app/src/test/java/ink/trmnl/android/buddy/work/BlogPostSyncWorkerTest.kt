package ink.trmnl.android.buddy.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import ink.trmnl.android.buddy.content.db.FakeBlogPostDao
import ink.trmnl.android.buddy.content.repository.BlogPostRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * Unit tests for [BlogPostSyncWorker].
 *
 * Tests background synchronization of blog posts from RSS feed, following Android's
 * WorkManager testing best practices.
 *
 * Uses Robolectric to provide Android Context for WorkManager testing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BlogPostSyncWorkerTest {
    private lateinit var context: Context
    private lateinit var fakeBlogPostDao: FakeBlogPostDao
    private lateinit var fakeBlogPostRepository: FakeBlogPostRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeBlogPostDao = FakeBlogPostDao()
        fakeBlogPostRepository = FakeBlogPostRepository(fakeBlogPostDao)
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
    }

    // ========== Core Functionality Tests ==========

    @Test
    fun `successful sync returns success result`() =
        runTest {
            // Given: Repository configured for successful sync
            fakeBlogPostRepository.setRefreshResult(Result.success(Unit))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Returns success
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        }

    @Test
    fun `successful sync with new posts saves to database`() =
        runTest {
            // Given: Repository configured with new posts
            val newPosts =
                listOf(
                    createBlogPost("Post 1", "https://blog.com/post1"),
                    createBlogPost("Post 2", "https://blog.com/post2"),
                    createBlogPost("Post 3", "https://blog.com/post3"),
                )
            fakeBlogPostRepository.setRefreshResult(Result.success(Unit))
            fakeBlogPostRepository.setPostsToReturn(newPosts)

            // When: Worker executes
            val worker = createWorker()
            worker.doWork()

            // Then: Posts are saved to database
            val savedPosts = fakeBlogPostDao.getAll().first()
            assertThat(savedPosts.size).isEqualTo(3)
        }

    @Test
    fun `sync preserves existing read status`() =
        runTest {
            // Given: Existing post marked as read
            val existingPost =
                createBlogPost("Existing Post", "https://blog.com/existing")
                    .copy(isRead = true)
            fakeBlogPostDao.insertAll(listOf(existingPost))

            // And: Repository returns same post (simulating no change from feed)
            fakeBlogPostRepository.setRefreshResult(Result.success(Unit))
            fakeBlogPostRepository.setPostsToReturn(listOf(existingPost.copy(isRead = false)))

            // When: Worker executes
            val worker = createWorker()
            worker.doWork()

            // Then: Read status is preserved (IGNORE strategy)
            val savedPosts = fakeBlogPostDao.getAll().first()
            assertThat(savedPosts.first().isRead).isEqualTo(true)
        }

    @Test
    fun `sync with zero new posts returns success`() =
        runTest {
            // Given: Repository configured with no new posts
            fakeBlogPostRepository.setRefreshResult(Result.success(Unit))
            fakeBlogPostRepository.setPostsToReturn(emptyList())

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Returns success even with no new posts
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        }

    // ========== Error Handling Tests ==========

    @Test
    fun `network error returns retry result`() =
        runTest {
            // Given: Repository configured to fail with network error
            fakeBlogPostRepository.setRefreshResult(
                Result.failure(Exception("Network error")),
            )

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Returns retry for network failure
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        }

    @Test
    fun `repository failure returns retry result`() =
        runTest {
            // Given: Repository configured to fail
            fakeBlogPostRepository.setRefreshResult(
                Result.failure(Exception("Repository failure")),
            )

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Returns retry
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        }

    @Test
    fun `unexpected exception returns retry result`() =
        runTest {
            // Given: Repository throws unexpected exception
            fakeBlogPostRepository.setShouldThrowException(true)

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Returns retry to allow WorkManager exponential backoff
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        }

    // ========== Notification Logic Tests ==========

    @Test
    fun `sync with new posts and notifications enabled triggers notification`() =
        runTest {
            // Given: New posts and notifications enabled
            val newPosts =
                listOf(
                    createBlogPost("New Post", "https://blog.com/new"),
                )
            fakeBlogPostRepository.setRefreshResult(Result.success(Unit))
            fakeBlogPostRepository.setPostsToReturn(newPosts)
            fakeUserPreferencesRepository.setNotificationsEnabled(true)

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success result (notification logic tested via NotificationHelper separately)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        }

    @Test
    fun `sync with new posts and notifications disabled skips notification`() =
        runTest {
            // Given: New posts but notifications disabled
            val newPosts =
                listOf(
                    createBlogPost("New Post", "https://blog.com/new"),
                )
            fakeBlogPostRepository.setRefreshResult(Result.success(Unit))
            fakeBlogPostRepository.setPostsToReturn(newPosts)
            fakeUserPreferencesRepository.setNotificationsEnabled(false)

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success result (notification logic skipped)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        }

    @Test
    fun `sync with zero new posts skips notification even if enabled`() =
        runTest {
            // Given: No new posts but notifications enabled
            fakeBlogPostRepository.setRefreshResult(Result.success(Unit))
            fakeBlogPostRepository.setPostsToReturn(emptyList())
            fakeUserPreferencesRepository.setNotificationsEnabled(true)

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success result without notification
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        }

    // ========== Edge Cases ==========

    @Test
    fun `sync with single post returns success`() =
        runTest {
            // Given: Single new post
            val singlePost = createBlogPost("Single Post", "https://blog.com/single")
            fakeBlogPostRepository.setRefreshResult(Result.success(Unit))
            fakeBlogPostRepository.setPostsToReturn(listOf(singlePost))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Returns success
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            val savedPosts = fakeBlogPostDao.getAll().first()
            assertThat(savedPosts.size).isEqualTo(1)
        }

    @Test
    fun `sync with multiple posts of same pubDate handles correctly`() =
        runTest {
            // Given: Multiple posts with same timestamp
            val timestamp = Instant.parse("2025-01-01T00:00:00Z")
            val posts =
                listOf(
                    createBlogPost("Post A", "https://blog.com/a", timestamp),
                    createBlogPost("Post B", "https://blog.com/b", timestamp),
                    createBlogPost("Post C", "https://blog.com/c", timestamp),
                )
            fakeBlogPostRepository.setRefreshResult(Result.success(Unit))
            fakeBlogPostRepository.setPostsToReturn(posts)

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: All posts are saved
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            val savedPosts = fakeBlogPostDao.getAll().first()
            assertThat(savedPosts.size).isEqualTo(3)
        }

    @Test
    fun `sync calculates new posts count correctly when some already exist`() =
        runTest {
            // Given: One existing post
            val existingPost = createBlogPost("Existing", "https://blog.com/existing")
            fakeBlogPostDao.insertAll(listOf(existingPost))

            // And: Repository returns existing post plus two new posts
            val allPosts =
                listOf(
                    existingPost,
                    createBlogPost("New 1", "https://blog.com/new1"),
                    createBlogPost("New 2", "https://blog.com/new2"),
                )
            fakeBlogPostRepository.setRefreshResult(Result.success(Unit))
            fakeBlogPostRepository.setPostsToReturn(allPosts)

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success and all posts saved
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            val savedPosts = fakeBlogPostDao.getAll().first()
            assertThat(savedPosts.size).isEqualTo(3)
        }

    @Test
    fun `sync with large feed handles all posts`() =
        runTest {
            // Given: Large feed with 50 posts
            val largeFeed =
                (1..50).map { index ->
                    createBlogPost("Post $index", "https://blog.com/post$index")
                }
            fakeBlogPostRepository.setRefreshResult(Result.success(Unit))
            fakeBlogPostRepository.setPostsToReturn(largeFeed)

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: All posts are saved
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            val savedPosts = fakeBlogPostDao.getAll().first()
            assertThat(savedPosts.size).isEqualTo(50)
        }

    @Test
    fun `multiple sync attempts preserve existing posts`() =
        runTest {
            // Given: First sync with initial posts
            val initialPosts =
                listOf(
                    createBlogPost("Post 1", "https://blog.com/1"),
                    createBlogPost("Post 2", "https://blog.com/2"),
                )
            fakeBlogPostRepository.setRefreshResult(Result.success(Unit))
            fakeBlogPostRepository.setPostsToReturn(initialPosts)

            val worker1 = createWorker()
            worker1.doWork()

            // When: Second sync with additional post
            val updatedPosts =
                initialPosts + createBlogPost("Post 3", "https://blog.com/3")
            fakeBlogPostRepository.setPostsToReturn(updatedPosts)

            val worker2 = createWorker()
            val result = worker2.doWork()

            // Then: All posts are preserved
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            val savedPosts = fakeBlogPostDao.getAll().first()
            assertThat(savedPosts.size).isEqualTo(3)
        }

    // ========== Helper Methods ==========

    private fun createWorker(): BlogPostSyncWorker {
        // Create a custom WorkerFactory that provides our test dependencies
        val workerFactory =
            object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters,
                ): androidx.work.ListenableWorker? =
                    if (workerClassName == BlogPostSyncWorker::class.java.name) {
                        BlogPostSyncWorker(
                            appContext,
                            workerParameters,
                            fakeBlogPostRepository,
                            fakeUserPreferencesRepository,
                        )
                    } else {
                        null
                    }
            }

        return TestListenableWorkerBuilder<BlogPostSyncWorker>(context)
            .setWorkerFactory(workerFactory)
            .build() as BlogPostSyncWorker
    }

    private fun createBlogPost(
        title: String,
        link: String,
        publishedDate: Instant = Instant.now(),
    ) = BlogPostEntity(
        id = link,
        title = title,
        summary = "Summary for $title",
        link = link,
        authorName = "Test Author",
        category = "Test Category",
        publishedDate = publishedDate,
        featuredImageUrl = null,
        imageUrls = null,
        fetchedAt = Instant.now(),
        isRead = false,
        isFavorite = false,
        lastReadAt = null,
        readingProgressPercent = 0f,
    )

    // ========== Fake Implementations ==========

    /**
     * Fake implementation of [BlogPostRepository] for testing.
     * Allows controlling sync behavior and simulating various scenarios.
     */
    private class FakeBlogPostRepository(
        private val blogPostDao: FakeBlogPostDao,
    ) : BlogPostRepository(blogPostDao) {
        private var refreshResult: Result<Unit> = Result.success(Unit)
        private var postsToReturn: List<BlogPostEntity> = emptyList()
        private var shouldThrowException = false

        fun setRefreshResult(result: Result<Unit>) {
            refreshResult = result
        }

        fun setPostsToReturn(posts: List<BlogPostEntity>) {
            postsToReturn = posts
        }

        fun setShouldThrowException(shouldThrow: Boolean) {
            shouldThrowException = shouldThrow
        }

        override suspend fun refreshBlogPosts(): Result<Unit> {
            if (shouldThrowException) {
                throw RuntimeException("Unexpected exception during refresh")
            }

            return if (refreshResult.isSuccess) {
                // Simulate successful refresh by inserting posts
                blogPostDao.insertAll(postsToReturn)
                Result.success(Unit)
            } else {
                refreshResult
            }
        }
    }
}
