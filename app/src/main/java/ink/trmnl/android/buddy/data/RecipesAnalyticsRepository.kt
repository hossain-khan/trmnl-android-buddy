package ink.trmnl.android.buddy.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.RecipesAnalytics
import ink.trmnl.android.buddy.api.util.toResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository interface for TRMNL recipes analytics operations.
 *
 * Analytics data is cached in memory to avoid redundant API calls when the
 * Settings screen is opened multiple times. Cache is invalidated if the API
 * token changes (indicating a different user or account).
 */
interface RecipesAnalyticsRepository {
    /**
     * Get recipes analytics data with in-memory caching.
     *
     * The cache is keyed by the authorization token. If the token changes,
     * the cache is invalidated and fresh data is fetched.
     *
     * @param authorization Bearer token for API authentication
     * @return Result containing RecipesAnalytics data or error
     */
    suspend fun getRecipesAnalytics(authorization: String): Result<RecipesAnalytics>

    /**
     * Clear the in-memory cache. Useful when logging out or switching accounts.
     */
    fun clearCache()
}

/**
 * Implementation of RecipesAnalyticsRepository with in-memory caching.
 *
 * Uses a mutex to ensure thread-safe cache access and modification.
 * The cache stores the last fetched analytics data along with the token
 * it was fetched with. If the token changes, the cache is invalidated.
 */
@Inject
@ContributesBinding(AppScope::class)
class RecipesAnalyticsRepositoryImpl(
    private val apiService: TrmnlApiService,
) : RecipesAnalyticsRepository {
    private val cacheMutex = Mutex()
    private var cachedToken: String? = null
    private var cachedAnalytics: RecipesAnalytics? = null

    override suspend fun getRecipesAnalytics(authorization: String): Result<RecipesAnalytics> =
        cacheMutex.withLock {
            // Check if we have valid cached data for this token
            if (cachedToken == authorization && cachedAnalytics != null) {
                return@withLock Result.success(cachedAnalytics!!)
            }

            // Fetch fresh data from API
            val result =
                apiService
                    .getRecipesAnalytics(authorization)
                    .toResult("Failed to fetch recipes analytics") { it.data }

            // Update cache if fetch was successful
            if (result.isSuccess) {
                val analytics = result.getOrNull()!!
                cachedToken = authorization
                cachedAnalytics = analytics
            }

            result
        }

    override fun clearCache() {
        // Note: clearCache is not suspend, but we don't need lock since this is
        // called rarely (logout/switch accounts) and losing the lock is acceptable
        cachedToken = null
        cachedAnalytics = null
    }
}
