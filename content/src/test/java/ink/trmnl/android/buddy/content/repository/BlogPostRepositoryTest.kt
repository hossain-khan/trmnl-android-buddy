package ink.trmnl.android.buddy.content.repository

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEmpty
import com.prof18.rssparser.RssParser
import ink.trmnl.android.buddy.content.db.BlogPostDao
import ink.trmnl.android.buddy.content.db.BlogPostEntity
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

/**
 * Unit tests for [BlogPostRepository] RSS parsing functionality.
 * Uses the real sample XML from test resources to verify parsing.
 *
 * Uses Robolectric to provide Android framework dependencies for XML parsing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // Use Android API 28 for testing
class BlogPostRepositoryTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeBlogPostDao: FakeBlogPostDao
    private lateinit var repository: BlogPostRepository

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        fakeBlogPostDao = FakeBlogPostDao()
        repository = BlogPostRepository(fakeBlogPostDao)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `test RSS parser extracts content from real sample XML`() =
        runTest {
            // Load the actual sample XML file
            val sampleXml = loadSampleXmlFile()

            // Setup mock server to return sample XML
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(sampleXml),
            )

            // Parse the feed directly to test RSS parser behavior
            val rssParser = RssParser()
            val channel = rssParser.getRssChannel(mockWebServer.url("/").toString())

            // Verify we got items
            assertThat(channel.items).isNotEmpty()

            // Check first item (Model X Progress Report)
            val firstItem = channel.items[0]

            assertThat(firstItem.title).isEqualTo("Model X Progress Report")
            assertThat(firstItem.link).isEqualTo("https://usetrmnl.com/blog/model-x-progress")

            // DIAGNOSTIC: Check what we actually got
            val hasContent = firstItem.content?.isNotBlank() == true
            val hasDescription = firstItem.description?.isNotBlank() == true

            // At least ONE should have text
            val hasAnyText = hasContent || hasDescription
            assertThat(hasAnyText).isEqualTo(true)

            // If we have content, verify it contains expected text
            if (hasContent) {
                assertThat(firstItem.content!!).contains("introduced X")
            }
        }

    @Test
    fun `test sanitizeHtmlToPlainText extracts text from sample post`() =
        runTest {
            // Load actual sample and parse
            val sampleXml = loadSampleXmlFile()
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(sampleXml))

            val rssParser = RssParser()
            val channel = rssParser.getRssChannel(mockWebServer.url("/").toString())

            val firstItem = channel.items[0]

            // Use reflection to access private sanitizeHtmlToPlainText method for testing
            val sanitizeMethod =
                BlogPostRepository::class.java.getDeclaredMethod(
                    "sanitizeHtmlToPlainText",
                    String::class.java,
                    Int::class.javaPrimitiveType,
                )
            sanitizeMethod.isAccessible = true

            // Test with content field
            val plainTextFromContent =
                sanitizeMethod.invoke(
                    repository,
                    firstItem.content,
                    300,
                ) as String

            // Should have extracted plain text without HTML tags
            assertThat(plainTextFromContent).isNotEmpty()
            assertThat(plainTextFromContent.trim().length).isGreaterThan(0)

            // Should not contain HTML tags
            val hasDiv = plainTextFromContent.contains("<div")
            val hasPTag = plainTextFromContent.contains("<p>")
            assertThat(hasDiv).isFalse()
            assertThat(hasPTag).isFalse()

            // Should contain actual text content
            assertThat(plainTextFromContent).contains("introduced X")
        }

    @Test
    fun `test full repository refresh with sample XML creates blog posts`() =
        runTest {
            // This test verifies the full flow from XML -> parsing -> entity creation
            // Note: We can't fully test this without mocking the URL constant in BlogPostRepository
            // But we can test the data transformation logic

            val sampleXml = loadSampleXmlFile()
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(sampleXml))

            val rssParser = RssParser()
            val channel = rssParser.getRssChannel(mockWebServer.url("/").toString())

            // Simulate what refreshBlogPosts() does
            val blogPosts =
                channel.items.mapNotNull { item ->
                    val sanitizeMethod =
                        BlogPostRepository::class.java.getDeclaredMethod(
                            "sanitizeHtmlToPlainText",
                            String::class.java,
                            Int::class.javaPrimitiveType,
                        )
                    sanitizeMethod.isAccessible = true

                    val rawSummary = item.content?.takeIf { it.isNotBlank() } ?: item.description
                    val sanitizedSummary =
                        sanitizeMethod.invoke(
                            repository,
                            rawSummary,
                            300,
                        ) as String

                    BlogPostEntity(
                        id = item.link ?: item.guid ?: return@mapNotNull null,
                        title = item.title ?: return@mapNotNull null,
                        summary = sanitizedSummary,
                        link = item.link ?: return@mapNotNull null,
                        authorName = item.author ?: "TRMNL Team",
                        category = item.categories.firstOrNull(),
                        publishedDate = java.time.Instant.now(),
                        featuredImageUrl = null,
                        fetchedAt = java.time.Instant.now(),
                    )
                }

            // Verify blog posts were created
            assertThat(blogPosts).isNotEmpty()

            // Verify summaries are not blank
            blogPosts.forEach { post ->
                println("Post: ${post.title}")
                println("Summary: ${post.summary}")
                println("Summary length: ${post.summary.length}")
                println("---")

                assertThat(post.summary).isNotEmpty()
                assertThat(post.summary.trim().length).isGreaterThan(0)
            }

            // Verify specific post
            val modelXPost = blogPosts.first { it.title == "Model X Progress Report" }
            assertThat(modelXPost.summary).contains("introduced X")
            assertThat(modelXPost.summary.length).isGreaterThan(50)
        }

    /**
     * Load the sample XML file from test resources.
     */
    private fun loadSampleXmlFile(): String {
        val resourceStream =
            javaClass.classLoader?.getResourceAsStream("usetrmnl.com-blog-posts.xml")
                ?: throw IllegalStateException("Sample XML file not found in test resources")

        return resourceStream.bufferedReader().use { it.readText() }
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
                posts[index] = it.copy(isRead = true, lastReadAt = java.time.Instant.now())
            }
        }

        override suspend fun updateReadingProgress(
            id: String,
            progress: Float,
            lastReadAt: java.time.Instant,
        ) {
            posts.find { it.id == id }?.let {
                val index = posts.indexOf(it)
                posts[index] = it.copy(lastReadAt = lastReadAt)
            }
        }

        override suspend fun toggleFavorite(id: String) {
            posts.find { it.id == id }?.let {
                val index = posts.indexOf(it)
                posts[index] = it.copy(isFavorite = !it.isFavorite)
            }
        }

        override suspend fun deleteOlderThan(timestamp: Long) {
            posts.removeAll { it.fetchedAt.epochSecond < timestamp }
        }
    }
}
