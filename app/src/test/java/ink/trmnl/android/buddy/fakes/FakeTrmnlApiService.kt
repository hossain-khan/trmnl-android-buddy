package ink.trmnl.android.buddy.fakes

import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.CategoriesResponse
import ink.trmnl.android.buddy.api.models.DeviceModelsResponse
import ink.trmnl.android.buddy.api.models.DeviceResponse
import ink.trmnl.android.buddy.api.models.DevicesResponse
import ink.trmnl.android.buddy.api.models.Display
import ink.trmnl.android.buddy.api.models.RecipeDetailResponse
import ink.trmnl.android.buddy.api.models.RecipesResponse
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

    override suspend fun getCategories(): ApiResult<CategoriesResponse, ApiError> =
        throw NotImplementedError("getCategories not implemented")
}
