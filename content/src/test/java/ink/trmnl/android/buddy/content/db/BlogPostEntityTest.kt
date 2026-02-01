package ink.trmnl.android.buddy.content.db

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [BlogPostEntity] data class.
 *
 * Tests verify the entity's properties, default values, and copy functionality.
 */
class BlogPostEntityTest {
    @Test
    fun `create BlogPostEntity with all properties`() {
        val publishedDate = Instant.parse("2024-01-01T12:00:00Z")
        val lastReadAt = Instant.parse("2024-01-05T14:00:00Z")
        val fetchedAt = Instant.parse("2024-01-02T10:00:00Z")
        val imageUrls = listOf("https://example.com/image1.jpg", "https://example.com/image2.jpg")

        val blogPost =
            BlogPostEntity(
                id = "post-1",
                title = "Getting Started with TRMNL",
                summary = "Learn how to set up your TRMNL device",
                link = "https://trmnl.com/blog/getting-started",
                authorName = "Ryan Kulp",
                category = "Tutorial",
                publishedDate = publishedDate,
                featuredImageUrl = "https://example.com/featured.jpg",
                imageUrls = imageUrls,
                isRead = true,
                readingProgressPercent = 75.5f,
                lastReadAt = lastReadAt,
                fetchedAt = fetchedAt,
                isFavorite = true,
            )

        assertThat(blogPost.id).isEqualTo("post-1")
        assertThat(blogPost.title).isEqualTo("Getting Started with TRMNL")
        assertThat(blogPost.summary).isEqualTo("Learn how to set up your TRMNL device")
        assertThat(blogPost.link).isEqualTo("https://trmnl.com/blog/getting-started")
        assertThat(blogPost.authorName).isEqualTo("Ryan Kulp")
        assertThat(blogPost.category).isEqualTo("Tutorial")
        assertThat(blogPost.publishedDate).isEqualTo(publishedDate)
        assertThat(blogPost.featuredImageUrl).isEqualTo("https://example.com/featured.jpg")
        assertThat(blogPost.imageUrls).isEqualTo(imageUrls)
        assertThat(blogPost.isRead).isTrue()
        assertThat(blogPost.readingProgressPercent).isEqualTo(75.5f)
        assertThat(blogPost.lastReadAt).isEqualTo(lastReadAt)
        assertThat(blogPost.fetchedAt).isEqualTo(fetchedAt)
        assertThat(blogPost.isFavorite).isTrue()
    }

    @Test
    fun `create BlogPostEntity with default values`() {
        val publishedDate = Instant.parse("2024-01-01T12:00:00Z")
        val beforeCreation = Instant.now().minusSeconds(1)

        val blogPost =
            BlogPostEntity(
                id = "post-2",
                title = "Title",
                summary = "Summary",
                link = "https://trmnl.com/blog/post-2",
                authorName = "Author",
                category = null,
                publishedDate = publishedDate,
                featuredImageUrl = null,
            )

        val afterCreation = Instant.now().plusSeconds(1)

        // Verify default values
        assertThat(blogPost.imageUrls).isNull()
        assertThat(blogPost.isRead).isFalse()
        assertThat(blogPost.readingProgressPercent).isEqualTo(0f)
        assertThat(blogPost.lastReadAt).isNull()
        assertThat(blogPost.fetchedAt.isAfter(beforeCreation)).isTrue()
        assertThat(blogPost.fetchedAt.isBefore(afterCreation)).isTrue()
        assertThat(blogPost.isFavorite).isFalse()
    }

    @Test
    fun `create BlogPostEntity with nullable category`() {
        val blogPost =
            BlogPostEntity(
                id = "post-3",
                title = "Uncategorized Post",
                summary = "A post without a category",
                link = "https://trmnl.com/blog/post-3",
                authorName = "Mario Lurig",
                category = null,
                publishedDate = Instant.parse("2024-01-01T12:00:00Z"),
                featuredImageUrl = null,
            )

        assertThat(blogPost.category).isNull()
        assertThat(blogPost.featuredImageUrl).isNull()
    }

    @Test
    fun `copy BlogPostEntity marking as read`() {
        val original =
            BlogPostEntity(
                id = "post-4",
                title = "Original Title",
                summary = "Original Summary",
                link = "https://trmnl.com/blog/post-4",
                authorName = "Author",
                category = "DevOps",
                publishedDate = Instant.parse("2024-01-01T12:00:00Z"),
                featuredImageUrl = null,
                isRead = false,
                readingProgressPercent = 0f,
            )

        val readTime = Instant.parse("2024-01-10T15:00:00Z")
        val modified =
            original.copy(
                isRead = true,
                lastReadAt = readTime,
            )

        assertThat(modified.id).isEqualTo(original.id)
        assertThat(modified.title).isEqualTo(original.title)
        assertThat(modified.isRead).isTrue()
        assertThat(modified.lastReadAt).isEqualTo(readTime)
        assertThat(modified.readingProgressPercent).isEqualTo(0f)
    }

    @Test
    fun `copy BlogPostEntity updating reading progress`() {
        val original =
            BlogPostEntity(
                id = "post-5",
                title = "In Progress Post",
                summary = "A post being read",
                link = "https://trmnl.com/blog/post-5",
                authorName = "Author",
                category = "TRMNL",
                publishedDate = Instant.parse("2024-01-01T12:00:00Z"),
                featuredImageUrl = null,
                readingProgressPercent = 25.0f,
            )

        val modified = original.copy(readingProgressPercent = 50.0f)

        assertThat(modified.id).isEqualTo(original.id)
        assertThat(modified.readingProgressPercent).isEqualTo(50.0f)
    }

    @Test
    fun `copy BlogPostEntity toggling favorite`() {
        val original =
            BlogPostEntity(
                id = "post-6",
                title = "Favorite Post",
                summary = "A post to favorite",
                link = "https://trmnl.com/blog/post-6",
                authorName = "Author",
                category = "Tutorial",
                publishedDate = Instant.parse("2024-01-01T12:00:00Z"),
                featuredImageUrl = null,
                isFavorite = false,
            )

        val modified = original.copy(isFavorite = true)

        assertThat(modified.id).isEqualTo(original.id)
        assertThat(modified.isFavorite).isTrue()
    }

    @Test
    fun `copy BlogPostEntity with multiple image URLs`() {
        val original =
            BlogPostEntity(
                id = "post-7",
                title = "Post with Images",
                summary = "A post with multiple images",
                link = "https://trmnl.com/blog/post-7",
                authorName = "Author",
                category = "Tutorial",
                publishedDate = Instant.parse("2024-01-01T12:00:00Z"),
                featuredImageUrl = "https://example.com/featured.jpg",
                imageUrls = null,
            )

        val newImageUrls =
            listOf(
                "https://example.com/image1.jpg",
                "https://example.com/image2.png",
                "https://example.com/image3.webp",
            )
        val modified = original.copy(imageUrls = newImageUrls)

        assertThat(modified.id).isEqualTo(original.id)
        assertThat(modified.imageUrls).isEqualTo(newImageUrls)
        assertThat(modified.featuredImageUrl).isEqualTo("https://example.com/featured.jpg")
    }

    @Test
    fun `equality checks work correctly`() {
        val publishedDate = Instant.parse("2024-01-01T12:00:00Z")
        val fetchedAt = Instant.parse("2024-01-02T10:00:00Z")

        val post1 =
            BlogPostEntity(
                id = "post-8",
                title = "Title",
                summary = "Summary",
                link = "https://trmnl.com/blog/post-8",
                authorName = "Author",
                category = "Category",
                publishedDate = publishedDate,
                featuredImageUrl = null,
                fetchedAt = fetchedAt,
            )

        val post2 =
            BlogPostEntity(
                id = "post-8",
                title = "Title",
                summary = "Summary",
                link = "https://trmnl.com/blog/post-8",
                authorName = "Author",
                category = "Category",
                publishedDate = publishedDate,
                featuredImageUrl = null,
                fetchedAt = fetchedAt,
            )

        assertThat(post1).isEqualTo(post2)
    }

    @Test
    fun `toString provides readable representation`() {
        val blogPost =
            BlogPostEntity(
                id = "post-9",
                title = "Test Blog Post",
                summary = "Test Summary",
                link = "https://trmnl.com/blog/post-9",
                authorName = "Test Author",
                category = "Test Category",
                publishedDate = Instant.parse("2024-01-01T12:00:00Z"),
                featuredImageUrl = null,
            )

        val stringRepresentation = blogPost.toString()

        // Verify the toString generates a valid string representation
        assertThat(stringRepresentation.isNotEmpty()).isTrue()
    }

    @Test
    fun `reading progress can be 100 percent`() {
        val blogPost =
            BlogPostEntity(
                id = "post-10",
                title = "Completed Post",
                summary = "A fully read post",
                link = "https://trmnl.com/blog/post-10",
                authorName = "Author",
                category = "Tutorial",
                publishedDate = Instant.parse("2024-01-01T12:00:00Z"),
                featuredImageUrl = null,
                readingProgressPercent = 100f,
            )

        assertThat(blogPost.readingProgressPercent).isEqualTo(100f)
    }
}
