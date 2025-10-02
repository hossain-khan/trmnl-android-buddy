package ink.trmnl.android.buddy.api

import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.api.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for TRMNL device operations.
 *
 * Provides a clean API for fetching and managing TRMNL devices.
 * Handles API calls using EitherNet's type-safe ApiResult for error handling.
 *
 * @property apiService The Retrofit API service instance
 * @property apiKey User's Account API key (format: "user_xxxxxx")
 */
class TrmnlDeviceRepository(
    private val apiService: TrmnlApiService,
    private val apiKey: String
) {
    private val authHeader: String
        get() = "Bearer $apiKey"
    
    /**
     * Fetch all devices belonging to the authenticated user.
     *
     * Makes an API call to `/devices` and returns the list of devices.
     *
     * @return ApiResult containing list of devices or error
     *
     * Example usage:
     * ```kotlin
     * val repository = TrmnlDeviceRepository(apiService, "user_abc123")
     * when (val result = repository.getDevices()) {
     *     is ApiResult.Success -> {
     *         println("Found ${result.value.size} devices")
     *         result.value.forEach { device ->
     *             println("${device.name}: ${device.percentCharged}%")
     *         }
     *     }
     *     is ApiResult.Failure.HttpFailure -> {
     *         println("HTTP Error: ${result.code}")
     *     }
     *     is ApiResult.Failure.NetworkFailure -> {
     *         println("Network Error: ${result.error}")
     *     }
     *     is ApiResult.Failure.ApiFailure -> {
     *         println("API Error: ${result.error}")
     *     }
     *     is ApiResult.Failure.UnknownFailure -> {
     *         println("Unknown Error: ${result.error}")
     *     }
     * }
     * ```
     */
    suspend fun getDevices(): ApiResult<List<Device>, *> = withContext(Dispatchers.IO) {
        when (val result = apiService.getDevices(authHeader)) {
            is ApiResult.Success -> ApiResult.success(result.value.data)
            is ApiResult.Failure -> result
        }
    }
    
    /**
     * Fetch a specific device by ID.
     *
     * Makes an API call to `/devices/{id}` and returns the device details.
     *
     * @param deviceId The device ID to fetch
     * @return ApiResult containing device details or error
     *
     * Example usage:
     * ```kotlin
     * val repository = TrmnlDeviceRepository(apiService, "user_abc123")
     * when (val result = repository.getDevice(12822)) {
     *     is ApiResult.Success -> {
     *         val device = result.value
     *         println("${device.name}: Battery ${device.percentCharged}%")
     *     }
     *     is ApiResult.Failure.HttpFailure -> {
     *         when (result.code) {
     *             404 -> println("Device not found")
     *             401 -> println("Unauthorized")
     *             else -> println("HTTP Error: ${result.code}")
     *         }
     *     }
     *     is ApiResult.Failure.NetworkFailure -> {
     *         println("Network Error: ${result.error}")
     *     }
     *     is ApiResult.Failure.ApiFailure -> {
     *         println("API Error: ${result.error}")
     *     }
     *     is ApiResult.Failure.UnknownFailure -> {
     *         println("Unknown Error: ${result.error}")
     *     }
     * }
     * ```
     */
    suspend fun getDevice(deviceId: Int): ApiResult<Device, *> = withContext(Dispatchers.IO) {
        when (val result = apiService.getDevice(deviceId, authHeader)) {
            is ApiResult.Success -> ApiResult.success(result.value.data)
            is ApiResult.Failure -> result
        }
    }
    
    /**
     * Get devices with low battery (below 20%).
     *
     * Convenience method to filter devices that need charging.
     *
     * @return ApiResult containing list of devices with low battery or error
     */
    suspend fun getDevicesWithLowBattery(): ApiResult<List<Device>, *> {
        return when (val result = getDevices()) {
            is ApiResult.Success -> {
                val lowBatteryDevices = result.value.filter { it.isBatteryLow() }
                ApiResult.success(lowBatteryDevices)
            }
            is ApiResult.Failure -> result
        }
    }
    
    /**
     * Get devices with weak WiFi signal (below 40%).
     *
     * Convenience method to find devices with connectivity issues.
     *
     * @return ApiResult containing list of devices with weak WiFi or error
     */
    suspend fun getDevicesWithWeakWifi(): ApiResult<List<Device>, *> {
        return when (val result = getDevices()) {
            is ApiResult.Success -> {
                val weakWifiDevices = result.value.filter { it.isWifiWeak() }
                ApiResult.success(weakWifiDevices)
            }
            is ApiResult.Failure -> result
        }
    }
    
    /**
     * Get information about the authenticated user.
     *
     * Makes an API call to `/me` and returns the user profile information.
     *
     * @return ApiResult containing user data or error
     *
     * Example usage:
     * ```kotlin
     * val repository = TrmnlDeviceRepository(apiService, "user_abc123")
     * when (val result = repository.userInfo()) {
     *     is ApiResult.Success -> {
     *         val user = result.value
     *         println("Hello, ${user.firstName} ${user.lastName}!")
     *         println("Email: ${user.email}")
     *         println("Timezone: ${user.timeZoneIana}")
     *     }
     *     is ApiResult.Failure.HttpFailure -> {
     *         when (result.code) {
     *             401 -> println("Unauthorized - invalid API key")
     *             else -> println("HTTP Error: ${result.code}")
     *         }
     *     }
     *     is ApiResult.Failure.NetworkFailure -> {
     *         println("Network Error: ${result.error}")
     *     }
     *     is ApiResult.Failure.ApiFailure -> {
     *         println("API Error: ${result.error}")
     *     }
     *     is ApiResult.Failure.UnknownFailure -> {
     *         println("Unknown Error: ${result.error}")
     *     }
     * }
     * ```
     */
    suspend fun userInfo(): ApiResult<User, *> = withContext(Dispatchers.IO) {
        when (val result = apiService.userInfo(authHeader)) {
            is ApiResult.Success -> ApiResult.success(result.value.data)
            is ApiResult.Failure -> result
        }
    }
}
