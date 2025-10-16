package ink.trmnl.android.buddy.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.data.database.BatteryHistoryRepository
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.di.AppWorkerFactory
import ink.trmnl.android.buddy.di.AppWorkerFactory.WorkerInstanceFactory
import ink.trmnl.android.buddy.di.WorkerKey
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker that collects battery data for all user devices weekly.
 *
 * This worker:
 * 1. Fetches all devices from the TRMNL API
 * 2. Records current battery level and voltage for each device
 * 3. Stores the data in the local Room database for historical tracking
 *
 * Scheduled to run weekly to build battery health data over time.
 *
 * @see BatteryHistoryRepository
 */
@Inject
class BatteryCollectionWorker(
    context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: TrmnlApiService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val batteryHistoryRepository: BatteryHistoryRepository,
) : CoroutineWorker(context, params) {
    companion object {
        private const val TAG = "BatteryCollectionWorker"
        const val WORK_NAME = "battery_collection_work"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting battery collection work")

        try {
            // Get API token from preferences
            val preferences = userPreferencesRepository.userPreferencesFlow.first()
            val apiToken = preferences.apiToken

            if (apiToken.isNullOrBlank()) {
                Log.w(TAG, "No API token found, skipping battery collection")
                return Result.success()
            }

            // Fetch devices from API
            val authHeader = "Bearer $apiToken"
            when (val result = apiService.getDevices(authHeader)) {
                is ApiResult.Success -> {
                    val devices = result.value.data
                    Log.d(TAG, "Fetched ${devices.size} devices")

                    // Record battery data for each device
                    val timestamp = System.currentTimeMillis()
                    devices.forEach { device ->
                        batteryHistoryRepository.recordBatteryReading(
                            deviceId = device.friendlyId,
                            percentCharged = device.percentCharged,
                            batteryVoltage = device.batteryVoltage,
                            timestamp = timestamp,
                        )
                        Log.d(
                            TAG,
                            "Recorded battery data for ${device.name}: ${device.percentCharged}%",
                        )
                    }

                    Log.d(TAG, "Battery collection completed successfully")
                    return Result.success()
                }

                is ApiResult.Failure.HttpFailure -> {
                    Log.e(TAG, "HTTP error ${result.code} fetching devices")
                    return if (result.code == 401) {
                        // Token is invalid, don't retry
                        Result.failure()
                    } else {
                        // Temporary error, retry
                        Result.retry()
                    }
                }

                is ApiResult.Failure.NetworkFailure -> {
                    Log.e(TAG, "Network error fetching devices", result.error)
                    return Result.retry()
                }

                is ApiResult.Failure.ApiFailure -> {
                    Log.e(TAG, "API error: ${result.error}")
                    return Result.failure()
                }

                is ApiResult.Failure.UnknownFailure -> {
                    Log.e(TAG, "Unknown error fetching devices", result.error)
                    return Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting battery data", e)
            return Result.failure()
        }
    }

    @WorkerKey(BatteryCollectionWorker::class)
    @ContributesIntoMap(
        AppScope::class,
        binding = binding<WorkerInstanceFactory<*>>(),
    )
    @AssistedFactory
    abstract class Factory : WorkerInstanceFactory<BatteryCollectionWorker>
}
