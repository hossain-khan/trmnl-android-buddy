package ink.trmnl.android.buddy.calendar.repository

import ink.trmnl.android.buddy.calendar.models.SyncCalendar
import ink.trmnl.android.buddy.calendar.models.SyncEvent

/**
 * Interface for managing calendar sync operations.
 *
 * Responsibilities:
 * - Query available calendars from device
 * - Manage user's calendar selection preferences
 * - Query events from selected calendars
 * - Track sync status and errors
 *
 * PERMISSIONS: READ_CALENDAR only (no write operations)
 */
interface CalendarSyncRepositoryInterface {
    /**
     * Get all available calendars on the device.
     */
    suspend fun getAvailableCalendars(): List<SyncCalendar>

    /**
     * Update calendar selection preferences.
     *
     * @param selectedCalendarIds List of calendar IDs user wants to sync
     */
    suspend fun updateCalendarSelection(selectedCalendarIds: List<Long>)

    /**
     * Get events from selected calendars within time window.
     *
     * @param startTime Events after this timestamp (milliseconds since epoch)
     * @param endTime Events before this timestamp (milliseconds since epoch)
     * @return List of events in TRMNL sync format
     */
    suspend fun getEventsForSync(
        startTime: Long = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000),
        endTime: Long = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000),
    ): List<SyncEvent>

    /**
     * Check if calendar sync is enabled by user.
     */
    fun isSyncEnabled(): Boolean

    /**
     * Enable or disable calendar sync.
     *
     * @param enabled True to enable sync, false to disable
     */
    fun setSyncEnabled(enabled: Boolean)

    /**
     * Get the timestamp of last successful sync.
     *
     * @return Milliseconds since epoch, or 0 if never synced
     */
    fun getLastSyncTime(): Long

    /**
     * Record successful sync completion.
     */
    fun recordSyncSuccess()

    /**
     * Record sync error.
     *
     * @param error Error message
     */
    fun recordSyncError(error: String)

    /**
     * Get last sync error message.
     *
     * @return Error message, or null if no error
     */
    fun getLastSyncError(): String?

    /**
     * Disconnect calendar sync (disable and clear selection).
     */
    fun disconnect()

    /**
     * Get cached plugin setting ID for the calendar plugin.
     *
     * Used to avoid repeated calls to GET /plugin_settings in the sync workflow.
     *
     * @return Cached plugin setting ID, or null if not cached
     */
    fun getCachedPluginSettingId(): Int?

    /**
     * Cache the plugin setting ID for the calendar plugin.
     *
     * @param id Plugin setting ID returned by GET /plugin_settings?plugin_id=calendars
     */
    fun cachePluginSettingId(id: Int)
}
