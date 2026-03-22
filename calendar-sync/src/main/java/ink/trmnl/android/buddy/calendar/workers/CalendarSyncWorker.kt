package ink.trmnl.android.buddy.calendar.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ink.trmnl.android.buddy.calendar.repository.CalendarSyncRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * WorkManager worker for periodic calendar sync.
 *
 * This worker:
 * 1. Checks if calendar sync is enabled
 * 2. Gets all events from selected calendars (7 days past + 30 days future)
 * 3. Sends events to TRMNL API calendar plugin
 * 4. Records sync status
 *
 * Can be scheduled to run on a periodic basis (e.g., hourly, daily).
 *
 * PERMISSIONS: READ_CALENDAR only
 */
class CalendarSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val calendarSyncRepository: CalendarSyncRepository,
) : CoroutineWorker(context, params) {
    companion object {
        const val WORK_NAME = "calendar_sync_work"
        private const val TAG = "CalendarSyncWorker"
    }

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        Timber.d("[$TAG] Starting calendar sync (run attempt: ${runAttemptCount + 1})")

        return try {
            // Check if sync is enabled
            if (!calendarSyncRepository.isSyncEnabled()) {
                Timber.d("[$TAG] Calendar sync is disabled, skipping")
                return Result.success()
            }

            // Fetch events from selected calendars
            val events = calendarSyncRepository.getEventsForSync()

            if (events.isEmpty()) {
                Timber.d("[$TAG] No events to sync")
                calendarSyncRepository.recordSyncSuccess()
                return Result.success()
            }

            Timber.d("[$TAG] Fetched ${events.size} events for sync")

            // TODO: Send events to TRMNL API
            // This will be implemented when CalendarSyncApi is added to the api module
            // val syncResult = apiService.syncCalendarEvents(authHeader, calendarSyncRequest)
            // Handle sync result, update preferences, etc.

            val executionTime = System.currentTimeMillis() - startTime
            Timber.d("[$TAG] Calendar sync completed successfully in ${executionTime}ms")

            calendarSyncRepository.recordSyncSuccess()
            Result.success()
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            val errorMsg = e.message ?: "Unknown error"
            Timber.e(e, "[$TAG] Calendar sync failed after ${executionTime}ms: $errorMsg")

            calendarSyncRepository.recordSyncError(errorMsg)

            // Retry on first/second failure, give up on third
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
