package ink.trmnl.android.buddy.work

import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import timber.log.Timber

/**
 * Observer for WorkManager operations.
 * Provides access to WorkInfo for all background workers in the app.
 */
interface WorkManagerObserver {
    /**
     * Get WorkInfo for all workers as a Flow.
     * Updates whenever any worker state changes.
     */
    fun observeAllWorkers(): Flow<List<WorkerStatus>>

    /**
     * Get LiveData for a specific worker by its unique work name.
     */
    fun observeWorker(workName: String): LiveData<List<WorkInfo>>

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
    val constraints: String?,
    val lastRunTime: Long?,
    val nextRunTime: Long?,
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
    private val _allWorkers = MutableStateFlow<List<WorkerStatus>>(emptyList())

    override fun observeAllWorkers(): Flow<List<WorkerStatus>> {
        // Observe all known workers and combine their states
        val batteryCollectionFlow = workManager.getWorkInfosForUniqueWorkLiveData(BatteryCollectionWorker.WORK_NAME)
        val lowBatteryNotificationFlow = workManager.getWorkInfosForUniqueWorkLiveData(LowBatteryNotificationWorker.WORK_NAME)
        val blogPostSyncFlow = workManager.getWorkInfosForUniqueWorkLiveData(BlogPostSyncWorker.WORK_NAME)
        val announcementSyncFlow = workManager.getWorkInfosForUniqueWorkLiveData("announcement_sync")

        // Combine all worker info into a single flow
        return combine(
            _allWorkers,
        ) { _ ->
            getAllWorkerStatuses()
        }
    }

    override fun observeWorker(workName: String): LiveData<List<WorkInfo>> = workManager.getWorkInfosForUniqueWorkLiveData(workName)

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

        // Give WorkManager time to cancel before rescheduling
        // Reschedule all workers using the WorkerScheduler
        // Note: BatteryCollectionWorker is not directly scheduled via WorkerScheduler
        // It's scheduled when battery tracking is enabled in settings
        workerScheduler.scheduleLowBatteryNotification()
        workerScheduler.scheduleBlogPostSync()
        workerScheduler.scheduleAnnouncementSync()
    }

    /**
     * Get the current status of all workers.
     */
    private fun getAllWorkerStatuses(): List<WorkerStatus> {
        val statuses = mutableListOf<WorkerStatus>()

        // Battery Collection Worker
        val batteryCollectionInfo = workManager.getWorkInfosForUniqueWork(BatteryCollectionWorker.WORK_NAME).get()
        batteryCollectionInfo.firstOrNull()?.let {
            statuses.add(
                WorkerStatus(
                    name = BatteryCollectionWorker.WORK_NAME,
                    displayName = "Battery Collection",
                    state = it.state,
                    runAttemptCount = it.runAttemptCount,
                    tags = it.tags,
                    constraints = formatConstraints(it),
                    lastRunTime = null, // WorkInfo doesn't provide this directly
                    nextRunTime = null, // WorkInfo doesn't provide this directly
                ),
            )
        }

        // Low Battery Notification Worker
        val lowBatteryInfo = workManager.getWorkInfosForUniqueWork(LowBatteryNotificationWorker.WORK_NAME).get()
        lowBatteryInfo.firstOrNull()?.let {
            statuses.add(
                WorkerStatus(
                    name = LowBatteryNotificationWorker.WORK_NAME,
                    displayName = "Low Battery Notification",
                    state = it.state,
                    runAttemptCount = it.runAttemptCount,
                    tags = it.tags,
                    constraints = formatConstraints(it),
                    lastRunTime = null,
                    nextRunTime = null,
                ),
            )
        }

        // Blog Post Sync Worker
        val blogPostInfo = workManager.getWorkInfosForUniqueWork(BlogPostSyncWorker.WORK_NAME).get()
        blogPostInfo.firstOrNull()?.let {
            statuses.add(
                WorkerStatus(
                    name = BlogPostSyncWorker.WORK_NAME,
                    displayName = "Blog Post Sync",
                    state = it.state,
                    runAttemptCount = it.runAttemptCount,
                    tags = it.tags,
                    constraints = formatConstraints(it),
                    lastRunTime = null,
                    nextRunTime = null,
                ),
            )
        }

        // Announcement Sync Worker
        val announcementInfo = workManager.getWorkInfosForUniqueWork("announcement_sync").get()
        announcementInfo.firstOrNull()?.let {
            statuses.add(
                WorkerStatus(
                    name = "announcement_sync",
                    displayName = "Announcement Sync",
                    state = it.state,
                    runAttemptCount = it.runAttemptCount,
                    tags = it.tags,
                    constraints = formatConstraints(it),
                    lastRunTime = null,
                    nextRunTime = null,
                ),
            )
        }

        return statuses
    }

    /**
     * Format constraints for display.
     */
    private fun formatConstraints(workInfo: WorkInfo): String? {
        val constraints = mutableListOf<String>()

        // Note: WorkInfo doesn't expose constraints directly in a queryable way
        // We can infer some from tags, but this is limited
        // For now, just show tags that might indicate constraints
        workInfo.tags.forEach { tag ->
            when {
                tag.contains("network", ignoreCase = true) -> constraints.add("Network required")
                tag.contains("charging", ignoreCase = true) -> constraints.add("Charging required")
                tag.contains("idle", ignoreCase = true) -> constraints.add("Device idle required")
            }
        }

        return if (constraints.isNotEmpty()) constraints.joinToString(", ") else null
    }
}
