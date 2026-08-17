package ink.trmnl.android.buddy.data

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.ApiResponse
import ink.trmnl.android.buddy.api.models.PlaylistItem
import ink.trmnl.android.buddy.api.models.PluginSetting
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.domain.models.PlaylistItemUi
import ink.trmnl.android.buddy.fakes.FakeTrmnlApiService
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [PlaylistItemsRepositoryImpl] and [PlaylistItemsRepository] utility functions.
 *
 * Tests cover:
 * - In-memory caching, TTL, and cache invalidation
 * - Sorting by row_order
 * - API authentication token validation
 * - Optimistic visibility updates and failure rollbacks
 * - Device-specific filtering
 * - Utility functions like finding currently playing items
 */
class PlaylistItemsRepositoryTest {
    // ========== PlaylistItemsRepositoryImpl Tests ==========

    @Test
    fun `getPlaylistItems returns failure when API key is not configured`() =
        runTest {
            val userPrefsRepo = FakeUserPreferencesRepository(initialPreferences = UserPreferences(apiToken = null))
            val fakeApiService = FakeTrmnlApiService()
            val repository = PlaylistItemsRepositoryImpl(fakeApiService, userPrefsRepo)

            val result = repository.getPlaylistItems()

            assertThat(result.isFailure).isTrue()
            assertThat(fakeApiService.getPlaylistItemsCallCount).isEqualTo(0)
        }

    @Test
    fun `getPlaylistItems fetches from API, sorts by rowOrder, caches results and emits to itemsFlow`() =
        runTest {
            val userPrefsRepo = FakeUserPreferencesRepository(initialPreferences = UserPreferences(apiToken = "test_api_key"))
            val fakeApiService = FakeTrmnlApiService()
            val item1 = createApiPlaylistItem(id = 1, rowOrder = 200, pluginName = "Plugin B")
            val item2 = createApiPlaylistItem(id = 2, rowOrder = 100, pluginName = "Plugin A")
            fakeApiService.getPlaylistItemsResult = ApiResult.success(ApiResponse(listOf(item1, item2)))
            val repository = PlaylistItemsRepositoryImpl(fakeApiService, userPrefsRepo)

            val result = repository.getPlaylistItems()

            assertThat(result.isSuccess).isTrue()
            val items = result.getOrThrow()
            assertThat(items).hasSize(2)
            // Sorted by rowOrder ascending (100 before 200)
            assertThat(items[0].id).isEqualTo(2)
            assertThat(items[1].id).isEqualTo(1)

            // Emitted to flow
            val flowItems = repository.itemsFlow.first()
            assertThat(flowItems).hasSize(2)
            assertThat(flowItems[0].id).isEqualTo(2)

            // Cache check
            assertThat(repository.isCacheStale()).isFalse()
            assertThat(fakeApiService.getPlaylistItemsCallCount).isEqualTo(1)
        }

    @Test
    fun `getPlaylistItems uses cache on subsequent calls when fresh without calling API`() =
        runTest {
            val userPrefsRepo = FakeUserPreferencesRepository(initialPreferences = UserPreferences(apiToken = "test_api_key"))
            val fakeApiService = FakeTrmnlApiService()
            val item = createApiPlaylistItem(id = 1, rowOrder = 100)
            fakeApiService.getPlaylistItemsResult = ApiResult.success(ApiResponse(listOf(item)))
            val repository = PlaylistItemsRepositoryImpl(fakeApiService, userPrefsRepo)

            // First call fetches from API
            val result1 = repository.getPlaylistItems()
            assertThat(result1.isSuccess).isTrue()
            assertThat(fakeApiService.getPlaylistItemsCallCount).isEqualTo(1)

            // Second call uses cache
            val result2 = repository.getPlaylistItems(forceRefresh = false)
            assertThat(result2.isSuccess).isTrue()
            assertThat(fakeApiService.getPlaylistItemsCallCount).isEqualTo(1)
            assertThat(result2.getOrThrow()).hasSize(1)
        }

    @Test
    fun `getPlaylistItems with forceRefresh true bypasses cache and calls API`() =
        runTest {
            val userPrefsRepo = FakeUserPreferencesRepository(initialPreferences = UserPreferences(apiToken = "test_api_key"))
            val fakeApiService = FakeTrmnlApiService()
            val item = createApiPlaylistItem(id = 1, rowOrder = 100)
            fakeApiService.getPlaylistItemsResult = ApiResult.success(ApiResponse(listOf(item)))
            val repository = PlaylistItemsRepositoryImpl(fakeApiService, userPrefsRepo)

            // First call fetches
            repository.getPlaylistItems()
            assertThat(fakeApiService.getPlaylistItemsCallCount).isEqualTo(1)

            // Force refresh call
            val result = repository.getPlaylistItems(forceRefresh = true)
            assertThat(result.isSuccess).isTrue()
            assertThat(fakeApiService.getPlaylistItemsCallCount).isEqualTo(2)
        }

    @Test
    fun `getPlaylistItems returns failure when API returns error`() =
        runTest {
            val userPrefsRepo = FakeUserPreferencesRepository(initialPreferences = UserPreferences(apiToken = "test_api_key"))
            val fakeApiService = FakeTrmnlApiService()
            fakeApiService.getPlaylistItemsResult = ApiResult.httpFailure(401, ApiError("Unauthorized"))
            val repository = PlaylistItemsRepositoryImpl(fakeApiService, userPrefsRepo)

            val result = repository.getPlaylistItems()

            assertThat(result.isFailure).isTrue()
        }

    @Test
    fun `getPlaylistItemsForDevice filters items by device ID`() =
        runTest {
            val userPrefsRepo = FakeUserPreferencesRepository(initialPreferences = UserPreferences(apiToken = "test_api_key"))
            val fakeApiService = FakeTrmnlApiService()
            val itemDevice1 = createApiPlaylistItem(id = 1, deviceId = 100, rowOrder = 1)
            val itemDevice2 = createApiPlaylistItem(id = 2, deviceId = 200, rowOrder = 2)
            fakeApiService.getPlaylistItemsResult = ApiResult.success(ApiResponse(listOf(itemDevice1, itemDevice2)))
            val repository = PlaylistItemsRepositoryImpl(fakeApiService, userPrefsRepo)

            val device1Items = repository.getPlaylistItemsForDevice(deviceId = 100)
            assertThat(device1Items.isSuccess).isTrue()
            assertThat(device1Items.getOrThrow()).hasSize(1)
            assertThat(device1Items.getOrThrow()[0].deviceId).isEqualTo(100)

            val device2Items = repository.getPlaylistItemsForDevice(deviceId = 200)
            assertThat(device2Items.isSuccess).isTrue()
            assertThat(device2Items.getOrThrow()).hasSize(1)
            assertThat(device2Items.getOrThrow()[0].deviceId).isEqualTo(200)
        }

    @Test
    fun `clearCache resets cache and emits empty list to itemsFlow`() =
        runTest {
            val userPrefsRepo = FakeUserPreferencesRepository(initialPreferences = UserPreferences(apiToken = "test_api_key"))
            val fakeApiService = FakeTrmnlApiService()
            val item = createApiPlaylistItem(id = 1, rowOrder = 1)
            fakeApiService.getPlaylistItemsResult = ApiResult.success(ApiResponse(listOf(item)))
            val repository = PlaylistItemsRepositoryImpl(fakeApiService, userPrefsRepo)

            repository.getPlaylistItems()
            assertThat(repository.itemsFlow.value).hasSize(1)
            assertThat(repository.isCacheStale()).isFalse()

            repository.clearCache()
            assertThat(repository.itemsFlow.value).isEmpty()
            assertThat(repository.isCacheStale()).isTrue()
        }

    @Test
    fun `updatePlaylistItemVisibility returns failure when API key is missing`() =
        runTest {
            val userPrefsRepo = FakeUserPreferencesRepository(initialPreferences = UserPreferences(apiToken = null))
            val fakeApiService = FakeTrmnlApiService()
            val repository = PlaylistItemsRepositoryImpl(fakeApiService, userPrefsRepo)

            val result = repository.updatePlaylistItemVisibility(itemId = 1, visible = false)

            assertThat(result.isFailure).isTrue()
        }

    @Test
    fun `updatePlaylistItemVisibility returns success null when item is not in cache`() =
        runTest {
            val userPrefsRepo = FakeUserPreferencesRepository(initialPreferences = UserPreferences(apiToken = "test_key"))
            val fakeApiService = FakeTrmnlApiService()
            val repository = PlaylistItemsRepositoryImpl(fakeApiService, userPrefsRepo)

            // Cache is empty
            val result = repository.updatePlaylistItemVisibility(itemId = 999, visible = false)

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow()).isNull()
            assertThat(fakeApiService.updatePlaylistItemVisibilityCallCount).isEqualTo(0)
        }

    @Test
    fun `updatePlaylistItemVisibility optimistically updates cache and succeeds on API success`() =
        runTest {
            val userPrefsRepo = FakeUserPreferencesRepository(initialPreferences = UserPreferences(apiToken = "test_key"))
            val fakeApiService = FakeTrmnlApiService()
            val item = createApiPlaylistItem(id = 1, visible = true, rowOrder = 1)
            fakeApiService.getPlaylistItemsResult = ApiResult.success(ApiResponse(listOf(item)))
            fakeApiService.updatePlaylistItemVisibilityResult = ApiResult.success(Unit)
            val repository = PlaylistItemsRepositoryImpl(fakeApiService, userPrefsRepo)

            // Populate cache
            repository.getPlaylistItems()
            assertThat(
                repository.itemsFlow.value
                    .first()
                    .isVisible,
            ).isTrue()

            // Update visibility
            val result = repository.updatePlaylistItemVisibility(itemId = 1, visible = false)

            assertThat(result.isSuccess).isTrue()
            val updated = result.getOrThrow()
            assertThat(updated).isNotNull()
            assertThat(updated!!.isVisible).isFalse()

            // State flow updated
            assertThat(
                repository.itemsFlow.value
                    .first()
                    .isVisible,
            ).isFalse()
            assertThat(fakeApiService.lastUpdatePlaylistItemId).isEqualTo(1)
            assertThat(fakeApiService.lastUpdatePlaylistItemBody).isEqualTo(mapOf("visible" to false))
        }

    @Test
    fun `updatePlaylistItemVisibility reverts optimistic update on API failure`() =
        runTest {
            val userPrefsRepo = FakeUserPreferencesRepository(initialPreferences = UserPreferences(apiToken = "test_key"))
            val fakeApiService = FakeTrmnlApiService()
            val item = createApiPlaylistItem(id = 1, visible = true, rowOrder = 1)
            fakeApiService.getPlaylistItemsResult = ApiResult.success(ApiResponse(listOf(item)))
            fakeApiService.updatePlaylistItemVisibilityResult = ApiResult.httpFailure(500, ApiError("Server Error"))
            val repository = PlaylistItemsRepositoryImpl(fakeApiService, userPrefsRepo)

            // Populate cache
            repository.getPlaylistItems()
            assertThat(
                repository.itemsFlow.value
                    .first()
                    .isVisible,
            ).isTrue()

            // Update visibility fails on API
            val result = repository.updatePlaylistItemVisibility(itemId = 1, visible = false)

            assertThat(result.isFailure).isTrue()
            // Reverted back to true
            assertThat(
                repository.itemsFlow.value
                    .first()
                    .isVisible,
            ).isTrue()
        }

    @Test
    fun `updatePlaylistItemVisibility reverts optimistic update on unexpected exception`() =
        runTest {
            val userPrefsRepo = FakeUserPreferencesRepository(initialPreferences = UserPreferences(apiToken = "test_key"))
            val fakeApiService = FakeTrmnlApiService()
            val item = createApiPlaylistItem(id = 1, visible = true, rowOrder = 1)
            fakeApiService.getPlaylistItemsResult = ApiResult.success(ApiResponse(listOf(item)))
            fakeApiService.updatePlaylistItemVisibilityException = RuntimeException("Network timeout")
            val repository = PlaylistItemsRepositoryImpl(fakeApiService, userPrefsRepo)

            // Populate cache
            repository.getPlaylistItems()
            assertThat(
                repository.itemsFlow.value
                    .first()
                    .isVisible,
            ).isTrue()

            // Update visibility throws
            val result = repository.updatePlaylistItemVisibility(itemId = 1, visible = false)

            assertThat(result.isFailure).isTrue()
            // Reverted back to true
            assertThat(
                repository.itemsFlow.value
                    .first()
                    .isVisible,
            ).isTrue()
        }

    // ========== Utility Function Tests ==========

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

    // ========== Helpers ==========

    private fun createApiPlaylistItem(
        id: Int = 1,
        deviceId: Int = 1,
        rowOrder: Long = id.toLong(),
        visible: Boolean = true,
        renderedAt: String? = null,
        pluginName: String? = "Plugin $id",
        mashupId: Int? = null,
    ): PlaylistItem =
        PlaylistItem(
            id = id,
            deviceId = deviceId,
            pluginSettingId = if (mashupId == null) id else null,
            mashupId = mashupId,
            visible = visible,
            renderedAt = renderedAt,
            rowOrder = rowOrder,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
            mirror = false,
            pluginSetting = pluginName?.let { PluginSetting(id = id, name = it, pluginId = id) },
        )

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
