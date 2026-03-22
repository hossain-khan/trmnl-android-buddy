package ink.trmnl.android.buddy.fakes

import ink.trmnl.android.buddy.calendar.models.SyncCalendar
import ink.trmnl.android.buddy.calendar.models.SyncEvent
import ink.trmnl.android.buddy.calendar.repository.CalendarSyncRepositoryInterface

/**
 * Fake implementation of [CalendarSyncRepositoryInterface] for testing.
 *
 * Provides a working in-memory implementation suitable for unit tests,
 * following the project's testing guidelines of using fakes instead of mocks.
 *
 * Exposes test-visible properties for verifying that repository methods were called
 * with the expected values.
 *
 * @param initialCalendars Calendars returned by [getAvailableCalendars]. Defaults to empty list.
 * @param initialSyncEnabled Initial sync enabled state. Defaults to false.
 * @param initialLastSyncTime Last sync timestamp in ms. Defaults to 0 (never synced).
 * @param initialLastSyncError Last sync error message. Defaults to null.
 * @param shouldThrowOnGetCalendars If true, [getAvailableCalendars] throws an exception.
 * @param shouldThrowOnGetEvents If true, [getEventsForSync] throws an exception.
 * @param eventsToReturn Events returned by [getEventsForSync]. Defaults to empty list.
 */
class FakeCalendarSyncRepository(
    private val initialCalendars: List<SyncCalendar> = emptyList(),
    private val initialSyncEnabled: Boolean = false,
    private val initialLastSyncTime: Long = 0L,
    private val initialLastSyncError: String? = null,
    private val shouldThrowOnGetCalendars: Boolean = false,
    private val shouldThrowOnGetEvents: Boolean = false,
    private val eventsToReturn: List<SyncEvent> = emptyList(),
) : CalendarSyncRepositoryInterface {
    // ============================================================
    // In-memory state
    // ============================================================

    private var syncEnabled = initialSyncEnabled
    private var lastSyncTime = initialLastSyncTime
    private var lastSyncError = initialLastSyncError
    private val selectedCalendarIds =
        initialCalendars
            .filter { it.isSelected }
            .map { it.calendarId }
            .toMutableList()

    // ============================================================
    // Test-visible properties for assertions
    // ============================================================

    /** Number of times [getAvailableCalendars] was called. */
    var getAvailableCalendarsCallCount = 0
        private set

    /** Number of times [updateCalendarSelection] was called. */
    var updateCalendarSelectionCallCount = 0
        private set

    /** Last list of calendar IDs passed to [updateCalendarSelection]. */
    var lastUpdatedCalendarIds: List<Long>? = null
        private set

    /** Number of times [setSyncEnabled] was called. */
    var setSyncEnabledCallCount = 0
        private set

    /** Last value passed to [setSyncEnabled]. */
    var lastSyncEnabledValue: Boolean? = null
        private set

    /** Number of times [recordSyncSuccess] was called. */
    var recordSyncSuccessCallCount = 0
        private set

    /** Number of times [recordSyncError] was called. */
    var recordSyncErrorCallCount = 0
        private set

    /** Last error message passed to [recordSyncError]. */
    var lastSyncErrorRecorded: String? = null
        private set

    /** Whether [disconnect] was called. */
    var wasDisconnected = false
        private set

    // ============================================================
    // Interface implementation
    // ============================================================

    override suspend fun getAvailableCalendars(): List<SyncCalendar> {
        getAvailableCalendarsCallCount++
        if (shouldThrowOnGetCalendars) {
            throw Exception("Simulated calendar load failure")
        }
        return initialCalendars.map { calendar ->
            calendar.copy(isSelected = calendar.calendarId in selectedCalendarIds)
        }
    }

    override suspend fun updateCalendarSelection(selectedCalendarIds: List<Long>) {
        updateCalendarSelectionCallCount++
        lastUpdatedCalendarIds = selectedCalendarIds
        this.selectedCalendarIds.clear()
        this.selectedCalendarIds.addAll(selectedCalendarIds)
    }

    override suspend fun getEventsForSync(
        startTime: Long,
        endTime: Long,
    ): List<SyncEvent> {
        if (shouldThrowOnGetEvents) {
            throw Exception("Simulated sync failure")
        }
        return eventsToReturn
    }

    override fun isSyncEnabled(): Boolean = syncEnabled

    override fun setSyncEnabled(enabled: Boolean) {
        setSyncEnabledCallCount++
        lastSyncEnabledValue = enabled
        syncEnabled = enabled
    }

    override fun getLastSyncTime(): Long = lastSyncTime

    override fun recordSyncSuccess() {
        recordSyncSuccessCallCount++
        lastSyncTime = System.currentTimeMillis()
        lastSyncError = null
    }

    override fun recordSyncError(error: String) {
        recordSyncErrorCallCount++
        lastSyncErrorRecorded = error
        lastSyncError = error
    }

    override fun getLastSyncError(): String? = lastSyncError

    override fun disconnect() {
        wasDisconnected = true
        syncEnabled = false
        selectedCalendarIds.clear()
        lastSyncTime = 0L
        lastSyncError = null
    }
}
