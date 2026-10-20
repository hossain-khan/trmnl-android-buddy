package ink.trmnl.android.buddy.data

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.CategoriesResponse
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.api.models.DeviceModelsResponse
import ink.trmnl.android.buddy.api.models.DeviceResponse
import ink.trmnl.android.buddy.api.models.DevicesResponse
import ink.trmnl.android.buddy.api.models.Display
import ink.trmnl.android.buddy.api.models.PlaylistItemsResponse
import ink.trmnl.android.buddy.api.models.PluginSetting
import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.api.models.RecipeDetailResponse
import ink.trmnl.android.buddy.api.models.RecipeInstallData
import ink.trmnl.android.buddy.api.models.RecipeInstallRequest
import ink.trmnl.android.buddy.api.models.RecipeInstallResponse
import ink.trmnl.android.buddy.api.models.RecipeStats
import ink.trmnl.android.buddy.api.models.RecipesAnalyticsResponse
import ink.trmnl.android.buddy.api.models.RecipesResponse
import ink.trmnl.android.buddy.api.models.UserResponse
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [RecipesRepositoryImpl].
 *
 * Tests repository layer's error handling, data transformation,
 * and authenticated operations using fake API service and user preferences.
 */
class RecipesRepositoryTest {
    private val fakeUserPreferencesRepository =
        FakeUserPreferencesRepository(
            initialPreferences = UserPreferences(apiToken = "test_api_token"),
        )

    @Test
    fun `getRecipes returns success when API succeeds`() =
        runTest {
            // Given
            val fakeApiService = FakeTrmnlApiService()
            val repository = RecipesRepositoryImpl(fakeApiService, fakeUserPreferencesRepository)
            val recipes =
                listOf(
                    Recipe(
                        id = 1,
                        name = "Test Recipe",
                        iconUrl = "https://example.com/icon.png",
                        screenshotUrl = null,
                        stats = RecipeStats(installs = 100, forks = 10),
                    ),
                )
            val recipesResponse =
                RecipesResponse(
                    data = recipes,
                    total = 1,
                    from = 1,
                    to = 1,
                    perPage = 25,
                    currentPage = 1,
                    prevPageUrl = null,
                    nextPageUrl = null,
                )
            fakeApiService.recipesResult = ApiResult.success(recipesResponse)

            // When
            val result = repository.getRecipes()

            // Then
            assertThat(result).isSuccess()
            val response = result.getOrThrow()
            assertThat(response.data).hasSize(1)
            assertThat(response.data[0].name).isEqualTo("Test Recipe")
        }

    @Test
    fun `getRecipes passes parameters to API service`() =
        runTest {
            // Given
            val fakeApiService = FakeTrmnlApiService()
            val repository = RecipesRepositoryImpl(fakeApiService, fakeUserPreferencesRepository)
            val recipesResponse =
                RecipesResponse(
                    data = emptyList(),
                    total = 0,
                    from = 0,
                    to = 0,
                    perPage = 10,
                    currentPage = 2,
                    prevPageUrl = null,
                    nextPageUrl = null,
                )
            fakeApiService.recipesResult = ApiResult.success(recipesResponse)

            // When
            repository.getRecipes(search = "test", sortBy = "popularity", page = 2, perPage = 10)

            // Then
            assertThat(fakeApiService.lastSearch).isEqualTo("test")
            assertThat(fakeApiService.lastSortBy).isEqualTo("popularity")
            assertThat(fakeApiService.lastPage).isEqualTo(2)
            assertThat(fakeApiService.lastPerPage).isEqualTo(10)
        }

    @Test
    fun `getRecipe returns success when API succeeds`() =
        runTest {
            // Given
            val fakeApiService = FakeTrmnlApiService()
            val repository = RecipesRepositoryImpl(fakeApiService, fakeUserPreferencesRepository)
            val recipe =
                Recipe(
                    id = 1,
                    name = "Test Recipe",
                    iconUrl = "https://example.com/icon.png",
                    screenshotUrl = null,
                    stats = RecipeStats(installs = 100, forks = 10),
                )
            fakeApiService.recipeResult = ApiResult.success(RecipeDetailResponse(data = recipe))

            // When
            val result = repository.getRecipe(1)

            // Then
            assertThat(result).isSuccess()
            val fetchedRecipe = result.getOrThrow()
            assertThat(fetchedRecipe.name).isEqualTo("Test Recipe")
        }

    @Test
    fun `getRecipe passes ID to API service`() =
        runTest {
            // Given
            val fakeApiService = FakeTrmnlApiService()
            val repository = RecipesRepositoryImpl(fakeApiService, fakeUserPreferencesRepository)
            val recipe =
                Recipe(
                    id = 42,
                    name = "Test Recipe",
                    iconUrl = null,
                    screenshotUrl = null,
                    stats = RecipeStats(installs = 0, forks = 0),
                )
            fakeApiService.recipeResult = ApiResult.success(RecipeDetailResponse(data = recipe))

            // When
            repository.getRecipe(42)

            // Then
            assertThat(fakeApiService.lastRecipeId).isEqualTo(42)
        }

    @Test
    fun `installRecipe returns success when API succeeds`() =
        runTest {
            // Given
            val fakeApiService = FakeTrmnlApiService()
            val repository = RecipesRepositoryImpl(fakeApiService, fakeUserPreferencesRepository)
            val installResponse =
                RecipeInstallResponse(
                    data =
                        RecipeInstallData(
                            pluginSetting =
                                PluginSetting(
                                    id = 101,
                                    pluginId = 55,
                                    name = "Installed Plugin",
                                ),
                            installMethod = "one_click",
                        ),
                )
            fakeApiService.installRecipeResult = ApiResult.success(installResponse)

            // When
            val result = repository.installRecipe(id = 55, deviceId = 12)

            // Then
            assertThat(result).isSuccess()
            val installData = result.getOrThrow()
            assertThat(installData.installMethod).isEqualTo("one_click")
            assertThat(installData.pluginSetting?.id).isEqualTo(101)
            assertThat(fakeApiService.lastInstallId).isEqualTo(55)
            assertThat(fakeApiService.lastInstallAuth).isEqualTo("Bearer test_api_token")
            assertThat(fakeApiService.lastInstallRequest?.deviceId).isEqualTo(12)
        }

    @Test
    fun `installRecipe fails when API token is missing`() =
        runTest {
            // Given
            val emptyPrefsRepo = FakeUserPreferencesRepository(initialPreferences = UserPreferences(apiToken = null))
            val fakeApiService = FakeTrmnlApiService()
            val repository = RecipesRepositoryImpl(fakeApiService, emptyPrefsRepo)

            // When
            val result = repository.installRecipe(id = 55, deviceId = 12)

            // Then
            assertThat(result).isFailure()
            assertThat(result.exceptionOrNull()?.message).isEqualTo("No API key configured. Please set up your TRMNL API key first.")
        }

    @Test
    fun `getUserDevices returns success when API succeeds`() =
        runTest {
            // Given
            val fakeApiService = FakeTrmnlApiService()
            val repository = RecipesRepositoryImpl(fakeApiService, fakeUserPreferencesRepository)
            val devicesResponse =
                DevicesResponse(
                    data =
                        listOf(
                            Device(
                                id = 1,
                                name = "Living Room TRMNL",
                                friendlyId = "DEV-1",
                                macAddress = "AA:BB:CC:DD:EE:FF",
                                batteryVoltage = 4.0,
                                rssi = -60,
                                percentCharged = 80.0,
                                wifiStrength = 85.0,
                            ),
                        ),
                )
            fakeApiService.devicesResult = ApiResult.success(devicesResponse)

            // When
            val result = repository.getUserDevices()

            // Then
            assertThat(result).isSuccess()
            val devices = result.getOrThrow()
            assertThat(devices).hasSize(1)
            assertThat(devices[0].name).isEqualTo("Living Room TRMNL")
            assertThat(fakeApiService.lastDevicesAuth).isEqualTo("Bearer test_api_token")
        }

    @Test
    fun `getUserDevices fails when API token is missing`() =
        runTest {
            // Given
            val emptyPrefsRepo = FakeUserPreferencesRepository(initialPreferences = UserPreferences(apiToken = null))
            val fakeApiService = FakeTrmnlApiService()
            val repository = RecipesRepositoryImpl(fakeApiService, emptyPrefsRepo)

            // When
            val result = repository.getUserDevices()

            // Then
            assertThat(result).isFailure()
            assertThat(result.exceptionOrNull()?.message).isEqualTo("No API key configured. Please set up your TRMNL API key first.")
        }

    /**
     * Fake implementation of TrmnlApiService for testing.
     */
    private class FakeTrmnlApiService : TrmnlApiService {
        var recipesResult: ApiResult<RecipesResponse, ApiError>? = null
        var recipeResult: ApiResult<RecipeDetailResponse, ApiError>? = null
        var installRecipeResult: ApiResult<RecipeInstallResponse, ApiError>? = null
        var devicesResult: ApiResult<DevicesResponse, ApiError>? = null

        var lastSearch: String? = null
        var lastSortBy: String? = null
        var lastPage: Int? = null
        var lastPerPage: Int? = null
        var lastRecipeId: Int? = null
        var lastInstallId: Int? = null
        var lastInstallAuth: String? = null
        var lastInstallRequest: RecipeInstallRequest? = null
        var lastDevicesAuth: String? = null

        override suspend fun getRecipes(
            search: String?,
            sortBy: String?,
            page: Int?,
            perPage: Int?,
        ): ApiResult<RecipesResponse, ApiError> {
            lastSearch = search
            lastSortBy = sortBy
            lastPage = page
            lastPerPage = perPage
            return recipesResult ?: throw IllegalStateException("recipesResult not set")
        }

        override suspend fun getRecipe(id: Int): ApiResult<RecipeDetailResponse, ApiError> {
            lastRecipeId = id
            return recipeResult ?: throw IllegalStateException("recipeResult not set")
        }

        override suspend fun installRecipe(
            id: Int,
            authorization: String,
            request: RecipeInstallRequest,
        ): ApiResult<RecipeInstallResponse, ApiError> {
            lastInstallId = id
            lastInstallAuth = authorization
            lastInstallRequest = request
            return installRecipeResult ?: throw IllegalStateException("installRecipeResult not set")
        }

        override suspend fun getDevices(authorization: String): ApiResult<DevicesResponse, ApiError> {
            lastDevicesAuth = authorization
            return devicesResult ?: throw IllegalStateException("devicesResult not set")
        }

        // Not used in these tests, so throwing exceptions
        override suspend fun getDevice(
            id: Int,
            authorization: String,
        ): ApiResult<DeviceResponse, ApiError> = throw NotImplementedError()

        override suspend fun getDisplayCurrent(deviceApiKey: String): ApiResult<Display, ApiError> = throw NotImplementedError()

        override suspend fun getDisplay(deviceApiKey: String): ApiResult<Display, ApiError> = throw NotImplementedError()

        override suspend fun userInfo(authorization: String): ApiResult<UserResponse, ApiError> = throw NotImplementedError()

        override suspend fun getCategories(): ApiResult<CategoriesResponse, ApiError> = throw NotImplementedError()

        override suspend fun getDeviceModels(authorization: String): ApiResult<DeviceModelsResponse, ApiError> = throw NotImplementedError()

        override suspend fun getPlaylistItems(authorization: String): ApiResult<PlaylistItemsResponse, ApiError> =
            throw NotImplementedError()

        override suspend fun updatePlaylistItemVisibility(
            id: Int,
            authorization: String,
            body: Map<String, Boolean>,
        ): ApiResult<Unit, ApiError> = throw NotImplementedError()

        override suspend fun getRecipesAnalytics(authorization: String): ApiResult<RecipesAnalyticsResponse, ApiError> =
            throw NotImplementedError()
    }
}
