package ink.trmnl.android.buddy.work

import android.content.Context
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
import timber.log.Timber

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
        const val WORK_NAME = "battery_collection_work"
    }

    override suspend fun doWork(): Result {
        Timber.d("Starting battery collection work")

        try {
            // Get API token and preferences
            val preferences = userPreferencesRepository.userPreferencesFlow.first()
            val apiToken = preferences.apiToken

            // Check if battery tracking is enabled
            if (!preferences.isBatteryTrackingEnabled) {
                Timber.d("Battery tracking is disabled, skipping collection")
                return Result.success()
            }

            if (apiToken.isNullOrBlank()) {
                Timber.w("No API token found, skipping battery collection")
                return Result.success()
            }

            // Fetch devices from API
            val authHeader = "Bearer $apiToken"
            when (val result = apiService.getDevices(authHeader)) {
                is ApiResult.Success -> {
                    val devices = result.value.data
                    Timber.d("Fetched %d devices", devices.size)

                    // Record battery data for each device
                    val timestamp = System.currentTimeMillis()
                    devices.forEach { device ->
                        batteryHistoryRepository.recordBatteryReading(
                            deviceId = device.friendlyId,
                            percentCharged = device.percentCharged,
                            batteryVoltage = device.batteryVoltage,
                            timestamp = timestamp,
                        )
                        Timber.d(
                            "Recorded battery data for %s: %.1f%%",
                            device.name,
                            device.percentCharged,
                        )
                    }

                    Timber.d("Battery collection completed successfully")
                    return Result.success()
                }

                is ApiResult.Failure.HttpFailure -> {
                    Timber.e("HTTP error %d fetching devices", result.code)
                    return if (result.code == 401) {
                        // Token is invalid, don't retry
                        Result.failure()
                    } else {
                        // Temporary error, retry
                        Result.retry()
                    }
                }

                is ApiResult.Failure.NetworkFailure -> {
                    Timber.e(result.error, "Network error fetching devices")
                    return Result.retry()
                }

                is ApiResult.Failure.ApiFailure -> {
                    Timber.e("API error: %s", result.error)
                    return Result.failure()
                }

                is ApiResult.Failure.UnknownFailure -> {
                    Timber.e(result.error, "Unknown error fetching devices")
                    return Result.failure()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error collecting battery data")
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
