package ink.trmnl.android.buddy.content.db

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [AnnouncementEntity] data class.
 *
 * Tests verify the entity's properties, default values, and copy functionality.
 */
class AnnouncementEntityTest {
    @Test
    fun `create AnnouncementEntity with all properties`() {
        val publishedDate = Instant.parse("2024-01-01T12:00:00Z")
        val fetchedAt = Instant.parse("2024-01-02T10:00:00Z")

        val announcement =
            AnnouncementEntity(
                id = "announcement-1",
                title = "New Feature Release",
                summary = "We've released a new feature",
                link = "https://usetrmnl.com/announcement-1",
                publishedDate = publishedDate,
                isRead = true,
                fetchedAt = fetchedAt,
            )

        assertThat(announcement.id).isEqualTo("announcement-1")
        assertThat(announcement.title).isEqualTo("New Feature Release")
        assertThat(announcement.summary).isEqualTo("We've released a new feature")
        assertThat(announcement.link).isEqualTo("https://usetrmnl.com/announcement-1")
        assertThat(announcement.publishedDate).isEqualTo(publishedDate)
        assertThat(announcement.isRead).isTrue()
        assertThat(announcement.fetchedAt).isEqualTo(fetchedAt)
    }

    @Test
    fun `create AnnouncementEntity with default values`() {
        val publishedDate = Instant.parse("2024-01-01T12:00:00Z")
        val beforeCreation = Instant.now().minusSeconds(1)

        val announcement =
            AnnouncementEntity(
                id = "announcement-2",
                title = "Title",
                summary = "Summary",
                link = "https://usetrmnl.com/announcement-2",
                publishedDate = publishedDate,
            )

        val afterCreation = Instant.now().plusSeconds(1)

        // Verify default values
        assertThat(announcement.isRead).isFalse()
        assertThat(announcement.fetchedAt.isAfter(beforeCreation)).isTrue()
        assertThat(announcement.fetchedAt.isBefore(afterCreation)).isTrue()
    }

    @Test
    fun `copy AnnouncementEntity with modified properties`() {
        val original =
            AnnouncementEntity(
                id = "announcement-3",
                title = "Original Title",
                summary = "Original Summary",
                link = "https://usetrmnl.com/announcement-3",
                publishedDate = Instant.parse("2024-01-01T12:00:00Z"),
                isRead = false,
            )

        val modified = original.copy(isRead = true)

        assertThat(modified.id).isEqualTo(original.id)
        assertThat(modified.title).isEqualTo(original.title)
        assertThat(modified.summary).isEqualTo(original.summary)
        assertThat(modified.link).isEqualTo(original.link)
        assertThat(modified.publishedDate).isEqualTo(original.publishedDate)
        assertThat(modified.isRead).isTrue()
        assertThat(modified.fetchedAt).isEqualTo(original.fetchedAt)
    }

    @Test
    fun `copy AnnouncementEntity with multiple modified properties`() {
        val original =
            AnnouncementEntity(
                id = "announcement-4",
                title = "Original Title",
                summary = "Original Summary",
                link = "https://usetrmnl.com/announcement-4",
                publishedDate = Instant.parse("2024-01-01T12:00:00Z"),
                isRead = false,
            )

        val newFetchedAt = Instant.parse("2024-01-03T14:00:00Z")
        val modified =
            original.copy(
                title = "Updated Title",
                isRead = true,
                fetchedAt = newFetchedAt,
            )

        assertThat(modified.id).isEqualTo(original.id)
        assertThat(modified.title).isEqualTo("Updated Title")
        assertThat(modified.summary).isEqualTo(original.summary)
        assertThat(modified.link).isEqualTo(original.link)
        assertThat(modified.publishedDate).isEqualTo(original.publishedDate)
        assertThat(modified.isRead).isTrue()
        assertThat(modified.fetchedAt).isEqualTo(newFetchedAt)
    }

    @Test
    fun `equality checks work correctly`() {
        val publishedDate = Instant.parse("2024-01-01T12:00:00Z")
        val fetchedAt = Instant.parse("2024-01-02T10:00:00Z")

        val announcement1 =
            AnnouncementEntity(
                id = "announcement-5",
                title = "Title",
                summary = "Summary",
                link = "https://usetrmnl.com/announcement-5",
                publishedDate = publishedDate,
                isRead = false,
                fetchedAt = fetchedAt,
            )

        val announcement2 =
            AnnouncementEntity(
                id = "announcement-5",
                title = "Title",
                summary = "Summary",
                link = "https://usetrmnl.com/announcement-5",
                publishedDate = publishedDate,
                isRead = false,
                fetchedAt = fetchedAt,
            )

        assertThat(announcement1).isEqualTo(announcement2)
    }

    @Test
    fun `toString provides readable representation`() {
        val announcement =
            AnnouncementEntity(
                id = "announcement-6",
                title = "Test Title",
                summary = "Test Summary",
                link = "https://usetrmnl.com/announcement-6",
                publishedDate = Instant.parse("2024-01-01T12:00:00Z"),
                isRead = true,
            )

        val stringRepresentation = announcement.toString()

        // Verify the toString generates a valid string representation
        assertThat(stringRepresentation.isNotEmpty()).isTrue()
    }
}
