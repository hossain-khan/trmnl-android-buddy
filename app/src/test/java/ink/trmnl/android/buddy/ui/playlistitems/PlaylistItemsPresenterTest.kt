package ink.trmnl.android.buddy.ui.playlistitems

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.domain.models.PlaylistItemUi
import ink.trmnl.android.buddy.fakes.FakePlaylistItemsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * Unit tests for PlaylistItemsPresenter.
 *
 * Tests cover core functionality:
 * - Initial loading and playlist item fetch
 * - Device filtering (deviceId provided vs null)
 * - Empty state
 * - Error handling (various failure types)
 * - User interactions (refresh, item clicks, navigation)
 * - State transitions during loading and refresh
 */
@RunWith(RobolectricTestRunner::class)
class PlaylistItemsPresenterTest {
    /**
     * Test 1: Initial Loading State
     * Verify that the presenter starts with isLoading = true
     */
    @Test
    fun `initial state shows loading`() =
        runTest {
            // Given
            val repository = FakePlaylistItemsRepository()
            val screen = PlaylistItemsScreen(deviceId = null, deviceName = "All Devices")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                val initialState = awaitItem()
                assertThat(initialState.isLoading).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    /**
     * Test 2: Success Scenarios - Successful API response updates state with items
     * Verify that successful data load populates items and clears loading/error states
     */
    @Test
    fun `successful API call displays items`() =
        runTest {
            // Given
            val items =
                listOf(
                    createTestPlaylistItem(id = 1, deviceId = 1),
                    createTestPlaylistItem(id = 2, deviceId = 2),
                    createTestPlaylistItem(id = 3, deviceId = 1),
                )
            val repository = FakePlaylistItemsRepository(Result.success(items))
            val screen = PlaylistItemsScreen(deviceId = null, deviceName = "All Devices")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                var loadedState: PlaylistItemsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.isLoading)

                assertThat(loadedState.isLoading).isFalse()
                assertThat(loadedState.items).hasSize(3)
                assertThat(loadedState.errorMessage).isNull()
                assertThat(repository.getPlaylistItemsCallCount).isEqualTo(1)
            }
        }

    /**
     * Test 3: Success Scenarios - Filtering by deviceId when provided
     * Verify that when deviceId is specified, only items for that device are shown
     */
    @Test
    fun `filters items by deviceId when provided`() =
        runTest {
            // Given
            val allItems =
                listOf(
                    createTestPlaylistItem(id = 1, deviceId = 1),
                    createTestPlaylistItem(id = 2, deviceId = 2),
                    createTestPlaylistItem(id = 3, deviceId = 1),
                    createTestPlaylistItem(id = 4, deviceId = 3),
                )
            val repository = FakePlaylistItemsRepository(Result.success(allItems))
            val screen = PlaylistItemsScreen(deviceId = 1, deviceName = "Device 1")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                var loadedState: PlaylistItemsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.isLoading)

                assertThat(loadedState.items).hasSize(2)
                assertThat(loadedState.items.all { it.deviceId == 1 }).isTrue()
                assertThat(repository.getPlaylistItemsForDeviceCallCount).isEqualTo(1)
                assertThat(repository.lastDeviceId).isEqualTo(1)
            }
        }

    /**
     * Test 4: Success Scenarios - Showing all items when deviceId is null
     * Verify that when deviceId is null, all items are displayed
     */
    @Test
    fun `shows all items when deviceId is null`() =
        runTest {
            // Given
            val items =
                listOf(
                    createTestPlaylistItem(id = 1, deviceId = 1),
                    createTestPlaylistItem(id = 2, deviceId = 2),
                    createTestPlaylistItem(id = 3, deviceId = 3),
                )
            val repository = FakePlaylistItemsRepository(Result.success(items))
            val screen = PlaylistItemsScreen(deviceId = null, deviceName = "All Devices")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                var loadedState: PlaylistItemsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.isLoading)

                assertThat(loadedState.items).hasSize(3)
                assertThat(repository.getPlaylistItemsCallCount).isEqualTo(1)
            }
        }

    /**
     * Test 5: Success Scenarios - Empty list handling
     * Verify that empty response is handled gracefully
     */
    @Test
    fun `handles empty playlist items list`() =
        runTest {
            // Given
            val repository = FakePlaylistItemsRepository(Result.success(emptyList()))
            val screen = PlaylistItemsScreen(deviceId = null, deviceName = "All Devices")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                var loadedState: PlaylistItemsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.isLoading)

                assertThat(loadedState.items).isEmpty()
                assertThat(loadedState.isLoading).isFalse()
                assertThat(loadedState.errorMessage).isNull()
            }
        }

    /**
     * Test 6: Error Handling - Generic failure displays error message
     * Verify that a generic error displays an appropriate error message
     */
    @Test
    fun `generic failure displays error message`() =
        runTest {
            // Given
            val repository =
                FakePlaylistItemsRepository(
                    Result.failure(Exception("Failed to fetch playlist items")),
                )
            val screen = PlaylistItemsScreen(deviceId = null, deviceName = "All Devices")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                var errorState: PlaylistItemsScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.errorMessage == null)

                assertThat(errorState.isLoading).isFalse()
                assertThat(errorState.items).isEmpty()
                assertThat(errorState.errorMessage).isNotNull()
            }
        }

    /**
     * Test 7: Error Handling - Network failure displays appropriate error
     * Verify that network errors are properly handled and displayed
     */
    @Test
    fun `network failure displays error message`() =
        runTest {
            // Given
            val repository =
                FakePlaylistItemsRepository(
                    Result.failure(IOException("No network connection")),
                )
            val screen = PlaylistItemsScreen(deviceId = null, deviceName = "All Devices")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                var errorState: PlaylistItemsScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.errorMessage == null)

                assertThat(errorState.isLoading).isFalse()
                assertThat(errorState.items).isEmpty()
                assertThat(errorState.errorMessage).isEqualTo("No network connection")
            }
        }

    /**
     * Test 8: Error Handling - API failure with custom message
     * Verify that API failures with custom messages are properly displayed
     */
    @Test
    fun `api failure displays error message`() =
        runTest {
            // Given
            val repository =
                FakePlaylistItemsRepository(
                    Result.failure(Exception("API returned 500: Internal Server Error")),
                )
            val screen = PlaylistItemsScreen(deviceId = null, deviceName = "All Devices")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                var errorState: PlaylistItemsScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.errorMessage == null)

                assertThat(errorState.isLoading).isFalse()
                assertThat(errorState.errorMessage).isEqualTo("API returned 500: Internal Server Error")
            }
        }

    /**
     * Test 9: Error Handling - Unknown failure displays generic error
     * Verify that unknown failures display a fallback error message
     */
    @Test
    fun `unknown failure displays generic error message`() =
        runTest {
            // Given
            val repository =
                FakePlaylistItemsRepository(
                    Result.failure(Exception()),
                )
            val screen = PlaylistItemsScreen(deviceId = null, deviceName = "All Devices")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                var errorState: PlaylistItemsScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.errorMessage == null)

                assertThat(errorState.isLoading).isFalse()
                assertThat(errorState.items).isEmpty()
                assertThat(errorState.errorMessage).isNotNull()
            }
        }

    /**
     * Test 10: User Interactions - Refresh event triggers API call
     * Verify that pull-to-refresh triggers a new data fetch with forceRefresh=true
     */
    @Test
    fun `refresh event triggers data reload`() =
        runTest {
            // Given
            val initialItems = listOf(createTestPlaylistItem(id = 1, deviceId = 1))
            val repository = FakePlaylistItemsRepository(Result.success(initialItems))
            val screen = PlaylistItemsScreen(deviceId = null, deviceName = "All Devices")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                var loadedState: PlaylistItemsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.isLoading)

                // Initial call count should be 1
                assertThat(repository.getPlaylistItemsCallCount).isEqualTo(1)

                // Trigger refresh
                val refreshedItems =
                    listOf(
                        createTestPlaylistItem(id = 1, deviceId = 1),
                        createTestPlaylistItem(id = 2, deviceId = 1),
                    )
                repository.setResult(Result.success(refreshedItems))
                loadedState.eventSink(PlaylistItemsScreen.Event.Refresh)

                // Wait for loading to start and then complete
                var refreshedState: PlaylistItemsScreen.State
                do {
                    refreshedState = awaitItem()
                } while (refreshedState.isLoading)

                // Verify the refresh triggered a new API call
                assertThat(repository.getPlaylistItemsCallCount).isEqualTo(2)
                assertThat(repository.lastForceRefresh).isTrue()
                assertThat(refreshedState.items).hasSize(2)
            }
        }

    /**
     * Test 11: User Interactions - ItemClicked event
     * Verify that clicking an item is logged (no navigation yet - future phase)
     */
    @Test
    fun `item clicked event is handled`() =
        runTest {
            // Given
            val items = listOf(createTestPlaylistItem(id = 1, deviceId = 1))
            val repository = FakePlaylistItemsRepository(Result.success(items))
            val screen = PlaylistItemsScreen(deviceId = null, deviceName = "All Devices")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                var loadedState: PlaylistItemsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.isLoading)

                val item = loadedState.items.first()
                loadedState.eventSink(PlaylistItemsScreen.Event.ItemClicked(item))

                // No navigation happens yet - just verify no crash
                // In future phases, this might navigate to an item detail screen
                expectNoEvents()
            }
        }

    /**
     * Test 12: User Interactions - BackClicked event navigates back
     * Verify that clicking the back button triggers navigation.pop()
     */
    @Test
    fun `back clicked event navigates back`() =
        runTest {
            // Given
            val repository = FakePlaylistItemsRepository(Result.success(emptyList()))
            val screen = PlaylistItemsScreen(deviceId = null, deviceName = "All Devices")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                var loadedState: PlaylistItemsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.isLoading)

                loadedState.eventSink(PlaylistItemsScreen.Event.BackClicked)

                assertThat(navigator.awaitPop()).isNotNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    /**
     * Test 13: State Management - Error state clears on successful refresh
     * Verify that error messages are cleared when a refresh succeeds
     */
    @Test
    fun `error state clears on successful refresh`() =
        runTest {
            // Given - Start with an error
            val repository =
                FakePlaylistItemsRepository(
                    Result.failure(Exception("Initial error")),
                )
            val screen = PlaylistItemsScreen(deviceId = null, deviceName = "All Devices")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                var errorState: PlaylistItemsScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.errorMessage == null)

                assertThat(errorState.errorMessage).isEqualTo("Initial error")

                // Trigger refresh with successful result
                val items = listOf(createTestPlaylistItem(id = 1, deviceId = 1))
                repository.setResult(Result.success(items))
                errorState.eventSink(PlaylistItemsScreen.Event.Refresh)

                // Wait for loading and completion
                var finalState: PlaylistItemsScreen.State
                do {
                    finalState = awaitItem()
                } while (finalState.isLoading)

                // Error should be cleared
                assertThat(finalState.errorMessage).isNull()
                assertThat(finalState.items).hasSize(1)
            }
        }

    /**
     * Test 14: Device filtering with empty result
     * Verify that filtering by deviceId that has no items returns empty list
     */
    @Test
    fun `filters by deviceId returns empty when no items match`() =
        runTest {
            // Given
            val items =
                listOf(
                    createTestPlaylistItem(id = 1, deviceId = 1),
                    createTestPlaylistItem(id = 2, deviceId = 2),
                )
            val repository = FakePlaylistItemsRepository(Result.success(items))
            val screen = PlaylistItemsScreen(deviceId = 99, deviceName = "Device 99")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                var loadedState: PlaylistItemsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.isLoading)

                assertThat(loadedState.items).isEmpty()
                assertThat(loadedState.errorMessage).isNull()
                assertThat(repository.lastDeviceId).isEqualTo(99)
            }
        }

    /**
     * Test 15: Toggle Item Visibility - Failure Case
     * Verify that toggle failure propagates error message to UI state
     */
    @Test
    fun `toggle item visibility failure displays error message`() =
        runTest {
            // Given
            val items = listOf(createTestPlaylistItem(id = 1, deviceId = 1, isVisible = true))
            val repository = FakePlaylistItemsRepository(Result.success(items))
            val updateError = "API Error: 500 Internal Server Error"
            repository.setUpdateVisibilityError(updateError)

            val screen = PlaylistItemsScreen(deviceId = null, deviceName = "All Devices")
            val navigator = FakeNavigator(screen)
            val presenter = createPresenter(screen = screen, navigator = navigator, repository = repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                var state: PlaylistItemsScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Send toggle event
                state.eventSink(PlaylistItemsScreen.Event.ToggleItemVisibility(1, false))

                // Wait for error state
                do {
                    state = awaitItem()
                } while (state.errorMessage == null)

                assertThat(state.errorMessage).isEqualTo("Failed to toggle visibility: $updateError")
            }
        }

    // Helper function to create presenter with dependencies
    private fun createPresenter(
        screen: PlaylistItemsScreen,
        navigator: FakeNavigator,
        repository: FakePlaylistItemsRepository,
    ): PlaylistItemsPresenter =
        PlaylistItemsPresenter(
            screen = screen,
            navigator = navigator,
            repository = repository,
        )

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
