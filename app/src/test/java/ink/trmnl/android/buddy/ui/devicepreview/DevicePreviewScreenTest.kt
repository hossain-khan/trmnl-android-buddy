package ink.trmnl.android.buddy.ui.devicepreview

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.CategoriesResponse
import ink.trmnl.android.buddy.api.models.Display
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.fakes.FakeDeviceTokenRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
 * Tests for DevicePreviewScreen presenter.
 *
 * Tests the refresh functionality including:
 * - Successful refresh
 * - HTTP 429 (rate limit) error handling
 * - HTTP 500 (server error) error handling
 * - Network error handling
 * - Missing device token handling
 */
class DevicePreviewScreenTest {
    private val testScreen =
        DevicePreviewScreen(
            deviceId = "ABC-123",
            deviceName = "Test Device",
            imageUrl = "https://example.com/image.bmp",
        )

    @Test
    fun `presenter returns initial state with idle refresh state`() =
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
                assertThat(state.refreshState).isInstanceOf(DevicePreviewScreen.RefreshState.Idle::class)
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
                val initialState = awaitItem()

                // Trigger refresh
                initialState.eventSink(DevicePreviewScreen.Event.RefreshImageClicked)

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
                assertThat(error.message).isEqualTo("Server error occurred. Please try again later.")
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
    fun `refresh image handles null image URL in response`() =
        runTest {
            val navigator = FakeNavigator(testScreen)
            val apiService =
                FakeApiService(
                    displayResponse =
                        ApiResult.success(
                            Display(
                                status = 200,
                                refreshRate = 300,
                                imageUrl = null, // No image URL
                                filename = null,
                                renderedAt = null,
                            ),
                        ),
                )
            val tokenRepository =
                FakeDeviceTokenRepository(
                    initialTokens = mapOf("ABC-123" to "device-token-123"),
                )
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
                assertThat(error.message).isEqualTo("No preview image available")
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
) : TrmnlApiService {
    override suspend fun getDevices(authorization: String) = throw NotImplementedError("Not needed for DevicePreviewScreen tests")

    override suspend fun getDevice(
        id: Int,
        authorization: String,
    ) = throw NotImplementedError("Not needed for DevicePreviewScreen tests")

    override suspend fun getDisplayCurrent(deviceApiKey: String): ApiResult<Display, ApiError> = displayResponse

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
}
