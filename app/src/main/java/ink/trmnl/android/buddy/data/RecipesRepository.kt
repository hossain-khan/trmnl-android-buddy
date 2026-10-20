package ink.trmnl.android.buddy.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.api.models.RecipeInstallData
import ink.trmnl.android.buddy.api.models.RecipeInstallRequest
import ink.trmnl.android.buddy.api.models.RecipesResponse
import ink.trmnl.android.buddy.api.util.toResult
import ink.trmnl.android.buddy.api.util.toResultDirect
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first

/**
 * Repository interface for TRMNL recipe catalog operations.
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
     * @return Result containing list of category strings or error
     */
    suspend fun getCategories(): Result<List<String>>

    /**
     * Install a recipe into the user's account and assign it to a specific device's playlist.
     *
     * @param id Recipe ID to install
     * @param deviceId ID of the TRMNL device whose playlist the installed recipe joins
     * @return Result containing [RecipeInstallData] or error
     */
    suspend fun installRecipe(
        id: Int,
        deviceId: Int,
    ): Result<RecipeInstallData>

    /**
     * Get user's registered TRMNL devices for destination selection during install.
     *
     * @return Result containing list of devices or error
     */
    suspend fun getUserDevices(): Result<List<Device>>
}

/**
 * Implementation of RecipesRepository using TRMNL API service.
 */
@Inject
@ContributesBinding(AppScope::class)
class RecipesRepositoryImpl(
    private val apiService: TrmnlApiService,
    private val userPreferencesRepository: UserPreferencesRepository,
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

    override suspend fun installRecipe(
        id: Int,
        deviceId: Int,
    ): Result<RecipeInstallData> {
        val token =
            userPreferencesRepository.userPreferencesFlow.first().apiToken
                ?: return Result.failure(
                    IllegalStateException("No API key configured. Please set up your TRMNL API key first."),
                )

        val request = RecipeInstallRequest(deviceId = deviceId)
        return apiService
            .installRecipe(
                id = id,
                authorization = "Bearer $token",
                body = request,
            ).toResult("Failed to install recipe $id") { it.data }
    }

    override suspend fun getUserDevices(): Result<List<Device>> {
        val token =
            userPreferencesRepository.userPreferencesFlow.first().apiToken
                ?: return Result.failure(
                    IllegalStateException("No API key configured. Please set up your TRMNL API key first."),
                )

        return apiService
            .getDevices("Bearer $token")
            .toResult("Failed to fetch user devices") { it.data }
    }
}
