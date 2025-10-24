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
import ink.trmnl.android.buddy.di.AppWorkerFactory.WorkerInstanceFactory
import ink.trmnl.android.buddy.di.WorkerKey
import timber.log.Timber

/**
 * WorkManager worker that syncs announcements from TRMNL RSS feed.
 *
 * This worker:
 * 1. Fetches latest announcements from https://usetrmnl.com/feeds/announcements.xml
 * 2. Parses the RSS/Atom feed
 * 3. Stores new announcements in the local Room database
 * 4. Preserves read/unread status for existing announcements
 *
 * Scheduled to run every 4 hours to keep users informed of new announcements.
 * Uses exponential backoff for retry on failure.
 *
 * @see AnnouncementRepository
 */
@Inject
class AnnouncementSyncWorker(
    context: Context,
    @Assisted params: WorkerParameters,
    private val announcementRepository: AnnouncementRepository,
) : CoroutineWorker(context, params) {
    companion object {
        const val WORK_NAME = "announcement_sync_work"
    }

    override suspend fun doWork(): Result {
        Timber.d("Starting announcement sync work")

        return try {
            // Refresh announcements from RSS feed
            val result = announcementRepository.refreshAnnouncements()

            if (result.isSuccess) {
                Timber.d("Successfully synced announcements")
                Result.success()
            } else {
                val error = result.exceptionOrNull()
                Timber.e(error, "Failed to sync announcements")
                // Retry with exponential backoff
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during announcement sync")
            // Retry with exponential backoff
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
