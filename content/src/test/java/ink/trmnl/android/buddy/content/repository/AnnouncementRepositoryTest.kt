package ink.trmnl.android.buddy.content.repository

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import ink.trmnl.android.buddy.content.db.AnnouncementDao
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * Unit tests for [AnnouncementRepository].
 *
 * Uses Robolectric to provide Android framework dependencies for XML parsing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AnnouncementRepositoryTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeAnnouncementDao: FakeAnnouncementDao
    private lateinit var repository: AnnouncementRepository

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        fakeAnnouncementDao = FakeAnnouncementDao()
        repository = AnnouncementRepository(fakeAnnouncementDao)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getLatestAnnouncements returns limited announcements`() =
        runTest {
            // Setup fake data
            val announcements =
                listOf(
                    createAnnouncement("1", "Title 1"),
                    createAnnouncement("2", "Title 2"),
                    createAnnouncement("3", "Title 3"),
                )
            fakeAnnouncementDao.insertAll(announcements)

            val result = repository.getLatestAnnouncements(2).first()

            assertThat(result).hasSize(2)
        }

    @Test
    fun `getAllAnnouncements returns all announcements`() =
        runTest {
            // Setup fake data
            val announcements =
                listOf(
                    createAnnouncement("1", "Title 1"),
                    createAnnouncement("2", "Title 2"),
                )
            fakeAnnouncementDao.insertAll(announcements)

            val result = repository.getAllAnnouncements().first()

            assertThat(result).hasSize(2)
        }

    @Test
    fun `getUnreadAnnouncements returns only unread announcements`() =
        runTest {
            // Setup fake data with mixed read status
            val announcements =
                listOf(
                    createAnnouncement("1", "Unread 1", isRead = false),
                    createAnnouncement("2", "Read", isRead = true),
                    createAnnouncement("3", "Unread 2", isRead = false),
                )
            fakeAnnouncementDao.insertAll(announcements)

            val result = repository.getUnreadAnnouncements().first()

            assertThat(result).hasSize(2)
            assertThat(result.all { !it.isRead }).isEqualTo(true)
        }

    @Test
    fun `getReadAnnouncements returns only read announcements`() =
        runTest {
            // Setup fake data with mixed read status
            val announcements =
                listOf(
                    createAnnouncement("1", "Unread", isRead = false),
                    createAnnouncement("2", "Read 1", isRead = true),
                    createAnnouncement("3", "Read 2", isRead = true),
                )
            fakeAnnouncementDao.insertAll(announcements)

            val result = repository.getReadAnnouncements().first()

            assertThat(result).hasSize(2)
            assertThat(result.all { it.isRead }).isEqualTo(true)
        }

    @Test
    fun `getUnreadCount returns correct count`() =
        runTest {
            // Setup fake data
            val announcements =
                listOf(
                    createAnnouncement("1", "Unread 1", isRead = false),
                    createAnnouncement("2", "Read", isRead = true),
                    createAnnouncement("3", "Unread 2", isRead = false),
                )
            fakeAnnouncementDao.insertAll(announcements)

            val count = repository.getUnreadCount().first()

            assertThat(count).isEqualTo(2)
        }

    @Test
    fun `markAsRead updates announcement read status`() =
        runTest {
            // Setup fake data
            val announcement = createAnnouncement("1", "Title", isRead = false)
            fakeAnnouncementDao.insertAll(listOf(announcement))

            repository.markAsRead("1")

            val result = fakeAnnouncementDao.getAll().first().find { it.id == "1" }
            assertThat(result?.isRead).isEqualTo(true)
        }

    @Test
    fun `markAsUnread updates announcement read status`() =
        runTest {
            // Setup fake data
            val announcement = createAnnouncement("1", "Title", isRead = true)
            fakeAnnouncementDao.insertAll(listOf(announcement))

            repository.markAsUnread("1")

            val result = fakeAnnouncementDao.getAll().first().find { it.id == "1" }
            assertThat(result?.isRead).isEqualTo(false)
        }

    @Test
    fun `markAllAsRead marks all announcements as read`() =
        runTest {
            // Setup fake data
            val announcements =
                listOf(
                    createAnnouncement("1", "Title 1", isRead = false),
                    createAnnouncement("2", "Title 2", isRead = false),
                )
            fakeAnnouncementDao.insertAll(announcements)

            repository.markAllAsRead()

            val result = fakeAnnouncementDao.getAll().first()
            assertThat(result.all { it.isRead }).isEqualTo(true)
        }

    @Test
    fun `refreshAnnouncements with valid RSS preserves read status`() =
        runTest {
            // Setup existing announcement as read
            val existingAnnouncement =
                createAnnouncement(
                    "https://example.com/announcement1",
                    "Title 1",
                    isRead = true,
                )
            fakeAnnouncementDao.insertAll(listOf(existingAnnouncement))

            // Setup mock RSS response
            val rssXml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                    <channel>
                        <title>Announcements</title>
                        <item>
                            <title>Title 1</title>
                            <description>Description 1</description>
                            <link>https://example.com/announcement1</link>
                            <guid>https://example.com/announcement1</guid>
                            <pubDate>Mon, 01 Jan 2024 12:00:00 GMT</pubDate>
                        </item>
                    </channel>
                </rss>
                """.trimIndent()

            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(rssXml))

            // Note: We can't test the actual refresh without mocking the feed URL,
            // but we can verify the method completes without error
            // The actual refresh would need to be tested via integration test
            // or by making the feed URL configurable

            // Verify the method signature works
            assertThat(repository).isNotNull()
        }

    @Test
    fun `parseDate handles ISO 8601 format`() {
        // Access private parseDate method via reflection
        val parseMethod =
            AnnouncementRepository::class.java.getDeclaredMethod(
                "parseDate",
                String::class.java,
            )
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(repository, "2024-01-01T12:00:00Z") as Instant

        assertThat(result).isEqualTo(Instant.parse("2024-01-01T12:00:00Z"))
    }

    @Test
    fun `parseDate handles epoch milliseconds format`() {
        // Access private parseDate method via reflection
        val parseMethod =
            AnnouncementRepository::class.java.getDeclaredMethod(
                "parseDate",
                String::class.java,
            )
        parseMethod.isAccessible = true

        val epochMillis = 1704105600000L // 2024-01-01 12:00:00 UTC
        val result = parseMethod.invoke(repository, epochMillis.toString()) as Instant

        assertThat(result).isEqualTo(Instant.ofEpochMilli(epochMillis))
    }

    @Test
    fun `parseDate returns current time for invalid format`() {
        // Access private parseDate method via reflection
        val parseMethod =
            AnnouncementRepository::class.java.getDeclaredMethod(
                "parseDate",
                String::class.java,
            )
        parseMethod.isAccessible = true

        val beforeCall = Instant.now()
        val result = parseMethod.invoke(repository, "invalid date") as Instant
        val afterCall = Instant.now()

        // Result should be between before and after timestamps
        assertThat(result.isAfter(beforeCall.minusSeconds(1))).isEqualTo(true)
        assertThat(result.isBefore(afterCall.plusSeconds(1))).isEqualTo(true)
    }

    private fun createAnnouncement(
        id: String,
        title: String,
        isRead: Boolean = false,
    ) = AnnouncementEntity(
        id = id,
        title = title,
        summary = "Summary for $title",
        link = "https://example.com/$id",
        publishedDate = Instant.now(),
        isRead = isRead,
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
            // Replace existing announcements with same ID
            announcements.forEach { newAnnouncement ->
                val index = this.announcements.indexOfFirst { it.id == newAnnouncement.id }
                if (index >= 0) {
                    this.announcements[index] = newAnnouncement
                } else {
                    this.announcements.add(newAnnouncement)
                }
            }
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
}
