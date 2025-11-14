package ink.trmnl.android.buddy.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import ink.trmnl.android.buddy.content.repository.AnnouncementRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.dev.AppDevConfig
import ink.trmnl.android.buddy.di.AppWorkerFactory.WorkerInstanceFactory
import ink.trmnl.android.buddy.di.WorkerKey
import ink.trmnl.android.buddy.notification.NotificationHelper
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Background worker to sync announcements from TRMNL RSS feed.
 *
 * Scheduled to run periodically (every 4 hours) to keep announcements up-to-date.
 * Shows notification when new announcements are fetched (if enabled in preferences).
 * Uses exponential backoff for retry on failure.
 */
@Inject
class AnnouncementSyncWorker(
    context: Context,
    @Assisted params: WorkerParameters,
    private val announcementRepository: AnnouncementRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        Timber.d("[$WORK_NAME] Starting announcement sync (run attempt: ${runAttemptCount + 1})")

        return try {
            // Get user preferences to check if notifications are enabled
            val preferences = userPreferencesRepository.userPreferencesFlow.first()

            // Get unread count before refresh
            val unreadCountBefore = announcementRepository.getUnreadCount().first()
            Timber.d("[$WORK_NAME] Unread announcements before sync: $unreadCountBefore")

            val result = announcementRepository.refreshAnnouncements()

            if (result.isSuccess) {
                // Get unread count after refresh
                val unreadCountAfter = announcementRepository.getUnreadCount().first()
                val newAnnouncementsCount = unreadCountAfter - unreadCountBefore
                val executionTime = System.currentTimeMillis() - startTime

                Timber.d(
                    "[$WORK_NAME] Announcement sync completed successfully. New announcements: $newAnnouncementsCount (execution time: %d ms)",
                    executionTime,
                )

                // Show notification if new announcements were fetched AND user has notifications enabled
                // OR if dev flag is enabled for testing
                val shouldShowNotification =
                    newAnnouncementsCount > 0 &&
                        (preferences.isRssFeedContentNotificationEnabled || AppDevConfig.ENABLE_ANNOUNCEMENT_NOTIFICATION)

                if (shouldShowNotification) {
                    if (AppDevConfig.ENABLE_ANNOUNCEMENT_NOTIFICATION) {
                        Timber.d("[$WORK_NAME] Dev flag enabled - showing announcement notification for testing")
                    }
                    NotificationHelper.showAnnouncementNotification(applicationContext, newAnnouncementsCount)
                    Timber.d("[$WORK_NAME] Announcement notification sent for $newAnnouncementsCount new items")
                } else if (newAnnouncementsCount > 0) {
                    Timber.d("[$WORK_NAME] Notifications disabled, skipping notification")
                }

                Result.success()
            } else {
                val error = result.exceptionOrNull()
                val executionTime = System.currentTimeMillis() - startTime
                Timber.e(error, "[$WORK_NAME] Announcement sync failed (execution time: %d ms), scheduling retry", executionTime)
                Result.retry()
            }
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            Timber.e(e, "[$WORK_NAME] Unexpected error during announcement sync (execution time: %d ms), scheduling retry", executionTime)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "announcement_sync"
    }

    @WorkerKey(AnnouncementSyncWorker::class)
    @ContributesIntoMap(
        AppScope::class,
        binding = binding<WorkerInstanceFactory<*>>(),
    )
    @AssistedFactory
    abstract class Factory : WorkerInstanceFactory<AnnouncementSyncWorker>
}
