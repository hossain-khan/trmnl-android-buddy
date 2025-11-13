package ink.trmnl.android.buddy.content.db

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Tests for [BlogPostDao].
 *
 * Uses a fake in-memory implementation for testing without a real database.
 */
class BlogPostDaoTest {
    private lateinit var dao: BlogPostDao

    @Before
    fun setup() {
        dao = FakeBlogPostDao()
    }

    @Test
    fun `insertAll adds blog posts to database`() =
        runTest {
            val posts =
                listOf(
                    createBlogPost("1", "Post 1"),
                    createBlogPost("2", "Post 2"),
                )

            dao.insertAll(posts)

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result).hasSize(2)
            }
        }

    @Test
    fun `insertAll ignores existing posts to preserve user state`() =
        runTest {
            val post1 = createBlogPost("1", "Original Title", isFavorite = true, isRead = true)
            val post2 = createBlogPost("1", "Updated Title", isFavorite = false, isRead = false)

            dao.insertAll(listOf(post1))
            dao.insertAll(listOf(post2))

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result).hasSize(1)
                // Should keep original values (IGNORE strategy)
                assertThat(result[0].title).isEqualTo("Original Title")
                assertThat(result[0].isFavorite).isTrue()
                assertThat(result[0].isRead).isTrue()
            }
        }

    @Test
    fun `getAll returns posts ordered by published date descending`() =
        runTest {
            val date1 = Instant.parse("2024-01-01T12:00:00Z")
            val date2 = Instant.parse("2024-01-02T12:00:00Z")
            val date3 = Instant.parse("2024-01-03T12:00:00Z")

            val posts =
                listOf(
                    createBlogPost("1", "Oldest", publishedDate = date1),
                    createBlogPost("2", "Newest", publishedDate = date3),
                    createBlogPost("3", "Middle", publishedDate = date2),
                )

            dao.insertAll(posts)

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result).hasSize(3)
                assertThat(result[0].id).isEqualTo("2") // Newest first
                assertThat(result[1].id).isEqualTo("3")
                assertThat(result[2].id).isEqualTo("1") // Oldest last
            }
        }

    @Test
    fun `getByCategory returns posts filtered by category`() =
        runTest {
            val posts =
                listOf(
                    createBlogPost("1", "TRMNL Post", category = "TRMNL"),
                    createBlogPost("2", "DevOps Post", category = "DevOps"),
                    createBlogPost("3", "Another TRMNL Post", category = "TRMNL"),
                )

            dao.insertAll(posts)

            dao.getByCategory("TRMNL").test {
                val result = awaitItem()
                assertThat(result).hasSize(2)
                assertThat(result.all { it.category == "TRMNL" }).isTrue()
            }
        }

    @Test
    fun `getFavorites returns only favorite posts`() =
        runTest {
            val posts =
                listOf(
                    createBlogPost("1", "Favorite 1", isFavorite = true),
                    createBlogPost("2", "Not Favorite", isFavorite = false),
                    createBlogPost("3", "Favorite 2", isFavorite = true),
                )

            dao.insertAll(posts)

            dao.getFavorites().test {
                val result = awaitItem()
                assertThat(result).hasSize(2)
                assertThat(result.all { it.isFavorite }).isTrue()
            }
        }

    @Test
    fun `getUnread returns only unread posts`() =
        runTest {
            val posts =
                listOf(
                    createBlogPost("1", "Unread 1", isRead = false),
                    createBlogPost("2", "Read", isRead = true),
                    createBlogPost("3", "Unread 2", isRead = false),
                )

            dao.insertAll(posts)

            dao.getUnread().test {
                val result = awaitItem()
                assertThat(result).hasSize(2)
                assertThat(result.all { !it.isRead }).isTrue()
            }
        }

    @Test
    fun `getRecentlyRead returns posts ordered by lastReadAt descending`() =
        runTest {
            val time1 = Instant.parse("2024-01-01T12:00:00Z")
            val time2 = Instant.parse("2024-01-02T12:00:00Z")
            val time3 = Instant.parse("2024-01-03T12:00:00Z")

            val posts =
                listOf(
                    createBlogPost("1", "Read First", lastReadAt = time1),
                    createBlogPost("2", "Read Last", lastReadAt = time3),
                    createBlogPost("3", "Read Middle", lastReadAt = time2),
                    createBlogPost("4", "Never Read", lastReadAt = null),
                )

            dao.insertAll(posts)

            dao.getRecentlyRead().test {
                val result = awaitItem()
                assertThat(result).hasSize(3)
                assertThat(result[0].id).isEqualTo("2") // Most recent
                assertThat(result[1].id).isEqualTo("3")
                assertThat(result[2].id).isEqualTo("1")
            }
        }

    @Test
    fun `getRecentlyRead limits to 10 results`() =
        runTest {
            val posts =
                (1..15).map { i ->
                    createBlogPost(
                        i.toString(),
                        "Post $i",
                        lastReadAt = Instant.now().minusSeconds(i.toLong()),
                    )
                }

            dao.insertAll(posts)

            dao.getRecentlyRead().test {
                val result = awaitItem()
                assertThat(result).hasSize(10)
            }
        }

    @Test
    fun `markAsRead updates post read status`() =
        runTest {
            val post = createBlogPost("1", "Post", isRead = false)
            dao.insertAll(listOf(post))

            dao.markAsRead("1")

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result[0].isRead).isTrue()
            }
        }

    @Test
    fun `markAllAsRead updates all unread posts`() =
        runTest {
            val posts =
                listOf(
                    createBlogPost("1", "Unread 1", isRead = false),
                    createBlogPost("2", "Read", isRead = true),
                    createBlogPost("3", "Unread 2", isRead = false),
                )

            dao.insertAll(posts)

            val timestamp = Instant.now()
            dao.markAllAsRead(timestamp)

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result.all { it.isRead }).isTrue()
                // Posts that were unread should have lastReadAt updated
                val post1 = result.find { it.id == "1" }
                assertThat(post1?.lastReadAt).isNotNull()
            }
        }

    @Test
    fun `getUnreadCount returns correct count`() =
        runTest {
            val posts =
                listOf(
                    createBlogPost("1", "Unread 1", isRead = false),
                    createBlogPost("2", "Read", isRead = true),
                    createBlogPost("3", "Unread 2", isRead = false),
                    createBlogPost("4", "Unread 3", isRead = false),
                )

            dao.insertAll(posts)

            dao.getUnreadCount().test {
                val count = awaitItem()
                assertThat(count).isEqualTo(3)
            }
        }

    @Test
    fun `updateReadingProgress updates progress and timestamp`() =
        runTest {
            val post = createBlogPost("1", "Post", readingProgressPercent = 0f)
            dao.insertAll(listOf(post))

            val timestamp = Instant.now()
            dao.updateReadingProgress("1", 50.0f, timestamp)

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result[0].readingProgressPercent).isEqualTo(50.0f)
                assertThat(result[0].lastReadAt).isEqualTo(timestamp)
            }
        }

    @Test
    fun `toggleFavorite changes favorite status`() =
        runTest {
            val post = createBlogPost("1", "Post", isFavorite = false)
            dao.insertAll(listOf(post))

            dao.toggleFavorite("1")

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result[0].isFavorite).isTrue()
            }

            dao.toggleFavorite("1")

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result[0].isFavorite).isFalse()
            }
        }

    @Test
    fun `updateSummary updates post summary`() =
        runTest {
            val post = createBlogPost("1", "Post", summary = "Original summary")
            dao.insertAll(listOf(post))

            dao.updateSummary("1", "Updated summary")

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result[0].summary).isEqualTo("Updated summary")
            }
        }

    @Test
    fun `deleteOlderThan removes old posts`() =
        runTest {
            val now = Instant.now()
            val old = now.minusSeconds(86400 * 30) // 30 days ago
            val recent = now.minusSeconds(86400 * 5) // 5 days ago

            val posts =
                listOf(
                    createBlogPost("1", "Old", fetchedAt = old),
                    createBlogPost("2", "Recent", fetchedAt = recent),
                )

            dao.insertAll(posts)

            val threshold = now.minusSeconds(86400 * 10).epochSecond
            dao.deleteOlderThan(threshold)

            dao.getAll().test {
                val result = awaitItem()
                assertThat(result).hasSize(1)
                assertThat(result[0].id).isEqualTo("2")
            }
        }

    @Test
    fun `searchPosts finds posts by title`() =
        runTest {
            val posts =
                listOf(
                    createBlogPost("1", "Getting Started with TRMNL"),
                    createBlogPost("2", "Advanced TRMNL Features"),
                    createBlogPost("3", "DevOps Best Practices"),
                )

            dao.insertAll(posts)

            dao.searchPosts("TRMNL").test {
                val result = awaitItem()
                assertThat(result).hasSize(2)
            }
        }

    @Test
    fun `searchPosts finds posts by summary`() =
        runTest {
            val posts =
                listOf(
                    createBlogPost("1", "Post 1", summary = "Learn about widgets"),
                    createBlogPost("2", "Post 2", summary = "Advanced topics"),
                    createBlogPost("3", "Post 3", summary = "Widget configuration"),
                )

            dao.insertAll(posts)

            dao.searchPosts("widget").test {
                val result = awaitItem()
                assertThat(result).hasSize(2)
            }
        }

    @Test
    fun `searchPosts is case insensitive`() =
        runTest {
            val posts =
                listOf(
                    createBlogPost("1", "Getting Started with TRMNL"),
                    createBlogPost("2", "DevOps Best Practices"),
                )

            dao.insertAll(posts)

            dao.searchPosts("trmnl").test {
                val result = awaitItem()
                assertThat(result).hasSize(1)
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

            dao.getFavorites().test {
                assertThat(awaitItem()).isEmpty()
            }

            dao.getRecentlyRead().test {
                assertThat(awaitItem()).isEmpty()
            }
        }

    @Test
    fun `getUnreadCount returns zero when all are read`() =
        runTest {
            val posts =
                listOf(
                    createBlogPost("1", "Read 1", isRead = true),
                    createBlogPost("2", "Read 2", isRead = true),
                )

            dao.insertAll(posts)

            dao.getUnreadCount().test {
                val count = awaitItem()
                assertThat(count).isEqualTo(0)
            }
        }

    private fun createBlogPost(
        id: String,
        title: String,
        summary: String = "Summary for $title",
        category: String? = "TRMNL",
        isRead: Boolean = false,
        isFavorite: Boolean = false,
        readingProgressPercent: Float = 0f,
        lastReadAt: Instant? = null,
        publishedDate: Instant = Instant.now(),
        fetchedAt: Instant = Instant.now(),
    ) = BlogPostEntity(
        id = id,
        title = title,
        summary = summary,
        link = "https://usetrmnl.com/blog/$id",
        authorName = "Test Author",
        category = category,
        publishedDate = publishedDate,
        featuredImageUrl = null,
        isRead = isRead,
        readingProgressPercent = readingProgressPercent,
        lastReadAt = lastReadAt,
        fetchedAt = fetchedAt,
        isFavorite = isFavorite,
    )
}
