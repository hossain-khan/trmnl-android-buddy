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
        val startTime = System.currentTimeMillis()
        Timber.d("[$WORK_NAME] Starting low battery notification check (run attempt: ${runAttemptCount + 1})")

        try {
            // Get preferences
            val preferences = userPreferencesRepository.userPreferencesFlow.first()
            val apiToken = preferences.apiToken

            // Check if notifications are enabled
            if (!preferences.isLowBatteryNotificationEnabled) {
                Timber.d("[$WORK_NAME] Low battery notifications are disabled, skipping check")
                return Result.success()
            }

            if (apiToken.isNullOrBlank()) {
                Timber.w("[$WORK_NAME] No API token found, skipping notification check")
                return Result.success()
            }

            val thresholdPercent = preferences.lowBatteryThresholdPercent
            Timber.d("[$WORK_NAME] Checking for devices below %d%% battery threshold", thresholdPercent)

            // Fetch devices from API
            val authHeader = "Bearer $apiToken"
            when (val result = apiService.getDevices(authHeader)) {
                is ApiResult.Success -> {
                    val devices = result.value.data
                    val executionTime = System.currentTimeMillis() - startTime
                    Timber.d("[$WORK_NAME] Fetched %d devices in %d ms", devices.size, executionTime)

                    // Find devices with low battery
                    val lowBatteryDevices =
                        devices.filter { device ->
                            device.percentCharged < thresholdPercent
                        }

                    if (lowBatteryDevices.isNotEmpty()) {
                        Timber.d(
                            "[$WORK_NAME] Found %d devices with low battery: %s",
                            lowBatteryDevices.size,
                            lowBatteryDevices.joinToString { "${it.name} (${it.percentCharged}%)" },
                        )
                        val deviceNames = lowBatteryDevices.map { it.name }

                        // Send notification
                        NotificationHelper.showLowBatteryNotification(
                            context = applicationContext,
                            deviceNames = deviceNames,
                            thresholdPercent = thresholdPercent,
                        )
                        Timber.d("[$WORK_NAME] Low battery notification sent")
                    } else {
                        Timber.d("[$WORK_NAME] No devices with low battery (all above %d%%)", thresholdPercent)
                    }

                    val totalExecutionTime = System.currentTimeMillis() - startTime
                    Timber.d("[$WORK_NAME] Low battery notification check completed successfully in %d ms", totalExecutionTime)
                    return Result.success()
                }

                is ApiResult.Failure.HttpFailure -> {
                    val executionTime = System.currentTimeMillis() - startTime
                    Timber.e("[$WORK_NAME] HTTP error %d fetching devices (execution time: %d ms)", result.code, executionTime)
                    return if (result.code == 401) {
                        Timber.e("[$WORK_NAME] Authentication failed, returning failure")
                        Result.failure()
                    } else {
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
            Timber.e(e, "[$WORK_NAME] Error checking low battery (execution time: %d ms)", executionTime)
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
