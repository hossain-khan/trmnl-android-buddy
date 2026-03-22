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
import ink.trmnl.android.buddy.api.models.MergeVariables
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
 * This worker implements the TRMNL Companion 3-step sync workflow:
 * 1. Validate API key via GET /me
 * 2. Get calendar plugin setting ID via GET /plugin_settings?plugin_id=calendars
 * 3. Sync events via POST /plugin_settings/{id}/data
 *
 * The plugin setting ID is cached in [CalendarSyncRepositoryInterface] to avoid
 * repeated calls to the settings endpoint on every sync.
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
        private const val CALENDAR_PLUGIN_ID = "calendars"
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

            // Fetch events from selected calendars (early exit if empty)
            val syncEvents = calendarSyncRepository.getEventsForSync()
            if (syncEvents.isEmpty()) {
                Timber.d("[$WORK_NAME] No events to sync")
                calendarSyncRepository.recordSyncSuccess()
                return Result.success()
            }

            Timber.d("[$WORK_NAME] Fetched ${syncEvents.size} events for sync")

            val authorization = "Bearer $apiToken"

            // Step 1: Validate API key via GET /me
            val validateResult = validateApiKey(authorization)
            if (validateResult != null) return validateResult

            // Step 2: Get plugin setting ID (with caching)
            val settingId =
                resolvePluginSettingId(authorization)
                    ?: return run {
                        Timber.e("[$WORK_NAME] No calendar plugin setting found")
                        calendarSyncRepository.recordSyncError("No calendar plugin setting found")
                        Result.failure()
                    }

            // Step 3: Sync events via POST /plugin_settings/{id}/data
            val request =
                CalendarSyncRequest(
                    mergeVariables = MergeVariables(events = syncEvents.map { it.toCalendarEvent() }),
                )
            when (val apiResult = apiService.syncCalendarEvents(settingId, authorization, request)) {
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

    /**
     * Step 1: Validates the API key by calling GET /me.
     *
     * @return A [Result] to return early if validation fails, or null to continue
     */
    private suspend fun validateApiKey(authorization: String): Result? =
        when (val result = apiService.userInfo(authorization)) {
            is ApiResult.Success -> null // continue
            is ApiResult.Failure.HttpFailure -> {
                val errorMsg = "HTTP ${result.code} during API key validation"
                Timber.e("[$WORK_NAME] $errorMsg")
                calendarSyncRepository.recordSyncError(errorMsg)
                if (result.code == 401) {
                    Result.failure()
                } else if (runAttemptCount < 2) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
            is ApiResult.Failure.NetworkFailure -> {
                val errorMsg = result.error?.message ?: "Network error during API key validation"
                Timber.e(result.error, "[$WORK_NAME] $errorMsg")
                calendarSyncRepository.recordSyncError(errorMsg)
                if (runAttemptCount < 2) Result.retry() else Result.failure()
            }
            is ApiResult.Failure.ApiFailure -> {
                val errorMsg = "API error during validation: ${result.error}"
                Timber.e("[$WORK_NAME] $errorMsg")
                calendarSyncRepository.recordSyncError(errorMsg)
                Result.failure()
            }
            is ApiResult.Failure.UnknownFailure -> {
                val errorMsg = result.error?.message ?: "Unknown error during API key validation"
                Timber.e(result.error, "[$WORK_NAME] $errorMsg")
                calendarSyncRepository.recordSyncError(errorMsg)
                Result.failure()
            }
        }

    /**
     * Step 2: Resolves the calendar plugin setting ID.
     *
     * Returns the cached ID if available; otherwise fetches from GET /plugin_settings?plugin_id=calendars
     * and caches the result for future syncs.
     *
     * @return Plugin setting ID, or null if not found or an error occurred
     */
    private suspend fun resolvePluginSettingId(authorization: String): Int? {
        // Return cached ID if available
        calendarSyncRepository.getCachedPluginSettingId()?.let { return it }

        Timber.d("[$WORK_NAME] Fetching plugin setting ID for calendar plugin")
        return when (val result = apiService.getPluginSettings(CALENDAR_PLUGIN_ID, authorization)) {
            is ApiResult.Success -> {
                val id =
                    result.value.data
                        .firstOrNull()
                        ?.id
                if (id != null) {
                    calendarSyncRepository.cachePluginSettingId(id)
                    Timber.d("[$WORK_NAME] Cached plugin setting ID: $id")
                }
                id
            }
            is ApiResult.Failure -> {
                Timber.e("[$WORK_NAME] Failed to fetch plugin settings: $result")
                null
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
 * required by the TRMNL API, following the Companion app specification.
 */
private fun SyncEvent.toCalendarEvent(): CalendarEvent =
    CalendarEvent(
        summary = summary,
        start = startTime,
        startFull = startFull,
        dateTime = dateTime,
        end = endTime,
        endFull = endFull,
        allDay = allDay,
        description = description ?: "",
        status = status,
        calendarIdentifier = calendarName,
    )
