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
        val startTime = System.currentTimeMillis()
        Timber.d("[$WORK_NAME] Starting battery collection work")

        try {
            // Get API token and preferences
            val preferences = userPreferencesRepository.userPreferencesFlow.first()
            val apiToken = preferences.apiToken

            // Check if battery tracking is enabled
            if (!preferences.isBatteryTrackingEnabled) {
                Timber.d("[$WORK_NAME] Battery tracking is disabled, skipping collection")
                return Result.success()
            }

            if (apiToken.isNullOrBlank()) {
                Timber.w("[$WORK_NAME] No API token found, skipping battery collection")
                return Result.success()
            }

            // Fetch devices from API
            Timber.d("[$WORK_NAME] Fetching devices from API")
            val authHeader = "Bearer $apiToken"
            when (val result = apiService.getDevices(authHeader)) {
                is ApiResult.Success -> {
                    val devices = result.value.data
                    val executionTime = System.currentTimeMillis() - startTime
                    Timber.d("[$WORK_NAME] Fetched %d devices in %d ms", devices.size, executionTime)

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
                            "[$WORK_NAME] Recorded battery data for %s: %.1f%% (%.2fV)",
                            device.name,
                            device.percentCharged,
                            device.batteryVoltage ?: 0.0,
                        )
                    }

                    val totalExecutionTime = System.currentTimeMillis() - startTime
                    Timber.d("[$WORK_NAME] Battery collection completed successfully in %d ms", totalExecutionTime)
                    return Result.success()
                }

                is ApiResult.Failure.HttpFailure -> {
                    val executionTime = System.currentTimeMillis() - startTime
                    Timber.e("[$WORK_NAME] HTTP error %d fetching devices (execution time: %d ms)", result.code, executionTime)
                    return if (result.code == 401) {
                        // Token is invalid, don't retry
                        Timber.e("[$WORK_NAME] Authentication failed, returning failure")
                        Result.failure()
                    } else {
                        // Temporary error, retry
                        Timber.w("[$WORK_NAME] Temporary HTTP error, scheduling retry")
                        Result.retry()
                    }
                }

                is ApiResult.Failure.NetworkFailure -> {
                    val executionTime = System.currentTimeMillis() - startTime
                    Timber.e(
                        result.error,
                        "[$WORK_NAME] Network error fetching devices (execution time: %d ms), scheduling retry",
                        executionTime,
                    )
                    return Result.retry()
                }

                is ApiResult.Failure.ApiFailure -> {
                    val executionTime = System.currentTimeMillis() - startTime
                    Timber.e("[$WORK_NAME] API error: %s (execution time: %d ms)", result.error, executionTime)
                    return Result.failure()
                }

                is ApiResult.Failure.UnknownFailure -> {
                    val executionTime = System.currentTimeMillis() - startTime
                    Timber.e(result.error, "[$WORK_NAME] Unknown error fetching devices (execution time: %d ms)", executionTime)
                    return Result.failure()
                }
            }
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            Timber.e(e, "[$WORK_NAME] Error collecting battery data (execution time: %d ms)", executionTime)
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
