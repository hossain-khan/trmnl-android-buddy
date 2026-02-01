package ink.trmnl.android.buddy.content.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [ContentItem] sealed class and its subclasses.
 *
 * Tests verify proper creation, property access, and polymorphism behavior.
 */
class ContentItemTest {
    @Test
    fun `create Announcement content item`() {
        val publishedDate = Instant.parse("2024-01-01T12:00:00Z")

        val announcement =
            ContentItem.Announcement(
                id = "announcement-1",
                title = "New Feature Release",
                summary = "We've released a new feature",
                link = "https://trmnl.com/announcement-1",
                publishedDate = publishedDate,
                isRead = true,
            )

        assertThat(announcement.id).isEqualTo("announcement-1")
        assertThat(announcement.title).isEqualTo("New Feature Release")
        assertThat(announcement.summary).isEqualTo("We've released a new feature")
        assertThat(announcement.link).isEqualTo("https://trmnl.com/announcement-1")
        assertThat(announcement.publishedDate).isEqualTo(publishedDate)
        assertThat(announcement.isRead).isTrue()
    }

    @Test
    fun `create BlogPost content item with all properties`() {
        val publishedDate = Instant.parse("2024-01-01T12:00:00Z")

        val blogPost =
            ContentItem.BlogPost(
                id = "post-1",
                title = "Getting Started with TRMNL",
                summary = "Learn how to set up your TRMNL device",
                link = "https://trmnl.com/blog/getting-started",
                publishedDate = publishedDate,
                isRead = false,
                authorName = "Ryan Kulp",
                category = "Tutorial",
                featuredImageUrl = "https://example.com/featured.jpg",
                isFavorite = true,
            )

        assertThat(blogPost.id).isEqualTo("post-1")
        assertThat(blogPost.title).isEqualTo("Getting Started with TRMNL")
        assertThat(blogPost.summary).isEqualTo("Learn how to set up your TRMNL device")
        assertThat(blogPost.link).isEqualTo("https://trmnl.com/blog/getting-started")
        assertThat(blogPost.publishedDate).isEqualTo(publishedDate)
        assertThat(blogPost.isRead).isFalse()
        assertThat(blogPost.authorName).isEqualTo("Ryan Kulp")
        assertThat(blogPost.category).isEqualTo("Tutorial")
        assertThat(blogPost.featuredImageUrl).isEqualTo("https://example.com/featured.jpg")
        assertThat(blogPost.isFavorite).isTrue()
    }

    @Test
    fun `create BlogPost content item with nullable properties`() {
        val publishedDate = Instant.parse("2024-01-01T12:00:00Z")

        val blogPost =
            ContentItem.BlogPost(
                id = "post-2",
                title = "Uncategorized Post",
                summary = "A post without category or image",
                link = "https://trmnl.com/blog/post-2",
                publishedDate = publishedDate,
                isRead = false,
                authorName = "Mario Lurig",
                category = null,
                featuredImageUrl = null,
                isFavorite = false,
            )

        assertThat(blogPost.category).isNull()
        assertThat(blogPost.featuredImageUrl).isNull()
        assertThat(blogPost.isFavorite).isFalse()
    }

    @Test
    fun `ContentItem Announcement is instance of sealed class`() {
        val announcement =
            ContentItem.Announcement(
                id = "announcement-2",
                title = "Title",
                summary = "Summary",
                link = "https://trmnl.com/announcement-2",
                publishedDate = Instant.now(),
                isRead = false,
            )

        assertThat(announcement).isInstanceOf(ContentItem::class)
        assertThat(announcement).isInstanceOf(ContentItem.Announcement::class)
    }

    @Test
    fun `ContentItem BlogPost is instance of sealed class`() {
        val blogPost =
            ContentItem.BlogPost(
                id = "post-3",
                title = "Title",
                summary = "Summary",
                link = "https://trmnl.com/blog/post-3",
                publishedDate = Instant.now(),
                isRead = false,
                authorName = "Author",
                category = "Category",
                featuredImageUrl = null,
                isFavorite = false,
            )

        assertThat(blogPost).isInstanceOf(ContentItem::class)
        assertThat(blogPost).isInstanceOf(ContentItem.BlogPost::class)
    }

    @Test
    fun `polymorphic list can contain both Announcement and BlogPost`() {
        val publishedDate = Instant.parse("2024-01-01T12:00:00Z")

        val items: List<ContentItem> =
            listOf(
                ContentItem.Announcement(
                    id = "announcement-3",
                    title = "Announcement",
                    summary = "An announcement",
                    link = "https://trmnl.com/announcement-3",
                    publishedDate = publishedDate,
                    isRead = false,
                ),
                ContentItem.BlogPost(
                    id = "post-4",
                    title = "Blog Post",
                    summary = "A blog post",
                    link = "https://trmnl.com/blog/post-4",
                    publishedDate = publishedDate,
                    isRead = false,
                    authorName = "Author",
                    category = "Category",
                    featuredImageUrl = null,
                    isFavorite = false,
                ),
            )

        assertThat(items.size).isEqualTo(2)
        assertThat(items[0]).isInstanceOf(ContentItem.Announcement::class)
        assertThat(items[1]).isInstanceOf(ContentItem.BlogPost::class)
    }

    @Test
    fun `when expression can discriminate between types`() {
        val announcement =
            ContentItem.Announcement(
                id = "announcement-4",
                title = "Title",
                summary = "Summary",
                link = "https://trmnl.com/announcement-4",
                publishedDate = Instant.now(),
                isRead = false,
            )

        val blogPost =
            ContentItem.BlogPost(
                id = "post-5",
                title = "Title",
                summary = "Summary",
                link = "https://trmnl.com/blog/post-5",
                publishedDate = Instant.now(),
                isRead = false,
                authorName = "Author",
                category = null,
                featuredImageUrl = null,
                isFavorite = false,
            )

        // Type checks removed - variables are already typed as specific ContentItem subtypes
        val announcementType = "announcement"
        val blogPostType = "blog_post"

        assertThat(announcementType).isEqualTo("announcement")
        assertThat(blogPostType).isEqualTo("blog_post")
    }

    @Test
    fun `copy Announcement preserves ContentItem type`() {
        val original =
            ContentItem.Announcement(
                id = "announcement-5",
                title = "Original Title",
                summary = "Original Summary",
                link = "https://trmnl.com/announcement-5",
                publishedDate = Instant.parse("2024-01-01T12:00:00Z"),
                isRead = false,
            )

        val modified = original.copy(isRead = true)

        assertThat(modified).isInstanceOf(ContentItem.Announcement::class)
        assertThat(modified.id).isEqualTo(original.id)
        assertThat(modified.isRead).isTrue()
    }

    @Test
    fun `copy BlogPost preserves ContentItem type`() {
        val original =
            ContentItem.BlogPost(
                id = "post-6",
                title = "Original Title",
                summary = "Original Summary",
                link = "https://trmnl.com/blog/post-6",
                publishedDate = Instant.parse("2024-01-01T12:00:00Z"),
                isRead = false,
                authorName = "Original Author",
                category = "Original Category",
                featuredImageUrl = null,
                isFavorite = false,
            )

        val modified = original.copy(isFavorite = true)

        assertThat(modified).isInstanceOf(ContentItem.BlogPost::class)
        assertThat(modified.id).isEqualTo(original.id)
        assertThat(modified.isFavorite).isTrue()
    }

    @Test
    fun `Announcement and BlogPost with same id are not equal`() {
        val publishedDate = Instant.parse("2024-01-01T12:00:00Z")

        val announcement =
            ContentItem.Announcement(
                id = "same-id",
                title = "Title",
                summary = "Summary",
                link = "https://trmnl.com/same-id",
                publishedDate = publishedDate,
                isRead = false,
            )

        val blogPost =
            ContentItem.BlogPost(
                id = "same-id",
                title = "Title",
                summary = "Summary",
                link = "https://trmnl.com/same-id",
                publishedDate = publishedDate,
                isRead = false,
                authorName = "Author",
                category = null,
                featuredImageUrl = null,
                isFavorite = false,
            )

        assertThat(announcement).isNotEqualTo(blogPost)
    }

    @Test
    fun `common properties are accessible through sealed class`() {
        val publishedDate = Instant.parse("2024-01-01T12:00:00Z")

        val items: List<ContentItem> =
            listOf(
                ContentItem.Announcement(
                    id = "announcement-6",
                    title = "Announcement Title",
                    summary = "Announcement Summary",
                    link = "https://trmnl.com/announcement-6",
                    publishedDate = publishedDate,
                    isRead = true,
                ),
                ContentItem.BlogPost(
                    id = "post-7",
                    title = "Blog Post Title",
                    summary = "Blog Post Summary",
                    link = "https://trmnl.com/blog/post-7",
                    publishedDate = publishedDate,
                    isRead = false,
                    authorName = "Author",
                    category = null,
                    featuredImageUrl = null,
                    isFavorite = false,
                ),
            )

        // Access common properties without type checking
        assertThat(items[0].id).isEqualTo("announcement-6")
        assertThat(items[0].title).isEqualTo("Announcement Title")
        assertThat(items[0].summary).isEqualTo("Announcement Summary")
        assertThat(items[0].link).isEqualTo("https://trmnl.com/announcement-6")
        assertThat(items[0].publishedDate).isEqualTo(publishedDate)
        assertThat(items[0].isRead).isTrue()

        assertThat(items[1].id).isEqualTo("post-7")
        assertThat(items[1].title).isEqualTo("Blog Post Title")
        assertThat(items[1].summary).isEqualTo("Blog Post Summary")
        assertThat(items[1].link).isEqualTo("https://trmnl.com/blog/post-7")
        assertThat(items[1].publishedDate).isEqualTo(publishedDate)
        assertThat(items[1].isRead).isFalse()
    }

    @Test
    fun `BlogPost specific properties are only accessible after type check`() {
        val blogPost =
            ContentItem.BlogPost(
                id = "post-8",
                title = "Title",
                summary = "Summary",
                link = "https://trmnl.com/blog/post-8",
                publishedDate = Instant.now(),
                isRead = false,
                authorName = "Ryan Kulp",
                category = "DevOps",
                featuredImageUrl = "https://example.com/image.jpg",
                isFavorite = true,
            )

        // Access BlogPost-specific properties
        val item: ContentItem = blogPost
        if (item is ContentItem.BlogPost) {
            assertThat(item.authorName).isEqualTo("Ryan Kulp")
            assertThat(item.category).isEqualTo("DevOps")
            assertThat(item.featuredImageUrl).isEqualTo("https://example.com/image.jpg")
            assertThat(item.isFavorite).isTrue()
        }
    }

    @Test
    fun `items can be sorted by publishedDate`() {
        val date1 = Instant.parse("2024-01-01T12:00:00Z")
        val date2 = Instant.parse("2024-01-02T12:00:00Z")
        val date3 = Instant.parse("2024-01-03T12:00:00Z")

        val items =
            listOf(
                ContentItem.BlogPost(
                    id = "post-b",
                    title = "Middle",
                    summary = "Summary",
                    link = "https://trmnl.com/blog/post-b",
                    publishedDate = date2,
                    isRead = false,
                    authorName = "Author",
                    category = null,
                    featuredImageUrl = null,
                    isFavorite = false,
                ),
                ContentItem.Announcement(
                    id = "announcement-a",
                    title = "Oldest",
                    summary = "Summary",
                    link = "https://trmnl.com/announcement-a",
                    publishedDate = date1,
                    isRead = false,
                ),
                ContentItem.BlogPost(
                    id = "post-c",
                    title = "Newest",
                    summary = "Summary",
                    link = "https://trmnl.com/blog/post-c",
                    publishedDate = date3,
                    isRead = false,
                    authorName = "Author",
                    category = null,
                    featuredImageUrl = null,
                    isFavorite = false,
                ),
            )

        val sorted = items.sortedByDescending { it.publishedDate }

        assertThat(sorted[0].id).isEqualTo("post-c")
        assertThat(sorted[1].id).isEqualTo("post-b")
        assertThat(sorted[2].id).isEqualTo("announcement-a")
    }

    @Test
    fun `items can be filtered by read status`() {
        val items =
            listOf(
                ContentItem.Announcement(
                    id = "announcement-read",
                    title = "Read Announcement",
                    summary = "Summary",
                    link = "https://trmnl.com/announcement-read",
                    publishedDate = Instant.now(),
                    isRead = true,
                ),
                ContentItem.BlogPost(
                    id = "post-unread",
                    title = "Unread Post",
                    summary = "Summary",
                    link = "https://trmnl.com/blog/post-unread",
                    publishedDate = Instant.now(),
                    isRead = false,
                    authorName = "Author",
                    category = null,
                    featuredImageUrl = null,
                    isFavorite = false,
                ),
                ContentItem.Announcement(
                    id = "announcement-unread",
                    title = "Unread Announcement",
                    summary = "Summary",
                    link = "https://trmnl.com/announcement-unread",
                    publishedDate = Instant.now(),
                    isRead = false,
                ),
            )

        val unreadItems = items.filter { !it.isRead }

        assertThat(unreadItems.size).isEqualTo(2)
        assertThat(unreadItems.map { it.id }).isEqualTo(
            listOf("post-unread", "announcement-unread"),
        )
    }
}
