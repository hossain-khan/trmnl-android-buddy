package ink.trmnl.android.buddy.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.CalendarEvent
import ink.trmnl.android.buddy.api.models.CalendarSyncRequest
import ink.trmnl.android.buddy.calendar.models.SyncEvent
import ink.trmnl.android.buddy.calendar.repository.CalendarSyncRepositoryInterface
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.di.AppWorkerFactory.WorkerInstanceFactory
import ink.trmnl.android.buddy.di.WorkerKey
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * WorkManager worker for periodic calendar sync.
 *
 * This worker:
 * 1. Checks if calendar sync is enabled
 * 2. Gets the API token from user preferences
 * 3. Gets all events from selected calendars (7 days past + 30 days future)
 * 4. Sends events to TRMNL API calendar plugin
 * 5. Records sync status
 *
 * Scheduled to run periodically to keep calendar data up-to-date on TRMNL devices.
 * Retries on transient failures (network errors, temporary HTTP errors).
 *
 * PERMISSIONS: READ_CALENDAR only
 *
 * @see CalendarSyncRepositoryInterface
 */
@Inject
class CalendarSyncWorker(
    context: Context,
    @Assisted params: WorkerParameters,
    private val calendarSyncRepository: CalendarSyncRepositoryInterface,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val apiService: TrmnlApiService,
) : CoroutineWorker(context, params) {
    companion object {
        const val WORK_NAME = "calendar_sync_work"
    }

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        Timber.d("[$WORK_NAME] Starting calendar sync (run attempt: ${runAttemptCount + 1})")

        return try {
            // Check if sync is enabled
            if (!calendarSyncRepository.isSyncEnabled()) {
                Timber.d("[$WORK_NAME] Calendar sync is disabled, skipping")
                return Result.success()
            }

            // Get API token from user preferences
            val preferences = userPreferencesRepository.userPreferencesFlow.first()
            val apiToken = preferences.apiToken
            if (apiToken.isNullOrBlank()) {
                Timber.w("[$WORK_NAME] No API token found, skipping calendar sync")
                return Result.success()
            }

            // Fetch events from selected calendars
            val syncEvents = calendarSyncRepository.getEventsForSync()
            if (syncEvents.isEmpty()) {
                Timber.d("[$WORK_NAME] No events to sync")
                calendarSyncRepository.recordSyncSuccess()
                return Result.success()
            }

            Timber.d("[$WORK_NAME] Fetched ${syncEvents.size} events for sync")

            // Convert SyncEvent to CalendarEvent for API and send to TRMNL
            val request = CalendarSyncRequest(events = syncEvents.map { it.toCalendarEvent() })
            when (val apiResult = apiService.syncCalendarEvents("Bearer $apiToken", request)) {
                is ApiResult.Success -> {
                    val executionTime = System.currentTimeMillis() - startTime
                    Timber.d("[$WORK_NAME] Calendar sync completed successfully in ${executionTime}ms")
                    calendarSyncRepository.recordSyncSuccess()
                    Result.success()
                }

                is ApiResult.Failure.HttpFailure -> {
                    val executionTime = System.currentTimeMillis() - startTime
                    val errorMsg = "HTTP ${apiResult.code}"
                    Timber.e("[$WORK_NAME] HTTP error ${apiResult.code} during sync (execution time: ${executionTime}ms)")
                    calendarSyncRepository.recordSyncError(errorMsg)
                    if (apiResult.code == 401) {
                        // Token is invalid, don't retry
                        Result.failure()
                    } else if (runAttemptCount < 2) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }

                is ApiResult.Failure.NetworkFailure -> {
                    val executionTime = System.currentTimeMillis() - startTime
                    val errorMsg = apiResult.error?.message ?: "Network error"
                    Timber.e(apiResult.error, "[$WORK_NAME] Network error during sync (execution time: ${executionTime}ms)")
                    calendarSyncRepository.recordSyncError(errorMsg)
                    if (runAttemptCount < 2) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }

                is ApiResult.Failure.ApiFailure -> {
                    val executionTime = System.currentTimeMillis() - startTime
                    val errorMsg = "API error: ${apiResult.error}"
                    Timber.e("[$WORK_NAME] API error during sync (execution time: ${executionTime}ms): $errorMsg")
                    calendarSyncRepository.recordSyncError(errorMsg)
                    Result.failure()
                }

                is ApiResult.Failure.UnknownFailure -> {
                    val executionTime = System.currentTimeMillis() - startTime
                    val errorMsg = apiResult.error?.message ?: "Unknown error"
                    Timber.e(apiResult.error, "[$WORK_NAME] Unknown error during sync (execution time: ${executionTime}ms)")
                    calendarSyncRepository.recordSyncError(errorMsg)
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            val errorMsg = e.message ?: "Unknown error"
            Timber.e(e, "[$WORK_NAME] Calendar sync failed after ${executionTime}ms: $errorMsg")
            calendarSyncRepository.recordSyncError(errorMsg)
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    @WorkerKey(CalendarSyncWorker::class)
    @ContributesIntoMap(
        AppScope::class,
        binding = binding<WorkerInstanceFactory<*>>(),
    )
    @AssistedFactory
    abstract class Factory : WorkerInstanceFactory<CalendarSyncWorker>
}

/**
 * Converts a [SyncEvent] from the calendar-sync module to a [CalendarEvent]
 * required by the TRMNL API.
 */
private fun SyncEvent.toCalendarEvent(): CalendarEvent =
    CalendarEvent(
        title = summary,
        startTime = startFull,
        endTime = endFull,
        description = description,
        allDay = allDay,
    )
