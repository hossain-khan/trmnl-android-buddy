package ink.trmnl.android.buddy.data

import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.api.models.RecipesResponse

/**
 * Repository interface for TRMNL recipe catalog operations.
 *
 * **Note**: Recipe endpoints are public and do NOT require authentication.
 * The API is in alpha testing and may be moved before end of 2025.
 */
interface RecipesRepository {
    /**
     * Get a list of recipes from the TRMNL community catalog.
     *
     * @param search Optional search term to filter recipes
     * @param sortBy Optional sort order: "oldest", "newest", "popularity", "fork", "install"
     * @param page Page number for pagination (default: 1)
     * @param perPage Items per page (default: 25)
     * @return Result containing RecipesResponse with pagination metadata or error
     */
    suspend fun getRecipes(
        search: String? = null,
        sortBy: String? = null,
        page: Int = 1,
        perPage: Int = 25,
    ): Result<RecipesResponse>

    /**
     * Get detailed information about a specific recipe.
     *
     * @param id Recipe ID to fetch
     * @return Result containing Recipe or error
     */
    suspend fun getRecipe(id: Int): Result<Recipe>
}

/**
 * Implementation of RecipesRepository using TRMNL API service.
 */
@Inject
@ContributesBinding(AppScope::class)
class RecipesRepositoryImpl(
    private val apiService: TrmnlApiService,
) : RecipesRepository {
    override suspend fun getRecipes(
        search: String?,
        sortBy: String?,
        page: Int,
        perPage: Int,
    ): Result<RecipesResponse> =
        when (val result = apiService.getRecipes(search, sortBy, page, perPage)) {
            is ApiResult.Success -> Result.success(result.value)
            is ApiResult.Failure.HttpFailure ->
                Result.failure(
                    Exception("HTTP ${result.code}: Failed to fetch recipes"),
                )
            is ApiResult.Failure.NetworkFailure ->
                Result.failure(
                    Exception("Network error: ${result.error.message}"),
                )
            is ApiResult.Failure.ApiFailure ->
                Result.failure(
                    Exception("API error: ${result.error}"),
                )
            is ApiResult.Failure.UnknownFailure ->
                Result.failure(
                    Exception("Unknown error: ${result.error.message}"),
                )
        }

    override suspend fun getRecipe(id: Int): Result<Recipe> =
        when (val result = apiService.getRecipe(id)) {
            is ApiResult.Success -> Result.success(result.value.data)
            is ApiResult.Failure.HttpFailure ->
                Result.failure(
                    Exception("HTTP ${result.code}: Failed to fetch recipe $id"),
                )
            is ApiResult.Failure.NetworkFailure ->
                Result.failure(
                    Exception("Network error: ${result.error.message}"),
                )
            is ApiResult.Failure.ApiFailure ->
                Result.failure(
                    Exception("API error: ${result.error}"),
                )
            is ApiResult.Failure.UnknownFailure ->
                Result.failure(
                    Exception("Unknown error: ${result.error.message}"),
                )
        }
}
