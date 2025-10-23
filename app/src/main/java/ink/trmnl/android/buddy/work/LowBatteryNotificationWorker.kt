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
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.di.AppWorkerFactory.WorkerInstanceFactory
import ink.trmnl.android.buddy.di.WorkerKey
import ink.trmnl.android.buddy.notification.NotificationHelper
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker that checks battery levels and sends notifications for low battery.
 *
 * This worker:
 * 1. Fetches all devices from the TRMNL API
 * 2. Compares battery levels against the user-configured threshold
 * 3. Sends aggregated notifications for devices below threshold
 *
 * Scheduled to run weekly when low battery notifications are enabled.
 *
 * @see NotificationHelper
 * @see UserPreferencesRepository
 */
@Inject
class LowBatteryNotificationWorker(
    context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: TrmnlApiService,
    private val userPreferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(context, params) {
    companion object {
        private const val TAG = "LowBatteryNotificationWorker"
        const val WORK_NAME = "low_battery_notification_work"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting low battery notification check")

        try {
            // Get preferences
            val preferences = userPreferencesRepository.userPreferencesFlow.first()
            val apiToken = preferences.apiToken

            // Check if notifications are enabled
            if (!preferences.isLowBatteryNotificationEnabled) {
                Log.d(TAG, "Low battery notifications are disabled, skipping check")
                return Result.success()
            }

            if (apiToken.isNullOrBlank()) {
                Log.w(TAG, "No API token found, skipping notification check")
                return Result.success()
            }

            val thresholdPercent = preferences.lowBatteryThresholdPercent

            // Fetch devices from API
            val authHeader = "Bearer $apiToken"
            when (val result = apiService.getDevices(authHeader)) {
                is ApiResult.Success -> {
                    val devices = result.value.data
                    Log.d(TAG, "Fetched ${devices.size} devices")

                    // Find devices with low battery
                    val lowBatteryDevices =
                        devices.filter { device ->
                            device.percentCharged < thresholdPercent
                        }

                    if (lowBatteryDevices.isNotEmpty()) {
                        Log.d(
                            TAG,
                            "Found ${lowBatteryDevices.size} devices with low battery",
                        )
                        val deviceNames = lowBatteryDevices.map { it.name }

                        // Send notification
                        NotificationHelper.showLowBatteryNotification(
                            context = applicationContext,
                            deviceNames = deviceNames,
                            thresholdPercent = thresholdPercent,
                        )
                    } else {
                        Log.d(TAG, "No devices with low battery")
                    }

                    Log.d(TAG, "Low battery notification check completed successfully")
                    return Result.success()
                }

                is ApiResult.Failure.HttpFailure -> {
                    Log.e(TAG, "HTTP error ${result.code} fetching devices")
                    return if (result.code == 401) {
                        Result.failure()
                    } else {
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
            Log.e(TAG, "Error checking low battery", e)
            return Result.failure()
        }
    }

    @WorkerKey(LowBatteryNotificationWorker::class)
    @ContributesIntoMap(
        AppScope::class,
        binding = binding<WorkerInstanceFactory<*>>(),
    )
    @AssistedFactory
    abstract class Factory : WorkerInstanceFactory<LowBatteryNotificationWorker>
}
