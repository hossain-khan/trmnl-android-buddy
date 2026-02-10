package ink.trmnl.android.buddy.api

import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.CategoriesResponse
import ink.trmnl.android.buddy.api.models.DeviceModelsResponse
import ink.trmnl.android.buddy.api.models.DeviceResponse
import ink.trmnl.android.buddy.api.models.DevicesResponse
import ink.trmnl.android.buddy.api.models.Display
import ink.trmnl.android.buddy.api.models.PlaylistItemsResponse
import ink.trmnl.android.buddy.api.models.RecipeDetailResponse
import ink.trmnl.android.buddy.api.models.RecipesResponse
import ink.trmnl.android.buddy.api.models.UserResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TRMNL API Service interface.
 *
 * Defines all available API endpoints for interacting with the TRMNL server.
 * Full API documentation: https://trmnl.com/api-docs/index.html
 *
 * ## Authentication
 * Most endpoints require authentication via Bearer token (Account API key).
 * Pass the API key via the Authorization header:
 * ```
 * Authorization: Bearer user_xxxxxx
 * ```
 *
 * ## Base URL
 * Production: `https://trmnl.com/api`
 *
 * ## Error Handling
 * Uses EitherNet's ApiResult for type-safe error handling:
 * - ApiResult.Success<T> - Successful response with data
 * - ApiResult.Failure.NetworkFailure - Network connectivity issues
 * - ApiResult.Failure.HttpFailure - HTTP errors (4xx, 5xx)
 * - ApiResult.Failure.ApiFailure - API-specific errors with decoded error body
 * - ApiResult.Failure.UnknownFailure - Unexpected errors
 *
 * Common HTTP status codes:
 * - 200: Success
 * - 401: Unauthorized (invalid or missing API key)
 * - 404: Resource not found
 * - 422: Unprocessable entity (validation error)
 */
interface TrmnlApiService {
    // ========================================
    // Devices API
    // ========================================

    /**
     * Get a list of all devices belonging to the authenticated user.
     *
     * Requires authentication via Bearer token.
     *
     * @param authorization Bearer token with format "Bearer user_xxxxxx"
     * @return ApiResult containing a list of devices or error
     *
     * Example response:
     * ```json
     * {
     *   "data": [
     *     {
     *       "id": 12345,
     *       "name": "Kitchen Display",
     *       "friendly_id": "ABC123",
     *       "mac_address": "00:11:22:33:44:55",
     *       "battery_voltage": 3.8,
     *       "rssi": -27,
     *       "percent_charged": 66.67,
     *       "wifi_strength": 100
     *     }
     *   ]
     * }
     * ```
     *
     * Example usage:
     * ```kotlin
     * when (val result = api.getDevices("Bearer user_abc123")) {
     *     is ApiResult.Success -> println("Found ${result.value.data.size} devices")
     *     is ApiResult.Failure.HttpFailure -> println("HTTP error: ${result.code}")
     *     is ApiResult.Failure.NetworkFailure -> println("Network error")
     *     is ApiResult.Failure.ApiFailure -> println("API error: ${result.error}")
     *     is ApiResult.Failure.UnknownFailure -> println("Unknown error")
     * }
     * ```
     */
    @GET("devices")
    suspend fun getDevices(
        @Header("Authorization") authorization: String,
    ): ApiResult<DevicesResponse, ApiError>

    /**
     * Get detailed information about a specific device.
     *
     * Requires authentication via Bearer token.
     *
     * @param id Device ID to fetch
     * @param authorization Bearer token with format "Bearer user_xxxxxx"
     * @return ApiResult containing device details or error
     *
     * Example usage:
     * ```kotlin
     * when (val result = api.getDevice(12822, "Bearer user_abc123")) {
     *     is ApiResult.Success -> println("Device: ${result.value.data.name}")
     *     is ApiResult.Failure.HttpFailure -> when (result.code) {
     *         404 -> println("Device not found")
     *         401 -> println("Unauthorized")
     *         else -> println("HTTP error: ${result.code}")
     *     }
     *     is ApiResult.Failure.NetworkFailure -> println("Network error")
     *     is ApiResult.Failure.ApiFailure -> println("API error: ${result.error}")
     *     is ApiResult.Failure.UnknownFailure -> println("Unknown error")
     * }
     * ```
     */
    @GET("devices/{id}")
    suspend fun getDevice(
        @Path("id") id: Int,
        @Header("Authorization") authorization: String,
    ): ApiResult<DeviceResponse, ApiError>

    // ========================================
    // Display API (Device API)
    // ========================================

    /**
     * Get the current display content for a specific device.
     *
     * This endpoint uses the Device API authentication (Access-Token header)
     * and returns the currently shown content on the device's e-ink screen.
     *
     * Note: This is a Device API endpoint that requires the device's API key,
     * not the user's Account API key.
     *
     * @param deviceApiKey Device API Key (format: "abc-123")
     * @return ApiResult containing display data or error
     *
     * Example response:
     * ```json
     * {
     *   "status": 200,
     *   "refresh_rate": 300,
     *   "image_url": "https://trmnl.com/images/setup/setup-logo.bmp",
     *   "filename": "setup-logo.bmp",
     *   "rendered_at": "2023-01-01T00:00:00Z"
     * }
     * ```
     *
     * Example usage:
     * ```kotlin
     * when (val result = api.getDisplayCurrent("abc-123")) {
     *     is ApiResult.Success -> {
     *         val display = result.value
     *         println("Display image: ${display.imageUrl}")
     *         println("Refresh rate: ${display.refreshRate}s")
     *     }
     *     is ApiResult.Failure.HttpFailure -> when (result.code) {
     *         404 -> println("Device not found")
     *         401 -> println("Unauthorized")
     *         else -> println("HTTP error: ${result.code}")
     *     }
     *     is ApiResult.Failure.NetworkFailure -> println("Network error")
     *     is ApiResult.Failure.ApiFailure -> println("API error: ${result.error}")
     *     is ApiResult.Failure.UnknownFailure -> println("Unknown error")
     * }
     * ```
     */
    @GET("display/current")
    suspend fun getDisplayCurrent(
        @Header("Access-Token") deviceApiKey: String,
    ): ApiResult<Display, ApiError>

    // ========================================
    // Playlists API
    // ========================================

    /**
     * Get playlist items for a specific device or all devices.
     *
     * Retrieves the list of content items (plugins and mashups) that are configured
     * to display on the device(s). Each item includes visibility status, last rendered
     * timestamp, and display order information.
     *
     * Requires authentication via Bearer token.
     *
     * @param authorization Bearer token with format "Bearer user_xxxxxx"
     * @param deviceId Optional device ID to filter playlist items (null returns all devices)
     * @return ApiResult containing list of playlist items or error
     *
     * Example response:
     * ```json
     * {
     *   "data": [
     *     {
     *       "id": 491784,
     *       "device_id": 41448,
     *       "plugin_setting_id": 241324,
     *       "mashup_id": null,
     *       "visible": true,
     *       "rendered_at": "2026-02-09T01:27:58.423Z",
     *       "row_order": 2146435072,
     *       "created_at": "2026-02-09T15:15:47.444Z",
     *       "updated_at": "2026-02-09T15:15:47.444Z",
     *       "mirror": false,
     *       "plugin_setting": {
     *         "id": 241324,
     *         "name": "Kung Fu Panda Quotes",
     *         "plugin_id": 37
     *       }
     *     }
     *   ]
     * }
     * ```
     *
     * Example usage:
     * ```kotlin
     * // Get playlist items for a specific device
     * when (val result = api.getPlaylistItems("Bearer user_abc123", deviceId = 12345)) {
     *     is ApiResult.Success -> {
     *         val items = result.value.data
     *         val activeCount = items.count { it.visible }
     *         println("Found $activeCount active items")
     *     }
     *     is ApiResult.Failure.HttpFailure -> when (result.code) {
     *         401 -> println("Unauthorized")
     *         404 -> println("Device not found")
     *         else -> println("HTTP error: ${result.code}")
     *     }
     *     is ApiResult.Failure.NetworkFailure -> println("Network error")
     *     is ApiResult.Failure.ApiFailure -> println("API error: ${result.error}")
     *     is ApiResult.Failure.UnknownFailure -> println("Unknown error")
     * }
     * ```
     */
    @GET("playlists/items")
    suspend fun getPlaylistItems(
        @Header("Authorization") authorization: String,
        @Query("device_id") deviceId: Int? = null,
    ): ApiResult<PlaylistItemsResponse, ApiError>

    // ========================================
    // Users API
    // ========================================

    /**
     * Get information about the authenticated user.
     *
     * Retrieves the profile information for the currently authenticated user,
     * including their name, email, timezone, and API key.
     *
     * Requires authentication via Bearer token.
     *
     * @param authorization Bearer token with format "Bearer user_xxxxxx"
     * @return ApiResult containing user data or error
     *
     * Example response:
     * ```json
     * {
     *   "data": {
     *     "name": "Jim Bob",
     *     "email": "jimbob@gmail.net",
     *     "first_name": "Jim",
     *     "last_name": "Bob",
     *     "locale": "en",
     *     "time_zone": "Eastern Time (US & Canada)",
     *     "time_zone_iana": "America/New_York",
     *     "utc_offset": -14400,
     *     "api_key": "user_xxxxxx"
     *   }
     * }
     * ```
     *
     * Example usage:
     * ```kotlin
     * when (val result = api.userInfo("Bearer user_abc123")) {
     *     is ApiResult.Success -> {
     *         val user = result.value.data
     *         println("Hello, ${user.firstName}!")
     *     }
     *     is ApiResult.Failure.HttpFailure -> when (result.code) {
     *         401 -> println("Unauthorized - invalid API key")
     *         else -> println("HTTP error: ${result.code}")
     *     }
     *     is ApiResult.Failure.NetworkFailure -> println("Network error")
     *     is ApiResult.Failure.ApiFailure -> println("API error: ${result.error}")
     *     is ApiResult.Failure.UnknownFailure -> println("Unknown error")
     * }
     * ```
     */
    @GET("me")
    suspend fun userInfo(
        @Header("Authorization") authorization: String,
    ): ApiResult<UserResponse, ApiError>

    // ========================================
    // Recipes API (Public Endpoints)
    // ========================================

    /**
     * Get a list of community plugin recipes from the TRMNL catalog.
     *
     * This is a **public endpoint** that does NOT require authentication.
     *
     * **Note**: This endpoint is in alpha testing and may be moved to `/api/recipes`
     * or `/api/plugins` before end of 2025.
     *
     * @param search Optional search term to filter recipes by keyword
     * @param sortBy Optional sort order: "oldest", "newest", "popularity", "fork", "install"
     * @param page Optional page number for pagination (default: 1)
     * @param perPage Optional items per page (default: 25)
     * @return ApiResult containing paginated recipes list or error
     *
     * Example response:
     * ```json
     * {
     *   "data": [
     *     {
     *       "id": 1,
     *       "name": "Weather Chum",
     *       "icon_url": "https://...",
     *       "screenshot_url": "https://...",
     *       "stats": {
     *         "installs": 1230,
     *         "forks": 1
     *       }
     *     }
     *   ],
     *   "total": 100,
     *   "from": 1,
     *   "to": 25,
     *   "per_page": 25,
     *   "current_page": 1,
     *   "prev_page_url": null,
     *   "next_page_url": "https://trmnl.com/recipes.json?page=2"
     * }
     * ```
     *
     * Example usage:
     * ```kotlin
     * // Get first page with default sort (newest)
     * val result = api.getRecipes()
     *
     * // Search for weather-related recipes
     * val searchResult = api.getRecipes(search = "weather")
     *
     * // Get most popular recipes
     * val popularResult = api.getRecipes(sortBy = "popularity")
     *
     * // Get page 2 with custom page size
     * val page2Result = api.getRecipes(page = 2, perPage = 50)
     * ```
     */
    @GET("https://trmnl.com/recipes.json")
    suspend fun getRecipes(
        @Query("search") search: String? = null,
        @Query("sort-by") sortBy: String? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
    ): ApiResult<RecipesResponse, ApiError>

    /**
     * Get detailed information about a specific recipe.
     *
     * This is a **public endpoint** that does NOT require authentication.
     *
     * **Note**: This endpoint is in alpha testing and may be moved to `/api/recipes/{id}`
     * or `/api/plugins/{id}` before end of 2025.
     *
     * @param id Recipe ID to fetch
     * @return ApiResult containing recipe details or error
     *
     * Example usage:
     * ```kotlin
     * when (val result = api.getRecipe(123)) {
     *     is ApiResult.Success -> {
     *         val recipe = result.value.data
     *         println("Recipe: ${recipe.name}")
     *     }
     *     is ApiResult.Failure.HttpFailure -> when (result.code) {
     *         404 -> println("Recipe not found")
     *         else -> println("HTTP error: ${result.code}")
     *     }
     *     is ApiResult.Failure.NetworkFailure -> println("Network error")
     *     is ApiResult.Failure.ApiFailure -> println("API error: ${result.error}")
     *     is ApiResult.Failure.UnknownFailure -> println("Unknown error")
     * }
     * ```
     */
    @GET("https://trmnl.com/recipes/{id}.json")
    suspend fun getRecipe(
        @Path("id") id: Int,
    ): ApiResult<RecipeDetailResponse, ApiError>

    // ========================================
    // Categories API
    // ========================================

    /**
     * Get a list of all valid plugin categories.
     *
     * Returns all available categories that can be used to improve search exposure
     * and filtering for Public or Recipe style plugins. This endpoint does not
     * require authentication.
     *
     * @return ApiResult containing a list of category identifiers (strings) or error
     *
     * Example response:
     * ```json
     * {
     *   "data": [
     *     "analytics",
     *     "art",
     *     "calendar",
     *     "comics",
     *     "crm",
     *     "custom",
     *     "discovery",
     *     "ecommerce",
     *     "education",
     *     "email",
     *     "entertainment",
     *     "environment",
     *     "finance",
     *     "games",
     *     "humor",
     *     "images",
     *     "kpi",
     *     "life",
     *     "marketing",
     *     "nature",
     *     "news",
     *     "personal",
     *     "productivity",
     *     "programming",
     *     "sales",
     *     "sports",
     *     "travel"
     *   ]
     * }
     * ```
     *
     * Example usage:
     * ```kotlin
     * when (val result = api.getCategories()) {
     *     is ApiResult.Success -> println("Found ${result.value.data.size} categories")
     *     is ApiResult.Failure.HttpFailure -> println("HTTP error: ${result.code}")
     *     is ApiResult.Failure.NetworkFailure -> println("Network error")
     *     is ApiResult.Failure.ApiFailure -> println("API error: ${result.error}")
     *     is ApiResult.Failure.UnknownFailure -> println("Unknown error")
     * }
     * ```
     */
    @GET("categories")
    suspend fun getCategories(): ApiResult<CategoriesResponse, ApiError>

    // ========================================
    // Models API
    // ========================================

    /**
     * Get a list of all supported device models.
     *
     * Returns all supported e-ink display device models including official TRMNL devices,
     * Amazon Kindle e-readers, and third-party BYOD devices.
     *
     * Requires authentication via Bearer token.
     *
     * @param authorization Bearer token with format "Bearer user_xxxxxx"
     * @return ApiResult containing a list of device models or error
     *
     * Example response:
     * ```json
     * {
     *   "data": [
     *     {
     *       "name": "og_png",
     *       "label": "TRMNL OG (1-bit)",
     *       "description": "TRMNL OG (1-bit)",
     *       "width": 800,
     *       "height": 480,
     *       "colors": 2,
     *       "bit_depth": 1,
     *       "scale_factor": 1.0,
     *       "rotation": 0,
     *       "mime_type": "image/png",
     *       "offset_x": 0,
     *       "offset_y": 0,
     *       "published_at": "2024-01-01T00:00:00.000Z",
     *       "kind": "trmnl",
     *       "palette_ids": ["bw"]
     *     }
     *   ]
     * }
     * ```
     *
     * Example usage:
     * ```kotlin
     * when (val result = api.getDeviceModels("Bearer user_abc123")) {
     *     is ApiResult.Success -> println("Found ${result.value.data.size} models")
     *     is ApiResult.Failure.HttpFailure -> println("HTTP error: ${result.code}")
     *     is ApiResult.Failure.NetworkFailure -> println("Network error")
     *     is ApiResult.Failure.ApiFailure -> println("API error: ${result.error}")
     *     is ApiResult.Failure.UnknownFailure -> println("Unknown error")
     * }
     * ```
     */
    @GET("models")
    suspend fun getDeviceModels(
        @Header("Authorization") authorization: String,
    ): ApiResult<DeviceModelsResponse, ApiError>
}
