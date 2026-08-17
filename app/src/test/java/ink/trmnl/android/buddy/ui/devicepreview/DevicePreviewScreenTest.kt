package ink.trmnl.android.buddy.ui.devicepreview

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.CategoriesResponse
import ink.trmnl.android.buddy.api.models.Display
import ink.trmnl.android.buddy.fakes.FakeDeviceTokenRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
 * Tests for DevicePreviewScreen presenter.
 *
 * Tests the refresh and display navigation functionality including:
 * - Successful refresh
 * - HTTP error and network error handling
 * - Missing device token handling
 * - Next display API calls and caching
 * - Backward/forward navigation in image history
 * - Navigation pill visibility based on token configuration
 */
class DevicePreviewScreenTest {
    private val testScreen =
        DevicePreviewScreen(
            deviceId = "ABC-123",
            deviceName = "Test Device",
            imageUrl = "https://example.com/image.bmp",
        )

    @Test
    fun `presenter returns initial state with idle refresh state and unconfigured when no token`() =
        runTest {
            val navigator = FakeNavigator(testScreen)
            val apiService = FakeApiService()
            val tokenRepository = FakeDeviceTokenRepository()
            val presenter = DevicePreviewPresenter(testScreen, navigator, apiService, tokenRepository)

            presenter.test {
                val state = awaitItem()
                assertThat(state.deviceId).isEqualTo("ABC-123")
                assertThat(state.deviceName).isEqualTo("Test Device")
                assertThat(state.imageUrl).isEqualTo("https://example.com/image.bmp")
                assertThat(state.isConfigured).isFalse()
                assertThat(state.canGoPrevious).isFalse()
                assertThat(state.canGoNext).isFalse()
                assertThat(state.currentImageIndex).isEqualTo(0)
                assertThat(state.totalImages).isEqualTo(1)
                assertThat(state.refreshState).isInstanceOf(DevicePreviewScreen.RefreshState.Idle::class)
                assertThat(state.loadNextState).isInstanceOf(DevicePreviewScreen.LoadNextState.Idle::class)
            }
        }

    @Test
    fun `presenter enables next navigation when device token is configured`() =
        runTest {
            val navigator = FakeNavigator(testScreen)
            val apiService = FakeApiService()
            val tokenRepository = FakeDeviceTokenRepository(initialTokens = mapOf("ABC-123" to "device-token-123"))
            val presenter = DevicePreviewPresenter(testScreen, navigator, apiService, tokenRepository)

            presenter.test {
                // Initial unconfigured frame before LaunchedEffect runs
                val initialState = awaitItem()
                assertThat(initialState.isConfigured).isFalse()

                // State after LaunchedEffect checks device token
                val configuredState = awaitItem()
                assertThat(configuredState.isConfigured).isTrue()
                assertThat(configuredState.canGoPrevious).isFalse()
                assertThat(configuredState.canGoNext).isTrue()
                assertThat(configuredState.currentImageIndex).isEqualTo(0)
                assertThat(configuredState.totalImages).isEqualTo(1)
            }
        }

    @Test
    fun `refresh image successfully updates the image URL`() =
        runTest {
            val navigator = FakeNavigator(testScreen)
            val apiService =
                FakeApiService(
                    displayResponse =
                        ApiResult.success(
                            Display(
                                status = 200,
                                refreshRate = 300,
                                imageUrl = "https://example.com/new-image.bmp",
                                filename = "new-image.bmp",
                                renderedAt = "2024-10-23T00:00:00Z",
                            ),
                        ),
                )
            val tokenRepository =
                FakeDeviceTokenRepository(
                    initialTokens = mapOf("ABC-123" to "device-token-123"),
                )
            val presenter = DevicePreviewPresenter(testScreen, navigator, apiService, tokenRepository)

            presenter.test {
                awaitItem() // Initial frame
                val configuredState = awaitItem()

                // Trigger refresh
                configuredState.eventSink(DevicePreviewScreen.Event.RefreshImageClicked)

                // Wait for refreshing state
                val refreshingState = awaitItem()
                assertThat(refreshingState.refreshState).isInstanceOf(DevicePreviewScreen.RefreshState.Refreshing::class)

                // Wait for success state
                val successState = awaitItem()
                assertThat(successState.refreshState).isInstanceOf(DevicePreviewScreen.RefreshState.Success::class)
                assertThat(successState.imageUrl).isEqualTo("https://example.com/new-image.bmp")

                val success = successState.refreshState as DevicePreviewScreen.RefreshState.Success
                assertThat(success.message).isEqualTo("Preview image refreshed successfully")
            }
        }

    @Test
    fun `refresh image handles HTTP 429 rate limit error`() =
        runTest {
            val navigator = FakeNavigator(testScreen)
            val apiService =
                FakeApiService(
                    displayResponse = ApiResult.httpFailure(429, null),
                )
            val tokenRepository =
                FakeDeviceTokenRepository(
                    initialTokens = mapOf("ABC-123" to "device-token-123"),
                )
            val presenter = DevicePreviewPresenter(testScreen, navigator, apiService, tokenRepository)

            presenter.test {
                awaitItem() // Initial frame
                val configuredState = awaitItem()

                // Trigger refresh
                configuredState.eventSink(DevicePreviewScreen.Event.RefreshImageClicked)

                // Wait for refreshing state
                val refreshingState = awaitItem()
                assertThat(refreshingState.refreshState).isInstanceOf(DevicePreviewScreen.RefreshState.Refreshing::class)

                // Wait for error state
                val errorState = awaitItem()
                assertThat(errorState.refreshState).isInstanceOf(DevicePreviewScreen.RefreshState.Error::class)

                val error = errorState.refreshState as DevicePreviewScreen.RefreshState.Error
                assertThat(error.message).isEqualTo("Too many requests. Please try again later.")
            }
        }

    @Test
    fun `refresh image handles HTTP 500 server error`() =
        runTest {
            val navigator = FakeNavigator(testScreen)
            val apiService =
                FakeApiService(
                    displayResponse = ApiResult.httpFailure(500, null),
                )
            val tokenRepository =
                FakeDeviceTokenRepository(
                    initialTokens = mapOf("ABC-123" to "device-token-123"),
                )
            val presenter = DevicePreviewPresenter(testScreen, navigator, apiService, tokenRepository)

            presenter.test {
                awaitItem() // Initial frame
                val configuredState = awaitItem()

                // Trigger refresh
                configuredState.eventSink(DevicePreviewScreen.Event.RefreshImageClicked)

                // Wait for refreshing state
                val refreshingState = awaitItem()
                assertThat(refreshingState.refreshState).isInstanceOf(DevicePreviewScreen.RefreshState.Refreshing::class)

                // Wait for error state
                val errorState = awaitItem()
                assertThat(errorState.refreshState).isInstanceOf(DevicePreviewScreen.RefreshState.Error::class)

                val error = errorState.refreshState as DevicePreviewScreen.RefreshState.Error
                assertThat(error.message).isEqualTo("Server error. Please try again later.")
            }
        }

    @Test
    fun `refresh image handles network error`() =
        runTest {
            val navigator = FakeNavigator(testScreen)
            val apiService =
                FakeApiService(
                    displayResponse = ApiResult.networkFailure(IOException("Network error")),
                )
            val tokenRepository =
                FakeDeviceTokenRepository(
                    initialTokens = mapOf("ABC-123" to "device-token-123"),
                )
            val presenter = DevicePreviewPresenter(testScreen, navigator, apiService, tokenRepository)

            presenter.test {
                awaitItem() // Initial frame
                val configuredState = awaitItem()

                // Trigger refresh
                configuredState.eventSink(DevicePreviewScreen.Event.RefreshImageClicked)

                // Wait for refreshing state
                val refreshingState = awaitItem()
                assertThat(refreshingState.refreshState).isInstanceOf(DevicePreviewScreen.RefreshState.Refreshing::class)

                // Wait for error state
                val errorState = awaitItem()
                assertThat(errorState.refreshState).isInstanceOf(DevicePreviewScreen.RefreshState.Error::class)

                val error = errorState.refreshState as DevicePreviewScreen.RefreshState.Error
                assertThat(error.message).isEqualTo("Network error. Please check your connection.")
            }
        }

    @Test
    fun `refresh image handles missing device token`() =
        runTest {
            val navigator = FakeNavigator(testScreen)
            val apiService = FakeApiService()
            val tokenRepository = FakeDeviceTokenRepository() // No tokens
            val presenter = DevicePreviewPresenter(testScreen, navigator, apiService, tokenRepository)

            presenter.test {
                val initialState = awaitItem()

                // Trigger refresh
                initialState.eventSink(DevicePreviewScreen.Event.RefreshImageClicked)

                // Wait for refreshing state
                val refreshingState = awaitItem()
                assertThat(refreshingState.refreshState).isInstanceOf(DevicePreviewScreen.RefreshState.Refreshing::class)

                // Wait for error state
                val errorState = awaitItem()
                assertThat(errorState.refreshState).isInstanceOf(DevicePreviewScreen.RefreshState.Error::class)

                val error = errorState.refreshState as DevicePreviewScreen.RefreshState.Error
                assertThat(error.message).isEqualTo("Device API key not found. Please configure it in settings.")
            }
        }

    @Test
    fun `next image clicked calls getDisplay API when at latest image and updates cache`() =
        runTest {
            val navigator = FakeNavigator(testScreen)
            val apiService =
                FakeApiService(
                    nextDisplayResponse =
                        ApiResult.success(
                            Display(
                                status = 200,
                                refreshRate = 300,
                                imageUrl = "https://example.com/screen2.bmp",
                                filename = "screen2.bmp",
                                renderedAt = "2024-10-23T00:05:00Z",
                            ),
                        ),
                )
            val tokenRepository =
                FakeDeviceTokenRepository(
                    initialTokens = mapOf("ABC-123" to "device-token-123"),
                )
            val presenter = DevicePreviewPresenter(testScreen, navigator, apiService, tokenRepository)

            presenter.test {
                awaitItem() // Initial frame
                val configuredState = awaitItem()
                assertThat(configuredState.totalImages).isEqualTo(1)
                assertThat(configuredState.currentImageIndex).isEqualTo(0)
                assertThat(configuredState.canGoPrevious).isFalse()

                // Trigger NextImageClicked
                configuredState.eventSink(DevicePreviewScreen.Event.NextImageClicked)

                // Wait for loading state
                val loadingState = awaitItem()
                assertThat(loadingState.isLoadingNext).isTrue()

                // Wait for success state
                val successState = awaitItem()
                assertThat(successState.isLoadingNext).isFalse()
                assertThat(successState.imageUrl).isEqualTo("https://example.com/screen2.bmp")
                assertThat(successState.totalImages).isEqualTo(2)
                assertThat(successState.currentImageIndex).isEqualTo(1)
                assertThat(successState.canGoPrevious).isTrue()
                assertThat(successState.loadNextState).isInstanceOf(DevicePreviewScreen.LoadNextState.Idle::class)
            }
        }

    @Test
    fun `navigation moves back and forward through cache without network calls`() =
        runTest {
            val navigator = FakeNavigator(testScreen)
            val apiService =
                FakeApiService(
                    nextDisplayResponse =
                        ApiResult.success(
                            Display(
                                status = 200,
                                refreshRate = 300,
                                imageUrl = "https://example.com/screen2.bmp",
                                filename = "screen2.bmp",
                                renderedAt = "2024-10-23T00:05:00Z",
                            ),
                        ),
                )
            val tokenRepository =
                FakeDeviceTokenRepository(
                    initialTokens = mapOf("ABC-123" to "device-token-123"),
                )
            val presenter = DevicePreviewPresenter(testScreen, navigator, apiService, tokenRepository)

            presenter.test {
                awaitItem() // Initial frame
                val configuredState = awaitItem()

                // 1. Fetch screen 2
                configuredState.eventSink(DevicePreviewScreen.Event.NextImageClicked)
                awaitItem() // Loading
                val screen2State = awaitItem()
                assertThat(screen2State.imageUrl).isEqualTo("https://example.com/screen2.bmp")
                assertThat(screen2State.currentImageIndex).isEqualTo(1)
                assertThat(screen2State.totalImages).isEqualTo(2)

                // 2. Navigate back to screen 1
                screen2State.eventSink(DevicePreviewScreen.Event.PreviousImageClicked)
                val backToScreen1State = awaitItem()
                assertThat(backToScreen1State.imageUrl).isEqualTo("https://example.com/image.bmp")
                assertThat(backToScreen1State.currentImageIndex).isEqualTo(0)
                assertThat(backToScreen1State.totalImages).isEqualTo(2)
                assertThat(backToScreen1State.canGoPrevious).isFalse()

                // 3. Navigate forward to screen 2 from cache
                backToScreen1State.eventSink(DevicePreviewScreen.Event.NextImageClicked)
                val forwardToScreen2State = awaitItem()
                assertThat(forwardToScreen2State.imageUrl).isEqualTo("https://example.com/screen2.bmp")
                assertThat(forwardToScreen2State.currentImageIndex).isEqualTo(1)
                assertThat(forwardToScreen2State.totalImages).isEqualTo(2)
                assertThat(forwardToScreen2State.canGoPrevious).isTrue()
            }
        }

    @Test
    fun `next image handles getDisplay error gracefully`() =
        runTest {
            val navigator = FakeNavigator(testScreen)
            val apiService =
                FakeApiService(
                    nextDisplayResponse = ApiResult.httpFailure(500, null),
                )
            val tokenRepository =
                FakeDeviceTokenRepository(
                    initialTokens = mapOf("ABC-123" to "device-token-123"),
                )
            val presenter = DevicePreviewPresenter(testScreen, navigator, apiService, tokenRepository)

            presenter.test {
                awaitItem() // Initial frame
                val configuredState = awaitItem()

                // Trigger NextImageClicked
                configuredState.eventSink(DevicePreviewScreen.Event.NextImageClicked)

                // Wait for loading state
                val loadingState = awaitItem()
                assertThat(loadingState.isLoadingNext).isTrue()

                // Wait for error state
                val errorState = awaitItem()
                assertThat(errorState.isLoadingNext).isFalse()
                assertThat(errorState.imageUrl).isEqualTo("https://example.com/image.bmp")
                assertThat(errorState.totalImages).isEqualTo(1)
                assertThat(errorState.currentImageIndex).isEqualTo(0)
                assertThat(errorState.loadNextState).isInstanceOf(DevicePreviewScreen.LoadNextState.Error::class)

                val error = errorState.loadNextState as DevicePreviewScreen.LoadNextState.Error
                assertThat(error.message).isEqualTo("Server error. Please try again later.")
            }
        }

    @Test
    fun `back click pops result with newImageUrl when image has changed`() =
        runTest {
            val navigator = FakeNavigator(testScreen)
            val apiService =
                FakeApiService(
                    nextDisplayResponse =
                        ApiResult.success(
                            Display(
                                status = 200,
                                refreshRate = 300,
                                imageUrl = "https://example.com/screen2.bmp",
                                filename = "screen2.bmp",
                                renderedAt = "2024-10-23T00:05:00Z",
                            ),
                        ),
                )
            val tokenRepository =
                FakeDeviceTokenRepository(
                    initialTokens = mapOf("ABC-123" to "device-token-123"),
                )
            val presenter = DevicePreviewPresenter(testScreen, navigator, apiService, tokenRepository)

            presenter.test {
                awaitItem() // Initial frame
                val configuredState = awaitItem()

                // Fetch next image
                configuredState.eventSink(DevicePreviewScreen.Event.NextImageClicked)
                awaitItem() // Loading
                val updatedState = awaitItem()

                // Click back
                updatedState.eventSink(DevicePreviewScreen.Event.BackClicked)

                val popResult = navigator.awaitPop()
                val result = popResult.result as DevicePreviewScreen.Result
                assertThat(result.deviceId).isEqualTo("ABC-123")
                assertThat(result.newImageUrl).isEqualTo("https://example.com/screen2.bmp")
            }
        }

    @Test
    fun `back click pops result with null newImageUrl when image did not change`() =
        runTest {
            val navigator = FakeNavigator(testScreen)
            val apiService = FakeApiService()
            val tokenRepository = FakeDeviceTokenRepository()
            val presenter = DevicePreviewPresenter(testScreen, navigator, apiService, tokenRepository)

            presenter.test {
                val initialState = awaitItem()

                // Click back without changing image
                initialState.eventSink(DevicePreviewScreen.Event.BackClicked)

                val popResult = navigator.awaitPop()
                val result = popResult.result as DevicePreviewScreen.Result
                assertThat(result.deviceId).isEqualTo("ABC-123")
                assertThat(result.newImageUrl).isNull()
            }
        }

    @Test
    fun `normalizeImageUrl strips dynamic query parameters from signed storage URLs`() {
        val s3SignedUrl =
            "https://trmnl-screens.nyc3.digitaloceanspaces.com/vsgnimt6q3z4giloww5cmmb7iw0s?" +
                "response-content-disposition=inline%3B%20filename%3D%22plugin-aabc0b%22&" +
                "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20260817T072931Z&X-Amz-Signature=abc123"
        assertThat(normalizeImageUrl(s3SignedUrl))
            .isEqualTo("https://trmnl-screens.nyc3.digitaloceanspaces.com/vsgnimt6q3z4giloww5cmmb7iw0s")

        val standardUrl = "https://example.com/image.bmp"
        assertThat(normalizeImageUrl(standardUrl)).isEqualTo("https://example.com/image.bmp")
    }

    @Test
    fun `next image detects sleep mode and displays friendly error without duplicating cache`() =
        runTest {
            val navigator = FakeNavigator(testScreen)
            val apiService =
                FakeApiService(
                    nextDisplayResponse =
                        ApiResult.success(
                            Display(
                                status = 0,
                                refreshRate = 34565,
                                imageUrl = "https://trmnl-screens.nyc3.digitaloceanspaces.com/sleep-asset?sig=new",
                                filename = "sleep",
                                specialFunction = "identify",
                            ),
                        ),
                )
            val tokenRepository =
                FakeDeviceTokenRepository(
                    initialTokens = mapOf("ABC-123" to "device-token-123"),
                )
            val presenter = DevicePreviewPresenter(testScreen, navigator, apiService, tokenRepository)

            presenter.test {
                awaitItem() // Initial frame
                val configuredState = awaitItem()

                // Trigger next image
                configuredState.eventSink(DevicePreviewScreen.Event.NextImageClicked)

                // Wait for loading state
                val loadingState = awaitItem()
                assertThat(loadingState.isLoadingNext).isTrue()

                // Wait for error state detecting sleep mode
                val errorState = awaitItem()
                assertThat(errorState.isLoadingNext).isFalse()
                assertThat(errorState.totalImages).isEqualTo(1)
                assertThat(errorState.currentImageIndex).isEqualTo(0)
                assertThat(errorState.loadNextState).isInstanceOf(DevicePreviewScreen.LoadNextState.Error::class)

                val error = errorState.loadNextState as DevicePreviewScreen.LoadNextState.Error
                assertThat(error.message).isEqualTo("Device is in sleep mode. No new screen available.")
            }
        }

    @Test
    fun `next image detects same underlying screen despite dynamic signed query params`() =
        runTest {
            val screenWithSignedUrl =
                DevicePreviewScreen(
                    deviceId = "ABC-123",
                    deviceName = "Test Device",
                    imageUrl = "https://trmnl-screens.nyc3.digitaloceanspaces.com/vsgnimt6q3z4giloww5cmmb7iw0s?sig=request1",
                )
            val navigator = FakeNavigator(screenWithSignedUrl)
            val apiService =
                FakeApiService(
                    nextDisplayResponse =
                        ApiResult.success(
                            Display(
                                status = 0,
                                refreshRate = 16237,
                                imageUrl =
                                    "https://trmnl-screens.nyc3.digitaloceanspaces.com/vsgnimt6q3z4giloww5cmmb7iw0s?sig=request2_different_timestamp",
                                filename = "plugin-aabc0b",
                            ),
                        ),
                )
            val tokenRepository =
                FakeDeviceTokenRepository(
                    initialTokens = mapOf("ABC-123" to "device-token-123"),
                )
            val presenter = DevicePreviewPresenter(screenWithSignedUrl, navigator, apiService, tokenRepository)

            presenter.test {
                awaitItem() // Initial frame
                val configuredState = awaitItem()

                // Trigger next image
                configuredState.eventSink(DevicePreviewScreen.Event.NextImageClicked)

                // Wait for loading state
                val loadingState = awaitItem()
                assertThat(loadingState.isLoadingNext).isTrue()

                // Wait for error state detecting same screen
                val errorState = awaitItem()
                assertThat(errorState.isLoadingNext).isFalse()
                assertThat(errorState.totalImages).isEqualTo(1)
                assertThat(errorState.currentImageIndex).isEqualTo(0)
                assertThat(errorState.loadNextState).isInstanceOf(DevicePreviewScreen.LoadNextState.Error::class)

                val error = errorState.loadNextState as DevicePreviewScreen.LoadNextState.Error
                assertThat(error.message).isEqualTo("No new screen to display. Device may have only one screen in rotation.")
            }
        }
}

/**
 * Fake implementation of TrmnlApiService for testing.
 */
private class FakeApiService(
    private val displayResponse: ApiResult<Display, ApiError> =
        ApiResult.success(
            Display(
                status = 200,
                refreshRate = 300,
                imageUrl = "https://example.com/image.bmp",
                filename = "image.bmp",
                renderedAt = "2024-10-23T00:00:00Z",
            ),
        ),
    private val nextDisplayResponse: ApiResult<Display, ApiError> =
        ApiResult.success(
            Display(
                status = 200,
                refreshRate = 300,
                imageUrl = "https://example.com/next-image.bmp",
                filename = "next-image.bmp",
                renderedAt = "2024-10-23T00:00:00Z",
            ),
        ),
) : TrmnlApiService {
    override suspend fun getDevices(authorization: String) = throw NotImplementedError("Not needed for DevicePreviewScreen tests")

    override suspend fun getDevice(
        id: Int,
        authorization: String,
    ) = throw NotImplementedError("Not needed for DevicePreviewScreen tests")

    override suspend fun getDisplayCurrent(deviceApiKey: String): ApiResult<Display, ApiError> = displayResponse

    override suspend fun getDisplay(deviceApiKey: String): ApiResult<Display, ApiError> = nextDisplayResponse

    override suspend fun userInfo(authorization: String) = throw NotImplementedError("Not needed for DevicePreviewScreen tests")

    override suspend fun getDeviceModels(authorization: String) = throw NotImplementedError("Not needed for DevicePreviewScreen tests")

    override suspend fun getRecipes(
        search: String?,
        sortBy: String?,
        page: Int?,
        perPage: Int?,
    ) = throw NotImplementedError("Not needed for DevicePreviewScreen tests")

    override suspend fun getRecipe(id: Int) = throw NotImplementedError("Not needed for DevicePreviewScreen tests")

    override suspend fun getCategories(): ApiResult<CategoriesResponse, ApiError> =
        throw NotImplementedError("Not needed for DevicePreviewScreen tests")

    override suspend fun getPlaylistItems(authorization: String) = throw NotImplementedError("Not needed for DevicePreviewScreen tests")

    override suspend fun updatePlaylistItemVisibility(
        id: Int,
        authorization: String,
        body: Map<String, Boolean>,
    ) = throw NotImplementedError("Not needed for DevicePreviewScreen tests")

    override suspend fun getRecipesAnalytics(authorization: String) = throw NotImplementedError("Not needed for DevicePreviewScreen tests")
}
