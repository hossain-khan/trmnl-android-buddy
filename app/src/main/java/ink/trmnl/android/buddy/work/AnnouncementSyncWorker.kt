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
 * Background worker to sync announcements from TRMNL RSS feed.
 *
 * Scheduled to run periodically (every 4 hours) to keep announcements up-to-date.
 * Uses exponential backoff for retry on failure.
 */
@Inject
class AnnouncementSyncWorker(
    context: Context,
    @Assisted params: WorkerParameters,
    private val announcementRepository: AnnouncementRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Timber.d("Starting announcement sync")

        return try {
            val result = announcementRepository.refreshAnnouncements()

            if (result.isSuccess) {
                Timber.d("Announcement sync completed successfully")
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
