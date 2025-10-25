package ink.trmnl.android.buddy.work

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import timber.log.Timber
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

    /**
     * Schedules or reschedules the announcement sync worker.
     * The worker fetches announcements from TRMNL RSS feed every 4 hours.
     */
    fun scheduleAnnouncementSync()

    /**
     * Cancels the announcement sync worker.
     */
    fun cancelAnnouncementSync()

    /**
     * Schedules or reschedules the blog post sync worker.
     * The worker fetches blog posts from TRMNL RSS feed daily.
     */
    fun scheduleBlogPostSync()

    /**
     * Cancels the blog post sync worker.
     */
    fun cancelBlogPostSync()
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
    /**
     * Schedules or reschedules the low battery notification worker.
     * Uses REPLACE policy to ensure only one instance runs and to update the work
     * when threshold changes.
     *
     * Worker will run weekly and requires network connectivity to fetch device data.
     */
    override fun scheduleLowBatteryNotification() {
        Timber.d("Scheduling low battery notification worker (weekly, network required)")

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
        Timber.d("Cancelling low battery notification worker")
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
        Timber.d("Triggering immediate low battery notification check (for testing)")

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

    /**
     * Schedules or reschedules the announcement sync worker.
     * The worker fetches announcements from TRMNL RSS feed every 4 hours.
     * Uses KEEP policy to avoid duplicates.
     */
    override fun scheduleAnnouncementSync() {
        Timber.d("Scheduling announcement sync worker (every 4 hours, network required)")

        val announcementWorkRequest =
            PeriodicWorkRequestBuilder<AnnouncementSyncWorker>(
                repeatInterval = 4,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            ).setConstraints(
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            ).build()

        workManager.enqueueUniquePeriodicWork(
            ANNOUNCEMENT_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            announcementWorkRequest,
        )
    }

    /**
     * Cancels the announcement sync worker.
     * Called when user disables announcements in settings.
     */
    override fun cancelAnnouncementSync() {
        Timber.d("Cancelling announcement sync worker")
        workManager.cancelUniqueWork(ANNOUNCEMENT_SYNC_WORK_NAME)
    }

    /**
     * Schedules or reschedules the blog post sync worker.
     * The worker fetches blog posts from TRMNL RSS feed daily.
     * Uses KEEP policy to avoid duplicates.
     */
    override fun scheduleBlogPostSync() {
        Timber.d("Scheduling blog post sync worker (daily, network required)")

        val blogPostWorkRequest =
            PeriodicWorkRequestBuilder<ink.trmnl.android.buddy.worker.BlogPostSyncWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
            ).setConstraints(
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build(),
            ).build()

        workManager.enqueueUniquePeriodicWork(
            BLOG_POST_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            blogPostWorkRequest,
        )
    }

    /**
     * Cancels the blog post sync worker.
     * Called when user disables RSS feed content in settings.
     */
    override fun cancelBlogPostSync() {
        Timber.d("Cancelling blog post sync worker")
        workManager.cancelUniqueWork(BLOG_POST_SYNC_WORK_NAME)
    }

    companion object {
        private const val ANNOUNCEMENT_SYNC_WORK_NAME = "announcement_sync"
        private const val BLOG_POST_SYNC_WORK_NAME = "blog_post_sync"
    }
}
