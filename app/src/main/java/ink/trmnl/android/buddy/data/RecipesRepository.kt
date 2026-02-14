package ink.trmnl.android.buddy.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.api.models.RecipesResponse
import ink.trmnl.android.buddy.api.util.toResult
import ink.trmnl.android.buddy.api.util.toResultDirect

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

    /**
     * Get all available plugin categories.
     *
     * Returns a list of valid category identifiers that can be used for filtering
     * recipes and improving search exposure. This endpoint does not require authentication.
     *
     * Categories include: analytics, art, calendar, comics, crm, custom, discovery,
     * ecommerce, education, email, entertainment, environment, finance, games, humor,
     * images, kpi, life, marketing, nature, news, personal, productivity, programming,
     * sales, sports, and travel.
     *
     * @return Result containing list of category strings or error
     */
    suspend fun getCategories(): Result<List<String>>
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
        apiService
            .getRecipes(search, sortBy, page, perPage)
            .toResultDirect("Failed to fetch recipes")

    override suspend fun getRecipe(id: Int): Result<Recipe> =
        apiService
            .getRecipe(id)
            .toResult("Failed to fetch recipe $id") { it.data }

    override suspend fun getCategories(): Result<List<String>> =
        apiService
            .getCategories()
            .toResult("Failed to fetch categories") { it.data }
}
