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
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.di.AppWorkerFactory.WorkerInstanceFactory
import ink.trmnl.android.buddy.di.WorkerKey
import ink.trmnl.android.buddy.notification.NotificationHelper
import kotlinx.coroutines.flow.first
import timber.log.Timber

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
        const val WORK_NAME = "low_battery_notification_work"
    }

    override suspend fun doWork(): Result {
        Timber.d("Starting low battery notification check")

        try {
            // Get preferences
            val preferences = userPreferencesRepository.userPreferencesFlow.first()
            val apiToken = preferences.apiToken

            // Check if notifications are enabled
            if (!preferences.isLowBatteryNotificationEnabled) {
                Timber.d("Low battery notifications are disabled, skipping check")
                return Result.success()
            }

            if (apiToken.isNullOrBlank()) {
                Timber.w("No API token found, skipping notification check")
                return Result.success()
            }

            val thresholdPercent = preferences.lowBatteryThresholdPercent

            // Fetch devices from API
            val authHeader = "Bearer $apiToken"
            when (val result = apiService.getDevices(authHeader)) {
                is ApiResult.Success -> {
                    val devices = result.value.data
                    Timber.d("Fetched %d devices", devices.size)

                    // Find devices with low battery
                    val lowBatteryDevices =
                        devices.filter { device ->
                            device.percentCharged < thresholdPercent
                        }

                    if (lowBatteryDevices.isNotEmpty()) {
                        Timber.d(
                            "Found %d devices with low battery",
                            lowBatteryDevices.size,
                        )
                        val deviceNames = lowBatteryDevices.map { it.name }

                        // Send notification
                        NotificationHelper.showLowBatteryNotification(
                            context = applicationContext,
                            deviceNames = deviceNames,
                            thresholdPercent = thresholdPercent,
                        )
                    } else {
                        Timber.d("No devices with low battery")
                    }

                    Timber.d("Low battery notification check completed successfully")
                    return Result.success()
                }

                is ApiResult.Failure.HttpFailure -> {
                    Timber.e("HTTP error %d fetching devices", result.code)
                    return if (result.code == 401) {
                        Result.failure()
                    } else {
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
            Timber.e(e, "Error checking low battery")
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
