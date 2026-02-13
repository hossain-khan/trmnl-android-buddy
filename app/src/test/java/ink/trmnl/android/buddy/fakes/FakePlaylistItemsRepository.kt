package ink.trmnl.android.buddy.fakes

import ink.trmnl.android.buddy.data.PlaylistItemsRepository
import ink.trmnl.android.buddy.domain.models.PlaylistItemUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of [PlaylistItemsRepository] for testing.
 *
 * This fake provides a working in-memory implementation suitable for unit tests,
 * following the project's testing guidelines of using fakes instead of mocks.
 *
 * The fake allows tests to configure responses and verify behavior without making
 * actual API calls or dealing with cache timing issues.
 *
 * @param initialResult The initial result to return for getPlaylistItems() calls. Can be set/changed during tests.
 */
class FakePlaylistItemsRepository(
    private var initialResult: Result<List<PlaylistItemUi>> = Result.success(emptyList()),
) : PlaylistItemsRepository {
    private val _itemsFlow = MutableStateFlow<List<PlaylistItemUi>>(emptyList())
    override val itemsFlow: StateFlow<List<PlaylistItemUi>> = _itemsFlow.asStateFlow()

    /**
     * Test-visible property to track calls to getPlaylistItems.
     */
    var getPlaylistItemsCallCount = 0
        private set

    /**
     * Test-visible property to track calls to getPlaylistItemsForDevice.
     */
    var getPlaylistItemsForDeviceCallCount = 0
        private set

    /**
     * Test-visible property to track the last deviceId parameter.
     */
    var lastDeviceId: Int? = null
        private set

    /**
     * Test-visible property to track the last forceRefresh parameter.
     */
    var lastForceRefresh: Boolean = false
        private set

    /**
     * Test-visible property to verify clearCache() was called.
     */
    var wasCacheCleared = false
        private set

    /**
     * Test-visible property to verify isCacheStale() was called.
     */
    var cacheStaleCheckCount = 0
        private set

    /**
     * Configurable cache stale status for testing.
     */
    var isCacheStaleResult = false

    override suspend fun getPlaylistItems(forceRefresh: Boolean): Result<List<PlaylistItemUi>> {
        getPlaylistItemsCallCount++
        lastForceRefresh = forceRefresh

        return initialResult.also { result ->
            result.onSuccess { items ->
                _itemsFlow.value = items
            }
        }
    }

    override suspend fun getPlaylistItemsForDevice(
        deviceId: Int,
        forceRefresh: Boolean,
    ): Result<List<PlaylistItemUi>> {
        getPlaylistItemsForDeviceCallCount++
        lastDeviceId = deviceId
        lastForceRefresh = forceRefresh

        return initialResult
            .map { items ->
                items.filter { it.deviceId == deviceId }
            }.also { result ->
                result.onSuccess { items ->
                    _itemsFlow.value = items
                }
            }
    }

    override fun clearCache() {
        wasCacheCleared = true
        _itemsFlow.value = emptyList()
    }

    override fun isCacheStale(): Boolean {
        cacheStaleCheckCount++
        return isCacheStaleResult
    }

    override suspend fun updatePlaylistItemVisibility(
        itemId: Int,
        visible: Boolean,
    ): Result<PlaylistItemUi?> = Result.success(null)

    /**
     * Test helper to set the result for subsequent calls.
     */
    fun setResult(result: Result<List<PlaylistItemUi>>) {
        initialResult = result
    }
}
