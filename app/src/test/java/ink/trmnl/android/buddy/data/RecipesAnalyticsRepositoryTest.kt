package ink.trmnl.android.buddy.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.ApiResponse
import ink.trmnl.android.buddy.api.models.GrowthDataPoint
import ink.trmnl.android.buddy.api.models.RecipeAnalyticsHealth
import ink.trmnl.android.buddy.api.models.RecipeAnalyticsHealthStatus
import ink.trmnl.android.buddy.api.models.RecipeAnalyticsPlugin
import ink.trmnl.android.buddy.api.models.RecipeAnalyticsStats
import ink.trmnl.android.buddy.api.models.RecipesAnalytics
import ink.trmnl.android.buddy.fakes.FakeTrmnlApiService
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [RecipesAnalyticsRepositoryImpl].
 *
 * Tests cover:
 * - Fetching recipes analytics via API
 * - In-memory caching per authorization token
 * - Cache invalidation when token changes
 * - Cache clearing via [RecipesAnalyticsRepository.clearCache]
 * - API failure handling
 */
class RecipesAnalyticsRepositoryTest {
    @Test
    fun `getRecipesAnalytics fetches from API and caches data for token`() =
        runTest {
            val fakeApiService = FakeTrmnlApiService()
            val analytics = createSampleAnalytics()
            fakeApiService.getRecipesAnalyticsResult = ApiResult.success(ApiResponse(analytics))
            val repository = RecipesAnalyticsRepositoryImpl(fakeApiService)

            val result = repository.getRecipesAnalytics("Bearer token-123")

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow()).isEqualTo(analytics)
            assertThat(fakeApiService.lastAuthorizationHeader).isEqualTo("Bearer token-123")
        }

    @Test
    fun `getRecipesAnalytics returns cached data on second call with same token without calling API`() =
        runTest {
            val fakeApiService = FakeTrmnlApiService()
            val analytics = createSampleAnalytics()
            fakeApiService.getRecipesAnalyticsResult = ApiResult.success(ApiResponse(analytics))
            val repository = RecipesAnalyticsRepositoryImpl(fakeApiService)

            // First call fetches from API
            val result1 = repository.getRecipesAnalytics("Bearer token-123")
            assertThat(result1.isSuccess).isTrue()

            // Reset fake result to ensure API is not called again
            fakeApiService.getRecipesAnalyticsResult = null

            // Second call uses in-memory cache
            val result2 = repository.getRecipesAnalytics("Bearer token-123")
            assertThat(result2.isSuccess).isTrue()
            assertThat(result2.getOrThrow()).isEqualTo(analytics)
        }

    @Test
    fun `getRecipesAnalytics refetches from API when authorization token changes`() =
        runTest {
            val fakeApiService = FakeTrmnlApiService()
            val analytics1 = createSampleAnalytics(totalPlugins = 5)
            val analytics2 = createSampleAnalytics(totalPlugins = 10)
            val repository = RecipesAnalyticsRepositoryImpl(fakeApiService)

            // First call with token 1
            fakeApiService.getRecipesAnalyticsResult = ApiResult.success(ApiResponse(analytics1))
            val result1 = repository.getRecipesAnalytics("Bearer token-1")
            assertThat(result1.getOrThrow().stats.plugins).isEqualTo(5)

            // Second call with token 2 triggers new API fetch
            fakeApiService.getRecipesAnalyticsResult = ApiResult.success(ApiResponse(analytics2))
            val result2 = repository.getRecipesAnalytics("Bearer token-2")
            assertThat(result2.getOrThrow().stats.plugins).isEqualTo(10)
            assertThat(fakeApiService.lastAuthorizationHeader).isEqualTo("Bearer token-2")
        }

    @Test
    fun `clearCache removes cached data causing subsequent call to fetch from API`() =
        runTest {
            val fakeApiService = FakeTrmnlApiService()
            val analytics = createSampleAnalytics()
            fakeApiService.getRecipesAnalyticsResult = ApiResult.success(ApiResponse(analytics))
            val repository = RecipesAnalyticsRepositoryImpl(fakeApiService)

            // Initial fetch
            repository.getRecipesAnalytics("Bearer token-123")

            // Clear cache
            repository.clearCache()

            // Prepare new data
            val freshAnalytics = createSampleAnalytics(totalPlugins = 20)
            fakeApiService.getRecipesAnalyticsResult = ApiResult.success(ApiResponse(freshAnalytics))

            // Fetch again
            val result = repository.getRecipesAnalytics("Bearer token-123")
            assertThat(result.getOrThrow().stats.plugins).isEqualTo(20)
        }

    @Test
    fun `getRecipesAnalytics returns failure when API call fails`() =
        runTest {
            val fakeApiService = FakeTrmnlApiService()
            fakeApiService.getRecipesAnalyticsResult = ApiResult.httpFailure(500, ApiError("Internal Server Error"))
            val repository = RecipesAnalyticsRepositoryImpl(fakeApiService)

            val result = repository.getRecipesAnalytics("Bearer token-123")

            assertThat(result.isFailure).isTrue()
        }

    private fun createSampleAnalytics(totalPlugins: Int = 9): RecipesAnalytics =
        RecipesAnalytics(
            plugins =
                listOf(
                    RecipeAnalyticsPlugin(
                        name = "Sample Plugin",
                        state = "healthy",
                        installs = 10,
                        forks = 2,
                    ),
                ),
            stats =
                RecipeAnalyticsStats(
                    plugins = totalPlugins,
                    connections = 150,
                    pageviews = 500,
                ),
            health =
                RecipeAnalyticsHealth(
                    healthy = RecipeAnalyticsHealthStatus(percent = 90.0),
                    degraded = RecipeAnalyticsHealthStatus(percent = 10.0),
                    erroring = RecipeAnalyticsHealthStatus(percent = 0.0),
                ),
            growth =
                listOf(
                    GrowthDataPoint(date = "2026-04-09", value = 5),
                    GrowthDataPoint(date = "2026-04-10", value = 12),
                ),
        )
}
