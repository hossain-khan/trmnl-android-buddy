package ink.trmnl.android.buddy.fakes

import ink.trmnl.android.buddy.api.models.RecipesAnalytics
import ink.trmnl.android.buddy.data.RecipesAnalyticsRepository

/**
 * Fake implementation of [RecipesAnalyticsRepository] for testing.
 *
 * Provides a working in-memory cache implementation suitable for unit tests.
 */
class FakeRecipesAnalyticsRepository : RecipesAnalyticsRepository {
    // Configurable result for testing different scenarios
    var analyticsResult: Result<RecipesAnalytics>? = null

    // Track the last token used
    var lastAuthorizationToken: String? = null

    override suspend fun getRecipesAnalytics(authorization: String): Result<RecipesAnalytics> {
        lastAuthorizationToken = authorization
        return analyticsResult ?: throw NotImplementedError("analyticsResult not set - configure it in the test if needed")
    }

    override fun clearCache() {
        // Fake implementation - does nothing but satisfies the interface
    }
}
