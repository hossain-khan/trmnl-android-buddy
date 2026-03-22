package ink.trmnl.android.buddy.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.calendar.models.SyncEvent
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.fakes.FakeCalendarSyncRepository
import ink.trmnl.android.buddy.fakes.FakeTrmnlApiService
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * Unit tests for [CalendarSyncWorker].
 *
 * Tests cover:
 * - Sync disabled → returns success without calling API
 * - No API token → returns success without calling API
 * - No events → returns success and records success
 * - Successful sync with events → returns success, records success, calls API
 * - API 401 authentication failure → returns failure
 * - API HTTP error (non-401) → returns retry (up to 2 attempts), then failure
 * - Network error → returns retry (up to 2 attempts), then failure
 * - Unknown/API failure → returns failure
 * - Exception during event fetch → returns retry (up to 2 attempts), then failure
 *
 * Uses fake implementations following project testing guidelines:
 * - [FakeCalendarSyncRepository] for repository testing
 * - [FakeUserPreferencesRepository] for user preferences
 * - [FakeTrmnlApiService] for API calls
 */
@RunWith(RobolectricTestRunner::class)
class CalendarSyncWorkerTest {
    private lateinit var context: Context
    private lateinit var fakeCalendarSyncRepository: FakeCalendarSyncRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository
    private lateinit var fakeTrmnlApiService: FakeTrmnlApiService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeCalendarSyncRepository = FakeCalendarSyncRepository()
        fakeUserPreferencesRepository =
            FakeUserPreferencesRepository(
                initialPreferences = UserPreferences(apiToken = "test_api_token"),
            )
        fakeTrmnlApiService = FakeTrmnlApiService()
    }

    // ========================================
    // Sync disabled / skip scenarios
    // ========================================

    @Test
    fun `sync disabled returns success without calling api`() =
        runTest {
            // Given: Calendar sync is disabled (default)
            fakeCalendarSyncRepository = FakeCalendarSyncRepository(initialSyncEnabled = false)

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success returned without calling the API
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(fakeTrmnlApiService.syncCalendarEventsCallCount).isEqualTo(0)
        }

    @Test
    fun `no api token returns success without calling api`() =
        runTest {
            // Given: Sync enabled but no API token
            fakeCalendarSyncRepository = FakeCalendarSyncRepository(initialSyncEnabled = true)
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = null),
                )

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success returned without calling the API
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(fakeTrmnlApiService.syncCalendarEventsCallCount).isEqualTo(0)
        }

    @Test
    fun `blank api token returns success without calling api`() =
        runTest {
            // Given: Sync enabled but blank API token
            fakeCalendarSyncRepository = FakeCalendarSyncRepository(initialSyncEnabled = true)
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = ""),
                )

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success returned without calling the API
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(fakeTrmnlApiService.syncCalendarEventsCallCount).isEqualTo(0)
        }

    // ========================================
    // Empty events scenario
    // ========================================

    @Test
    fun `no events returns success and records sync success`() =
        runTest {
            // Given: Sync enabled, API token set, but no events to sync
            fakeCalendarSyncRepository = FakeCalendarSyncRepository(initialSyncEnabled = true)
            fakeTrmnlApiService.syncCalendarEventsResult = ApiResult.success(Unit)

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success returned, sync success recorded, no API call made
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(fakeCalendarSyncRepository.recordSyncSuccessCallCount).isEqualTo(1)
            assertThat(fakeTrmnlApiService.syncCalendarEventsCallCount).isEqualTo(0)
        }

    // ========================================
    // Successful sync scenarios
    // ========================================

    @Test
    fun `successful sync with events returns success`() =
        runTest {
            // Given: Sync enabled, events available, API returns success
            val events = listOf(createSyncEvent("Meeting"), createSyncEvent("Lunch"))
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    eventsToReturn = events,
                )
            fakeTrmnlApiService.syncCalendarEventsResult = ApiResult.success(Unit)

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Success returned
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        }

    @Test
    fun `successful sync records sync success`() =
        runTest {
            // Given: Sync enabled, events available, API returns success
            val events = listOf(createSyncEvent("Team Standup"))
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    eventsToReturn = events,
                )
            fakeTrmnlApiService.syncCalendarEventsResult = ApiResult.success(Unit)

            // When: Worker executes
            val worker = createWorker()
            worker.doWork()

            // Then: Sync success recorded
            assertThat(fakeCalendarSyncRepository.recordSyncSuccessCallCount).isEqualTo(1)
        }

    @Test
    fun `successful sync sends correct api token`() =
        runTest {
            // Given: Sync enabled, events available, API token set
            val events = listOf(createSyncEvent("All Hands"))
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    eventsToReturn = events,
                )
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = "my_secret_token"),
                )
            fakeTrmnlApiService.syncCalendarEventsResult = ApiResult.success(Unit)

            // When: Worker executes
            val worker = createWorker()
            worker.doWork()

            // Then: API called with correct bearer token
            assertThat(fakeTrmnlApiService.lastAuthorizationHeader).isEqualTo("Bearer my_secret_token")
        }

    @Test
    fun `successful sync sends all events to api`() =
        runTest {
            // Given: Sync enabled, 3 events available
            val events =
                listOf(
                    createSyncEvent("Event 1"),
                    createSyncEvent("Event 2"),
                    createSyncEvent("Event 3"),
                )
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    eventsToReturn = events,
                )
            fakeTrmnlApiService.syncCalendarEventsResult = ApiResult.success(Unit)

            // When: Worker executes
            val worker = createWorker()
            worker.doWork()

            // Then: API called with all 3 events
            assertThat(fakeTrmnlApiService.syncCalendarEventsCallCount).isEqualTo(1)
            assertThat(fakeTrmnlApiService.lastSyncCalendarEventsRequest).isNotNull()
            assertThat(fakeTrmnlApiService.lastSyncCalendarEventsRequest!!.events.size).isEqualTo(3)
        }

    // ========================================
    // API failure scenarios
    // ========================================

    @Test
    fun `http 401 failure returns failure without retry`() =
        runTest {
            // Given: API returns 401 unauthorized
            val events = listOf(createSyncEvent("Event"))
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    eventsToReturn = events,
                )
            fakeTrmnlApiService.syncCalendarEventsResult =
                ApiResult.httpFailure(401, ApiError(error = "Unauthorized"))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Failure returned (no retry for auth errors)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
        }

    @Test
    fun `http 401 failure records sync error`() =
        runTest {
            // Given: API returns 401 unauthorized
            val events = listOf(createSyncEvent("Event"))
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    eventsToReturn = events,
                )
            fakeTrmnlApiService.syncCalendarEventsResult =
                ApiResult.httpFailure(401, ApiError(error = "Unauthorized"))

            // When: Worker executes
            val worker = createWorker()
            worker.doWork()

            // Then: Sync error recorded
            assertThat(fakeCalendarSyncRepository.recordSyncErrorCallCount).isEqualTo(1)
        }

    @Test
    fun `http 500 failure on first attempt returns retry`() =
        runTest {
            // Given: API returns 500 server error
            val events = listOf(createSyncEvent("Event"))
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    eventsToReturn = events,
                )
            fakeTrmnlApiService.syncCalendarEventsResult =
                ApiResult.httpFailure(500, ApiError(error = "Internal Server Error"))

            // When: Worker executes on first attempt (runAttemptCount = 0)
            val worker = createWorker(runAttemptCount = 0)
            val result = worker.doWork()

            // Then: Retry returned
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        }

    @Test
    fun `network failure on first attempt returns retry`() =
        runTest {
            // Given: Network failure
            val events = listOf(createSyncEvent("Event"))
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    eventsToReturn = events,
                )
            fakeTrmnlApiService.syncCalendarEventsResult =
                ApiResult.networkFailure(IOException("No internet connection"))

            // When: Worker executes on first attempt
            val worker = createWorker(runAttemptCount = 0)
            val result = worker.doWork()

            // Then: Retry returned
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        }

    @Test
    fun `network failure records sync error`() =
        runTest {
            // Given: Network failure
            val events = listOf(createSyncEvent("Event"))
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    eventsToReturn = events,
                )
            fakeTrmnlApiService.syncCalendarEventsResult =
                ApiResult.networkFailure(IOException("No internet"))

            // When: Worker executes
            val worker = createWorker(runAttemptCount = 0)
            worker.doWork()

            // Then: Sync error recorded
            assertThat(fakeCalendarSyncRepository.recordSyncErrorCallCount).isEqualTo(1)
        }

    @Test
    fun `unknown failure returns failure`() =
        runTest {
            // Given: Unknown API failure
            val events = listOf(createSyncEvent("Event"))
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    eventsToReturn = events,
                )
            fakeTrmnlApiService.syncCalendarEventsResult =
                ApiResult.unknownFailure(RuntimeException("Unexpected error"))

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Failure returned
            assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
        }

    // ========================================
    // Exception handling scenarios
    // ========================================

    @Test
    fun `exception during event fetch on first attempt returns retry`() =
        runTest {
            // Given: Event fetch throws exception
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    shouldThrowOnGetEvents = true,
                )

            // When: Worker executes on first attempt
            val worker = createWorker(runAttemptCount = 0)
            val result = worker.doWork()

            // Then: Retry returned
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        }

    @Test
    fun `exception during event fetch records sync error`() =
        runTest {
            // Given: Event fetch throws exception
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    shouldThrowOnGetEvents = true,
                )

            // When: Worker executes
            val worker = createWorker(runAttemptCount = 0)
            worker.doWork()

            // Then: Sync error recorded
            assertThat(fakeCalendarSyncRepository.recordSyncErrorCallCount).isEqualTo(1)
        }

    @Test
    fun `exception on third attempt returns failure`() =
        runTest {
            // Given: Event fetch throws exception
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    shouldThrowOnGetEvents = true,
                )

            // When: Worker executes on third attempt (runAttemptCount = 2, 0-indexed)
            val worker = createWorker(runAttemptCount = 2)
            val result = worker.doWork()

            // Then: Failure returned (no more retries)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
        }

    // ========================================
    // Event conversion scenario
    // ========================================

    @Test
    fun `sync events are converted correctly to calendar events`() =
        runTest {
            // Given: Sync enabled with a specific event
            val event =
                createSyncEvent(
                    summary = "Important Meeting",
                    description = "Quarterly review",
                    startFull = "2025-06-15T09:00:00.000Z",
                    endFull = "2025-06-15T10:00:00.000Z",
                    allDay = false,
                )
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    eventsToReturn = listOf(event),
                )
            fakeTrmnlApiService.syncCalendarEventsResult = ApiResult.success(Unit)

            // When: Worker executes
            val worker = createWorker()
            worker.doWork()

            // Then: API called with correctly converted event
            val sentRequest = fakeTrmnlApiService.lastSyncCalendarEventsRequest
            assertThat(sentRequest).isNotNull()
            val calendarEvent = sentRequest!!.events.first()
            assertThat(calendarEvent.title).isEqualTo("Important Meeting")
            assertThat(calendarEvent.startTime).isEqualTo("2025-06-15T09:00:00.000Z")
            assertThat(calendarEvent.endTime).isEqualTo("2025-06-15T10:00:00.000Z")
            assertThat(calendarEvent.description).isEqualTo("Quarterly review")
            assertThat(calendarEvent.allDay).isEqualTo(false)
        }

    @Test
    fun `all-day event is converted correctly`() =
        runTest {
            // Given: All-day event
            val event = createSyncEvent(summary = "Holiday", allDay = true)
            fakeCalendarSyncRepository =
                FakeCalendarSyncRepository(
                    initialSyncEnabled = true,
                    eventsToReturn = listOf(event),
                )
            fakeTrmnlApiService.syncCalendarEventsResult = ApiResult.success(Unit)

            // When: Worker executes
            val worker = createWorker()
            worker.doWork()

            // Then: API event has allDay = true
            val sentRequest = fakeTrmnlApiService.lastSyncCalendarEventsRequest
            assertThat(sentRequest!!.events.first().allDay).isTrue()
        }

    // ========================================
    // Helper methods
    // ========================================

    /**
     * Creates a test worker instance with fake dependencies.
     *
     * @param runAttemptCount Simulates how many times the worker has been retried (0-indexed).
     */
    private fun createWorker(runAttemptCount: Int = 0): CalendarSyncWorker {
        val workerFactory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker =
                    CalendarSyncWorker(
                        context = appContext,
                        params = workerParameters,
                        calendarSyncRepository = fakeCalendarSyncRepository,
                        userPreferencesRepository = fakeUserPreferencesRepository,
                        apiService = fakeTrmnlApiService,
                    )
            }

        return TestListenableWorkerBuilder<CalendarSyncWorker>(context)
            .setWorkerFactory(workerFactory)
            .setRunAttemptCount(runAttemptCount)
            .build()
    }

    /**
     * Creates a test [SyncEvent] with configurable fields.
     */
    private fun createSyncEvent(
        summary: String,
        description: String? = null,
        startFull: String = "2025-06-15T09:00:00.000Z",
        endFull: String = "2025-06-15T10:00:00.000Z",
        allDay: Boolean = false,
        calendarName: String = "test@example.com",
    ): SyncEvent =
        SyncEvent(
            startTime = "09:00",
            endTime = "10:00",
            startFull = startFull,
            endFull = endFull,
            dateTime = startFull,
            summary = summary,
            description = description,
            allDay = allDay,
            status = "accepted",
            calendarName = calendarName,
        )
}
