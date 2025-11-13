package ink.trmnl.android.buddy.work

import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Observer for WorkManager operations.
 * Provides access to WorkInfo for all background workers in the app.
 */
interface WorkManagerObserver {
    /**
     * Get WorkInfo for all workers as a Flow.
     * Emits a snapshot of all worker statuses.
     */
    fun observeAllWorkers(): Flow<List<WorkerStatus>>

    /**
     * Cancel all workers.
     */
    fun cancelAllWorkers()

    /**
     * Reset all worker schedules (cancel and reschedule).
     */
    fun resetAllWorkerSchedules()
}

/**
 * Represents the status of a worker at a point in time.
 */
data class WorkerStatus(
    val name: String,
    val displayName: String,
    val state: WorkInfo.State,
    val runAttemptCount: Int,
    val tags: Set<String>,
)

/**
 * Default implementation of WorkManagerObserver.
 */
@ContributesBinding(AppScope::class)
@Inject
class WorkManagerObserverImpl(
    private val workManager: WorkManager,
    private val workerScheduler: WorkerScheduler,
) : WorkManagerObserver {
    override fun observeAllWorkers(): Flow<List<WorkerStatus>> =
        flow {
            // Emit initial empty state
            emit(emptyList())

            // Get current worker statuses as a snapshot
            val statuses = mutableListOf<WorkerStatus>()

            try {
                // Battery Collection Worker
                val batteryCollectionInfo =
                    workManager
                        .getWorkInfosForUniqueWork(BatteryCollectionWorker.WORK_NAME)
                        .get()
                        .firstOrNull()
                batteryCollectionInfo?.let {
                    statuses.add(
                        WorkerStatus(
                            name = BatteryCollectionWorker.WORK_NAME,
                            displayName = "Battery Collection",
                            state = it.state,
                            runAttemptCount = it.runAttemptCount,
                            tags = it.tags,
                        ),
                    )
                }

                // Low Battery Notification Worker
                val lowBatteryInfo =
                    workManager
                        .getWorkInfosForUniqueWork(LowBatteryNotificationWorker.WORK_NAME)
                        .get()
                        .firstOrNull()
                lowBatteryInfo?.let {
                    statuses.add(
                        WorkerStatus(
                            name = LowBatteryNotificationWorker.WORK_NAME,
                            displayName = "Low Battery Notification",
                            state = it.state,
                            runAttemptCount = it.runAttemptCount,
                            tags = it.tags,
                        ),
                    )
                }

                // Blog Post Sync Worker
                val blogPostInfo =
                    workManager
                        .getWorkInfosForUniqueWork(BlogPostSyncWorker.WORK_NAME)
                        .get()
                        .firstOrNull()
                blogPostInfo?.let {
                    statuses.add(
                        WorkerStatus(
                            name = BlogPostSyncWorker.WORK_NAME,
                            displayName = "Blog Post Sync",
                            state = it.state,
                            runAttemptCount = it.runAttemptCount,
                            tags = it.tags,
                        ),
                    )
                }

                // Announcement Sync Worker
                val announcementInfo =
                    workManager
                        .getWorkInfosForUniqueWork("announcement_sync")
                        .get()
                        .firstOrNull()
                announcementInfo?.let {
                    statuses.add(
                        WorkerStatus(
                            name = "announcement_sync",
                            displayName = "Announcement Sync",
                            state = it.state,
                            runAttemptCount = it.runAttemptCount,
                            tags = it.tags,
                        ),
                    )
                }

                emit(statuses)
            } catch (e: Exception) {
                Timber.e(e, "Error fetching worker statuses")
                emit(emptyList())
            }
        }

    override fun cancelAllWorkers() {
        Timber.d("Cancelling all workers")
        workManager.cancelUniqueWork(BatteryCollectionWorker.WORK_NAME)
        workManager.cancelUniqueWork(LowBatteryNotificationWorker.WORK_NAME)
        workManager.cancelUniqueWork(BlogPostSyncWorker.WORK_NAME)
        workManager.cancelUniqueWork("announcement_sync")
    }

    override fun resetAllWorkerSchedules() {
        Timber.d("Resetting all worker schedules")
        cancelAllWorkers()

        // Reschedule all workers using the WorkerScheduler
        // Note: BatteryCollectionWorker is not directly scheduled via WorkerScheduler
        // It's scheduled when battery tracking is enabled in settings
        workerScheduler.scheduleLowBatteryNotification()
        workerScheduler.scheduleBlogPostSync()
        workerScheduler.scheduleAnnouncementSync()
    }
}
