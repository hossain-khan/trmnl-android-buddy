package ink.trmnl.android.buddy.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import ink.trmnl.android.buddy.domain.models.PlaylistItemUi
import org.junit.Test

/**
 * Unit tests for PlaylistItemsRepository utility functions.
 *
 * Tests cover:
 * - Finding currently playing item by most recent renderedAt timestamp
 * - Handling empty item lists
 * - Handling items with null renderedAt values
 * - Fallback behavior when no items have been rendered
 */
class PlaylistItemsRepositoryTest {
    @Test
    fun `getCurrentlyPlayingItem returns null for empty list`() {
        // Given
        val items = emptyList<PlaylistItemUi>()

        // When
        val result = getCurrentlyPlayingItem(items)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `getCurrentlyPlayingItem returns first item when none have renderedAt`() {
        // Given
        val items =
            listOf(
                createTestPlaylistItem(id = 1, displayName = "First Item", renderedAt = null),
                createTestPlaylistItem(id = 2, displayName = "Second Item", renderedAt = null),
                createTestPlaylistItem(id = 3, displayName = "Third Item", renderedAt = null),
            )

        // When
        val result = getCurrentlyPlayingItem(items)

        // Then
        assertThat(result).isEqualTo(items.first())
        assertThat(result?.displayName).isEqualTo("First Item")
    }

    @Test
    fun `getCurrentlyPlayingItem returns item with most recent renderedAt timestamp`() {
        // Given
        val items =
            listOf(
                createTestPlaylistItem(
                    id = 1,
                    displayName = "First Item",
                    renderedAt = "2026-02-14T10:00:00Z",
                ),
                createTestPlaylistItem(
                    id = 2,
                    displayName = "Currently Playing",
                    renderedAt = "2026-02-14T12:00:00Z",
                ),
                createTestPlaylistItem(
                    id = 3,
                    displayName = "Third Item",
                    renderedAt = "2026-02-14T11:00:00Z",
                ),
            )

        // When
        val result = getCurrentlyPlayingItem(items)

        // Then
        assertThat(result?.id).isEqualTo(2)
        assertThat(result?.displayName).isEqualTo("Currently Playing")
    }

    @Test
    fun `getCurrentlyPlayingItem ignores items with null renderedAt`() {
        // Given
        val items =
            listOf(
                createTestPlaylistItem(id = 1, displayName = "No Render Time", renderedAt = null),
                createTestPlaylistItem(
                    id = 2,
                    displayName = "Has Render Time",
                    renderedAt = "2026-02-14T12:00:00Z",
                ),
                createTestPlaylistItem(id = 3, displayName = "Also No Render", renderedAt = null),
            )

        // When
        val result = getCurrentlyPlayingItem(items)

        // Then
        assertThat(result?.id).isEqualTo(2)
        assertThat(result?.displayName).isEqualTo("Has Render Time")
    }

    @Test
    fun `getCurrentlyPlayingItem with single item returns that item`() {
        // Given
        val items =
            listOf(
                createTestPlaylistItem(id = 1, displayName = "Only Item", renderedAt = "2026-02-14T10:00:00Z"),
            )

        // When
        val result = getCurrentlyPlayingItem(items)

        // Then
        assertThat(result).isEqualTo(items.first())
        assertThat(result?.displayName).isEqualTo("Only Item")
    }

    @Test
    fun `getCurrentlyPlayingItem with single item without renderedAt returns that item`() {
        // Given
        val items =
            listOf(createTestPlaylistItem(id = 1, displayName = "Only Item", renderedAt = null))

        // When
        val result = getCurrentlyPlayingItem(items)

        // Then
        assertThat(result).isEqualTo(items.first())
        assertThat(result?.displayName).isEqualTo("Only Item")
    }

    @Test
    fun `getCurrentlyPlayingItem compares timestamps correctly`() {
        // Given - timestamps in mixed order
        val items =
            listOf(
                createTestPlaylistItem(
                    id = 1,
                    displayName = "Item 1",
                    renderedAt = "2026-02-14T15:00:00Z",
                ),
                createTestPlaylistItem(
                    id = 2,
                    displayName = "Item 2",
                    renderedAt = "2026-02-14T09:00:00Z",
                ),
                createTestPlaylistItem(id = 3, displayName = "Item 3", renderedAt = null),
                createTestPlaylistItem(
                    id = 4,
                    displayName = "Item 4",
                    renderedAt = "2026-02-14T20:00:00Z",
                ),
            )

        // When
        val result = getCurrentlyPlayingItem(items)

        // Then
        assertThat(result?.id).isEqualTo(4)
        assertThat(result?.displayName).isEqualTo("Item 4")
    }

    // Helper function to create test playlist items
    private fun createTestPlaylistItem(
        id: Int = 1,
        deviceId: Int = 1,
        displayName: String = "Test Plugin $id",
        isVisible: Boolean = true,
        isMashup: Boolean = false,
        isNeverRendered: Boolean = false,
        renderedAt: String? = null,
        rowOrder: Long = id.toLong(),
        pluginName: String? = "Test Plugin $id",
        mashupId: Int? = null,
    ): PlaylistItemUi =
        PlaylistItemUi(
            id = id,
            deviceId = deviceId,
            displayName = displayName,
            isVisible = isVisible,
            isMashup = isMashup,
            isNeverRendered = isNeverRendered,
            renderedAt = renderedAt,
            rowOrder = rowOrder,
            pluginName = pluginName,
            mashupId = mashupId,
        )
}
