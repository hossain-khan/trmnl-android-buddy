package ink.trmnl.android.buddy.work

import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.util.concurrent.TimeUnit

/**
 * Interface for scheduling and canceling workers.
 * Abstracts WorkManager operations for testability.
 */
interface WorkerScheduler {
    /**
     * Schedules or reschedules the low battery notification worker.
     * The worker checks device battery levels weekly and sends notifications when below threshold.
     */
    fun scheduleLowBatteryNotification()

    /**
     * Cancels the low battery notification worker.
     */
    fun cancelLowBatteryNotification()

    /**
     * Triggers an immediate one-time execution of the low battery notification check.
     * Useful for testing in debug builds without waiting for the scheduled periodic work.
     */
    fun triggerLowBatteryNotificationNow()
}

/**
 * Default implementation of WorkerScheduler using WorkManager.
 * Manages the scheduling and cancellation of periodic background workers.
 */
@ContributesBinding(AppScope::class)
@Inject
class WorkerSchedulerImpl(
    private val workManager: WorkManager,
) : WorkerScheduler {
    companion object {
        private const val TAG = "WorkerScheduler"
    }

    /**
     * Schedules or reschedules the low battery notification worker.
     * Uses REPLACE policy to ensure only one instance runs and to update the work
     * when threshold changes.
     *
     * Worker will run weekly and requires network connectivity to fetch device data.
     */
    override fun scheduleLowBatteryNotification() {
        Log.d(TAG, "Scheduling low battery notification worker (weekly, network required)")

        val notificationWorkRequest =
            PeriodicWorkRequestBuilder<LowBatteryNotificationWorker>(
                repeatInterval = 7,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
            ).setConstraints(
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            ).build()

        // Use REPLACE policy to update the work when threshold changes
        workManager.enqueueUniquePeriodicWork(
            LowBatteryNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            notificationWorkRequest,
        )
    }

    /**
     * Cancels the low battery notification worker.
     * Called when user disables low battery notifications in settings.
     */
    override fun cancelLowBatteryNotification() {
        Log.d(TAG, "Cancelling low battery notification worker")
        workManager.cancelUniqueWork(LowBatteryNotificationWorker.WORK_NAME)
    }

    /**
     * Triggers an immediate one-time execution of the low battery notification check.
     * Useful for testing in debug builds without waiting for the scheduled periodic work.
     *
     * This enqueues a one-time work request that runs immediately (or as soon as
     * network is available) to check battery levels and send notifications.
     */
    override fun triggerLowBatteryNotificationNow() {
        Log.d(TAG, "Triggering immediate low battery notification check (for testing)")

        val immediateWorkRequest =
            androidx.work
                .OneTimeWorkRequestBuilder<LowBatteryNotificationWorker>()
                .setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                ).build()

        workManager.enqueue(immediateWorkRequest)
    }
}
