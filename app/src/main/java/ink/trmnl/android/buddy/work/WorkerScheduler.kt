package ink.trmnl.android.buddy.work

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
}

/**
 * Default implementation of WorkerScheduler using WorkManager.
 */
@ContributesBinding(AppScope::class)
@Inject
class WorkerSchedulerImpl(
    private val workManager: WorkManager,
) : WorkerScheduler {
    override fun scheduleLowBatteryNotification() {
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

    override fun cancelLowBatteryNotification() {
        workManager.cancelUniqueWork(LowBatteryNotificationWorker.WORK_NAME)
    }
}
