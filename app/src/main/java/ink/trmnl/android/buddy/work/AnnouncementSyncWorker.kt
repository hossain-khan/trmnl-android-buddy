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
        Timber.d("Starting announcement sync")

        return try {
            // Get user preferences to check if notifications are enabled
            val preferences = userPreferencesRepository.userPreferencesFlow.first()

            // Get unread count before refresh
            val unreadCountBefore = announcementRepository.getUnreadCount().first()

            val result = announcementRepository.refreshAnnouncements()

            if (result.isSuccess) {
                // Get unread count after refresh
                val unreadCountAfter = announcementRepository.getUnreadCount().first()
                val newAnnouncementsCount = unreadCountAfter - unreadCountBefore

                Timber.d("Announcement sync completed successfully. New announcements: $newAnnouncementsCount")

                // Show notification if new announcements were fetched AND user has notifications enabled
                // OR if dev flag is enabled for testing
                val shouldShowNotification =
                    newAnnouncementsCount > 0 &&
                        (preferences.isRssFeedContentNotificationEnabled || AppDevConfig.ENABLE_ANNOUNCEMENT_NOTIFICATION)

                if (shouldShowNotification) {
                    if (AppDevConfig.ENABLE_ANNOUNCEMENT_NOTIFICATION) {
                        Timber.d("Dev flag enabled - showing announcement notification for testing")
                    }
                    NotificationHelper.showAnnouncementNotification(applicationContext, newAnnouncementsCount)
                } else if (newAnnouncementsCount > 0) {
                    Timber.d("Notifications disabled, skipping notification")
                }

                Result.success()
            } else {
                val error = result.exceptionOrNull()
                Timber.e(error, "Announcement sync failed")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during announcement sync")
            Result.retry()
        }
    }

    @WorkerKey(AnnouncementSyncWorker::class)
    @ContributesIntoMap(
        AppScope::class,
        binding = binding<WorkerInstanceFactory<*>>(),
    )
    @AssistedFactory
    abstract class Factory : WorkerInstanceFactory<AnnouncementSyncWorker>
}
