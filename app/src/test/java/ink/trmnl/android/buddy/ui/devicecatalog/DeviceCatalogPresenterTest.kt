package ink.trmnl.android.buddy.ui.devicecatalog

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.DeviceModel
import ink.trmnl.android.buddy.api.models.DeviceModelsResponse
import ink.trmnl.android.buddy.api.models.DeviceResponse
import ink.trmnl.android.buddy.api.models.DevicesResponse
import ink.trmnl.android.buddy.api.models.Display
import ink.trmnl.android.buddy.api.models.RecipeDetailResponse
import ink.trmnl.android.buddy.api.models.RecipesResponse
import ink.trmnl.android.buddy.api.models.UserResponse
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for DeviceCatalogPresenter.
 *
 * Tests cover:
 * - Successful device fetch
 * - Filter selection (All, TRMNL, Kindle, BYOD)
 * - Error handling
 * - Navigation events
 */
class DeviceCatalogPresenterTest {
    @Test
    fun `presenter fetches and displays device models successfully`() =
        runTest {
            val navigator = FakeNavigator(DeviceCatalogScreen)
            val deviceModels = createTestDeviceModels()
            val apiService =
                FakeApiService(
                    deviceModelsResponse = ApiResult.success(DeviceModelsResponse(data = deviceModels)),
                )
            val repository =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = "test_token"),
                )
            val presenter = DeviceCatalogPresenter(navigator, apiService, repository)

            presenter.test {
                // Wait until we get devices loaded (skip initial and loading states)
                var loadedState: DeviceCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty() && loadedState.error == null)

                assertThat(loadedState.isLoading).isFalse()
                assertThat(loadedState.devices).hasSize(4)
                assertThat(loadedState.error).isNull()
            }
        }

    @Test
    fun `presenter handles filter selection to TRMNL`() =
        runTest {
            val navigator = FakeNavigator(DeviceCatalogScreen)
            val deviceModels = createTestDeviceModels()
            val apiService =
                FakeApiService(
                    deviceModelsResponse = ApiResult.success(DeviceModelsResponse(data = deviceModels)),
                )
            val repository =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = "test_token"),
                )
            val presenter = DeviceCatalogPresenter(navigator, apiService, repository)

            presenter.test {
                // Wait for loaded state
                var loadedState: DeviceCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                // Select TRMNL filter
                loadedState.eventSink(DeviceCatalogScreen.Event.FilterSelected(DeviceKind.TRMNL))

                val filteredState = awaitItem()
                assertThat(filteredState.selectedFilter).isEqualTo(DeviceKind.TRMNL)
            }
        }

    @Test
    fun `presenter handles filter selection to Kindle`() =
        runTest {
            val navigator = FakeNavigator(DeviceCatalogScreen)
            val deviceModels = createTestDeviceModels()
            val apiService =
                FakeApiService(
                    deviceModelsResponse = ApiResult.success(DeviceModelsResponse(data = deviceModels)),
                )
            val repository =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = "test_token"),
                )
            val presenter = DeviceCatalogPresenter(navigator, apiService, repository)

            presenter.test {
                // Wait for loaded state
                var loadedState: DeviceCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                // Select Kindle filter
                loadedState.eventSink(DeviceCatalogScreen.Event.FilterSelected(DeviceKind.KINDLE))

                val filteredState = awaitItem()
                assertThat(filteredState.selectedFilter).isEqualTo(DeviceKind.KINDLE)
            }
        }

    @Test
    fun `presenter handles filter selection to BYOD`() =
        runTest {
            val navigator = FakeNavigator(DeviceCatalogScreen)
            val deviceModels = createTestDeviceModels()
            val apiService =
                FakeApiService(
                    deviceModelsResponse = ApiResult.success(DeviceModelsResponse(data = deviceModels)),
                )
            val repository =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = "test_token"),
                )
            val presenter = DeviceCatalogPresenter(navigator, apiService, repository)

            presenter.test {
                // Wait for loaded state
                var loadedState: DeviceCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                // Select BYOD filter
                loadedState.eventSink(DeviceCatalogScreen.Event.FilterSelected(DeviceKind.BYOD))

                val filteredState = awaitItem()
                assertThat(filteredState.selectedFilter).isEqualTo(DeviceKind.BYOD)
            }
        }

    @Test
    fun `presenter handles filter selection to All (null)`() =
        runTest {
            val navigator = FakeNavigator(DeviceCatalogScreen)
            val deviceModels = createTestDeviceModels()
            val apiService =
                FakeApiService(
                    deviceModelsResponse = ApiResult.success(DeviceModelsResponse(data = deviceModels)),
                )
            val repository =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = "test_token"),
                )
            val presenter = DeviceCatalogPresenter(navigator, apiService, repository)

            presenter.test {
                // Wait for loaded state
                var loadedState: DeviceCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                // First select TRMNL
                loadedState.eventSink(DeviceCatalogScreen.Event.FilterSelected(DeviceKind.TRMNL))
                awaitItem()

                // Then select All
                loadedState.eventSink(DeviceCatalogScreen.Event.FilterSelected(null))

                val filteredState = awaitItem()
                assertThat(filteredState.selectedFilter).isNull()
            }
        }

    @Test
    fun `presenter handles HTTP error 401`() =
        runTest {
            val navigator = FakeNavigator(DeviceCatalogScreen)
            val apiService =
                FakeApiService(
                    deviceModelsResponse =
                        ApiResult.httpFailure(
                            401,
                            ApiError("Unauthorized"),
                        ),
                )
            val repository =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = "test_token"),
                )
            val presenter = DeviceCatalogPresenter(navigator, apiService, repository)

            presenter.test {
                // Wait for error state
                var errorState: DeviceCatalogScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.error == null)

                assertThat(errorState.isLoading).isFalse()
                assertThat(errorState.error).isNotNull()
                assertThat(errorState.error).isEqualTo("Unauthorized. Please log in again.")
            }
        }

    @Test
    fun `presenter handles network failure`() =
        runTest {
            val navigator = FakeNavigator(DeviceCatalogScreen)
            val apiService =
                FakeApiService(
                    deviceModelsResponse =
                        ApiResult.networkFailure(
                            IOException("Network error"),
                        ),
                )
            val repository =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = "test_token"),
                )
            val presenter = DeviceCatalogPresenter(navigator, apiService, repository)

            presenter.test {
                // Wait for error state
                var errorState: DeviceCatalogScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.error == null)

                assertThat(errorState.isLoading).isFalse()
                assertThat(errorState.error).isNotNull()
                assertThat(errorState.error).isEqualTo("Network error. Please check your connection.")
            }
        }

    @Test
    fun `presenter handles retry after error`() =
        runTest {
            val navigator = FakeNavigator(DeviceCatalogScreen)
            var failureCount = 0
            val deviceModels = createTestDeviceModels()
            val apiService =
                object : FakeApiService(
                    deviceModelsResponse = ApiResult.success(DeviceModelsResponse(data = deviceModels)),
                ) {
                    override suspend fun getRecipes(
                        search: String?,
                        sortBy: String?,
                        page: Int?,
                        perPage: Int?,
                    ): ApiResult<RecipesResponse, ApiError> = throw NotImplementedError("Not needed for DeviceCatalogPresenter tests")

                    override suspend fun getRecipe(id: Int): ApiResult<RecipeDetailResponse, ApiError> =
                        throw NotImplementedError("Not needed for DeviceCatalogPresenter tests")

                    override suspend fun getDeviceModels(authorization: String): ApiResult<DeviceModelsResponse, ApiError> =
                        if (failureCount++ == 0) {
                            ApiResult.networkFailure(IOException("Network error"))
                        } else {
                            ApiResult.success(DeviceModelsResponse(data = deviceModels))
                        }
                }
            val repository =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = "test_token"),
                )
            val presenter = DeviceCatalogPresenter(navigator, apiService, repository)

            presenter.test {
                // Wait for error state
                var errorState: DeviceCatalogScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.error == null)

                assertThat(errorState.error).isNotNull()

                // Retry
                errorState.eventSink(DeviceCatalogScreen.Event.RetryClicked)

                // Wait for success state after retry
                var successState: DeviceCatalogScreen.State
                do {
                    successState = awaitItem()
                } while (successState.devices.isEmpty() && successState.error != null)

                assertThat(successState.isLoading).isFalse()
                assertThat(successState.error).isNull()
                assertThat(successState.devices).hasSize(4)
            }
        }

    @Test
    fun `presenter handles missing API key`() =
        runTest {
            val navigator = FakeNavigator(DeviceCatalogScreen)
            val apiService =
                FakeApiService(
                    deviceModelsResponse = ApiResult.success(DeviceModelsResponse(data = emptyList())),
                )
            val repository =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = null),
                )
            val presenter = DeviceCatalogPresenter(navigator, apiService, repository)

            presenter.test {
                // Wait for error state
                var errorState: DeviceCatalogScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.error == null)

                assertThat(errorState.isLoading).isFalse()
                assertThat(errorState.error).isEqualTo("API key not found. Please log in again.")
            }
        }

    /**
     * Helper function to create test device models.
     */
    private fun createTestDeviceModels(): List<DeviceModel> =
        listOf(
            DeviceModel(
                name = "og_png",
                label = "TRMNL OG (1-bit)",
                description = "TRMNL OG (1-bit)",
                width = 800,
                height = 480,
                colors = 2,
                bitDepth = 1,
                scaleFactor = 1.0,
                rotation = 0,
                mimeType = "image/png",
                offsetX = 0,
                offsetY = 0,
                publishedAt = "2024-01-01T00:00:00.000Z",
                kind = "trmnl",
                paletteIds = listOf("bw"),
            ),
            DeviceModel(
                name = "og_plus",
                label = "TRMNL OG (2-bit)",
                description = "TRMNL OG (2-bit)",
                width = 800,
                height = 480,
                colors = 4,
                bitDepth = 2,
                scaleFactor = 1.0,
                rotation = 0,
                mimeType = "image/png",
                offsetX = 0,
                offsetY = 0,
                publishedAt = "2024-01-01T00:00:00.000Z",
                kind = "trmnl",
                paletteIds = listOf("gray-4", "bw"),
            ),
            DeviceModel(
                name = "amazon_kindle_2024",
                label = "Amazon Kindle 2024",
                description = "Amazon Kindle 2024",
                width = 1400,
                height = 840,
                colors = 256,
                bitDepth = 8,
                scaleFactor = 1.75,
                rotation = 90,
                mimeType = "image/png",
                offsetX = 75,
                offsetY = 25,
                publishedAt = "2024-01-01T00:00:00.000Z",
                kind = "kindle",
                paletteIds = listOf("gray-256"),
            ),
            DeviceModel(
                name = "inkplate_10",
                label = "Inkplate 10",
                description = "Inkplate 10",
                width = 1200,
                height = 820,
                colors = 8,
                bitDepth = 3,
                scaleFactor = 1.0,
                rotation = 0,
                mimeType = "image/png",
                offsetX = 0,
                offsetY = 0,
                publishedAt = "2024-01-01T00:00:00.000Z",
                kind = "byod",
                paletteIds = listOf("gray-4", "bw"),
            ),
        )
}

/**
 * Fake implementation of TrmnlApiService for testing.
 */
private open class FakeApiService(
    private val deviceModelsResponse: ApiResult<DeviceModelsResponse, ApiError> =
        ApiResult.success(DeviceModelsResponse(data = emptyList())),
) : TrmnlApiService {
    override suspend fun getDevices(authorization: String) = throw NotImplementedError("Not needed for DeviceCatalogPresenter tests")

    override suspend fun getDevice(
        id: Int,
        authorization: String,
    ) = throw NotImplementedError("Not needed for DeviceCatalogPresenter tests")

    override suspend fun getDisplayCurrent(deviceApiKey: String) = throw NotImplementedError("Not needed for DeviceCatalogPresenter tests")

    override suspend fun userInfo(authorization: String) = throw NotImplementedError("Not needed for DeviceCatalogPresenter tests")

    override suspend fun getRecipes(
        search: String?,
        sortBy: String?,
        page: Int?,
        perPage: Int?,
    ): ApiResult<RecipesResponse, ApiError> = throw NotImplementedError("Not needed for DeviceCatalogPresenter tests")

    override suspend fun getRecipe(id: Int): ApiResult<RecipeDetailResponse, ApiError> =
        throw NotImplementedError("Not needed for DeviceCatalogPresenter tests")

    override suspend fun getDeviceModels(authorization: String): ApiResult<DeviceModelsResponse, ApiError> = deviceModelsResponse
}
