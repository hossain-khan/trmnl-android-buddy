package ink.trmnl.android.buddy.content.db

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Tests for [AnnouncementDao].
 *
 * Uses a fake in-memory implementation for testing without a real database.
 */
class AnnouncementDaoTest {
    private lateinit var dao: AnnouncementDao

    @Before
    fun setup() {
        dao = FakeAnnouncementDao()
    }

    @Test
    fun `insertAll adds announcements to database`() =
        runTest {
            val announcements =
                listOf(
                    createAnnouncement("1", "Title 1"),
                    createAnnouncement("2", "Title 2"),
                )

            dao.insertAll(announcements)

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result).hasSize(2)
            }
        }

    @Test
    fun `insertAll replaces existing announcements with same ID`() =
        runTest {
            val announcement1 = createAnnouncement("1", "Original Title")
            val announcement2 = createAnnouncement("1", "Updated Title")

            dao.insertAll(listOf(announcement1))
            dao.insertAll(listOf(announcement2))

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result).hasSize(1)
                assertThat(result[0].title).isEqualTo("Updated Title")
            }
        }

    @Test
    fun `getAll returns announcements ordered by published date descending`() =
        runTest {
            val date1 = Instant.parse("2024-01-01T12:00:00Z")
            val date2 = Instant.parse("2024-01-02T12:00:00Z")
            val date3 = Instant.parse("2024-01-03T12:00:00Z")

            val announcements =
                listOf(
                    createAnnouncement("1", "Oldest", publishedDate = date1),
                    createAnnouncement("2", "Newest", publishedDate = date3),
                    createAnnouncement("3", "Middle", publishedDate = date2),
                )

            dao.insertAll(announcements)

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result).hasSize(3)
                assertThat(result[0].id).isEqualTo("2") // Newest first
                assertThat(result[1].id).isEqualTo("3")
                assertThat(result[2].id).isEqualTo("1") // Oldest last
            }
        }

    @Test
    fun `getLatest returns limited number of announcements`() =
        runTest {
            val announcements =
                listOf(
                    createAnnouncement("1", "Title 1"),
                    createAnnouncement("2", "Title 2"),
                    createAnnouncement("3", "Title 3"),
                    createAnnouncement("4", "Title 4"),
                )

            dao.insertAll(announcements)

            dao.getLatest(2).test {
                val result = awaitItem()
                assertThat(result).hasSize(2)
            }
        }

    @Test
    fun `getUnread returns only unread announcements`() =
        runTest {
            val announcements =
                listOf(
                    createAnnouncement("1", "Unread 1", isRead = false),
                    createAnnouncement("2", "Read", isRead = true),
                    createAnnouncement("3", "Unread 2", isRead = false),
                )

            dao.insertAll(announcements)

            dao.getUnread().test {
                val result = awaitItem()
                assertThat(result).hasSize(2)
                assertThat(result.all { !it.isRead }).isTrue()
            }
        }

    @Test
    fun `getRead returns only read announcements`() =
        runTest {
            val announcements =
                listOf(
                    createAnnouncement("1", "Unread", isRead = false),
                    createAnnouncement("2", "Read 1", isRead = true),
                    createAnnouncement("3", "Read 2", isRead = true),
                )

            dao.insertAll(announcements)

            dao.getRead().test {
                val result = awaitItem()
                assertThat(result).hasSize(2)
                assertThat(result.all { it.isRead }).isTrue()
            }
        }

    @Test
    fun `markAsRead updates announcement read status to true`() =
        runTest {
            val announcement = createAnnouncement("1", "Title", isRead = false)
            dao.insertAll(listOf(announcement))

            dao.markAsRead("1")

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result[0].isRead).isTrue()
            }
        }

    @Test
    fun `markAsUnread updates announcement read status to false`() =
        runTest {
            val announcement = createAnnouncement("1", "Title", isRead = true)
            dao.insertAll(listOf(announcement))

            dao.markAsUnread("1")

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result[0].isRead).isFalse()
            }
        }

    @Test
    fun `markAllAsRead marks all announcements as read`() =
        runTest {
            val announcements =
                listOf(
                    createAnnouncement("1", "Unread 1", isRead = false),
                    createAnnouncement("2", "Unread 2", isRead = false),
                    createAnnouncement("3", "Unread 3", isRead = false),
                )

            dao.insertAll(announcements)
            dao.markAllAsRead()

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result.all { it.isRead }).isTrue()
            }
        }

    @Test
    fun `deleteOlderThan removes announcements older than threshold`() =
        runTest {
            val now = Instant.now()
            val old = now.minusSeconds(86400 * 30) // 30 days ago
            val recent = now.minusSeconds(86400 * 5) // 5 days ago

            val announcements =
                listOf(
                    createAnnouncement("1", "Old", fetchedAt = old),
                    createAnnouncement("2", "Recent", fetchedAt = recent),
                )

            dao.insertAll(announcements)

            val threshold = now.minusSeconds(86400 * 10).epochSecond // 10 days threshold
            dao.deleteOlderThan(threshold)

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result).hasSize(1)
                assertThat(result[0].id).isEqualTo("2")
            }
        }

    @Test
    fun `getUnreadCount returns correct count of unread announcements`() =
        runTest {
            val announcements =
                listOf(
                    createAnnouncement("1", "Unread 1", isRead = false),
                    createAnnouncement("2", "Read", isRead = true),
                    createAnnouncement("3", "Unread 2", isRead = false),
                    createAnnouncement("4", "Unread 3", isRead = false),
                )

            dao.insertAll(announcements)

            dao.getUnreadCount().test {
                val count = awaitItem()
                assertThat(count).isEqualTo(3)
            }
        }

    @Test
    fun `getUnreadCount returns zero when all are read`() =
        runTest {
            val announcements =
                listOf(
                    createAnnouncement("1", "Read 1", isRead = true),
                    createAnnouncement("2", "Read 2", isRead = true),
                )

            dao.insertAll(announcements)

            dao.getUnreadCount().test {
                val count = awaitItem()
                assertThat(count).isEqualTo(0)
            }
        }

    @Test
    fun `getUnreadCount returns zero when database is empty`() =
        runTest {
            dao.getUnreadCount().test {
                val count = awaitItem()
                assertThat(count).isEqualTo(0)
            }
        }

    @Test
    fun `multiple operations maintain consistency`() =
        runTest {
            // Insert announcements
            dao.insertAll(
                listOf(
                    createAnnouncement("1", "Title 1", isRead = false),
                    createAnnouncement("2", "Title 2", isRead = false),
                ),
            )

            // Mark one as read
            dao.markAsRead("1")

            // Verify state
            dao.getAll().test {
                val all = awaitItem()
                assertThat(all).hasSize(2)
            }

            dao.getUnread().test {
                val unread = awaitItem()
                assertThat(unread).hasSize(1)
                assertThat(unread[0].id).isEqualTo("2")
            }

            dao.getRead().test {
                val read = awaitItem()
                assertThat(read).hasSize(1)
                assertThat(read[0].id).isEqualTo("1")
            }
        }

    @Test
    fun `empty database returns empty lists`() =
        runTest {
            dao.getAll().test {
                assertThat(awaitItem()).isEmpty()
            }

            dao.getUnread().test {
                assertThat(awaitItem()).isEmpty()
            }

            dao.getRead().test {
                assertThat(awaitItem()).isEmpty()
            }
        }

    private fun createAnnouncement(
        id: String,
        title: String,
        isRead: Boolean = false,
        publishedDate: Instant = Instant.now(),
        fetchedAt: Instant = Instant.now(),
    ) = AnnouncementEntity(
        id = id,
        title = title,
        summary = "Summary for $title",
        link = "https://usetrmnl.com/$id",
        publishedDate = publishedDate,
        isRead = isRead,
        fetchedAt = fetchedAt,
    )
}
