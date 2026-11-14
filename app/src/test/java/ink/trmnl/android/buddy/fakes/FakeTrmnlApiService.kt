package ink.trmnl.android.buddy.fakes

import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.AuthorPluginErrorsResponse
import ink.trmnl.android.buddy.api.models.CategoriesResponse
import ink.trmnl.android.buddy.api.models.DeviceForecastResponse
import ink.trmnl.android.buddy.api.models.DeviceLogsResponse
import ink.trmnl.android.buddy.api.models.DeviceModelsResponse
import ink.trmnl.android.buddy.api.models.DeviceResponse
import ink.trmnl.android.buddy.api.models.DeviceTimelineResponse
import ink.trmnl.android.buddy.api.models.DevicesResponse
import ink.trmnl.android.buddy.api.models.Display
import ink.trmnl.android.buddy.api.models.PlaylistItemsResponse
import ink.trmnl.android.buddy.api.models.RecipeDetailResponse
import ink.trmnl.android.buddy.api.models.RecipesAnalyticsResponse
import ink.trmnl.android.buddy.api.models.RecipesResponse
import ink.trmnl.android.buddy.api.models.UninstallFeedbackResponse
import ink.trmnl.android.buddy.api.models.UserResponse

/**
 * Fake implementation of [TrmnlApiService] for testing.
 *
 * This fake provides a working in-memory implementation suitable for unit tests,
 * following the project's testing guidelines of using fakes instead of mocks.
 *
 * The fake allows tests to configure responses for API calls and verify that
 * methods were called with expected parameters.
 *
 * @param getDevicesResult The result to return for getDevices() calls. Can be set/changed during tests.
 */
class FakeTrmnlApiService : TrmnlApiService {
    var getDevicesResult: ApiResult<DevicesResponse, ApiError>? = null
    var getDeviceResult: ApiResult<DeviceResponse, ApiError>? = null
    var userInfoResult: ApiResult<UserResponse, ApiError>? = null
    var getRecipesResult: ApiResult<RecipesResponse, ApiError>? = null
    var getRecipeResult: ApiResult<RecipeDetailResponse, ApiError>? = null
    var getDeviceModelsResult: ApiResult<DeviceModelsResponse, ApiError>? = null
    var getDisplayCurrentResult: ApiResult<Display, ApiError>? = null
    var getDisplayResult: ApiResult<Display, ApiError>? = null
    var getPlaylistItemsResult: ApiResult<PlaylistItemsResponse, ApiError>? = null
    var getRecipesAnalyticsResult: ApiResult<RecipesAnalyticsResponse, ApiError>? = null
    var getAuthorPluginErrorsResult: ApiResult<AuthorPluginErrorsResponse, ApiError>? = null
    var getUninstallFeedbackResult: ApiResult<UninstallFeedbackResponse, ApiError>? = null

    var lastAuthorizationHeader: String? = null
    var getDevicesCallCount = 0

    override suspend fun getDevices(authorization: String): ApiResult<DevicesResponse, ApiError> {
        lastAuthorizationHeader = authorization
        getDevicesCallCount++
        return getDevicesResult ?: throw IllegalStateException("getDevicesResult not set")
    }

    override suspend fun getDevice(
        id: Int,
        authorization: String,
    ): ApiResult<DeviceResponse, ApiError> {
        lastAuthorizationHeader = authorization
        return getDeviceResult ?: throw NotImplementedError("getDeviceResult not implemented")
    }

    override suspend fun userInfo(authorization: String): ApiResult<UserResponse, ApiError> {
        lastAuthorizationHeader = authorization
        return userInfoResult ?: throw NotImplementedError("userInfoResult not implemented")
    }

    override suspend fun getRecipes(
        search: String?,
        sortBy: String?,
        page: Int?,
        perPage: Int?,
    ): ApiResult<RecipesResponse, ApiError> = getRecipesResult ?: throw NotImplementedError("getRecipesResult not implemented")

    override suspend fun getRecipe(id: Int): ApiResult<RecipeDetailResponse, ApiError> =
        getRecipeResult ?: throw NotImplementedError("getRecipeResult not implemented")

    override suspend fun getDeviceModels(authorization: String): ApiResult<DeviceModelsResponse, ApiError> {
        lastAuthorizationHeader = authorization
        return getDeviceModelsResult ?: throw NotImplementedError("getDeviceModelsResult not implemented")
    }

    override suspend fun getDisplayCurrent(deviceApiKey: String): ApiResult<Display, ApiError> =
        getDisplayCurrentResult ?: throw NotImplementedError("getDisplayCurrentResult not implemented")

    var updatePlaylistItemVisibilityResult: ApiResult<Unit, ApiError>? = ApiResult.success(Unit)
    var updatePlaylistItemVisibilityException: Exception? = null
    var getPlaylistItemsCallCount = 0
    var updatePlaylistItemVisibilityCallCount = 0
    var lastUpdatePlaylistItemId: Int? = null
    var lastUpdatePlaylistItemBody: Map<String, Boolean>? = null

    override suspend fun getDisplay(deviceApiKey: String): ApiResult<Display, ApiError> =
        getDisplayResult ?: throw NotImplementedError("getDisplayResult not implemented")

    override suspend fun getPlaylistItems(authorization: String): ApiResult<PlaylistItemsResponse, ApiError> {
        lastAuthorizationHeader = authorization
        getPlaylistItemsCallCount++
        return getPlaylistItemsResult ?: throw NotImplementedError("getPlaylistItemsResult not implemented")
    }

    override suspend fun getRecipesAnalytics(authorization: String): ApiResult<RecipesAnalyticsResponse, ApiError> {
        lastAuthorizationHeader = authorization
        return getRecipesAnalyticsResult ?: throw NotImplementedError("getRecipesAnalyticsResult not set")
    }

    override suspend fun getAuthorPluginErrors(authorization: String): ApiResult<AuthorPluginErrorsResponse, ApiError> {
        lastAuthorizationHeader = authorization
        return getAuthorPluginErrorsResult ?: throw NotImplementedError("getAuthorPluginErrorsResult not set")
    }

    override suspend fun getUninstallFeedback(authorization: String): ApiResult<UninstallFeedbackResponse, ApiError> {
        lastAuthorizationHeader = authorization
        return getUninstallFeedbackResult ?: throw NotImplementedError("getUninstallFeedbackResult not set")
    }

    override suspend fun updatePlaylistItemVisibility(
        id: Int,
        authorization: String,
        body: Map<String, Boolean>,
    ): ApiResult<Unit, ApiError> {
        lastAuthorizationHeader = authorization
        lastUpdatePlaylistItemId = id
        lastUpdatePlaylistItemBody = body
        updatePlaylistItemVisibilityCallCount++
        updatePlaylistItemVisibilityException?.let { throw it }
        return updatePlaylistItemVisibilityResult ?: throw NotImplementedError("updatePlaylistItemVisibilityResult not set")
    }

    override suspend fun getCategories(): ApiResult<CategoriesResponse, ApiError> =
        throw NotImplementedError("getCategories not implemented")

    var getDeviceForecastResult: ApiResult<DeviceForecastResponse, ApiError>? = null
    var getDeviceTimelineResult: ApiResult<DeviceTimelineResponse, ApiError>? = null
    var getDeviceLogsResult: ApiResult<DeviceLogsResponse, ApiError>? = null
    var lastForecastHours: Int? = null
    var lastTimelineHours: Int? = null
    var lastLogsLevel: String? = null
    var lastLogsBeforeTs: String? = null
    var lastLogsBeforeEventId: String? = null

    override suspend fun getDeviceForecast(
        authorization: String,
        deviceId: Int,
        hours: Int?,
    ): ApiResult<DeviceForecastResponse, ApiError> {
        lastAuthorizationHeader = authorization
        lastForecastHours = hours
        return getDeviceForecastResult ?: throw NotImplementedError("getDeviceForecastResult not implemented")
    }

    override suspend fun getDeviceTimeline(
        authorization: String,
        deviceId: Int,
        date: String?,
        source: String?,
        hours: Int?,
    ): ApiResult<DeviceTimelineResponse, ApiError> {
        lastAuthorizationHeader = authorization
        lastTimelineHours = hours
        return getDeviceTimelineResult ?: throw NotImplementedError("getDeviceTimelineResult not implemented")
    }

    override suspend fun getDeviceLogs(
        authorization: String,
        deviceId: Int,
        level: String?,
        source: String?,
        limit: Int?,
        beforeTs: String?,
        beforeEventId: String?,
    ): ApiResult<DeviceLogsResponse, ApiError> {
        lastAuthorizationHeader = authorization
        lastLogsLevel = level
        lastLogsBeforeTs = beforeTs
        lastLogsBeforeEventId = beforeEventId
        return getDeviceLogsResult ?: throw NotImplementedError("getDeviceLogsResult not implemented")
    }
}
