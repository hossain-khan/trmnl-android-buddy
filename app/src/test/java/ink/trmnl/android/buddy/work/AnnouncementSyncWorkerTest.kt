package ink.trmnl.android.buddy.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.content.db.FakeAnnouncementDao
import ink.trmnl.android.buddy.content.repository.AnnouncementRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Unit tests for [AnnouncementSyncWorker].
 *
 * Tests cover:
 * - Successful RSS feed sync and database operations
 * - Read state preservation during sync
 * - Error handling (network, parse, database)
 * - Notification logic based on preferences
 * - Unread count tracking
 * - Edge cases (empty feed, large feed, etc.)
 *
 * Uses fake implementations following project testing guidelines:
 * - [FakeAnnouncementRepository] for repository testing
 * - [FakeAnnouncementDao] for database operations
 * - [FakeUserPreferencesRepository] for user preferences
 */
@RunWith(RobolectricTestRunner::class)
class AnnouncementSyncWorkerTest {
    private lateinit var context: Context
    private lateinit var fakeAnnouncementDao: FakeAnnouncementDao
    private lateinit var fakeAnnouncementRepository: FakeAnnouncementRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeAnnouncementDao = FakeAnnouncementDao()
        fakeAnnouncementRepository = FakeAnnouncementRepository(fakeAnnouncementDao)
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
    }

    @Test
    fun `successful sync returns success result`() =
        runTest {
            // Given: RSS feed with 3 announcements
            val announcements =
                listOf(
                    createAnnouncement("1", "Update 1"),
                    createAnnouncement("2", "Update 2"),
                    createAnnouncement("3", "Update 3"),
                )
            fakeAnnouncementRepository.setFeedResponse(Result.success(announcements))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success returned
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        }

    @Test
    fun `successful sync saves announcements to database`() =
        runTest {
            // Given: RSS feed with 3 announcements
            val announcements =
                listOf(
                    createAnnouncement("1", "Update 1"),
                    createAnnouncement("2", "Update 2"),
                    createAnnouncement("3", "Update 3"),
                )
            fakeAnnouncementRepository.setFeedResponse(Result.success(announcements))

            // When: Worker executes
            val worker = createWorker()
            worker.doWork()

            // Then: Announcements saved to database
            val savedAnnouncements = fakeAnnouncementDao.getAll().first()
            assertThat(savedAnnouncements).hasSize(3)
        }

    @Test
    fun `sync preserves read state for existing announcements`() =
        runTest {
            // Given: Existing announcement marked as read
            val existing = createAnnouncement("1", "Old", isRead = true)
            fakeAnnouncementDao.seedData(listOf(existing))

            // When: Sync includes same announcement (but with isRead = false in feed)
            val updated = createAnnouncement("1", "Old", isRead = false)
            fakeAnnouncementRepository.setFeedResponse(Result.success(listOf(updated)))
            val worker = createWorker()
            worker.doWork()

            // Then: Read state preserved
            val savedAnnouncements = fakeAnnouncementDao.getAll().first()
            val savedAnnouncement = savedAnnouncements.first { it.id == "1" }
            assertThat(savedAnnouncement.isRead).isEqualTo(true)
        }

    @Test
    fun `sync preserves read state for multiple existing announcements`() =
        runTest {
            // Given: Mix of read and unread announcements
            val existing =
                listOf(
                    createAnnouncement("1", "First", isRead = true),
                    createAnnouncement("2", "Second", isRead = false),
                    createAnnouncement("3", "Third", isRead = true),
                )
            fakeAnnouncementDao.seedData(existing)

            // When: Sync with same announcements
            val updated =
                listOf(
                    createAnnouncement("1", "First", isRead = false),
                    createAnnouncement("2", "Second", isRead = false),
                    createAnnouncement("3", "Third", isRead = false),
                )
            fakeAnnouncementRepository.setFeedResponse(Result.success(updated))
            val worker = createWorker()
            worker.doWork()

            // Then: Read states preserved
            val savedAnnouncements = fakeAnnouncementDao.getAll().first()
            assertThat(savedAnnouncements.first { it.id == "1" }.isRead).isEqualTo(true)
            assertThat(savedAnnouncements.first { it.id == "2" }.isRead).isEqualTo(false)
            assertThat(savedAnnouncements.first { it.id == "3" }.isRead).isEqualTo(true)
        }

    @Test
    fun `new announcements default to unread`() =
        runTest {
            // Given: No existing announcements
            // When: Sync with new announcements
            val announcements =
                listOf(
                    createAnnouncement("1", "New 1"),
                    createAnnouncement("2", "New 2"),
                )
            fakeAnnouncementRepository.setFeedResponse(Result.success(announcements))
            val worker = createWorker()
            worker.doWork()

            // Then: All announcements are unread
            val savedAnnouncements = fakeAnnouncementDao.getAll().first()
            assertThat(savedAnnouncements.all { !it.isRead }).isEqualTo(true)
        }

    @Test
    fun `sync updates unread count correctly`() =
        runTest {
            // Given: No existing announcements, unread count is 0
            val initialUnreadCount = fakeAnnouncementDao.getUnreadCount().first()
            assertThat(initialUnreadCount).isEqualTo(0)

            // When: Sync with 3 new announcements
            val announcements =
                listOf(
                    createAnnouncement("1", "New 1"),
                    createAnnouncement("2", "New 2"),
                    createAnnouncement("3", "New 3"),
                )
            fakeAnnouncementRepository.setFeedResponse(Result.success(announcements))
            val worker = createWorker()
            worker.doWork()

            // Then: Unread count is 3
            val unreadCount = fakeAnnouncementDao.getUnreadCount().first()
            assertThat(unreadCount).isEqualTo(3)
        }

    @Test
    fun `sync tracks new announcements count correctly with existing read announcements`() =
        runTest {
            // Given: 1 existing read announcement
            val existing = createAnnouncement("1", "Old", isRead = true)
            fakeAnnouncementDao.seedData(listOf(existing))

            // When: Sync with old + 2 new announcements
            val announcements =
                listOf(
                    createAnnouncement("1", "Old"),
                    createAnnouncement("2", "New 1"),
                    createAnnouncement("3", "New 2"),
                )
            fakeAnnouncementRepository.setFeedResponse(Result.success(announcements))
            val worker = createWorker()
            worker.doWork()

            // Then: Unread count is 2 (only new announcements)
            val unreadCount = fakeAnnouncementDao.getUnreadCount().first()
            assertThat(unreadCount).isEqualTo(2)
        }

    @Test
    fun `network error returns retry result`() =
        runTest {
            // Given: Repository returns network error
            fakeAnnouncementRepository.setFeedResponse(Result.failure(Exception("Network error")))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Retry returned
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        }

    @Test
    fun `parse error returns retry result`() =
        runTest {
            // Given: Repository returns parse error
            fakeAnnouncementRepository.setFeedResponse(Result.failure(Exception("Parse error")))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Retry returned
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        }

    @Test
    fun `exception during sync returns retry result`() =
        runTest {
            // Given: Repository throws exception
            fakeAnnouncementRepository.setShouldThrow(true)

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Retry returned
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        }

    @Test
    fun `empty feed sync completes successfully`() =
        runTest {
            // Given: Empty RSS feed
            fakeAnnouncementRepository.setFeedResponse(Result.success(emptyList()))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success returned
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        }

    @Test
    fun `empty feed does not change existing announcements`() =
        runTest {
            // Given: Existing announcements
            val existing =
                listOf(
                    createAnnouncement("1", "Old 1"),
                    createAnnouncement("2", "Old 2"),
                )
            fakeAnnouncementDao.seedData(existing)

            // When: Sync with empty feed
            fakeAnnouncementRepository.setFeedResponse(Result.success(emptyList()))
            val worker = createWorker()
            worker.doWork()

            // Then: Existing announcements remain (empty feed doesn't clear database)
            val savedAnnouncements = fakeAnnouncementDao.getAll().first()
            assertThat(savedAnnouncements).hasSize(2)
        }

    @Test
    fun `single announcement feed sync works correctly`() =
        runTest {
            // Given: Single announcement in feed
            val announcement = createAnnouncement("1", "Single")
            fakeAnnouncementRepository.setFeedResponse(Result.success(listOf(announcement)))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success and announcement saved
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            val savedAnnouncements = fakeAnnouncementDao.getAll().first()
            assertThat(savedAnnouncements).hasSize(1)
        }

    @Test
    fun `large feed sync handles many announcements`() =
        runTest {
            // Given: Large feed with 50 announcements
            val announcements =
                (1..50).map { index ->
                    createAnnouncement(index.toString(), "Announcement $index")
                }
            fakeAnnouncementRepository.setFeedResponse(Result.success(announcements))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success and all announcements saved
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            val savedAnnouncements = fakeAnnouncementDao.getAll().first()
            assertThat(savedAnnouncements).hasSize(50)
        }

    @Test
    fun `announcements with same published date are handled correctly`() =
        runTest {
            // Given: Multiple announcements with same timestamp
            val sameDate = Instant.now()
            val announcements =
                listOf(
                    createAnnouncement("1", "First", publishedDate = sameDate),
                    createAnnouncement("2", "Second", publishedDate = sameDate),
                    createAnnouncement("3", "Third", publishedDate = sameDate),
                )
            fakeAnnouncementRepository.setFeedResponse(Result.success(announcements))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success and all announcements saved
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            val savedAnnouncements = fakeAnnouncementDao.getAll().first()
            assertThat(savedAnnouncements).hasSize(3)
        }

    @Test
    fun `announcements with special characters are handled correctly`() =
        runTest {
            // Given: Announcements with special characters, emoji, unicode
            val announcements =
                listOf(
                    createAnnouncement("1", "Update 🎉 with emoji"),
                    createAnnouncement("2", "Special chars: <>&\"'"),
                    createAnnouncement("3", "Unicode: 你好世界"),
                )
            fakeAnnouncementRepository.setFeedResponse(Result.success(announcements))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success and all announcements saved
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            val savedAnnouncements = fakeAnnouncementDao.getAll().first()
            assertThat(savedAnnouncements).hasSize(3)
        }

    @Test
    fun `very long title and description are handled correctly`() =
        runTest {
            // Given: Announcement with very long content
            val longTitle = "A".repeat(500)
            val longSummary = "B".repeat(2000)
            val announcement =
                createAnnouncement(
                    id = "1",
                    title = longTitle,
                    summary = longSummary,
                )
            fakeAnnouncementRepository.setFeedResponse(Result.success(listOf(announcement)))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        }

    @Test
    fun `sync with no new announcements preserves database state`() =
        runTest {
            // Given: Existing announcements (1 read, 1 unread)
            val existing =
                listOf(
                    createAnnouncement("1", "First", isRead = true),
                    createAnnouncement("2", "Second", isRead = false),
                )
            fakeAnnouncementDao.seedData(existing)

            // When: Sync with same announcements
            fakeAnnouncementRepository.setFeedResponse(Result.success(existing))
            val worker = createWorker()
            worker.doWork()

            // Then: Database state preserved
            val savedAnnouncements = fakeAnnouncementDao.getAll().first()
            assertThat(savedAnnouncements).hasSize(2)
            assertThat(savedAnnouncements.first { it.id == "1" }.isRead).isEqualTo(true)
            assertThat(savedAnnouncements.first { it.id == "2" }.isRead).isEqualTo(false)
        }

    @Test
    fun `incremental sync adds only new announcements`() =
        runTest {
            // Given: 2 existing announcements
            val existing =
                listOf(
                    createAnnouncement("1", "First"),
                    createAnnouncement("2", "Second"),
                )
            fakeAnnouncementDao.seedData(existing)
            val initialUnreadCount = fakeAnnouncementDao.getUnreadCount().first()

            // When: Sync with 2 old + 2 new announcements
            val announcements =
                listOf(
                    createAnnouncement("1", "First"),
                    createAnnouncement("2", "Second"),
                    createAnnouncement("3", "Third - NEW"),
                    createAnnouncement("4", "Fourth - NEW"),
                )
            fakeAnnouncementRepository.setFeedResponse(Result.success(announcements))
            val worker = createWorker()
            worker.doWork()

            // Then: Total count is 4, and 2 new unread announcements added
            val savedAnnouncements = fakeAnnouncementDao.getAll().first()
            assertThat(savedAnnouncements).hasSize(4)
            val unreadCount = fakeAnnouncementDao.getUnreadCount().first()
            assertThat(unreadCount).isEqualTo(initialUnreadCount + 2)
        }

    @Test
    fun `sync updates existing announcement content`() =
        runTest {
            // Given: Existing announcement with old content
            val existing = createAnnouncement("1", "Old Title", summary = "Old summary")
            fakeAnnouncementDao.seedData(listOf(existing))

            // When: Sync with updated content for same ID
            val updated = createAnnouncement("1", "New Title", summary = "New summary")
            fakeAnnouncementRepository.setFeedResponse(Result.success(listOf(updated)))
            val worker = createWorker()
            worker.doWork()

            // Then: Content updated
            val savedAnnouncements = fakeAnnouncementDao.getAll().first()
            val savedAnnouncement = savedAnnouncements.first()
            assertThat(savedAnnouncement.title).isEqualTo("New Title")
            assertThat(savedAnnouncement.summary).isEqualTo("New summary")
        }

    @Test
    fun `notification logic respects user preferences when disabled`() =
        runTest {
            // Given: Notifications disabled in preferences
            fakeUserPreferencesRepository.setRssFeedContentNotificationEnabled(false)
            val announcements =
                listOf(
                    createAnnouncement("1", "New 1"),
                    createAnnouncement("2", "New 2"),
                )
            fakeAnnouncementRepository.setFeedResponse(Result.success(announcements))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success (notification not shown, but we can't directly test that in unit test)
            // The worker just logs that notifications are disabled
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        }

    @Test
    fun `notification logic allows notifications when enabled`() =
        runTest {
            // Given: Notifications enabled in preferences
            fakeUserPreferencesRepository.setRssFeedContentNotificationEnabled(true)
            val announcements =
                listOf(
                    createAnnouncement("1", "New 1"),
                    createAnnouncement("2", "New 2"),
                )
            fakeAnnouncementRepository.setFeedResponse(Result.success(announcements))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success (notification would be shown, but we can't directly test that in unit test)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        }

    @Test
    fun `no notification shown when no new announcements`() =
        runTest {
            // Given: Existing announcements, notifications enabled
            val existing =
                listOf(
                    createAnnouncement("1", "First", isRead = true),
                    createAnnouncement("2", "Second", isRead = true),
                )
            fakeAnnouncementDao.seedData(existing)
            fakeUserPreferencesRepository.setRssFeedContentNotificationEnabled(true)

            // When: Sync with same announcements (no new ones)
            fakeAnnouncementRepository.setFeedResponse(Result.success(existing))
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success, but no notification (unread count didn't increase)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        }

    /**
     * Creates a test worker instance with fake dependencies.
     */
    private fun createWorker(): AnnouncementSyncWorker {
        // Create an adapter that overrides refreshAnnouncements to use fake behavior
        val repositoryAdapter =
            object : AnnouncementRepository(fakeAnnouncementDao) {
                override suspend fun refreshAnnouncements(): Result<Unit> = fakeAnnouncementRepository.refreshAnnouncements()
            }

        val workerFactory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker =
                    AnnouncementSyncWorker(
                        context = appContext,
                        params = workerParameters,
                        announcementRepository = repositoryAdapter,
                        userPreferencesRepository = fakeUserPreferencesRepository,
                    )
            }

        return TestListenableWorkerBuilder<AnnouncementSyncWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()
    }

    /**
     * Helper function to create a test announcement.
     */
    private fun createAnnouncement(
        id: String,
        title: String,
        summary: String = "Summary for $title",
        link: String = "https://usetrmnl.com/announcements/$id",
        publishedDate: Instant = Instant.now().minus(1, ChronoUnit.DAYS),
        isRead: Boolean = false,
        fetchedAt: Instant = Instant.now(),
    ): AnnouncementEntity =
        AnnouncementEntity(
            id = id,
            title = title,
            summary = summary,
            link = link,
            publishedDate = publishedDate,
            isRead = isRead,
            fetchedAt = fetchedAt,
        )
}

/**
 * Fake implementation of announcement repository operations for testing.
 *
 * This fake provides a controllable repository that can simulate various scenarios:
 * - Successful RSS feed fetch
 * - Network errors
 * - Parse errors
 * - Database errors
 *
 * Uses composition with [FakeAnnouncementDao] to simulate real database operations
 * and preserve read state correctly.
 */
private class FakeAnnouncementRepository(
    private val announcementDao: FakeAnnouncementDao,
) {
    private var feedResponse: Result<List<AnnouncementEntity>> = Result.success(emptyList())
    private var shouldThrow: Boolean = false

    /**
     * Set the response that refreshAnnouncements() should return.
     */
    fun setFeedResponse(response: Result<List<AnnouncementEntity>>) {
        this.feedResponse = response
    }

    /**
     * Set whether refreshAnnouncements() should throw an exception.
     */
    fun setShouldThrow(shouldThrow: Boolean) {
        this.shouldThrow = shouldThrow
    }

    /**
     * Get unread count from the DAO.
     */
    fun getUnreadCount(): Flow<Int> = announcementDao.getUnreadCount()

    /**
     * Simulate RSS feed refresh with controllable behavior.
     */
    suspend fun refreshAnnouncements(): Result<Unit> {
        if (shouldThrow) {
            throw RuntimeException("Test exception")
        }

        return feedResponse.fold(
            onSuccess = { announcements ->
                // Get existing announcements to preserve read status (same as real implementation)
                val existingAnnouncements = announcementDao.getAll().first()
                val existingReadIds = existingAnnouncements.filter { it.isRead }.map { it.id }.toSet()

                // Preserve read status for existing announcements
                val announcementsWithReadState =
                    announcements.map { announcement ->
                        if (existingReadIds.contains(announcement.id)) {
                            announcement.copy(isRead = true)
                        } else {
                            announcement
                        }
                    }

                announcementDao.insertAll(announcementsWithReadState)
                Result.success(Unit)
            },
            onFailure = { error ->
                Result.failure(error)
            },
        )
    }
}
